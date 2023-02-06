package ru.yandex.market.promo.dyn;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import Market.DataCamp.DataCampPromo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.supplier.promo.model.PromoType;
import ru.yandex.market.core.supplier.promo.model.StrategyType;
import ru.yandex.market.promo.model.AdditionalInfoYt;
import ru.yandex.market.promo.model.AnaplanPromoStatus;
import ru.yandex.market.promo.model.BluePromocodeYt;
import ru.yandex.market.promo.model.CompensationType;
import ru.yandex.market.promo.model.ErrorPromo;
import ru.yandex.market.promo.model.ExportOperationalPromoYtTable;
import ru.yandex.market.promo.model.ExportRestrictionYtTable;
import ru.yandex.market.promo.model.PromoCheckInfo;
import ru.yandex.market.promo.model.PromoConstraintsYt;
import ru.yandex.market.promo.model.PromoResponsibleYt;
import ru.yandex.market.promo.model.PromoValidator;
import ru.yandex.market.promo.model.PromocodeType;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.supplier.promo.service.PromoService.getDateInSeconds;

public class PromoValidatorTest extends FunctionalTest {

    @Test
    public void testValidateDiscount() {
        PromoResponsibleYt promoResponsible = createResponsible();
        ExportOperationalPromoYtTable promo = new ExportOperationalPromoYtTable.Builder()
                .withId("id")
                .withType(PromoType.DISCOUNT)
                .withStatus(AnaplanPromoStatus.READY)
                .withAdditionalInfo(
                        new AdditionalInfoYt.Builder()
                                .withCompensation(CompensationType.MARKET)
                                .withStrategyType(StrategyType.CATEGORY)
                                .withPiPublishDate("2020-12-01T00:00:00")
                                .withSendPromoPi(true)
                                .build())
                .withPromoConstraints(
                        new PromoConstraintsYt.Builder()
                                .withStartDate("2020-12-02T00:00:00")
                                .build())
                .withPromoCheckInfo(getPromoCheckInfo())
                .withPromoResponsible(promoResponsible)
                .build();
        ExportRestrictionYtTable restriction =
                new ExportRestrictionYtTable.Builder("id")
                        .withPromosHidRestrictionCategory(
                                Map.of(1L,
                                        new ExportRestrictionYtTable.HidCategory.Builder()
                                                .withHid("1")
                                                .withDiscount("0")
                                                .build()
                                )
                        ).build();

        var resultError = PromoValidator.validate(promo, restriction, null, null, null);
        var expectedError = new ErrorPromo();
        expectedError.addError(PromoValidator.ErrorType.NO_ONE_CORRECT_CATEGORY);

        Assertions.assertEquals(expectedError.getError(), resultError.getError());
    }

    @Test
    public void testValidateCategoryDiscount() {
        PromoResponsibleYt promoResponsible = createResponsible();
        ExportOperationalPromoYtTable promo = new ExportOperationalPromoYtTable.Builder()
                .withId("#1")
                .withType(PromoType.DISCOUNT)
                .withStatus(AnaplanPromoStatus.READY)
                .withAdditionalInfo(
                        new AdditionalInfoYt.Builder()
                                .withCompensation(CompensationType.MARKET)
                                .withStrategyType(StrategyType.CATEGORY)
                                .withPiPublishDate("2020-12-01T00:00:00")
                                .withSendPromoPi(true)
                                .build())
                .withPromoConstraints(
                        new PromoConstraintsYt.Builder()
                                .withStartDate("2020-12-02T00:00:00")
                                .build())
                .withPromoCheckInfo(getPromoCheckInfo())
                .withPromoResponsible(promoResponsible)
                .build();

        Map<Long, ExportRestrictionYtTable.HidCategory> hids = new HashMap<>();
        hids.put(1L, new ExportRestrictionYtTable.HidCategory.Builder()
                .withHid("1")
                .withDiscount("0")
                .build());
        hids.put(2L, new ExportRestrictionYtTable.HidCategory.Builder()
                .withHid("2")
                .withDiscount("10")
                .build());
        ExportRestrictionYtTable restriction =
                new ExportRestrictionYtTable.Builder("id")
                        .withPromosHidRestrictionCategory(hids)
                        .build();

        var resultError = PromoValidator.validate(promo, restriction, null, null, null);
        var expectedError = new ErrorPromo();
        expectedError.addError(PromoValidator.ErrorType.INCORRECT_CATEGORY_DISCOUNT, 1, 0);

        Assertions.assertEquals(expectedError.getError(), resultError.getError());
        Assertions.assertEquals(restriction.getPromosHidRestrictionCategory().size(), 1);
        Assertions.assertEquals(restriction.getPromosHidRestrictionCategory().get(2L).getHid(), 2);
        Assertions.assertEquals(restriction.getPromosHidRestrictionCategory().get(2L).getDiscount(), 10);
    }

    private PromoCheckInfo getPromoCheckInfo() {
        return new PromoCheckInfo.Builder()
                .withPromoId("#1")
                .withOriginalRestrictionCategoryCount("0")
                .withOriginalExcludeRestrictionCategoryCount("0")
                .withOriginalBrandCount("0")
                .withOriginalExcludeBrandCount("0")
                .withChannelsCount("0")
                .withMskuCount("0")
                .withMskuExcludeCount("0")
                .withSupplierCount("0")
                .withSupplierExcludeCount("0")
                .withWarehouseCount("0")
                .withWarehouseExcludeCount("0")
                .withVendorTriggerCount("0")
                .withVendorTriggerExcludeCount("0")
                .withHidTriggerCount("0")
                .withHidTriggerExcludeCount("0")
                .withRegionCount("0")
                .withRegionExcludeCount("0")
                .withRegionTriggerCount("0")
                .withRegionTriggerExcludeCount("0")
                .build();
    }

    @Test
    public void testValidateCategoryExistence() {
        PromoResponsibleYt promoResponsible = createResponsible();
        ExportOperationalPromoYtTable promo = new ExportOperationalPromoYtTable.Builder()
                .withId("id")
                .withType(PromoType.DISCOUNT)
                .withStatus(AnaplanPromoStatus.READY)
                .withAdditionalInfo(
                        new AdditionalInfoYt.Builder()
                                .withCompensation(CompensationType.MARKET)
                                .withStrategyType(StrategyType.CATEGORY)
                                .withPiPublishDate("2020-12-01T00:00:00")
                                .withSendPromoPi(true)
                                .build())
                .withPromoConstraints(
                        new PromoConstraintsYt.Builder()
                                .withStartDate("2020-12-02T00:00:00")
                                .build())
                .withPromoCheckInfo(getPromoCheckInfo())
                .withPromoResponsible(promoResponsible)
                .build();

        Map<Long, ExportRestrictionYtTable.HidCategory> hids = new HashMap<>();
        hids.put(1L, new ExportRestrictionYtTable.HidCategory.Builder()
                .withHid("1")
                .withDiscount("5")
                .build());
        ExportRestrictionYtTable restriction =
                new ExportRestrictionYtTable.Builder("id")
                        .withPromosHidRestrictionCategory(hids).build();

        var resultError = PromoValidator.validate(promo, restriction, null, null, null);
        var expectedError = new ErrorPromo();
        ;

        Assertions.assertEquals(expectedError.getError(), resultError.getError());
    }


    @Test
    public void testValidatePreviousPromoDescription() {
        PromoResponsibleYt promoResponsible = createResponsible();
        String promoId = "id";
        PromoType mechanic = PromoType.DISCOUNT;
        String endDate = "2021-01-02T00:00:00";
        String endDateWithTime = "2021-01-02 00:00:00";
        String startDate = "2020-12-01T00:00:00";
        String startDateWithTime = "2020-12-01 00:00:00";
        String piPublishDate = "2020-12-01T00:00:00";
        String piPublishDateWithTime = "2020-12-01 00:00:00";
        LocalDateTime endDateLocal =
                LocalDateTime.parse(endDateWithTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime piPublishDateDateLocal =
                LocalDateTime.parse(piPublishDateWithTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime startDateLocal =
                LocalDateTime.parse(startDateWithTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        StrategyType strategyType = StrategyType.CATEGORY;
        ExportOperationalPromoYtTable promo = new ExportOperationalPromoYtTable.Builder()
                .withId(promoId)
                .withType(mechanic)
                .withStatus(AnaplanPromoStatus.READY)
                .withAdditionalInfo(
                        new AdditionalInfoYt.Builder()
                                .withCompensation(CompensationType.MARKET)
                                .withStrategyType(strategyType)
                                .withPiPublishDate(piPublishDate)
                                .withSendPromoPi(true)
                                .build())
                .withPromoConstraints(
                        new PromoConstraintsYt.Builder()
                                .withStartDate(startDate)
                                .withEndDate(endDate)
                                .build())
                .withPromoCheckInfo(getPromoCheckInfo())
                .withPromoResponsible(promoResponsible)
                .build();

        int fiveHoursInSeconds = 5 * 60 * 60;
        DataCampPromo.PromoDescription previousDescription =
                DataCampPromo.PromoDescription.newBuilder()
                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(promoId)
                                .build())
                        .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(mechanic.getPromoStoragePromoType())
                                .build())
                        .setAdditionalInfo(
                                DataCampPromo.PromoAdditionalInfo.newBuilder()
                                        .setPublishDatePi(getDateInSeconds(piPublishDateDateLocal))
                                        .setSendPromoPi(true)
                                        .setStrategyType(strategyType.getStrategyTypePromotionalStorage())
                                        .build())
                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                .setStartDate(getDateInSeconds(startDateLocal) + fiveHoursInSeconds)
                                .setEndDate(getDateInSeconds(endDateLocal) - fiveHoursInSeconds)
                                .build())
                        .build();

        ExportRestrictionYtTable restriction =
                new ExportRestrictionYtTable.Builder("id")
                        .withPromosHidRestrictionCategory(
                                Map.of(1L,
                                        new ExportRestrictionYtTable.HidCategory.Builder()
                                                .withHid("1")
                                                .withDiscount("5")
                                                .build()
                                )
                        ).build();

        var resultError = PromoValidator.validate(promo, restriction, null, previousDescription, null);
        var expectedError = new ErrorPromo();
        expectedError.addError(PromoValidator.ErrorType.CANT_REDUCE_START_DATE_FOR_CHANGES);
        expectedError.addError(PromoValidator.ErrorType.CANT_INCREASE_END_DATE_FOR_CHANGES);
        expectedError.addError(PromoValidator.ErrorType.CATEGORY_CANT_BE_NULL);

        Assertions.assertEquals(expectedError.getError(), resultError.getError());
    }


    @Test
    public void testValidatePreviousPromoDescriptionVendor() {
        PromoResponsibleYt promoResponsible = createResponsible();
        String promoId = "id";
        PromoType mechanic = PromoType.DISCOUNT;
        String endDate = "2021-01-02T00:00:00";
        String endDateWithTime = "2021-01-02 00:00:00";
        String startDate = "2020-12-01T00:00:00";
        String startDateWithTime = "2020-12-01 00:00:00";
        String piPublishDate = "2020-12-01T00:00:00";
        String piPublishDateWithTime = "2020-12-01 00:00:00";
        LocalDateTime endDateLocal =
                LocalDateTime.parse(endDateWithTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime piPublishDateDateLocal =
                LocalDateTime.parse(piPublishDateWithTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime startDateLocal =
                LocalDateTime.parse(startDateWithTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        StrategyType strategyType = StrategyType.VENDOR;
        ExportOperationalPromoYtTable promo = new ExportOperationalPromoYtTable.Builder()
                .withId(promoId)
                .withType(mechanic)
                .withStatus(AnaplanPromoStatus.CONFIRMED)
                .withAdditionalInfo(
                        new AdditionalInfoYt.Builder()
                                .withCompensation(CompensationType.MARKET)
                                .withStrategyType(strategyType)
                                .withPiPublishDate(piPublishDate)
                                .withSendPromoPi(true)
                                .build())
                .withPromoConstraints(
                        new PromoConstraintsYt.Builder()
                                .withStartDate(startDate)
                                .withEndDate(endDate)
                                .build())
                .withPromoCheckInfo(getPromoCheckInfo())
                .withPromoResponsible(promoResponsible)
                .build();

        DataCampPromo.PromoDescription previousDescription =
                DataCampPromo.PromoDescription.newBuilder()
                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(promoId)
                                .build())
                        .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(mechanic.getPromoStoragePromoType())
                                .build())
                        .setAdditionalInfo(
                                DataCampPromo.PromoAdditionalInfo.newBuilder()
                                        .setPublishDatePi(getDateInSeconds(piPublishDateDateLocal))
                                        .setSendPromoPi(true)
                                        .setStrategyType(strategyType.getStrategyTypePromotionalStorage())
                                        .build())
                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                .setStartDate(getDateInSeconds(startDateLocal) + 1)
                                .setEndDate(getDateInSeconds(endDateLocal) - 1)
                                .build())
                        .build();
        ExportRestrictionYtTable restriction =
                new ExportRestrictionYtTable.Builder("id")
                        .withPromosHidRestrictionCategory(
                                Map.of(1L,
                                        new ExportRestrictionYtTable.HidCategory.Builder()
                                                .withHid("1")
                                                .withDiscount("5")
                                                .build()
                                )
                        ).build();

        var resultError = PromoValidator.validate(promo, restriction, null, previousDescription, null);
        var expectedError = new ErrorPromo();

        Assertions.assertEquals(expectedError.getError(), resultError.getError());
    }

    @Test
    // промо не 1p, ее дата старта сдвинулась на более раннюю. Если бы было промо
    public void testValidateAllowedTypeForPi() {
        PromoResponsibleYt promoResponsible = createResponsible();
        String promoId = "id";
        PromoType mechanic = PromoType.BLUE_PROMOCODE;
        String endDate = "2021-01-02T00:00:00";
        String endDateWithTime = "2021-01-02 00:00:00";
        String startDate = "2020-12-01T00:00:00";
        String startDateWithTime = "2020-12-01 00:00:00";
        String piPublishDate = "2020-12-01T00:00:00";
        String piPublishDateWithTime = "2020-12-01 00:00:00";
        LocalDateTime endDateLocal =
                LocalDateTime.parse(endDateWithTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime piPublishDateDateLocal =
                LocalDateTime.parse(piPublishDateWithTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime startDateLocal =
                LocalDateTime.parse(startDateWithTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        StrategyType strategyType = StrategyType.VENDOR;
        ExportOperationalPromoYtTable promo = new ExportOperationalPromoYtTable.Builder()
                .withId(promoId)
                .withType(mechanic)
                .withStatus(AnaplanPromoStatus.CONFIRMED)
                .withAdditionalInfo(
                        new AdditionalInfoYt.Builder()
                                .withCompensation(CompensationType.MARKET)
                                .withStrategyType(strategyType)
                                .withPiPublishDate(piPublishDate)
                                .withSendPromoPi(true)
                                .build())
                .withBluePromocode(
                        new BluePromocodeYt.Builder()
                                .withPromocode("promocode")
                                .withTypePromocode(PromocodeType.VALUE)
                                .withPromocodeRatingRub(123)
                                .build())
                .withPromoConstraints(
                        new PromoConstraintsYt.Builder()
                                .withStartDate(startDate)
                                .withEndDate(endDate)
                                .build())
                .withPromoCheckInfo(getPromoCheckInfo())
                .withPromoResponsible(promoResponsible)
                .build();

        DataCampPromo.PromoDescription previousDescription =
                DataCampPromo.PromoDescription.newBuilder()
                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(promoId)
                                .build())
                        .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(mechanic.getPromoStoragePromoType())
                                .build())
                        .setAdditionalInfo(
                                DataCampPromo.PromoAdditionalInfo.newBuilder()
                                        .setPublishDatePi(getDateInSeconds(piPublishDateDateLocal))
                                        .setSendPromoPi(true)
                                        .setStrategyType(strategyType.getStrategyTypePromotionalStorage())
                                        .build())
                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                .setStartDate(getDateInSeconds(startDateLocal) + 3)
                                .setEndDate(getDateInSeconds(endDateLocal) - 1)
                                .build())
                        .build();
        ExportRestrictionYtTable restriction =
                new ExportRestrictionYtTable.Builder("id")
                        .withPromosHidRestrictionCategory(
                                Map.of(1L,
                                        new ExportRestrictionYtTable.HidCategory.Builder()
                                                .withHid("1")
                                                .withDiscount("5")
                                                .build()
                                )
                        ).build();

        var resultError = PromoValidator.validate(promo, restriction, null, previousDescription, null);
        var expectedError = new ErrorPromo();

        Assertions.assertEquals(expectedError.getError(), resultError.getError());
    }

    @DisplayName("Проверка на то, что ограничения уже опубликованной акции не могут сужаться.")
    @Test
    public void testValidateNarrowedRestrictions() {
        PromoResponsibleYt promoResponsible = createResponsible();
        String promoId = "id";
        PromoType mechanic = PromoType.DISCOUNT;
        String endDate = "2029-01-02T00:00:00";
        String endDateWithTime = "2029-01-02 00:00:00";
        String startDate = "2020-12-01T00:00:00";
        String startDateWithTime = "2020-12-01 00:00:00";
        String piPublishDate = "2020-12-01T00:00:00";
        String piPublishDateWithTime = "2020-12-01 00:00:00";
        LocalDateTime endDateLocal =
                LocalDateTime.parse(endDateWithTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime piPublishDateDateLocal =
                LocalDateTime.parse(piPublishDateWithTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime startDateLocal =
                LocalDateTime.parse(startDateWithTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        StrategyType strategyType = StrategyType.NATIONAL;
        ExportOperationalPromoYtTable promo = new ExportOperationalPromoYtTable.Builder()
                .withId(promoId)
                .withType(mechanic)
                .withStatus(AnaplanPromoStatus.READY)
                .withAdditionalInfo(
                        new AdditionalInfoYt.Builder()
                                .withCompensation(CompensationType.MARKET)
                                .withStrategyType(strategyType)
                                .withPiPublishDate(piPublishDate)
                                .withSendPromoPi(true)
                                .build())
                .withPromoConstraints(
                        new PromoConstraintsYt.Builder()
                                .withStartDate(startDate)
                                .withEndDate(endDate)
                                .build())
                .withPromoCheckInfo(getPromoCheckInfo())
                .withPromoResponsible(promoResponsible)
                .withPromoCheckInfo(new PromoCheckInfo.Builder()
                        .withMskuCount("0")
                        .withChannelsCount("0")
                        .withHidTriggerCount("0")
                        .withHidTriggerExcludeCount("0")
                        .withMskuExcludeCount("0")
                        .withOriginalBrandCount("0")
                        .withOriginalExcludeBrandCount("0")
                        .withPromoId(promoId)
                        .withOriginalExcludeRestrictionCategoryCount("0")
                        .withOriginalRestrictionCategoryCount("1")
                        .withRegionCount("0")
                        .withRegionExcludeCount("0")
                        .withRegionTriggerCount("0")
                        .withRegionTriggerExcludeCount("0")
                        .withSupplierCount("2")
                        .withSupplierExcludeCount("0")
                        .withVendorTriggerCount("0")
                        .withVendorTriggerExcludeCount("0")
                        .withWarehouseCount("0")
                        .withWarehouseExcludeCount("0")
                        .build())
                .build();

        DataCampPromo.PromoDescription previousDescription =
                DataCampPromo.PromoDescription.newBuilder()
                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(promoId)
                                .build())
                        .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(mechanic.getPromoStoragePromoType())
                                .build())
                        .setAdditionalInfo(
                                DataCampPromo.PromoAdditionalInfo.newBuilder()
                                        .setPublishDatePi(getDateInSeconds(piPublishDateDateLocal))
                                        .setSendPromoPi(true)
                                        .setStrategyType(strategyType.getStrategyTypePromotionalStorage())
                                        .build())
                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                        .setOrigionalCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction.newBuilder()
                                                .addIncludeCategegoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                        .setId(1)
                                                        .setMinDiscount(5)
                                                        .build())
                                                .addIncludeCategegoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                        .setId(2)
                                                        .setMinDiscount(5)
                                                        .build())
                                                .addIncludeCategegoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                        .setId(3)
                                                        .setMinDiscount(5)
                                                        .build())
                                                .build()
                                        )
                                        .setSupplierRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.SupplierRestriction.newBuilder()
                                                .setSuppliers(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                        .addId(111)
                                                        .addId(222)
                                                        .addId(333)
                                                        .build())
                                                .build())
                                        .build())
                                .setStartDate(getDateInSeconds(startDateLocal) + 1)
                                .setEndDate(getDateInSeconds(endDateLocal) - 1)
                                .build())
                        .build();
        ExportRestrictionYtTable restriction =
                new ExportRestrictionYtTable.Builder("id")
                        .withPromosHidRestrictionCategory(
                                Map.of(1L,
                                                new ExportRestrictionYtTable.HidCategory.Builder()
                                                        .withHid("1")
                                                        .withDiscount("5")
                                                        .build()
                                )
                        )
                        .addSuppliers("111", "false")
                        .addSuppliers("333", "false")
                        .withOriginalRestrictionCategory(
                                new ExportRestrictionYtTable.OriginalCategoryRestriction("false")
                                        .addCategory("1", "5")
                        ).build();

        var resultError = PromoValidator.validate(promo, restriction, null, previousDescription, null);
        var expectedError = new ErrorPromo();
        expectedError.addError(PromoValidator.ErrorType.CAN_NOT_DELETE_SUPPLIERS_FROM_RESTRICTIONS, "222");
        expectedError.addError(PromoValidator.ErrorType.CAN_NOT_DELETE_CATEGORY_FROM_RESTRICTIONS, "2, 3");

        assertThat(resultError.getError()).isEqualTo(expectedError.getError());
    }

    /**
     * Минимальный необходимый набор полей для ответственных за акцию.
     */
    private PromoResponsibleYt createResponsible() {
        return new PromoResponsibleYt.Builder()
                .withAuthor("author")
                .withMarcomLogin("marcom")
                .withTmLogin("tmLogin")
                .build();
    }
}
