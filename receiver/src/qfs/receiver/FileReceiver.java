package qfs.receiver;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

import qfs.common.Block;

public class FileReceiver {
	private File file;
	
	private ServerSocket serverSocket;
	private Socket clientSocker;
	
	final private int buffer_size = 100;
	final private int block_size = 100*1024;
	private ArrayBlockingQueue<Block> buffer;
	
	public FileReceiver(String file_path) {
		file = new File(file_path);
		buffer = new ArrayBlockingQueue<Block>(buffer_size);
	}
	
	public void receive(final int port) {
		try {
			serverSocket = new ServerSocket(port);
			clientSocker = serverSocket.accept();
		
		Thread3 threadWriteBuffer = new Thread3(0);
		Thread4 threadWriteFile = new Thread4(0);
		
		while (threadWriteBuffer.isAlive() || threadWriteFile.isAlive()) {
			Thread.sleep(100);
		}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//Thread 3 -> receive data and write the bytes in a buffer.
	private class Thread3 implements Runnable {
		
		private int number; //number of thread
		private Thread t3;
		private int t3_id = 0; //package id
		
		public Thread3(int number) {
			this.number = number;
			t3 = new Thread();
			t3.start();
		}

		public boolean isAlive() {
			return t3.isAlive();
		}

		@Override
		public void run() {
			try {
				System.out.println("New connection with client " + clientSocker.getInetAddress().getHostAddress());
				DataInputStream dis = new DataInputStream(clientSocker.getInputStream());
				byte[] b = new byte[block_size];
				
				while (dis.read(b) > 0) {
					buffer.put(new Block(b, block_size, t3_id++));
				}					
				
				buffer.put(new Block());
				
				serverSocket.close();
				clientSocker.close();
			} catch(IOException | InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}

	//Thread 4 -> write bytes to a file 
	private class Thread4 implements Runnable {

		int number; //number of thread
		Thread t4;
		private int t4_id = 0; //package id
		
		public Thread4(int number) {
			this.number = number;
			t4 = new Thread();
			t4.start();
		}

		public boolean isAlive() {
			return t4.isAlive();
		}
		
		@Override
		public void run() {
			try {
				FileOutputStream fos = null;
					fos = new FileOutputStream(file);
				
				while (!clientSocker.isOutputShutdown()) {
						Block b = buffer.take();
						if(b.getSize() > 0)
							fos.write(b.getBytes());
						else break;
									
				}
					fos.close();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
}
