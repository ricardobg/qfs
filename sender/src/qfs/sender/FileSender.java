package qfs.sender;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;

import qfs.common.Block;

public class FileSender {
	private File file;
	//Circular multi-thread queue
	private volatile boolean tcp_error = false;
	private volatile boolean read_error = false;
	private Exception reading_exception = null;
	private Exception tcp_exception = null;
	private ArrayBlockingQueue<Block> queue;
	private volatile long read_time = 0;
	private volatile long send_time = 0;
	private int threads;
	private final boolean shared_connection;
	Socket socket;
	public FileSender(String file_path, int queue_size, int block_size, boolean shared_connection) throws Exception {
		file = new File(file_path);
		if (!file.exists()) {
			throw new Exception("File '" + file_path + "' not found");
		}
		Block.SetBlockSize(block_size);
		queue = new ArrayBlockingQueue<Block>(queue_size);
		this.shared_connection = shared_connection;
	}
	
	public void send(final String destination, final int port, final int threads) throws Exception {
		this.threads = threads;
		Thread1 thread1 = new Thread1();//read thread
		Thread2[] threads2 = new Thread2[threads];//send thread
		long start = System.currentTimeMillis();
		try  {
			if (shared_connection) {
				socket = new Socket(destination, port);
				System.out.println("New connection with server " + socket.getInetAddress().getHostAddress());
			}
			for (int i = 0; i < threads; i++) {
				if (this.shared_connection)
					threads2[i] = new Thread2(socket);
				else
					threads2[i] = new Thread2(destination, port);
			}
		}
		catch (UnknownHostException e) {
			throw new Exception("Unkown host: " + e.getMessage());
		}
		catch (IOException e) {
			throw new Exception("I/O error: " + e.getMessage());
		} 
		
		for (int i = 0; i < threads; i++)
			threads2[i].start();
		thread1.start();
		while (HasAlive(thread1, threads2)) {
			try {
			    Thread.sleep(50);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
		}
		try {
			System.out.println("Closing connection");
			if (shared_connection)
				this.socket.close();
		} catch (Exception e) {
			
		}
		send_time = System.currentTimeMillis() - start;
		System.out.println();
		//Finished loop, check for errors
		if (read_error)
			throw reading_exception;
		if (tcp_error)
			throw tcp_exception;
		System.out.println("File sent!");
		System.out.println("Time to Read: " + (read_time/(1000*1000.0)) + "ms");
		System.out.println("Time to Send: " + send_time + "ms");
		
	}
	
	private class Thread1 extends Thread {
		@Override
		public void run() {
			try {
				long start = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
				FileInputStream fis = new FileInputStream(file);
				int read;
				read_time = 0;
				byte[] buffer = new byte[Block.getBlockSize()];
				int part = 0;
				do
				{
					
					read = fis.read(buffer); 
					
					Block b = new Block(buffer, read, part);
					part += Block.getBlockSize();
					queue.put(b);
				}
				while (read > 0 && !tcp_error);
				
				fis.close();
				read_time = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - start;
				for (int i = 0; i < threads; i++)
					queue.put(new Block());
			}
			catch (FileNotFoundException e) {
				reading_exception = new Exception("File '" + file + "' not found");
				read_error = true;
			} catch (IOException e) {
				// failed reading
				reading_exception = new Exception("I/O error: " + e.getMessage());
				read_error = true;
			} catch (InterruptedException e) {
				// couldn't put buffer in queue
				reading_exception = new Exception("Sync error: " + e.getMessage());
				read_error = true;
			}
		}
	}
	
	private class Thread2 extends Thread {
		private Socket socket;
		public Thread2(String destination, int port) throws UnknownHostException, IOException {
			this.socket = new Socket(destination, port);
			System.out.println("New connection with server " + socket.getInetAddress().getHostAddress());
			
		}
		public Thread2(Socket socket) {
			this.socket = socket;
		}
		
		@Override
		public void run() {
			// Initializes TCP
			try {
				DataOutputStream output = new DataOutputStream(this.socket.getOutputStream());
				while (true) {
					Block buffer = queue.take();
					if (buffer.finished()) {
						break;
					}
					output.write(buffer.getBytes());
					output.flush();
					
				}
				output.close();
				try {
					if (!shared_connection)
						this.socket.close();
				} catch (Exception e) {
					
				}
			} 
			catch (IOException e) {
				tcp_exception = new Exception("I/O error on thread: " + e.getMessage());
				tcp_error = true;
			} catch (InterruptedException e) {
				tcp_exception = new Exception("Sync error: " + e.getMessage());
				tcp_error = true;
			}
		}
	}
	
	public static boolean HasAlive(Object... objs) throws Exception {
		for (Object obj : objs) {
			if (obj instanceof Thread) {
				if (((Thread)obj).isAlive())
					return true;
			}
			else if (obj instanceof Thread[]) {
				for (Thread thr : (Thread[]) obj) {
					if (thr.isAlive())
						return true;
				}
			}
			else
				throw new Exception ("Invalid argument");
		}
		return false;
	}
}
