package ru.yandex.direct.grid.processing.service.campaign;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.model.campaign.GdCampaignTruncated;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.GdSmartCampaign;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignInfoServiceGridProcessingTest {
    @Autowired
    private Steps steps;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CampaignInfoService campaignInfoService;

    @Test
    public void getTruncatedCampaigns_success_withPerformanceCampaign() {
        //Создаём исходные данные
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        Campaign campaign = campaignInfo.getCampaign();
        Long uid = clientInfo.getUid();
        User user = userRepository.fetchByUids(clientInfo.getShard(), singletonList(uid)).get(0);

        //Ожидаемые результаты
        GdCampaignTruncated expectedCampaign = new GdSmartCampaign()
                .withId(campaignId)
                .withName(campaign.getName())
                .withType(GdCampaignType.PERFORMANCE);

        //Выполняем запрос
        GridGraphQLContext context = buildContext(user);
        gridContextProvider.setGridContext(context);
        GdClientInfo client = context.getQueriedClient();
        Map<Long, GdCampaignTruncated> truncatedCampaigns =
                campaignInfoService.getTruncatedCampaigns(ClientId.fromLong(client.getId()), singletonList(campaignId));
        GdCampaignTruncated gdCampaignTruncated = truncatedCampaigns.get(campaignId);

        //Сверяем ожидания и реальность
        assertThat(gdCampaignTruncated,
                beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields())
        );
    }

    @Test
    public void getFirstNotArchivedTextCampaignIdInvalidUidTest() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var result = campaignInfoService.getFirstNotArchivedTextCampaignId(clientInfo.getShard(),
                clientInfo.getClientId(), clientInfo.getUid() + 42);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void getFirstNotArchivedTextCampaignIdSuccessTest() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfoArchived = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        steps.campaignSteps().archiveCampaign(campaignInfoArchived);
        steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        steps.campaignSteps().createActivePerformanceCampaign(clientInfo);
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var result = campaignInfoService.getFirstNotArchivedTextCampaignId(clientInfo.getShard(),
                clientInfo.getClientId(), clientInfo.getUid());
        assertThat(result.get(), is(campaignInfo.getCampaignId()));
    }

    @Test
    public void getFirstNotArchivedTextCampaignNoTextCampaignTest() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        steps.campaignSteps().createDraftPerformanceCampaign(clientInfo);
        var result = campaignInfoService.getFirstNotArchivedTextCampaignId(clientInfo.getShard(),
                clientInfo.getClientId(), clientInfo.getUid());
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void getFirstNotArchivedTextCampaignAllArchivedTest() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfoArchivedFirst = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        steps.campaignSteps().archiveCampaign(campaignInfoArchivedFirst);
        CampaignInfo campaignInfoArchivedSecond = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        steps.campaignSteps().archiveCampaign(campaignInfoArchivedSecond);
        var result = campaignInfoService.getFirstNotArchivedTextCampaignId(clientInfo.getShard(),
                clientInfo.getClientId(), clientInfo.getUid());
        assertThat(result.isEmpty(), is(true));

    }
}
