package com.provider;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author litianxiang
 * @date 2020/3/6 15:32
 */
public class Provider {

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
