import qfs.common.Block;
import qfs.receiver.FileReceiver;

public class Receive {

	public static void main(String[] args) {
		String file = null;
		int port = -1, 
			nJobs = 1, 
			block_size = Block.getBlockSize(), 
			queue_size = 100;
		boolean shared_con = false; 
		
		if (args.length == 0) {
			System.out.println("Usage: qfs-receive FILE -p port [-b block_size] [-q queue_size] [-c number_connections] [-jN] [--help]");
		} else {
			for (int i = 0; i < args.length; i++) {
				if (args[i].startsWith("-")) {
					if(args[i].equals("-p")) {
							if (i == args.length - 1) {
								System.out.println("Missing or invalid port");
								return;
							} else {
								try {
									port = Integer.parseInt(args[++i]);
								} catch (NumberFormatException e) {
									System.out.println("Invalid port: " + args[i]);
									return;
								}
							}
					} else if (args[i].startsWith("-j") && args[i].length() > 2) {
						try {
							nJobs = Integer.parseInt(args[i].substring(2, args[i].length()));
						} catch (NumberFormatException e) {
							System.out.println("Invalid number of threads: " + args[i]);
							return;							
						}
					} else if (args[i].equals("-q")) { 
						if (i == args.length - 1){
							System.out.println("Missing or invalid queue size");
							return;
						} else {
							try {
								queue_size = Integer.parseInt(args[++i]);
							} catch (NumberFormatException e) {
								System.out.println("Invalid number format at queue_size.");
								return;
							}
						}
					
					} else if (args[i].equals("-b")) {
						if (i == args.length - 1){
							System.out.println("Missing or invalid block size");
							return;
						} else {
							try {
								block_size = Integer.parseInt(args[++i]);
								Block.SetBlockSize(block_size);
							} catch (NumberFormatException e) {
								System.out.println("Invalid number format at block_size.");
								return;
							}
						}
					}  else if (args[i].equals("-s")) {
						shared_con = true;
					} else if (args[i].equals("--help")) {
						System.out.println("Command to prepare computer for receiving file through TCP:");
						System.out.println("");
						System.out.println("FILE: name of the output file.");
						System.out.println("-p N: N is the port number.");
						System.out.println("-b N: N is the block size.");
						System.out.println("-q N: N is the queue size.");
						System.out.println("-s: one connection per thread.");
						System.out.println("-jN: N ir the number of threads to receive.");
						return;
					}
				} else {
					if (file == null) {
						file = args[i];
					} else {
						System.out.println("Two files specified. qfs Only supports one file per time");
						return;
					}
				}
			}
		}
		FileReceiver fr = new FileReceiver(file);
		fr.receive(port, nJobs, block_size, queue_size, shared_con);
	}
}
