package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.transportation;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Перемещение вложений между родительскими тарами")
@Epic("Selenium Tests")
public class ReplaceBetweenParentContainerTest extends AbstractUiTest {

    private String totId;
    private String flipboxId;

    @BeforeEach
    @Step("Подготовка: Создаем тары")
    public void setUp() throws Exception {
        totId = DatacreatorSteps.Label().createContainer(ContainerIdType.TM);
        flipboxId = DatacreatorSteps.Label().createContainer(ContainerIdType.RCP);
    }

    //TODO очищать привязку тар, после реализации в https://st.yandex-team.ru/MARKETWMS-12649

    @RetryableTest
    @DisplayName("Перемещение флипбоксов в пустую родительскую тару")
    @ResourceLock("Перемещение флипбоксов в пустую родительскую тару")
    public void replaceFlipBetweenEmptyParentContainer() {
        uiSteps.Login().PerformLogin();
        uiSteps.Nok().replaceFlipBetweenParentEmptyContainer(totId, flipboxId);
    }

}
