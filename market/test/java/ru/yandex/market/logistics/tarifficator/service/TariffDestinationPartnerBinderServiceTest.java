package ru.yandex.market.logistics.tarifficator.service;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.tarifficator.AbstractUnitTest;
import ru.yandex.market.logistics.tarifficator.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.tarifficator.exception.http.ResourceType;
import ru.yandex.market.logistics.tarifficator.jobs.model.TariffDestinationPartnerPayload;
import ru.yandex.market.logistics.tarifficator.jobs.processor.TariffDestinationPartnerBinderService;
import ru.yandex.market.logistics.tarifficator.jobs.producer.GenerateRevisionProducer;
import ru.yandex.market.logistics.tarifficator.model.entity.Tariff;
import ru.yandex.market.logistics.tarifficator.model.entity.TariffDestinationPartner;
import ru.yandex.market.logistics.tarifficator.repository.TariffDestinationPartnerRepository;
import ru.yandex.market.logistics.tarifficator.service.partner.PartnerService;
import ru.yandex.market.logistics.tarifficator.service.partner.PartnerSubtype;
import ru.yandex.market.logistics.tarifficator.service.tariff.TariffService;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Unit-тест сервиса TariffDestinationPartnerBinderService")
@ExtendWith(MockitoExtension.class)
class TariffDestinationPartnerBinderServiceTest extends AbstractUnitTest {

    private static final long TARIFF_ID = 1L;
    private static final long PARTNER_ID_1 = 3L;
    private static final long PARTNER_ID_2 = 4L;
    private static final long PARTNER_ID_3 = 5L;
    private static final long PARTNER_ID_4 = 6L;
    private static final long PARTNER_ID_5 = 7L;

    @Mock
    private TariffService tariffService;
    @Mock
    private PartnerService partnerService;
    @Mock
    private TariffDestinationPartnerRepository tariffDestinationPartnerRepository;
    @Mock
    private GenerateRevisionProducer generateRevisionProducer;
    @Spy
    private final TransactionTemplate mockTransactionTemplate = new TransactionTemplate(
        mock(PlatformTransactionManager.class)
    );
    @InjectMocks
    private TariffDestinationPartnerBinderService tariffDestinationPartnerBinderService;

    @Test
    @DisplayName("Пейлод с несуществующим тарифом")
    void processEmptyPayload() {
        when(tariffService.getTariff(anyLong()))
            .thenThrow(new ResourceNotFoundException(ResourceType.TARIFF, TARIFF_ID));
        softly.assertThatThrownBy(
            () -> tariffDestinationPartnerBinderService.processPayload(payload())
        ).isInstanceOf(ResourceNotFoundException.class);
        softly.assertAll();
        verify(tariffService, only()).getTariff(TARIFF_ID);
    }

    @Test
    @DisplayName("Не получается получить список партнёров")
    void processPayloadWithFailedToGetPartners() {
        when(partnerService.findPickupPartners())
            .thenThrow(new UnsupportedOperationException());
        softly.assertThatThrownBy(
            () -> tariffDestinationPartnerBinderService.processPayload(payload())
        ).isInstanceOf(UnsupportedOperationException.class);
        softly.assertAll();
        verify(partnerService, only()).findPickupPartners();
    }

    @Test
    @DisplayName("Нет партнёров для связки и нет связанных")
    void tariffBoundedPartnersListIsEmpty() {
        var tariff = tariff();

        when(tariffService.getTariff(TARIFF_ID))
            .thenReturn(tariff);
        when(partnerService.findPickupPartners())
            .thenReturn(emptyList());

        tariffDestinationPartnerBinderService.processPayload(payload());

        verifyZeroInteractions(tariffDestinationPartnerRepository, generateRevisionProducer);
    }

    @Test
    @DisplayName("Список партнёров для связки такой же как и список связанных")
    void tariffBoundedPartnersListIsSameAsPickupPartners() {
        var tariff = tariff();
        tariff.getDestinationPartners().addAll(Set.of(
            tariffDestinationPartner(PARTNER_ID_1)
        ));

        when(tariffService.getTariff(TARIFF_ID))
            .thenReturn(tariff);
        when(partnerService.findPickupPartners())
            .thenReturn(List.of(partnerResponse(PARTNER_ID_1)));

        tariffDestinationPartnerBinderService.processPayload(payload());

        verifyZeroInteractions(tariffDestinationPartnerRepository, generateRevisionProducer);
    }

    @Test
    @DisplayName("Успешный вариант привязки")
    void success() {
        // given:
        final var tariff = tariff();
        tariff.getDestinationPartners().addAll(Set.of(
            tariffDestinationPartner(PARTNER_ID_1),
            tariffDestinationPartner(PARTNER_ID_2),
            tariffDestinationPartner(PARTNER_ID_3)
        ));
        doReturn(tariff)
            .when(tariffService).getTariff(anyLong());
        doReturn(List.of(
            partnerResponse(PARTNER_ID_1),
            partnerResponse(PARTNER_ID_4),
            partnerResponse(PARTNER_ID_5, PartnerSubtype.GO_PARTNER_LOCKER)
        ))
            .when(partnerService).findPickupPartners();

        // when:
        tariffDestinationPartnerBinderService.processPayload(payload());

        // then:
        verify(tariffService).getTariff(TARIFF_ID);
        verify(partnerService).findPickupPartners();
        ArgumentCaptor<TariffDestinationPartnerList> tariffDestinationPartnerListCaptor =
            ArgumentCaptor.forClass(TariffDestinationPartnerList.class);
        verify(tariffDestinationPartnerRepository, only()).saveAll(tariffDestinationPartnerListCaptor.capture());
        softly.assertThat(tariffDestinationPartnerListCaptor.getValue())
            .extracting(TariffDestinationPartner::getPartnerId)
            .containsExactlyElementsOf(List.of(PARTNER_ID_4, PARTNER_ID_5));
        verify(generateRevisionProducer, only()).produceByTariffId(TARIFF_ID);
    }

    @Test
    @DisplayName("Не генерировать поколение для выключенного тарифа")
    void shouldNotBindPartners_whenTariffIsDisabled() {
        // given:
        final var tariff = tariff().setEnabled(false);
        tariff.getDestinationPartners().addAll(Set.of(
            tariffDestinationPartner(PARTNER_ID_1),
            tariffDestinationPartner(PARTNER_ID_2),
            tariffDestinationPartner(PARTNER_ID_3)
        ));
        doReturn(tariff)
            .when(tariffService).getTariff(anyLong());
        doReturn(List.of(
            partnerResponse(PARTNER_ID_1),
            partnerResponse(PARTNER_ID_4)
        ))
            .when(partnerService).findPickupPartners();

        // when:
        tariffDestinationPartnerBinderService.processPayload(payload());

        // then:
        verify(tariffService).getTariff(TARIFF_ID);
        verify(partnerService).findPickupPartners();
        verify(tariffDestinationPartnerRepository, only()).saveAll(anyList());
        verifyZeroInteractions(generateRevisionProducer);
    }

    @Nonnull
    private Tariff tariff() {
        var tariff = new Tariff()
            .setPartnerId(2L);
        tariff = spy(tariff);
        lenient().doReturn(TARIFF_ID).when(tariff).getId();
        return tariff;
    }

    @Nonnull
    private TariffDestinationPartner tariffDestinationPartner(long partnerId) {
        return new TariffDestinationPartner().setPartnerId(partnerId);
    }

    @Nonnull
    private PartnerResponse partnerResponse(long id, PartnerSubtype partnerSubtype) {
        return PartnerResponse.newBuilder()
            .id(id)
            .subtype(PartnerSubtypeResponse.newBuilder().id(partnerSubtype.getId()).build())
            .build();
    }


    @Nonnull
    private PartnerResponse partnerResponse(long id) {
        return partnerResponse(id, PartnerSubtype.PARTNER_PVZ);
    }

    @Nonnull
    private TariffDestinationPartnerPayload payload() {
        return new TariffDestinationPartnerPayload(null, TARIFF_ID);
    }

    private interface TariffDestinationPartnerList extends List<TariffDestinationPartner> {
    }
}
