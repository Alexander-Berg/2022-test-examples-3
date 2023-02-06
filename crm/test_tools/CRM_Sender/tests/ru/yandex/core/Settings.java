package ru.yandex.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by nasyrov on 12.03.2016.
 */
public class Settings {

    protected static final Properties configProp = new Properties();
    private static String oraString = null;

    static {
        try {
            InputStream in = new FileInputStream("app.properties");
            configProp.load(in);
            System.setProperty("oracle.net.tns_admin", configProp.getProperty("oracle.tns"));
            oraString = configProp.getProperty("oracle.connectionString");
        } catch (IOException e) {
            System.err.print("Properties loading was failed");
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return String.valueOf(configProp.get(key));
    }

    public static Integer getInt(String key) {
        return Integer.parseInt((String) configProp.get(key));
    }

    public static String getOraString() {
        return oraString;
    }
}
