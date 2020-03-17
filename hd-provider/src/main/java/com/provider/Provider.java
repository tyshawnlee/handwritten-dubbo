package com.provider;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.client.service.IBookService;
import com.provider.service.BookServiceImpl;

/**
 * @author litianxiang
 * @date 2020/3/6 15:32
 */
public class Provider {
	private static Logger logger = LoggerFactory.getLogger(Provider.class);
	private static Map<String, Class> serviceMap = new HashMap<>();
	private static String tcpHost = "127.0.0.1:12000";

	static {
		/**
		 * 模拟service配置处理逻辑
		 * <dubbo:service interface="com.client.service.IBookService" ref="bookService" />
		 * <bean id="bookService" class="com.provider.service.BookServiceImpl" />
		 */
		serviceMap.put(IBookService.class.getName(), BookServiceImpl.class);
	}

	public static void main(String[] args) {
		//将服务和服务提供者URL注册到注册中心
		RegisterCenter registerCenter = new RegisterCenter();
		for (Map.Entry<String, Class> entry : serviceMap.entrySet()) {
			registerCenter.register(entry.getKey(), tcpHost);
		}

		//监听Consumer的远程调用(为了简化代码, 这里使用TCP代替Netty)
		RpcServer rpcServer = new RpcServer(serviceMap);
		rpcServer.start();
	}
}
