package ru.yandex.market.delivery.mdbapp.integration.converter;

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import steps.shopSteps.PaymentStatusSteps;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.components.geo.Location;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.LocationFetcher;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.Sender;
import ru.yandex.market.logistic.gateway.common.model.delivery.Taxation;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;
import ru.yandex.market.mbi.api.client.entity.shops.ShopOrgInfo;

import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static steps.orderSteps.OrderSteps.getFilledOrder;
import static steps.shopSteps.ShopSteps.getDefaultShop;

@RunWith(MockitoJUnitRunner.class)
public class SenderConverterTest {

    @Mock
    private LocationFetcher locationFetcher;

    private SenderConverter senderConverter;

    private final SoftAssertions assertions = new SoftAssertions();

    @Before
    public void setUp() {
        senderConverter = new SenderConverter(locationFetcher, new TaxConverter());
    }

    @After
    public void after() {
        assertions.assertAll();
    }

    @Test
    public void convertSenderSuccessTest() {
        List<ShopOrgInfo> shopOrgInfos = Collections.singletonList(
            new ShopOrgInfo("ooo",
                "1023500000160",
                "name",
                "FACT_ADDRESS",
                "JURIDICAL_ADDRESS",
                "ya_money",
                "registration_number",
                "info_url"
            ));

        Shop shop = getDefaultShop(1,
            shopOrgInfos,
            PaymentStatusSteps.getPaymentStatus());

        when(locationFetcher.fetchNullable(refEq(shop)))
            .thenReturn(new Location()
                .setCountry("Россия")
                .setLocality("Москва")
                .setRegion("Москва и Московская область"));

        ru.yandex.market.logistic.gateway.common.model.delivery.Location convertedLocation =
            new ru.yandex.market.logistic.gateway.common.model.delivery.Location.LocationBuilder(
                "Россия", "Москва", "Москва и Московская область")
                .build();

        Sender sender = senderConverter.convert(shop, getFilledOrder());

        verify(locationFetcher).fetchNullable(refEq(shop));

        assertions.assertThat(sender.getOgrn())
            .as("Check ogrn after convert to Sender")
            .isEqualTo("1023500000160");
        assertions.assertThat(sender.getAddress())
            .as("Check address after convert to Sender")
            .isEqualToComparingFieldByFieldRecursively(convertedLocation);
        assertions.assertThat(sender.getIncorporation())
            .as("Check incorporation after convert to Sender")
            .isEqualTo("ООО name");
        assertions.assertThat(sender.getPhones())
            .as("Check phones count after convert to Sender")
            .hasSize(1);
        assertions.assertThat(sender.getPhones().get(0))
            .as("Check phone after convert to Sender")
            .isEqualToComparingFieldByFieldRecursively(new Phone("+71234567890", null));
        assertions.assertThat(sender.getType())
            .as("Check type after convert to Sender")
            .isEqualTo("ooo");
        assertions.assertThat(sender.getTaxation())
            .as("Check taxation after convert to Sender")
            .isEqualTo(Taxation.ENVD);
    }

    @Test
    public void convertSenderForBlueOrder() {
        List<ShopOrgInfo> shopOrgInfos = Collections.singletonList(
            new ShopOrgInfo("ooo",
                "1023500000160",
                "name",
                "FACT_ADDRESS",
                "JURIDICAL_ADDRESS",
                "ya_money",
                "registration_number",
                "info_url"
            ));

        Shop shop = getDefaultShop(1,
            shopOrgInfos,
            PaymentStatusSteps.getPaymentStatus());

        when(locationFetcher.fetchNullable(refEq(shop)))
            .thenReturn(new Location()
                .setCountry("Россия")
                .setLocality("Москва")
                .setRegion("Москва и Московская область"));

        ru.yandex.market.logistic.gateway.common.model.delivery.Location convertedLocation =
            new ru.yandex.market.logistic.gateway.common.model.delivery.Location.LocationBuilder(
                "Россия", "Москва", "Москва и Московская область")
                .build();

        Sender sender = senderConverter.convert(shop, getFilledOrder(), "Беру");

        verify(locationFetcher).fetchNullable(refEq(shop));

        assertions.assertThat(sender.getOgrn())
            .as("Check ogrn after convert to Sender")
            .isEqualTo("1023500000160");
        assertions.assertThat(sender.getAddress())
            .as("Check address after convert to Sender")
            .isEqualToComparingFieldByFieldRecursively(convertedLocation);
        assertions.assertThat(sender.getIncorporation())
            .as("Check incorporation after convert to Sender")
            .isEqualTo("ООО name");
        assertions.assertThat(sender.getPhones())
            .as("Check phones count after convert to Sender")
            .hasSize(1);
        assertions.assertThat(sender.getPhones().get(0))
            .as("Check phone after convert to Sender")
            .isEqualToComparingFieldByFieldRecursively(new Phone("+71234567890", null));
        assertions.assertThat(sender.getType())
            .as("Check type after convert to Sender")
            .isEqualTo("ooo");
        assertions.assertThat(sender.getTaxation())
            .as("Check taxation after convert to Sender")
            .isEqualTo(Taxation.ENVD);
    }

    @Test
    public void convertSenderWithMinimumData() {
        List<ShopOrgInfo> shopOrgInfos = Collections.singletonList(
            new ShopOrgInfo(null,
                "1023500000160",
                null,
                null,
                null,
                "ya_money",
                "registration_number",
                "info_url"
            ));

        Shop shop = new Shop(
            1,
            null,
            null,
            null,
            null,
            shopOrgInfos,
            null,
            null,
            false,
            false,
            null,
            null,
            null,
            false
        );

        when(locationFetcher.fetchNullable(refEq(shop)))
            .thenReturn(null);

        Sender sender = senderConverter.convert(shop, new Order());

        verify(locationFetcher).fetchNullable(refEq(shop));

        assertions.assertThat(sender.getOgrn())
            .as("Check ogrn after convert to Sender")
            .isEqualTo("1023500000160");
        assertions.assertThat(sender.getPhones())
            .as("Check phone after convert to Sender")
            .isEmpty();
        assertions.assertThat(sender.getIncorporation())
            .as("Check incorporation after convert to Sender")
            .isEqualTo("");
        assertions.assertThat(sender.getAddress())
            .as("Check address after convert to Sender")
            .isNull();
        assertions.assertThat(sender.getType())
            .as("Check type after convert to Sender")
            .isNull();
        assertions.assertThat(sender.getTaxation())
            .as("Check taxation after convert to Sender")
            .isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertSenderWrongInfoSourceTest() {
        senderConverter.convert(getDefaultShop(), getFilledOrder());
    }
}
