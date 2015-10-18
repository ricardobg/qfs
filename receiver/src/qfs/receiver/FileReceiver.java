package qfs.receiver;

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
	
	public void receive(final int port, final int nThreads, final int block_size, final int queue_size) {
		
		try {
			serverSocket = new ServerSocket(port);
			clientSocker = serverSocket.accept();
			System.out.println("New connection with client " + clientSocker.getInetAddress().getHostAddress());
			
			multiThread3 = new Thread3[nThreads];
			
			for (int i = 0; i < nThreads; i++) {
				multiThread3[i] = new Thread3(i);
				multiThread3[i].start();
			}
			
			Thread4 thread4 = new Thread4(0, nThreads);
			thread4.start();
			
			while (threadsAreAlive(nThreads) || thread4.isAlive()) {
				Thread.sleep(100);
			}
		
			serverSocket.close();
			clientSocker.close();
			
			System.out.println("Time to receive : " + rec_time / 1000000.0 + "ms");
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//Thread 3 -> receive data and write the bytes in a buffer.
	private class Thread3 extends Thread {
		
		private int number; //number of thread
		
		public Thread3(int number) {
			this.number = number;
		}

		@Override
		public void run() {
			try {
				byte[] b = new byte[Block.getRealBlockSize()];
				DataInputStream dis = new DataInputStream(clientSocker.getInputStream());
				
				long start = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
				int read = dis.read(b);
				long now = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
				rec_time += now - start;
				
				while (read > 0) {
					Block block = new Block(b, read);
					buffer.put(block);
					read = dis.read(b);
				}
				
				buffer.put(new Block());
				
			} catch(IOException | InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}

	//Thread 4 -> write bytes to a file 
	private class Thread4 extends Thread {

		private int number; //number of thread
		private int t4_id = 0; //package id
		private int nThreads;
		private int counterThreads3 = 0;
		
		public Thread4(int number, int n) {
			this.number = number;
			this.nThreads = n;
		}
		
		@Override
		public void run() {
			try {
				FileOutputStream fos = new FileOutputStream(file);
				
				long start = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
				while (true) {
						Block b = buffer.take();
						if (b.finished())
							counterThreads3++;
						
						if (counterThreads3 == nThreads)
							break;
						
						System.out.println("Block arrived: " + b.getId() + "	Block expected: " + t4_id);
						if (b.getId() == t4_id) {
							fos.write(b.getBytes());
							t4_id += Block.getBlockSize();
						} else {
							map_blocks.put(b.getId(), b);
							t4_id = writeBlockFromMapIfExists(t4_id, fos);
						}
						
				}
				long now = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
				System.out.println("Time to write file: " + (now - start) / 1000000.0 + "ms");
		        
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
