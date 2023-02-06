package ru.yandex.direct.core.entity.bidmodifiers.add.retargeting;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientRetargetingModifier;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
@Description("Негативные сценарии добавления корректировок ставок ретаргетинга с несуществующими ID")
public class AddBidModifiersRetargetingConditionsInvalidIdTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private RetargetingConditionRepository retConditionRepository;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public Supplier<Long> retCondIdSupplier;

    @Parameterized.Parameter(2)
    public Path defectPath;

    @Parameterized.Parameter(3)
    public Function<Long, Defect> defectSupplier;

    private CampaignInfo campaign;
    private static Long anotherClientRetCondId;
    private static Long deletedRetCondId;

    private static Path adjustmentsPath = path(index(0), field("retargetingAdjustments"), index(0));

    @Parameterized.Parameters(name = "test = {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Чужое условие ретаргетинга",
                        (Supplier<Long>) () -> anotherClientRetCondId,
                        adjustmentsPath,
                        (Function<Long, Defect>)
                                RetargetingDefects::retargetingConditionNotFoundDetailed
                },
                {
                        "Несуществующее условие ретаргетинга",
                        (Supplier<Long>) () -> 1234567L,
                        adjustmentsPath,
                        (Function<Long, Defect>)
                                RetargetingDefects::retargetingConditionNotFoundDetailed
                },
                {
                        "Удаленное условие ретаргетинга",
                        (Supplier<Long>) () -> deletedRetCondId,
                        adjustmentsPath,
                        (Function<Long, Defect>)
                                RetargetingDefects::retargetingConditionNotFoundDetailed
                },
                {
                        "Отрицательный ID условия ретаргетинга",
                        (Supplier<Long>) () -> -1L,
                        path(index(0), field("retargetingAdjustments"), index(0), field("retargetingConditionId")),
                        (Function<Long, Defect>) id -> CommonDefects.validId()
                },
                {
                        "Нулевой ID условия ретаргетинга",
                        (Supplier<Long>) () -> 0L,
                        path(index(0), field("retargetingAdjustments"), index(0), field("retargetingConditionId")),
                        (Function<Long, Defect>) id -> CommonDefects.validId()
                }
        });
    }

    private TestContextManager testContextManager;

    @Before
    public void before() throws Exception {
        // Manual Spring integration (because we're using Parametrized runner)
        this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);

        campaign = campaignSteps.createActiveTextCampaign();

        anotherClientRetCondId = retConditionSteps.createDefaultRetCondition().getRetConditionId();

        RetConditionInfo deletedRetCondition = retConditionSteps.createDefaultRetCondition(campaign.getClientInfo());
        retConditionRepository.delete(
                campaign.getShard(), campaign.getClientId(), singletonList(deletedRetCondition.getRetConditionId()));
        deletedRetCondId = deletedRetCondition.getRetConditionId();
    }

    @Test
    public void testInvalidRetCondId() {
        Long retCondId = retCondIdSupplier.get();
        MassResult<List<Long>> result = bidModifierService.add(singletonList(
                createEmptyClientRetargetingModifier()
                        .withCampaignId(campaign.getCampaignId())
                        .withEnabled(true)
                        .withRetargetingAdjustments(singletonList(
                                new BidModifierRetargetingAdjustment()
                                        .withRetargetingConditionId(retCondId)
                                        .withPercent(130)))),
                campaign.getClientId(), campaign.getUid());

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(defectPath, defectSupplier.apply(retCondId))));
    }
}
