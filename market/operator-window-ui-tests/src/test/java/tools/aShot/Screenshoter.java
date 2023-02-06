package ui_tests.src.test.java.tools.aShot;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.comparison.ImageDiff;
import ru.yandex.qatools.ashot.comparison.ImageDiffer;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Screenshoter {

    private WebDriver webDriver;

    public Screenshoter(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Получить скриншот из папки screenshots/Expected
     *
     * @param fullFileNameOfActualScreenshot название файла скриншота
     * @return
     */
    public File getActualScreenshot(String fullFileNameOfActualScreenshot) {
        File actualFile = new File("screenshots/expected/" + fullFileNameOfActualScreenshot + ".png");
        return actualFile;
    }

    public File makePageScreenshot(String fileName) {
        return makePageScreenshot(fileName, "screenshots/actual");
    }

    public File makePageScreenshot(String fileName, String filePath) {
        Screenshot pageScreenshot = new AShot()
                .shootingStrategy(ShootingStrategies.viewportPasting(100))
                .takeScreenshot(webDriver);
        File actualFile = new File(filePath + "/" + fileName + ".png");
        // save page screenshot
        try {
            ImageIO.write(pageScreenshot.getImage(), "png", actualFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return actualFile;
    }

    public File makeElementScreenshot(String fileName, WebElement element) {
        Screenshot elementScreenshot = new AShot()
                .shootingStrategy(ShootingStrategies.viewportPasting(100))
                .takeScreenshot(webDriver, element);
        File actualFile = new File("screenshots/actual/" + fileName + ".png");
        // save page screenshot
        try {
            ImageIO.write(elementScreenshot.getImage(), "png", actualFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return actualFile;
    }

    /**
     * Сравнивает скриншоты на различия
     *
     * @param expected     эталонный скриншот
     * @param actual       полученый скриншот
     * @param diffFileName имя файла в котором будут отмеченытразличия
     * @return true - есть различия false - нет различий
     * @throws IOException
     */
    public boolean hasDiff(File expected, File actual, String diffFileName) {
        Screenshot expectedScreen;
        Screenshot actualScreen;

        try {
            expectedScreen = new Screenshot(ImageIO.read(expected));
        } catch (Throwable e) {
            throw new Error("Не удалось прочитать файл " + expected.getPath() + " \n" + e);
        }
        try {
            actualScreen = new Screenshot(ImageIO.read(actual));
        } catch (Throwable t) {
            throw new Error("Не удалось прочитать файл " + expected.getPath() + " \n" + t);
        }

        ImageDiff diff = new ImageDiffer().makeDiff(expectedScreen, actualScreen);
        if (diff.hasDiff()) {
            File diffFile = new File("target/surefire-reports/" + diffFileName + ".png");

            try {
                ImageIO.write(diff.getMarkedImage(), "png", diffFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    /**
     * Сравнивает скриншоты на различия
     *
     * @param expected     эталонный скриншот
     * @param actual       полученый скриншот
     * @param diffFileName имя файла в котором будут отмеченытразличия
     * @param colorIgnore  цвет который будет игнорироваться при сравении скринов
     * @return true - есть различия false - нет различий
     * @throws IOException
     */
    public boolean hasDiff(File expected, File actual, Color colorIgnore, String diffFileName) {
        Screenshot expectedScreen;
        Screenshot actualScreen;

        try {
            expectedScreen = new Screenshot(ImageIO.read(expected));
        } catch (Throwable e) {
            throw new Error("Не удалось прочитать файл " + expected.getPath() + " \n" + e);
        }
        try {
            actualScreen = new Screenshot(ImageIO.read(actual));
        } catch (Throwable t) {
            throw new Error("Не удалось прочитать файл " + expected.getPath() + " \n" + t);
        }

        ImageDiffer imageDifferWithIgnored = new ImageDiffer().withIgnoredColor(colorIgnore);
        ImageDiff diffIgnoreColor = imageDifferWithIgnored.makeDiff(expectedScreen, actualScreen);
        if (diffIgnoreColor.hasDiff()) {
            File diffFile = new File("target/surefire-reports/" + diffFileName + ".png");

            try {
                ImageIO.write(diffIgnoreColor.getMarkedImage(), "png", diffFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
}
