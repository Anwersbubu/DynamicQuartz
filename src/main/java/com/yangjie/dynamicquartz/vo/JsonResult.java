package com.yangjie.dynamicquartz.vo;

import lombok.Data;

import java.util.HashMap;

@Data
public class JsonResult {

    boolean success;
    String message;
    HashMap<String,Object> data = new HashMap<>();

}
