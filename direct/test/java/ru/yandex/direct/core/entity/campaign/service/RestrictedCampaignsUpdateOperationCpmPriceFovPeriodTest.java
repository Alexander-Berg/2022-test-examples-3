package ru.yandex.direct.core.entity.campaign.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.Supplier;

import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPricePackage;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperationCpmPriceFovPeriodTest.Result.Success;
import static ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperationCpmPriceFovPeriodTest.Result.ValidationError;
import static ru.yandex.direct.rbac.RbacRole.CLIENT;
import static ru.yandex.direct.rbac.RbacRole.MANAGER;
import static ru.yandex.direct.rbac.RbacRole.SUPER;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тесты на изменение полей прайсовой кампании, которые зависят от текущего statusApprove и роли и периода действия
 * кампании, а именно:
 * - flightOrderVolume
 * - startDate
 * - endDate
 */
@CoreTest
@RunWith(Parameterized.class)
public class RestrictedCampaignsUpdateOperationCpmPriceFovPeriodTest extends
        RestrictedCampaignsUpdateOperationCpmPriceTestBase {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameter
    public RbacRole operatorRole;

    @Parameter(1)
    public Supplier<LocalDate> startDateSupplier;

    @Parameter(2)
    public Supplier<LocalDate> endDateSupplier;

    @Parameter(3)
    public PriceFlightStatusApprove statusApprove;

    @Parameter(4)
    public Result resultFlightOrderVolume;

    @Parameter(5)
    public Result resultStartDate;

    @Parameter(6)
    public Result resultEndDate;

    enum Result {Success, ValidationError}

    private CampaignWithPricePackage campaign;

    @Parameters()
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // Кампания не началась и statusApprove NEW - клиент может редактировать объем заказа и период
                {CLIENT, TOMORROW, PLUS_MONTHS, PriceFlightStatusApprove.NEW, Success, Success, Success},
                // Кампания не началась и statusApprove NEW - супер может редактировать объем заказа и период
                {SUPER, TOMORROW, PLUS_MONTHS, PriceFlightStatusApprove.NEW, Success, Success, Success},
                // Кампания не началась и statusApprove YES - клиент не может редактировать объем заказа и период
                {CLIENT, TOMORROW, PLUS_MONTHS, PriceFlightStatusApprove.YES, ValidationError, ValidationError,
                        ValidationError},
                // Кампания не началась и statusApprove YES - супер может редактировать объем заказа и период
                {SUPER, TOMORROW, PLUS_MONTHS, PriceFlightStatusApprove.YES, Success, Success, Success},
                // Кампания не началась и statusApprove NO - клиент может редактировать объем заказа и период
                {CLIENT, TOMORROW, PLUS_MONTHS, PriceFlightStatusApprove.NO, Success, Success, Success},
                // Кампания не началась и statusApprove NO - супер может редактировать объем заказа и период
                {SUPER, TOMORROW, PLUS_MONTHS, PriceFlightStatusApprove.NO, Success, Success, Success},

                // Кампания в процессе и statusApprove NEW - клиент может редактировать объем заказа и период
                {CLIENT, MINUS_MONTHS, TODAY, PriceFlightStatusApprove.NEW, Success, Success, Success},
                // Кампания в процессе и statusApprove NEW - супер может редактировать объем заказа и период
                {SUPER, MINUS_MONTHS, TODAY, PriceFlightStatusApprove.NEW, Success, Success, Success},
                // Кампания в процессе и statusApprove YES - клиент не может редактировать объем заказа и период
                {CLIENT, MINUS_MONTHS, TODAY, PriceFlightStatusApprove.YES, ValidationError, ValidationError,
                        ValidationError},
                // Кампания в процессе и statusApprove YES - супер может редактировать объем заказа и дату окончания,
                // а дату начала не может
                {SUPER, MINUS_MONTHS, TODAY, PriceFlightStatusApprove.YES, Success, ValidationError, Success},
                // Кампания в процессе и statusApprove NO - клиент может редактировать объем заказа и период
                {CLIENT, MINUS_MONTHS, TODAY, PriceFlightStatusApprove.NO, Success, Success, Success},
                // Кампания в процессе и statusApprove NO - супер может редактировать объем заказа и период
                {SUPER, MINUS_MONTHS, TODAY, PriceFlightStatusApprove.NO, Success, Success, Success},

                // Кампания закончилась и statusApprove NEW - клиент может редактировать объем заказа и период
                {CLIENT, MINUS_MONTHS, YESTERDAY, PriceFlightStatusApprove.NEW, Success, Success, Success},
                // Кампания закончилась и statusApprove NEW - супер может редактировать объем заказа и период
                {SUPER, MINUS_MONTHS, YESTERDAY, PriceFlightStatusApprove.NEW, Success, Success, Success},
                // Кампания закончилась и statusApprove YES - клиент не может редактировать объем заказа и период
                {CLIENT, MINUS_MONTHS, YESTERDAY, PriceFlightStatusApprove.YES, ValidationError, ValidationError,
                        ValidationError},
                // Кампания закончилась и statusApprove YES - супер не может редактировать объем заказа и дату начала,
                // а дату окончания может
                {SUPER, MINUS_MONTHS, YESTERDAY, PriceFlightStatusApprove.YES, ValidationError, ValidationError,
                        Success},
                // Кампания закончилась и statusApprove NO - клиент может редактировать объем заказа и период
                {CLIENT, MINUS_MONTHS, YESTERDAY, PriceFlightStatusApprove.NO, Success, Success, Success},
                // Кампания закончилась и statusApprove NO - супер может редактировать объем заказа и период
                {SUPER, MINUS_MONTHS, YESTERDAY, PriceFlightStatusApprove.NO, Success, Success, Success},

                // Кампания в процессе и statusApprove YES - менеджер может редактировать объем заказа и период
                {MANAGER, YESTERDAY, PLUS_MONTHS, PriceFlightStatusApprove.YES, Success, Success,
                        Success},
                // Кампания не началась и statusApprove YES - менеджер может редактировать объем заказа и период
                {MANAGER, TOMORROW, PLUS_MONTHS, PriceFlightStatusApprove.YES, Success, Success, Success},
                // Кампания не началась и statusApprove NEW - менеджер может редактировать объем заказа и период
                {MANAGER, TOMORROW, PLUS_MONTHS, PriceFlightStatusApprove.NEW, Success, Success, Success},
        });
    }

    @Test
    public void changeFlightOrderVolume() {
        setupOperator(operatorRole);
        switch (resultFlightOrderVolume) {
            case Success:
                changeFlightOrderVolume_Success();
                break;
            case ValidationError:
                changeFlightOrderVolume_Error();
                break;
        }
    }

    @Test
    public void changePeriod() {
        setupOperator(operatorRole);
        if (resultStartDate == Success && resultEndDate == Success) {
            changePeriod_Success();
        } else {
            changePeriod_Error();
        }
    }

    private void changeFlightOrderVolume_Success() {
        createTestCampaign();

        long newFlightOrderVolume = 2000;
        // expectedBudget вычисляется по формуле: v * p / 1000, где
        // v: объём заказа
        // p: cpm цена
        // newFlightOrderVolume * pricePackagePrice / 1000
        BigDecimal expectedBudget = BigDecimal.valueOf(40L);
        MassResult<Long> result = applyNewFlightOrderVolume(newFlightOrderVolume, expectedBudget);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        CampaignWithPricePackage expected = updateOriginCampaignToExpected(campaign,
                expectedBudget,
                campaign.getStartDate(),
                campaign.getEndDate())
                .withFlightOrderVolume(newFlightOrderVolume);
        if (statusApprove == PriceFlightStatusApprove.NO) {
            expected.withFlightStatusApprove(PriceFlightStatusApprove.NEW);
        }
        if (statusApprove == PriceFlightStatusApprove.YES) {
            expected.withFlightStatusApprove(PriceFlightStatusApprove.NEW);
        }
        if (operatorRole == MANAGER) {
            expected.setStatusPostModerate(CampaignStatusPostmoderate.ACCEPTED);
        }

        assertThat(actualCampaign)
                .is(matchedBy(beanDiffer(expected
                ).useCompareStrategy(cpmPriceCampaignCompareStrategy())));
    }

    private void changeFlightOrderVolume_Error() {
        createTestCampaign();

        long newFlightOrderVolume = 2000;
        // expectedBudget вычисляется по формуле: v * p / 1000, где
        // v: объём заказа
        // p: cpm цена
        // newFlightOrderVolume * pricePackagePrice / 1000
        BigDecimal expectedBudget = BigDecimal.valueOf(40L);
        MassResult<Long> result = applyNewFlightOrderVolume(newFlightOrderVolume, expectedBudget);

        assertThat(result.getValidationResult())
                .is(matchedBy(forbiddenToChangeMatcher(CpmPriceCampaign.FLIGHT_ORDER_VOLUME)));
    }

    private void changePeriod_Success() {
        createTestCampaign();

        LocalDate newStartDate = LocalDate.now().plusDays(43);
        LocalDate newEndDate = LocalDate.now().plusDays(86);
        MassResult<Long> result = applyNewPeriod(newStartDate, newEndDate);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        CampaignWithPricePackage expected = updateOriginCampaignToExpected(campaign,
                campaign.getStrategy().getStrategyData().getBudget(),
                newStartDate,
                newEndDate);
        if (statusApprove == PriceFlightStatusApprove.NO) {
            expected.withFlightStatusApprove(PriceFlightStatusApprove.NEW);
        }
        if (statusApprove == PriceFlightStatusApprove.YES) {
            expected.withFlightStatusApprove(PriceFlightStatusApprove.NEW);
        }
        if (operatorRole == MANAGER) {
            expected.setStatusPostModerate(CampaignStatusPostmoderate.ACCEPTED);
        }

        assertThat(actualCampaign)
                .is(matchedBy(beanDiffer(expected
                ).useCompareStrategy(cpmPriceCampaignCompareStrategy())));
    }

    private void changePeriod_Error() {
        createTestCampaign();

        LocalDate newStartDate = LocalDate.now().plusDays(43);
        LocalDate newEndDate = LocalDate.now().plusDays(86);
        MassResult<Long> result = applyNewPeriod(newStartDate, newEndDate);

        SoftAssertions.assertSoftly(softly -> {
            if (resultStartDate == ValidationError) {
                softly.assertThat(result.getValidationResult())
                        .is(matchedBy(forbiddenToChangeMatcher(CpmPriceCampaign.START_DATE)));
            } else {
                softly.assertThat(result.getValidationResult())
                        .isNot(matchedBy(forbiddenToChangeMatcher(CpmPriceCampaign.START_DATE)));
            }
            if (resultEndDate == ValidationError) {
                softly.assertThat(result.getValidationResult())
                        .is(matchedBy(forbiddenToChangeMatcher(CpmPriceCampaign.END_DATE)));
            } else {
                softly.assertThat(result.getValidationResult())
                        .isNot(matchedBy(forbiddenToChangeMatcher(CpmPriceCampaign.END_DATE)));
            }
        });
    }

    private void createTestCampaign() {
        campaign = defaultCampaign()
                .withFlightStatusApprove(statusApprove)
                .withStartDate(startDateSupplier.get())
                .withEndDate(endDateSupplier.get());
        if (operatorRole == MANAGER) {
            campaign.setManagerUid(operatorUid);
        }
        createPriceCampaign(campaign);
    }

    private MassResult<Long> applyNewFlightOrderVolume(long newFlightOrderVolume, BigDecimal expectedBudget) {
        assumeThat(defaultPricePackage.getPrice(), greaterThanOrEqualTo(BigDecimal.valueOf(1)));
        assumeThat(campaign.getStrategy().getStrategyData().getBudget(), not(equalTo((expectedBudget))));

        ModelChanges<CampaignWithPricePackage> modelChanges =
                ModelChanges.build(campaign, CampaignWithPricePackage.FLIGHT_ORDER_VOLUME, newFlightOrderVolume);

        return apply(modelChanges);
    }

    private MassResult<Long> applyNewPeriod(LocalDate newStartDate, LocalDate newEndDate) {
        assumeThat(campaign.getStartDate(), not(equalTo(newStartDate)));
        assumeThat(campaign.getEndDate(), not(equalTo(newEndDate)));

        ModelChanges<CpmPriceCampaign> modelChanges = new ModelChanges<>(campaign.getId(), CpmPriceCampaign.class);
        modelChanges.process(newStartDate, CpmPriceCampaign.START_DATE);
        modelChanges.process(newEndDate, CpmPriceCampaign.END_DATE);

        return apply(modelChanges);
    }

    private <T> Matcher<ValidationResult<T, Defect>> forbiddenToChangeMatcher(ModelProperty<?, T> property) {
        return hasDefectWithDefinition(validationError(
                path(index(0), field(property)), DefectIds.FORBIDDEN_TO_CHANGE));
    }

}
