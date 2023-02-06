package ru.yandex.market.logistics.nesu.controller.own.delivery;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffDto;
import ru.yandex.market.logistics.tarifficator.model.enums.DeliveryMethod;
import ru.yandex.market.logistics.tarifficator.model.enums.TariffType;

import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@ParametersAreNonnullByDefault
public class TestOwnDeliveryUtils {

    public static final long OWN_PARTNER_ID = 45L;
    public static final long TARIFF_ID = 12L;
    public static final long PRICE_LIST_ID = 125L;

    private TestOwnDeliveryUtils() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static PartnerResponse.PartnerResponseBuilder partnerBuilder() {
        return PartnerResponse.newBuilder()
            .id(OWN_PARTNER_ID)
            .marketId(1L)
            .businessId(41L)
            .partnerType(PartnerType.OWN_DELIVERY)
            .name("own delivery")
            .readableName("readable own delivery")
            .status(PartnerStatus.ACTIVE);
    }

    @Nonnull
    public static TariffDto.TariffDtoBuilder tariffBuilder() {
        return TariffDto.builder()
            .archived(false)
            .currency("RUB")
            .code("trf_code")
            .deliveryMethod(DeliveryMethod.COURIER)
            .description("my tariff description")
            .enabled(true)
            .name("my tariff")
            .partnerId(OWN_PARTNER_ID)
            .type(TariffType.OWN_DELIVERY);
    }

    public static void mockSearchOwnDeliveries(List<PartnerResponse> result, LMSClient lmsClient) {
        mockSearchOwnDeliveries(result, lmsClient, ownDeliveryFilter(null).build());
    }

    public static void mockSearchOwnDeliveries(
        List<PartnerResponse> result,
        LMSClient lmsClient,
        SearchPartnerFilter filter
    ) {
        when(lmsClient.searchPartners(safeRefEq(filter))).thenReturn(result);
    }

    @Nonnull
    public static SearchPartnerFilter.Builder ownDeliveryFilter(@Nullable Set<Long> partnerIds) {
        return SearchPartnerFilter.builder()
            .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
            .setIds(partnerIds)
            .setMarketIds(Set.of(1L))
            .setTypes(Set.of(PartnerType.OWN_DELIVERY));
    }

    @Nonnull
    public static SearchPartnerFilter.Builder ownDeliveryFilter() {
        return SearchPartnerFilter.builder()
            .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
            .setMarketIds(Set.of(1L))
            .setStatuses(Set.of(PartnerStatus.ACTIVE))
            .setTypes(Set.of(PartnerType.OWN_DELIVERY));
    }
}
