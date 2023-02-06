package ru.yandex.direct.ess.router.rules.promocodescheckcampaignchanges;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.ess.logicobjects.promocodescheckcampaignchanges.IdTypeEnum;
import ru.yandex.direct.ess.logicobjects.promocodescheckcampaignchanges.PromocodesCheckCampaignChangesObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.AdgroupsDynamicTableChange;
import ru.yandex.direct.ess.router.testutils.BannersTableChange;
import ru.yandex.direct.ess.router.testutils.PhrasesTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_DYNAMIC;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.ess.router.testutils.AdgroupsDynamicTableChange.createAdgroupsDynamicEvent;
import static ru.yandex.direct.ess.router.testutils.BannersTableChange.createBannersEvent;
import static ru.yandex.direct.ess.router.testutils.PhrasesTableChange.createPhrasesEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class PromocodesCheckCampaignChangesRuleTest {

    @Autowired
    private PromocodesCheckCampaignChangesRule rule;

    @Test
    void mapBinlogEventTest_InsertIntoBanners() {
        List<BannersTableChange> bannersTableChanges = new ArrayList<>();

        BannersTableChange bannersTableChange1 = new BannersTableChange().withBid(1L).withCid(2L).withPid(3L);
        bannersTableChanges.add(bannersTableChange1);

        BannersTableChange bannersTableChange2 = new BannersTableChange().withBid(4L).withCid(5L).withPid(6L);
        bannersTableChanges.add(bannersTableChange2);

        BinlogEvent binlogEvent = createBannersEvent(bannersTableChanges, INSERT);
        List<PromocodesCheckCampaignChangesObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        PromocodesCheckCampaignChangesObject[] expected = bannersTableChanges.stream()
                .map(tableChange -> new PromocodesCheckCampaignChangesObject(tableChange.cid, IdTypeEnum.CID))
                .toArray(PromocodesCheckCampaignChangesObject[]::new);
        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_InsertIntoPhrases() {
        List<PhrasesTableChange> phrasesTableChanges = new ArrayList<>();

        PhrasesTableChange tableChange = new PhrasesTableChange().withCid(1L).withPid(2L);
        phrasesTableChanges.add(tableChange);

        BinlogEvent binlogEvent = createPhrasesEvent(phrasesTableChanges, INSERT);
        List<PromocodesCheckCampaignChangesObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).isEmpty();
    }

    @Test
    void mapBinlogEventTest_UpdateBanners() {
        List<BannersTableChange> tableChanges = new ArrayList<>();
        String domainBefore = "example.com";
        String domainAfter = "example.org";
        String domainAnotherOne = "example.ru";

        /* Поля domain нет в списке изменившихся полей */
        BannersTableChange bannersTableChangeWithoutDomain =
                new BannersTableChange().withBid(1L).withCid(2L).withPid(3L);
        bannersTableChangeWithoutDomain.addChangedColumn(BANNERS.BODY, "foo", "bar");
        tableChanges.add(bannersTableChangeWithoutDomain);

        BannersTableChange bannersTableChangeWithDomain =
                new BannersTableChange().withBid(4L).withCid(5L).withPid(6L);
        bannersTableChangeWithDomain.addChangedColumn(BANNERS.DOMAIN, domainBefore, domainAfter);
        tableChanges.add(bannersTableChangeWithDomain);

        BannersTableChange bannersTableChangeDomainEquals =
                new BannersTableChange().withBid(7L).withCid(8L).withPid(9L);
        bannersTableChangeDomainEquals.addChangedColumn(BANNERS.DOMAIN, domainAnotherOne, domainAnotherOne);

        BannersTableChange bannersTableChangeWithDomain2 =
                new BannersTableChange().withBid(10L).withCid(11L).withPid(12L);
        bannersTableChangeWithDomain2.addChangedColumn(BANNERS.DOMAIN, null, domainAfter);
        tableChanges.add(bannersTableChangeWithDomain2);

        BinlogEvent binlogEvent = createBannersEvent(tableChanges, UPDATE);
        List<PromocodesCheckCampaignChangesObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        PromocodesCheckCampaignChangesObject[] expected = new PromocodesCheckCampaignChangesObject[]{
                new PromocodesCheckCampaignChangesObject(5L, IdTypeEnum.CID),
                new PromocodesCheckCampaignChangesObject(11L, IdTypeEnum.CID),
        };

        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }


    @Test
    void mapBinlogEventTest_InsertIntoAdgroupsDynamic() {
        List<AdgroupsDynamicTableChange> tableChanges = new ArrayList<>();

        AdgroupsDynamicTableChange tableChange1 = new AdgroupsDynamicTableChange().withPid(1L);
        tableChanges.add(tableChange1);

        AdgroupsDynamicTableChange tableChange2 = new AdgroupsDynamicTableChange().withPid(2L);
        tableChanges.add(tableChange2);

        BinlogEvent binlogEvent = createAdgroupsDynamicEvent(tableChanges, INSERT);
        List<PromocodesCheckCampaignChangesObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        PromocodesCheckCampaignChangesObject[] expected = tableChanges.stream()
                .map(tableChange -> new PromocodesCheckCampaignChangesObject(tableChange.pid, IdTypeEnum.PID))
                .toArray(PromocodesCheckCampaignChangesObject[]::new);
        assertThat(resultObjects).hasSize(2);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_UpdateAdgroupsDynamic() {
        List<AdgroupsDynamicTableChange> tableChanges = new ArrayList<>();

        /* Поля main_domain_id нет в списке изменившихся полей */
        AdgroupsDynamicTableChange tableChangeWithoutMainDomainId =
                new AdgroupsDynamicTableChange().withPid(1L);
        tableChangeWithoutMainDomainId.addChangedColumn(ADGROUPS_DYNAMIC.FEED_ID, 42L, 43L);
        tableChanges.add(tableChangeWithoutMainDomainId);

        AdgroupsDynamicTableChange tableChangeWithMainDomainId =
                new AdgroupsDynamicTableChange().withPid(2L);
        tableChangeWithMainDomainId.addChangedColumn(ADGROUPS_DYNAMIC.MAIN_DOMAIN_ID, 44L, 45L);
        tableChanges.add(tableChangeWithMainDomainId);

        AdgroupsDynamicTableChange tableChangeMainDomainIdEquals =
                new AdgroupsDynamicTableChange().withPid(3L);
        tableChangeMainDomainIdEquals.addChangedColumn(ADGROUPS_DYNAMIC.MAIN_DOMAIN_ID, 46L, 46L);

        BinlogEvent binlogEvent = createAdgroupsDynamicEvent(tableChanges, UPDATE);
        List<PromocodesCheckCampaignChangesObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        PromocodesCheckCampaignChangesObject[] expected = new PromocodesCheckCampaignChangesObject[]{
                new PromocodesCheckCampaignChangesObject(2L, IdTypeEnum.PID)
        };

        assertThat(resultObjects).hasSize(1);
        assertThat(resultObjects).containsExactly(expected);
    }
}
