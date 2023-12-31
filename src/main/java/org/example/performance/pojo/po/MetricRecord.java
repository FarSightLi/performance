package org.example.performance.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @TableName metric_record
 */
@TableName(value = "metric_record")
@Data
public class MetricRecord implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 指标id
     */
    private Integer metricId;

    /**
     * 指标具体值
     */
    private BigDecimal metricValue;

    /**
     * 指标来源维度
     */
    private Long metricOrigin;

    /**
     * 监控时间
     */
    private LocalDateTime monitorTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}