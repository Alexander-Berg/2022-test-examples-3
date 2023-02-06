package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithPricePackage;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.DateDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.defect.DateDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.defect.NumberDefects.inInterval;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CampaignWithPricePackageValidatorTest extends CampaignWithPricePackageValidatorTestBase {

    private ClientId clientId = ClientId.fromLong(6L);

    @Test
    public void valid() {
        var pricePackage = defaultPricePackage(clientId);
        var campaign = validCampaign();

        var result = validate(pricePackage, campaign);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void orderVolumeBelowMin() {
        var pricePackage = defaultPricePackage(clientId)
                .withOrderVolumeMin(100L)
                .withOrderVolumeMax(1000L);
        var campaign = validCampaign()
                .withFlightOrderVolume(99L);

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_ORDER_VOLUME)),
                inInterval(100L, 1000L))));
    }

    @Test
    public void orderVolumeAboveMax() {
        var pricePackage = defaultPricePackage(clientId)
                .withOrderVolumeMin(100L)
                .withOrderVolumeMax(1000L);
        var campaign = validCampaign()
                .withFlightOrderVolume(1001L);

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_ORDER_VOLUME)),
                inInterval(100L, 1000L))));
    }

    @Test
    public void startDateNull() {
        var pricePackage = defaultPricePackage(clientId);
        var campaign = validCampaign()
                .withStartDate(null);

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.START_DATE)),
                notNull())));
    }

    @Test
    public void startDateBeforePackageDateStart() {
        var pricePackage = defaultPricePackage(clientId)
                .withDateStart(LocalDate.of(2020, 1, 1))
                .withDateEnd(LocalDate.of(2020, 12, 1));
        var campaign = validCampaign()
                .withStartDate(LocalDate.of(2019, 12, 31))
                .withEndDate(LocalDate.of(2020, 12, 1));

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.START_DATE)),
                greaterThanOrEqualTo(LocalDate.of(2020, 1, 1)))));
    }

    @Test
    public void endDateNull() {
        var pricePackage = defaultPricePackage(clientId);
        var campaign = validCampaign()
                .withEndDate(null);

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.END_DATE)),
                notNull())));
    }

    @Test
    public void endDateAfterPackageDateEnd() {
        var pricePackage = defaultPricePackage(clientId)
                .withDateStart(LocalDate.of(2020, 1, 1))
                .withDateEnd(LocalDate.of(2020, 12, 1));
        var campaign = validCampaign()
                .withStartDate(LocalDate.of(2020, 1, 1))
                .withEndDate(LocalDate.of(2020, 12, 2));

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.END_DATE)),
                lessThanOrEqualTo(LocalDate.of(2020, 12, 1)))));
    }

    @Test
    public void currencyIncorrect() {
        var pricePackage = defaultPricePackage(clientId)
                .withCurrency(CurrencyCode.RUB);
        var campaign = validCampaign()
                .withCurrency(CurrencyCode.USD);

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.CURRENCY)),
                invalidValue())));
    }

    private ValidationResult<CampaignWithPricePackage, Defect> validate(PricePackage pricePackage,
                                                                        CpmPriceCampaign campaign) {
        var validator = new CampaignWithPricePackageValidator(Map.of(pricePackage.getId(), pricePackage),
                Collections.emptySet());
        return validator.apply(campaign);
    }
}
