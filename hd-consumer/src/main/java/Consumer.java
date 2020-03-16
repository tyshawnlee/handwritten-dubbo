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
import com.client.utils.ProxyFactory;
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
	private ZooKeeper zk;
	private List<String> providerList;

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
	 */
	public void subscribe(String serviceName) {
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
			logger.info(serviceName + "的服务提供者列表: " + providerList);
		} catch (Exception e) {
			logger.error("Consumer在ZooKeeper订阅服务报错, service=" + serviceName, e);
		}
	}

	/**
	 * 获取RPC代理
	 * @param clazz
	 * @return
	 */
	public static Object getRpcProxy(final Class clazz) {
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

	public static void main(String[] args) {
		Consumer consumer = new Consumer("114.55.27.138:2181");
		consumer.subscribe(IBookService.class.getName());

		IBookService bookService = (IBookService) getRpcProxy(IBookService.class);
		BookDTO bookInfo = bookService.getBookInfo(1);
		System.out.println(bookInfo);
	}

}
