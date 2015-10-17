package qfs.common;

public class Block {
	public final static int BLOCK_SIZE = 500;
	public final static int REAL_BLOCK_SIZE = BLOCK_SIZE + 4;
	//Constructor that read id from bytes
	public Block(byte[] bytes, int size) {
		if (size <= 4) {
			this.bytes = new byte[0];
			this.size = 0;
			this.id = -1;
		}
		else {
			this.bytes = new byte[size - 4];

			this.id = bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
			for (int i = 4; i < size; i++)
				this.bytes[i-4] = bytes[i];
			this.size = size - 4;
		}
	}
	//Constructor that put id before bytes
	public Block(byte[] bytes, int size, int id) {
		if (size <= 0) {
			this.bytes = new byte[0];
			this.size = 0;
			this.id = -1;
		}
		else {
			this.size = size + 4;
			this.bytes = new byte[this.size];
			int i = 0;
			this.bytes[i++] = (byte) (id >> 24);
			this.bytes[i++] = (byte) (id >> 16);
			this.bytes[i++] = (byte) (id >> 8);
			this.bytes[i++] = (byte) (id);
			
			for ( ; i < this.size; i++)
				this.bytes[i] = bytes[i-4];
			this.id = id;
			
		}
	}
	
	public Block() {
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
