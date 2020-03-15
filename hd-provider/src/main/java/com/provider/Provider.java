package com.provider;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import com.client.constant.DubboConst;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author litianxiang
 * @date 2020/3/6 15:32
 */
public class Provider {
	private static Logger logger = LoggerFactory.getLogger(Provider.class);
	private ZooKeeper zk;

	/**
	 * 连接zk
	 * @param host zk server地址
	 */
	public Provider(String host) {
		try {
			CountDownLatch connectedSignal = new CountDownLatch(1);
			zk = new ZooKeeper(host, 5000, new Watcher() {
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
			if (zk.exists(DubboConst.rootNode, false) == null) {
				zk.create(DubboConst.rootNode, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
		} catch (Exception e) {
			logger.error("connect zookeeper server error.", e);
		}
	}

	/**
	 * Provider注册服务到ZooKeeper
	 * @param serviceName 服务名
	 * @param host 服务地址
	 */
	public void register(String serviceName, String host) {
		try {
			String nodePath = DubboConst.rootNode + "/" + serviceName + "/" + DubboConst.providerNode + "/" + host;
			if (zk.exists(nodePath, false) == null) {
				zk.create(nodePath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
			}
		} catch (Exception e) {
			logger.error("Provider注册服务到ZooKeeper报错, service=" + serviceName + ",host=" + host, e);
		}
	}

	public static void main(String[] args) {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(12000);
			System.out.println("server start...");
			while (true) {
				Socket socket = serverSocket.accept();
				new Thread(new ServerHandler(socket)).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(serverSocket != null){
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
