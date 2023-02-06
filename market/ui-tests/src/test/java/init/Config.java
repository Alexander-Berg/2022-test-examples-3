package init;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class Config {

    private static final Properties PROPERTIES = new Properties();

    private static FileInputStream fileInputStream;
    private static String mainUserLogin;
    private static String mainUserPass;
    private static String projectURL;
    private static String gridURL;
    private static String gridBrowser;
    private static String gridBrowserVersion;
    private static boolean useGrid;


    private static boolean freeConfig = true;

    private static String getProperty(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            throw new Error("В файле конфига не найден ключ " + key);
        }
        return value;
    }

    public static String getMainUserLogin() {
        return mainUserLogin;
    }

    public static String getMainUserPass() {
        return mainUserPass;
    }

    public static String getProjectURL() {
        return projectURL;
    }

    public static String getGridURL() {
        return gridURL;
    }

    public static String getGridBrowser() {
        return gridBrowser;
    }

    public static String getGridBrowserVersion() {
        return gridBrowserVersion;
    }

    public static boolean getUseGrid() {
        return useGrid;
    }

    /**
     * Прочитать файл конфигураций
     */
    public static void readFileConfig() {
        if (!freeConfig) {
            return;
        }
        freeConfig = false;

        File fileConfig = new File("resources/config.properties");
        if (!fileConfig.exists()) {
            throw new Error("В папке resources нет файла с конфигураций config.properties");
        }

        try {
            // Указание пути до файла с настройками
            fileInputStream = new FileInputStream("resources/config.properties");
            PROPERTIES.clear();
            PROPERTIES.load(fileInputStream);
        } catch (IOException e) {
            e.printStackTrace();
            // Обработка возможного исключения (нет файла и т.п.)
        } finally {
            if (fileInputStream != null)
                try {
                    fileInputStream.close();
                } catch (IOException ignored) {
                }
        }


        mainUserLogin = getProperty("main_user_login");
        mainUserPass = getProperty("main_user_pass");
        projectURL = getProperty("project_url");
        gridURL = getProperty("grid_url");
        gridBrowser = getProperty("grid_browser_name");
        gridBrowserVersion = getProperty("grid_browser_version");
        useGrid = getProperty("use_grid").equals("true");
    }
}
