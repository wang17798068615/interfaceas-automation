package com.interfaces.iat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
@TableName("tb_environment")
public class EnvironmentEntity extends BaseEntity {
    @TableId(type= IdType.AUTO)
    private Integer id;
    private String name;
    private String host;
    private String dbConfig;
    private String description;
    private Boolean isDelete;
    private Integer projectId;
}
