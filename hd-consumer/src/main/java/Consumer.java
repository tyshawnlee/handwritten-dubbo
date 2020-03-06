import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;

import com.client.dto.BookDTO;
import com.client.service.IBookService;

/**
 * @author litianxiang
 * @date 2020/3/6 15:46
 */
public class Consumer {
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
