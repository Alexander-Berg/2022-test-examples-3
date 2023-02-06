package ru.yandex.direct.oneshot.oneshots.bsexport;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportSpecials;
import ru.yandex.direct.core.entity.bs.export.queue.model.QueueType;
import ru.yandex.direct.core.entity.bs.export.queue.repository.BsExportSpecialsRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.oneshot.configuration.OneshotTest;

import static org.assertj.core.api.Assertions.assertThat;

@OneshotTest
@RunWith(SpringRunner.class)
public class DeleteGarbageFromBsExportSpecialsTest {

    @Autowired
    private DeleteGarbageFromBsExportSpecials oneshot;

    @Autowired
    private BsExportSpecialsRepository specialsRepository;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    private Steps steps;

    private CampaignInfo campaignInfo;

    @Before
    public void createTestData() {
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        BsExportSpecials specials = new BsExportSpecials()
                .withCampaignId(campaignInfo.getCampaignId())
                .withType(QueueType.BUGGY);
        specialsRepository.add(campaignInfo.getShard(), List.of(specials));
    }

    @Test
    public void goodCampaignIsNotDeleted() {
        oneshot.execute();

        var specials = specialsRepository.getByCampaignIds(campaignInfo.getShard(),
                List.of(campaignInfo.getCampaignId()));
        assertThat(specials).isNotEmpty();
    }

    @Test
    public void nonExistenceCampaignDeleted() {
        testCampaignRepository.deleteCampaign(campaignInfo.getShard(), campaignInfo.getCampaignId());

        oneshot.execute();

        var specials = specialsRepository.getByCampaignIds(campaignInfo.getShard(),
                List.of(campaignInfo.getCampaignId()));
        assertThat(specials).isEmpty();

    }


}
