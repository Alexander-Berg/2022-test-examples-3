package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithPricePackage;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightTargetingsSnapshot;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.pricepackage.service.validation.PricePackageValidator.REGION_TYPE_REGION;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.URAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_PROVINCE;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CampaignWithPricePackageValidatorFlightTargetingsSnapshotTest extends
        CampaignWithPricePackageValidatorTestBase {

    private ClientId clientId = ClientId.fromLong(6L);

    @Test
    public void geoFixedValid() {
        var pricePackage = defaultPricePackageWithNullGeoFields();
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA, -URAL_DISTRICT))
                .withGeoType(REGION_TYPE_DISTRICT)
                .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT));
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withGeoType(REGION_TYPE_DISTRICT)
                .withGeoExpanded(List.of(SIBERIAN_DISTRICT, VOLGA_DISTRICT));

        var result = validate(pricePackage, campaign);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void geoCustomValid() {
        var pricePackage = defaultPricePackageWithNullGeoFields();
        pricePackage.getTargetingsCustom()
                .withGeo(List.of(RUSSIA))
                .withGeoType(REGION_TYPE_DISTRICT)
                .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT, URAL_DISTRICT));
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withGeoType(REGION_TYPE_DISTRICT)
                .withGeoExpanded(List.of(SIBERIAN_DISTRICT, VOLGA_DISTRICT));

        var result = validate(pricePackage, campaign);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void geoTypeNull() {
        var pricePackage = defaultPricePackage(clientId);
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withGeoType(null);

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_TARGETINGS_SNAPSHOT),
                field(PriceFlightTargetingsSnapshot.GEO_TYPE)),
                notNull())));
    }

    @Test
    public void geoTypeFixedInvalid() {
        var pricePackage = defaultPricePackageWithNullGeoFields();
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                .withGeoType(REGION_TYPE_DISTRICT)
                .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT));
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withGeoType(REGION_TYPE_PROVINCE)
                .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT));

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_TARGETINGS_SNAPSHOT),
                field(PriceFlightTargetingsSnapshot.GEO_TYPE)),
                invalidValue())));
    }

    @Test
    public void geoTypeCustomInvalid() {
        var pricePackage = defaultPricePackageWithNullGeoFields();
        pricePackage.getTargetingsCustom()
                .withGeo(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                .withGeoType(REGION_TYPE_DISTRICT)
                .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT));
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withGeoType(REGION_TYPE_PROVINCE)
                .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT));

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_TARGETINGS_SNAPSHOT),
                field(PriceFlightTargetingsSnapshot.GEO_TYPE)),
                invalidValue())));
    }

    @Test
    public void geoExpandedNull() {
        var pricePackage = defaultPricePackage(clientId);
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withGeoExpanded(null);

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_TARGETINGS_SNAPSHOT),
                field(PriceFlightTargetingsSnapshot.GEO_EXPANDED)),
                notNull())));
    }

    @Test
    public void geoExpandedEmpty() {
        var pricePackage = defaultPricePackage(clientId);
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withGeoExpanded(emptyList());

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_TARGETINGS_SNAPSHOT),
                field(PriceFlightTargetingsSnapshot.GEO_EXPANDED)),
                notEmptyCollection())));
    }

    @Test
    public void geoExpandedNotContainsAllFixed() {
        var pricePackage = defaultPricePackageWithNullGeoFields();
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                .withGeoType(REGION_TYPE_DISTRICT)
                .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT));
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withGeoType(REGION_TYPE_DISTRICT)
                .withGeoExpanded(List.of(VOLGA_DISTRICT));

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_TARGETINGS_SNAPSHOT),
                field(PriceFlightTargetingsSnapshot.GEO_EXPANDED)),
                invalidValue())));
    }

    @Test
    public void geoExpandedNotFromFixed() {
        var pricePackage = defaultPricePackageWithNullGeoFields();
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                .withGeoType(REGION_TYPE_DISTRICT)
                .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT));
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withGeoType(REGION_TYPE_DISTRICT)
                .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT, URAL_DISTRICT));

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_TARGETINGS_SNAPSHOT),
                field(PriceFlightTargetingsSnapshot.GEO_EXPANDED)),
                invalidValue())));
    }

    @Test
    public void geoExpandedNotFromCustom() {
        var pricePackage = defaultPricePackageWithNullGeoFields();
        pricePackage.getTargetingsCustom()
                .withGeo(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                .withGeoType(REGION_TYPE_DISTRICT)
                .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT));
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withGeoType(REGION_TYPE_DISTRICT)
                .withGeoExpanded(List.of(VOLGA_DISTRICT, URAL_DISTRICT));

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_TARGETINGS_SNAPSHOT),
                field(PriceFlightTargetingsSnapshot.GEO_EXPANDED)),
                invalidValue())));
    }

    @Test
    public void viewTypesValid() {
        var pricePackage = defaultPricePackage(clientId);
        pricePackage.getTargetingsFixed()
                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB));
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withViewTypes(List.of(ViewType.MOBILE, ViewType.DESKTOP, ViewType.NEW_TAB));

        var result = validate(pricePackage, campaign);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void viewTypesNull() {
        var pricePackage = defaultPricePackage(clientId);
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withViewTypes(null);

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_TARGETINGS_SNAPSHOT),
                field(PriceFlightTargetingsSnapshot.VIEW_TYPES)),
                notNull())));
    }

    @Test
    public void viewTypesNotContainsAllFixed() {
        var pricePackage = defaultPricePackage(clientId);
        pricePackage.getTargetingsFixed()
                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE));
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withViewTypes(List.of(ViewType.DESKTOP));

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_TARGETINGS_SNAPSHOT),
                field(PriceFlightTargetingsSnapshot.VIEW_TYPES)),
                invalidValue())));
    }

    @Test
    public void viewTypesNotFromFixed() {
        var pricePackage = defaultPricePackage(clientId);
        pricePackage.getTargetingsFixed()
                .withViewTypes(List.of(ViewType.DESKTOP));
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE));

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_TARGETINGS_SNAPSHOT),
                field(PriceFlightTargetingsSnapshot.VIEW_TYPES)),
                invalidValue())));
    }

    @Test
    public void allowExpandedDesktopCreativeNull() {
        var pricePackage = defaultPricePackage(clientId);
        var campaign = validCampaign();
        campaign.getFlightTargetingsSnapshot()
                .withAllowExpandedDesktopCreative(null);

        var result = validate(pricePackage, campaign);
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                field(CampaignWithPricePackage.FLIGHT_TARGETINGS_SNAPSHOT),
                field(PriceFlightTargetingsSnapshot.ALLOW_EXPANDED_DESKTOP_CREATIVE)),
                notNull())));
    }

    private PricePackage defaultPricePackageWithGeoType10() {
        var pricePackage = defaultPricePackage(clientId);
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoType(REGION_TYPE_REGION)
                .withGeoExpanded(null);
        pricePackage.getTargetingsCustom()
                .withGeo(emptyList())
                .withGeoType(REGION_TYPE_REGION)
                .withGeoExpanded(null);
        return pricePackage;
    }

    private PricePackage defaultPricePackageWithNullGeoFields() {
        var pricePackage = defaultPricePackage(clientId);
        pricePackage.getTargetingsFixed()
                .withGeo(null)
                .withGeoType(null)
                .withGeoExpanded(null);
        pricePackage.getTargetingsCustom()
                .withGeo(null)
                .withGeoType(null)
                .withGeoExpanded(null);
        return pricePackage;
    }

    private ValidationResult<CampaignWithPricePackage, Defect> validate(PricePackage pricePackage,
                                                                        CpmPriceCampaign campaign) {
        var validator = new CampaignWithPricePackageValidator(Map.of(pricePackage.getId(), pricePackage),
                Collections.emptySet());
        return validator.apply(campaign);
    }
}
