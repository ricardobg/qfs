package qfs.sender;

import java.io.File;
import java.nio.Buffer;
import java.util.concurrent.ArrayBlockingQueue;

public class FileSender {
	private File file;
	private ArrayBlockingQueue queue;
	private int queue_size = 100;
	public FileSender(String file_path) throws Exception {
		file = new File(file_path);
		if (!file.exists()) {
			throw new Exception("File '" + file_path + "' not found");
		}
		queue = new ArrayBlockingQueue<Byte[]>(queue_size);
		//Buffer fifo = BufferUtils.synchronizedBuffer(new CircularFifoBuffer());
	}
}
