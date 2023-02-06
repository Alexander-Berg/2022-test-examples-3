package ru.yandex.market.partner.logistic.point;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.eq;

/**
 * Функциональные тесты для {@link PartnerLogisticPointExecutor}.
 */
@DbUnitDataSet(before = "PartnerLogisticPointExecutorFunctionalTest.before.csv")
public class PartnerLogisticPointExecutorFunctionalTest extends FunctionalTest {
    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private PartnerLogisticPointExecutor partnerLogisticPointExecutor;

    @Test
    @DbUnitDataSet(
            before = "PartnerLogisticPointExecutorFunctionalTest.testJob.before.csv",
            after = "PartnerLogisticPointExecutorFunctionalTest.testJob.after.csv")
    void testJob() {
        partnerLogisticPointExecutor.doJob(null);
        RegisterShopDto registerShopDto = RegisterShopDto.builder()
                .id(1L)
                .externalId(null)
                .businessId(2L)
                .regionId(213)
                .name("dropship1")
                .role(ShopRole.DROPSHIP)
                .createPartnerOnRegistration(true)
                .build();
        Mockito.verify(nesuClient).registerShop(eq(registerShopDto));
        Mockito.verifyNoMoreInteractions(nesuClient);
    }

    @Test
    void testJobNoFlag() {
        partnerLogisticPointExecutor.doJob(null);
        Mockito.verifyNoInteractions(nesuClient);
    }
}
