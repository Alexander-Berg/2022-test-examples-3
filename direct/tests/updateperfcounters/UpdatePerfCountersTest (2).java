package ru.yandex.autotests.directintapi.tests.updateperfcounters;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsPerformanceNowOptimizingBy;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.MetrikaCountersRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.UPDATE_PERF_COUNTERS)
@Description("Работа скрипта update_perf_counters.pl")
@Issue("https://st.yandex-team.ru/DIRECT-52696")
@RunWith(Parameterized.class)
public class UpdatePerfCountersTest {

    private static final Long METRIKA_COUNTER_ECOMMERCE = 42L;
    private static final Long WRONG_METRIKA_COUNTER = 111L;
    private static final Long METRIKA_COUNTER = 31844711L;

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private DirectJooqDbSteps dbSteps;
    private int shard;
    private Long campaignId;

    @Parameterized.Parameter(0)
    public String client;

    @Parameterized.Parameter(1)
    public Long metrikaCounter;

    @Parameterized.Parameter(2)
    public Integer isDeleted;

    @Parameterized.Parameter(3)
    public Integer hasEcommerce;

    @Parameterized.Parameter(4)
    public Integer expectedIsDeleted;

    @Parameterized.Parameter(5)
    public Integer expectedHasEcommerce;

    @Parameterized.Parameters(name = "У {0} проверяем счетчик {1} с isDeleted = {4}, hasEcommerce = {5}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"holodilnikru", METRIKA_COUNTER_ECOMMERCE, 1, 0, 0, 1},
                {"holodilnikru", WRONG_METRIKA_COUNTER, 0, 1, 1, null},
                {Logins.LOGIN_MAIN, METRIKA_COUNTER, 1, 1, 0, 0}
        });
    }

    @Before
    public void before() {
        shard = api.userSteps.clientFakeSteps().getUserShard(client);
        dbSteps = new DirectJooqDbSteps().useShard(shard);

        campaignId = api.userSteps.addDraftCampaign(client);
        api.userSteps.getDirectJooqDbSteps().useShard(shard);
        api.userSteps.getDirectJooqDbSteps().campaignsSteps()
                .addCampaignsPerformanceWithNowOptimizingBy(campaignId, CampaignsPerformanceNowOptimizingBy.CPC);
        dbSteps.ppcPropertiesSteps().deleteUpdatePerfCountersScriptSuccess();

        setMetrikaCountersInDB();
    }

    @Test
    @Description("Проверка изменения флагов таблицы metrika_counters после запуска скрипта update_perf_counters.pl")
    public void changeMetrikaCounterEcommerceTest() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runUpdatePerfCounters(shard, campaignId.intValue());

        sendAndCheck();
    }

    private void sendAndCheck() {
        MetrikaCountersRecord actualMetrikaCounters = dbSteps.metrikaCountersSteps()
                .getMetrikaCounters(campaignId, metrikaCounter);

        assertThat("Флаг is_deleted соответствует ожиданию",
                actualMetrikaCounters.getIsDeleted(), equalTo(expectedIsDeleted));

        assertThat("Флаг metrika_has_ecommerce соответствует ожиданию",
                actualMetrikaCounters.getHasEcommerce(), equalTo(expectedHasEcommerce));
    }

    private void setMetrikaCountersInDB() {
        dbSteps.metrikaCountersSteps().createMetrikaCounters(campaignId, metrikaCounter, isDeleted, hasEcommerce);

        MetrikaCountersRecord metrikaCounters = dbSteps.metrikaCountersSteps()
                .getMetrikaCounters(campaignId, metrikaCounter);
        assumeThat("Флаг isDeleted установлен", metrikaCounters.getIsDeleted(), equalTo(isDeleted));
        assumeThat("Флаг metrika_has_ecommerce установлен", metrikaCounters.getHasEcommerce(), equalTo(hasEcommerce));
    }
}
