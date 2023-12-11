import java.io.*;
import java.net.*;
import mma.MessageFormat;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;

 public class peer{
  public static void main(String[] args) {
    peer p = new peer(args);
  }

  public peer(String []args)
  {
    Init(args);
  }
  public void Init(String []args){
    try{
    int port;
    String sharedDir;
    Properties prop = new Properties();
    int peer_id = Integer.parseInt(args[1]);
    sharedDir = args[2];
    String fileName = args[0];
    InputStream is = new FileInputStream(fileName);
    prop.load(is);
    port = CLIENT_LISTENING_PORT;
    new Thread(new DownloadRequestsHandlerRunnable(port, sharedDir)).start();
    RegisterAndQueryServers(peer_id,prop, args[2]);
        }catch (IOException io) {
            io.printStackTrace();
        }
  }
  public void RegisterAndQueryServers(int id, Properties prop, String folder){
    String server = prop.getProperty("peer" + id + ".server");
    if (server != null) {
            register(server, SERVER_LISTENING_PORT, folder);   
    }
  }
    private static final int CLIENT_LISTENING_PORT = 9877;
    private static final int SERVER_LISTENING_PORT = 1234;
        public void register(String ip, int port, String dir) {
            try{
                Socket socket = new Socket(ip, port);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                Scanner scanner = new Scanner(System.in);

                File folder = new File("./"+dir);
                File[] listOfFiles = folder.listFiles();

                if (listOfFiles != null) {
                    for (File file: listOfFiles) {
                        if (file.isFile()) {
                            writer.println("register");
                            writer.println(file.getName());
                            System.out.println("Server "+ ip+ " responded: " + reader.readLine());
                        }

                    }
                }
                while (true) {
                    System.out.println("Enter the name of the file to search for or 'exit' to quit:");
                    String fileName = scanner.nextLine();
    
                    if ("exit".equalsIgnoreCase(fileName.trim())) {
                        System.out.println("Exiting...");
                        System.exit(0);
                    }
                    writer.println("search " + fileName);
                    String response = reader.readLine();
                    System.out.println("Server responded: " + response);
    
                    if (response.contains("IPs containing")) {
                        System.out.println("Enter the IP from the list to obtain the file or 'cancel' to cancel:");
                        String ipaddr = scanner.nextLine();
    
                        if (!"cancel".equalsIgnoreCase(ipaddr)) {
                            obtain(ipaddr, CLIENT_LISTENING_PORT, fileName,dir);
                        }
                    }
                }

            } catch (IOException e) {
                System.out.println("Exception in Register With Server: " + e.getMessage());
            }
        }




private String getFiletodownload() {
    System.out.println("Enter file to be downloaded");
    return new Scanner(System.in).nextLine();
}



    public static class DownloadRequestsHandlerRunnable implements Runnable {
        int port;
        String folder;
        public DownloadRequestsHandlerRunnable(int port,String folder) {
            this.port = port;
            this.folder = folder;
        }

        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    Socket socket = serverSocket.accept();
                    new Thread(new DownloadRunnable(socket, folder)).start();
                }
            } catch (IOException e) {
                System.out.println("Exception in ClientResponderRunnable: " + e.getMessage());
            }
        }
    }

    public static class DownloadRunnable implements Runnable {
        Socket socket;
        String folder;
        public DownloadRunnable(Socket socket,String folder) {
            this.socket = socket;
            this.folder = folder;
        }
        @Override
        public void run() {
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                String fileName = in .readUTF();

                File file = new File(folder + "/" + fileName);
                if (file.exists() && !file.isDirectory()) {
                    out.writeLong(file.length());
                    try (FileInputStream fileIn = new FileInputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = fileIn.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                } else {
                    out.writeLong(-1);
                }
            } catch (IOException e) {
                System.out.println("Error in FileHandler: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
 public static void obtain(String ip, int port, String fileName,String folder) {
    Socket socket = null;
    DataInputStream in = null;
    DataOutputStream out = null;
    try {
        socket = new Socket(ip, port); in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        out.writeUTF(fileName);
        System.out.println("Connected to " + ip + " at port " + port + " to get " + fileName);

        long fileLength = in .readLong();
        if (fileLength < 0) {
            System.out.println("File not found on the server.");
            return;
        }

        File receivedFile = new File(folder+ "/" + fileName);
        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(receivedFile);
            byte[] buffer = new byte[4096];
            long remaining = fileLength;
            int read;

            while ((read = in .read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
                fileOut.write(buffer, 0, read);
                remaining -= read;
            }
        } catch (IOException e) {
            System.out.println("Error in file transmission: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (fileOut != null) {
                try {
                    fileOut.close();
                } catch (IOException e) {
                    System.out.println("Error in saving the obtained file: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        System.out.println("File received and saved to: " + receivedFile.getAbsolutePath());

    } catch (IOException e) {
        System.out.println("Error obtaining file: " + e.getMessage());
    } finally {
        try {
            if (out != null) out.close();
            if ( in != null) in .close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Error closing connection to other client: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
  
}
