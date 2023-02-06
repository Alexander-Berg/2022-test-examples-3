package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.MetrikaCounter;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_METRIKA_COUNTERS;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignMetrikaCountersRepositoryTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;
    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private CampMetrikaCountersRepository campMetrikaCountersRepository;

    private int shard;
    private long cid;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        CampaignInfo defaultCampaign = steps.campaignSteps().createDefaultCampaignByCampaignType(campaignType);
        shard = defaultCampaign.getShard();
        cid = defaultCampaign.getCampaignId();

        List<Long> defaultCounters = Arrays.asList(1234L, 567L);
        Map<Long, List<MetrikaCounter>> countersForUpdate = new HashMap<>();
        countersForUpdate.put(cid, mapList(defaultCounters, c -> new MetrikaCounter().withId(c)));

        campMetrikaCountersRepository.updateMetrikaCounters(shard, countersForUpdate);

        Map<Long, List<Long>> campMetrikaCounters =
                campMetrikaCountersRepository.getMetrikaCountersByCids(shard, Collections.singletonList(cid));

        checkState(defaultCounters.equals(campMetrikaCounters.get(cid)),
                "проверяем, что в базу добавились счетчики метрики");
    }

    @Test
    public void checkUpdateCampMetrikaCounters() {
        List<Long> expectedCounters = Arrays.asList(78L, 17L);
        Map<Long, List<MetrikaCounter>> countersForUpdate = new HashMap<>();
        countersForUpdate.put(cid, mapList(expectedCounters, c -> new MetrikaCounter().withId(c)));

        campMetrikaCountersRepository.updateMetrikaCounters(shard, countersForUpdate);

        Map<Long, List<Long>> actualCounters =
                campMetrikaCountersRepository.getMetrikaCountersByCids(shard, Collections.singletonList(cid));

        assertThat("проверяем, что в базе сохранились корректные счетчики метрики",
                actualCounters.get(cid), equalTo(expectedCounters));
    }

    @Test
    public void checkDeleteCampMetrikaCounters() {
        campMetrikaCountersRepository.deleteMetrikaCounters(shard, Collections.singletonList(cid));

        Map<Long, List<Long>> actualCounters =
                campMetrikaCountersRepository.getMetrikaCountersByCids(shard, Collections.singletonList(cid));

        assertThat("проверяем, что счетчики метрики удалены из базы",
                actualCounters.get(cid).size(), equalTo(0));
    }

    /**
     * Тест: если в таблице CAMP_METRIKA_COUNTERS есть строка содержащая campId и пустое значение metrika_counters ->
     * метод getMetrikaCountersByCids, по данному campId, вернет набор содержащий campId с пустым списоком
     */
    @Test
    public void getCampMetrikaCountersByCids_whenHasEmptyMetrikaCounter() {
        insertCampMetrikaCounter(cid, CampaignMappings.metrikaCounterIdsToDb(List.of()));

        Map<Long, List<Long>> map = campMetrikaCountersRepository.getMetrikaCountersByCids(shard, List.of(cid));
        Assertions.assertThat(map).containsEntry(cid, Collections.emptyList());
    }

    /**
     * Тест: если в таблице CAMP_METRIKA_COUNTERS есть строка содержащая campId и большое значение metrika_counters ->
     * метод getMetrikaCountersByCids, по данному campId, вернет набор содержащий campId со списком из одного этого
     * большого значения
     */
    @Test
    public void getCampMetrikaCountersByCids_whenMetrikaCounterIsBig() {
        List<Long> metrikaCountersExpected = List.of(9223372036854775807L);
        insertCampMetrikaCounter(cid, CampaignMappings.metrikaCounterIdsToDb(metrikaCountersExpected));

        Map<Long, List<Long>> map = campMetrikaCountersRepository.getMetrikaCountersByCids(shard, List.of(cid));
        Assertions.assertThat(map).containsEntry(cid, metrikaCountersExpected);
    }

    /**
     * Тест: если в таблице CAMP_METRIKA_COUNTERS есть строка содержащая campId и несколько значений metrika_counters ->
     * метод getMetrikaCountersByCids, по данному campId, вернет набор содержащий campId со списком из этих значений
     */
    @Test
    public void getCampMetrikaCountersByCids_whenHasMultipleMetrikaCounters() {
        List<Long> metrikaCountersExpected = List.of(1L, 555L, 17555L, 0L);
        insertCampMetrikaCounter(cid, CampaignMappings.metrikaCounterIdsToDb(metrikaCountersExpected));

        Map<Long, List<Long>> map = campMetrikaCountersRepository.getMetrikaCountersByCids(shard, List.of(cid));
        Assertions.assertThat(map).containsEntry(cid, metrikaCountersExpected);
    }

    private void insertCampMetrikaCounter(long campId, String metrikaCounter) {
        dslContextProvider.ppc(shard)
                .insertInto(CAMP_METRIKA_COUNTERS, CAMP_METRIKA_COUNTERS.CID, CAMP_METRIKA_COUNTERS.METRIKA_COUNTERS)
                .values(campId, metrikaCounter)
                .onDuplicateKeyUpdate()
                .set(CAMP_METRIKA_COUNTERS.METRIKA_COUNTERS, metrikaCounter)
                .execute();
    }
}
