package qfs.sender;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.util.Arrays;
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
	private volatile int read_blocks = 0;
	private volatile int sent_blocks = 0;
	final private int queue_size = 100;
	Socket socket;
	public FileSender(String file_path) throws Exception {
		file = new File(file_path);
		if (!file.exists()) {
			throw new Exception("File '" + file_path + "' not found");
		}
		queue = new ArrayBlockingQueue<Block>(queue_size);
	}
	
	public void send(final String destination, final int port) throws Exception {
		//Reading thread
		Thread read_thread = new Thread(){
			@Override
			public void run() {
				try {
					FileInputStream fis = new FileInputStream(file);
					
					
					int read;
					do
					{
						byte[] buffer = new byte[Block.BLOCK_SIZE];
						read = fis.read(buffer); 
						Block b = new Block(buffer, read, 0);
						queue.put(b);
						read_blocks++;
					}
					while (read > 0 && !tcp_error);
					fis.close();
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
		};
		
		
		Thread send_thread = new Thread(){
			@Override
			public void run() {
				// Initializes TCP
				try {
					DataOutputStream output = new DataOutputStream(socket.getOutputStream());
					while (true) {
						Block buffer = queue.take();
						if (buffer.finished()) {
							System.out.println("CABO");
							break;
						}
						output.write(buffer.getBytes());
						output.flush();
						sent_blocks++;
					}
					output.close();
					//socket.
					socket.close();
				} 
				catch (IOException e) {
					tcp_exception = new Exception("I/O error: " + e.getMessage());
					tcp_error = true;
				} catch (InterruptedException e) {
					tcp_exception = new Exception("Sync error: " + e.getMessage());
					tcp_error = true;
				}
			}
		};
		// sender thread
		try  {
			socket = new Socket(destination, port);
		}
		catch (UnknownHostException e) {
			tcp_exception = new Exception("Unkown host: " + e.getMessage());
			tcp_error = true;
		}
		catch (IOException e) {
			tcp_exception = new Exception("I/O error: " + e.getMessage());
			tcp_error = true;
		} 
		if (!tcp_error) {
			read_thread.start();
			send_thread.start();
			while (send_thread.isAlive() || read_thread.isAlive()) {
				System.out.printf("\rRead Blocks: %d. Sent Blocks: %d        ", read_blocks, sent_blocks);
				try {
				    Thread.sleep(100);
				} catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
				}
			}
			System.out.printf("     ");
			System.out.println();
		}
		//Finished loop, check for errors
		if (read_error)
			throw reading_exception;
		if (tcp_error)
			throw tcp_exception;
		System.out.printf("Read Blocks: %5d. Sent Blocks: %5d", read_blocks, sent_blocks);
	}
}
