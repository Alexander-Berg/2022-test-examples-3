package ru.yandex.direct.core.entity.campaign.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_ACTIVIZATION;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddCampsForActivizationTest {

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private CampActivizationRepository campActivizationRepository;

    private int shard;
    private long cid;
    private ClientId clientId;

    @Before
    public void before() {
        CampaignInfo defaultCampaign = steps.campaignSteps().createDefaultCampaign();
        shard = defaultCampaign.getShard();
        cid = defaultCampaign.getCampaignId();
        clientId = defaultCampaign.getClientId();
    }


    @Test
    public void checkCid() {
        campActivizationRepository.addCampsForActivization(shard, Arrays.asList(cid));

        Long cidFromDb = dslContextProvider.ppc(shard)
                .select(CAMP_ACTIVIZATION.CID)
                .from(CAMP_ACTIVIZATION)
                .where(CAMP_ACTIVIZATION.CID.eq(cid))
                .fetchOne(CAMP_ACTIVIZATION.CID);

        assertThat("проверяем, что cid сохранился в таблице camp_activization", cidFromDb, equalTo(cid));
    }

    @Test
    public void checkAddTwoCampsForActivization() {
        long anotherCid = shardHelper.generateCampaignIds(clientId.asLong(), 1).get(0);
        campActivizationRepository.addCampsForActivization(shard, Arrays.asList(cid, anotherCid));

        List<Long> cidsFromDb = dslContextProvider.ppc(shard)
                .select(CAMP_ACTIVIZATION.CID)
                .from(CAMP_ACTIVIZATION)
                .where(CAMP_ACTIVIZATION.CID.in(cid, anotherCid))
                .fetch(CAMP_ACTIVIZATION.CID);

        assertThat("проверяем, что получили из базы два cid-а", cidsFromDb, hasSize(2));
        assertThat("проверяем, что cid и anotherCid сохранились в таблице camp_activization",
                cidsFromDb, hasItems(cid, anotherCid));
    }

    @Test
    public void checkSendTime() {
        LocalDateTime sendTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        campActivizationRepository.addCampsForActivization(shard, Arrays.asList(cid), sendTime);

        LocalDateTime sendTimeFromDb = dslContextProvider.ppc(shard)
                .select(CAMP_ACTIVIZATION.SEND_TIME)
                .from(CAMP_ACTIVIZATION)
                .where(CAMP_ACTIVIZATION.CID.eq(cid))
                .fetchOne(CAMP_ACTIVIZATION.SEND_TIME);

        assertThat("проверяем, что send_time сохранился с ожидаемым значением в таблице camp_activization",
                sendTimeFromDb, equalTo(sendTime));
    }

    @Test
    public void checkSendTimeAfterDoubleInsertOneCid() {
        LocalDateTime sendTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        campActivizationRepository.addCampsForActivization(shard, Arrays.asList(cid), sendTime);

        LocalDateTime anotherSendTime = LocalDateTime.now().plusHours(1);
        campActivizationRepository.addCampsForActivization(shard, Arrays.asList(cid), anotherSendTime);

        LocalDateTime sendTimeFromDb = dslContextProvider.ppc(shard)
                .select(CAMP_ACTIVIZATION.SEND_TIME)
                .from(CAMP_ACTIVIZATION)
                .where(CAMP_ACTIVIZATION.CID.eq(cid))
                .fetchOne(CAMP_ACTIVIZATION.SEND_TIME);

        assertThat("проверяем, что send_time не изменился в таблице camp_activization, "
                        + "т.к. вставка id кампании, которая уже есть в таблице, игнорируется.",
                sendTimeFromDb, equalTo(sendTimeFromDb));
    }
}
