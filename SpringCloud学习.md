## SpringCloud学习

### 1. cloud-provider-payment8001

​	模块启动总结：

1. 建module
2. 改POM
3. 写YML
4. 主启动
5. 业务类

#### 热部署Devtools

1. 加入xml语句到pom中

   ```xml
   <dependency>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-devtools</artifactId>
               <scope>runtime</scope>
               <optional>true</optional>
           </dependency>
   ```

2. 在父类总工程中加入插件到pom中

   ```xml
   <build>
           <plugins>
               <plugin>
                   <groupId>org.springframework.boot</groupId>
                   <artifactId>spring-boot-maven-plugin</artifactId>
                   <configuration>
                       <fork>true</fork>
                       <addResources>true</addResources>
                   </configuration>
               </plugin>
           </plugins>
       </build>
   ```

3. 开启自动编译的选项

4. 更新值 ctrl + shift + alt + /

   ![image-20200916200151097](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20200916200151097.png)

   ![image-20200916200315907](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20200916200315907.png)

5. 重启IDEA



### 2. cloud-consumer-order80

**RestTemplate提供了多种便捷访问远程Http服务的方法，是一种简单便捷的访问restful服务模板类，由Spring提供的用于访问Rest服务的客户端模板工具集**



#### **RestTemplate配置类**

```java
@Configuration
public class ApplicationContextConfig {
    @Bean
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }
}
```



#### Controller业务代码，调用其他provider的dao和service层服务

```java
@RestController
@Slf4j
public class OrderController {

    public static final String PAYMENT_URL = "http://localhost:8001";

    @Resource
    private RestTemplate restTemplate;
    @GetMapping("/consumer/payment/create")
    public CommonResult<Payment> create(Payment payment){
        return restTemplate.postForObject(PAYMENT_URL+"/payment/create",payment,CommonResult.class);
    }
    @GetMapping("/consumer/payment/get/{id}")
    public CommonResult<Payment> getPayment(@PathVariable("id") Long id){
        return restTemplate.getForObject(PAYMENT_URL+"payment/get/"+id,CommonResult.class);
    }

}
```



### 3. 工程重构

**将重复的代码提取出来**，降低耦合



- 观察问题：有重复部分，重构
- 新建工程
- POM文件配置
- 将相同代码放入包中
- maven命令clean,install
- 其他模块删除相同代码部分
- 其他模块改动POM文件

```xml
        <dependency><!-- 引入自己定义的api通用包，可以使用Payment支付Entity -->
            <groupId>com.atguigu.springcloud</groupId>
            <artifactId>cloud-api-commons</artifactId>
            <version>${project.version}</version>
        </dependency>
```



### 4. Eureka服务注册与发现

**服务治理**：在传统的RPC远程调用框架中，管理每个服务与服务之间依赖关系复杂，管理比较复杂，所以需要服务治理，管理服务于服务之间的依赖关系，可以实现服务调用，负载均衡，容错等，实现服务发现与注册

​	各个微服务节点通过配置启动后，都会在EurekaServer中进行注册，这样EurekaServer中的服务注册表中会存储所有可用服务节点的信息

​	**EurekaClient通过注册中心进行访问**

​		是一个java客户端，用于简化交互，在应用启动后，会每隔一段时间（默认30秒）对EurekaServer发送心跳，如果在多个周期内EurekaServer没有接收到某节点的心跳，那么就会自动移除该节点（默认90秒）

#### 配置主启动类

需要添加 `@EnableEurekaServer`注解，表示这是一个服务类

##### yml文件具体配置

```yml
server:
  port: 7001


eureka:
  instance:
    hostname: eureka7001.com #eureka服务端的实例名称
  client:
    register-with-eureka: false     #false表示不向注册中心注册自己。
    fetch-registry: false     #false表示自己端就是注册中心，我的职责就是维护服务实例，并不需要去检索服务
    service-url:
      #集群指向其它eureka
      #defaultZone: http://eureka7002.com:7002/eureka/
      #单机就是7001自己
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
    #server:
    #关闭自我保护机制，保证不可用服务被及时踢除
    #enable-self-preservation: false
    #eviction-interval-timer-in-ms: 2000
```





###### 接下来将服务提供者注册进入Eureka服务中心

首先，进入服务提供者8001工程，改动pom文件，引入eureka-client

```xml
        <!--eureka-client-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
```

在主启动类中加入注解

`@EnableEurekaClient`表示这是一个服务提供者Client，注册进入Eureka服务中心

改动yml文件

```yml
eureka:
  client:
    #表示是否将自己注册进EurekaServer默认为true。
    register-with-eureka: true
    #是否从EurekaServer抓取已有的注册信息，默认为true。单节点无所谓，集群必须设置为true才能配合ribbon使用负载均衡
    fetchRegistry: true
    service-url:
      #单机版
      defaultZone: http://localhost:7001/eureka
      # 集群版
      #defaultZone: http://eureka7001.com:7001/eureka,http://eureka7002.com:7002/eureka
  #instance:
    #instance-id: payment8001
    #访问路径可以显示IP地址
    #prefer-ip-address: true
    #Eureka客户端向服务端发送心跳的时间间隔，单位为秒(默认是30秒)
    #lease-renewal-interval-in-seconds: 1
    #Eureka服务端在收到最后一次心跳后等待时间上限，单位为秒(默认是90秒)，超时将剔除服务
    #lease-expiration-duration-in-seconds: 2
```



将消费者cloud-consumer-order80注册进入服务中心



**引入pom文件**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**改动yml**

```yml
server:
  port: 81
spring:
  application:
    name: cloud-order-service
eureka:
  client:
    #表示是否将自己注册进EurekaServer默认为true。
    register-with-eureka: true
    #是否从EurekaServer抓取已有的注册信息，默认为true。单节点无所谓，集群必须设置为true才能配合ribbon使用负载均衡
    fetchRegistry: true
    service-url:
      #单机
      #defaultZone: http://localhost:7001/eureka
      # 集群
      defaultZone: http://localhost:7001/eureka
```

**在主启动类加入注解**

`@EnableEurekaClient` 注册进入Eureka服务中心





#### Eureka集群原理

**互相注册，相互守望**

##### 建立一个cloud-eureka-server7002

- ​	修改映射配置文件 修改C:\Windows\System32\drivers\etc\host文件 添加映射

  `127.0.0.1 	eureka7001.com
  127.0.0.1	eureka7002.com`

- ​	关键在于写YML文件，以前是单机，现在需要相互注册，有改动

```yml
server:
  port: 7001


eureka:
  instance:
    hostname: eureka7001.com #eureka服务端的实例名称
  client:
    register-with-eureka: false     #false表示不向注册中心注册自己。
    fetch-registry: false     #false表示自己端就是注册中心，我的职责就是维护服务实例，并不需要去检索服务
    service-url:
      #集群指向其它eureka
      #defaultZone: http://eureka7002.com:7002/eureka/
      #单机就是7001自己
      defaultZone: http://eureka7002.com:7002/eureka/
    #server:
    #关闭自我保护机制，保证不可用服务被及时踢除
    #enable-self-preservation: false
    #eviction-interval-timer-in-ms: 2000
```



##### 将consumer-8001也注册进入eureka集群

**修改yml文件即可**	

```yml
  #单机版
  #defaultZone: http://localhost:7001/eureka
  # 集群版
  defaultZone: http://eureka7001.com:7001/eureka,http://eureka7002.com:7002/eureka
#instance:
```

##### order80也类似，修改yml文件即可



注意启动顺序必须得是 **先7001,7002 再 8001 然后80**





##### 支付提供者8001集群环境构建

​	**这里遇到一个巨坑，不能在idea中随意复制粘贴项目文件，如果copy也必须要在windows资源文件夹下进行操作，不然关系会乱！！！**

​	**如果随意复制粘贴，项目会自动引用你复制的那个文件，会报错**



​	**这里我也没有找到什么很好的解决办法，只能把项目重写了一遍，原因可能就是cv大法使得依赖关系错乱了**

​	这里可以使用idea并发同一个程序，只需要书写不同的方法即可

​	这里附上yml代码，用---配置

​	使用下面两行代码分别启动不同端口的server构成集群

```
--spring.profiles.active=eureka7001.com
```

```
--spring.profiles.active=eureka7002.com
```



```yml
server:
  port: 7001
spring:
  profiles: eureka7001.com
  application:
    name: eureka-ha

eureka:
  instance:
    hostname: eureka7001.com #eureka服务端的实例名称
  client:
    register-with-eureka: false     #false表示不向注册中心注册自己。
    fetch-registry: false     #false表示自己端就是注册中心，我的职责就是维护服务实例，并不需要去检索服务
    service-url:
      #集群指向其它eureka
      defaultZone: http://eureka7002.com:7002/eureka/
      #单机就是7001自己
      #defaultZone: http://eureka7001.com:7001/eureka/
    #server:
    #关闭自我保护机制，保证不可用服务被及时踢除
    #enable-self-preservation: false
    #eviction-interval-timer-in-ms: 2000
---
server:
  port: 7002
spring:
  profiles: eureka7002.com
  application:
    name: eureka-ha

eureka:
  instance:
    hostname: eureka7002.com #eureka服务端的实例名称
  client:
    register-with-eureka: false     #false表示不向注册中心注册自己。
    fetch-registry: false     #false表示自己端就是注册中心，我的职责就是维护服务实例，并不需要去检索服务
    service-url:
      #集群指向其它eureka
      defaultZone: http://eureka7001.com:7001/eureka/
```





**顺便一提：出现sql连接超时等情况，一般都是数据库连接出了问题：可以在后面加上** `&&serverTimezone=GMT`

如果出现了别的情况，比如 

```
Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed; nested exception is org.springframework.web.client.HttpClientErrorException$NotFound: 404 : [{"timestamp":"2020-09-18T12:32:02.893+0000","status":404,"error":"Not Found","message":"No message available","path":"/payment/get/31"}]] with root cause
```

那么大概率就是dao/service/controller没有加相应的注解，只需要添加上即可





**在这里有一个idea的并行运行方法**

​	首先，在Edit Configurations中打开**并行运行**选项

​	然后添加参数，在7001的Program argument中加 `--spring.profiles.active=eureka7001.com` 	**这里在同一个yml文件中添加了---隔开，上文有介绍**

​	在8001的VM options中加入 `-Dspring.config.location=classpath:/application2.yml`	**这里是另外加了一个yml文件**





##### 改变（隐藏）主机地址

**显示IP地址配置**

```yml
  instance:
    instance-id: payment8002	#隐藏主机地址
    prefer-ip-address: true		#显示ip地址
```



#### 服务发现Discovery 

对于注册进eureka里面的微服务，可以通过服务发现来获得该服务的信息



修改PaymentMain8001的controller，将微服务暴露给对方

在controller层添加如下方法，有2层遍历

```java
@GetMapping(value = "/payment/discovery")
public Object discovery(){
    List<String> services = discoveryClient.getServices();
    for(String element : services)
    {
        System.out.println("*****element: "+element);
    }
    List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
    for(ServiceInstance instance:instances){
        System.out.println(instance.getServiceId()+"\t"+instance.getHost()+"\t"+instance.getPort()+"\t"+instance.getUri());
    }
    return this.discoveryClient;
}
```

在主启动类添加如下注释

`@EnableDiscoveryClient`



### 5. 使用zookeeper作为注册中心



#### zookeeper概念初步认识

**ZooKeeper 是一个开源的分布式协调服务**

ZooKeeper 是一个典型的分布式数据一致性解决方案，分布式应用程序可以基于 ZooKeeper 实现诸如***数据发布/订阅***、**负载均衡**、**命名服务**、***分布式协调/通知***、**集群管理**、***Master 选举***、***分布式锁***和***分布式队列***等功能。

ZooKeeper 的设计目标是将那些复杂且容易出错的分布式一致性服务封装起来，构成一个高效可靠的原语集，并以一系列简单易用的接口提供给用户使用。

![img](https://img-blog.csdn.net/20180911221613235?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2ppYWhhbzExODY=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

**最好使用奇数台服务器构成zookeeper集群**

​	因为采取的算法规则是半数以上(不包含半数)以上的服务器存活，那么服务继续，也就是说3台服务器最多允许一台挂掉，4台服务器也只允许一台挂掉



- **ZooKeeper 本身就是一个\*分布式程序\***（只要半数以上节点存活，ZooKeeper 就能正常服务）。

- **为了保证\**高可用\**，最好是以集群形态来部署 ZooKeeper，**这样只要集群中大部分机器是可用的（能够容忍一定的机器故障），那么 ZooKeeper 本身仍然是可用的。

- **ZooKeeper 将数据保存在\**内存中\**，**这也就保证了 高吞吐量和低延迟（但是内存限制了能够存储的容量不太大，此限制也是保持 Znode 中存储的数据量较小的进一步原因）。

- **ZooKeeper 是高性能的。**在“读”多于“写”的应用程序中尤其地高性能，因为“写”会导致所有的服务器间同步状态。（“读”多于“写”是协调服务的典型场景。）

- **ZooKeeper 有临时节点的概念。**当创建临时节点的客户端会话一直保持活动，瞬时节点就一直存在。

  而当会话终结时，瞬时节点被删除。持久节点是指一旦这个 ZNode 被创建了，除非主动进行 ZNode 的移除操作，否则这个 ZNode 将一直保存在 Zookeeper 上。

- **ZooKeeper 底层其实只提供了两个功能**：①管理（存储、读取）用户程序提交的数据；②为用户程序提交数据节点监听服务。

#### 支付服务进入进zookeeper

- 创工程

- POM配置

  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  <project xmlns="http://maven.apache.org/POM/4.0.0"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
      <parent>
          <artifactId>cloud2020</artifactId>
          <groupId>com.atguigu.springcloud</groupId>
          <version>1.0-SNAPSHOT</version>
      </parent>
      <modelVersion>4.0.0</modelVersion>
  
      <artifactId>cloud-provider-payment8004</artifactId>
  
  
      <dependencies>
          <!-- SpringBoot整合Web组件 -->
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-web</artifactId>
          </dependency>
          <dependency><!-- 引入自己定义的api通用包，可以使用Payment支付Entity -->
              <groupId>com.atguigu.springcloud</groupId>
              <artifactId>cloud-api-commons</artifactId>
              <version>${project.version}</version>
          </dependency>
          <!-- SpringBoot整合zookeeper客户端 -->
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-zookeeper-discovery</artifactId>
              <!--先排除自带的zookeeper3.5.3-->
              <exclusions>
                  <exclusion>
                      <groupId>org.apache.zookeeper</groupId>
                      <artifactId>zookeeper</artifactId>
                  </exclusion>
              </exclusions>
          </dependency>
          <!--添加zookeeper3.4.9版本-->
          <dependency>
              <groupId>org.apache.zookeeper</groupId>
              <artifactId>zookeeper</artifactId>
              <version>3.4.9</version>
          </dependency>
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-devtools</artifactId>
              <scope>runtime</scope>
              <optional>true</optional>
          </dependency>
          <dependency>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <optional>true</optional>
          </dependency>
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-test</artifactId>
              <scope>test</scope>
          </dependency>
      </dependencies>
  </project>
  ```

- 改YML

  ```yml
  #8004表示注册到zookeeper服务器的支付服务提供者端口号
  server:
    port: 8004
  
  #服务别名----注册zookeeper到注册中心名称
  spring:
    application:
      name: cloud-provider-payment
    cloud:
      zookeeper:
        connect-string: 192.168.119.128:2181
        #connec-string: 是在虚拟机中启动的主机地址:zookeeper端口号
  
  ```

- 基本启动类和controller配置

- 在Linux中通过docker启动zookeeper

  `root@ubuntu:/home/fhawke# docker exec -it 5c8484724b8b zkCli.sh`

  然后通过下图中命令进行测试

![image-20200922224810660](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20200922224810660.png)



**在zookeeper中的服务节点是临时节点**，在心跳检测中不会删除，超过时限后就删除服务





#### 添加订单服务注册consumer-80

**一样的步骤不再赘述**

application.yml文件

```yml
server:
  port: 81

spring:
  application:
    name: cloud-consumer-order
  cloud:
    #注册到zookeeper地址
    zookeeper:
      connect-string: 192.168.119.128:2181
```

**添加Config和Controller**



​	**controller：**

PaymentInfo方法测试：通过这个方法，consumer调用provider的服务方法地址	http://cloud-provider-payment/payment/zk  ，通过restTemplate得到result返回，完成测试过程

```java
package com.atguigu.springcloud.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@RestController
@Slf4j
public class OrderZKController {
    public static final String INVOKE_URL = "http://cloud-provider-payment";
    @Resource
    private RestTemplate restTemplate;
    //注册进入zookeeper
    //能够调用8004

    @GetMapping(value="/consumer/payment/zk")
    public String PaymentInfo(){
        String result = restTemplate.getForObject(INVOKE_URL+"/payment/zk",String.class);
        return result;
    }
}
```

```java

package com.atguigu.springcloud.config;


        import org.springframework.cloud.client.loadbalancer.LoadBalanced;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationContextConfig {
    @Bean
    @LoadBalanced
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }
}
```





### 6. Consul

**Consul是一套开源的分布式服务发现和配置管理系统**

提供了微服务系统中的服务治理，配置中心，控制总线等功能，这些功能中的每一个都可以根据需要单独使用，Consul提供了一种完整的服务网格解决方案

​	它基于raft协议，较为简洁；支持健康检查，同时支持HTTP和DNS协议，支持跨数据中心的WAN集群





#### 通过docker安装Consul

- 简单的docker search consul 和 docker pull consul 不必多说

- `docker run --name consul -d -p 8500:8500 -p 8300:8300 -p 8301:8301 -p 8302:8302 -p 8600:8600 consul agent -server -bootstrap-expect 1 -ui -bind=0.0.0.0 -client=0.0.0.0`

- 然后ifconfig查看主机ip地址 访问查询到的地址(设为ip) 访问 `ip:8500`

- 出现以下界面代表访问成功，安装完成

  ![image-20200923143019999](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20200923143019999.png)

- 这里给出各参数代表意义

```markdown
–net=host docker参数, 使得docker容器越过了net namespace的隔离，免去手动指定端口映射的步骤
-server consul支持以server或client的模式运行, server是服务发现模块的核心, client主要用于转发请求
-advertise 将本机私有IP传递到consul
-retry-join 指定要加入的consul节点地址，失败后会重试, 可多次指定不同的地址
-client 指定consul绑定在哪个client地址上，这个地址可提供HTTP、DNS、RPC等服务，默认是>127.0.0.1
-bind 绑定服务器的ip地址；该地址用来在集群内部的通讯，集群内的所有节点到地址必须是可达的，>默认是0.0.0.0
allow_stale 设置为true则表明可从consul集群的任一server节点获取dns信息, false则表明每次请求都会>经过consul的server leader
-bootstrap-expect 数据中心中预期的服务器数。指定后，Consul将等待指定数量的服务器可用，然后>启动群集。允许自动选举leader，但不能与传统-bootstrap标志一起使用, 需要在server模式下运行。
-data-dir 数据存放的位置，用于持久化保存集群状态
-node 群集中此节点的名称，这在群集中必须是唯一的，默认情况下是节点的主机名。
-config-dir 指定配置文件，当这个目录下有 .json 结尾的文件就会被加载，详细可参考https://www.consul.io/docs/agent/options.html#configuration_files
-enable-script-checks 检查服务是否处于活动状态，类似开启心跳
-datacenter 数据中心名称
-ui 开启ui界面
-join 指定ip, 加入到已有的集群中
```

**注意：**

在**-bootstrap-expect** 后面如果加1，代表单机，如果加2，代表集群

**主要是后面加-client 0.0.0.0，Consul将接受绑定到所有接口的选项**





#### 服务提供者注册进Consul

- 建立module **PaymentMain8006** 

- POM和YML文件修改

  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  <project xmlns="http://maven.apache.org/POM/4.0.0"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
      <parent>
          <artifactId>cloud2020</artifactId>
          <groupId>com.atguigu.springcloud</groupId>
          <version>1.0-SNAPSHOT</version>
      </parent>
      <modelVersion>4.0.0</modelVersion>
  
      <artifactId>cloud-providerconsul-payment8006</artifactId>
  
  
  
  
      <dependencies>
          <!-- 引入自己定义的api通用包，可以使用Payment支付Entity -->
          <dependency>
              <groupId>com.atguigu.springcloud</groupId>
              <artifactId>cloud-api-commons</artifactId>
              <version>${project.version}</version>
          </dependency>
          <!--SpringCloud consul-server -->
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-consul-discovery</artifactId>
          </dependency>
          <!-- SpringBoot整合Web组件 -->
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-web</artifactId>
          </dependency>
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-actuator</artifactId>
          </dependency>
          <!--日常通用jar包配置-->
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-devtools</artifactId>
              <scope>runtime</scope>
              <optional>true</optional>
          </dependency>
          <dependency>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <optional>true</optional>
          </dependency>
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-test</artifactId>
              <scope>test</scope>
          </dependency>
          <dependency>
              <groupId>cn.hutool</groupId>
              <artifactId>hutool-all</artifactId>
              <version>RELEASE</version>
              <scope>test</scope>
          </dependency>
          <dependency>
              <groupId>cn.hutool</groupId>
              <artifactId>hutool-all</artifactId>
              <version>RELEASE</version>
              <scope>test</scope>
          </dependency>
      </dependencies>
  </project>
  
  ```

  

```yml
###consul服务端口号
server:
  port: 8006

spring:
  application:
    name: consul-provider-payment
  ####consul注册中心地址
  cloud:
    consul:
      #虚拟机ip地址
      host: 192.168.119.128
      port: 8500
      discovery:
        #hostname: 127.0.0.1
        service-name: ${spring.application.name}


```

- 然后建立Controller和主启动类进行测试，与8004相同
- 成功界面，看到consul-provider-payment(模块名)已经成功进入了consul
- ![image-20200923144240460](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20200923144240460.png)



#### 服务消费者注册进Consul

- 和zookeeper大同小异，这里给上文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>cloud2020</artifactId>
        <groupId>com.atguigu.springcloud</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>cloud-consumerconsul-order80</artifactId>


    <dependencies>
        <!--SpringCloud consul-server -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-consul-discovery</artifactId>
        </dependency>
        <!-- SpringBoot整合Web组件 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <!--日常通用jar包配置-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```



```yml
###consul服务端口号
server:
  port: 81

spring:
  application:
    name: cloud-consumer-order
  ####consul注册中心地址
  cloud:
    consul:
      host: 192.168.119.128
      port: 8500
      discovery:
        #hostname: 127.0.0.1
        service-name: ${spring.application.name}
```

- Config使用RestTemplate

- controller测试方法

  ```java
  @RestController
  @Slf4j
  public class OrderConsulController {
      public static final String INVOKE_URL = "http://consul-provider-payment";
      @Resource
      private RestTemplate restTemplate;
      //consul
      //8006
  
      @GetMapping(value="/consumer/payment/consul")
      public String PaymentInfo(){
          String result = restTemplate.getForObject(INVOKE_URL+"/payment/consul",String.class);
          return result;
      }
  }
  ```

- 验证目的：消费者既被注册进了consul，又可以输入 `http://localhost:81/consumer/payment/consul`来调用服务注册者





#### 三个注册中心的异同(eureka，zookeeper，consul)

| 组件名    | 语言 | CAP  | 服务健康检查 | 对外暴露接口 | Spring Cloud集成 |
| --------- | ---- | ---- | ------------ | ------------ | ---------------- |
| Eureka    | Java | AP   | 可配支持     | HTTP         | 已集成           |
| Consul    | Go   | CP   | 支持         | HTTP/DNS     | 已集成           |
| Zookeeper | Java | CP   | 支持         | 客户端       | 已集成           |

**CAP**：CAP原则又称CAP定理，指的是在一个分布式系统中，[一致性](https://baike.baidu.com/item/一致性/9840083)（Consistency）、[可用性](https://baike.baidu.com/item/可用性/109628)（Availability）、分区容错性（Partition tolerance）。CAP 原则指的是，这三个要素最多只能同时实现两点，不可能三者兼顾。

<img src="https://bkimg.cdn.bcebos.com/pic/5bafa40f4bfbfbed9c15b19b72f0f736aec31f81?x-bce-process=image/resize,m_lfit,w_268,limit_1/format,f_jpg" alt="img" style="zoom:200%;" />



**AP架构**：当网络分区出现后，为了保证可用性，第二个系统可以返回旧值，保证系统的可用性

**CP架构**：当网络分区出现后，为了保证一致性，就必须拒绝请求，否则无法保证一致性





### 7. Ribbon

**Ribbon负载均衡服务调用**

- Spring Cloud Ribbon是基于Netflix Ribbon实现的一套 **客户端负载均衡**的工具 (Order80)
- 简单来说，Ribbon的主要功能是提供**客户端的软件负载均衡算法和服务调用**
- Ribbon客户端组件提供一系列完善的配置项如连接超时，重试等
- 简单来说，就是在配置文件中列出Load Banlancer后面所有的及其，Ribbon会自动的帮助你基于某种规则(**简单轮询，随机连接**)去连接这些机器，我们很容易使用Ribbon实现自定义的负载均衡算法
- Ribbon实际上就是**负载均衡+RestTemplate**调用



**负载均衡是什么？**

​	简单来说就是将用户的请求平摊的分配到多个服务上，从而达到系统的HA(**高可用**)，常用的负载均衡有软件Nginx,LVS,硬件 F5等



**Ribbon本地负载客户端 VS Nginx服务端负载均衡区别**

- Nginx是服务器负载均衡，客户端所有请求都会交给nginx，然后由nginx实现转发请求，即负载均衡是由服务端实现的

​	**（集中式LB）: 即在服务的消费方和提供方之间使用独立的LB设施，由该设施负责把访问请求通过某种策略转发至服务的提供方**



- Ribbon本地负载均衡，在调用微服务接口的时候，会在注册中心上获取注册信息服务列表之后缓存在JVM本地，从而在本地实现RPC远程服务调用技术 

​	**（进程内LB）：把LB逻辑集成到消费方，消费方从服务注册中心获知有哪些地址可用，然后自己再从这些地址选择出一个合适的服务器**



<font color='red'>通俗来说，nginx会把所有人的请求postmapping发到nginx，nginx来对这些请求发到哪些服务器进行负载均衡 </font>

<font color='red'>ribbon是A服务要调用B服务，B服务有三个提供者，A会把这三个提供者缓存到jvm里，进行一个远程调用</font>

<font color='red'>一个是服务器端，一个是客户端</font>

<font color='red'>nginx是外边对服务器的调用，ribbon是微服务之间的相互调用</font>

#### 

#### Ribbon负载均衡演示

**Ribbon其实就是一个软负载均衡的客户端组件，他可以和其他所需请求的客户端结合使用，和eureka结合只是其中的一个实例**

**所谓软负载均衡：就是消费者自己集成了Ribbon组件，对于负载均衡请求可以自己查询可用服务列表**



Ribbon工作分2步：

- 第一步先选择EurekaServer，它优先选择在同一个区域内负载较少的server
- 第二部再根据用户指定的策略，在从server取到的服务注册列表中选择一个地址，然后调用
- 其中Ribbon提供了多种策略：比如轮询，随机和根据响应时间加权



**在最新的pom文件中，即使你没有引用Ribbon也可以使用，因为下面的eureka已经和Ribbon进行了整合**

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
```

![image-20200923160400076](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20200923160400076.png)



**图中蓝色框就是Ribbon的引入，可见eureka已经整合了Ribbon**



#### Ribbon默认自带的负载规则

**IRule**：根据特定算法中从服务列表中选取一个要访问的服务

默认自带：

- 轮询
- 随机
- 先按照轮询策略获取服务，如果获取服务失败则在指定时间内重试，获取可用的服务
- 对轮询的扩展，响应速度越快的实例选择权重越大，越容易被选择
- 会先过滤掉由于多次访问故障而处于断路器跳闸状态的服务，然后选择一个并发量小的服务
- 先过滤掉故障实例，再选择并发较小的实例
- ZoneAvoidanceRule 默认规则，复合判断server所在区域的性能和server的可用性选择服务器



#### 如何替换负载规则

- 修改cloud-consumer-order80

- <font color='red'>警告!</font> 修改负载规则的自定义配置类不能放在**@ComponentScan**所扫描的当前包下以及子包下，否则我们自定义的这个配置类就会被**所有Ribbon客户端**所共享，无法达到特殊化的目的 **@SpringbootApplication同理**

- 在java下另建一个包myrule，建立类MySelfRule

  ```java
  
  @Configuration
  public class MySelfRule {
      @Bean
      public IRule myRule(){
          return new RandomRule();    //定义为随机
      }
  }
  
  ```

- 在主启动类中添加RibbonClient

  `@RibbonClient(name = "CLOUD-PAYMENT-SERVICE",configuration = MySelfRule.class)`



#### Ribbon负载均衡算法

- 原理：rest接口第几次请求数 % 服务器集群总数量 = 实际调用服务器位置下标 ，每次服务重启动后rest接口技术从1开始





#### 手写负载算法

**原理+JUC（CAS + 自旋锁复习）**

- 首先注释掉消费者80config配置类中的@LoadBalanced
- 然后添加lb包，保证lb包能被扫描到，在MyLB类中添加@Component注解
- 定义LoadBalancer接口，让MyLB实现它
- 以下就是MyLB类对于轮询算法的实现

```java

@Component
public class MyLB implements LoadBalancer {
    //原子类
    private AtomicInteger atomicInteger = new AtomicInteger(0);

    public final int getAndIncrement(){
        int current;
        int next;
        do{
            current = this.atomicInteger.get();
            next = current >= 2147483647 ? 0:current+1;
            //2147483647是整形的最大值
        }while(!this.atomicInteger.compareAndSet(current,next));    //自旋锁
        System.out.println("******next:  "+next);
        return next;
    }

    @Override
    public ServiceInstance instances(List<ServiceInstance> serviceInstances) {
                    //第几次访问 % 集群服务器总数量 = 实际调用服务器位置下标
        int index = getAndIncrement() % serviceInstances.size();
        return serviceInstances.get(index);
    }
}

```

- 编写测试类进行测试(消费者80)

```java
 //定义自己的轮询算法启动
    @GetMapping(value="/consumer/payment/lb")
    public String getPaymentLB(){
        List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
        if(instances == null||instances.size() <= 0){
            return null;
        }else{
            ServiceInstance serviceInstance = loadBalancer.instances(instances);
            URI uri = serviceInstance.getUri();
            return restTemplate.getForObject(uri+"payment/lb",String.class);
        }
    }
```

- 在8001的Controller层加入测试方法

  ```java
  @GetMapping(value="/payment/lb")
      public String getPaymentLB(){
          return serverPort;
      }
  ```

  

实际上80测试类调用的就是8001的方法，也就是说消费者调用了服务提供者

 

### 8. OpenFeign

- Feign是一个声明式WebService客户端，使用Feign能让编写Web Service客户端更简单
- 他的使用方法是定义一个服务接口然后在上面添加注解，Feign也支持可拔插式的编码器和解码器，Feign可以与Eureka和Ribbon组合使用以支持负载均衡
- Feign也整合了Ribbon，所以也具有轮询功能

#### OpenFeign使用步骤

- 接口+注解	@FeignClient
- 项目创建     	
  - feign用在消费端  
  - @EnableFeignClients 使用并激活Feign

```yml
server:
  port: 81

eureka:
  client:
    register-with-eureka: false
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/,http://eureka7002.com:7002/eureka/

```

```xml
<!--openfeign-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

- Controller层调用Service层

  ```java
  @RestController
  @Slf4j
  public class OrderFeignController {
  
      @Resource
      private PaymentFeignService paymentFeignService;
  
      @GetMapping(value="/consumer/payment/get/{id}")
      public CommonResult<Payment> getPaymentById(@PathVariable("id") Long id){
          return paymentFeignService.getPaymentById(id);
      }
  
  }
  ```

- Service层通过**@FeignClient**注解查询服务提供者，然后根据GetMapping的参数**调用服务提供者的方法**

```java
@Component
@FeignClient(value="CLOUD-PAYMENT-SERVICE")
public interface PaymentFeignService {
    @GetMapping(value = "/payment/get/{id}")
    public CommonResult<Payment> getPaymentById(@PathVariable("id") Long id);
}
```

- 测试 `http://localhost:81/consumer/payment/get/31`

  发现是**轮询**，由此可见Feign实现了**负载均衡**



#### OpenFeign超时控制

```yml
#设置feign客户端超时时间(OpenFeign默认支持ribbon)
ribbon:
  #指的是建立连接所用的时间，适用于网络状况正常的情况下,两端连接所用的时间
  ReadTimeout: 5000
  #指的是建立连接后从服务器读取到可用资源所用的时间
  ConnectTimeout: 5000
```



#### OpenFeign日志增强

```yml
logging:
  level:
    # feign日志以什么级别监控哪个接口
    com.atguigu.springcloud.service.PaymentFeignService: debug
```

在Config类中加一个配置类

```java
@Configuration
public class FeignConfig {
    @Bean
    Logger.Level feignLoggerLevel(){
        return Logger.Level.FULL;
    }
}
```

### 9. Hystrix断路器

- **服务雪崩**：服务雪崩效应是一种因“服务提供者的不可用”（原因）导致“服务调用者不可用”（结果），并将不可用逐渐放大的现象
  - 对于高流量的应用来说，单一的后端依赖可能会导致所有服务器上的所有资源都在几秒钟内饱和，比失败更糟糕的是，这些应用程序还可能导致服务之间的延迟增加，备份队列，线程和其他系统资源紧张，导致整个系统发生更多的级联故障。这些都表示需要对故障和延迟进行隔离和管理，以便单个依赖关系的失败，不能取消整个应用系统



- **Hystrix是一个用于处理分布式系统的延迟和容错的开源库**，在分布式系统里，许多依赖不可避免的会调用失败，比如超时/异常 等等，Hystrix能够保证在一个依赖出问题的情况下，不会导致整体服务失败，避免级联故障，以提高分布式系统的弹性
  - 服务降级
    - 程序运行异常
    - 超时
    - 服务熔断出发服务降级
    - 线程池/信号量打满也会导致服务降级
  - 服务熔断
    - 类比保险丝达到最大服务访问后，直接拒绝访问，拉闸限电，然后调用服务降级的方法并返回友好提示
  - 接近实时的监控
  - 服务限流
    - 秒杀高并发等操作，有序进行

#### Hystrix支付微服务搭建

- 基本的建module然后搭配

```xml
        <!--hystrix-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
        </dependency>
```

```yml
server:
  port: 8001

spring:
  application:
    name: cloud-provider-hystrix-payment

eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka,http://eureka7002.com:7002/eureka
      #defaultZone: http://eureka7001.com:7001/eureka
```

- Controller层

```java
@RestController
@Slf4j
public class PaymentController {
    @Resource
    private PaymentService paymentService;
    @Value("${server.port}")
    private String serverPort;

    @GetMapping("payment/hystrix/ok/{id}")
    public String paymentInfo_OK(@PathVariable("id") Integer id){
        String result = paymentService.paymentInfo_OK(id);
        log.info("*****result"+result);
        return result;
    }

    @GetMapping("payment/hystrix/timeout/{id}")
    public String paymentInfo_Timeout(@PathVariable("id") Integer id){
        String result = paymentService.paymentInfo_Timeout(id);
        log.info("*****result"+result);
        return result;
    }
}
```

- 调用Service层

```java
@Service
public class PaymentService {
    /**
     * 正常访问
     * @param id
     * @return
     */
    public String paymentInfo_OK(Integer id){
        return "线程池： "+Thread.currentThread().getName()+"       paymentInfo_OK,id:   "+id + "\t" + "O(∩_∩)O";
    }
    public String paymentInfo_Timeout(Integer id){
        int timeNumber = 3;
        try{
            TimeUnit.SECONDS.sleep(timeNumber);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return "线程池： "+Thread.currentThread().getName()+"       paymentInfo_Timeout,id:   "+id + "\t" + "O(∩_∩)O" + "耗时：" +timeNumber;
    }

}
```



#### JMeter高并发压测后卡顿

- 开启Jmeter，来20000个请求同时访问paymentinfo_Timeout
- 这时访问页面出现卡顿，为什么ok路径也会被卡死呢，因为tomcat要分出很多压力去处理timeout路径，也就没有余力去处理ok，所以ok响应速度也会变慢



#### 80消费者测试加入，进一步加大并发量

- 建立消费者80
- 改动POM和YML文件

```yml
server:
  port: 81

eureka:
  client:
    register-with-eureka: false
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/

#feign:
#  hystrix:
#    enabled: true
ribbon:
  #指的是建立连接所用的时间，适用于网络状况正常的情况下,两端连接所用的时间
  ReadTimeout: 5000
  #指的是建立连接后从服务器读取到可用资源所用的时间
  ConnectTimeout: 5000
```



- Service层调用**cloud-provider-hystrix-payment**的service方法

```java
@Component
@FeignClient(value = "CLOUD-PROVIDER-HYSTRIX-PAYMENT")
public interface PaymentHystrixService {
    @GetMapping("payment/hystrix/ok/{id}")
    public String paymentInfo_OK(@PathVariable("id") Integer id);
    @GetMapping("payment/hystrix/timeout/{id}")
    public String paymentInfo_Timeout(@PathVariable("id") Integer id);
}
```

- controller层调用service层方法

```java
@RestController
@Slf4j
public class OrderHystrixController {
    @Resource
    private PaymentHystrixService paymentHystrixService;

    @GetMapping("/consumer/payment/hystrix/ok/{id}")
    public String paymentInfo_OK(@PathVariable("id") Integer id){
        String result = paymentHystrixService.paymentInfo_OK(id);
        return result;
    }
    @GetMapping("/consumer/payment/hystrix/timeout/{id}")
    public String paymentInfo_Timeout(@PathVariable("id") Integer id){
        String result = paymentHystrixService.paymentInfo_Timeout(id);
        return result;
    }
}
```

- 经过测试，卡顿进一步增加



#### 降级容错解决的维度要求

- 超时不再等待
- 出错要有最终处理措施
- 解决
  - 对方服务（8001）超时了，调用者（80）不能一直卡死等待，必须要有服务降级
  - 对方服务（8001）down机了，调用者（80）不能一直卡死等待，必须有服务降级
  - 对方服务（8001）ok，调用者（80）自己出故障或有自我要求（自己的等待时间小于服务提供者），自己处理降级





#### 服务降级

- 降级配置 ： @HystrixCommand

- 8001自身配置

  - **设置自身调用超时时间的峰值**，峰值内可以正常运行

  - 超过了需要有方法处理，作服务降级fallback

  - 一旦调用服务方法失败并抛出了错误信息后，会自动调用@HystrixCommand标注好的fallbackMethod调用类中的指定方法

  - ```
    @HystrixCommand(fallbackMethod = "paymentInfo_TimeoutHandler",commandProperties = {
                @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds",value="3000")
        })
    ```

  - 主启动类激活：添加新注解**@EnableCircuitBreaker**

- 80fallback

  - ```yml
    feign:
      hystrix:
        enabled: true
    ```

- 主启动类添加 ： `@EnableHystrix`

- 在controller层直接加方法进行测试，修改超时参数测试

```java
    @GetMapping("/consumer/payment/hystrix/timeout/{id}")
    @HystrixCommand(fallbackMethod = "paymentInfo_TimeoutHandler",commandProperties = {
            @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds",value="1500")
    })
    public String paymentInfo_Timeout(Integer id){
        int timeNumber = 3;
        //int age = 10 / 0;
        try{
            TimeUnit.SECONDS.sleep(timeNumber);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return "线程池： "+Thread.currentThread().getName()+"       paymentInfo_Timeout,id:   "+id + "\t" + "O(∩_∩)O" + "耗时：" +timeNumber;
    }

    public String paymentInfo_TimeoutHandler(Integer id){
        return "线程池： "+Thread.currentThread().getName()+"      我是消费者80，请稍后再试,id:   "+id + "\t" + "/(ㄒoㄒ)/~~";
    }
}
```



- 目前问题：每个业务对应一个兜底处理方法，代码膨胀

  - 每个方法配置一个-----膨胀

    - @DefaultProperties(defaultFallback = "  ")

      - 全局配置：`@DefaultProperties(defaultFallback = "payment_Global_FallbackMethod")`

      - 注释掉配置：

        ```
        //@HystrixCommand(fallbackMethod = "paymentTimeOutFallbackMethod",commandProperties = {
        //            @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds",value="1500")
        //    })
        ```

    ```java
    //下面是全局fallback方法
    public String payment_Global_FallbackMethod(){
        return "Global异常处理信息，请稍后再试,(●'◡'●)";
    }
    ```

  - 和业务逻辑混在一起-----混乱

    - 首先明确，controller层里面的方法调用的是Service层里的接口方法

    - 那么我们对Service接口内的所有方法增加一个处理（添加统一的服务降级类）

    - 那么解决方法就是：根据Service层的接口，重新定义一个类**PaymentFallbackService**实现该接口，统一为接口里面的方法进行异常处理

    - ```yml
      feign:
        hystrix:
          enabled: true
      ```

    - **PaymentFallbackService**类：

    - ```java
      @Component
      public class PaymentFallbackService implements PaymentHystrixService{
      
          @Override
          public String paymentInfo_OK(Integer id) {
              return "-----PaymentFallbackService fall back,O(∩_∩)O";
          }
      
          @Override
          public String paymentInfo_Timeout(Integer id) {
              return "-----PaymentFallbackService  paymentInfo_Timeout fall back,O(∩_∩)O";
          }
      }
      ```

    - Service注解改动

    - ```java
      @FeignClient(value = "CLOUD-PROVIDER-HYSTRIX-PAYMENT",fallback = PaymentFallbackService.class)
      ```

    - 测试：打开7001，8001，80
    - 首先 `http://localhost:81/consumer/payment/hystrix/ok/31`访问正常
    - 然后关闭8001，因为80调用的是8001的方法，也就是消费者调用服务器，模拟服务器宕机
    - `-----PaymentFallbackService fall back,O(∩_∩)O`出现自己自定义的字符，说明服务降级处理生效，让客户端在服务端不可用时也会获得提示信息而不会挂起耗死服务器
    - 如此一来，业务逻辑也就和全局处理分开

#### 服务熔断

- 熔断机制是对应雪崩效应的一种微服务链路保护机制。当扇出链路的某个微服务出错不可用或者响应时间太长时，会进行服务的降级，进而熔断该节点微服务的调用，快速返回错误的响应信息。
- 当检测到该微服务调用响应正常后，恢复调用链路
- 在Spring Cloud框架里，熔断机制是通过Hystrix实现，Hystrix会监控微服务间的调用情况，当失败的调用到达一定的阈值，缺省是5秒内20次调用失败，就会启动熔断机制。熔断机制的注解是@HystrixCommand



- 测试：

  - 首先在service中添加代码，@HystrixCommand开启熔断，fallbackMethod指定熔断方法名称

  ```java
  /**
   * 服务熔断
   */
  @HystrixCommand(fallbackMethod = "paymentCircuitBreaker_fallback",commandProperties = {
          @HystrixProperty(name = "circuitBreaker.enabled",value = "true"),// 是否开启断路器
          @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold",value = "10"),// 请求次数
          @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds",value = "10000"), // 时间窗口期
          @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage",value = "60"),// 失败率达到多少后跳闸
  })
  public String paymentCircuitBreaker(@PathVariable("id") Integer id)
  {
      if(id < 0)
      {
          throw new RuntimeException("******id 不能负数");
      }
      String serialNumber = IdUtil.simpleUUID();
  
      return Thread.currentThread().getName()+"\t"+"调用成功，流水号: " + serialNumber;
  }
  public String paymentCircuitBreaker_fallback(@PathVariable("id") Integer id)
  {
      return "id 不能负数，请稍后再试，/(ㄒoㄒ)/~~   id: " +id;
  }
  ```

  - 然后在controller中添加代码，调用service中的方法

  ```java
  /**
   *  服务熔断
   */
  @GetMapping("/payment/circuit/{id}")
  public String paymentCircuitBreaker(@PathVariable("id") Integer id)
  {
      String result = paymentService.paymentCircuitBreaker(id);
      log.info("****result: "+result);
      return result;
  }
  ```

- **重点**：多次错误，慢慢正确，发现刚开始不满足条件，就算是正确的访问地址也会调用降级方法，这时表示熔断服务已经开启，需要达到一定的正确率才会恢复服务



##### 熔断原理总结：

**熔断类型：**

- 熔断打开：请求不再调用服务，内部设置时钟一般为MTTR（平均故障处理时间），当打开时长到所设时钟便进入半熔断状态
- 熔断关闭：熔断关闭不会对服务进行熔断
- 熔断半开：部分请求根据规则调用当前服务，如果请求成功且符合规则则认为当前服务恢复正常，关闭熔断



服务熔断工作：

- 再有请求调用时，将不会调用主逻辑，而是直接调用降级fallback，通过断路器，实现了自动地发现错误并将降级逻辑切换为主逻辑，减少响应延迟的效果
- 原来的主逻辑恢复：hystrix实现了自动恢复功能
  - 当断路器打开，对主逻辑进行熔断之后，hystrix会启动一个休眠时间窗，在这个时间窗内，降级逻辑是临时的成为主逻辑，当休眠时间窗到期，断路器将进入半开状态，释放一次请求到原来的主逻辑上，如果此次请求正常返回，那么断路器继续闭合，主逻辑恢复，如果这次请求依然有问题，断路器继续进入打开状态，休眠时间窗重新开始计时



#### Hystrix图形化Dashboard搭建

- 建立module，改动YML和POM文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>cloud2020</artifactId>
        <groupId>com.atguigu.springcloud</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>cloud-consumer-hystrix-dashboard9001</artifactId>


    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>


</project>
```

```yml
server:
  port: 9001
```

- 主启动类添加注解

```java
@SpringBootApplication
@EnableHystrixDashboard
```

- 所有Provider微服务提供类(8001/8002/8003)都需要监控依赖配置

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

- 开启9001，访问 http://localhost:9001/hystrix

- 8001修改启动类

```java
/**
 *此配置是为了服务监控而配置，与服务容错本身无关，springcloud升级后的坑
 *ServletRegistrationBean因为springboot的默认路径不是"/hystrix.stream"，
 *只要在自己的项目里配置上下面的servlet就可以了
 */
@Bean
public ServletRegistrationBean getServlet() {
    HystrixMetricsStreamServlet streamServlet = new HystrixMetricsStreamServlet();
    ServletRegistrationBean registrationBean = new ServletRegistrationBean(streamServlet);
    registrationBean.setLoadOnStartup(1);
    registrationBean.addUrlMappings("/hystrix.stream");
    registrationBean.setName("HystrixMetricsStreamServlet");
    return registrationBean;
}
```
- 开始测试
  - http://localhost:9001/hystrix 监控  http://localhost:8001/hystrix.stream
  - 访问 http://localhost:8001/payment/circuit/31 和 http://localhost:8001/payment/circuit/-31
  - 观察曲线和 **Circuit: OPEN/CLOSE**



### 10. GateWay

- Gateway是在Spring生态系统上构建的API网关服务，基于Spring5，Springboot2和Project Reactor等技术
- Gateway旨在提供一种简单而有效的方式来对API进行路由，以及提供一些强大的过滤器功能，例如：熔断，限流，重试等
- SpringCloud Gateway是基于**WebFlux**框架实现的，而WebFlux框架底层则使用了高性能的Reactor模式通信框架 **Netty**



**三大核心概念**

- Route(路由)
  - 路由是构建网关的基本模块，他由ID，目标URI，一系列的断言和过滤器组成，如果断言为true则匹配该路由
- Predicate(断言)
  - 参考java8中的java.util.function.Predicate
  - 开发人员可以匹配HTTP请求中的所有内容（例如请求头或请求参数），如果请求与断言相匹配则进行路由
- Filter(过滤)
  - 指的是Spring框架中GatewayFilter的实例，使用过滤器，可以在请求被路由前或者之后对请求进行修改



web请求，通过一些匹配条件，定位到真正的服务节点。并在这个转发过程的前后，进行一些精细化控制。

predicate就是**匹配条件**；

而filter，可以理解为一个无所不能的**拦截器**。有这两个元素加上目标URI，就可以实现一个具体的路由

匹配方式就是**断言**，实现这个匹配方式就叫filter，对外表现出来就是路由的功能



**Gateway工作流程：**

- 客户端向 Spring Cloud GateWay 发出请求，然后再Gateway Handler Mapping 中找到与请求相匹配的路由，将其发送到 Gateway Web Handler
- Handler 再通过指定的过滤器链来将请求发送到我们实际的服务执行业务逻辑，然后返回
- 过滤器之间用虚线分开是因为过滤器可能会在发送代理请求之前("pre") 或 之后("post")执行业务逻辑
- Filter在 "pre" 类型的过滤器可以做参数检验，权限校验，流量监控，日志输出，协议转换等
- Filter在 "post"类型的过滤器中可以做响应内容，响应头的修改，日志的输出，流量监控等有着非常重要的作用



**核心逻辑：路由转发+执行过滤器链**



#### Gateway9527搭建

- 建立module，修改pom和yml文件

```yml
server:
  port: 9528

spring:
  application:
    name: cloud-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true #开启从注册中心动态创建路由的功能，利用微服务名进行路由
      routes:
        - id: payment_routh #payment_route    #路由的ID，没有固定规则但要求唯一，建议配合服务名
          #uri: http://localhost:8001          #匹配后提供服务的路由地址
          uri: lb://cloud-payment-service 		#匹配后提供服务的路由地址
          predicates:
            - Path=/payment/get/**         # 断言，路径相匹配的进行路由

        - id: payment_routh2 #payment_route    #路由的ID，没有固定规则但要求唯一，建议配合服务名
          #uri: http://localhost:8001          #匹配后提供服务的路由地址
          uri: lb://cloud-payment-service #匹配后提供服务的路由地址
          predicates:
            - Path=/payment/lb/**         # 断言，路径相匹配的进行路由
            #- After=2020-02-21T15:51:37.485+08:00[Asia/Shanghai]
            #- Cookie=username,zzyy
            #- Header=X-Request-Id, \d+  # 请求头要有X-Request-Id属性并且值为整数的正则表达式

eureka:
  instance:
    hostname: cloud-gateway-service
  client: #服务提供者provider注册进eureka服务列表内
    service-url:
      register-with-eureka: true
      fetch-registry: true
      defaultZone: http://eureka7001.com:7001/eureka
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>cloud2020</artifactId>
        <groupId>com.atguigu.springcloud</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>cloud-gateway-gateway9527</artifactId>
    <dependencies>
        <!--gateway-->
        <!-- https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-gateway -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
            <version>2.2.5.RELEASE</version>
        </dependency>

        <!--eureka-client-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <!-- 引入自己定义的api通用包，可以使用Payment支付Entity -->
        <dependency>
            <groupId>com.atguigu.springcloud</groupId>
            <artifactId>cloud-api-commons</artifactId>
            <version>${project.version}</version>
        </dependency>
        <!--一般基础配置类-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

- 然后打开7001，8001，9527，测试发现本来需要访问 `http://localhost:8001/payment/get/31` 才能查询得到，现在通过访问

`http://localhost:9528/payment/get/31`也可以得到，可以知道通过**添加网关**隐藏了端口号



- 创建Config包下的类，使用编码的方式实现转发
- 配置一个id为route-name的路由规则
- 当访问地址http:localhost:9528/guonei时会自动转发到地址: `http://news.baidu.com/guonei`

```java
@Configuration
public class GateWayConfig {
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder routeLocatorBuilder){
        RouteLocatorBuilder.Builder routes = routeLocatorBuilder.routes();
        routes.route("path_route_atguigu",
                r -> r.path("/guonei")
                        .uri("http://news.baidu.com/guonei")).build();
        return routes.build();
    }
}
```



#### GateWay配置动态路由

- 默认情况下GateWay会根据注册中心注册的服务列表，以注册中心上的微服务名为路径创建**动态路由进行转发，从而实现动态路由的功能**
- yml文件改动即可

```yml
gateway:
  discovery:
    locator:
      enabled: true #开启从注册中心动态创建路由的功能，利用微服务名进行路由
  routes:
    - id: payment_routh #payment_route    #路由的ID，没有固定规则但要求唯一，建议配合服务名
      #uri: http://localhost:8001          #匹配后提供服务的路由地址
      uri: lb://cloud-payment-service #匹配后提供服务的路由地址
      predicates:
        - Path=/payment/get/**         # 断言，路径相匹配的进行路由

    - id: payment_routh2 #payment_route    #路由的ID，没有固定规则但要求唯一，建议配合服务名
      #uri: http://localhost:8001          #匹配后提供服务的路由地址
      uri: lb://cloud-payment-service #匹配后提供服务的路由地址
      predicates:
        - Path=/payment/lb/**         # 断言，路径相匹配的进行路由
        #- After=2020-02-21T15:51:37.485+08:00[Asia/Shanghai]
        #- Cookie=username,zzyy
        #- Header=X-Request-Id, \d+  # 请求头要有X-Request-Id属性并且值为整数的正则表达式
```



#### Predicate

- 在yml上加入各种参数即可，类似于cookie,between,after,before,head

#### Filter

- 生命周期
  - pre
  - post
- 种类
  - GatewayFilter
  - GlobalFilter
- 配置filter类，自定义filter
- 实现2个主要接口 **GlobalFilter, Ordered** 

```java
@Component
@Slf4j
public class MyLogGateWayFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("******come in MyLogGateWayFilter:  "+new Date());
        String uname = exchange.getRequest().getQueryParams().getFirst("uname");
        if(uname == null)
        {
            log.info("****username == NULL ,illeagl user");
            exchange.getResponse().setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```



### 11. Config

#### 概念：

- 微服务意味着要将单体应用中的业务拆分成一个个子服务，每个服务的粒度相对较小，因此系统中会出现大量的服务。由于每个服务都需要必要的配置信息才能运行，所以一套集中的，动态的配置管理设施是必不可少
- SpringCloud提供了ConfigServer来解决这个问题，我们每个微服务都自己带有一个application.yml，上百个配置文件的管理
- SpringCloud Config为微服务架构中的微服务提供集中式的外部配置支持，配置服务器为**各个不同的微服务应用**的所有环境提供了一个**中心化的外部配置**
- SpringCloud分为 **服务端和客户端两部分**
  - 服务端也称分布式配置中心，它是一个独立的微服务应用，用来连接配置服务器并为客户端提供获取配置信息，加密/解密信息等访问接口
  - 客户端则是通过指定的配置中心来管理应用资源，以及与业务相关的配置内容，并在启动的时候从配置中心获取和加载配置信息，配置服务器默认采取git来存储配置信息，这样就有助于对环境配置进行版本管理，并且可以通过git客户端工具来方便的管理和访问配置内容。



#### 建立工程 **cloud-config-center-3344**

- 改动pom,yml文件

```yml
server:
  port: 3344

spring:
  application:
    name:  cloud-config-center #注册进Eureka服务器的微服务名
  cloud:
    config:
      server:
        git:
          uri: https://github.com/fhawke/springcloud-config.git #GitHub上面的git仓库名字
          ####搜索目录
          search-paths:
            - springcloud-config
          username: 7142220@qq.com
          password: 20010521lsH
      ####读取分支
      label: master
#rabbitmq相关配置
#rabbitmq:
#  host: localhost
#  port: 5672
#  username: guest
#  password: guest

#服务注册到eureka地址
eureka:
  client:
    service-url:
      defaultZone: http://localhost:7001/eureka

##rabbitmq相关配置,暴露bus刷新配置的端点
#management:
#  endpoints: #暴露bus刷新配置的端点
#    web:
#      exposure:
#        include: 'bus-refresh'
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>cloud2020</artifactId>
        <groupId>com.atguigu.springcloud</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>cloud-config-center-3344</artifactId>
    <dependencies>
        <!--添加消息总线RabbitMQ支持-->
        <!-- https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-bus-amqp -->
        <!-- https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-bus-amqp -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-bus-amqp</artifactId>
            <version>2.2.3.RELEASE</version>
        </dependency>


        <!-- https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-config-server -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-config-server</artifactId>
            <version>2.2.0.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

然后访问 `http://config-3344.com:3344/master/config-dev.yml`就可以得到github目录下master分支的config-dev.yml文件

![image-20201003212223708](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201003212223708.png)





#### Config客户端 client

- 建立项目，改动pom，yml文件，这里yml文件是 **bootstrap.yml**
- **bootstrap.yml**是系统级的，优先级更高
  - SpringCloud会创建一个 " Bootstrap Context "，作为Spring应用的"Application Context"的父上下文，初始化的时候，Bootstrap Context负责从外部源加载配置属性并解析配置，这两个上下文共享一个从外部获取的 "Environment"
  - `Bootstrap Context` 属性有高优先级，默认情况下，他们不会被本地配置覆盖

```yml
server:
  port: 3355

spring:
  application:
    name: config-client
  cloud:
    #Config客户端配置
    config:
      label: master #分支名称
      name: config #配置文件名称
      profile: dev #读取后缀名称   上述3个综合：master分支上config-dev.yml的配置文件被读取http://config-3344.com:3344/master/config-dev.yml
      uri: http://localhost:3344 #配置中心地址k
      username: 7142220@qq.com
      password: 20010521lsH

  #rabbitmq相关配置 15672是Web管理界面的端口；5672是MQ访问的端口
#  rabbitmq:
#    host: localhost
#    port: 5672
#    username: guest
#    password: guest

#服务注册到eureka地址
eureka:
  client:
    service-url:
      defaultZone: http://localhost:7001/eureka

# 暴露监控端点
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>cloud2020</artifactId>
        <groupId>com.atguigu.springcloud</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>cloud-config-client-3355</artifactId>


    <dependencies>
        <!--添加消息总线RabbitMQ支持-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-bus-amqp</artifactId>
            <version>2.2.3.RELEASE</version>
        </dependency>


        <!-- https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-config -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
            <version>2.2.5.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>


</project>
```

- Controller类作测试

```java
@RestController
public class ConfigClientController {
    @Value("${config.info}")
    private String configInfo;

    @GetMapping("/configInfo")
    public String getConfigInfo(){
        return configInfo;
    }
}
```

- 测试通过，可以通过3344访问到github页面

![image-20201003220333271](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201003220333271.png)



#### 分布式配置的动态刷新问题

- ```
  加入@RefreshScope注解，开启刷新
  ```

- yml文件添加

```yml
# 暴露监控端点
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

- 每次从github上修改后都需要发送post请求,同样麻烦，如何处理？
- **下节BUS消息队列**



### 12. Bus消息队列

**Spring Cloud Bus 配合Spring Cloud Config 使用可以实现配置的动态刷新**

Spring Cloud Bus 能管理和传播分布式系统间的消息，就像一个分布式执行器，可用于广播状态更改，事件推送等，也可以当作微服务间的通信通道



### 13. Stream

#### 概念

新技术作用：不关注具体MQ细节，我们只需要用一种适配绑定的方式，自动的给我们在各种MQ中切换

**屏蔽底层消息中间件的差异，降低切换成本，统一消息的编程模型**

**Spring Cloud Stream 是一个构建消息驱动的框架**



**rabbitmq启动命令**

`docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq rabbitmq:management`



#### 创建消息驱动生产者8801

- pom，yml文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>cloud2020</artifactId>
        <groupId>com.atguigu.springcloud</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>cloud-stream-rabbitmq-provider8801</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
        </dependency>
        <!--基础配置-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

```yml
server:
  port: 8801

spring:
  application:
    name: cloud-stream-provider
  cloud:
    stream:
      binders: # 在此处配置要绑定的rabbitmq的服务信息；
        defaultRabbit: # 表示定义的名称，用于于binding整合
          type: rabbit # 消息组件类型
          environment: # 设置rabbitmq的相关的环境配置
            spring:
              rabbitmq:
                host: 192.168.119.129
                port: 5672
                username: guest
                password: guest
      bindings: # 服务的整合处理
        output: # 这个名字是一个通道的名称
          destination: studyExchange # 表示要使用的Exchange名称定义
          content-type: application/json # 设置消息类型，本次为json，文本则设置“text/plain”
          binder: defaultRabbit # 设置要绑定的消息服务的具体设置

eureka:
  client: # 客户端进行Eureka注册的配置
    service-url:
      defaultZone: http://localhost:7001/eureka
  instance:
    lease-renewal-interval-in-seconds: 2 # 设置心跳的时间间隔（默认是30秒）
    lease-expiration-duration-in-seconds: 5 # 如果现在超过了5秒的间隔（默认是90秒）
    instance-id: send-8801.com  # 在信息列表时显示主机名称
    prefer-ip-address: true     # 访问的路径变为IP地址
```

- service及其实现，此处不是调用dao，是调用消息组件的@input和@output，因此没有service接口，此处用来测试消息发送管道

  - service

  - serviceimpl
  - controller

```java
package com.atguigu.springcloud.service.impl;

import com.atguigu.springcloud.service.IMessageProvider;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;

@EnableBinding(Source.class)        //定义消息的推送管道
public class MessageProviderImpl implements IMessageProvider {

    @Resource
    private MessageChannel output;  //消息发送管道
    @Override
    public String send() {
        String serial = UUID.randomUUID().toString();
        output.send(MessageBuilder.withPayload(serial).build());
        System.out.println("******serial: "+serial);
        return null;
    }
}
```

```java
public interface IMessageProvider {
    public String send();
}
```

 			

```java
package com.atguigu.springcloud.service.impl;

import com.atguigu.springcloud.service.IMessageProvider;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;

@EnableBinding(Source.class)        //定义消息的推送管道
public class MessageProviderImpl implements IMessageProvider {

    @Resource
    private MessageChannel output;  //消息发送管道
    @Override
    public String send() {
        String serial = UUID.randomUUID().toString();
        output.send(MessageBuilder.withPayload(serial).build());
        System.out.println("******serial: "+serial);
        return null;
    }
}
```

- 启动7001，8801，rabbitmq，访问 `http://localhost:8801/sendMessage` 看到rabbitmq-management出现如下界面即可

![image-20201006111928450](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201006111928450.png)



#### 消费者8802建立

- pom和yml文件

```yml
server:
  port: 8802

spring:
  application:
    name: cloud-stream-consumer
  cloud:
    stream:
      binders: # 在此处配置要绑定的rabbitmq的服务信息；
        defaultRabbit: # 表示定义的名称，用于于binding整合
          type: rabbit # 消息组件类型
          environment: # 设置rabbitmq的相关的环境配置
            spring:
              rabbitmq:
                host: 192.168.119.129
                port: 5672
                username: guest
                password: guest
      bindings: # 服务的整合处理
        input: # 这个名字是一个通道的名称
          destination: studyExchange # 表示要使用的Exchange名称定义
          content-type: application/json # 设置消息类型，本次为对象json，如果是文本则设置“text/plain”
          binder: defaultRabbit # 设置要绑定的消息服务的具体设置


eureka:
  client: # 客户端进行Eureka注册的配置
    service-url:
      defaultZone: http://localhost:7001/eureka
  instance:
    lease-renewal-interval-in-seconds: 2 # 设置心跳的时间间隔（默认是30秒）
    lease-expiration-duration-in-seconds: 5 # 如果现在超过了5秒的间隔（默认是90秒）
    instance-id: receive-8802.com  # 在信息列表时显示主机名称
    prefer-ip-address: true     # 访问的路径变为IP地址
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>cloud2020</artifactId>
        <groupId>com.atguigu.springcloud</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>cloud-stream-rabbitmq-consumer8802</artifactId>



    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <!--基础配置-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

- 业务逻辑类只有controller，添加注解`@EnableBinding`进行绑定，参数为Sink.class 
- 定义input方法，从message中拿消息 getPayload()，对应生产者的`MessageBuilder.withPayload(serial)`（把serial放进消息队列）

```java
@Component
@EnableBinding(Sink.class)
public class ReceiveMessageListenerController {
    @Value("${server.port}")
    private String serverPort;

    @StreamListener(Sink.INPUT)
    public void input(Message<String> message){
        System.out.println("消费者1号,----->收到的消息:"+message.getPayload()+"\t port:"+serverPort);
    }
}
```

- 继续访问 `http://localhost:8801/sendMessage`
- 看到以下结果即成功绑定，注意**yml文件中主机地址是虚拟机地址**（在我的电脑上是使用虚拟机docker）

![image-20201006113648792](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201006113648792.png)



#### 再创建一个8803，会出现一系列问题

![image-20201006123643405](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201006123643405.png)



- 有重复消费问题
  - 目前是8802/8803同时都收到了，存在重复问题
  - **如何解决：**分组和持久化属性group
  - 在Stream中，处于同一个组group中的多个消费者是竞争关系，就能够保证消息只会被其中一个应用消费一次
  - **不同组是可以全面消费的(重复消费)**
  - **同一组内会发生竞争关系，只有其中一个可以消费**
- 消息持久化问题





#### 分组

- 原理：
  - 微服务应用放置于同一个group中，就能够保证消息只会被其中一个应用消费一次。
  - 不同的组是可以同时消费的，同一个组内存在竞争关系，只有其中一个可以被消费
- 具体实现：
  - 只需要在yml文件中配置一个 **group：**，全部都加相同的组号就分到了相同的组

```yml
cloud:
  stream:
    binders: # 在此处配置要绑定的rabbitmq的服务信息；
      defaultRabbit: # 表示定义的名称，用于于binding整合
        type: rabbit # 消息组件类型
        environment: # 设置rabbitmq的相关的环境配置
          spring:
            rabbitmq:
              host: 192.168.119.129
              port: 5672
              username: guest
              password: guest
    bindings: # 服务的整合处理
      input: # 这个名字是一个通道的名称
        destination: studyExchange # 表示要使用的Exchange名称定义
        content-type: application/json # 设置消息类型，本次为对象json，如果是文本则设置“text/plain”
        binder: defaultRabbit # 设置要绑定的消息服务的具体设置
        group: atguiguB
```

```yml
cloud:
  stream:
    binders: # 在此处配置要绑定的rabbitmq的服务信息；
      defaultRabbit: # 表示定义的名称，用于于binding整合
        type: rabbit # 消息组件类型
        environment: # 设置rabbitmq的相关的环境配置
          spring:
            rabbitmq:
              host: 192.168.119.129
              port: 5672
              username: guest
              password: guest
    bindings: # 服务的整合处理
      input: # 这个名字是一个通道的名称
        destination: studyExchange # 表示要使用的Exchange名称定义
        content-type: application/json # 设置消息类型，本次为对象json，如果是文本则设置“text/plain”
        binder: defaultRabbit # 设置要绑定的消息服务的具体设置
        group: atguiguB
```





#### 消息持久化

- 只要加了**group**属性，就可以避免消息丢失
  - 当你删掉group关键字，停止服务，让生产者发送消息到Stream中，如果没有group，那么消息就会丢失，重新启动服务也无法接收到消息
  - 只有配置了group关键字，即使微服务已经停止，在再次启动的时候也不会丢失，而是会正确接受





### 14. Spring Cloud Sleuth

#### 概念：

- **分布式请求链路跟踪**
- **Spring Cloud Sleuth**提供了一套完整的服务跟踪的解决方案
  - 在分布式系统中提供追踪解决方案并且兼容支持了zipkin
  - zipkin dashboard以图形化网页的方式体现了追踪的路线
- 表示--请求链路，一条链路通过Trace ID唯一标识，Span标识发起的请求信息，各Span通过parent id关联起来
- Trace：类似于树结构的Span集合，表示一条调用链路，存在唯一标识
- Span：标识调用链路来源，通俗的理解Span就是一次请求信息



#### 在docker中安装zipkin

- `docker search zipkin`
- `docker pull openzipkin/zipkin`
- `docker run -d -p 9411:9411 openzipkin/zipkin --restart=Always`
- 启动后访问 `http://192.168.119.129:9411`,看到如下界面即代表成功

![image-20201006195729892](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201006195729892.png)

- 然后在8001，80文件中都加入controller测试方法

```java
@GetMapping("/payment/zipkin")
public String paymentZipkin()
{
    return "hi ,i'am paymentzipkin server fall back，welcome to atguigu，O(∩_∩)O哈哈~";
}
```

- 改动yml文件，加入配置

```yml
zipkin:
  base-url: http://192.168.119.129:9411
sleuth:
  sampler:
  #采样率值介于 0 到 1 之间，1 则表示全部采集
  probability: 1
```

- 注意 `192.168.119.129`是虚拟机 `ifconfig`得到的地址

- 最后进行测试即可



### 15. SpringCloud Alibaba

#### 功能：

- 服务限流降级
- 服务注册与发现
- 分布式配置管理
- 消息驱动能力
- 阿里云对象存储
- 分布式任务调度

#### 组件：

- Sentinel
- Nacos
- RocketMQ
- Dubbo
- Seata
- Alibaba Cloud OSS
- Alibaba Cloud SchedulerX





### 16. Nacos

**服务注册和配置中心**：

- Dynamic Naming and Configuration Service

- 一个更易于构建云原生应用的动态服务发现，配置管理和服务管理平台
- Nacos就是注册中心 + 配置中心的组合
- Nacos = Eureka + Config + Bus

![image-20201007152511985](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007152511985.png)





#### 注册payment9001服务提供者

- pom，yml文件

```yml
server:
  port: 9001

spring:
  application:
    name: nacos-payment-provider
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848 #配置Nacos地址

management:
  endpoints:
    web:
      exposure:
        include: '*'
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>cloud2020</artifactId>
        <groupId>com.atguigu.springcloud</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>cloudalibaba-provider-payment9001</artifactId>



    <dependencies>
        <!--SpringCloud ailibaba nacos -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <!-- SpringBoot整合Web组件 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <!--日常通用jar包配置-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- 启动类注解

```java
@SpringBootApplication
@EnableDiscoveryClient
public class PaymentMain9001 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentMain9001.class,args);
    }
}
```

- controller类

```java
@RestController
public class PaymentController
{
    @Value("${server.port}")
    private String serverPort;

    @GetMapping(value = "/payment/nacos/{id}")
    public String getPayment(@PathVariable("id") Integer id)
    {
        return "nacos registry, serverPort: "+ serverPort+"\t id"+id;
    }
}
```



- 创建另一个提供者9011，但是这里可以使用**copy**

![image-20201007154635064](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007154635064.png)

![image-20201007154710137](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007154710137.png)

- 加入 `-DServer.port=9011`即可指定端口启动



#### 服务消费者注册和负载

**Nacos本身支持负载均衡** : 原因是整合了ribbon，加@banlance注解就可以实现负载均衡



- 建立83消费者
- pom，yml，controller测试，config配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>cloud2020</artifactId>
        <groupId>com.atguigu.springcloud</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>cloudalibaba-consumer-nacos-order83</artifactId>




    <dependencies>
        <!--SpringCloud ailibaba nacos -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <!-- 引入自己定义的api通用包，可以使用Payment支付Entity -->
        <dependency>
            <groupId>com.atguigu.springcloud</groupId>
            <artifactId>cloud-api-commons</artifactId>
            <version>${project.version}</version>
        </dependency>
        <!-- SpringBoot整合Web组件 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <!--日常通用jar包配置-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

```yml
server:
  port: 83

spring:
  application:
    name: nacos-order-consumer
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848


#消费者将要去访问的微服务名称(注册成功进nacos的微服务提供者)
service-url:
  nacos-user-service: http://nacos-payment-provider
```

```java
@Configuration
public class ApplicationContextConfig {
    @Bean
    @LoadBalanced
    public RestTemplate getRestTemplate()
    {
        return new RestTemplate();
    }
}
```

```java
@RestController
@Slf4j
public class OrderNacosController {
    @Resource
    private RestTemplate restTemplate;

    @Value("${service-url.nacos-user-service}")
    private String serverURL;

    @GetMapping(value = "/consumer/payment/nacos/{id}")
    public String paymentInfo(@PathVariable("id") Long id){
        return restTemplate.getForObject(serverURL+"/payment/nacos/"+id,String.class);
    }
}
```

- **nacos出现消费者，表示注册成功**

![image-20201007160101513](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007160101513.png)

- 访问`http://localhost:83/consumer/payment/nacos/13`，测试负载均衡



![image-20201007160140195](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007160140195.png)![image-20201007160146479](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007160146479.png)

- 测试成功





#### Nacos服务注册中心对比提升

**Nacos支持AP和CP，可以自主切换**

- a ：高可用性
- c ：强一致性
- p ：分区容错性



#### 服务配置中心

**替代Config**

- Nacos和Config一样，在项目初始化的时候，要保证先从配置中心进行配置拉取，拉取配置之后，才能保证项目的正常启动
- Springboot中配置文件的加载是存在优先级顺序的，<font color = 'red'>bootstrap 优先级高于 application</font>
- 创建3377，改动yml文件

```yml
server:
  port: 3377	#此处可能存在端口被占用问题，请根据具体情况更改端口号
#bootstrap
spring:
  application:
    name: nacos-config-client
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848 #Nacos服务注册中心地址
      config:
        server-addr: localhost:8848 #Nacos作为配置中心地址
        file-extension: yaml #指定yaml格式的配置
        
# ${spring.application.name}-${spring.profile.active}.${spring.cloud.nacos.config.file-extension}
# nacos-config-client-dev.yaml

# nacos-config-client-test.yaml   ----> config.info
```

```yml
spring:
  profiles:
    active: dev # 表示开发环境
    #active: test # 表示测试环境
    #active: info
    
    #application
```

- 这里要注意语法
- 在nacos网页上发布配置

![image-20201007190041969](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007190041969.png)

- `DataID`是以下格式

`${spring.application.name}-${spring.profile.active}.${spring.cloud.nacos.config.file-extension}`

`nacos-config-client-dev.yaml`



- 测试：修改配置中的配置内容

![image-20201007190131225](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007190131225.png)

![image-20201007190143994](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007190143994.png)



- 通过访问 `http://localhost:8001/config/info`可以知道已经修改了，自动更改，不需要广播

![image-20201007190229414](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007190229414.png)

 

**下面这张图明确显示了DataID的语法规则**

<img src="C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007190332838.png" alt="image-20201007190332838" style="zoom: 67%;" />





#### 命名空间分组和DataID三者关系



<img src="C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007190742440.png" alt="image-20201007190742440" style="zoom:67%;" />



**类似于Java里面的package名和类名**

**最外层的namespace是可以用于区分部署环境的，Group和DataID逻辑上区分两个目标对象**

- Nacos默认的命名空间是public，Namespace主要用来隔离
- 比如：现在有3个生产环境，我们就可以创建三个Namespace，不同的Namespace之间是隔离的
- Group默认是DEFAULT_GROUP , GROUP可以把不同的微服务划分到同一个分组里面去
- Service就是微服务，一个微服务可以包含多个Cluster（集群）
- 新建一个配置

<img src="C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007192051574.png" alt="image-20201007192051574" style="zoom: 67%;" />



- 指定环境![image-20201007191918526](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007191918526.png)
- 进行测试 `http://localhost:8001/config/info`
- 已经被更改 ![image-20201007191958972](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007191958972.png)
- 测试成功



#### Group分组方案

- 新建了2个分组 ![image-20201007193337013](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007193337013.png)

- 根据DATAID语法配置，注意group

![image-20201007193406854](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007193406854.png)

![image-20201007193415116](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007193415116.png)	



- 访问 `http://localhost:8001/config/info`测试成功

![image-20201007193422860](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007193422860.png)





#### Namespace空间方案

- 定义命名空间，创建同一个命名空间下的不同组，并记录namespace

![image-20201007195953139](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007195953139.png)

![image-20201007200003758](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007200003758.png)

- 这样的配置下，访问 `http://localhost:8001/config/info`得到的就是![image-20201007200033598](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007200033598.png)

- 也就是 `8e364a5f-e9e5-4319-b8b2-238afe28ed98`命名空间下，DEV组下的`nacos-config-client-dev.yaml`





### 17. 集群和持久化配置

默认Nacos使用嵌入式数据库实现数据的存储。所以，如果启动多个默认配置下的Nacos节点，数据存储是存在一致性问题的，为了解决这个问题，Nacos使用了 **集中式存储的方式来支持集群化部署，目前只支持MySQL的存储**



- 这里进入/nacous/conf/ , 选定数据库执行nacos-mysql.sql语句，然后进入/nacous/conf/application.properties添加语句

```mysql
### Count of DB:
 db.num=1

### Connect URL of DB:
 db.url.0=jdbc:mysql://localhost:3306/cache?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=UTC
 db.user=root
 db.password=123456
```

- 重启nacos服务，访问页面发现页面为全新页面，代表启动成功

![image-20201007210437017](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201007210437017.png)

- 在使用数据库持久化配置以后，任何在 `localhost:8844/nacous`页面作的修改都会保存到数据库中



#### Linux版本集群安装与部署

- 预计需要，一个Nginx + 3个nacos注册中心 



**这次踩坑无数，等等一一记录，首先截图成功的界面**

- 首先，3个nacos集群启动
- 命令执行过程，首先切换到`nacos/bin`目录，然后 `./startup.sh -p 3333/4444/5555`启动三个服务，由于本地内存不足只启动了2个
- 可以用 `	ps -ef|grep nacos|grep -v grep|wc -l`来查询nacos进程数量

![image-20201008173542679](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201008173542679.png)

- 然后启动nginx，在root目录下执行以下命令 `nginx -c /etc/nginx/nginx.conf` 意思是以-c 后面的路径为配置文件启动niginx

![image-20201008173608437](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201008173608437.png)



- 首先验证nacos ,访问`http://192.168.119.129:3333/nacos`，进入以下界面代表nacos以集群方式启动成功

![image-20201008173950522](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201008173950522.png)



- 然后验证Nginx启动是否生效，访问 `http://192.168.119.129:1111/nacos` 如果出现以下界面则代表成功

![image-20201008174034271](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201008174034271.png)

-  那么接下来给出配置文件,第一个是nginix.conf(nginx),第二个是application.properties(nacos),第三个是startup.sh(nacos)

```
user www-data;
worker_processes auto;
pid /run/nginx.pid;

events {
	worker_connections 768;
	# multi_accept on;
}

http {

	##
	# Basic Settings
	##

	sendfile on;
	tcp_nopush on;
	tcp_nodelay on;
	keepalive_timeout 65;
	types_hash_max_size 2048;
	# server_tokens off;

	# server_names_hash_bucket_size 64;
	# server_name_in_redirect off;

	include /etc/nginx/mime.types;
	default_type application/octet-stream;

	##
	# SSL Settings
	##

	ssl_protocols TLSv1 TLSv1.1 TLSv1.2; # Dropping SSLv3, ref: POODLE
	ssl_prefer_server_ciphers on;

	##
	# Logging Settings
	##

	access_log /var/log/nginx/access.log;
	error_log /var/log/nginx/error.log;

	##
	# Gzip Settings
	##

	gzip on;
	
	gzip_disable "msie6";
	
	# gzip_vary on;
	# gzip_proxied any;
	# gzip_comp_level 6;
	# gzip_buffers 16 8k;
	# gzip_http_version 1.1;
	# gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

	##
	# Virtual Host Configs
	##

	include /etc/nginx/conf.d/*.conf;
	include /etc/nginx/sites-enabled/*;

	upstream cluster{
            server 192.168.119.129:3333;
            server 192.168.119.129:4444;
            server 192.168.119.129:5555;
	}
	server {
        	listen 1111;
        	server_name localhost;

        	location / {
            	    proxy_pass http://cluster;
        	}
    	}
}


#mail {
#	# See sample authentication script at:
#	# http://wiki.nginx.org/ImapAuthenticateWithApachePhpScript
# 
#	# auth_http localhost/auth.php;
#	# pop3_capabilities "TOP" "USER";
#	# imap_capabilities "IMAP4rev1" "UIDPLUS";
# 
#	server {
#		listen     localhost:110;
#		protocol   pop3;
#		proxy      on;
#	}
# 
#	server {
#		listen     localhost:143;
#		protocol   imap;
#		proxy      on;
#	}
#}
```

```
# spring

server.contextPath=/nacos
server.servlet.contextPath=/nacos
server.port=8848

# nacos.cmdb.dumpTaskInterval=3600
# nacos.cmdb.eventTaskInterval=10
# nacos.cmdb.labelTaskInterval=300
# nacos.cmdb.loadDataAtStart=false


# metrics for prometheus
#management.endpoints.web.exposure.include=*

# metrics for elastic search
management.metrics.export.elastic.enabled=false
#management.metrics.export.elastic.host=http://localhost:9200

# metrics for influx
management.metrics.export.influx.enabled=false
#management.metrics.export.influx.db=springboot
#management.metrics.export.influx.uri=http://localhost:8086
#management.metrics.export.influx.auto-create-db=true
#management.metrics.export.influx.consistency=one
#management.metrics.export.influx.compressed=true

server.tomcat.accesslog.enabled=true
server.tomcat.accesslog.pattern=%h %l %u %t "%r" %s %b %D %{User-Agent}i
# default current work dir
server.tomcat.basedir=

## spring security config
### turn off security
#spring.security.enabled=false
#management.security=false
#security.basic.enabled=false
#nacos.security.ignore.urls=/**

nacos.security.ignore.urls=/,/**/*.css,/**/*.js,/**/*.html,/**/*.map,/**/*.svg,/**/*.png,/**/*.ico,/console-fe/public/**,/v1/auth/login,/v1/console/health/**,/v1/cs/**,/v1/ns/**,/v1/cmdb/**,/actuator/**,/v1/console/server/**

# nacos.naming.distro.taskDispatchPeriod=200
# nacos.naming.distro.batchSyncKeyCount=1000
# nacos.naming.distro.syncRetryDelay=5000
# nacos.naming.data.warmup=true
# nacos.naming.expireInstance=true

nacos.istio.mcp.server.enabled=false

######################################################

 spring.datasource.platform=mysql

### Count of DB:
 db.num=1

### Connect URL of DB:
 db.url.0=jdbc:mysql://localhost:3306/nacos_config?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=UTC
 db.user=root
 db.password=123456
```

```
#!/bin/sh

# Copyright 1999-2018 Alibaba Group Holding Ltd.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

cygwin=false
darwin=false
os400=false
case "`uname`" in
CYGWIN*) cygwin=true;;
Darwin*) darwin=true;;
OS400*) os400=true;;
esac
error_exit ()
{
    echo "ERROR: $1 !!"
    exit 1
}
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=$HOME/jdk/java
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/usr/java
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/opt/taobao/java
[ ! -e "$JAVA_HOME/bin/java" ] && unset JAVA_HOME

if [ -z "$JAVA_HOME" ]; then
  if $darwin; then

    if [ -x '/usr/libexec/java_home' ] ; then
      export JAVA_HOME=`/usr/libexec/java_home`

    elif [ -d "/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home" ]; then
      export JAVA_HOME="/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home"
    fi
  else
    JAVA_PATH=`dirname $(readlink -f $(which javac))`
    if [ "x$JAVA_PATH" != "x" ]; then
      export JAVA_HOME=`dirname $JAVA_PATH 2>/dev/null`
    fi
  fi
  if [ -z "$JAVA_HOME" ]; then
        error_exit "Please set the JAVA_HOME variable in your environment, We need java(x64)! jdk8 or later is better!"
  fi
fi

export SERVER="nacos-server"
export MODE="cluster"
export FUNCTION_MODE="all"
while getopts ":m:f:s:p:" opt
do
    case $opt in
        m)
            MODE=$OPTARG;;
        f)
            FUNCTION_MODE=$OPTARG;;
        s)
            SERVER=$OPTARG;;
	p)
	    PORT=$OPTARG;;
        ?)
        echo "Unknown parameter"
        exit 1;;
    esac
done

export JAVA_HOME
export JAVA="$JAVA_HOME/bin/java"
export BASE_DIR=`cd $(dirname $0)/..; pwd`
export DEFAULT_SEARCH_LOCATIONS="classpath:/,classpath:/config/,file:./,file:./config/"
export CUSTOM_SEARCH_LOCATIONS=${DEFAULT_SEARCH_LOCATIONS},file:${BASE_DIR}/conf/

#===========================================================================================
# JVM Configuration
#===========================================================================================
if [[ "${MODE}" == "standalone" ]]; then
    JAVA_OPT="${JAVA_OPT} -Xms512m -Xmx512m -Xmn256m"
    JAVA_OPT="${JAVA_OPT} -Dnacos.standalone=true"
else
    JAVA_OPT="${JAVA_OPT} -server -Xms2g -Xmx2g -Xmn1g -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=320m"
    JAVA_OPT="${JAVA_OPT} -XX:-OmitStackTraceInFastThrow -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${BASE_DIR}/logs/java_heapdump.hprof"
    JAVA_OPT="${JAVA_OPT} -XX:-UseLargePages"

fi

if [[ "${FUNCTION_MODE}" == "config" ]]; then
    JAVA_OPT="${JAVA_OPT} -Dnacos.functionMode=config"
elif [[ "${FUNCTION_MODE}" == "naming" ]]; then
    JAVA_OPT="${JAVA_OPT} -Dnacos.functionMode=naming"
fi


JAVA_MAJOR_VERSION=$($JAVA -version 2>&1 | sed -E -n 's/.* version "([0-9]*).*$/\1/p')
if [[ "$JAVA_MAJOR_VERSION" -ge "9" ]] ; then
  JAVA_OPT="${JAVA_OPT} -cp .:${BASE_DIR}/plugins/cmdb/*.jar:${BASE_DIR}/plugins/mysql/*.jar"
  JAVA_OPT="${JAVA_OPT} -Xlog:gc*:file=${BASE_DIR}/logs/nacos_gc.log:time,tags:filecount=10,filesize=102400"
else
  JAVA_OPT="${JAVA_OPT} -Djava.ext.dirs=${JAVA_HOME}/jre/lib/ext:${JAVA_HOME}/lib/ext:${BASE_DIR}/plugins/cmdb:${BASE_DIR}/plugins/mysql"
  JAVA_OPT="${JAVA_OPT} -Xloggc:${BASE_DIR}/logs/nacos_gc.log -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M"
fi

JAVA_OPT="${JAVA_OPT} -Dnacos.home=${BASE_DIR}"
JAVA_OPT="${JAVA_OPT} -Dloader.path=${BASE_DIR}/plugins/health -jar ${BASE_DIR}/target/${SERVER}.jar"
JAVA_OPT="${JAVA_OPT} ${JAVA_OPT_EXT}"
JAVA_OPT="${JAVA_OPT} --spring.config.location=${CUSTOM_SEARCH_LOCATIONS}"
JAVA_OPT="${JAVA_OPT} --logging.config=${BASE_DIR}/conf/nacos-logback.xml"
JAVA_OPT="${JAVA_OPT} --server.max-http-header-size=524288"

if [ ! -d "${BASE_DIR}/logs" ]; then
  mkdir ${BASE_DIR}/logs
fi

echo "$JAVA ${JAVA_OPT}"

if [[ "${MODE}" == "standalone" ]]; then
    echo "nacos is starting with standalone"
else
    echo "nacos is starting with cluster"
fi

# check the start.out log output file
if [ ! -f "${BASE_DIR}/logs/start.out" ]; then
  touch "${BASE_DIR}/logs/start.out"
fi
# start
echo "$JAVA ${JAVA_OPT}" > ${BASE_DIR}/logs/start.out 2>&1 &
nohup $JAVA -Dserver.port=${PORT} ${JAVA_OPT} nacos.nacos >> ${BASE_DIR}/logs/start.out 2>&1 &
echo "nacos is starting，you can check the ${BASE_DIR}/logs/start.out"
```





### 18. Spring Cloud Sentinel

**面向云原生微服务的流量控制，熔断降级组件**

[Sentinel官方](https://github.com/alibaba/Sentinel/wiki/%E4%BB%8B%E7%BB%8D)

![image-20201009085201219](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009085201219.png)

![image-20201009085217698](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009085217698.png)

![image-20201009085226279](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009085226279.png)



**sentinel下载与安装**

![image-20201009085809365](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009085809365.png)

- 直接下载dashboard到指定文件夹，然后cmd打开命令行，由于下载的是jar包，直接`java -jar sentinel-dashboard-1.7.1.jar` 就可以运行

![image-20201009085859925](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009085859925.png)

- 出现以下界面就代表启动成功，访问 `localhost:8080`,账户与密码均是 `sentinel`

![image-20201009090005047](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009090005047.png)





- 首先启动Nacos8848（standalone）
- 新建module `cloudalibaba-sentinel-service8401`

```xml
<!--SpringCloud ailibaba nacos -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
        <!--SpringCloud ailibaba sentinel -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
        </dependency>
```

```yml
server:
  port: 8401

spring:
  application:
    name: cloudalibaba-sentinel-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848 #Nacos服务注册中心地址
    sentinel:
      transport:
        dashboard: localhost:8080 #配置Sentinel dashboard地址
        port: 8719
management:
  endpoints:
    web:
      exposure:
        include: '*'
```

- 主启动类增加注解，配置业务类

```java
@SpringBootApplication
@EnableDiscoveryClient
```

```java
@RestController
public class FlowLimitController {
    @GetMapping("/testA")
    public String testA(){
        return "-------TestA";
    }
    @GetMapping("/testB")
    public String testB(){
        return "-------TestB";
    }
}
```

- 启动nacos8848，启动sentinel8080，启动微服务8401，访问 `http://localhost:8401/testB`
  - **注意** sentinel 是懒加载机制，如果不进行方法的访问，那么微服务并不会被检测到

![image-20201009092139061](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009092139061.png)

![image-20201009092146994](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009092146994.png)



**微服务已经被注册进nacos和sentinel**



#### sentinel监控配置：

![image-20201009092832809](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009092832809.png)

- **QPS：**每秒钟查询次数，当调用该api的QPS到达阈值的时候进行限流，可以开启**高级选项**来选择流控模式和流控效果，默认是直接-快速失败，返回以下界面
- **线程数：**当调用该api的线程数到达阈值的时候，进行限流
- 流控模式
  - 直接：api到达限流条件时，直接限流
  - 关联：当关联的资源达到阈值时，就限流自己（B达到阈值，A限流） **支付接口达到阈值，就限流订单接口**
  - 链路：只记录指定链路上的数量（指定资源从入口资源进来的流量，如果达到阈值，就进行限流）【api级别的针对来源】
- 流控效果
  - 快速失败：直接失败，抛异常
  - Warm up：根据codeFactot（冷加载因子，默认3）的值，从阈值/codeFactor，经过预热时长，才达到设置的QPS阈值
  - 

![image-20201009092952147](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009092952147.png)

#### 关联

**使用postman并发，持续访问testB**
![image-20201009143817559](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009143817559.png)

![image-20201009143842570](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009143842570.png)



**此时访问A出现** ![image-20201009143906234](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009143906234.png)

**一直访问B，导致B到达阈值，因此和B关联的A受到限流处理**





#### 预热

- **Warm up**：根据codeFactot（冷加载因子，默认3）的值，从阈值/codeFactor，经过预热时长，才达到设置的QPS阈值
- 让通过的流量缓慢增加，在一定时间内逐渐增加到阈值上限，给冷系统一个预热的时间
- 默认codeFactor（冷加载因子）为3，经过预热时长后才会达到阈值

**设置**

![image-20201009144922576](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009144922576.png)



#### 排队等待

匀速排队，让请求以均匀的速度通过，阈值类型必须设成QPS，否则无效

设置含义：/testA每秒一次请求，超过的话就排队等待，等待的超时时间为20000ms

![image-20201009145123539](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009145123539.png)

应对场景：比如消息队列，前一秒有大量的请求，后一秒通道空闲，排队等待可以让一些请求在空闲的时间段进行处理，而不是简单的拒绝请求，对于时延不敏感的应用可以使用



#### 降级

![image-20201009150439510](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009150439510.png)



- **sentinel熔断降级**会在调用链路中某个资源出现不稳定状态时（例如调用超时或异常比例升高），对这个资源的调用进行限制，让请求快速失败，避免影响到其他的资源而导致级联错误

- 当资源被降级后，在接下来的降级时间窗口之内，对该资源的调用都自动熔断（默认行为是抛出DegradeException）



**sentinel的断路器没有半开状态**

- 半开的状态系统自动去检测是否请求有异常，没有异常就关闭断路器恢复使用，有异常就继续打开断路器不可用，也就是Hystrix的设计原理





#### 热点key限流

**@SentinelResource和@HystrixCommand极其相似**

- 下面是sentinel网页端的设置界面，在热点规则里面添加配置，资源名和 `@SentinelResource`注解中的value相同

![image-20201009154925406](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009154925406.png)

- 在controller中增加测试方法和配置

```java
@GetMapping("/testHotKey")
@SentinelResource(value = "testHotKey",blockHandler = "deal_testHotKey")
public String testHotKey(@RequestParam(value = "p1",required = false)String p1,
                         @RequestParam(value = "p2",required = false)String p2){
    return "------testHotKey";
}

```

- 上述代码中， `@SentinelResource(value = "testHotKey",blockHandler = "deal_testHotKey")`需要注意
- 参数 `value = "testHotKey"`表示指定了资源名， `blockHandler = "deal_testHotKey"`表示指定了兜底方法，

- 指定方法后，在下面添加一个方法，名字和 `blockHandler设置的value一样`

```java
public String deal_testHotKey(String p1, String p2, BlockException exception){
    return "------deal_testHotKey,/(ㄒoㄒ)/~~";
}
```

- 这样，限流后，超出设定时显示的页面就是自己定义的页面 ![image-20201009155018141](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009155018141.png)

##### 参数例外项

- 打开高级选项，可以额外设置参数，让某种参数限流阈值和其他参数不同

![image-20201009160919589](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009160919589.png)



- 访问`http://localhost:8401/testHotKey?p1=5`，不被限流，而 `p1=a`时，一秒一次以上的访问量就会限流

- 注意，java 的RuntimeException ，@SentinelResource的兜底方法不进行处理





#### 系统规则

![image-20201009164550019](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009164550019.png)

![image-20201009164609780](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009164609780.png)





#### @SentinelResource

- 两种情况，一种按Resource访问，一种按URL访问

```java
@GetMapping("/byResource")
@SentinelResource(value = "byResource",blockHandler = "handleException")
public CommonResult byResource()
{
    return new CommonResult(200,"按资源名称限流测试ok",new Payment(2020L,"serial001"));
}

public CommonResult handleException(BlockException exception){
    return new CommonResult(444,exception.getClass().getCanonicalName()+"\t"+"服务不可用");
}
```

- **这种自己配置指定了兜底方法**

```java
@GetMapping("/rateLimit/byUrl")
@SentinelResource(value = "byUrl")
public CommonResult byUrl()
{
    return new CommonResult(200,"按URL限流测试ok",new Payment(2020L,"serial002"));
}
```

- **这种使用Sentinel默认的兜底方法**





**以上情况都有缺点**

- 系统默认的，没有体现我们自己的业务要求
- 依照现有条件，我们自定义的处理方法又和业务代码耦合在一起，不直观
- 每个业务方法都添加一个兜底方法，代码膨胀加剧
- 全局统一的处理方法没有体现



#### 自己配置，将兜底方法提取到一个类中进行调用

![image-20201009171522470](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009171522470.png)



**CustomBlockHandler**

```java
public class CustomBlockHandler {
    public static CommonResult handlerException(BlockException exception){
        return new CommonResult(4444,"按客户自定义,global handler Exception------1");
    }
    public static CommonResult handleException2(BlockException exception){
        return new CommonResult(4444,"按客户自定义,global handler Exception------2");
    }
}
```

**controller业务类进行改动**

```java
@GetMapping("/byResource")
@SentinelResource(value = "byResource",
        blockHandlerClass = CustomBlockHandler.class,
        blockHandler = "handlerException")
public CommonResult byResource()
{
    return new CommonResult(200,"按资源名称限流测试ok",new Payment(2020L,"serial001"));
}
@GetMapping("/rateLimit/CustomBlockHandler")
    @SentinelResource(value = "CustomBlockHandler",
            blockHandlerClass = CustomBlockHandler.class,
            blockHandler = "handleException2")
    public CommonResult CustomBlockHandler()
    {
        return new CommonResult(200,"按客户自定义",new Payment(2020L,"serial003"));
    }
```

- 注意看注解 `blockHandlerClass = CustomBlockHandler.class` `blockHandler = "handlerException"`
- 首先指定类名，然后指定方法，这样就可以指定别的包下的兜底方法，让代码耦合度降低



#### 服务熔断

- controller类，业务代码

```java
@RestController
@Slf4j
public class CircleBreakerController {

    public static final String SERVICE_URL = "http://nacos-payment-provider";
    @Resource
    private RestTemplate restTemplate;

    @RequestMapping("/consumer/fallback/{id}")
    @SentinelResource(value = "fallback",
            fallback = "handlerFallback",
            blockHandler = "blockHandler",
            exceptionsToIgnore = {IllegalArgumentException.class})
    public CommonResult<Payment> fallback(@PathVariable Long id){
        CommonResult<Payment> result = restTemplate.getForObject(SERVICE_URL+"/paymentSQL/"+id,CommonResult.class,id);
        if(id == 4){
            throw new IllegalArgumentException("IllegalArgumentException非法参数异常");
        }else if(result.getData()==null){
            throw new NullPointerException("NullPointerException,该ID没有对应记录，空指针异常");
        }
        return result;
    }
    public CommonResult handlerFallback(@PathVariable  Long id,Throwable e) {
        Payment payment = new Payment(id,"null");
        return new CommonResult<>(444,"兜底异常handlerFallback,exception内容  "+e.getMessage(),payment);
    }
    //本例是blockHandler
    public CommonResult blockHandler(@PathVariable  Long id, BlockException blockException) {
        Payment payment = new Payment(id,"null");
        return new CommonResult<>(445,"blockHandler-sentinel限流,无此流水: blockException  "+blockException.getMessage(),payment);
    }

    @Resource
    private PaymentService paymentService;

    @GetMapping(value = "/consumer/paymentSQL/{id}")
    public CommonResult<Payment> paymentSQL(@PathVariable("id") Long id){
        return paymentService.paymentSQL(id);
    }
}
```

- config，调用restTemplate

```java
@Configuration
public class ApplicationContextConfig {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
```

- service层，调用openfeign框架

```java
@FeignClient(value = "nacos-payment-provider",fallback = PaymentFallbackService.class)
public interface PaymentService {
    @GetMapping(value = "/paymentSQL/{id}")
    public CommonResult<Payment> paymentSQL(@PathVariable("id") Long id);
}
```

```java
@Component
public class PaymentFallbackService implements PaymentService {

    @Override
    public CommonResult<Payment> paymentSQL(Long id) {
        return new CommonResult<>(44444,"服务降级返回，-----PaymentFallbackService",new Payment(id,"errorSerial"));
    }
}
```

- yml文件配置

```yml
server:
  port: 84


spring:
  application:
    name: nacos-order-consumer
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
    sentinel:
      transport:
        #配置Sentinel dashboard地址
        dashboard: localhost:8080
        #默认8719端口，假如被占用会自动从8719开始依次+1扫描,直至找到未被占用的端口
        port: 8719

#消费者将要去访问的微服务名称(注册成功进nacos的微服务提供者)
service-url:
  nacos-user-service: http://nacos-payment-provider

# 激活Sentinel对Feign的支持
feign:
  sentinel:
    enabled: true
```



- 当`blockHandler` 和 `handlerFallback`同时出现时，如果出现降级将会调用`blockHandler` 
- 当配置`exceptionsToIgnore = {IllegalArgumentException.class}`这个参数，那么就不走带有 `IllegalArgumentException`的兜底方法





#### Sentinel持久化规则

- 一旦我们重启应用，sentinel规则将消失，生产环境需要将配置规则进行持久化
- 将限流配置规则持久化进Nacos保存，只要刷新8401某个rest地址，sentinel控制台的流控规则就能看到，只要Nacos里面的配置不删除，针对8401上sentinel的流控规则就持续有效



**配置json字符串**

![image-20201009195203839](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009195203839.png)

```json
[
    {
        "resource": "/rateLimit/byUrl",
        "limitApp": "default",
        "grade": 1,
        "count": 1,
        "strategy": 0,
        "controBehavior": 0,
        "clusterMode": false
    }
]
```



- 这样配置以后就做到了持久化配置，也就是说已经将规则注册到了nacos中，dashboard中只要8401启动，(重启后需要访问以下对应持久化的rest)那么就会出现流控规则，重启后也不会删除

![image-20201009195603610](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201009195603610.png)

### 



### 18. SpringCloud Alibaba Seata



#### 分布式事务

- 一次业务操作需要跨多个数据源或需要多个系统进行远程调用，就会产生分布式事务问题
- 保证全局数据一致性问题



#### Seata

**Seata 是一款开源的分布式事务解决方案，致力于提供高性能和简单易用的分布式事务服务。Seata 将为用户提供了 AT、TCC、SAGA 和 XA 事务模式，为用户打造一站式的分布式解决方案。**

**TC (Transaction Coordinator) - 事务协调者**

维护全局和分支事务的状态，驱动全局事务提交或回滚。

**TM (Transaction Manager) - 事务管理器**

定义全局事务的范围：开始全局事务、提交或回滚全局事务。

**RM (Resource Manager) - 资源管理器**

管理分支事务处理的资源，与TC交谈以注册分支事务和报告分支事务的状态，并驱动分支事务提交或回滚。





#### 业务测试

![image-20201010160908787](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201010160908787.png)



创建三个数据库

```mysql
create database seata_order;
USE seata_order;
CREATE TABLE `t_order`  (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',
  `product_id` bigint(11) DEFAULT NULL COMMENT '产品id',
  `count` int(11) DEFAULT NULL COMMENT '数量',
  `money` decimal(11, 0) DEFAULT NULL COMMENT '金额',
  `status` int(1) DEFAULT NULL COMMENT '订单状态:  0:创建中 1:已完结',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '订单表' ROW_FORMAT = Dynamic;

CREATE TABLE `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  `ext` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

create database seata_storage;
USE seata_storage;
DROP TABLE IF EXISTS `t_storage`;
CREATE TABLE `t_storage`  (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `product_id` bigint(11) DEFAULT NULL COMMENT '产品id',
  `total` int(11) DEFAULT NULL COMMENT '总库存',
  `used` int(11) DEFAULT NULL COMMENT '已用库存',	
  `residue` int(11) DEFAULT NULL COMMENT '剩余库存',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '库存' ROW_FORMAT = Dynamic;
INSERT INTO `t_storage` VALUES (1, 1, 100, 0, 100);

CREATE TABLE `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  `ext` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE database seata_account;
USE seata_account;
DROP TABLE IF EXISTS `t_account`;
CREATE TABLE `t_account`  (
  `id` bigint(11) NOT NULL COMMENT 'id',
  `user_id` bigint(11) DEFAULT NULL COMMENT '用户id',
  `total` decimal(10, 0) DEFAULT NULL COMMENT '总额度',
  `used` decimal(10, 0) DEFAULT NULL COMMENT '已用余额',
  `residue` decimal(10, 0) DEFAULT NULL COMMENT '剩余可用额度',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '账户表' ROW_FORMAT = Dynamic;

INSERT INTO `t_account` VALUES (1, 1, 1000, 0, 1000);

CREATE TABLE `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  `ext` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
```

在每个数据库下面都执行下面语句，以记录日志

```mysql
-- the table to store seata xid data
-- 0.7.0+ add context
-- you must to init this sql for you business databese. the seata server not need it.
-- 此脚本必须初始化在你当前的业务数据库中，用于AT 模式XID记录。与server端无关（注：业务数据库）
-- 注意此处0.3.0+ 增加唯一索引 ux_undo_log
drop table `undo_log`;
CREATE TABLE `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  `ext` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
```

![image-20201010163142982](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201010163142982.png)

创建完成





#### 搭建业务代码

- 业务需求：`下订单-->减库存-->扣余额-->改（订单）状态`
- 三个module `seata-order-sevice2001` `seata-storage-service2002` `seata-account-service2003`
- Config类配置 `DatasourceProxyConfig` `MyBatisConfig`

```java
package com.atguigu.springcloud.alibaba.config;

import com.alibaba.druid.pool.DruidDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * @auther zzyy
 * @create 2020-02-26 16:24
 * 使用Seata对数据源进行代理
 */
@Configuration
public class DataSourceProxyConfig {

    @Value("${mybatis.mapperLocations}")
    private String mapperLocations;

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource druidDataSource(){
        return new DruidDataSource();
    }

    @Bean
    public DataSourceProxy dataSourceProxy(DataSource dataSource) {
        return new DataSourceProxy(dataSource);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactoryBean(DataSourceProxy dataSourceProxy) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSourceProxy);
        sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mapperLocations));
        sqlSessionFactoryBean.setTransactionFactory(new SpringManagedTransactionFactory());
        return sqlSessionFactoryBean.getObject();
    }

}
```

```java
package com.atguigu.springcloud.alibaba.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * @auther zzyy
 * @create 2019-12-11 16:57
 */
@Configuration
@MapperScan({"com.atguigu.springcloud.alibaba.dao"})
public class MyBatisConfig {
}
```

- 此处只给出order项目的配置，因为是核心类，其他项目业务代码无太大差别
- 首先，Config已经给出，那么我们先写`domain`下的实体类 `order`和 消息返回格式类`CommonResult`

```java
package com.atguigu.springcloud.alibaba.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order
{
    private Long id;

    private Long userId;

    private Long productId;

    private Integer count;

    private BigDecimal money;

    private Integer status; //订单状态：0：创建中；1：已完结
}
```

```java
package com.atguigu.springcloud.alibaba.domain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonResult<T>
{
    private Integer code;
    private String  message;
    private T       data;

    public CommonResult(Integer code, String message)
    {
        this(code,message,null);
    }
}
```

- 数据库字段

![image-20201011103018057](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201011103018057.png)

- 然后写dao层接口，定义create方法接口，定义参数让service实现

```java
@Mapper
public interface OrderDao
{
    //1 新建订单
    void create(Order order);

    //2 修改订单状态，从零改为1
    void update(@Param("userId") Long userId,@Param("status") Integer status);
}
```

- Service层分接口和impl实现类，首先看接口，接口与dao一样，但是这里是seata微服务调用，使用了openfeign，调用了其他类中的微服务方法，那么我们需要有3个service接口，以及在controller中调用这三个，impl实现了业务代码，所以他需要调用三个service接口，然后进行业务逻辑的处理，是业务代码的核心，这里调用了所有的微服务方法进行处理，符合了上文所说的 `下订单-->减库存-->扣余额-->改（订单）状态` ![image-20201011103225543](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201011103225543.png)

```java
public interface OrderService {
    void create(Order order);
}
```

```java
@FeignClient(value = "seata-account-service")
public interface AccountService{
    @PostMapping(value = "/account/decrease")
    CommonResult decrease(@RequestParam("userId")Long userId,@RequestParam("money") BigDecimal money);
}
```

```java
@FeignClient(value = "seata-storage-service")
public interface StorageService {
    @PostMapping(value = "/storage/decrease")
    CommonResult decrease(@RequestParam("productId")Long productId,@RequestParam("used")Integer used);
}
```

- OrderServiceImpl，首先用@Resource引入了2个service和dao，这里其他微服务方法用@feign注解标注后被order调用，实际使用的是对应@feign的服务的方法，实现了微服务调用，因此在上面的2个service接口中，使用了 **@FeignClient(value="xxxxxx")**这样就可以指定微服务，调用具体微服务的方法

  ```java
  @Slf4j
  @Service
  public class OrderServiceImpl implements OrderService{
      @Resource
      private StorageService storageService;
      @Resource
      private OrderDao orderDao;
      @Resource
      private AccountService accountService;
  
      @Override
      public void create(Order order) {
          log.info("---->>开始新建订单");
          //1. 新建订单
          orderDao.create(order);
  
          log.info("---->>订单微服务开始调用库存，做扣减count");
          //2. 扣减库存
          storageService.decrease(order.getProductId(),order.getCount());
          log.info("---->>订单微服务开始调用库存，做扣减end");
  
          log.info("---->>订单微服务开始调用账户，做扣减Money");
          //3. 扣减账户余额
          accountService.decrease(order.getUserId(),order.getMoney());
          log.info("---->>订单微服务开始调用账户，做扣减end");
  
          //4. 修改订单状态，从0到1，1代表已经完成
          log.info("---->>修改订单状态开始");
          orderDao.update(order.getUserId(),0);
          log.info("---->>修改订单状态结束");
  
          log.info("---->>下订单结束了，O(∩_∩)O");
      }
  }
  ```

```java
@Slf4j
@Service
public class OrderServiceImpl implements OrderService{
    @Resource
    private StorageService storageService;
    @Resource
    private OrderDao orderDao;
    @Resource
    private AccountService accountService;

    @Override
    public void create(Order order) {
        log.info("---->>开始新建订单");
        //1. 新建订单
        orderDao.create(order);

        log.info("---->>订单微服务开始调用库存，做扣减count");
        //2. 扣减库存
        storageService.decrease(order.getProductId(),order.getCount());
        log.info("---->>订单微服务开始调用库存，做扣减end");

        log.info("---->>订单微服务开始调用账户，做扣减Money");
        //3. 扣减账户余额
        accountService.decrease(order.getUserId(),order.getMoney());
        log.info("---->>订单微服务开始调用账户，做扣减end");

        //4. 修改订单状态，从0到1，1代表已经完成
        log.info("---->>修改订单状态开始");
        orderDao.update(order.getUserId(),0);
        log.info("---->>修改订单状态结束");

        log.info("---->>下订单结束了，O(∩_∩)O");
    }
}
```

- 接下来看controller层，controller层只需要简单调用service层中的方法即可

```java
@RestController
public class OrderController {
    @Resource
    private OrderService orderService;

    @GetMapping("/order/create")
    public CommonResult create(Order order){
        orderService.create(order);
        return new CommonResult(200,"订单创建成功");
    }
}
```

- 最后我们需要看mapper文件下的xml文件，此处在书写完dao和domain就可以进行编码了，就是简单的数据库增删改查语句以及resultMap格式的返回，但是此处需要注意一个点，首先在yml文件中需要配置一下  

```yml
mybatis:
  mapperLocations: classpath:mapper/*.xml
```

- 需要让mybatis扫描到你的xml文件，其次，**你在创建文件的时候必须显示指定 `XXXX.xml`这样才能被扫描到**，不然会出现参数错误，无法找到的error

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd" >

<mapper namespace="com.atguigu.springcloud.alibaba.dao.OrderDao">

    <resultMap id="BaseResultMap" type="com.atguigu.springcloud.alibaba.domain.Order">
        <id column="id" property="id" jdbcType="BIGINT"/>
        <result column="user_id" property="userId" jdbcType="BIGINT"/>
        <result column="product_id" property="productId" jdbcType="BIGINT"/>
        <result column="count" property="count" jdbcType="INTEGER"/>
        <result column="money" property="money" jdbcType="DECIMAL"/>
        <result column="status" property="status" jdbcType="INTEGER"/>
    </resultMap>

    <insert id="create">
        insert into t_order (id,user_id,product_id,count,money,status)
        values (null,#{userId},#{productId},#{count},#{money},0);
    </insert>


    <update id="update">
        update t_order set status = 1
        where user_id=#{userId} and status = #{status};
    </update>

</mapper>
```

- 最后，需要在yml文件中配置ribbon超时时间，不然在调用微服务，实际输入网址的时候可能会提示你超时，事务回滚，也就是以下代码

```yml
#ribbon的超时时间
ribbon:
  ReadTimeout: 30000
  ConnectTimeout: 30000
```

- 给出yml的所有代码

```yml
server:
  port: 2001

spring:
  application:
    name: seata-order-service
  cloud:
    alibaba:
      seata:
        #自定义事务组名称需要与seata-server中的对应
        tx-service-group: fsp_tx_group
    nacos:
      discovery:
        server-addr: localhost:8848
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://localhost:3306/seata_order
    username: root
    password: 123456
#ribbon的超时时间
ribbon:
  ReadTimeout: 30000
  ConnectTimeout: 30000
feign:
  hystrix:
    enabled: false

logging:
  level:
    io:
      seata: info

mybatis:
  mapperLocations: classpath:mapper/*.xml
```

- **测试**： 访问 `http://localhost:2001/order/create?userId=1&productId=1&count=10&money=100`得到以下结果，首先网页端

![image-20201011104157242](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201011104157242.png)

- 返回我们自己定义的`CommonResult`字符串信息，再看数据库`t_order`表，也就是下订单的表，添加一行数据表示下达订单

![image-20201011104250555](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201011104250555.png)

- 表示已经下单了，也就是说用户已经买了物品，再看 `t_storage`，也就是库存表，本来是

![image-20201011104709232](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201011104709232.png)

- 经过访问后，变成

![image-20201011105743324](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201011105743324.png)

- 看`t_account`表

![image-20201011105803036](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201011105803036.png)

- 数据库也发生了变化，因此创建一个订单，库存，账户余额都发生了变化，测试通过



#### 异常事件

- 如果加入线程延迟， 那么事务就会报出执行超时，那么部分事务会执行，部分不会，就会导致很严重的问题，比如扣钱了，但是库存没少等等
- 这里seata提供了一个注解 `@GlobalTransactional`来解决这个问题，意思就是如果出现异常那么全局回滚，保证事务的一致性（原子性）
- 也就是说，成功就全部成功，不然就全部失败，而不是部分微服务成功执行，部分不执行，这样就可以解决事务一致性的问题

- 在类的create方法上添加注解  `@GlobalTransactional(name = "fsp-create-order",rollbackFor = Exception.class)`即可
- 参数的意思是对于`seata-name` 为 `fsp-create-order`的事务，出现任何异常都回滚 `rollbackFor = Exception.class`

#### 



#### Seata原理简介

**简单可扩展自治事务框架**

![image-20201011144403071](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201011144403071.png)

![image-20201011144510849](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201011144510849.png)

![image-20201011145154283](C:\Users\fhawk\AppData\Roaming\Typora\typora-user-images\image-20201011145154283.png)



**[官方文档](https://seata.io/zh-cn/docs/overview/what-is-seata.html)**