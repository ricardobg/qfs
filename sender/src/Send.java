import qfs.common.Block;
import qfs.sender.FileSender;

/**
 * Class to implement user interaction
 * @author ricardo
 *
 */
public class Send {
	public static void main(String[] args) {
		/*
		 * How to call this program:
		 * qfs-send FILE -d destination -p port [-jN] [-b block_size] [-q queue_size] [--help]
		 * Where N is the number of threads to send the file 
		 */
		String file = null, destination = null;
		int port = -1, threads = 1, block_size = Block.getBlockSize(), queue_size = 100;
		if (args.length == 0)
			System.out.println("Usage: qfs-send FILE -d destination -p port [-b block_size] [-q queue_size] [-jN] [--help]");
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-")) {
				//Command arguments
				switch (args[i]) {
					case "-d":
						if (i == args.length - 1) {
							System.out.println("Missing destination");
							return;
						}
						destination = args[++i];
						continue;
					case "-p":
						if (i == args.length - 1) {
							System.out.println("Missing port");
							return;
						}
						try {
							port = Integer.parseInt(args[++i]);
						}
						catch (NumberFormatException e) {
							System.out.println("Invalid port: '" + args[i]);
							return;
						}
						continue;
					case "-b":
						if (i == args.length - 1) {
							System.out.println("Missing block size");
							return;
						}
						try {
							block_size = Integer.parseInt(args[++i]);
						}
						catch (NumberFormatException e) {
							System.out.println("Invalid Block size: '" + args[i]);
							return;
						}
						continue;
					case "-q":
						if (i == args.length - 1) {
							System.out.println("Missing queue size");
							return;
						}
						try {
							queue_size = Integer.parseInt(args[++i]);
						}
						catch (NumberFormatException e) {
							System.out.println("Invalid Queue size: '" + args[i]);
							return;
						}
						continue;
				}
				if (args[i].startsWith("-j") && args[i].length() > 2) {
					try {
						threads = Integer.parseInt(args[i].substring(2));
					}
					catch (Exception e) {
						System.out.println("Invalid number of threads: '" + args[i]);
						return;
					}
				}
				 else if (args[i].equals("--help")) {
						System.out.println("Command to send file through TCP:");
						System.out.println("Usage: qfs-send FILE -d destination -p port [-b block_size] [-q queue_size] [-jN] [--help]");
						System.out.println("FILE: name of the input file.");
						System.out.println("-d DEST: DEST is the destination.");
						System.out.println("-p PORT: PORT is the port number of the receiver.");
						System.out.println("-jN: N ir the number of threads to send.");
						System.out.println("-b SIZE: SIZE is the size of the block.");
						System.out.println("-q SIZE: SIZE is the maximum queue size.");
						return;
					}
				else {
					System.out.println("Invalid option: '" + args[i] + "'" );
					return;
				}
			}
			else {
				//File
				if (file == null) {
					file = args[i];
				}
				else {
					System.out.println("Two files specified. qfs Only supports one file per time");
					return;
				}
			}
		}
		if (file == null) {
			System.out.println("Error: No input file.");
			return;
		}
		if (destination == null) {
			System.out.println("Specify the destination");
			return;
		}
		if (port == -1) {
			System.out.println("Specify the port");
			return;
		}
		
		// send the file
		try {
			FileSender fs = new FileSender(file, queue_size, block_size);
			fs.send(destination, port, threads);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
