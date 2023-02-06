package ru.yandex.core;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MailProvider {

    private static Map<String,String> stageMessageIds;

    static {
        stageMessageIds = new HashMap<>();
    }

    public static RawMailSender mailSender() {
        return new RawMailSender(
                Settings.get("smtp.host"),
                Settings.getInt("smtp.port"),
                Settings.get("smtp.rcpt_to"),
                //Settings.get("smtp.prod_rcpt_to"),
                Settings.get("smtp.mail_from")
        );
    }

    @BeforeClass
    public static void beforeClass() throws IOException {
        loadMessageIdStage();
    }


    @AfterClass
    public static void afterClass() throws IOException {
        saveMessageIdStage();
    }

    public static String newMessageId() {
        return "<" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date()) + "@tester.crm.yandex.ru>";
    }
    public static String newXuid() {
        return new SimpleDateFormat("MMddHHmmss").format(new Date());
    }

    protected static void saveMessageIdStage() throws IOException {
        Properties properties = new Properties();
        properties.putAll(stageMessageIds);
        properties.store(new FileOutputStream("stage-messageid.txt"), null);
    }


    protected static void loadMessageIdStage() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("stage-messageid.txt"));

        stageMessageIds = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            stageMessageIds.put(key, properties.get(key).toString());
        }
    }

    protected void putStageMessageId(String key, String messageId) {
        stageMessageIds.put(key, messageId);
    }

    protected String getStageMessageId(String key) {
        if (stageMessageIds.containsKey(key)) {
            return stageMessageIds.get(key);
        } else {
            System.err.println("Stage key not found. key: " + key);
            Object o = null; o.toString(); // HACK так делать нельзя, но надо вызвать ошибку
            return null;
        }
    }

    public static String newClientLogin() {
        return new SimpleDateFormat("yyyyMMddHHmms sSSS").format(new Date()) + "-login";
    }




}
