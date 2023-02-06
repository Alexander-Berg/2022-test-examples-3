package ru.yandex.direct.core.entity.metrika.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.metrika.model.objectinfo.CampaignInfoForMetrika;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.multitype.entity.LimitOffset.limited;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaCampaignRepositoryCampaignInfoTest {

    @Autowired
    private MetrikaCampaignRepository repoUnderTest;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    private long uid1;
    private long uid11;
    private long uid2;
    private long uid3;

    private LocalDateTime someTime;

    private CampaignInfo campaignInfo11;
    private CampaignInfo campaignInfo21;
    private CampaignInfo campaignInfo22;
    private CampaignInfo campaignInfo31;

    private List<Long> allCampIds;
    private int shard;

    @Before
    public void before() {
        /*
            Создаем несколько клиентов
         */
        ClientInfo clientInfo1 = clientSteps.createDefaultClient();
        ClientInfo clientInfo2 = clientSteps.createDefaultClient();
        ClientInfo clientInfo3 = clientSteps.createDefaultClient();

        shard = clientInfo1.getShard();

        /*
            Создаем представителей для каждого клиента.
            Для первого клиента создаем двух представителей, чтобы проверить конкатенацию uid
         */
        uid1 = clientInfo1.getUid();
        uid11 = steps.userSteps().createRepresentative(clientInfo1).getUid();

        uid2 = steps.userSteps().createRepresentative(clientInfo2).getUid();
        uid3 = steps.userSteps().createRepresentative(clientInfo3).getUid();
        /*
            Создаем кампании
         */
        campaignInfo11 = campaignSteps.createActiveTextCampaign(clientInfo1);
        campaignInfo21 = campaignSteps.createActiveTextCampaign(clientInfo2);
        campaignInfo22 = campaignSteps.createActiveTextCampaign(clientInfo2);
        campaignInfo31 = campaignSteps.createActiveTextCampaign(clientInfo3);
        allCampIds = asList(
                campaignInfo11.getCampaignId(),
                campaignInfo21.getCampaignId(),
                campaignInfo22.getCampaignId(),
                campaignInfo31.getCampaignId());

        someTime = LocalDateTime.now().minusMinutes(5).withNano(0);

        /*
            Выставляем кампаниям время последнего изменения.
            У первой кампании LastChange должен быть больше, чем у одной из кампаний с большим id,
            чтобы проверить правильность условия выборки
         */
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.LAST_CHANGE, someTime.plusSeconds(1))
                .where(CAMPAIGNS.CID.equal(campaignInfo11.getCampaignId()))
                .execute();
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.LAST_CHANGE, someTime)
                .where(CAMPAIGNS.CID.equal(campaignInfo21.getCampaignId()))
                .execute();
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.LAST_CHANGE, someTime)
                .where(CAMPAIGNS.CID.equal(campaignInfo22.getCampaignId()))
                .execute();
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.LAST_CHANGE, someTime.minusSeconds(1))
                .where(CAMPAIGNS.CID.equal(campaignInfo31.getCampaignId()))
                .execute();
    }

    @Test
    public void getCampaignsInfo_Short_LimitIsNull_LimitWorksFine() {
        List<CampaignInfoForMetrika> campInfoList =
                repoUnderTest.getCampaignsInfo(shard, null);
        List<Long> fetchedIds = mapList(campInfoList, CampaignInfoForMetrika::getId);

        boolean allCampsExists = fetchedIds.containsAll(allCampIds);
        assertThat("в ответе метода должны присутствовать все созданные в тесте кампании",
                allCampsExists, is(true));
    }

    @Test
    public void getCampaignsInfo_Short_LimitIsDefined_LimitWorksFine() {
        List<CampaignInfoForMetrika> campInfoList =
                repoUnderTest.getCampaignsInfo(shard, limited(3));

        assertThat("в ответе метода должно присутствовать равное лимиту количество кампаний",
                campInfoList, hasSize(3));
    }

    @Test
    public void getCampaignsInfo_LimitIsNull_LimitWorksFine() {
        List<CampaignInfoForMetrika> campInfoList =
                repoUnderTest.getCampaignsInfo(shard, someTime.minusSeconds(1), 0L, null);
        List<Long> fetchedIds = mapList(campInfoList, CampaignInfoForMetrika::getId);

        boolean allCampsExists = fetchedIds.containsAll(allCampIds);
        assertThat("в ответе метода должны присутствовать все созданные в тесте кампании",
                allCampsExists, is(true));
    }

    @Test
    public void getCampaignsInfo_ResponseContainSuitableCampaigns() {
        List<CampaignInfoForMetrika> campInfoList =
                repoUnderTest.getCampaignsInfo(shard, someTime, campaignInfo21.getCampaignId(), null);
        List<Long> fetchedIds = mapList(campInfoList, CampaignInfoForMetrika::getId);

        boolean suitableCampsExists = fetchedIds.containsAll(
                asList(campaignInfo11.getCampaignId(), campaignInfo22.getCampaignId()));
        assertThat("в ответе метода должны присутствовать кампании, "
                        + "измененные после указанного времени или измененные в то же время, "
                        + "но имеющие больший id, чем переданный",
                suitableCampsExists, is(true));
    }

    @Test
    public void getCampaignsInfo_ResponseDoesNotContainCampaignsWithLessLastChange() {
        List<CampaignInfoForMetrika> campInfoList =
                repoUnderTest.getCampaignsInfo(shard, someTime, campaignInfo21.getCampaignId(), null);
        List<Long> fetchedIds = mapList(campInfoList, CampaignInfoForMetrika::getId);

        boolean unsuitableCampExists = fetchedIds.contains(campaignInfo31.getCampaignId());
        assertThat("в ответе метода не должно присутствовать кампаний, "
                        + "измененных до указанного времени",
                unsuitableCampExists, is(false));
    }

    @Test
    public void getCampaignsInfo_ResponseDoesNotContainCampaignsWithEqualLastChangeAndLessId() {
        List<CampaignInfoForMetrika> campInfoList =
                repoUnderTest.getCampaignsInfo(shard, someTime, campaignInfo21.getCampaignId(), null);
        List<Long> fetchedIds = mapList(campInfoList, CampaignInfoForMetrika::getId);

        boolean unsuitableCampExists = fetchedIds.contains(campaignInfo21.getCampaignId());
        assertThat("в ответе метода не должно присутствовать кампаний, "
                        + "измененных в указанное время, но имеющих id меньше или равного указанному",
                unsuitableCampExists, is(false));
    }

    @Test
    public void getCampaignsInfo_ResponseDoesNotContainCampaignsWithTooLateLastChange() {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.LAST_CHANGE, LocalDateTime.now().minusSeconds(27))
                .where(CAMPAIGNS.CID.equal(campaignInfo22.getCampaignId()))
                .execute();

        List<CampaignInfoForMetrika> campInfoList =
                repoUnderTest.getCampaignsInfo(shard, someTime, campaignInfo21.getCampaignId(), null);
        List<Long> fetchedIds = mapList(campInfoList, CampaignInfoForMetrika::getId);

        boolean unsuitableCampExists = fetchedIds.contains(campaignInfo22.getCampaignId());
        assertThat("в ответе метода не должно присутствовать кампаний, измененных за последние 30 секунд",
                unsuitableCampExists, is(false));
    }

    @Test
    public void getCampaignsInfo_ResponseDoesNotContainCampaignsWithoutOrderId() {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.ORDER_ID, 0L)
                .set(CAMPAIGNS.LAST_CHANGE, someTime.plusSeconds(1))
                .where(CAMPAIGNS.CID.equal(campaignInfo11.getCampaignId()))
                .execute();

        List<CampaignInfoForMetrika> campInfoList =
                repoUnderTest.getCampaignsInfo(shard, someTime, campaignInfo21.getCampaignId(), null);
        List<Long> fetchedIds = mapList(campInfoList, CampaignInfoForMetrika::getId);

        boolean unsuitableCampExists = fetchedIds.contains(campaignInfo11.getCampaignId());
        assertThat("в ответе метода не должно присутствовать кампаний без OrderID",
                unsuitableCampExists, is(false));
    }

    @Test
    public void getCampaignsInfo_ConcatsAllUids() {
        List<CampaignInfoForMetrika> campInfoList =
                repoUnderTest.getCampaignsInfo(shard, someTime, campaignInfo21.getCampaignId(), null);
        CampaignInfoForMetrika campInfoWithTwoUids = campInfoList.stream()
                .filter(c -> c.getId().equals(campaignInfo11.getCampaignId()))
                .findFirst()
                .get();
        assertThat(campInfoWithTwoUids.getReps(), beanDiffer(asList(uid1, uid11)));
    }
}
