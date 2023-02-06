package ru.yandex.direct.core.copyentity;

import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.DefectInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaignWithStrategy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("unchecked")
public class CopyOperationSmartCampaignBetweenClientsTest {
    @Autowired
    private Steps steps;

    @Autowired
    private CopyOperationFactory factory;

    private CopyOperation xerox;

    @Before
    public void setUp() {
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        Long uid = superClientInfo.getUid();

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();

        ClientInfo clientInfoTo = steps.clientSteps().createDefaultClient();

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activePerformanceCampaignWithStrategy(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru")
                        .withStartTime(LocalDate.now().plusDays(1L)),
                clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        xerox = factory.build(clientInfo.getShard(), clientInfo.getClient(),
                clientInfoTo.getShard(), clientInfoTo.getClient(),
                uid,
                BaseCampaign.class,
                List.of(campaignId),
                new CopyCampaignFlags.Builder().withCopyNotificationSettings(true).build());
    }


    @Test
    public void smartCampaignNotCopied() {
        var copyResult = xerox.copy();
        List<DefectInfo> errors = copyResult.getMassResult().getValidationResult().flattenErrors();
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0).getDefect()).isEqualTo(CampaignDefects.campaignTypeNotSupported());
    }
}
