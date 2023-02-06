package ru.yandex.market.pvz.core.domain.approve.delivery_service;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.SetupStage.API_SETTINGS;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.SetupStage.CUSTOMER_INFO;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.SetupStage.MDB;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.SetupStage.PARTNER_ACTIVATE;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.SetupStage.PARTNER_CAPACITY;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.SetupStage.PARTNER_CREATE;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.SetupStage.PARTNER_EXTERNAL_PARAM;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.SetupStage.PLATFORM_CLIENT_PARTNER;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.SetupStage.POSSIBLE_ORDER_CHANGE;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.SetupStage.TARIFFICATOR_UPDATE_TAGS;

class SetupStageTest {

    @Test
    void testSorting() {
        List<SetupStage> actual = SetupStage.sortedStages();

        List<SetupStage> expected = List.of(
                PARTNER_CREATE,
                CUSTOMER_INFO,
                API_SETTINGS,
                POSSIBLE_ORDER_CHANGE,
                PARTNER_EXTERNAL_PARAM,
                PLATFORM_CLIENT_PARTNER,
                PARTNER_CAPACITY,
                MDB,
                TARIFFICATOR_UPDATE_TAGS,
                PARTNER_ACTIVATE
        );

        assertThat(actual).isEqualTo(expected);
    }
}
