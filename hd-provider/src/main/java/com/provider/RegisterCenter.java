package com.provider;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.client.constant.ZooKeeperConst;

/**
 * @author litianxiang
 * @date 2020/3/17 11:28
 */
public class RegisterCenter {
	private static Logger logger = LoggerFactory.getLogger(RegisterCenter.class);
	private ZooKeeper zk;


	/**
	 * 连接ZooKeeper, 创建dubbo根节点
	 */
	public RegisterCenter() {
		try {
			CountDownLatch connectedSignal = new CountDownLatch(1);
			zk = new ZooKeeper(ZooKeeperConst.host, 5000, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					if (event.getState() == Event.KeeperState.SyncConnected) {
						connectedSignal.countDown();
					}
				}
			});
			//因为监听器是异步操作, 要保证监听器操作先完成, 即要确保先连接上ZooKeeper再返回实例.
			connectedSignal.await();

			//创建dubbo注册中心的根节点(持久节点)
			if (zk.exists(ZooKeeperConst.rootNode, false) == null) {
				zk.create(ZooKeeperConst.rootNode, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
		} catch (Exception e) {
			logger.error("connect zookeeper server error.", e);
		}
	}

	/**
	 * 将服务和服务提供者URL注册到注册中心
	 * @param serviceName
	 * @param serviceProviderAddr
	 */
	public void register(String serviceName, String serviceProviderAddr) {
		try {
			//创建服务节点
			String servicePath = ZooKeeperConst.rootNode + "/" + serviceName;
			if (zk.exists(servicePath, false) == null) {
				zk.create(servicePath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}

			//创建服务地址节点
			String serviceAddrPath = servicePath + "/" + serviceProviderAddr;
			if (zk.exists(serviceAddrPath, false) == null) {
				zk.create(serviceAddrPath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
			}

			logger.info("服务注册成功, 服务路径: " + serviceAddrPath);
		} catch (Exception e) {
			logger.error("注册中心-注册服务报错", e);
		}
	}

	public static void main(String[] args) throws IOException {
		RegisterCenter registerCenter = new RegisterCenter();
		registerCenter.register("com.client.dto.BookDTO.IBookService", "192.168.2.97:12000");
		System.in.read();
	}
}
