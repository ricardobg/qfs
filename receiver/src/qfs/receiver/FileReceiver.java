package qfs.receiver;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

public class FileReceiver {
	private File file;
	
	private ServerSocket serverSocket;
	private Socket clientSocker;
	
	final private int buffer_size = 100;
	final private int block_size = 100*1024;
	private ArrayBlockingQueue<byte[]> buffer;
	
	public FileReceiver(String file_path, int port, int nThreads) {
		file = new File(file_path);
		buffer = new ArrayBlockingQueue<byte[]>(buffer_size);
		
		receive(port);
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
		
		int number; //number of thread
		Thread t3;
		
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
					buffer.put(b);
				}					
				
				buffer.put(new byte[0]);
				
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
						byte[] b = buffer.take();
						if(b.length > 0)
							fos.write(b);
						else break;
									
				}
					fos.close();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
}
