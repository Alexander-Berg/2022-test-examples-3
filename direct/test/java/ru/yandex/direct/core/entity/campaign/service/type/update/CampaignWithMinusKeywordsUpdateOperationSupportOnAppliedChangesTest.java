package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.adgroup.service.MinusKeywordPreparingTool;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMinusKeywords;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignType;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class CampaignWithMinusKeywordsUpdateOperationSupportOnAppliedChangesTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MinusKeywordPreparingTool minusKeywordPreparingTool;

    @InjectMocks
    private CampaignWithMinusKeywordsUpdateOperationSupport updateOperationSupport;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MCBANNER},
        });
    }

    @Test
    public void testFullPrepareForSavingOnChangesApplied_NoMinusKeywordsChanges() {
        updateOperationSupport.onChangesApplied(null, List.of(getChanges(false), getChanges(false)));
        verify(minusKeywordPreparingTool, never()).fullPrepareForSaving(anyList());
    }

    @Test
    public void testFullPrepareForSavingOnChangesApplied_PartialMinusKeywordsChanges() {
        updateOperationSupport.onChangesApplied(null, List.of(getChanges(false), getChanges(true)));
        verify(minusKeywordPreparingTool).fullPrepareForSaving(anyList());
    }

    @Test
    public void testFullPrepareForSavingOnChangesApplied_FullMinusKeywordsChanges() {
        updateOperationSupport.onChangesApplied(null, List.of(getChanges(true), getChanges(true)));
        verify(minusKeywordPreparingTool, times(2)).fullPrepareForSaving(anyList());
    }

    private AppliedChanges<CampaignWithMinusKeywords> getChanges(boolean withMinusKeywords) {
        Long cid = nextLong();
        CampaignWithMinusKeywords campaign = ((CampaignWithMinusKeywords) newCampaignByCampaignType(campaignType))
                .withId(cid)
                .withMinusKeywords(List.of(RandomStringUtils.random(16)));

        ModelChanges<CampaignWithMinusKeywords> campaignModelChanges = new ModelChanges<>(cid,
                CampaignWithMinusKeywords.class);

        campaignModelChanges.process(LocalDateTime.now(), CampaignWithMinusKeywords.LAST_CHANGE);
        if (withMinusKeywords) {
            campaignModelChanges.process(List.of(RandomStringUtils.random(16)),
                    CampaignWithMinusKeywords.MINUS_KEYWORDS);
        }

        return campaignModelChanges.applyTo(campaign);
    }

}
