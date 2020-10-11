package com.atguigu.springcloud.alibaba.myhandler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.atguigu.springcloud.entities.CommonResult;
import com.atguigu.springcloud.entities.Payment;

public class CustomBlockHandler {
    public static CommonResult handlerException(BlockException exception){
        return new CommonResult(4444,"按客户自定义,global handler Exception------1");
    }
    public static CommonResult handleException2(BlockException exception){
        return new CommonResult(4444,"按客户自定义,global handler Exception------2");
    }
}
