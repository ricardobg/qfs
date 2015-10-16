package qfs.sender;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.util.concurrent.ArrayBlockingQueue;

public class FileSender {
	private File file;
	//Circular multi-thread queue
	private volatile boolean reading_finished = false;
	private volatile boolean tcp_error = false;
	private volatile boolean read_error = false;
	private Exception reading_exception = null;
	private Exception tcp_exception = null;
	private ArrayBlockingQueue<byte[]> queue;
	private volatile int read_blocks = 0;
	private volatile int sent_blocks = 0;
	final private int queue_size = 100;
	final private int block_size = 10*1024;
	
	public FileSender(String file_path) throws Exception {
		file = new File(file_path);
		if (!file.exists()) {
			throw new Exception("File '" + file_path + "' not found");
		}
		queue = new ArrayBlockingQueue<byte[]>(queue_size);
	}
	
	public void send(final String destination, final int port) throws Exception {
		//Reading thread
		Thread read_thread = new Thread(){
			@Override
			public void run() {
				byte[] buffer = new byte[block_size]; 
				FileInputStream fis;
				try {
					fis = new FileInputStream(file);
					while (!tcp_error && fis.read(buffer) > 0) {
						queue.put(buffer);
						read_blocks++;
					}
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
				finally {
					reading_finished = true;
				}
			}
		};
		
		// sender thread
		Thread send_thread = new Thread(){
			@Override
			public void run() {
				// Initializes TCP
				try {
					Socket socket = new Socket(destination, port);
					DataOutputStream output = new DataOutputStream(socket.getOutputStream());
					while ((!queue.isEmpty() || !reading_finished) && !read_error ) {
						byte[] buffer = queue.take();
						output.write(buffer);
						output.flush();
						sent_blocks++;
					}
					socket.close();
				} catch (UnknownHostException e) {
					tcp_exception = new Exception("Unkown host: " + e.getMessage());
					tcp_error = true;
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
		
		read_thread.start();
		send_thread.start();
		while (send_thread.isAlive() || read_thread.isAlive()) {
			System.out.printf("\rRead Blocks: %5d. Sent Blocks: %5d", read_blocks, sent_blocks);
			try {
			    Thread.sleep(100);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
		}
		//Finished loop, check for errors
		if (read_error)
			throw reading_exception;
		if (tcp_error)
			throw tcp_exception;
		System.out.printf("\rRead Blocks: %5d. Sent Blocks: %5d", read_blocks, sent_blocks);
	}
}
