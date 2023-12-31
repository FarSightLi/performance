package org.example.performance.service;

import org.example.performance.pojo.vo.ContainerInfoVO;
import org.example.performance.pojo.vo.ContainerTrendVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author bonree
 * @description 针对表【container_metrics(容器性能表)】的数据库操作Service
 * @createDate 2023-12-06 14:14:04
 */
public interface ContainerMetricsService {

    List<ContainerInfoVO> getContainerMetricsByIp(String ip, LocalDateTime startTime, LocalDateTime endTime);

    ContainerInfoVO getContainerMetricsById(Long id, LocalDateTime startTime, LocalDateTime endTime);

    List<ContainerTrendVO> getMetricTrend(String ip, LocalDateTime startTime, LocalDateTime endTime);

    List<ContainerTrendVO> getMetricTrendById(Long id, LocalDateTime startTime, LocalDateTime endTime);
}
