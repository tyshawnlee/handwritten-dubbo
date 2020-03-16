package com.provider;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.client.constant.DubboConst;
import com.client.service.IBookService;
import com.provider.service.BookServiceImpl;
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
	private static Map<String, String> serviceMap = new HashMap<>();
	private String host = "127.0.0.1:12000";
	private ZooKeeper zk;

	static {
		//将所有service都注册到ZooKeeper上
		serviceMap.put(IBookService.class.getName(), BookServiceImpl.class.getName());
	}

	/**
	 * 连接zk
	 *
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
	 */
	public void register() {
		try {
			for (Map.Entry<String, String> entry : serviceMap.entrySet()) {
				String nodePath = DubboConst.rootNode + "/" + entry.getKey() + "/" + DubboConst.providerNode + "/" + host;
				if (zk.exists(nodePath, false) == null) {
					zk.create(nodePath, entry.getValue().getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
				}
			}
		} catch (Exception e) {
			logger.error("Provider注册服务到ZooKeeper报错", e);
		}
	}


	public static void main(String[] args) {
		//模拟启动Provider
		Provider provider = new Provider("114.55.27.138:2181");
		provider.register();

		//监听端口, 处理rpc请求
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
			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
