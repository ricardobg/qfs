package qfs.common;

public class BytesHolder {
	private final static int BLOCK_SIZE = 10 * 1024;
	public BytesHolder(byte[] bytes, int size, int id) {
		this.bytes = new byte[size];
		for (int i = 0; i < size; i++)
			this.bytes[i] = this.bytes[i];
		this.id = id;
	}
	
	public BytesHolder() {
		this.bytes = new byte[0];
		this.size = 0;
		this.id = -1;
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
