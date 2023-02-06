package ru.yandex.market.marketpromo.core.validation;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ru.yandex.market.marketpromo.core.test.generator.DatacampOfferPromoMechanics;
import ru.yandex.market.marketpromo.core.test.generator.Offers;
import ru.yandex.market.marketpromo.model.DatacampOffer;
import ru.yandex.market.marketpromo.model.SupplierType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.marketpromo.core.test.utils.GenerateTestHelper.someString;

public class DatacampOfferValidatorTest {

    @Test
    void shouldBeReadyForCreationIfAllCorrect() {
        assertThat(DatacampOfferValidator.readyForCreation(Offers.datacampOffer(
                Offers.shopSku(someString()),
                Offers.name(someString()),
                Offers.price(100),
                Offers.potentialPromo(someString()),
                Offers.supplierType(SupplierType._1P),
                Offers.activePromos(
                        DatacampOfferPromoMechanics.directDiscount(someString())
                )
        )), is(true));
    }

    @Test
    void shouldBeReadyForCreationIfNoActivePromo() {
        assertThat(DatacampOfferValidator.readyForCreation(Offers.datacampOffer(
                Offers.shopSku(someString()),
                Offers.name(someString()),
                Offers.supplierType(SupplierType._1P),
                Offers.price(100),
                Offers.potentialPromo(someString())
        )), is(true));
    }

    @Test
    void shouldNotBeReadyForCreationIfNoPromo() {
        assertThat(DatacampOfferValidator.readyForCreation(Offers.datacampOffer(
                Offers.shopSku(someString()),
                Offers.name(someString()),
                Offers.price(100)
        )), is(false));
    }

    @Test
    void shouldNotBeReadyForCreationIfDisabled() {
        assertThat(DatacampOfferValidator.readyForCreation(Offers.datacampOffer(
                Offers.shopSku(someString()),
                Offers.name(someString()),
                Offers.price(100),
                Offers.disabled(true),
                Offers.potentialPromo(someString())
        )), is(false));
    }

    @Test
    void shouldNotBeReadyForCreationIfNoCategory() {
        assertThat(DatacampOfferValidator.readyForCreation(Offers.datacampOffer(
                Offers.shopSku(someString()),
                Offers.name(someString()),
                Offers.price(100),
                Offers.categoryId(null),
                Offers.potentialPromo(someString())
        )), is(false));
    }

    @Test
    void shouldNotBeReadyForCreationIfNoPrice() {
        assertThat(DatacampOfferValidator.readyForCreation(Offers.datacampOffer(
                Offers.shopSku(someString()),
                Offers.name(someString()),
                Offers.price(null),
                Offers.potentialPromo(someString())
        )), is(false));
    }

    @Test
    void shouldNotBeReadyForCreationIfNoName() {
        assertThat(DatacampOfferValidator.readyForCreation(Offers.datacampOffer(
                Offers.shopSku(someString()),
                Offers.price(100),
                Offers.name(null),
                Offers.potentialPromo(someString())
        )), is(false));
    }

    @Test
    void shouldNotBeReadyForCreationIfZeroBusinessId() {
        assertThat(DatacampOfferValidator.readyForCreation(Offers.datacampOffer(
                Offers.business(0),
                Offers.shopSku(someString()),
                Offers.name(someString()),
                Offers.price(100),
                Offers.potentialPromo(someString())
        )), is(false));
    }

    @Test
    void shouldNotBeReadyForCreationIfZeroShopId() {
        assertThat(DatacampOfferValidator.readyForCreation(Offers.datacampOffer(
                Offers.shop(0),
                Offers.shopSku(someString()),
                Offers.name(someString()),
                Offers.price(100),
                Offers.potentialPromo(someString())
        )), is(false));
    }

    @Test
    void shouldNotBeReadyForCreationIfZeroWarehouseId() {
        assertThat(DatacampOfferValidator.readyForCreation(Offers.datacampOffer(
                Offers.warehouse(0),
                Offers.shopSku(someString()),
                Offers.name(someString()),
                Offers.price(100),
                Offers.potentialPromo(someString())
        )), is(false));
    }

    @Test
    void shouldNotBeReadyForCreationIfZeroFeedId() {
        assertThat(DatacampOfferValidator.readyForCreation(Offers.datacampOffer(
                Offers.feed(0),
                Offers.shopSku(someString()),
                Offers.name(someString()),
                Offers.price(100),
                Offers.potentialPromo(someString())
        )), is(false));
    }

    @Test
    void shouldNotBeReadyForCreationIfEmptyShopSku() {
        assertThat(DatacampOfferValidator.readyForCreation(Offers.datacampOffer(
                Offers.shopSku(""),
                Offers.name(someString()),
                Offers.price(100),
                Offers.potentialPromo(someString())
        )), is(false));
    }

    @Test
    void shouldBeReadyForUpdateIfMinimalViewPresented() {
        assertThat(DatacampOfferValidator.readyForUpdate(DatacampOffer.builder()
                .shopSku(someString())
                .id(someString())
                .shopId(213)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()), is(true));
    }
}
