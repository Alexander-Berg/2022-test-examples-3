package ru.yandex.direct.core.entity.metrika.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.metrika.model.objectinfo.RetargetingConditionInfoForMetrika;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.RETARGETING_CONDITIONS;
import static ru.yandex.direct.multitype.entity.LimitOffset.limited;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaRetargetingConditionRepositoryInfoTest {

    @Autowired
    private MetrikaRetargetingConditionRepository repoUnderTest;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    private LocalDateTime someTime;

    private RetConditionInfo retConditionInfo1;
    private RetConditionInfo retConditionInfo2;
    private RetConditionInfo retConditionInfo3;
    private RetConditionInfo retConditionInfo4;

    private long retCondId1;
    private long retCondId2;
    private long retCondId3;
    private long retCondId4;

    private List<Long> allRetCondIds;
    private int shard;

    @Before
    public void before() {
        retConditionInfo1 = retConditionSteps.createDefaultRetCondition();
        retConditionInfo2 = retConditionSteps.createDefaultRetCondition();
        retConditionInfo3 = retConditionSteps.createDefaultRetCondition();
        retConditionInfo4 = retConditionSteps.createDefaultRetCondition();

        retCondId1 = retConditionInfo1.getRetConditionId();
        retCondId2 = retConditionInfo2.getRetConditionId();
        retCondId3 = retConditionInfo3.getRetConditionId();
        retCondId4 = retConditionInfo4.getRetConditionId();

        shard = retConditionInfo1.getShard();

        someTime = LocalDateTime.now().minusMinutes(5).withNano(0);

        /*
            Выставляем условиям ретаргетинга время последнего изменения
         */
        dslContextProvider.ppc(shard)
                .update(RETARGETING_CONDITIONS)
                .set(RETARGETING_CONDITIONS.MODTIME, someTime.plusSeconds(1))
                .where(RETARGETING_CONDITIONS.RET_COND_ID.equal(retConditionInfo1.getRetConditionId()))
                .execute();
        dslContextProvider.ppc(shard)
                .update(RETARGETING_CONDITIONS)
                .set(RETARGETING_CONDITIONS.MODTIME, someTime)
                .where(RETARGETING_CONDITIONS.RET_COND_ID.equal(retConditionInfo2.getRetConditionId()))
                .execute();
        dslContextProvider.ppc(shard)
                .update(RETARGETING_CONDITIONS)
                .set(RETARGETING_CONDITIONS.MODTIME, someTime)
                .where(RETARGETING_CONDITIONS.RET_COND_ID.equal(retConditionInfo3.getRetConditionId()))
                .execute();
        dslContextProvider.ppc(shard)
                .update(RETARGETING_CONDITIONS)
                .set(RETARGETING_CONDITIONS.MODTIME, someTime.minusSeconds(1))
                .where(RETARGETING_CONDITIONS.RET_COND_ID.equal(retConditionInfo4.getRetConditionId()))
                .execute();

        allRetCondIds = asList(retCondId1, retCondId2, retCondId3, retCondId4);
    }

    @Test
    public void getRetargetingConditionsInfo_Short_LimitIsNull_LimitWorksFine() {
        List<RetargetingConditionInfoForMetrika> retConditionsInfoList =
                repoUnderTest.getRetargetingConditionsInfo(shard, null);
        List<Long> fetchedIds = mapList(retConditionsInfoList, RetargetingConditionInfoForMetrika::getId);

        boolean allRetConditionsExists = fetchedIds.containsAll(allRetCondIds);
        assertThat("в ответе метода должны присутствовать все созданные в тесте условия ретаргетинга",
                allRetConditionsExists, is(true));
    }

    @Test
    public void getRetargetingConditionsInfo_Short_LimitIsDefined_LimitWorksFine() {
        List<RetargetingConditionInfoForMetrika> retConditionsInfoList =
                repoUnderTest.getRetargetingConditionsInfo(shard, limited(3));

        assertThat("в ответе метода должно присутствовать равное лимиту количество условий ретаргетинга",
                retConditionsInfoList, hasSize(3));
    }

    @Test
    public void getRetargetingConditionsInfo_LimitIsNull_LimitWorksFine() {
        List<RetargetingConditionInfoForMetrika> retConditionsInfoList =
                repoUnderTest.getRetargetingConditionsInfo(shard, someTime.minusSeconds(1), 0L, null);
        List<Long> fetchedIds = mapList(retConditionsInfoList, RetargetingConditionInfoForMetrika::getId);

        boolean allRetConditionsExists = fetchedIds.containsAll(allRetCondIds);
        assertThat("в ответе метода должны присутствовать все созданные в тесте условия ретаргетинга",
                allRetConditionsExists, is(true));
    }

    @Test
    public void getRetargetingConditionsInfo_LimitIsDefined_LimitWorksFine() {

        List<RetargetingConditionInfoForMetrika> retConditionsInfoList =
                repoUnderTest.getRetargetingConditionsInfo(shard, someTime.minusSeconds(1),
                        0L, limited(3));

        assertThat("в ответе метода должно присутствовать равное лимиту количество условий ретаргетинга",
                retConditionsInfoList, hasSize(3));
    }

    @Test
    public void getRetargetingConditionsInfo_ResponseContainSuitableConditions() {
        List<RetargetingConditionInfoForMetrika> retConditionsInfoList =
                repoUnderTest.getRetargetingConditionsInfo(shard, someTime, retCondId2, null);
        List<Long> fetchedIds = mapList(retConditionsInfoList, RetargetingConditionInfoForMetrika::getId);

        boolean suitableBannersExists = fetchedIds.containsAll(asList(retCondId1, retCondId3));
        assertThat("в ответе метода должны присутствовать условия ретаргетинга, "
                        + "измененные после указанного времени или измененные в то же время, "
                        + "но имеющие больший ret_cond_id, чем переданный",
                suitableBannersExists, is(true));
    }

    @Test
    public void getRetargetingConditionsInfo_ResponseDoesNotContainConditionsWithLessLastChange() {
        List<RetargetingConditionInfoForMetrika> retConditionsInfoList =
                repoUnderTest.getRetargetingConditionsInfo(shard, someTime, retCondId2, null);
        List<Long> fetchedIds = mapList(retConditionsInfoList, RetargetingConditionInfoForMetrika::getId);

        boolean unsuitableBannersExists = fetchedIds.contains(retCondId4);
        assertThat("в ответе метода не должно присутствовать условий ретаргетинга, "
                        + "измененных до указанного времени",
                unsuitableBannersExists, is(false));
    }

    @Test
    public void getRetargetingConditionsInfo_ResponseDoesNotContainConditionsWithEqualLastChangeAndLessId() {
        List<RetargetingConditionInfoForMetrika> retConditionsInfoList =
                repoUnderTest.getRetargetingConditionsInfo(shard, someTime, retCondId2, null);
        List<Long> fetchedIds = mapList(retConditionsInfoList, RetargetingConditionInfoForMetrika::getId);

        boolean unsuitableBannersExists = fetchedIds.contains(retCondId2);
        assertThat("в ответе метода не должно присутствовать условий ретаргетинга, "
                        + "измененных в указанное время, но имеющих id меньше или равного указанному",
                unsuitableBannersExists, is(false));
    }

}
