import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.client.constant.DubboConst;
import com.client.dto.BookDTO;
import com.client.service.IBookService;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author litianxiang
 * @date 2020/3/6 15:46
 */
public class Consumer {
	private static Logger logger = LoggerFactory.getLogger(Consumer.class);
	private List<String> providerList;
	private ZooKeeper zk;

	/**
	 * 连接zk
	 * @param host zk server地址
	 */
	public Consumer(String host) {
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
	 * Consumer在ZooKeeper订阅服务
	 * @param serviceName
	 * @param host
	 */
	public void subscribe(String serviceName, String host) {
		try {
			//获取服务provider地址, 同时注册一个监听器, 当provider数目发生变化时要及时更新provider的地址
			String subscribeNodePath = DubboConst.rootNode + "/" + serviceName + "/" + DubboConst.providerNode;
			providerList = zk.getChildren(subscribeNodePath, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					if (event.getType() == Event.EventType.NodeChildrenChanged) {
						try {
							//循环监听
							providerList = zk.getChildren(subscribeNodePath, true);
						} catch (KeeperException | InterruptedException e) {
							logger.error("Consumer在ZooKeeper订阅服务-注册监听器报错", e);
						}
					}
				}
			});

			//创建consumer节点
			String nodePath = DubboConst.rootNode + "/" + serviceName + "/" + DubboConst.consumerNode + "/" + host;
			if (zk.exists(nodePath, false) == null) {
				zk.create(nodePath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
			}
		} catch (Exception e) {
			logger.error("Consumer在ZooKeeper订阅服务报错, service=" + serviceName + ",host=" + host, e);
		}
	}

	public static void main(String[] args) {
		IBookService bookService = (IBookService) getRpcProxy(IBookService.class);
		BookDTO bookInfo = bookService.getBookInfo(1);
		System.out.println(bookInfo);
	}

	private static Object getRpcProxy(final Class clazz) {
		return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				Socket socket = new Socket("127.0.0.1", 12000);
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

				// 向服务端发送接口名, 方法名, 方法参数class, 方法参数
				String className = clazz.getName();
				String methodName = method.getName();
				Class[] paramTypes = method.getParameterTypes();
				out.writeUTF(className);
				out.writeUTF(methodName);
				out.writeObject(paramTypes);
				out.writeObject(args);
				out.flush();

				//接收方法执行结果
				Object object = in.readObject();
				in.close();
				out.close();
				socket.close();

				return object;
			}
		});
	}

}
