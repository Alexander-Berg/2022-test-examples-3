package ru.yandex.core;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by nasyrov on 10.03.2016.
 */
public class RawMailSender {

    private int port;
    private String host;
    private String to;
    private String from;

    public RawMailSender(String host, int port, String to, String from) {
        this.port = port;
        this.host = host;
        this.to = to;
        this.from = from;
    }

    public String sendFile(String path, String messageId, String ClientLogin, String Xuid) throws IOException {
        System.out.format("Sending: '%s'", path);
        System.out.println();
        String raw = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        sendMail(raw, messageId, ClientLogin, Xuid);

        return messageId;
    }

    public void sendMail(String raw, String messageId, String ClientLogin, String Xuid) throws IOException {
        Socket socket = null;
        PrintWriter writer;
        BufferedReader reader;

        try {
            socket = new Socket(host, port);
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            Action dialog = (line) -> {
                writer.println(line);
                writer.flush();
                System.out.println("> " + line);
                System.out.println("< " + reader.readLine());
            };

            dialog.run("HELO tester.crm.yandex.ru");
            System.out.println("< " + reader.readLine());
            dialog.run("MAIL FROM: " + from);
            dialog.run("RCPT TO: " + to);
            dialog.run("DATA");

            if (messageId != null)
                raw = replaceMessageId(raw, messageId);
            if (ClientLogin != null)
                raw = replaceClientLogin(raw, ClientLogin);
            if (Xuid != null)
                raw = replaceXuid(raw, Xuid);
            writer.print(raw);

            writer.println();
            writer.println(".");
            dialog.run("quit");


            writer.close();
            reader.close();
        } catch (IOException e) {
            System.err.print(e);
        } finally {
            socket.close();
        }
    }

    private static String replaceMessageId(String raw, String messageId) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader bufReader = new BufferedReader(new StringReader(raw));
        String line=null;
        while ((line=bufReader.readLine()) != null) {
            if (line.toLowerCase().startsWith("message-id: replacer")) {
                sb.append("message-id: " + messageId);
                System.out.println("message-id replaced");
            }
            else {
                sb.append(line);
            }
            sb.append("\r\n");
        }

        return sb.toString();
    }

    private static String replaceXuid(String raw, String xuid) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader bufReader = new BufferedReader(new StringReader(raw));
        String line=null;
        while ((line=bufReader.readLine()) != null) {
            if (line.toLowerCase().startsWith("x-user-uid: replacer")) {
                sb.append("x-user-uid: " + xuid);
                System.out.println("x-user-uid replaced");
            }
            else {
                sb.append(line);
            }
            sb.append("\r\n");
        }

        return sb.toString();
    }

    private static String replaceClientLogin(String raw, String ClientLogin) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader bufReaderLogin = new BufferedReader(new StringReader(raw));
        String line=null;
        while ((line=bufReaderLogin.readLine()) != null) {
            if (line.toLowerCase().startsWith("x-otrs-login: replacer")) {
                sb.append("x-otrs-login: " + ClientLogin);
                System.out.println("ClientLogin replaced");
            }
            else {
                sb.append(line);
            }
            sb.append("\r\n");
        }

        return sb.toString();
    }

    private interface Action {
        void run(String input) throws IOException;
    }
}
