

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

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
		 * qfs-send FILE [-d destination -p port] [-jN] [--help]
		 * Where N is the number of threads to send the file 
		 */
		String file = null, destination = null;
		int port = -1, threads = 1;
		if (args.length == 0)
			System.out.println("Usage: qfs-send FILE [-d destination -p port] [-jN] [--help]");
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
					//TODO: Print help
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
			FileSender fs = new FileSender(file, threads);
			fs.send(destination, port);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
