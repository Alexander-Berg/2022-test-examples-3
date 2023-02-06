package ru.yandex.market.checkout.checkouter.promo;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Sets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author sergeykoles
 * Created on: 23.03.18
 */

public class LoyaltyClientDeviceTypeTest extends AbstractWebTestBase {

    private static final String REAL_PROMO_CODE = "REAL-PROMO-CODE";
    private static final String JPATH_CLIENT_DEVICE_TYPE = "$.operationContext.clientDeviceType";


    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(new Object[][]{
                // {platform, expected client_device_type, test_name
                {Platform.DESKTOP, UsageClientDeviceType.DESKTOP, null},
                {Platform.MOBILE_BROWSER, UsageClientDeviceType.TOUCH, null},
                {Platform.APP_WEBVIEW, UsageClientDeviceType.APPLICATION, null},
                {Platform.ANDROID, UsageClientDeviceType.APPLICATION, null},
                {Platform.IOS, UsageClientDeviceType.APPLICATION, null},
                {Platform.YANDEX_GO_IOS, UsageClientDeviceType.MARKET_GO, null},
                {Platform.YANDEX_GO_ANDROID, UsageClientDeviceType.MARKET_GO, null},
                {Platform.UNKNOWN, null, null}
        })
                .peek(c -> c[2] = String.valueOf(c[0]))
                .collect(Collectors.toList())
                .stream().map(Arguments::of);
    }

    private static void checkClientDeviceType(LoggedRequest a, UsageClientDeviceType expectedClientDeviceType) {
        if (expectedClientDeviceType != null) {
            JsonTest.checkJson(
                    a.getBodyAsString(),
                    JPATH_CLIENT_DEVICE_TYPE,
                    expectedClientDeviceType.getCode()
            );
        } else {
            JsonTest.checkJsonNotExist(a.getBodyAsString(), JPATH_CLIENT_DEVICE_TYPE);
        }
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testCartAndCheckoutClientDeviceType(Platform requestPlatform,
                                                    UsageClientDeviceType expectedClientDeviceType, String testName) {
        String offerTag = UUID.randomUUID().toString();
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.configureMultiCart(multiCart -> {
            Order order = multiCart.getCarts().get(0);
            order.addItem(OrderItemProvider.buildOrderItem("2_" + offerTag, 1L, 1));
            assertThat(order.getItems(), hasSize(2));

            parameters.getReportParameters().setOffers(order.getItems().stream()
                    .map(FoundOfferBuilder::createFrom)
                    .map(FoundOfferBuilder::build)
                    .collect(Collectors.toList()));

            order.setChanges(Sets.immutableEnumSet(CartChange.DELIVERY)); //проверка - игнорирование входящих changes
            order.getItems().forEach(item -> item.setChanges(EnumSet.of(ItemChange.PRICE)));
            multiCart.setPaymentMethod(PaymentMethod.YANDEX);
            multiCart.setPaymentType(PaymentType.PREPAID);
            multiCart.setPromoCode(REAL_PROMO_CODE);
        });
        parameters.getReportParameters().setShopSupportsSubsidies(true);
        parameters.setPlatform(requestPlatform);
        parameters.setMockLoyalty(true);

        Order order = orderCreateHelper.createOrder(parameters);

        Stream.of(LoyaltyConfigurer.URI_CALC_V3, LoyaltyConfigurer.URI_SPEND_V3)
                .map(WireMock::urlPathEqualTo)
                .map(WireMock::postRequestedFor)
                .map(builder -> builder.withRequestBody(WireMock.containing(offerTag)))
                .map(loyaltyConfigurer::findAll)
                .flatMap(List::stream)
                .forEach(a -> checkClientDeviceType(a, expectedClientDeviceType));
    }

}
