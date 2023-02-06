package ui_tests.src.test.java.tools;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class File {
    private static String workingDirectory = "target/file/";

    /**
     * Создать файл с текстом
     *
     * @param fileName    имя файла
     * @param contextFile содержимое файла
     * @param encoding    кодировка файла
     * @return
     */
    public String createFile(String fileName, String contextFile, String encoding) {
        java.io.File newFile;
        try {
            if (!new java.io.File(workingDirectory).exists()) {
                new java.io.File(workingDirectory).mkdirs();
            }

            newFile = new java.io.File(workingDirectory + fileName);
            if (!newFile.exists()) {
                newFile.createNewFile();
            }

            PrintWriter out = new PrintWriter(newFile.getAbsoluteFile(), encoding);
            out.print(contextFile);
            out.close();

        } catch (IOException e) {
            throw new Error(e);
        }
        return newFile.getAbsolutePath();
    }

    /**
     * Создать файл с текстом и кодировкой "Windows-1251"
     *
     * @param fileName    - имя файла
     * @param contextFile - содержимое файла
     * @return
     */
    public String createFile(String fileName, String contextFile) {
        return createFile(fileName, contextFile, "Windows-1251");
    }

    /**
     * Скачать файл по ссылке и сохранить в новый файл
     *
     * @param linkOnFile ссылка для скачивания
     * @return
     */
    public String readFileFromTheLink(WebDriver webDriver, String linkOnFile, String encoding) {
        StringBuilder result = new StringBuilder();
        int secondsToWaitForResult = 10;
        String s = "(async function() {\n" +
                "  const URL = '" + linkOnFile + "';\n" +
                "\n" +
                "  const data = await fetch(URL);\n" +
                "  const reader = data.body.getReader('blob');\n" +
                "\n" +
                "  reader.read().then(function processText({ done, value }) {\n" +
                "    if (done) {\n" +
                "      console.error(new TextDecoder(\"" + encoding + "\").decode(value));\n" +
                "      return;\n" +
                "    }\n" +
                "\n" +
                "    console.error('Successful result '+ new TextDecoder(\"" + encoding + "\").decode(value));\n" +
                "\n" +
                "    return reader.read().then(processText);\n" +
                "  });\n" +
                "\n" +
                "})();";
        webDriver.manage().logs().get(LogType.BROWSER);
        Tools.scripts(webDriver).runScript(s);

        do {
            if (secondsToWaitForResult-- > 0) {
                Tools.waitElement(webDriver).waitTime(1000);

                LogEntries logEntries = webDriver.manage().logs().get(LogType.BROWSER);
                for (LogEntry logEntry : logEntries) {
                    String message = logEntry.getMessage();

                    if (message.contains("Successful result")) {
                        Pattern pattern = Pattern.compile("\".*\"");
                        Matcher matcher = pattern.matcher(message);
                        while (matcher.find()) {
                            result.append(message.substring(matcher.start(), matcher.end() - 1).replace("\"Successful result", "").trim());
                        }
                        break;
                    }
                }
            } else {
                break;
            }

        } while (result.toString().equals(""));

        if (result.toString().equals("")) {
            return null;
        } else {
            return result.toString().replace("Successful result", "").trim();
        }
    }
}
