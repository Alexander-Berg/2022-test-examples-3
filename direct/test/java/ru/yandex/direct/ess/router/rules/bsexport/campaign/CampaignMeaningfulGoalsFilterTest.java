package ru.yandex.direct.ess.router.rules.bsexport.campaign;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.BsExportCampaignObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.CampOptionsTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignResourceType.CAMPAIGN_MEANINGFUL_GOALS;
import static ru.yandex.direct.ess.router.testutils.CampOptionsTableChange.createCampOptionsEvent;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class CampaignMeaningfulGoalsFilterTest {

    @Autowired
    private BsExportCampaignRule bsExportCampaignRule;

    @Test
    void insertCampaignTest() {
        long cid = 10L;
        CampOptionsTableChange campaignsTableChange =
                new CampOptionsTableChange().withCid(cid);
        BinlogEvent binlogEvent = createCampOptionsEvent(List.of(campaignsTableChange), INSERT);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportCampaignObject.Builder()
                .setCid(cid)
                .setService("service")
                .setMethod("method")
                .setCampaignResourceType(CAMPAIGN_MEANINGFUL_GOALS)
                .setReqid(123L)
                .build();
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject);
    }


    @Test
    void updateMeaningfulGoalsTest() {
        long cid = 10L;
        CampOptionsTableChange campOptionsTableChange =
                new CampOptionsTableChange().withCid(cid);
        campOptionsTableChange.addChangedColumn(CAMP_OPTIONS.MEANINGFUL_GOALS, "",
                "[{\"value\": 200, \"goal_id\": 14312479}])");
        BinlogEvent binlogEvent = createCampOptionsEvent(List.of(campOptionsTableChange), UPDATE);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportCampaignObject.Builder()
                .setCid(cid)
                .setService("service")
                .setMethod("method")
                .setCampaignResourceType(CAMPAIGN_MEANINGFUL_GOALS)
                .setReqid(123L)
                .build();
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject);
    }

}
