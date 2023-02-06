package ru.yandex.travel.steps;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.htmlelements.WebPage;
import io.qameta.htmlelements.WebPageFactory;
import org.openqa.selenium.WebDriver;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;
import ru.yandex.travel.beans.TourInformation;
import ru.yandex.travel.elliptics.Elliptics;
import ru.yandex.travel.webdriver.WebDriverManager;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.UUID;


public class WebDriverSteps {

    @Inject
    private WebDriverManager manager;

    protected <T extends WebPage> T onPage(Class<T> page) {
        WebPageFactory factory = new WebPageFactory();
        return factory.get(getDriver(), page);
    }

    protected WebDriver getDriver() {
        return manager.getDriver();
    }

    @Step
    public WebDriverSteps addCurrentUrl(TourInformation tourInformation) throws IOException {
        tourInformation.setUrl(getDriver().getCurrentUrl());
        return this;
    }

    @Step
    public WebDriverSteps saveScreenshot(TourInformation tourInformation) throws IOException {
        Screenshot screenshot = new AShot()
                .shootingStrategy(ShootingStrategies.viewportPasting(100))
                .takeScreenshot(getDriver());
        File img = File.createTempFile(UUID.randomUUID().toString(), ".png");
        ImageIO.write(screenshot.getImage(), "png", img);
        String url = Elliptics.upload(WebDriverSteps.class).image(img);
        img.delete();
        tourInformation.setScreenUrl(url);
        return this;
    }
}

