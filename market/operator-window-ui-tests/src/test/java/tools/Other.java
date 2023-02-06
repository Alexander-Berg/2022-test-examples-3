package ui_tests.src.test.java.tools;

import net.bytebuddy.utility.RandomString;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Other {

    /**
     * Создание скриншота браузера
     *
     * @param fileName - имя скриншота
     */
    public void captureScreenshot(WebDriver webDriver, String fileForScreen, String fileName) {
        try {

            new File("target/surefire-reports/" + fileForScreen + "/").mkdirs(); // Insure directory is there
            Tools.screenshoter(webDriver).makePageScreenshot("screenshot-" + fileName, "target/surefire-reports/" + fileForScreen);

        } catch (Throwable e) {
            throw new Error("Не получилось сделать скриншот \n" + e);
        }
    }

    /**
     * Получить рандомный номер
     *
     * @param min - мнимальное число
     * @param max - максимальное число
     * @return
     */
    public int getRandomNumber(int min, int max) {
        max -= min;
        return (int) (Math.random() * ++max) + min;
    }

    /**
     * Получить рандомный текст
     *
     * @return
     */
    public String getRandomText() {
        RandomString r = new RandomString();
        String text = r.nextString() + r.nextString();
        return text;
    }


    /**
     * Получить текст из буфера
     *
     * @return текст из буфера
     */
    public String getValueFromBuffer() {
        try {
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (Throwable e) {
            throw new Error("Не удалось получить данные из буфера \n" + e);
        }
    }

    /**
     * Получить gid открытой в данный момент сущности
     */
    public String getGidFromCurrentPageUrl(WebDriver webDriver) {
        return webDriver.getCurrentUrl().replaceAll(".*\\/", "");
    }

    /**
     * Посчитать, сколько раз элемент отображается на странице
     */
    public int getNumberOfElementOccurrences(WebDriver webDriver, By byTag) {
        try {
            return Tools.findElement(webDriver).findElements(byTag).size();
        } catch (Throwable e) {
            throw new Error("Не получилось почитать, сколько раз элемент отображается на странице " + "\n" + e);
        }
    }

    /**
     * Передать фокус в iframe
     */
    public void takeFocusIFrame(WebDriver webDriver, String name) {
        webDriver.switchTo().frame(name);
    }

    /**
     * Получить подстроку по регулярному выражению
     * @param regexp
     * @param text
     * @return
     */
    public String getSubString(String regexp, String text){
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            return (text.substring(matcher.start(), matcher.end()));
        }
        return null;
    }

    /**
     * Есть ли в тексте подстрока
     * @param regexp
     * @param text
     * @return
     */
    public boolean isContainsSubstring(String regexp, String text){
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

}


