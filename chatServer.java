import java.io.*;
import java.net.*;
import java.util.*;

public class chatServer {
    private static final int PORT = 12345; // 服务器监听的端口号
    private Set<PrintWriter> clientWriters = new HashSet<>(); // 存储所有客户端输出流，以便消息可以广播到每个客户端
    private Map<String, PrintWriter> userWriters = new HashMap<>();// 存储所有客户端输出流，以便消息可以广播到每个客户端

    public void startServer() {
        System.out.println("Chat Server is running...");
        // 创建服务器Socket并绑定到特定的IP地址
        try (ServerSocket serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("192.168.1.82"));) { // 创建服务器Socket监听指定端口
            while (true) {
                System.out.println("Waiting for client connection...");
                new Handler(serverSocket.accept()).start(); // 接受客户端连接，为每个连接创建一个新的线程
                System.out.println("Client connected");
            }
        } catch (IOException e) {
            System.out.println("Exception in Chat Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private class Handler extends Thread {
        // 用于处理客户端连接的线程
        private Socket socket;
        // 用于向客户端发送数据
        private PrintWriter writer;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            // 设定一个null的用户名
            String userName = null;
            try {
                // InputStream 用于从客户端读取数据
                InputStream input = socket.getInputStream();
                // OutputStream 用于向客户端发送数据
                OutputStream output = socket.getOutputStream();
                // 从输入流中读取数据
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                // 创建一个PrintWriter对象，用于向客户端发送数据
                PrintWriter writer = new PrintWriter(output, true);
                userName = reader.readLine(); // 读取用户名, 从客户端发送的第一行数据
                synchronized (userWriters) {
                    // 检查用户名是否已存在
                    if (userWriters.containsKey(userName)) {
                        writer.println(
                                "ERROR: Username '" + userName + "' is already taken. Please choose a different name.");
                        return; // 结束这个线程，因为用户名已被占用
                    } else {

                        // 广播新用户加入的消息
                        for (PrintWriter clientWriter : clientWriters) {
                            clientWriter.println("NOTICE: " + userName + " has joined the chat.");
                        }
                        // 将当前用户列表发送给新用户
                        writer.println(
                                "Current users in the chat: " + String.join(", ", userWriters.keySet())
                                        + "and YOU\nYou can send private message by typing @username message\n");
                        // 用户名未被占用，加入聊天室,先输出其他的用户名,不用输出自己的用户名
                        userWriters.put(userName, writer);
                    }
                }
                // 将PrintWriter对象存储到Set集合中
                synchronized (clientWriters) {
                    clientWriters.add(writer);
                }
                // 从客户端读取信息
                String message;
                while ((message = reader.readLine()) != null) {
                    // 打印从客户端接收到的消息
                    System.out.println("Received: " + message); // 确保服务器接收到消息
                    boolean isPrivateMessage = message.contains("@");// 判断是否为私聊,私聊格式为: @用户名 消息
                    if (isPrivateMessage) {
                        // 找到分隔符的位置 :@, 分隔符to blank的内容就是私聊的用户名
                        int splitIndex = message.indexOf(":@");
                        // 找到空格的位置, 空格后面的内容就是私聊的消息
                        int blankIndex = message.indexOf(" ");
                        if (splitIndex >= 0) {
                            // 获取私聊的用户名
                            String recipient = message.substring(splitIndex + 2, blankIndex);
                            System.out.println("Recipient: " + recipient);
                            // 获取私聊的消息
                            String privateMessage = message.substring(splitIndex + 1);
                            // 获取私聊的用户的输出流
                            PrintWriter recipientWriter = userWriters.get(recipient);
                            if (recipientWriter != null) {
                                recipientWriter.println(userName + " (private): " + privateMessage);
                            }
                        }
                    } else {
                        synchronized (clientWriters) {
                            // 广播消息
                            for (PrintWriter clientWriter : clientWriters) {
                                clientWriter.println(message); // 广播消息
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Exception in Handler: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // 用户离开处理逻辑
                if (userName != null) {
                    System.out.println(userName + " has left.");
                    userWriters.remove(userName);
                    synchronized (clientWriters) {
                        clientWriters.remove(writer);
                    }
                    // 广播用户离开的消息
                    for (PrintWriter clientWriter : clientWriters) {
                        clientWriter.println("NOTICE: " + userName + " has left the chat.");
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

    }

    public static void main(String[] args) {
        new chatServer().startServer(); // 启动服务器
    }
}
