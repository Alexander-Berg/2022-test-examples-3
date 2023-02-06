package ru.yandex.market.mbi.api.controller.operation.business;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для {@link ru.yandex.market.mbi.api.controller.operation.business.BusinessApiController}
 */
@DbUnitDataSet(before = "BusinessApiControllerFunctionalTest.before.csv")
public class BusinessApiControllerFunctionalTest extends FunctionalTest {

    @Test
    @DisplayName("Получить партнеров по бизнесу")
    void getPartnerIdsByBusinessId() {
        List<Long> partnerIds = getMbiOpenApiClient().getPartnerIdsByBusinessId(100L);
        assertThat(partnerIds, containsInAnyOrder(100L,  200L));

        partnerIds = getMbiOpenApiClient().getPartnerIdsByBusinessId(101L);
        assertEquals(partnerIds, List.of(101L));
    }
}
