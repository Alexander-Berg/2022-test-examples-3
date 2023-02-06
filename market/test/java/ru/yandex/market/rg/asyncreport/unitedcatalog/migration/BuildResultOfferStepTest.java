package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.core.asyncreport.model.unitedcatalog.CopyOffersParams;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class BuildResultOfferStepTest {
    private static final int OLD_BUSINESS_ID = 1;
    private static final int NEW_BUSINESS_ID = 2;
    private static final int SERVICE_ID = 10;
    private static final String OFFER_ID = "test-offer-id";

    BuildResultOfferStep buildStep;

    @BeforeEach
    void init() {
        buildStep = new BuildResultOfferStep(
                new CopyOfferStrategy(true),
                new CopyOffersParams(OLD_BUSINESS_ID, NEW_BUSINESS_ID, SERVICE_ID)
        );
    }

    @Test
    void empty() {
        assertDoesNotThrow(() -> buildStep.accept(Collections.emptyList()));
    }

    @Test
    void oneEmptyItem() {
        var item = List.of(BusinessMigration.MergeOffersRequestItem.newBuilder());
        buildStep.accept(item);
        var expected = BusinessMigration.MergeOffersRequestItem.newBuilder();
        expected.getResultBuilder()
                .getBasicBuilder()
                .getIdentifiersBuilder()
                .setBusinessId(NEW_BUSINESS_ID);
        expected.setConflictResolutionStrategy(BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE);
        assertThat(item.get(0).build(), equalTo(expected.build()));

    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("categoriesParams")
    void categories(
            BusinessMigration.MergeOffersRequestItem original,
            DataCampOffer.Offer expectedBasic,
            @SuppressWarnings("unused") String desc
    ) {
        var item = List.of(
                original.toBuilder()
        );
        buildStep.accept(item);
        var actual = item.get(0).getResult().getBasic();
        assertThat(actual, equalTo(expectedBasic));
    }

    static Stream<Arguments> categoriesParams() {
        return Stream.of(
                Arguments.of(
                        BusinessMigration.MergeOffersRequestItem.newBuilder()
                                .setSource(UnitedOfferBuilder.offerBuilder(OLD_BUSINESS_ID, SERVICE_ID, OFFER_ID)
                                        .withActualCategory(100)
                                        .build())
                                .build(),
                        UnitedOfferBuilder.offerBuilder(NEW_BUSINESS_ID, SERVICE_ID, OFFER_ID)
                                .withActualCategory(100)
                                .build().getBasic(),
                        "actual category"
                ),
                Arguments.of(
                        BusinessMigration.MergeOffersRequestItem.newBuilder()
                                .setSource(UnitedOfferBuilder.offerBuilder(OLD_BUSINESS_ID, SERVICE_ID, OFFER_ID)
                                        .withOriginalCategory(100)
                                        .build())
                                .build(),
                        UnitedOfferBuilder.offerBuilder(NEW_BUSINESS_ID, SERVICE_ID, OFFER_ID)
                                .withOriginalCategory(100)
                                .build().getBasic(),
                        "original category"
                ),
                Arguments.of(
                        BusinessMigration.MergeOffersRequestItem.newBuilder()
                                .setSource(UnitedOfferBuilder.offerBuilder(OLD_BUSINESS_ID, SERVICE_ID, OFFER_ID)
                                        .withActualCategory(100)
                                        .withOriginalCategory(100)
                                        .build())
                                .build(),
                        UnitedOfferBuilder.offerBuilder(NEW_BUSINESS_ID, SERVICE_ID, OFFER_ID)
                                .withActualCategory(100)
                                .withOriginalCategory(100)
                                .build().getBasic(),
                        "original and actual categories both"
                )
        );
    }

    @Test
    @DisplayName("Проверка resultOffer с merge")
    void sourceAndTargetWithMerge() {
        var item = List.of(BusinessMigration.MergeOffersRequestItem.newBuilder()
                .setSource(UnitedOfferBuilder.offerBuilder(OLD_BUSINESS_ID, SERVICE_ID, OFFER_ID)
                        .withName("name source")
                        .withBarcode("barcodeSrc")
                        .withVendorCode("vendorSrc", "vendorCodeSrc")
                        .withContentSystemStatus(DataCampContentStatus.ContentSystemStatus.newBuilder()
                                .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY)
                                .build())
                        .build())
                .setTarget(UnitedOfferBuilder.offerBuilder(OLD_BUSINESS_ID, SERVICE_ID, OFFER_ID)
                        .build()));
        buildStep.accept(item);
        var expected = UnitedOfferBuilder.offerBuilder(NEW_BUSINESS_ID, SERVICE_ID, OFFER_ID)
                .withName("name source")
                .withBarcode("barcodeSrc")
                .withVendorCode("vendorSrc", "vendorCodeSrc")
                .withContentSystemStatus(DataCampContentStatus.ContentSystemStatus.newBuilder().build())
                .build();
        assertThat(item.get(0).build().getResult(), equalTo(expected));

    }

}
