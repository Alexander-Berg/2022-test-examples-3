package ui_tests.src.test.java.tools;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class FindElement {

    private WebDriver webDriver;

    public FindElement(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Поиск элемента на странице
     *
     * @param byTag
     * @return
     */
    public WebElement findVisibleElement(By byTag) {
        try {
            Tools.waitElement(webDriver).waitVisibilityElementTheTime(byTag, 3);
        } catch (Throwable e) {
            throw new Error("Не получилось найти элемент на странице " + "\n" + e);
        }
        return webDriver.findElement(byTag);
    }

    /**
     * Поиск элемента
     * метод не выполняет ожиданий элемента и т.п.
     *
     * @param byTag
     * @return
     */
    public WebElement findElement(By byTag) {
        return webDriver.findElement(byTag);
    }

    /**
     * Поиск элемента в DOM
     *
     * @param byTag
     * @return
     */
    public WebElement findElementInDOM(By byTag) {
        try {
            Tools.waitElement(webDriver).waitElementToAppearInDOM(byTag);
        } catch (Throwable e) {
            throw new Error("Не получилось найти элемент на странице " + "\n" + e);
        }
        return webDriver.findElement(byTag);
    }

    /**
     * Поиск элемента в DOM
     *
     * @param byTag
     * @return
     */
    public WebElement findElementInDOM(By byTag, int secondTime) {
        try {
            Tools.waitElement(webDriver).waitElementToAppearInDOMTheTime(byTag, secondTime);
        } catch (Throwable e) {
            throw new Error("Не получилось найти элемент на странице за " + secondTime + " секунд\n" + e);
        }
        return webDriver.findElement(byTag);
    }

    /**
     * Поиск всех элементов с ожиданием их появления в DOM
     *
     * @param teg список найденных элементов
     * @return
     */
    public List<WebElement> findElementsWithAnExpectationToAppearInDOM(By teg) {
        Tools.waitElement(webDriver).waitElementToAppearInDOM(teg);
        return findElements(teg);
    }

    public List<WebElement> findElements(By teg) {
        try {
            return webDriver.findElements(teg);
        } catch (Throwable e) {
            return new ArrayList<>();
        }

    }
}
