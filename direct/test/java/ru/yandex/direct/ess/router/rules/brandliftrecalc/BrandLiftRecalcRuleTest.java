package ru.yandex.direct.ess.router.rules.brandliftrecalc;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.dbschema.ppc.enums.CampaignsType;
import ru.yandex.direct.ess.logicobjects.brandliftrecalc.BrandLiftRecalcObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.CampaignsTableChange;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.ess.router.testutils.CampaignsTableChange.createCampaignEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class BrandLiftRecalcRuleTest {

    @Autowired
    private BrandLiftRecalcRule rule;

    @Test
    public void mapBinlogEventTest_updateCpmBannerCampaign_CidInChangeObject() {
        var campaignsTableChange = new CampaignsTableChange().withCid(1L).withType(CampaignsType.cpm_banner);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.FINISH_TIME, null, "2020-12-03");
        var binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);

        List<BrandLiftRecalcObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(new BrandLiftRecalcObject(1L));
    }

    @Test
    public void mapBinlogEventTest_updateCpmYndxFrontpageCampaign_CidInChangeObject() {
        var campaignsTableChange = new CampaignsTableChange().withCid(2L).withType(CampaignsType.cpm_yndx_frontpage);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.FINISH_TIME, null, "2020-12-03");
        var binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);

        List<BrandLiftRecalcObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(new BrandLiftRecalcObject(2L));
    }

    @Test
    public void mapBinlogEventTest_updateCpmTextCampaign_CidNotInChangeObject() {
        var campaignsTableChange = new CampaignsTableChange().withCid(4L).withType(CampaignsType.text);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.FINISH_TIME, null, "2020-12-03");
        var binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);

        List<BrandLiftRecalcObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).isEmpty();
    }
}
