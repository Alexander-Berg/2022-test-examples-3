package ru.yandex.direct.core.entity.campaign.service;

import java.util.Arrays;
import java.util.Collection;

import jdk.jfr.Description;
import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithPricePackage;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.pricepackage.model.ShowsFrequencyLimit;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.DefectIds;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
@Description("Проверка обновления частоты показов на прайсовой кампании")
public class RestrictedCampaignsUpdateOperationCpmPriceImpressionRateTest
        extends RestrictedCampaignsUpdateOperationCpmPriceTestBase {
    private static final ShowsFrequencyLimit DEFAULT_SHOWS_FREQUENCY_LIMIT = new ShowsFrequencyLimit()
            .withFrequencyLimit(3)
            .withFrequencyLimitDays(7)
            .withFrequencyLimitIsForCampaignTime(false);
    private static final ShowsFrequencyLimit MIN_SHOWS_FREQUENCY_LIMIT = new ShowsFrequencyLimit()
            .withFrequencyLimit(3)
            .withFrequencyLimitDays(7)
            .withFrequencyLimitIsForCampaignTime(false)
            .withMinLimit(true);

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter
    public String testDescription;
    @Parameterized.Parameter(1)
    public ShowsFrequencyLimit packageShowsFrequencyLimit;
    @Parameterized.Parameter(2)
    public Integer oldImpressionRate;
    @Parameterized.Parameter(3)
    public Integer newImpressionRate;
    @Parameterized.Parameter(4)
    public Matcher matcher;
    @Parameterized.Parameter(5)
    public Integer expectedImpressionRate;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {
                        "Пакет не запрещает задавать частоту на РК. Частота обновляется",
                        new ShowsFrequencyLimit(),
                        12,
                        4,
                        hasNoDefectsDefinitions(),
                        4
                },
                {
                        "Пакет не запрещает задавать частоту на РК. Частота сбрасывается",
                        new ShowsFrequencyLimit(),
                        12,
                        null,
                        hasNoDefectsDefinitions(),
                        null
                },
                {
                        "Пакет явно задаёт частоту. На пустые изменения не обновляем",
                        DEFAULT_SHOWS_FREQUENCY_LIMIT,
                        DEFAULT_SHOWS_FREQUENCY_LIMIT.getFrequencyLimit(),
                        null,
                        hasNoDefectsDefinitions(),
                        DEFAULT_SHOWS_FREQUENCY_LIMIT.getFrequencyLimit()
                },
                {
                        "Пакет явно задаёт частоту. На случайные изменения ругаемся",
                        DEFAULT_SHOWS_FREQUENCY_LIMIT,
                        DEFAULT_SHOWS_FREQUENCY_LIMIT.getFrequencyLimit(),
                        42,
                        hasDefectWithDefinition(validationError(path(index(0),
                                field(CpmPriceCampaign.IMPRESSION_RATE_COUNT)),
                                DefectIds.INVALID_VALUE)),
                        DEFAULT_SHOWS_FREQUENCY_LIMIT.getFrequencyLimit()
                },
                {
                        "Пакет явно задаёт частоту. На изменения без изменений успешно",
                        DEFAULT_SHOWS_FREQUENCY_LIMIT,
                        DEFAULT_SHOWS_FREQUENCY_LIMIT.getFrequencyLimit(),
                        DEFAULT_SHOWS_FREQUENCY_LIMIT.getFrequencyLimit(),
                        hasNoDefectsDefinitions(),
                        DEFAULT_SHOWS_FREQUENCY_LIMIT.getFrequencyLimit()
                },
                {
                        "Пакет запрещает задавать частоту. На пустые изменения ничего не происходит",
                        null,
                        null,
                        null,
                        hasNoDefectsDefinitions(),
                        null
                },
                {
                        "Пакет запрещает задавать частоту. На явные изменения ругаемся",
                        null,
                        null,
                        4,
                        hasDefectWithDefinition(validationError(path(index(0),
                                field(CpmPriceCampaign.IMPRESSION_RATE_COUNT)),
                                DefectIds.MUST_BE_NULL)),
                        null
                },
                {
                        "Режим минимальной частоты. Можно как в пакете",
                        MIN_SHOWS_FREQUENCY_LIMIT,
                        4,
                        DEFAULT_SHOWS_FREQUENCY_LIMIT.getFrequencyLimit(),
                        hasNoDefectsDefinitions(),
                        DEFAULT_SHOWS_FREQUENCY_LIMIT.getFrequencyLimit()
                },
                {
                        "Режим минимальной частоты. Можно числа болше чем в пакете",
                        MIN_SHOWS_FREQUENCY_LIMIT,
                        3,
                        4,
                        hasNoDefectsDefinitions(),
                        4
                },
                {
                        "Режим минимальной частоты. Не может быть пусто",
                        MIN_SHOWS_FREQUENCY_LIMIT,
                        3,
                        null,
                        hasDefectWithDefinition(validationError(path(index(0),
                                field(CpmPriceCampaign.IMPRESSION_RATE_COUNT)),
                                DefectIds.CANNOT_BE_NULL)),
                        3
                },
                {
                        "Режим минимальной частоты. Нельзя задать частоту меньше чем в пакете",
                        MIN_SHOWS_FREQUENCY_LIMIT,
                        3,
                        2,
                        hasDefectWithDefinition(validationError(path(index(0),
                                field(CpmPriceCampaign.IMPRESSION_RATE_COUNT)),
                                DefectIds.INVALID_VALUE)),
                        3
                },
        });
    }

    @Test
    public void test() {
        setupOperator(RbacRole.CLIENT);
        var pricePackageId = createPricePackageFR(packageShowsFrequencyLimit);
        var campaign = defaultCampaign()
                .withPricePackageId(pricePackageId)
                .withImpressionRateCount(oldImpressionRate);
        if (packageShowsFrequencyLimit != null) {
            campaign.setImpressionRateIntervalDays(packageShowsFrequencyLimit.getFrequencyLimitDays());
        }
        createPriceCampaign(campaign);
        ModelChanges<CampaignWithPricePackage> modelChanges =
                ModelChanges.build(campaign, CampaignWithPricePackage.IMPRESSION_RATE_COUNT, newImpressionRate);
        if (packageShowsFrequencyLimit != null) {
            modelChanges.process(packageShowsFrequencyLimit.getFrequencyLimitDays(),
                    CampaignWithPricePackage.IMPRESSION_RATE_INTERVAL_DAYS);
        }

        MassResult<Long> result = apply(modelChanges);

        assertThat(result.getValidationResult()).is(matchedBy(matcher));
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getImpressionRateCount()).isEqualTo(expectedImpressionRate);
    }

    private Long createPricePackageFR(ShowsFrequencyLimit showsFrequencyLimit) {
        var pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().setShowsFrequencyLimit(showsFrequencyLimit);
        return steps.pricePackageSteps()
                .createPricePackage(pricePackage)
                .getPricePackageId();
    }
}
