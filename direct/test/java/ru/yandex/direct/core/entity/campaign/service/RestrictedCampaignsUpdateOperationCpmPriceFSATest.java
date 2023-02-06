package ru.yandex.direct.core.entity.campaign.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPricePackage;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusCorrect;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.DefectIds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.CAMPAIGN_NO_WRITE_RIGHTS;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тесты на изменение поля flightStatusApprove прайсовой кампании
 */
@CoreTest
@RunWith(Parameterized.class)
public class RestrictedCampaignsUpdateOperationCpmPriceFSATest extends
        RestrictedCampaignsUpdateOperationCpmPriceTestBase {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameter
    public RbacRole operatorRole;

    @Parameter(1)
    public Boolean operatorDeveloper;

    @Parameter(2)
    public Supplier<LocalDate> startDateSupplier;

    @Parameter(3)
    public Supplier<LocalDate> endDateSupplier;

    @Parameter(4)
    public Long shows;

    @Parameter(5)
    public PriceFlightStatusCorrect statusCorrect;

    @Parameter(6)
    public PriceFlightStatusApprove statusApprove;

    @Parameter(7)
    public PriceFlightStatusApprove newStatusApprove;

    @Parameter(8)
    public Result result;

    enum Result {Success, ValidationError}

    private CampaignWithPricePackage campaign;

    @Parameters()
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // клиент не может сбрасвыать statusApprove
                {RbacRole.CLIENT, false, TOMORROW, PLUS_MONTHS, 1L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NO, PriceFlightStatusApprove.NEW, Result.ValidationError},
                // клиент не может сбрасвыать statusApprove, даже если ещё нет показов и кампания не началась
                {RbacRole.CLIENT, false, TOMORROW, PLUS_MONTHS, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NO, PriceFlightStatusApprove.NEW, Result.ValidationError},
                // обычный SUPERREADER не может сбрасвыать statusApprove, даже если ещё нет показов и кампания не
                // началась
                {RbacRole.SUPERREADER, false, TOMORROW, PLUS_MONTHS, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NO, PriceFlightStatusApprove.NEW, Result.ValidationError},
                // SUPERREADER-developer не может сбрасвыать statusApprove, даже если ещё нет показов и кампания не
                // началась
                {RbacRole.SUPERREADER, true, TOMORROW, PLUS_MONTHS, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NO, PriceFlightStatusApprove.NEW, Result.ValidationError},
                // MANAGER может сбрасвыать statusApprove, если ещё нет показов и кампания не началась
                {RbacRole.MANAGER, false, TOMORROW, PLUS_MONTHS, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NO, PriceFlightStatusApprove.NEW, Result.Success},
                // SUPER может сбрасвыать statusApprove, если ещё нет показов и кампания не началась
                {RbacRole.SUPER, false, TOMORROW, PLUS_MONTHS, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NO, PriceFlightStatusApprove.NEW, Result.Success},
                // SUPPORT может сбрасвыать statusApprove, если ещё нет показов и кампания не началась
                {RbacRole.SUPPORT, false, TOMORROW, PLUS_MONTHS, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NO, PriceFlightStatusApprove.NEW, Result.Success},
                // клиент не может аппрувить
                {RbacRole.CLIENT, false, TOMORROW, PLUS_MONTHS, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NEW, PriceFlightStatusApprove.YES, Result.ValidationError},
                // до начала кампании new -> no
                {RbacRole.SUPER, false, TOMORROW, PLUS_MONTHS, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NEW, PriceFlightStatusApprove.NO, Result.Success},
                // до начала кампании new -> yes
                {RbacRole.SUPER, false, TOMORROW, PLUS_MONTHS, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NEW, PriceFlightStatusApprove.YES, Result.Success},
                // до начала кампании yes -> no
                {RbacRole.SUPER, false, TOMORROW, PLUS_MONTHS, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.YES, PriceFlightStatusApprove.NO, Result.Success},
                // до начала кампании no -> yes
                {RbacRole.SUPER, false, TOMORROW, PLUS_MONTHS, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NO, PriceFlightStatusApprove.YES, Result.Success},

                // нельзя сбрасвыать statusApprove, если уже есть показы
                {RbacRole.CLIENT, false, MINUS_MONTHS, TODAY, 1L, PriceFlightStatusCorrect.NO,
                        PriceFlightStatusApprove.NO, PriceFlightStatusApprove.NEW, Result.ValidationError},
                // можно сбрасвыать statusApprove, если ещё нет показов и statusCorrect = NO
                {RbacRole.MANAGER, false, MINUS_MONTHS, TODAY, 0L, PriceFlightStatusCorrect.NO,
                        PriceFlightStatusApprove.NO, PriceFlightStatusApprove.NEW, Result.Success},
                // можно сбрасвыать statusApprove, если ещё нет показов и statusCorrect = NEW
                {RbacRole.SUPER, false, MINUS_MONTHS, TODAY, 0L, PriceFlightStatusCorrect.NEW,
                        PriceFlightStatusApprove.NO, PriceFlightStatusApprove.NEW, Result.Success},
                // клиент не может менять statusApprove
                {RbacRole.CLIENT, false, MINUS_MONTHS, TODAY, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NEW, PriceFlightStatusApprove.YES, Result.ValidationError},
                // кампания в процессе new -> no
                {RbacRole.SUPER, false, MINUS_MONTHS, TODAY, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NEW, PriceFlightStatusApprove.NO, Result.Success},
                // кампания в процессе new -> yes можно
                {RbacRole.SUPER, false, MINUS_MONTHS, TODAY, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NEW, PriceFlightStatusApprove.YES, Result.Success},
                // кампания в процессе yes -> no можно
                {RbacRole.SUPER, false, MINUS_MONTHS, TODAY, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.YES, PriceFlightStatusApprove.NO, Result.Success},
                // кампания в процессе no -> yes можно
                {RbacRole.SUPER, false, MINUS_MONTHS, TODAY, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NO, PriceFlightStatusApprove.YES, Result.Success},

                // клиент не может менять statusApprove
                {RbacRole.CLIENT, false, MINUS_MONTHS, YESTERDAY, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NEW, PriceFlightStatusApprove.YES, Result.ValidationError},
                // кампания закончилась new -> no
                {RbacRole.SUPER, false, MINUS_MONTHS, YESTERDAY, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NEW, PriceFlightStatusApprove.NO, Result.Success},
                // кампания закончилась new -> yes нельзя
                {RbacRole.SUPER, false, MINUS_MONTHS, YESTERDAY, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NEW, PriceFlightStatusApprove.YES, Result.ValidationError},
                // кампания закончилась yes -> no нельзя
                {RbacRole.SUPER, false, MINUS_MONTHS, YESTERDAY, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.YES, PriceFlightStatusApprove.NO, Result.ValidationError},
                // кампания закончилась no -> yes нельзя
                {RbacRole.SUPER, false, MINUS_MONTHS, YESTERDAY, 0L, PriceFlightStatusCorrect.YES,
                        PriceFlightStatusApprove.NO, PriceFlightStatusApprove.YES, Result.ValidationError},
        });
    }

    @Test
    public void changeFlightStatusApprove() {
        setupOperator(operatorRole, operatorDeveloper);
        switch (result) {
            case Success:
                changeFlightStatusApprove_Success();
                break;
            case ValidationError:
                changeFlightStatusApprove_Error();
                break;
        }
    }

    private void changeFlightStatusApprove_Success() {
        createTestCampaign();

        MassResult<Long> result = applyNewStatusApprove();

        assumeThat(result, isFullySuccessful());
        CampaignWithPricePackage actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign)
                .is(matchedBy(beanDiffer(updateOriginCampaignToExpected(campaign)
                        .withFlightStatusApprove(newStatusApprove)
                        .withStatusPostModerate(CampaignStatusPostmoderate.ACCEPTED)
                        .withStatusBsSynced(CampaignStatusBsSynced.NO)
                ).useCompareStrategy(cpmPriceCampaignCompareStrategy())));
    }

    private void changeFlightStatusApprove_Error() {
        createTestCampaign();

        MassResult<Long> result = applyNewStatusApprove();

        var field = operatorRole == RbacRole.SUPERREADER ? CpmPriceCampaign.ID :
                CpmPriceCampaign.FLIGHT_STATUS_APPROVE;
        var defect = operatorRole == RbacRole.SUPERREADER ? CAMPAIGN_NO_WRITE_RIGHTS :
                DefectIds.FORBIDDEN_TO_CHANGE;
        Assert.assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(field)),
                        defect))
        );
    }

    private void createTestCampaign() {
        campaign = defaultCampaign()
                .withFlightStatusApprove(statusApprove)
                .withFlightStatusCorrect(statusCorrect)
                .withStartDate(startDateSupplier.get())
                .withEndDate(endDateSupplier.get())
                .withShows(shows);
        if (operatorRole == RbacRole.MANAGER) {
            campaign.setManagerUid(operatorUid);
        }
        createPriceCampaign(campaign);

        steps.adGroupSteps().createDefaultAdGroupForPriceSales((CpmPriceCampaign) campaign, defaultClient);
    }

    private MassResult<Long> applyNewStatusApprove() {
        ModelChanges<CampaignWithPricePackage> modelChanges =
                ModelChanges.build(campaign, CampaignWithPricePackage.FLIGHT_STATUS_APPROVE, newStatusApprove);
        return apply(modelChanges);
    }

}
