package ru.yandex.autotests.innerpochta.rules;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.WebDriverEventListener;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;

import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_DISABLE_ANIMATION_CAL;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_DISABLE_ANIMATION_LIZA_AND_TOUCH;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_REMOVE_CARET;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_SPEEDUP_JQUERY_ANIMATION;

/**
 * @author a-zoshchuk
 */
public class EventHandler implements WebDriverEventListener {

    public void beforeAlertAccept(WebDriver driver) {

    }

    public void afterAlertAccept(WebDriver driver) {

    }

    public void afterAlertDismiss(WebDriver driver) {

    }

    public void beforeAlertDismiss(WebDriver driver) {

    }

    public void beforeNavigateTo(String url, WebDriver driver) {

    }

    public void afterNavigateTo(String url, WebDriver driver) {
        disableAnimationAndCaret(driver);
        removeAlerts(driver);
    }

    public void beforeNavigateBack(WebDriver driver) {

    }

    public void afterNavigateBack(WebDriver driver) {

    }

    public void beforeNavigateForward(WebDriver driver) {

    }

    public void afterNavigateForward(WebDriver driver) {

    }

    public void beforeNavigateRefresh(WebDriver driver) {

    }

    public void afterNavigateRefresh(WebDriver driver) {
        disableAnimationAndCaret(driver);
        removeAlerts(driver);
    }

    public void beforeFindBy(By by, WebElement element, WebDriver driver) {

    }

    public void afterFindBy(By by, WebElement element, WebDriver driver) {

    }

    public void beforeClickOn(WebElement element, WebDriver driver) {
        disableAnimationAndCaret(driver);
    }

    public void afterClickOn(WebElement element, WebDriver driver) {

    }

    public void beforeChangeValueOf(WebElement element, WebDriver driver, CharSequence[] keysToSend) {

    }

    public void afterChangeValueOf(WebElement element, WebDriver driver, CharSequence[] keysToSend) {

    }

    public void beforeScript(String script, WebDriver driver) {

    }

    public void afterScript(String script, WebDriver driver) {

    }

    public void beforeSwitchToWindow(String windowName, WebDriver driver) {

    }

    public void afterSwitchToWindow(String windowName, WebDriver driver) {

    }

    public void onException(Throwable throwable, WebDriver driver) {

    }

    public <X> void beforeGetScreenshotAs(OutputType<X> target) {

    }

    public <X> void afterGetScreenshotAs(OutputType<X> target, X screenshot) {

    }

    public void beforeGetText(WebElement element, WebDriver driver) {

    }

    public void afterGetText(WebElement element, WebDriver driver, String text) {

    }

    private void disableAnimationAndCaret(WebDriver driver) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        try {
            jsExecutor.executeScript(SCRIPT_REMOVE_CARET);
        } catch (Exception e) {
            System.out.println("Couldn't color caret " + e.getMessage());
        }
        try {
            if (UrlProps.urlProps().getProject().equals("cal")) {
                jsExecutor.executeScript(SCRIPT_DISABLE_ANIMATION_CAL);
            }
            else {
                jsExecutor.executeScript(SCRIPT_DISABLE_ANIMATION_LIZA_AND_TOUCH);
            }
        } catch (Exception e) {
            System.out.println("Couldn't disable animation " + e.getMessage());
        }
        try {
            jsExecutor.executeScript(SCRIPT_SPEEDUP_JQUERY_ANIMATION);
        } catch (Exception e) {
            System.out.println("Couldn't speedup jquery animation " + e.getMessage());
        }
    }

    private void removeAlerts(WebDriver driver){
        try {
            driver.switchTo().alert().accept();
        } catch (NoAlertPresentException e) {
            // ничего не делаем, алерта и так нет
        }
    }
}
