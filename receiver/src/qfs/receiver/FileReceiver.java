package qfs.receiver;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
	
	Thread3[] multiThread3;
	Thread4[] multiThread4;
	
	Map<Integer, Block> map_blocks;	//hash table of id_block to block;
	
	public FileReceiver(String file_path) {
		file = new File(file_path);
		buffer = new ArrayBlockingQueue<Block>(buffer_size);
		map_blocks = new HashMap<Integer, Block>();
		
	}
	
	public void receive(final int port, final int nThreads) {
		
		try {
			serverSocket = new ServerSocket(port);
			clientSocker = serverSocket.accept();
			System.out.println("New connection with client " + clientSocker.getInetAddress().getHostAddress());
			
			multiThread3 = new Thread3[nThreads];
			multiThread4 = new Thread4[nThreads];
			
			for (int i = 0; i < nThreads; i++) {
				multiThread3[i] = new Thread3(i);
				multiThread3[i].start();
				
				multiThread4[i] = new Thread4(i);
				multiThread4[i].start();
			}
			
			while (threadsAreAlive(nThreads)) {
				Thread.sleep(100);
			}
			
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
				DataInputStream dis = new DataInputStream(clientSocker.getInputStream());
				byte[] b = new byte[Block.BLOCK_SIZE];
				int read = dis.read(b);
				
				long start = System.currentTimeMillis();
				while (read > 0) {
					buffer.put(new Block(b, read));
					read = dis.read(b);
				}
				long now = System.currentTimeMillis();
		        System.out.println("Time to transmit in thread " + number + ": " + (now - start) + "ms");
				
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
				FileOutputStream fos = new FileOutputStream(file);
				
				long start = System.currentTimeMillis();
				while (true) {
						Block b = buffer.take();
						if (b.finished())
							break;
						System.out.println(b.getId() + " " + t4_id);
						if (b.getId() == t4_id) {
							fos.write(b.getBytes());
							t4_id += b.getSize();
						} else {
							map_blocks.put(b.getId(), b);
							t4_id = writeBlockFromMapIfExists(t4_id, fos);
						}
						
				}
				long now = System.currentTimeMillis();
				System.out.println("Time to write in thread " + number + ": " + (now - start) + "ms");
		        
				fos.close();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private boolean threadsAreAlive(int nThreads) {
		for (int i = 0; i < nThreads; i++)
			if (multiThread3[i].isAlive() || multiThread4[i].isAlive())
				return true;
		return false;
	}
	
	// searches it Map if the block with id = current_id exists and write it into fos
	private int writeBlockFromMapIfExists(int current_id, FileOutputStream fos) throws IOException {		
		Block block = map_blocks.get(current_id);
		while (block != null) {
			fos.write(block.getBytes());
			block = map_blocks.get(++current_id);
		}
		
		return current_id;
		
	}
	
}
