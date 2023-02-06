package ru.yandex.market.checkout.providers.v2.multicart.request;

import javax.annotation.Nonnull;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.BuyerDeviceInfoRequest;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.BuyerRequest;
import ru.yandex.market.checkout.helpers.utils.configuration.ActualizationRequestConfiguration;

public final class BuyerRequestProvider {

    private BuyerRequestProvider() {
    }

    public static BuyerRequest.Builder buildBuyer() {
        return BuyerRequest.builder()
                .withUserId(123L)
                .withIp("ip")
                .withRegionId(213L)
                .withCurrency(Currency.RUR)
                .withLastName("Ivan")
                .withFirstName("Ivanov")
                .withMiddleName("Ivanovich")
                .withPhone("+7 (916) 123-45-67")
                .withEmail("ivanov@ya.ru")
                .withAssessor(true)
                .withIpRegionId(123L)
                .withUserAgent("userAgent")
                .withBindKey("bindKey")
                .withUnreadImportantEvents(1L)
                .withUserEsiaToken("userEsiaToken");
    }

    @Nonnull
    public static BuyerRequest fromBuyer(@Nonnull ActualizationRequestConfiguration requestConfiguration,
                                         @Nonnull MultiCart multicart) {
        Buyer buyer = multicart.getBuyer();
        Currency buyerCurrency = multicart.getBuyerCurrency();

        BuyerRequest.Builder buyerBuilder = BuyerRequest.builder()
                .withIp(buyer.getIp())
                .withRegionId(buyer.getRegionId())
                .withCurrency(buyerCurrency)
                .withLastName(buyer.getLastName())
                .withFirstName(buyer.getFirstName())
                .withMiddleName(buyer.getMiddleName())
                .withPhone(buyer.getPhone())
                .withPersonalPhoneId(buyer.getPersonalPhoneId())
                .withEmail(buyer.getEmail())
                .withAssessor(buyer.getAssessor())
                .withBusinessBalanceId(buyer.getBusinessBalanceId())
                .withIpRegionId(buyer.getIpRegionId())
                .withUserAgent(buyer.getUserAgent())
                .withBindKey(buyer.getBindKey())
                .withUnreadImportantEvents(buyer.getUnreadImportantEvents())
                .withUserEsiaToken(buyer.getUserEsiaToken());

        if (CollectionUtils.isNonEmpty(multicart.getCarts()) && multicart.getCarts().get(0).getDelivery() != null) {
            buyerBuilder.withAddress(
                    AddressRequestProvider.fromAddress(multicart.getCarts().get(0).getDelivery().getBuyerAddress()));
        }

        if (buyer.getUid() != null) {
            buyerBuilder
                    .withUserId(buyer.getUid());
        }
        if (buyer.getMuid() != null) {
            buyerBuilder
                    .withUserId(buyer.getMuid());
        }
        if (buyer.getYandexUid() != null) {
            buyerBuilder
                    .withDeviceInfo(BuyerDeviceInfoRequest.builder()
                            .withYandexUid(buyer.getYandexUid())
                            .build());
        } else if (requestConfiguration.getGoogleServiceId() != null) {
            buyerBuilder
                    .withDeviceInfo(BuyerDeviceInfoRequest.builder()
                            .withGoogleServiceId(requestConfiguration.getGoogleServiceId())
                            .build());
        } else if (requestConfiguration.getIosDeviceId() != null) {
            buyerBuilder
                    .withDeviceInfo(BuyerDeviceInfoRequest.builder()
                            .withIosDeviceId(requestConfiguration.getIosDeviceId())
                            .build());
        }

        return buyerBuilder.build();
    }
}
