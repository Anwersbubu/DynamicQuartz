package com.yangjie.dynamicquartz.util;

import java.util.List;

/**
 * 自定义枚举单例对象 StringUtil
 */
public enum StringUtil {
    ;

    //获取List参数值
    public static String getListString(List<String> list) {
        StringBuilder result = new StringBuilder();
        for (String s : list) {
            result.append(s).append(" ");
        }
        return result.toString();
    }

}
