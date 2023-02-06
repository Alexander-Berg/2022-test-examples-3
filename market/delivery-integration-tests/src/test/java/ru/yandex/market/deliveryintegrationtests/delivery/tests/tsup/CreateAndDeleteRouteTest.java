package ru.yandex.market.deliveryintegrationtests.delivery.tests.tsup;

import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Resource;

@Resource.Classpath({"delivery/delivery.properties"})
@Slf4j
@DisplayName("Tsup Test")
@Epic("Tsup")

public class CreateAndDeleteRouteTest extends AbstractTsupTest {

    /**
     * Константы с набором данных ниже нужны только в этом тесте, уносить их наружу нет смысла
     **/
    private static final Long START_PARTNER_ID = 48099L;
    private static final Long END_PARTNER_ID = 48102L;

    @Test
    @DisplayName("Тsup: Создание и удаление маршрута")
    void createAndDeleteRouteTest() {
        TSUP_STEPS.createRoutePipeline(START_PARTNER_ID, END_PARTNER_ID);
        long route = TSUP_STEPS.getRouteBetweenPartners(START_PARTNER_ID, END_PARTNER_ID);
        TSUP_STEPS.deleteRoute(route);
        TSUP_STEPS.verifyRouteBetweenPartnersNotExists(START_PARTNER_ID, END_PARTNER_ID);
    }

}
