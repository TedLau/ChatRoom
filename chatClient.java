import java.io.*;
import java.net.*;

public class chatClient {
    // 服务器地址和端口
    private static final String SERVER_ADDRESS = "192.168.1.82"; // 服务器地址
    private static final int SERVER_PORT = 12345; // 与服务器端相同的端口
    // 启动客户端

    public void startClient(String userName) {
        // 创建一个新的Socket连接到服务器
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            System.out.println("Attempting to get streams...");
            // 获取输入输入流, 用于从服务器读取数据,结合下面的reader.readLine()方法
            InputStream input = socket.getInputStream();
            System.out.println("Input stream obtained.");
            // 获取输出流, 用于向服务器发送数据, 结合下面的writer.println()方法
            OutputStream output = socket.getOutputStream();
            System.out.println("Output stream obtained.");
            // 创建一个BufferedReader对象，用于从服务器读取数据
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            // 创建一个PrintWriter对象，用于向服务器发送数据
            PrintWriter writer = new PrintWriter(output, true);
            System.out.println("Writer created");
            // 发送用户名到服务器
            writer.println(userName);
            System.out.println("Welcome, " + userName + "!");
            // 创建一个 新线程 来监听从服务器接收到的消息
            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    // 从服务器读取消息
                    while ((serverMessage = reader.readLine()) != null) {
                        // 如果消息是由当前客户端发送的，则不打印
                        if (serverMessage.startsWith("<" + userName + ">:"))
                            continue;
                        // 打印从服务器接收到的消息
                        System.out.println(serverMessage); // 打印从服务器接收到的消息
                    }
                } catch (IOException e) {
                    System.out.println("Error reading from server: " + e.getMessage());
                }
            });
            listenerThread.start();
            // 从控制台读取用户输入并发送到服务器
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String userInput;
            while ((userInput = consoleReader.readLine()) != null) {
                writer.println("<" + userName + ">:" + userInput); // 确保这行代码能执行，以发送消息到服务器
            }
        } catch (UnknownHostException e) {
            System.out.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Syntax: java ChatClient <username>");
            return;
        }
        new chatClient().startClient(args[0]); // 启动客户端
    }
}
