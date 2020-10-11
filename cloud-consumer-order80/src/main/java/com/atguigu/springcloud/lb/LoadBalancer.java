package com.atguigu.springcloud.lb;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * 手写轮询算法
 */
public interface LoadBalancer {
    //得到List里面的所有对象(所有服务都放进List)
    ServiceInstance instances(List<ServiceInstance> serviceInstances);
}
