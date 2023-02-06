package ru.yandex.direct.core.entity.adgroup.service.validation.types;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoEmptyRegions;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoIncorrectRegions;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.CENTRAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.URYUPINSK_REGION_ID;
import static ru.yandex.direct.regions.Region.KYIV_REGION_ID;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.StringUtils.joinLongsToString;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PriceSalesSpecificAdGroupGeoValidatorTest {

    @Autowired
    private Steps steps;

    @Autowired
    private PricePackageService pricePackageService;

    private PriceSalesSpecificAdGroupGeoValidator validator;

    private CpmPriceCampaign campaign;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        PricePackage pricePackage = approvedPricePackage()
                .withTargetingsCustom(emptyTargetingsCustom());
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA))
                .withGeoType(REGION_TYPE_COUNTRY);
        steps.pricePackageSteps().createPricePackage(pricePackage);

        campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);

        validator = new PriceSalesSpecificAdGroupGeoValidator(pricePackageService.getGeoTree(), campaign, pricePackage, null);
    }

    @Test
    public void geoNull() {
        ValidationResult<List<Long>, Defect> result = validateGeo(null);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(path(), notNull()))));
    }

    @Test
    public void geoItemNull() {
        List<Long> geo = new ArrayList<>();
        geo.add(null);
        ValidationResult<List<Long>, Defect> result = validateGeo(geo);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(path(), geoEmptyRegions()))));
    }

    @Test
    public void geoSubTreeOfCampaignGeo() {
        ValidationResult<List<Long>, Defect> result = validateGeo(List.of(URYUPINSK_REGION_ID, CENTRAL_DISTRICT));
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void geoNotSubTreeOfCampaignGeo() {
        ValidationResult<List<Long>, Defect> result = validateGeo(List.of(KYIV_REGION_ID));
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(path(), invalidValue()))));
    }

    @Test
    public void minusRegionsIgnored() {
        ValidationResult<List<Long>, Defect> result =
                validateGeo(List.of(URYUPINSK_REGION_ID, CENTRAL_DISTRICT, -KYIV_REGION_ID));
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void nonExistingRegion() {
        List<Long> nonExistingRegions = List.of(Long.MAX_VALUE);
        ValidationResult<List<Long>, Defect> result = validateGeo(nonExistingRegions);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(path(),
                geoIncorrectRegions(joinLongsToString(nonExistingRegions))))));
    }

    private ValidationResult<List<Long>, Defect> validateGeo(List<Long> adGroupGeo) {
        return validator.apply(adGroupGeo);
    }
}
