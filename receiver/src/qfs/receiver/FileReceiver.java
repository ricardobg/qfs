package qfs.receiver;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

import qfs.common.Block;

public class FileReceiver {
	private File file;
	
	private ServerSocket serverSocket;
	private Socket clientSocker;
	
	final private int buffer_size = 100;
	private ArrayBlockingQueue<Block> buffer;
	
	private Thread3[] multiThread3;
	private volatile long rec_time = 0;
	
	Map<Integer, Block> map_blocks;	//hash table of id_block to block;
	
	public FileReceiver(String file_path) {
		file = new File(file_path);
		buffer = new ArrayBlockingQueue<Block>(buffer_size);
		map_blocks = new HashMap<Integer, Block>();
		
	}
	
	public void receive(final int port, final int nThreads, final int block_size, final int queue_size, final boolean shared_con) {
		
		try {
			
			serverSocket = new ServerSocket(port);
			DataInputStream dis = null;
			if (shared_con) {
				this.clientSocker = serverSocket.accept();
				System.out.println("New connection with client " + clientSocker.getInetAddress().getHostAddress());
				dis = new DataInputStream(clientSocker.getInputStream());
			}
			multiThread3 = new Thread3[nThreads];
			for (int i = 0; i < nThreads; i++) {
				multiThread3[i] = new Thread3(shared_con, port, dis);
				multiThread3[i].start();
			}
			
			Thread4 thread4 = new Thread4(nThreads);
			thread4.start();
			
			while (threadsAreAlive(nThreads) || thread4.isAlive()) {
				Thread.sleep(1);
			}
			if (shared_con)
				dis.close();
			serverSocket.close();
			if (shared_con) {
				this.clientSocker.close();
			}
			
			System.out.println("Time to receive : " + rec_time / 1000000.0 + "ms");
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static Semaphore semaphore = new Semaphore(1);
	//Thread 3 -> receive data and write the bytes in a buffer.
	private class Thread3 extends Thread {
		
		private Socket clientSocker;
		private boolean shared_con;
		private DataInputStream dis;
		
		
		public Thread3(boolean shared_con, int port, DataInputStream stream) throws IOException {
			this.shared_con = shared_con;
			if (!shared_con) { this.clientSocker = serverSocket.accept(); 
			System.out.println("New connection with client " + this.clientSocker.getInetAddress().getHostAddress()); }
			else 			{
				this.clientSocker = FileReceiver.this.clientSocker;
				this.dis = stream;
			}
		}

		@Override
		public void run() {
			try {
				byte[] b = new byte[Block.getRealBlockSize()];
				if (!shared_con)
					dis = new DataInputStream(this.clientSocker.getInputStream());
				
				long start = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
				
				rec_time +=  ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - start;
				int read = 0, last_read = 0;
				Block block;
				boolean got_semaphore = false;
				do {
					
					if (shared_con && !got_semaphore) {
						semaphore.acquire();
						got_semaphore = true;
					}
					last_read = dis.read(b, read, Block.getRealBlockSize() - read);
					read += last_read;
					if (read == Block.getRealBlockSize()) {
						if (shared_con) {
							semaphore.release();
							got_semaphore = false;
						}
						block = new Block(b, read);
						read = 0;
						buffer.put(block);
						
					}
					start = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
					rec_time +=  ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - start;
				}
				while (last_read > 0);
				if (read > 0) {
					block = new Block(b, read);
					buffer.put(block);
				}
				if (shared_con)
					semaphore.release();
				
				buffer.put(new Block());
				
				
				if (!shared_con) this.clientSocker.close();
				
			} catch(IOException | InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}

	//Thread 4 -> write bytes to a file 
	private class Thread4 extends Thread {

		private int t4_id = 0; //package id
		private int nThreads;
		private int counterThreads3 = 0;
		
		public Thread4(int n) {
			this.nThreads = n;
		}
		
		@Override
		public void run() {
			try {
				FileOutputStream fos = new FileOutputStream(file);
				
				long save_time = 0;
				while (true) {
						Block b = buffer.take();
						if (b.finished())
							counterThreads3++;
						
						if (counterThreads3 == nThreads)
							break;
						
						System.out.println("Block arrived: " + b.getId() + "	Block expected: " + t4_id);
						if (b.getId() == t4_id) {
							long start = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
							fos.write(b.getBytes());
							save_time += ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - start;
							t4_id += Block.getBlockSize();
						} else {
							map_blocks.put(b.getId(), b);
							long start = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
							t4_id = writeBlockFromMapIfExists(t4_id, fos);
							save_time += ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - start;
						}
						
				}
				System.out.println("Time to write file: " + (save_time) / 1000000.0 + "ms");
		        
				fos.close();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private boolean threadsAreAlive(int nThreads) {
		for (int i = 0; i < nThreads; i++)
			if (multiThread3[i].isAlive())
				return true;
		return false;
	}
	
	// searches it Map if the block with id = current_id exists and write it into fos
	private int writeBlockFromMapIfExists(int current_id, FileOutputStream fos) throws IOException {		
		Block block = map_blocks.get(current_id);
		while (block != null) {
			fos.write(block.getBytes());
			current_id += Block.getBlockSize();
			block = map_blocks.get(current_id);
		}
		
		return current_id;
		
	}
	
}
