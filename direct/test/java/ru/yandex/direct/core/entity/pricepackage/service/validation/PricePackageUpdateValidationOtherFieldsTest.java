package ru.yandex.direct.core.entity.pricepackage.service.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_GEO;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_GEO_TYPE;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_RETARGETING_CONDITION;
import static ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тест на валидацию property'ей для которых нет специальных тестов.
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PricePackageUpdateValidationOtherFieldsTest extends PricePackageUpdateValidationTestBase {

    private static final Set<ModelProperty<?, ?>> OTHER_PROPERTIES = ImmutableSet.of(
            PricePackage.TRACKER_URL, PricePackage.PRICE, PricePackage.CURRENCY,
            PricePackage.ORDER_VOLUME_MIN, PricePackage.ORDER_VOLUME_MAX, PricePackage.TARGETINGS_FIXED,
            PricePackage.TARGETINGS_CUSTOM, PricePackage.DATE_START, PricePackage.IS_PUBLIC);

    @Test
    public void managerCanChangeNewPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NEW);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceManager, modelChanges, pricePackage);
        assertNoDefects(result);
    }

    @Test
    public void managerCanChangeWaitingPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.WAITING);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceManager, modelChanges, pricePackage);
        assertNoDefects(result);
    }

    @Test
    public void managerCanChangeRejectedPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NO);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceManager, modelChanges, pricePackage);
        assertNoDefects(result);
    }

    @Test
    public void managerCanNotChangeApprovedPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.YES);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceManager, modelChanges, pricePackage);
        assertDefects(result);
    }

    @Test
    public void managerCanNotChangeArchivedPackage() {
        var pricePackage = activePricePackageWithIsArchived(true);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceManager, modelChanges, pricePackage);
        assertDefects(result);
    }

    @Test
    public void managerCanNotChangeIsArchived() {
        var pricePackage = activePricePackageWithIsArchived(false);
        var modelChanges = ModelChanges.build(pricePackage, PricePackage.IS_ARCHIVED, true);
        var result = validate(priceManager, modelChanges, pricePackage);
        var validationResult = result.get().get(0).getValidationResult();
        assertThat(validationResult).is(matchedBy(hasDefectWithDefinition(validationError(
                path(), forbiddenToChange()))));
    }

    @Test
    public void approverCanNotChangeNewPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NEW);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceApprover, modelChanges, pricePackage);
        assertDefects(result);
    }

    @Test
    public void approverCanNotChangeWaitingPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.WAITING);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceApprover, modelChanges, pricePackage);
        assertDefects(result);
    }

    @Test
    public void approverCanNotChangeRejectedPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NO);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceApprover, modelChanges, pricePackage);
        assertDefects(result);
    }

    @Test
    public void approverCanNotChangeApprovedPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.YES);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceApprover, modelChanges, pricePackage);
        assertDefects(result);
    }

    @Test
    public void approverCanNotChangeArchivedPackage() {
        var pricePackage = activePricePackageWithIsArchived(true);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceApprover, modelChanges, pricePackage);
        assertDefects(result);
    }

    @Test
    public void approverCanChangeIsArchived() {
        var pricePackage = activePricePackageWithIsArchived(false);
        var modelChanges = ModelChanges.build(pricePackage, PricePackage.IS_ARCHIVED, true);
        var result = validate(priceApprover, modelChanges, pricePackage);
        assertNoDefects(result);
    }

    @Test
    public void supportCanNotChangeNewPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NEW);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(support, modelChanges, pricePackage);
        assertDefects(result);
    }

    @Test
    public void supportCanNotChangeWaitingPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.WAITING);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(support, modelChanges, pricePackage);
        assertDefects(result);
    }

    @Test
    public void supportCanNotChangeRejectedPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NO);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(support, modelChanges, pricePackage);
        assertDefects(result);
    }

    @Test
    public void supportCanNotChangeApprovedPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.YES);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(support, modelChanges, pricePackage);
        assertDefects(result);
    }

    @Test
    public void supportCanNotChangeArchivedPackage() {
        var pricePackage = activePricePackageWithIsArchived(true);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(support, modelChanges, pricePackage);
        assertDefects(result);
    }

    @Test
    public void supportCanChangeIsArchived() {
        var pricePackage = activePricePackageWithIsArchived(false);
        var modelChanges = ModelChanges.build(pricePackage, PricePackage.IS_ARCHIVED, true);
        var result = validate(support, modelChanges, pricePackage);
        assertNoDefects(result);
    }

    private ModelChanges<PricePackage> modelChanges(PricePackage pricePackage) {
        return new ModelChanges<>(pricePackage.getId(), PricePackage.class)
                .process("OtherFieldsTest new title", PricePackage.TITLE)
                .process("http://OtherFieldsTest.url", PricePackage.TRACKER_URL)
                .process(BigDecimal.valueOf(47), PricePackage.PRICE)
                .process(CurrencyCode.USD, PricePackage.CURRENCY)
                .process(90L, PricePackage.ORDER_VOLUME_MIN)
                .process(846L, PricePackage.ORDER_VOLUME_MAX)
                .process(newTargetingsFixed(), PricePackage.TARGETINGS_FIXED)
                .process(newTargetingsCustom(), PricePackage.TARGETINGS_CUSTOM)
                .process(LocalDate.now().plusDays(8), PricePackage.DATE_START)
                .process(LocalDate.now().plusDays(8), PricePackage.DATE_END)
                .process(true, PricePackage.IS_PUBLIC);
    }

    private static TargetingsFixed newTargetingsFixed() {
        return new TargetingsFixed()
                .withGeo(null)
                .withGeoType(null)
                .withViewTypes(List.of(ViewType.DESKTOP))
                .withAllowExpandedDesktopCreative(true);
    }

    private static TargetingsCustom newTargetingsCustom() {
        return new TargetingsCustom()
                .withGeo(DEFAULT_GEO)
                .withGeoType(DEFAULT_GEO_TYPE)
                .withRetargetingCondition(DEFAULT_RETARGETING_CONDITION);
    }

    private void assertNoDefects(Optional<MassResult<Long>> result) {
        assertThat(result).isEmpty();
    }

    private void assertDefects(Optional<MassResult<Long>> result) {
        assertThat(result).isNotEmpty();

        var validationResult = result.get().get(0).getValidationResult();
        var matchers = OTHER_PROPERTIES.stream()
                .map(prop -> validationError(path(), forbiddenToChange()))
                .collect(Collectors.toList());

        SoftAssertions assertions = new SoftAssertions();
        matchers.forEach(matcher ->
                assertions.assertThat(validationResult).is(matchedBy(hasDefectWithDefinition(matcher))));
        assertions.assertAll();
    }

}
