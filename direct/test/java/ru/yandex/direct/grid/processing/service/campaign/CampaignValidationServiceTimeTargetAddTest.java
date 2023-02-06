package ru.yandex.direct.grid.processing.service.campaign;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddDynamicCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddMcBannerCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddSmartCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddTextCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;

import static ru.yandex.direct.result.MassResult.emptyMassAction;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(Parameterized.class)
public class CampaignValidationServiceTimeTargetAddTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private CampaignValidationService campaignValidationService;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Test
    public void testEmptyTimeBoard() {
        final var clientId = ClientId.fromLong(1L);
        GdTimeTarget timeTarget = new GdTimeTarget()
                .withTimeBoard(null)
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(false);

        campaignValidationService.validateAddCampaigns(clientId, pack(timeTarget));
        GdValidationResult result = campaignValidationService.getValidationResult(emptyMassAction(),
                path(field(GdUpdateCampaigns.CAMPAIGN_UPDATE_ITEMS)));

        Assert.assertTrue(result == null || result.getErrors().isEmpty());
    }

    @Test
    public void testNoTimeTarget() {
        final var clientId = ClientId.fromLong(1L);
        campaignValidationService.validateAddCampaigns(clientId, pack(null));
        GdValidationResult result = campaignValidationService.getValidationResult(emptyMassAction(),
                path(field(GdUpdateCampaigns.CAMPAIGN_UPDATE_ITEMS)));

        Assert.assertTrue(result == null || result.getErrors().isEmpty());
    }

    private GdAddCampaigns pack(@Nullable GdTimeTarget timeTarget) {
        GdAddCampaignUnion union = new GdAddCampaignUnion();
        if (campaignType == CampaignType.DYNAMIC) {
            GdAddDynamicCampaign campaignUpdate = new GdAddDynamicCampaign().withTimeTarget(timeTarget);
            union.withDynamicCampaign(campaignUpdate);
        } else if (campaignType == CampaignType.PERFORMANCE) {
            GdAddSmartCampaign campaignUpdate = new GdAddSmartCampaign().withTimeTarget(timeTarget);
            union.withSmartCampaign(campaignUpdate);
        } else if (campaignType == CampaignType.MCBANNER) {
            GdAddMcBannerCampaign campaignUpdate = new GdAddMcBannerCampaign().withTimeTarget(timeTarget);
            union.withMcBannerCampaign(campaignUpdate);
        } else {
            GdAddTextCampaign campaignUpdate = new GdAddTextCampaign().withTimeTarget(timeTarget);
            union.withTextCampaign(campaignUpdate);
        }
        return new GdAddCampaigns().withCampaignAddItems(List.of(union));
    }
}
