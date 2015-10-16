package qfs.receiver;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

public class FileReceiver {
	private File file;
	
	private ServerSocket serverSocket;
	private Socket clientSocker;
	
	final private int buffer_size = 100;
	final private int block_size = 100*1024;
	private ArrayBlockingQueue<byte[]> buffer;
	
	private volatile boolean continueReading = true;
	private String mutex = "";
	public FileReceiver(String file_path, int port) {
		file = new File(file_path);
		buffer = new ArrayBlockingQueue<byte[]>(buffer_size);
		
		receive(port);
	}
	
	public void receive(final int port) {
		
		try {
			serverSocket = new ServerSocket(port);
			clientSocker = serverSocket.accept();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		//Thread 4 -> write bytes to a file 
				final Thread threadWriteFile = new Thread(){
					@Override
					public void run() {
						super.run();
						
						FileOutputStream fos = null;
						try {
							fos = new FileOutputStream(file);
						} catch (FileNotFoundException e1) {
							e1.printStackTrace();
						};
						
						while (true) {
							synchronized (mutex) {
								if (!continueReading && !buffer.isEmpty())
									break;
							}
							try {
								System.out.println("ola");
								fos.write(buffer.take());
								
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}			
						}
						
						try {
							fos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						
					}
				};
		
		//Thread 3 -> receive data and write the bytes in a buffer.
		Thread threadWriteBuffer = new Thread() {
			@Override
			public void run(){
				super.run();
				
				try {
					System.out.println("New connection with client " + clientSocker.getInetAddress().getHostAddress());
					DataInputStream dis = new DataInputStream(clientSocker.getInputStream());
					byte[] b = new byte[block_size];
					do
					{
						int read = 0;
						synchronized (mutex) {
							read = dis.read(b);
							if (read == 0)
								continueReading = false;
						}
						if (read > 0) {
							buffer.put(b);
						}
						else
							break;
					}
					while (true);
					
					
					serverSocket.close();
					clientSocker.close();
				} catch (IOException e) {
					System.out.println("Fail to access port.");
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
		threadWriteBuffer.start();
		threadWriteFile.start();
		
		while (threadWriteBuffer.isAlive() || threadWriteFile.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void readBytesToBuffer(InputStream is) {
		Scanner scanner = new Scanner(is);
		byte[] sequence_bytes = new byte[block_size];
		
		for (int i = 0; scanner.hasNextByte(); i++) {
			sequence_bytes[i] = scanner.nextByte();
		}
		
		try {
			buffer.put(sequence_bytes);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		scanner.close();
	}
}
