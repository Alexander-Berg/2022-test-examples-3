package ui_tests.src.test.java.unit;

import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;


public class Config {

    private static final Properties PROPERTIES = new Properties();

    private static FileInputStream fileInputStream;
    public static int DEF_TIME_WAIT_LOAD_PAGE;
    //Флаг показывающий что дополнительный пользователь не занят тестом
    public static boolean freeAdditionalUser = true;
    public static boolean isReceivedEmails = false;
    public static HashMap<String, String> emails = new HashMap<>();

    private static String mainUserLogin;
    private static String mainUserPass;
    private static String additionalUserLogin;
    private static String additionalUserPass;
    private static String projectURL;
    private static String outgoingMailUserEmail;
    private static String outgoingMailPass;
    private static String seleniumGridUrl;
    private static String seleniumGridBrowserName;
    private static String seleniumGridBrowserVersion;
    private static Boolean useSeleniumGrid;


    private static boolean freeConfig = true;


    public static boolean isRecordVideo() {
        return recordVideo;
    }

    public static void setRecordVideo(boolean recordVideo) {
        Config.recordVideo = recordVideo;
    }

    private static boolean recordVideo;

    private static String getProperty(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            throw new Error("В файле конфига не найден ключ " + key);
        }
        return value;
    }

    public static String getSeleniumGridUrl() {
        return seleniumGridUrl;
    }

    public static String getSeleniumGridBrowserName() {
        return seleniumGridBrowserName;
    }

    public static String getSeleniumGridBrowserVersion() {
        return seleniumGridBrowserVersion;
    }

    public static Boolean getUseSeleniumGrid() {
        return useSeleniumGrid;
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

    public static String getOutgoingMailUserEmail() {
        return outgoingMailUserEmail;
    }

    public static String getOutgoingMailPass() {
        return outgoingMailPass;
    }

    public static String getAdditionalUserLogin() {
        return additionalUserLogin;
    }

    public static String getAdditionalUserPass() {
        return additionalUserPass;
    }

    /**
     * Прочитать файл конфигураций
     */
    public static void readFileConfig() {
        if (!freeConfig) {
            return;
        }
        freeConfig = false;
        String expectedFileConfigVersion;

        File fileConfig = new File("resources/config.properties");
        if (!fileConfig.exists()) {
            throw new Error("В папке resources нет файла с конфигураций config.properties");
        }

        // из файла шаблона конфига читаем актуальную версию файла конфига
        try {
            // Указание пути до файла с настройками
            fileInputStream = new FileInputStream("resources/.config.properties");
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

        expectedFileConfigVersion = getProperty("version");

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

        //Проверка на использование актуальной версии файла конфига. Проверять с версией из файла шаблона.
        if (!getProperty("version").equals(expectedFileConfigVersion)) {
            throw new Error("У вас устаревшая версия файла конфига, обновите его. Актуальную версию можно взять из шаблона - resources/.config.properties " + expectedFileConfigVersion + getProperty("version"));
        }


        mainUserLogin = (getProperty("main_user_login"));
        mainUserPass = (getProperty("main_user_pass"));

        additionalUserLogin = getProperty("additional_user_login");
        additionalUserPass = getProperty("additional_user_pass");

        projectURL = (getProperty("project_url"));

        outgoingMailPass = (getProperty("box_for_outgoing_email_login"));
        outgoingMailUserEmail = (getProperty("box_for_outgoing_email_pass"));

        seleniumGridUrl = getProperty("selenium_grid_url");
        seleniumGridBrowserName = getProperty("selenium_grid_browser_name");
        seleniumGridBrowserVersion = getProperty("selenium_grid_browser_version");
        useSeleniumGrid = getProperty("selenium_grid_use_selenium_grid").equals("true");

        DEF_TIME_WAIT_LOAD_PAGE = Integer.parseInt(getProperty("def_timer"));

        recordVideo = getProperty("recordVideo").equalsIgnoreCase("true");
    }

    /**
     * Создает папку если такой нет, а если есть то удаляет из нее все файлы
     *
     * @param pathToFolder - массив с путями до папок
     */
    public static void deletingFilesFromFolder(String... pathToFolder) {
        File folder;
        for (String path : pathToFolder) {
            folder = new File(path);

            String[] entries1;
            // Проверяем наличие папки folder
            if (!folder.exists()) {
                // Создаем папку folder
                folder.mkdir();
            } else {
                // Получаем список файлов в папке
                entries1 = folder.list();
                // Удаляем все файлы из папки
                for (String s : entries1) {
                    File currentFile = new File(folder.getPath(), s);
                    currentFile.delete();
                }
            }
        }
    }

    /**
     * Получить из БД мапу "code"="email" ящиков сборщиков системы
     *
     * @param webDriver
     */
    public static void getConnectionEmails(WebDriver webDriver) {

        if (!Config.isReceivedEmails) {
            Config.isReceivedEmails = true;
            if (emails.isEmpty()) {
                String emailsFromBD = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def emails = api.db.of('mailConnection').withFilters{\n" +
                        "  eq('enabled',true)\n" +
                        "}.list()\n" +
                        "\n" +
                        "def map=\\\"\\\"\n" +
                        "for (def email:emails){\n" +
                        " map += email.code + ', '+email.title.replaceAll(\\\".*<\\\",\\\"\\\").replaceAll(\\\">.*\\\",\\\"\\\")+';'\n" +
                        "}\n" +
                        "return map");
                String[] listEmails = emailsFromBD.split(";");
                for (String email : listEmails) {
                    if (!email.equals("")) {
                        String[] codeEndEmail = email.split(",");
                        emails.put(codeEndEmail[0], codeEndEmail[1]);
                    }
                }
            }
        }

        while (Config.isReceivedEmails&&emails.isEmpty())
        {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
