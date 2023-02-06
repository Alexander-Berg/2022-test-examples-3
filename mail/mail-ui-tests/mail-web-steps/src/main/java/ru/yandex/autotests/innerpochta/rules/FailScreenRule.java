package ru.yandex.autotests.innerpochta.rules;

import io.qameta.allure.Attachment;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.remote.Augmenter;

public class FailScreenRule extends TestWatcher {

    private final WebDriverRule webDriverRule;

    private FailScreenRule(WebDriverRule webDriverRule) {
        this.webDriverRule = webDriverRule;
    }

    public static FailScreenRule failScreenRule(WebDriverRule webDriverRule) {
        return new FailScreenRule(webDriverRule);
    }

    @Override
    protected void failed(Throwable e, Description description) {
        try {
            String url = webDriverRule.getDriver().getCurrentUrl();
            String cutUrl = url.substring(0, url.indexOf(".ru") + 3);
            screen(cutUrl);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Attachment(value = "{0}", type = "image/png", fileExtension = "png")
    private byte[] screen(String url) {
        return ((TakesScreenshot) (new Augmenter()).augment(webDriverRule.getDriver().getWrappedDriver()))
            .getScreenshotAs(OutputType.BYTES);
    }
}
