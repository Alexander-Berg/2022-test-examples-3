package ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.tests;


import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.PropertyLoader;
import ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.step.WmsSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.InboundTable;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;

@Epic("Selenium Tests")
public abstract class AbstractWMSTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractWMSTest.class);

    protected WmsSteps wmsSteps;
    private InboundTable inboundTable;

    {
        PropertyLoader.newInstance().populate(this);
    }

    @BeforeEach
    @Step("Общая для selenium тестов подготовка данных")
    public void initialSetUp() {
        inboundTable = DatacreatorSteps
                .Location()
                .createInboundTable();

        wmsSteps = new WmsSteps(inboundTable);
    }

    @AfterEach
    @Step("Общая для тестов очистка данных")
    public void finalTearDown() {
        DatacreatorSteps
                .Location()
                .deleteInboundTable(inboundTable);
    }
}
