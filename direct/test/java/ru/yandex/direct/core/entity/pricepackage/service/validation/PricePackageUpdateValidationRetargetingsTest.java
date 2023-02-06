package ru.yandex.direct.core.entity.pricepackage.service.validation;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PriceRetargetingCondition;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.pricepackage.service.validation.defects.PricePackageDefects.retargetingCategoriesAmountGreaterUpperLimit;
import static ru.yandex.direct.core.entity.pricepackage.service.validation.defects.PricePackageDefects.retargetingCategoriesAmountLessLowerLimit;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CINEMA_GENRES_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.FEMALE_CRYPTA_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.SPORT_GOAL_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PricePackageUpdateValidationRetargetingsTest extends PricePackageUpdateValidationTestBase {
    @Test
    public void zeroLowerCountOneCryptaSegment_Success() {
        PriceRetargetingCondition retargetingCondition = new PriceRetargetingCondition()
                                        .withCryptaSegments(List.of(FEMALE_CRYPTA_GOAL_ID))
                                        .withLowerCryptaTypesCount(0);

        var result = runValidateWithNewRetargeting(retargetingCondition);
        assertNoDefects(result);
    }

    @Test
    public void nullLowerCountOneCryptaSegment_Success() {
        PriceRetargetingCondition retargetingCondition = new PriceRetargetingCondition()
                .withCryptaSegments(List.of(FEMALE_CRYPTA_GOAL_ID))
                .withLowerCryptaTypesCount(null);

        var result = runValidateWithNewRetargeting(retargetingCondition);
        assertNoDefects(result);
    }

    @Test
    public void oneLowerCountOneCryptaSegment_Success() {
        PriceRetargetingCondition retargetingCondition = new PriceRetargetingCondition()
                .withCryptaSegments(List.of(FEMALE_CRYPTA_GOAL_ID))
                .withLowerCryptaTypesCount(1);

        var result = runValidateWithNewRetargeting(retargetingCondition);
        assertNoDefects(result);
    }

    @Test
    public void twoLowerCountOneCryptaSegment_Error() {
        PriceRetargetingCondition retargetingCondition = new PriceRetargetingCondition()
                .withCryptaSegments(List.of(FEMALE_CRYPTA_GOAL_ID))
                .withLowerCryptaTypesCount(2);
        var result = runValidateWithNewRetargeting(retargetingCondition);
        assertLowerLimitDefects(result);
    }

    @Test
    public void genreAndCategoryGlued_Error() {
        PriceRetargetingCondition retargetingCondition = new PriceRetargetingCondition()
                .withCryptaSegments(List.of(CINEMA_GENRES_GOAL_ID, 4294968319L)) //Книги и литература (content_category)
                .withLowerCryptaTypesCount(2);
        var result = runValidateWithNewRetargeting(retargetingCondition);
        assertLowerLimitDefects(result);
    }

    @Test
    public void audienceAndMetrikaNotGlued_Success() {
        PriceRetargetingCondition retargetingCondition = new PriceRetargetingCondition()
                .withCryptaSegments(List.of(FEMALE_CRYPTA_GOAL_ID))
                .withAllowAudienceSegments(true)
                .withAllowMetrikaSegments(true)
                .withLowerCryptaTypesCount(3);
        var result = runValidateWithNewRetargeting(retargetingCondition);
        assertNoDefects(result);
    }

    @Test
    public void cryptaSegmentsCountLessUpperLimit_Success() {
        List<Long> cryptaSegments = List.of(FEMALE_CRYPTA_GOAL_ID, SPORT_GOAL_ID);
        int upperCryptaTypesLimit = 3;
        var result = runValidateWithNewCryptaSegments(cryptaSegments, upperCryptaTypesLimit);
        assertNoDefects(result);
    }

    @Test
    public void cryptaSegmentsCountEqualToUpperLimit_Success() {
        List<Long> cryptaSegments = List.of(FEMALE_CRYPTA_GOAL_ID, SPORT_GOAL_ID);
        int upperCryptaTypesLimit = 2;
        var result = runValidateWithNewCryptaSegments(cryptaSegments, upperCryptaTypesLimit);
        assertNoDefects(result);
    }

    @Test
    public void cryptaSegmentsCountGreaterUpperLimit_Error() {
        List<Long> cryptaSegments = List.of(FEMALE_CRYPTA_GOAL_ID, SPORT_GOAL_ID);
        int upperCryptaTypesLimit = 1;
        var result = runValidateWithNewCryptaSegments(cryptaSegments, upperCryptaTypesLimit);
        assertUpperLimitDefects(result);
    }

    @Test
    public void zeroUpperLimit_Success() {
        List<Long> cryptaSegments = List.of(FEMALE_CRYPTA_GOAL_ID, SPORT_GOAL_ID);
        int upperCryptaTypesLimit = 0;
        var result = runValidateWithNewCryptaSegments(cryptaSegments, upperCryptaTypesLimit);
        assertNoDefects(result);
    }

    private Optional<MassResult<Long>> runValidateWithNewRetargeting(PriceRetargetingCondition retargetingCondition) {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NEW);
        var modelChanges = createTargetingsCustomModelChanges(pricePackage, retargetingCondition);
        return validate(priceManager, modelChanges, pricePackage);
    }

    private Optional<MassResult<Long>> runValidateWithNewCryptaSegments(List<Long> cryptaSegments, int upperCryptaTypesLimit) {
        TargetingsCustom targetingsCustom = createDefaultTargetingsCustomWithUpperLimit(upperCryptaTypesLimit);
        var pricePackage = activePricePackageWithTargetingsCustom(targetingsCustom);
        var modelChanges = createTargetingsFixedModelChanges(pricePackage, cryptaSegments);
        return validate(priceManager, modelChanges, pricePackage);
    }

    private TargetingsCustom createDefaultTargetingsCustomWithUpperLimit(int upperCryptaTypesLimit) {
        return new TargetingsCustom()
                .withRetargetingCondition(new PriceRetargetingCondition()
                        .withAllowAudienceSegments(true)
                        .withAllowMetrikaSegments(false)
                        .withLowerCryptaTypesCount(0)
                        .withUpperCryptaTypesCount(upperCryptaTypesLimit)
                        .withCryptaSegments(emptyList()));
    }

    private void assertNoDefects(Optional<MassResult<Long>> result) {
        assertThat(result).isEmpty();
    }

    private void assertLowerLimitDefects(Optional<MassResult<Long>> result) {
        assertThat(result).isNotEmpty();

        assertThat(result.get().get(0).getValidationResult()).is(
                matchedBy(hasDefectWithDefinition(validationError(path(field(PricePackage.TARGETINGS_CUSTOM)),
                        retargetingCategoriesAmountLessLowerLimit()))));
    }

    private void assertUpperLimitDefects(Optional<MassResult<Long>> result) {
        assertThat(result).isNotEmpty();

        assertThat(result.get().get(0).getValidationResult()).is(
                matchedBy(hasDefectWithDefinition(validationError(path(
                        field(PricePackage.TARGETINGS_FIXED), field(TargetingsFixed.CRYPTA_SEGMENTS)),
                        retargetingCategoriesAmountGreaterUpperLimit()))));
    }

    private ModelChanges<PricePackage> createTargetingsCustomModelChanges(PricePackage pricePackage,
            PriceRetargetingCondition retargetingCondition) {
        return ModelChanges.build(pricePackage, PricePackage.TARGETINGS_CUSTOM,
                pricePackage.getTargetingsCustom().withRetargetingCondition(
                        retargetingCondition.withUpperCryptaTypesCount(0)));
    }

    private ModelChanges<PricePackage> createTargetingsFixedModelChanges(PricePackage pricePackage,
           List<Long> cryptaSegments) {
        return ModelChanges.build(pricePackage, PricePackage.TARGETINGS_FIXED,
                pricePackage.getTargetingsFixed().withCryptaSegments(cryptaSegments));
    }
}
