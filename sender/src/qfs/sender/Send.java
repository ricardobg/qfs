package qfs.sender;

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
	public static final String CONF_FILE = ".qfs-sender-conf";
	public static void main(String[] args) {
		/*
		 * How to call this program:
		 * qfs-send FILE [-d destination -p port] [-jN] [--help]
		 * When you send a file, it saves the destination and port in a .qfs- file
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
		
		//Try to read default
		File file_conf = new File(System.getProperty("user.home") + CONF_FILE);
		if (!file_conf.exists()) {
			if (destination == null) {
				System.out.println("Specify the destination");
				return;
			}
			if (port == -1) {
				System.out.println("Specify the port");
				return;
			}
		}
		else if (destination == null || port == -1){
			try { 
				List<String> lines = Files.readAllLines(Paths.get(file_conf.getAbsolutePath()), Charset.defaultCharset());
				if (destination == null)
					destination = lines.get(0);
				if (port == -1)
					port = Integer.parseInt(lines.get(1));
			}
			catch (FileNotFoundException e) {
				//Shouldn't happen
				System.out.println("Unexpected error: File " + CONF_FILE + " not found");
				return;
			} catch (Exception e) {
				System.out.println(e.getMessage());
				return;
			}
		}
		//Writes new configuration
		try {
			PrintWriter writer = new PrintWriter(file_conf.getAbsolutePath(), "UTF-8");
			writer.println(destination);
			writer.println(port);
			writer.close();
		} catch (IOException e) {
			System.out.println("erro: " + e.getMessage());
			return;
		}
	}
}
