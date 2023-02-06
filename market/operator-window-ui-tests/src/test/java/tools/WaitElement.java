package ui_tests.src.test.java.tools;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import unit.Config;

import java.util.List;


public class WaitElement {
    private WebDriver webDriver;

    public WaitElement(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    private WebDriverWait webDriverWait(int secondTime) {
        return new WebDriverWait(webDriver, secondTime, 500);
    }

    /**
     * Ожидание пока элемент пропадет
     *
     * @param webElement элемент который должен пропасть
     * @param secondTime время ожидания пока пропадет элемент
     */
    public void waitInvisibilityElementTheTime(WebElement webElement, int secondTime) {
        webDriverWait(secondTime).until(ExpectedConditions.invisibilityOf(webElement));
    }

    /**
     * Ожидание пока элемент пропадет
     *
     * @param byElement  элемент который должен пропасть
     * @param secondTime время ожидания пока пропадет элемент
     */
    public void waitInvisibilityElementTheTime(By byElement, int secondTime) {
        try {
            webDriverWait(secondTime).until(ExpectedConditions.invisibilityOfElementLocated(byElement));
        } catch (Throwable e) {
            throw new Error("Элемент " + byElement + " не пропал со страницы за " + secondTime + "сек" + " \n" + e);
        }

    }

    /**
     * Ожидание пока элемент станет видимым на странице
     *
     * @param byTag      элемент который ожидаем
     * @param secondTime время ожидания
     */
    public void waitVisibilityElementTheTime(By byTag, int secondTime) {
        try {
            webDriverWait(secondTime).until(ExpectedConditions.visibilityOfElementLocated(byTag));
        } catch (Throwable e) {
            throw new Error("Не дождались когда элемент " + byTag + " появится на странице" + " \n" + e.getMessage());
        }
    }

    /**
     * Ожидание пока элемент станет видимым на странице
     *
     * @param byTag      элемент который ожидаем
     * @param secondTime время ожидания
     */
    public void waitElementToAppearInDOMTheTime(By byTag, int secondTime) {
        try {
            webDriverWait(secondTime).until(ExpectedConditions.presenceOfElementLocated(byTag));
        } catch (Throwable e) {
            throw new Error("Не дождались когда элемент " + byTag + " появится в DOM" + " \n" + e);
        }
    }

    /**
     * Ожидание пока элемент появится
     *
     * @param webElement - элемент который ожидаем
     * @param secondTime - время ожидания
     */
    public void waitVisibilityElementTheTime(WebElement webElement, int secondTime) {
        try {
            webDriverWait(secondTime).until(ExpectedConditions.visibilityOf(webElement));
        } catch (Throwable e) {
            throw new Error("Не дождались когда элемент " + webElement + " появится на странице" + " \n" + e);
        }
    }

    /**
     * Ожидание пока элементы появятся
     *
     * @param webElement - элемент который ожидаем
     * @param secondTime - время ожидания
     */
    public void waitVisibilityElementTheTime(List<WebElement> webElement, int secondTime) {
        try {
            webDriverWait(secondTime).until(ExpectedConditions.visibilityOfAllElements(webElement));
        } catch (Throwable e) {
            throw new Error("Не дождались когда элементы " + webElement + " появится на странице" + " \n" + e);
        }
    }

    /**
     * Ожидание пока элемент будет виден на странице
     *
     * @param byTag - элемент который ожидаем
     */
    public void waitVisibilityElement(By byTag) {
        waitVisibilityElementTheTime(byTag, Config.DEF_TIME_WAIT_LOAD_PAGE);
    }

    /**
     * Ожидание пока элемент будет виден
     *
     * @param byTag - элемент который ожидаем
     */
    public void waitElementToAppearInDOM(By byTag) {
        waitElementToAppearInDOMTheTime(byTag, Config.DEF_TIME_WAIT_LOAD_PAGE);
    }

    /**
     * Ожидание пока элемент будет виден
     *
     * @param webElement - элемент который ожидаем
     */
    public boolean waitVisibilityElement(WebElement webElement) {
        try {
            waitVisibilityElementTheTime(webElement, Config.DEF_TIME_WAIT_LOAD_PAGE);
            return true;
        } catch (Throwable e) {
            return false;
        }

    }

    /**
     * Ожидание пока элемент будет виден
     *
     * @param listWebElement - элемент который ожидаем
     */
    public void waitVisibilityAllElements(List<WebElement> listWebElement) {
        waitVisibilityElementTheTime(listWebElement, Config.DEF_TIME_WAIT_LOAD_PAGE);
    }

    /**
     * Ожидание пока элемент станет кликабелен
     *
     * @param webElement - элемент который ожидаем
     * @param secondTime - время ожидания
     */
    public WebElement waitClickableElementTheTime(WebElement webElement, int secondTime) {
        try {
            webDriverWait(secondTime).until(ExpectedConditions.elementToBeClickable(webElement));
        } catch (Throwable e) {
            throw new Error("Не дождались когда элемент " + webElement + " станет кликабельным" + " \n" + e);
        }
        return webElement;
    }

    /**
     * Ожидание пока элемент станет кликабелен
     *
     * @param byTag      - элемент который ожидаем
     * @param secondTime - время ожидания
     */
    public WebElement waitClickableElementTheTime(By byTag, int secondTime) {
        try {
            webDriverWait(secondTime).until(ExpectedConditions.elementToBeClickable(byTag));
        } catch (Throwable e) {
            throw new Error("Не дождались когда элемент " + byTag + " станет кликабельным" + " \n" + e);
        }
        return Tools.findElement(webDriver).findElement(byTag);
    }

    /**
     * Ожидание пока элемент не станет кликабелен
     *
     * @param webElement - элемент который ожидаем
     */
    public WebElement waitClickableElement(WebElement webElement) {
        waitClickableElementTheTime(webElement, Config.DEF_TIME_WAIT_LOAD_PAGE);
        return webElement;
    }

    /**
     * Ожидание пока элемент станет кликабелен
     *
     * @param byTag - элемент который ожидаем
     */
    public WebElement waitClickableElement(By byTag) {
        return waitClickableElementTheTime(byTag, Config.DEF_TIME_WAIT_LOAD_PAGE);
    }

    /**
     * Ожидание алерта
     *
     * @param secondTime - время ожидания
     */
    public void waitAlertIsPresentForTime(int secondTime) {
        webDriverWait(secondTime).until(ExpectedConditions.alertIsPresent());
    }

    /**
     * Ждем N-ое время
     *
     * @param millis - количество ожидания в млс.
     */
    public void waitTime(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new Error(e);
        }
    }

    /**
     * Ожидание пока загрузятся элементы после действий
     */
    public void waitInvisibleLoadingElement() {
        waitTime(1000);
        int waitInvisibleTime = Config.DEF_TIME_WAIT_LOAD_PAGE * 2;
        for (int i = 0; i < 2; i++) {
            try {
                waitInvisibilityElementTheTime(By.xpath("//*[contains(@class,'ZIw2VZUG')]"), waitInvisibleTime);
                waitInvisibilityElementTheTime(By.xpath("//*[contains(@class,'_3QKu1ftw')]"), waitInvisibleTime);
                waitInvisibilityElementTheTime(By.xpath("//*[contains(@class,'_30xKtdy5')]"), waitInvisibleTime);
            } catch (Throwable e) {
                throw new Error("Слишком долго выполнялся процесс с загрузкой 'спиннер'. Спиннер крутился дольше " + waitInvisibleTime + " секунд \n" + e);
            }

            try {
                waitInvisibilityElementTheTime(By.xpath("//*[contains(@class,'_1nDqupmk KWAdGDMK _2UC8HPJ1')]"), waitInvisibleTime);
            } catch (Throwable e) {
                throw new Error("Слишком долго выполнялся процесс с загрузкой свойств. Загрузка длилась дольше " + waitInvisibleTime + " секунд \n" + e);
            }

            try {
                waitInvisibilityElementTheTime(By.xpath("//*[contains(@class,'_2wGNg4a8')]"), waitInvisibleTime);
            } catch (Throwable e) {
                throw new Error("Слишком долго загружались кнопки (анимация бегущая зебра на кнопках) " + " \n" + e);
            }
        }
        waitTime(500);
    }

    public void waitBrowserAlert(int waitSecond) {
        try {
            (new WebDriverWait(webDriver, waitSecond)).until(ExpectedConditions.alertIsPresent());
        } catch (Throwable e) {
            throw new Error("Не появился браузерный алерт " + " \n" + e);
        }
    }
}

