package qfs.common;

public class Block {
	public final static int BLOCK_SIZE = 5;
	public Block(byte[] bytes, int size, int id) {
		if (size <= 0) {
			this.bytes = new byte[0];
			this.size = 0;
			this.id = -1;
		}
		else {
			this.bytes = new byte[size];
			for (int i = 0; i < size; i++)
				this.bytes[i] = bytes[i];
			this.id = id;
			this.size = size;
		}
	}
	
	public Block() {
		this(null, 0, 0);
	}
	
	public boolean finished() {
		return this.size == 0;
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public int getId() {
		return id;
	}
	
	public int getSize() {
		return size;
	}
	
	private byte[] bytes;
	private int id;
	private int size;
}
