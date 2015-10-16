package qfs.sender;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.nio.Buffer;
import java.util.concurrent.ArrayBlockingQueue;

public class FileSender {
	private File file;
	//Circular multi-thread queue
	private volatile boolean reading_finished = false;
	private volatile boolean tcp_error = false;
	private Exception reading_error = null;
	private ArrayBlockingQueue<byte[]> queue;
	
	private int queue_size = 100;
	private int block_size = 10*1024;
	
	public FileSender(String file_path) throws Exception {
		file = new File(file_path);
		if (!file.exists()) {
			throw new Exception("File '" + file_path + "' not found");
		}
		queue = new ArrayBlockingQueue<byte[]>(queue_size);
	}
	
	public void send(final String destination, final int port) {
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
					}
				}
				catch (FileNotFoundException e) {
					reading_error = new Exception("File '" + file + "' not found");
				} catch (IOException e) {
					// failed reading
					reading_error = new Exception("I/O error: " + e.getMessage());
				} catch (InterruptedException e) {
					// couldn't put buffer in queue
					reading_error = new Exception("Sync error: " + e.getMessage());
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
					while (!queue.isEmpty() || !reading_finished) {
						byte[] buffer = queue.take();
						output.write(buffer);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
		
	}
}
