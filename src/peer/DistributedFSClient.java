package peer;

import shared.FileSystemInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class DistributedFSClient {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: DistributedFSClient <peerName> <host> <port>");
            System.exit(1);
        }

        String peerName = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);

        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            FileSystemInterface fs = (FileSystemInterface) registry.lookup(peerName);

            Scanner scanner = new Scanner(System.in);
            System.out.println("Connected to " + peerName + "@" + host + ":" + port);

            while (true) {
                System.out.print(">>> ");
                String input = scanner.nextLine().trim();

                if (input.equals("exit")) break;

                String[] tokens = input.split(" ");
                if (tokens.length == 0) continue;

                switch (tokens[0]) {
                    case "mkdir":
                        if (tokens.length != 2) {
                            System.out.println("Usage: mkdir /path");
                        } else {
                            boolean ok = fs.mkdir(tokens[1]);
                            System.out.println(ok ? "Directory created" : "Failed to create directory");
                        }
                        break;

                    case "mknod":
                        if (tokens.length != 2) {
                            System.out.println("Usage: mknod /file");
                        } else {
                            boolean ok = fs.mknod(tokens[1]);
                            System.out.println(ok ? "File created" : "Failed to create file");
                        }
                        break;

                    case "symlink":
                        if (tokens.length != 3) {
                            System.out.println("Usage: symlink <target> <linkPath>");
                        } else {
                            boolean ok = fs.symlink(tokens[1], tokens[2]);
                            System.out.println(ok ? "Symlink created" : "Failed to create symlink");
                        }
                        break;

                    case "write":
                        if (tokens.length < 3) {
                            System.out.println("Usage: write /file content");
                        } else {
                            String path = tokens[1];
                            String content = input.substring(input.indexOf(path) + path.length()).trim();
                            boolean ok = fs.write(path, content.getBytes());
                            System.out.println(ok ? "Written" : "Write failed");
                        }
                        break;

                    case "read":
                        if (tokens.length != 2) {
                            System.out.println("Usage: read /file");
                        } else {
                            byte[] data = fs.read(tokens[1]);
                            System.out.println(data != null ? new String(data) : "File not found");
                        }
                        break;

                    case "ls":
                        if (tokens.length != 2) {
                            System.out.println("Usage: ls /dir");
                        } else {
                            for (String name : fs.readdir(tokens[1])) {
                                System.out.println("- " + name);
                            }
                        }
                        break;

                    case "locate":
                        if (tokens.length != 2) {
                            System.out.println("Usage: locate /file");
                        } else {
                            String result = fs.locate(tokens[1]);
                            System.out.println("File located at: " + result);
                        }
                        break;

                    case "neighbors":
                        try {
                            List<String> neighs = fs.getNeighbors();
                            if (neighs.isEmpty()) {
                                System.out.println("No neighbors");
                            } else {
                                System.out.println("Neighbors:");
                                for (String n : neighs) {
                                    System.out.println(" - " + n);
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Failed to get neighbors: " + e.getMessage());
                        }
                        break;

                    default:
                        System.out.println("Unknown command. Try: mkdir, mknod, symlink, write, read, ls, locate, neighbors, exit");
                        break;
                }
            }

            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
