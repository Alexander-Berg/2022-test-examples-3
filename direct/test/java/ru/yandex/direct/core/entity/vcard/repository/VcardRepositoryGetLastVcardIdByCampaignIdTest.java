package ru.yandex.direct.core.entity.vcard.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VcardRepositoryGetLastVcardIdByCampaignIdTest {

    @Autowired
    private VcardRepository vcardRepository;

    @Autowired
    private Steps steps;

    private CampaignInfo activeCampaign;
    private ClientInfo defaultClient;

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createDefaultClient();
        activeCampaign = steps.campaignSteps().createActiveCampaign(defaultClient);
    }

    @Test
    public void getLastVcardIdByCampaignId() {
        steps.vcardSteps().createVcard(fullVcard(activeCampaign.getClientId().asLong(), activeCampaign.getCampaignId()),
                activeCampaign);
        Vcard lastVcard = fullVcard(activeCampaign.getClientId().asLong(), activeCampaign.getCampaignId())
                .withCity("newCity")
                .withLastChange(LocalDateTime.now().plusDays(1));
        VcardInfo lastVcardInfo = steps.vcardSteps().createVcard(lastVcard, activeCampaign);
        Map<Long, Long> lastVcardIdByCampaignId = vcardRepository.getLastVcardIdByCampaignId(activeCampaign.getShard(),
                Collections.singletonList(activeCampaign.getCampaignId()));

        assertThat(lastVcardIdByCampaignId).containsKey(activeCampaign.getCampaignId());
        assertThat(lastVcardIdByCampaignId.get(activeCampaign.getCampaignId())).isEqualTo(lastVcardInfo.getVcardId());
    }

    @Test
    public void getLastVcardIdByCampaignIdForCampaignWithoutVcard() {
        Map<Long, Long> lastVcardIdByCampaignId = vcardRepository.getLastVcardIdByCampaignId(activeCampaign.getShard(),
                Collections.singletonList(activeCampaign.getCampaignId()));

        assertThat(lastVcardIdByCampaignId).isEmpty();
    }
}
