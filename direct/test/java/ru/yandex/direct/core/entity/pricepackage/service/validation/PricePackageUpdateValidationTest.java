package ru.yandex.direct.core.entity.pricepackage.service.validation;

import java.util.List;
import java.util.Optional;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.DefectInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.pricepackage.service.validation.defects.PricePackageDefects.userTimestampNotEqualsLastUpdateTime;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PricePackageUpdateValidationTest extends PricePackageUpdateValidationTestBase {

    @Test
    public void outdatedTimestamp() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NEW);
        var modelChanges = modelChangesWithValidTitle(pricePackage);
        var outDatedTimestamp = pricePackage.getLastUpdateTime().minusDays(1);

        var result = validate(priceManager, modelChanges, outDatedTimestamp);
        assertDefects(result, validationError(path(),
                userTimestampNotEqualsLastUpdateTime(pricePackage.getLastUpdateTime())));
    }

    @Test
    public void duplicates() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NEW);
        var modelChanges = modelChangesWithValidTitle(pricePackage);

        var massResult = validate(priceManager, List.of(modelChanges, modelChanges),
                List.of(pricePackage.getLastUpdateTime(), pricePackage.getLastUpdateTime()));
        var result0 = massResult.get().getResult().get(0);
        var result1 = massResult.get().getResult().get(1);

        assertDefects(result0, validationError(path(), duplicatedElement()));
        assertDefects(result1, validationError(path(), duplicatedElement()));
    }

    @Test
    public void invalidPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NEW);
        var modelChanges = modelChangesWithTitle(pricePackage, "");

        var result = validate(priceManager, modelChanges, pricePackage);
        assertDefects(result, validationError(path(field(PricePackage.TITLE)), notEmptyString()));
    }

    @Test
    public void changeTitleInArchivedPackage() {
        var pricePackage = activePricePackageWithIsArchived(true);
        var modelChanges = modelChangesWithValidTitle(pricePackage);
        var result = validate(priceManager, modelChanges, pricePackage);
        assertDefects(result, validationError(path(), forbiddenToChange()));

        pricePackage = activePricePackageWithIsArchived(false);
        modelChanges = modelChangesWithValidTitle(pricePackage);
        result = validate(priceManager, modelChanges, pricePackage);
        assertThat(result).isEmpty();
    }

    private ModelChanges<PricePackage> modelChangesWithValidTitle(PricePackage pricePackage) {
        return modelChangesWithTitle(pricePackage, "New title");
    }

    private ModelChanges<PricePackage> modelChangesWithTitle(PricePackage pricePackage, String title) {
        return ModelChanges.build(pricePackage.getId(), PricePackage.class, PricePackage.TITLE, title);
    }

    private void assertDefects(Optional<MassResult<Long>> result, Matcher<DefectInfo<Defect>> defectMatcher) {
        assertThat(result).isNotEmpty();
        assertDefects(result.get().get(0), defectMatcher);
    }

    private void assertDefects(Result<Long> result, Matcher<DefectInfo<Defect>> defectMatcher) {
        assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(defectMatcher)));
    }

    @Test
    public void сanNotDisableBrandSafetyApprovedPackage() {
        var pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withCampaignOptions(defaultPricePackage().getCampaignOptions().withAllowBrandSafety(true))
                .withStatusApprove(StatusApprove.YES)
                .withIsArchived(false)
                .withCurrency(CurrencyCode.RUB))
                .getPricePackage();
        var modelChanges = ModelChanges.build(pricePackage.getId(), PricePackage.class,
                PricePackage.CAMPAIGN_OPTIONS, defaultPricePackage().getCampaignOptions().withAllowBrandSafety(false));

        var result = validate(priceManager, modelChanges, pricePackage);
        assertDefects(result, validationError(path(), DefectIds.FORBIDDEN_TO_CHANGE));
    }

    @Test
    public void сanEnableBrandSafetyApprovedPackage() {
        var pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withCampaignOptions(defaultPricePackage().getCampaignOptions().withAllowBrandSafety(false))
                .withStatusApprove(StatusApprove.YES)
                .withIsArchived(false)
                .withCurrency(CurrencyCode.RUB))
                .getPricePackage();
        var modelChanges = ModelChanges.build(pricePackage.getId(), PricePackage.class,
                PricePackage.CAMPAIGN_OPTIONS, defaultPricePackage().getCampaignOptions().withAllowBrandSafety(true));

        var result = validate(priceManager, modelChanges, pricePackage);
        assertThat(result).isEmpty();
    }
}
