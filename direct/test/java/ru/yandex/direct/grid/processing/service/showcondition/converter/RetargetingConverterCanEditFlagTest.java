package ru.yandex.direct.grid.processing.service.showcondition.converter;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.core.entity.showcondition.model.GdiBidsRetargeting;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdPriceCampaign;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyManual;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdCpmPriceAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargeting;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingAccess;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.service.showcondition.converter.RetargetingConverter.toGdRetargeting;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdAdGroupStatus;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdBaseGroup;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaignStatus;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaignStrategyManual;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.gdPriceCampaign;
import static ru.yandex.direct.grid.processing.util.RetargetingTestDataUtils.defaultGdiRetargeting;

@RunWith(Parameterized.class)
public class RetargetingConverterCanEditFlagTest {

    @Parameterized.Parameter(0)
    public GdCampaign gdCampaign;
    @Parameterized.Parameter(1)
    public GdAdGroup gdAdGroup;
    @Parameterized.Parameter(2)
    public GdiBidsRetargeting gdiBidsRetargeting;
    @Parameterized.Parameter(3)
    public boolean canEdit;
    @Parameterized.Parameter(4)
    public String description;

    @Parameterized.Parameters(name = "{4}")
    public static Collection<Object[]> parameters() {
        GdCampaignStrategyManual strategy = defaultGdCampaignStrategyManual();
        GdCampaign activeGdCampaign = defaultGdCampaign().withStrategy(strategy).withFlatStrategy(strategy)
                .withStatus(defaultGdCampaignStatus().withReadOnly(false));
        GdCampaign readOnlyGdCampaign = defaultGdCampaign().withStrategy(strategy).withFlatStrategy(strategy)
                .withStatus(defaultGdCampaignStatus().withReadOnly(true));
        GdTextAdGroup activeGdTextAdGroup = defaultGdBaseGroup().withStatus(defaultGdAdGroupStatus().withArchived(false));
        GdTextAdGroup archivedGdTextAdGroup = defaultGdBaseGroup().withStatus(defaultGdAdGroupStatus().withArchived(true));

        GdPriceCampaign activeGdPriceCampaign = gdPriceCampaign()
                .withStatus(defaultGdCampaignStatus().withReadOnly(false));
        GdCpmPriceAdGroup activeCpmPriceAdGroup = new GdCpmPriceAdGroup()
                .withStatus(defaultGdAdGroupStatus().withArchived(false));

        return Arrays.asList(new Object[][]{
                {activeGdCampaign, activeGdTextAdGroup, defaultGdiRetargeting(), true,
                        "Активная кампания, активная группа"},
                {readOnlyGdCampaign, activeGdTextAdGroup, defaultGdiRetargeting(), false,
                        "Read-only кампания, активная группа"},
                {activeGdCampaign, archivedGdTextAdGroup, defaultGdiRetargeting(), false,
                        "Активная кампания, архивная группа"},
                {readOnlyGdCampaign, archivedGdTextAdGroup, defaultGdiRetargeting(), false,
                        "Read-only кампания, архивная группа"},
                {activeGdPriceCampaign, activeCpmPriceAdGroup, defaultGdiRetargeting(), false,
                        "Активная прайсовая кампания, активная группа"}
        });
    }

    @Test
    public void testCanEdit() {
        GdRetargeting actual = toGdRetargeting(gdiBidsRetargeting, gdCampaign, gdAdGroup, false);

        GdRetargeting expected = new GdRetargeting()
                .withAccess(new GdRetargetingAccess()
                        .withCanEdit(canEdit));

        assertThat(actual, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }
}
