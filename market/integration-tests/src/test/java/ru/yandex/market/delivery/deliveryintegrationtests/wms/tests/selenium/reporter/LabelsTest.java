package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.reporter;


import io.qameta.allure.Epic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report.PrintPLTPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;

@DisplayName("Selenium: Reports")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/wms.properties"})
public class LabelsTest extends AbstractUiTest {

    @Property("wms.ui.receiving.printer")
    private String printer;

    public LabelsTest() {
        PropertyLoader.newInstance().populate(this);
    }


    @RetryableTest
    @DisplayName("Проверка отчета в новом интерфейсе \"Печать пэшки\"")
    @ResourceLock("Проверка отчета в новом интерфейсе \"Печать пэшки\"")
    void lpnLabelReportTest() {
        uiSteps.Login().PerformLogin();
        PrintPLTPage ptlPage = uiSteps
                .Navigation()
                .menu()
                .openLabels()
                .openPrintPLTPage();

        ptlPage.printPackage("123", printer);
        String notificationText = ptlPage.getNotificationTitleText();
        Assertions.assertEquals(notificationText, "Отправлено на принтер");
    }

}
