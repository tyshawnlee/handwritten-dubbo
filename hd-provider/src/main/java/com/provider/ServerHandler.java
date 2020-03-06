package com.provider;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.Socket;

import com.client.service.IBookService;
import com.provider.service.BookServiceImpl;

/**
 * @author litianxiang
 * @date 2020/3/6 17:52
 */
public class ServerHandler implements Runnable {
	private Socket socket;

	public ServerHandler(Socket socket) {
		this.socket = socket;
	}

	public void run() {
		ObjectInputStream in = null;
		ObjectOutputStream out = null;
		try {
			in = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());

			//获取接口名, 方法名, 方法参数class, 方法参数
			String className = in.readUTF();
			String methodName = in.readUTF();
			Class[] paramTypes = (Class[]) in.readObject();
			Object[] params = (Object[]) in.readObject();

			Class clazz = null;
			//模拟<dubbo:service interface="com.client.service.IBookService" ref="bookService"/>, 获取接口对应的实现类
			if (className.equals(IBookService.class.getName())) {
				clazz = BookServiceImpl.class;
			} else {
				return;
			}

			//执行对应方法
			Method method = clazz.getMethod(methodName, paramTypes);
			Object invoke = method.invoke(clazz.newInstance(), params);

			//返回方法执行结果
			out.writeObject(invoke);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			socket = null;
		}
	}
}
