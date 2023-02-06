package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomCheckPositionEvent;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class CampaignWithCheckPositionEventUpdateOperationSupportEnrichTest {

    private CampaignWithCustomCheckPositionEventUpdateOperationSupport updateOperationSupport;

    private ModelChanges<CampaignWithCustomCheckPositionEvent> modelChanges;
    private CampaignWithCustomCheckPositionEvent campaignFromDb;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void initTestData() {
        updateOperationSupport = new CampaignWithCustomCheckPositionEventUpdateOperationSupport();
        long campaignId = RandomNumberUtils.nextPositiveLong();
        modelChanges = new ModelChanges<>(campaignId, CampaignWithCustomCheckPositionEvent.class);

        campaignFromDb = ((CampaignWithCustomCheckPositionEvent) TestCampaigns.newCampaignByCampaignType(campaignType))
                .withId(campaignId)
                .withEnableCheckPositionEvent(true)
                .withCheckPositionIntervalEvent(CampaignConstants.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL);
    }


    @Test
    public void enrichWithoutChanges() {
        var appliedChanges = modelChanges.applyTo(campaignFromDb);

        updateOperationSupport.onChangesApplied(null, List.of(appliedChanges));
        assertThat(appliedChanges.getActuallyChangedProps())
                .isEmpty();
    }

    @Test
    public void enrich_whenCheckPositionIntervalEventValueChanged() {
        modelChanges.process(true, CampaignWithCustomCheckPositionEvent.ENABLE_CHECK_POSITION_EVENT);
        modelChanges.process(CampaignWarnPlaceInterval._15,
                CampaignWithCustomCheckPositionEvent.CHECK_POSITION_INTERVAL_EVENT);

        var appliedChanges = modelChanges.applyTo(campaignFromDb);

        updateOperationSupport.onChangesApplied(null, List.of(appliedChanges));
        assertThat(appliedChanges.getActuallyChangedProps())
                .containsExactly(CampaignWithCustomCheckPositionEvent.CHECK_POSITION_INTERVAL_EVENT);
    }

    @Test
    public void enrich_whenCheckPositionIntervalEventValueIsNull() {
        modelChanges.process(false, CampaignWithCustomCheckPositionEvent.ENABLE_CHECK_POSITION_EVENT);
        modelChanges.process(null, CampaignWithCustomCheckPositionEvent.CHECK_POSITION_INTERVAL_EVENT);

        var appliedChanges = modelChanges.applyTo(campaignFromDb);

        updateOperationSupport.onChangesApplied(null, List.of(appliedChanges));
        assertThat(appliedChanges.getActuallyChangedProps())
                .containsExactly(CampaignWithCustomCheckPositionEvent.ENABLE_CHECK_POSITION_EVENT);
    }

}
