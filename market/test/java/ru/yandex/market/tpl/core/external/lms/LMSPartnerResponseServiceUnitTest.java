package ru.yandex.market.tpl.core.external.lms;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class LMSPartnerResponseServiceUnitTest {

    @InjectMocks
    private LMSPartnerResponseService lmsPartnerResponseService;
    @Mock
    private LMSClient lmsClient;

    @DisplayName("Получение партнера с сабтипом партнера Лавка")
    @Test
    void getLavkaPartner() {
        Mockito.when(lmsClient.getPartner(Mockito.anyLong()))
                .thenReturn(buildLavkaPartnerResponse());

        ru.yandex.market.tpl.core.domain.pickup.PartnerResponse partner = lmsPartnerResponseService.getPartner(1L);

        assertEquals(partner.getSubtype(), PartnerSubType.LAVKA);
    }

    @DisplayName("Получение партнера с сабтипом партнера ПВЗ")
    @Test
    void getPvzPartner() {
        Mockito.when(lmsClient.getPartner(Mockito.anyLong()))
                .thenReturn(buildPvzPartnerResponse());

        ru.yandex.market.tpl.core.domain.pickup.PartnerResponse partner = lmsPartnerResponseService.getPartner(1L);

        assertEquals(partner.getSubtype(), PartnerSubType.PVZ);
    }

    @DisplayName("Получение партнера с сабтипом партнера Постамат")
    @Test
    void getLockerPartner() {
        Mockito.when(lmsClient.getPartner(Mockito.anyLong()))
                .thenReturn(buildLockerPartnerResponse());

        ru.yandex.market.tpl.core.domain.pickup.PartnerResponse partner = lmsPartnerResponseService.getPartner(1L);

        assertEquals(partner.getSubtype(), PartnerSubType.LOCKER);
    }

    private Optional<PartnerResponse> buildLavkaPartnerResponse() {
        PartnerResponse lavka = PartnerResponse.newBuilder()
                .subtype(PartnerSubtypeResponse.newBuilder()
                        .id(8L)
                        .name("LAVKA")
                        .build())
                .build();

        return Optional.of(lavka);
    }

    private Optional<PartnerResponse> buildPvzPartnerResponse() {
        PartnerResponse pvz = PartnerResponse.newBuilder()
                .subtype(PartnerSubtypeResponse.newBuilder()
                        .id(3L)
                        .name("PVZ")
                        .build())
                .build();

        return Optional.of(pvz);
    }

    private Optional<PartnerResponse> buildLockerPartnerResponse() {
        PartnerResponse locker = PartnerResponse.newBuilder()
                .subtype(PartnerSubtypeResponse.newBuilder()
                        .id(5L)
                        .name("Locker")
                        .build())
                .build();

        return Optional.of(locker);
    }

}
