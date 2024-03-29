package org.example.performance.component.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.example.performance.component.CacheInfo;
import org.example.performance.component.FileWatcher;
import org.example.performance.component.exception.BusinessException;
import org.example.performance.component.exception.CodeMsg;
import org.example.performance.config.SessionConfig;
import org.example.performance.config.ThreadPoolConfig;
import org.example.performance.pojo.bo.ContainerMetricsBO;
import org.example.performance.pojo.bo.HostMetricsBO;
import org.example.performance.pojo.po.ContainerInfo;
import org.example.performance.pojo.po.DiskInfo;
import org.example.performance.pojo.po.HostInfo;
import org.example.performance.service.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MetricsScheduledTask {
    private final InfoService infoService = new InfoService();
    @Resource
    private HostInfoService hostInfoService;
    @Resource
    private DiskInfoService diskInfoService;
    @Resource
    private ContainerInfoService containerInfoService;
    @Resource
    private FileWatcher fileWatcher;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private MetricRecordService metricRecordService;
    @Resource
    private JobLockService jobLockService;

    /**
     * ip对应容器map的redis的key
     */
    private static final String IP_CONTAINER_KEY = "container:ip_container";

    @PostConstruct
    public void init() {
        fileWatcher.read();
        log.info("采集任务前获得主机账号密码");
    }

    /**
     * 获得主机信息
     */
    @Scheduled(fixedRate = 10 * 60 * 1000) //十分钟
    public void getSysInfoTask() {
        String jobName = "sysInfo";
        boolean lock = jobLockService.getLock(jobName, 10);
        if (lock) {
            List<CompletableFuture<Void>> sysFutures = new ArrayList<>();
            List<HostInfo> hostInfoList = Collections.synchronizedList(new ArrayList<>());

            // 主机信息采集
            CacheInfo.getIpList().forEach(ip -> {
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            HostInfo hostInfo = infoService.getSysInfo(SessionConfig.getSession(ip), ip);
                            hostInfoList.add(hostInfo);
                        }, ThreadPoolConfig.getSys());
                        sysFutures.add(future);
                    }
            );
            sysFutures.forEach(CompletableFuture::join);
            log.info("主机信息:" + hostInfoList);
            // 保存主机信息
            hostInfoService.updateOrInsertBatch(hostInfoList);

            // 保存容器信息
            List<ContainerInfo> containerList = getContainerList(hostInfoList);
            containerInfoService.updateOrInsertContainer(containerList);
            // 保存ip和容器id对应关系到缓存
            updateCache(containerList
                    .stream().collect(Collectors.groupingBy(ContainerInfo::getHostIp, Collectors.mapping(ContainerInfo::getContainerId, Collectors.toList()))));
            log.info("所有主机采集完毕");
        }
        if (lock) {
            jobLockService.releaseLock(jobName);
        }
    }

    /**
     * 获得容器信息
     */
    @Scheduled(fixedRate = 10 * 60 * 1000) //十分钟
    public void getContainerInfoTask() {
        String jobName = "ContainerInfo";
        boolean lock = jobLockService.getLock(jobName, 10);
        if (lock) {
            Map<String, List<String>> containerMap = getContainerMap();
            List<ContainerInfo> containerInfoList = Collections.synchronizedList(new ArrayList<>());
            // 容器信息采集
            List<CompletableFuture<Void>> containerFutures = new ArrayList<>();
            containerMap.forEach((ip, idList) -> idList.forEach(id -> {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    containerInfoList.add(infoService.getContainerInfo(SessionConfig.getSession(ip), id, ip));
                }, ThreadPoolConfig.getContainer());
                containerFutures.add(future);
            }));
            containerFutures.forEach(CompletableFuture::join);
            log.info("容器基础信息:" + containerInfoList);
            containerInfoService.updateOrInsertContainer(containerInfoList);
            log.info("所有容器基础信息采集完毕");
        }
        if (lock) {
            jobLockService.releaseLock(jobName);
        }
    }

    /**
     * 获得主机性能指标
     */
    @Scheduled(fixedRate = 60 * 1000) //每分钟
    public void getSysIndexTask() {
        String jobName = "HostMetric";
        boolean lock = jobLockService.getLock(jobName, 1);
        if (lock) {
            List<CompletableFuture<Void>> sysFutures = new ArrayList<>();
            List<HostMetricsBO> hostMetricsBOList = Collections.synchronizedList(new ArrayList<>());
            List<DiskInfo> diskInfoList = Collections.synchronizedList(new ArrayList<>());
            // 主机性能采集
            CacheInfo.getIpList().forEach(ip -> {
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            HostMetricsBO hostMetricsBO = infoService.getSysIndex(SessionConfig.getSession(ip), ip);
                            hostMetricsBOList.add(hostMetricsBO);
                            diskInfoList.addAll(hostMetricsBO.getDiskInfoList());
                        }, ThreadPoolConfig.getSys());
                        sysFutures.add(future);
                    }
            );
            sysFutures.forEach(CompletableFuture::join);
            log.info("主机性能指标:" + hostMetricsBOList);
            diskInfoService.saveDiskInfo(diskInfoList);
            metricRecordService.insertHostBatch(hostMetricsBOList);
            log.info("所有主机性能采集完毕");
            jobLockService.releaseLock(jobName);
        }
    }

    /**
     * 获得容器性能指标
     */
    @Scheduled(fixedRate = 60 * 1000) //每分钟
    public void getContainerIndexInfoTask() {
        String jobName = "ContainerMetric";
        boolean lock = jobLockService.getLock(jobName, 1);
        if (lock) {
            Map<String, List<String>> containerMap = getContainerMap();
            List<ContainerMetricsBO> containerMetricsBOList = Collections.synchronizedList(new ArrayList<>());
            // 容器信息采集
            List<CompletableFuture<Void>> containerFutures = new ArrayList<>();
            containerMap.forEach((ip, idList) -> idList.forEach(id -> {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> containerMetricsBOList.add(infoService.getContainerIndexInfo(SessionConfig.getSession(ip), id, ip)), ThreadPoolConfig.getContainer());
                containerFutures.add(future);
            }));
            containerFutures.forEach(CompletableFuture::join);
            log.info("容器性能指标:" + containerMetricsBOList);
            metricRecordService.insertContainerBatch(containerMetricsBOList);
            log.info("所有容器性能指标采集完毕");
            jobLockService.releaseLock(jobName);
        }
    }

    private Map<String, List<String>> getContainerMap() {
        Object containerMapObject = redisTemplate.opsForValue().get(IP_CONTAINER_KEY);
        if (containerMapObject == null) {
            log.info("没有获得ip2ContainerMap缓存信息，查询数据库");
            return containerInfoService.getContainerId(CacheInfo.getIpList());
        }
        if (containerMapObject instanceof HashMap) {
            return (HashMap<String, List<String>>) containerMapObject;
        } else {
            throw new BusinessException(CodeMsg.SYSTEM_ERROR, "ip2ContainerMap在从redis中获取时类型出错");
        }
    }

    /**
     * 更新ip对应容器信息缓存
     *
     * @param newData
     */
    public void updateCache(Map<String, List<String>> newData) {
        redisTemplate.opsForValue().set(IP_CONTAINER_KEY, newData, 10L, TimeUnit.MINUTES);
        log.info("ip对应容器信息缓存已更新");
    }

    private List<ContainerInfo> getContainerList(List<HostInfo> hostInfoList) {
        return hostInfoList.stream()
                .flatMap(hostInfo -> hostInfo.getContainerIdList().stream()
                        .map(containerInfoId -> {
                            ContainerInfo containerInfo = new ContainerInfo();
                            containerInfo.setHostIp(hostInfo.getIp());
                            containerInfo.setContainerId(containerInfoId);
                            return containerInfo;
                        })
                )
                .collect(Collectors.toList());
    }

}
