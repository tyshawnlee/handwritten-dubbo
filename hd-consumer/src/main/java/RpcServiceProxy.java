import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;

/**
 * @author litianxiang
 * @date 2020/3/17 18:47
 */
public class RpcServiceProxy {
	private ServiceSubscribe serviceSubscribe;

	public RpcServiceProxy(ServiceSubscribe serviceSubscribe) {
		this.serviceSubscribe = serviceSubscribe;
	}

	/**
	 * 获取RPC代理
	 * @param clazz
	 * @return
	 */
	public Object getProxy(final Class clazz) {
		return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				String rpcHost = serviceSubscribe.subscribe(clazz.getName());
				String[] split = rpcHost.split(":");
				Socket socket = new Socket(split[0], Integer.parseInt(split[1]));
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
