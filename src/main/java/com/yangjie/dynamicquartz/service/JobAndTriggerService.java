package com.yangjie.dynamicquartz.service;

import com.github.pagehelper.PageInfo;

public interface JobAndTriggerService {

    //分页查询
    PageInfo getJobAndTriggerDetails(int pageNum, int pageSize);

}
