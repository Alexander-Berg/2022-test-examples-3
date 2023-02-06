package ru.yandex.market.partner.notification.service.providers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.core.xml.impl.NamedWithAttributesContainer;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypesRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypesResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerWithPlacementProgramTypeDTO;
import ru.yandex.market.notification.common.model.destination.MbiDestination;
import ru.yandex.market.notification.common.model.destination.UserUidDestination;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.transport.NotificationDestination;
import ru.yandex.market.notification.service.provider.content.TelegramBotContentProvider;
import ru.yandex.market.notification.simple.model.data.ArrayListNotificationData;
import ru.yandex.market.partner.notification.service.external.MbiClient;
import ru.yandex.market.partner.notification.service.providers.data.PartnerPlacementAndCampaignTypeDataProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO.CLICK_AND_COLLECT;
import static ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO.CPC;
import static ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO.CROSSDOCK;
import static ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO.DROPSHIP;
import static ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO.FULFILLMENT;
import static ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO.TURBO_PLUS;

class PartnerPlacementAndCampaignTypeDataProviderTest {
    private MbiOpenApiClient mbiApiClientMock;
    private NotificationContext notificationContextMock;
    private PartnerPlacementAndCampaignTypeDataProvider dataProvider;

    @BeforeEach
    void setUp() {
        mbiApiClientMock = mock(MbiOpenApiClient.class);
        notificationContextMock = mock(NotificationContext.class);
        dataProvider = new PartnerPlacementAndCampaignTypeDataProvider(new MbiClient(mbiApiClientMock));
    }

    @Test
    void supplier() {
        initApiClientMock(Map.of(12345L, List.of(CROSSDOCK, FULFILLMENT, CROSSDOCK, TURBO_PLUS)));
        initContextMock(List.of(
                UserUidDestination.create(777L),
                MbiDestination.create(12345L, null, null)
        ));

        var result = (ArrayListNotificationData<NamedContainer>) dataProvider.provide(notificationContextMock);

        assertThat(result, containsInAnyOrder(
                new NamedWithAttributesContainer(
                        "partner-placement-types",
                        "|CROSSDOCK|FULFILLMENT|CROSSDOCK|TURBO_PLUS|",
                        Map.of(TelegramBotContentProvider.DISABLE_MARKDOWN_ESCAPING_ATTR, true)),
                new NamedContainer(
                        "campaign-type",
                        "SUPPLIER"
                )
        ));

        verify(mbiApiClientMock).providePartnerPlacementProgramTypes(
                new PartnerPlacementProgramTypesRequest()
                        .partnerIds(List.of(12345L))
        );
    }

    @Test
    void shop() {
        initApiClientMock(Map.of(12345L, List.of(CPC)));
        initContextMock(List.of(
                UserUidDestination.create(777L),
                MbiDestination.create(12345L, null, null)
        ));

        var result = (ArrayListNotificationData<NamedContainer>) dataProvider.provide(notificationContextMock);

        assertThat(result, containsInAnyOrder(
                new NamedWithAttributesContainer(
                        "partner-placement-types",
                        "|CPC|",
                        Map.of(TelegramBotContentProvider.DISABLE_MARKDOWN_ESCAPING_ATTR, true)),
                new NamedContainer(
                        "campaign-type",
                        "SHOP"
                )
        ));

        verify(mbiApiClientMock).providePartnerPlacementProgramTypes(
                new PartnerPlacementProgramTypesRequest()
                        .partnerIds(List.of(12345L))
        );
    }

    @Test
    void emptyPlacementProgramTypes() {
        initApiClientMock(Map.of(123456L, Collections.emptyList()));
        initContextMock(List.of(
                UserUidDestination.create(777L),
                MbiDestination.create(123456L, null, null)
        ));

        var result = (ArrayListNotificationData<NamedContainer>) dataProvider.provide(notificationContextMock);

        assertThat(result, empty());

        verify(mbiApiClientMock).providePartnerPlacementProgramTypes(
                new PartnerPlacementProgramTypesRequest()
                        .partnerIds(List.of(123456L))
        );
    }

    @Test
    void emptyPlacementProgramTypesResponse() {
        initApiClientMock(Collections.emptyMap());
        initContextMock(List.of(
                UserUidDestination.create(777L),
                MbiDestination.create(123456L, null, null)
        ));

        var result = (ArrayListNotificationData<NamedContainer>) dataProvider.provide(notificationContextMock);

        assertThat(result, empty());

        verify(mbiApiClientMock).providePartnerPlacementProgramTypes(
                new PartnerPlacementProgramTypesRequest()
                        .partnerIds(List.of(123456L))
        );
    }

    @Test
    void incorrectMixOfSupplierAndShopPlacementPrograms() {
        initApiClientMock(Map.of(12345L, List.of(DROPSHIP, CPC)));
        initContextMock(List.of(
                UserUidDestination.create(777L),
                MbiDestination.create(12345L, null, null)
        ));

        var result = (ArrayListNotificationData<NamedContainer>) dataProvider.provide(notificationContextMock);

        assertThat(result, containsInAnyOrder(
                new NamedWithAttributesContainer(
                        "partner-placement-types",
                        "|DROPSHIP|CPC|",
                        Map.of(TelegramBotContentProvider.DISABLE_MARKDOWN_ESCAPING_ATTR, true))
        ));

        verify(mbiApiClientMock).providePartnerPlacementProgramTypes(
                new PartnerPlacementProgramTypesRequest()
                        .partnerIds(List.of(12345L))
        );
    }

    @Test
    void placementTypesNotConvertibleToCampaignType() {
        initApiClientMock(Map.of(12345L, List.of(TURBO_PLUS, CLICK_AND_COLLECT)));
        initContextMock(List.of(
                UserUidDestination.create(777L),
                MbiDestination.create(12345L, null, null)
        ));

        var result = (ArrayListNotificationData<NamedContainer>) dataProvider.provide(notificationContextMock);

        assertThat(result, containsInAnyOrder(
                new NamedWithAttributesContainer(
                        "partner-placement-types",
                        "|TURBO_PLUS|CLICK_AND_COLLECT|",
                        Map.of(TelegramBotContentProvider.DISABLE_MARKDOWN_ESCAPING_ATTR, true))
        ));

        verify(mbiApiClientMock).providePartnerPlacementProgramTypes(
                new PartnerPlacementProgramTypesRequest()
                        .partnerIds(List.of(12345L))
        );
    }

    @Test
    void notShopDestination() {
        initApiClientMock(Map.of(12345L, List.of(TURBO_PLUS, CLICK_AND_COLLECT)));
        initContextMock(List.of(
                UserUidDestination.create(777L)
        ));

        var result = (ArrayListNotificationData<NamedContainer>) dataProvider.provide(notificationContextMock);

        assertThat(result, empty());

        verifyNoInteractions(mbiApiClientMock);
    }

    private void initContextMock(List<NotificationDestination> destinations) {
        when(notificationContextMock.getDestinations()).thenReturn(destinations);
    }

    private void initApiClientMock(Map<Long, List<PartnerPlacementProgramTypeDTO>> types) {
        var response = new PartnerPlacementProgramTypesResponse()
                .programTypes(
                        types.entrySet().stream()
                                .map(entry -> new PartnerWithPlacementProgramTypeDTO()
                                        .partnerId(entry.getKey())
                                        .programTypes(entry.getValue())
                                ).collect(Collectors.toList())
                );
        when(mbiApiClientMock.providePartnerPlacementProgramTypes(any())).thenReturn(response);
    }
}
