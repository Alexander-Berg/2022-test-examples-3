package ru.yandex.market.vendors.analytics.platform.controller.dashboard.external.unfulfilled_demand;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.external.unfulfilled_demand.UnfulfilledDemandController;
import ru.yandex.market.vendors.analytics.platform.controller.external.unfulfilled_demand.response.UnfulfilledDemandDetailsResponse;
import ru.yandex.market.vendors.analytics.platform.yt.YtMetaTree;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

public class UnfulfilledDemandControllerTest extends FunctionalTest {
    @Autowired
    private UnfulfilledDemandController unfulfilledDemandController;

    @Autowired
    private YtMetaTree ytMetaTree;

    @Test
    @DisplayName("Проверка вызова метода getDetails")
    public void getDetails() {
        Mockito.when(ytMetaTree.getAttributeStringValue(any(), any())).thenReturn(Optional.of("2021-08-17"));

        UnfulfilledDemandDetailsResponse details = unfulfilledDemandController.getDetails();

        Assertions.assertNotNull(details);
        Assertions.assertTrue(details.getActualDate().isEqual(LocalDate.of(2021, 8, 17)));
    }
}
