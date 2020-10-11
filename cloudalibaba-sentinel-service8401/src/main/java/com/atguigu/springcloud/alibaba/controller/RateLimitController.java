package com.atguigu.springcloud.alibaba.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.atguigu.springcloud.alibaba.myhandler.CustomBlockHandler;
import com.atguigu.springcloud.entities.CommonResult;
import com.atguigu.springcloud.entities.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class RateLimitController {
    @GetMapping("/byResource")
    @SentinelResource(value = "byResource",
            blockHandlerClass = CustomBlockHandler.class,
            blockHandler = "handlerException")
    public CommonResult byResource()
    {
        return new CommonResult(200,"按资源名称限流测试ok",new Payment(2020L,"serial001"));
    }

    @GetMapping("/rateLimit/byUrl")
    @SentinelResource(value = "byUrl")
    public CommonResult byUrl()
    {
        return new CommonResult(200,"按URL限流测试ok",new Payment(2020L,"serial002"));
    }

    /**
     * CustomBlockHandler 全局处理类
     */
    @GetMapping("/rateLimit/CustomBlockHandler")
    @SentinelResource(value = "CustomBlockHandler",
            blockHandlerClass = CustomBlockHandler.class,
            blockHandler = "handleException2")
    public CommonResult CustomBlockHandler()
    {
        return new CommonResult(200,"按客户自定义",new Payment(2020L,"serial003"));
    }
}
