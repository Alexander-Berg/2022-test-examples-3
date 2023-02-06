package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import java.util.concurrent.TimeUnit;

import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;

public class ParcelLabelReportPage extends AbstractReportPage {

    public ParcelLabelReportPage(WebDriver driver) {
        super(driver);
    }

}
