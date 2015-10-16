

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
		int port = -1;
		if (args.length == 0)
			System.out.println("Usage: qfs-send FILE [-d destination -p port] [-jN] [--help]");
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-")) {
				//Command arguments
				if (args[i].equals( "-d")) {
					if (i == args.length - 1) {
						System.out.println("Missing destination");
						return;
					}
					destination = args[++i];
				}
				else if (args[i].equals("-p")) {
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
				}
				else if (args[i].startsWith("-j") && args[i].length() > 2) {
					
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
	}
}
