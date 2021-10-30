package com.yangjie.dynamicquartz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;

/**
 * Created by EalenXie on 2018/6/4 14:09
 * 这里个人示例,可自定义相关属性
 */

@TableName("job_entity")
@Data
@Accessors(chain = true)
public class JobEntity implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("name")
    private String name;          //job名称

    @TableField("job_group")
    private String jobGroup;      //job组名

    @TableField("cron")
    private String cron;          //执行的cron

    @TableField("parameter")
    private String parameter;     //job的参数

    @TableField("description")
    private String description;   //job描述信息

    @TableField("vm_param")
    private String vmParam;       //vm参数

    @TableField("jar_path")
    private String jarPath;       //job的jar路径

    @TableField("status")
    private String status;        //job的执行状态,这里我设置为OPEN/CLOSE且只有该值为OPEN才会执行该Job

    public JobEntity(Integer id, String name, String jobGroup, String cron, String parameter, String description, String vmParam, String jarPath, String status) {
        this.id = id;
        this.name = name;
        this.jobGroup = jobGroup;
        this.cron = cron;
        this.parameter = parameter;
        this.description = description;
        this.vmParam = vmParam;
        this.jarPath = jarPath;
        this.status = status;
    }
}
