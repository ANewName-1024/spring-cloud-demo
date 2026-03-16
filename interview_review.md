# 魏超 - 面试复习资料

> 📺 推荐：B站搜关键字即可，以下是常用UP主

---

## 一、Java 基础

### 1. 集合类
- **ArrayList vs LinkedList**
  - ArrayList：动态数组，查询O(1)，增删O(n)
  - LinkedList：双向链表，增删O(1)，查询O(n)
- **HashMap**：JDK1.7 vs 1.8
  - 1.7：数组+链表，链表法解决冲突
  - 1.8：数组+链表+红黑树，链表长度>8转红黑树
- **ConcurrentHashMap**：
  - 1.7：Segment分段锁
  - 1.8：CAS+synchronized

**📺 视频推荐**：
- 【hashmap详解】https://www.bilibili.com/video/BV1hE411v7b5
- 【ArrayList源码分析】https://www.bilibili.com/video/BV1yE411v7u8

### 2. 多线程
- **Thread状态**：NEW → RUNNABLE → BLOCKED/WAITING → TERMINATED
- **synchronized**：对象锁，保证可见性、原子性
- **volatile**：保证可见性，不保证原子性
- **ThreadLocal**：线程本地变量

**📺 视频推荐**：
- 【多线程详解】https://www.bilibili.com/video/BV1hE411v7b5
- 【synchronized原理】https://www.bilibili.com/video/BV1aE41167uC
- 【volatile关键字】https://www.bilibili.com/video/BV1hE411v7b5

### 3. 反射与注解
- 反射：动态获取类信息
- 注解：元数据标记

**📺 视频推荐**：
- 【反射机制】https://www.bilibili.com/video/BV1mE411x7Zd

---

## 二、Spring/Spring Boot

### 1. Spring Bean 生命周期
1. 实例化
2. 属性赋值
3. 初始化
4. 销毁

### 2. Spring 事务传播行为
- REQUIRED：支持当前事务
- REQUIRES_NEW：新建事务
- NESTED：嵌套事务

### 3. Spring MVC 工作流程
1. 请求 → DispatcherServlet
2. HandlerMapping 找处理器
3. 执行Handler
4. 返回ModelAndView
5. 视图解析
6. 渲染返回

### 4. Spring Boot 自动装配
- @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
- META-INF/spring.factories

### 5. Spring Cloud 组件
- **Eureka**：服务注册与发现
- **Ribbon**：负载均衡
- **Feign**：声明式HTTP客户端
- **Hystrix**：熔断器
- **Zuul/Gateway**：网关

**📺 视频推荐**：
- 【Spring源码】https://www.bilibili.com/video/BV1iE411v7bP
- 【SpringBoot精通】https://www.bilibili.com/video/BV1Et411Y7tQ
- 【SpringCloudAlibaba】https://www.bilibili.com/video/BV1nE411v7uT

---

## 三、数据库

### 1. MySQL
- **索引**：B+树、Hash
- **事务**：ACID（原子性、一致性、隔离性、持久性）
- **隔离级别**：READ UNCOMMITTED → READ COMMITTED → REPEATABLE READ → SERIALIZABLE
- **锁**：行锁、表锁、间隙锁
- **慢查询优化**：EXPLAIN、索引、最左前缀原则

**📺 视频推荐**：
- 【MySQL索引底层】https://www.bilibili.com/video/BV1Vox411c7m
- 【MySQL事务】https://www.bilibili.com/video/BV1Vox411c7m
- 【MySQL优化】https://www.bilibili.com/video/BV1SQ4y1Z7Tb

### 2. Redis
- **数据类型**：String、List、Hash、Set、ZSet
- **持久化**：RDB、AOF
- **淘汰策略**：LRU、LFU、TTL
- **集群**：主从复制、哨兵、Cluster

**📺 视频推荐**：
- 【Redis入门】https://www.bilibili.com/video/BV1S54y1R7c5
- 【Redis持久化】https://www.bilibili.com/video/BV1S54y1R7c5
- 【Redis集群】https://www.bilibili.com/video/BV1S54y1R7c5

---

## 四、JVM

### 1. 内存区域
- **堆**：对象实例、字符串常量池
- **栈**：局部变量、方法参数
- **方法区**：类信息、静态变量
- **本地方法栈**：Native方法

### 2. 垃圾回收
- **算法**：标记-清除、复制、标记-整理
- **垃圾收集器**：Serial、ParNew、CMS、G1、ZGC
- **GC Roots**：对象引用链起点

### 3. 调优参数
- -Xms -Xmx：堆大小
- -Xss：栈大小
- -XX:+UseG1GC：使用G1收集器

**📺 视频推荐**：
- 【JVM精讲】https://www.bilibili.com/video/BV1yE411v7b5
- 【JVM调优实战】https://www.bilibili.com/video/BV1yE411v7b5
- 【G1收集器】https://www.bilibili.com/video/BV1yE411v7b5

---

## 五、项目相关知识点

### 权限管理系统
- **OAuth2**：授权框架
- **JWT**：Token生成与验证
- **Shiro/Spring Security**：权限框架
- **单点登录(SSO)**

**📺 视频推荐**：
- 【SpringSecurity】https://www.bilibili.com/video/BV1Ee411W7gu
- 【OAuth2实战】https://www.bilibili.com/video/BV1HE411w7w5

### 订单管理系统
- **分布式事务**：CAP、BASE
- **消息队列**：RabbitMQ、Kafka
- **接口幂等性**

**📺 视频推荐**：
- 【分布式事务】https://www.bilibili.com/video/BV1J5411c7XZ
- 【RabbitMQ】https://www.bilibili.com/video/BV1dE411K7MG
- 【Kafka入门】https://www.bilibili.com/video/BV1aE411v7uP

### 应急响应/数据安全
- **备份策略**：全量、增量、差异
- **安全扫描**：OWASP Top 10
- **等保合规**

---

## 六、常见面试题

### Q1: HashMap扩容机制
- 数组+链表，当链表长度>8且数组长度>=64时转为红黑树
- 负载因子0.75，扩容2倍

### Q2: synchronized与Lock区别
- synchronized：JVM层面，自动释放
- Lock：API层面，需手动释放，支持公平锁

### Q3: MySQL索引失效
- like以%开头
- or条件
- 函数操作
- 类型转换

### Q4: Spring Boot启动流程
1. SpringApplication.run()
2. 加载spring.factories
3. 启动内嵌Tomcat
4. 自动装配

### Q5: JVM调优思路
- 监控GC日志
- 分析堆内存
- 调整堆大小
- 选择合适GC收集器

---

## 📚 推荐学习资源

### 博客/网站
- 掘金：https://juejin.cn
- 思否：https://segmentfault.com
- 博客园：https://www.cnblogs.com
- CSDN：https://blog.csdn.net

### 视频UP主
- **CodeSheep**：https://space.bilibili.com/384280368
- **古董技术官**：https://space.bilibili.com/476381187
- **编程迷思**：https://space.bilibili.com/351014170
- **程序员鱼皮**：https://space.bilibili.com/128904453

### 书籍推荐
- 《Java核心技术卷I》
- 《深入理解Java虚拟机》
- 《MySQL必知必会》
- 《Redis设计与实现》
