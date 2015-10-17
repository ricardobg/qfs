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
			threadWriteBuffer.start();
			threadWriteFile.start();
			while (threadWriteBuffer.isAlive() || threadWriteFile.isAlive()) {
				Thread.sleep(100);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//Thread 3 -> receive data and write the bytes in a buffer.
	private class Thread3 extends Thread {
		
		private int number; //number of thread
		private int t3_id = 0; //package id
		
		public Thread3(int number) {
			this.number = number;
		}

		@Override
		public void run() {
			try {
				System.out.println("New connection with client " + clientSocker.getInetAddress().getHostAddress());
				DataInputStream dis = new DataInputStream(clientSocker.getInputStream());
				byte[] b = new byte[Block.BLOCK_SIZE];
				int read = dis.read(b);
				while (read > 0) {
					buffer.put(new Block(b, read, t3_id++));
					read = dis.read(b);
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
	private class Thread4 extends Thread {

		int number; //number of thread
		private int t4_id = 0; //package id
		
		public Thread4(int number) {
			this.number = number;
		}
		
		@Override
		public void run() {
			try {
				FileOutputStream fos = null;
					fos = new FileOutputStream(file);
				
				while (true) {
						Block b = buffer.take();
						if (b.finished())
							break;
						fos.write(b.getBytes()); 
				}
				fos.close();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
}
