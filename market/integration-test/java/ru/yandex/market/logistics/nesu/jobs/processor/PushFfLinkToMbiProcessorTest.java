package ru.yandex.market.logistics.nesu.jobs.processor;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerExternalParamRequest;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.model.ShopIdPartnerIdPayload;
import ru.yandex.market.logistics.nesu.utils.PartnerUtils;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.model.MbiFactory.getPartnerFulfillmentLinksDTO;

@DisplayName("Пуш ff-линки в MBI")
@DatabaseSetup("/jobs/processor/push_ff_link/prepare.xml")
public class PushFfLinkToMbiProcessorTest extends AbstractContextualTest {
    @Autowired
    private PushFfLinkToMbiProcessor pushFfLinkToMbiProcessor;
    @Autowired
    private MbiApiClient mbiApiClient;
    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setup() {
        when(lmsClient.searchPartners(partnerFilter()))
            .thenReturn(List.of(PartnerResponse.newBuilder().id(100L).build()));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mbiApiClient, lmsClient);
    }

    @Test
    @DisplayName("Магазин не найден")
    void shopNotFound() {
        softly.assertThatCode(() -> pushFfLinkToMbiProcessor.processPayload(new ShopIdPartnerIdPayload("1", 200, 100)))
            .hasMessage("Failed to find [SHOP] with ids [100]");
    }

    @Test
    @DisplayName("Настройка не найдена")
    void settingNotFound() {
        softly.assertThatCode(() -> pushFfLinkToMbiProcessor.processPayload(new ShopIdPartnerIdPayload("1", 200, 3)))
            .hasMessage("There is no relation between partner 200 and shop 3");
    }

    @Test
    @DisplayName("Линка существует - дропшип")
    void linkExistDropship() {
        mockLinks(1, Set.of(2L));
        when(mbiApiClient.getPartnerFulfillments(1)).thenReturn(getPartnerFulfillmentLinksDTO(1, Set.of(100L, 2L)));
        pushFfLinkToMbiProcessor.processPayload(new ShopIdPartnerIdPayload("1", 2, 1));
        verify(mbiApiClient).getPartnerFulfillments(1);
    }

    @Test
    @DisplayName("Линка не существует - дропшип")
    void linkNotExistDropship() {
        mockLinks(1, Set.of());
        pushFfLinkToMbiProcessor.processPayload(new ShopIdPartnerIdPayload("1", 2, 1));
        verify(mbiApiClient).getPartnerFulfillments(1);
        verify(mbiApiClient).updatePartnerFulfillmentLink(1, 2);
    }

    @Test
    @DisplayName("Обе линки для кроссдока существуют")
    void bothLinkExistSupplier() {
        mockLinks(2, Set.of(100L, 3L));

        pushFfLinkToMbiProcessor.processPayload(new ShopIdPartnerIdPayload("1", 3, 2));

        verify(mbiApiClient).getPartnerFulfillments(2);

        verify(lmsClient).searchPartners(partnerFilter());
    }

    @Test
    @DisplayName("Обе линки для кроссдока не существуют")
    void bothLinkNotExistSupplier() {
        mockLinks(2, Set.of());

        pushFfLinkToMbiProcessor.processPayload(new ShopIdPartnerIdPayload("1", 3, 2));

        verify(mbiApiClient).getPartnerFulfillments(2);
        verify(mbiApiClient).updatePartnerFulfillmentLink(2, 3);
        verify(mbiApiClient).updatePartnerFulfillmentLink(2, 100);

        verify(lmsClient).searchPartners(partnerFilter());
    }

    @Test
    @DisplayName("Нет линки кроссдока с партнером")
    void linkWithPartnerNotExistSupplier() {
        mockLinks(2, Set.of(100L));

        pushFfLinkToMbiProcessor.processPayload(new ShopIdPartnerIdPayload("1", 3, 2));

        verify(mbiApiClient).getPartnerFulfillments(2);
        verify(mbiApiClient).updatePartnerFulfillmentLink(2, 3);

        verify(lmsClient).searchPartners(partnerFilter());
    }

    @Test
    @DisplayName("Нет линки кроссдока с дефолтным фулфиллментом кроссдока")
    void linkWithDefaultFfNotExistSupplier() {
        mockLinks(2, Set.of(3L));
        pushFfLinkToMbiProcessor.processPayload(new ShopIdPartnerIdPayload("1", 3, 2));
        verify(lmsClient).searchPartners(partnerFilter());
        verify(mbiApiClient).getPartnerFulfillments(2);
        verify(mbiApiClient).updatePartnerFulfillmentLink(2, 100);
    }

    @Test
    @DisplayName("Нет фулфиллмента для кроссдок поставщиков по умолчанию")
    void noDefaultFf() {
        mockLinks(2, Set.of(3L));
        when(lmsClient.searchPartners(partnerFilter())).thenReturn(List.of());
        softly.assertThatCode(() -> pushFfLinkToMbiProcessor.processPayload(new ShopIdPartnerIdPayload("1", 3, 2)))
            .hasMessage("Failed to find partners with DEFAULT_SUPPLIER_FULFILLMENT == 1");
        verify(mbiApiClient).getPartnerFulfillments(2);
        verify(lmsClient).searchPartners(partnerFilter());
    }

    private void mockLinks(int partnerId, Set<Long> services) {
        when(mbiApiClient.getPartnerFulfillments(partnerId))
            .thenReturn(getPartnerFulfillmentLinksDTO(partnerId, services));
    }

    @Nonnull
    private SearchPartnerFilter partnerFilter() {
        return SearchPartnerFilter.builder()
            .setTypes(Set.of(PartnerType.FULFILLMENT))
            .setStatuses(PartnerUtils.VALID_GLOBAL_STATUSES)
            .setExternalParamsIntersection(Set.of(
                new PartnerExternalParamRequest(PartnerExternalParamType.DEFAULT_SUPPLIER_FULFILLMENT, "1")
            ))
            .build();
    }
}
