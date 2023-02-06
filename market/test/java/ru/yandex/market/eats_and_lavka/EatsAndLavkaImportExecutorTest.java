package ru.yandex.market.eats_and_lavka;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.eats_and_lavka.EatsAndLavkaPartnerYtInfo;
import ru.yandex.market.core.eats_and_lavka.EatsAndLavkaPartnersYtDao;
import ru.yandex.market.core.eats_and_lavka.EatsAndLavkaService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.supplier.dao.PartnerFulfillmentLinkDao;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

public class EatsAndLavkaImportExecutorTest extends FunctionalTest {
    @Autowired
    private PartnerFulfillmentLinkDao partnerFulfillmentLinkDao;
    @Autowired
    private EatsAndLavkaService eatsAndLavkaService;
    @Autowired
    private NesuClient nesuClient;
    @Autowired
    private ProtocolService protocolService;
    @Autowired
    private EnvironmentService environmentService;

    private EatsAndLavkaPartnersYtDao eatsAndLavkaPartnersYtDao = mock(EatsAndLavkaPartnersYtDao.class);

    private EatsAndLavkaImportExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new EatsAndLavkaImportExecutor(eatsAndLavkaService, partnerFulfillmentLinkDao,
                eatsAndLavkaPartnersYtDao, protocolService, nesuClient, environmentService);
    }

    @Test
    @DbUnitDataSet(before = "EatsAndLavkaImportExecutorTest.before.csv")
    void emptyData() {
        when(eatsAndLavkaPartnersYtDao.getFromYt()).thenReturn(Collections.emptyList());
        executor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "EatsAndLavkaImportExecutorTest.before.csv",
            after = "EatsAndLavkaImportExecutorTest.after.csv"
    )
    void doJob() {
        when(eatsAndLavkaPartnersYtDao.getFromYt()).thenReturn(getYT());
        executor.doJob(null);
        verify(nesuClient).registerShop(eq(RegisterShopDto.builder()
                .id(456L)
                .regionId(56)
                .businessId(900L)
                .name("shopName1")
                .role(ShopRole.RETAIL)
                .build()));
        verify(nesuClient).registerShop(eq(RegisterShopDto.builder()
                .id(1053L)
                .regionId(276)
                .businessId(902L)
                .name("ЗОЖ-пит №312")
                .role(ShopRole.RETAIL)
                .build()));
        verify(nesuClient, times(2)).registerShop(any());
    }

    private List<EatsAndLavkaPartnerYtInfo> getYT() {
        return List.of(
                //456 - обновляем все и создаем склад
                EatsAndLavkaPartnerYtInfo.builder()
                        .shopName("shopName1")
                        .businessName("businessName1")
                        .schedule(getString(this.getClass(), "schedule/ScheduleEatPartner.json"))
                        .partnerId(456L)
                        .geoId(56L)
                        .build(),
                // Магазина нет в БД
                EatsAndLavkaPartnerYtInfo.builder()
                        .shopName("shopName9")
                        .businessName("businessName9")
                        .schedule("")
                        .partnerId(999999L)
                        .build(),
                // пустая строка
                EatsAndLavkaPartnerYtInfo.builder().build(),
                // нет данных в Yt по партнеру
                EatsAndLavkaPartnerYtInfo.builder().partnerId(1053L).build()
        );
    }
}
