import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

public class Client {
    private static JTextPane chatPane; // 聊天面板，用于显示消息
    private static JTextField inputField; // 输入字段，用于输入消息
    private static PrintWriter out; // 输出流，用于发送消息
    private static Socket socket; // 服务器套接字
    private static StyledDocument doc; // 文档对象，用于管理文本样式

    public static void main(String[] args) {
        JFrame frame = new JFrame("客户端"); // 创建客户端窗口
        frame.setSize(800, 800); // 设置窗口大小
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 设置关闭操作

        chatPane = new JTextPane(); // 初始化聊天面板
        chatPane.setEditable(false); // 设置聊天面板不可编辑
        chatPane.setFont(new Font("Arial", Font.PLAIN, 18)); // 设置字体
        doc = chatPane.getStyledDocument(); // 获取文档对象
        JScrollPane scrollPane = new JScrollPane(chatPane); // 创建滚动面板

        inputField = new JTextField(); // 初始化输入字段
        JButton sendButton = new JButton("发送"); // 创建发送按钮
        JButton sendImageButton = new JButton("发送图片"); // 创建发送图片按钮

        sendButton.addActionListener(e -> sendMessage()); // 为发送按钮添加监听器
        inputField.addActionListener(e -> sendMessage()); // 为输入字段添加监听器
        sendImageButton.addActionListener(e -> sendImage()); // 为发送图片按钮添加监听器

        JPanel panel = new JPanel(new BorderLayout()); // 创建面板
        panel.add(inputField, BorderLayout.CENTER); // 添加输入字段到面板
        panel.add(sendButton, BorderLayout.EAST); // 添加发送按钮到面板
        panel.add(sendImageButton, BorderLayout.WEST); // 添加发送图片按钮到面板

        frame.add(scrollPane, BorderLayout.CENTER); // 添加滚动面板到窗口
        frame.add(panel, BorderLayout.SOUTH); // 添加面板到窗口

        frame.setVisible(true); // 设置窗口可见

        startClient(); // 启动客户端
    }

    private static void startClient() {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 12345); // 连接服务器
                addMessage("已连接到服务器！", false); // 添加连接消息

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // 初始化输入流
                out = new PrintWriter(socket.getOutputStream(), true); // 初始化输出流
                DataInputStream dataIn = new DataInputStream(socket.getInputStream()); // 初始化数据输入流
                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream()); // 初始化数据输出流

                String receivedMessage;
                while ((receivedMessage = in.readLine()) != null) { // 读取消息
                    if (receivedMessage.equals("IMAGE")) {
                        receiveImage(dataIn); // 接收图片
                    } else {
                        addMessage(receivedMessage, false); // 添加消息
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "服务器未启动或断开，请检查服务器端是否启动。", "连接错误", JOptionPane.ERROR_MESSAGE); // 显示错误信息
                System.exit(1); // 退出客户端
            }
        }).start(); // 启动线程
    }

    private static void sendMessage() {
        String message = inputField.getText(); // 获取输入的消息
        if (!message.isEmpty()) { // 如果消息不为空
            out.println(message); // 发送消息
            addMessage(message, true); // 添加消息到聊天面板
            inputField.setText(""); // 清空输入字段
        }
    }

    private static void sendImage() {
        JFileChooser fileChooser = new JFileChooser(); // 创建文件选择器
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) { // 如果选择了文件
            File file = fileChooser.getSelectedFile(); // 获取选中的文件
            try {
                out.println("IMAGE"); // 发送图片标识
                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream()); // 初始化数据输出流
                BufferedImage image = ImageIO.read(file); // 读取图片
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); // 创建字节数组输出流
                ImageIO.write(image, "jpg", byteArrayOutputStream); // 将图片写入字节数组输出流
                byte[] size = ByteBuffer.allocate(4).putInt(byteArrayOutputStream.size()).array(); // 获取图片大小
                dataOut.write(size); // 发送图片大小
                dataOut.write(byteArrayOutputStream.toByteArray()); // 发送图片数据
                addImage(image, true); // 添加图片到聊天面板
            } catch (IOException e) {
                e.printStackTrace(); // 捕获并打印异常
            }
        }
    }

    private static void receiveImage(DataInputStream dataIn) {
        try {
            byte[] sizeAr = new byte[4]; // 创建字节数组
            dataIn.readFully(sizeAr); // 读取图片大小
            int size = ByteBuffer.wrap(sizeAr).asIntBuffer().get(); // 获取图片大小
            byte[] imageAr = new byte[size]; // 创建字节数组
            dataIn.readFully(imageAr); // 读取图片数据
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageAr)); // 读取图片
            addImage(image, false); // 添加图片到聊天面板
        } catch (IOException e) {
            e.printStackTrace(); // 捕获并打印异常
        }
    }

    private static void addMessage(String message, boolean isSent) {
        try {
            Style style = chatPane.addStyle("Message Style", null); // 创建样式
            StyleConstants.setFontSize(style, 14); // 设置字体大小
            StyleConstants.setFontFamily(style, "Arial"); // 设置字体

            if (isSent) {
                StyleConstants.setForeground(style, Color.BLUE); // 设置发送消息的颜色
                StyleConstants.setAlignment(style, StyleConstants.ALIGN_RIGHT); // 设置发送消息的对齐方式
            } else {
                StyleConstants.setForeground(style, Color.BLACK); // 设置接收消息的颜色
                StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT); // 设置接收消息的对齐方式
            }

            doc.insertString(doc.getLength(), message + "\n", style); // 插入消息

            int length = doc.getLength(); // 获取文档长度
            doc.setParagraphAttributes(length - message.length() - 1, length, style, false); // 设置段落属性

            chatPane.setCaretPosition(doc.getLength()); // 设置插入符位置
        } catch (BadLocationException e) {
            e.printStackTrace(); // 捕获并打印异常
        }
    }

    private static void addImage(BufferedImage image, boolean isSent) {
        Style style = chatPane.addStyle("Image Style", null); // 创建样式
        StyleConstants.setAlignment(style, isSent ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT); // 设置对齐方式

        chatPane.setCaretPosition(chatPane.getDocument().getLength()); // 设置插入符位置
        chatPane.insertIcon(new ImageIcon(image)); // 插入图片

        try {
            doc.insertString(doc.getLength(), "\n", null); // 插入换行
        } catch (BadLocationException e) {
            e.printStackTrace(); // 捕获并打印异常
        }

        int length = doc.getLength(); // 获取文档长度
        doc.setParagraphAttributes(length - 1, length, style, false); // 设置段落属性
    }
}
