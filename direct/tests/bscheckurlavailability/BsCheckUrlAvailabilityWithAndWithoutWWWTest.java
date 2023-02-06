package ru.yandex.autotests.directintapi.tests.bscheckurlavailability;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BsDeadDomainsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppcdict.tables.records.DomainsDictRecord;
import ru.yandex.autotests.direct.db.steps.BsDeadDomainsSteps;
import ru.yandex.autotests.direct.db.steps.DomainsDictSteps;
import ru.yandex.autotests.direct.db.steps.DomainsSteps;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

import org.junit.*;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aqua.Test
@Title("Проверка успешной записи и удаления доменов из bs_dead_domains скриптом bsCheckUrlAvailability.pl")
@Issue("https://st.yandex-team.ru/DIRECT-69913")
@RunWith(Parameterized.class)
public class BsCheckUrlAvailabilityWithAndWithoutWWWTest {

    private static final String LOGIN = Logins.PPC_FEED_TO_BANNER_LAND_4;
    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    private static int shardId;
    private List<Long> aliveIds;
    private List<Long> deadIds;

    private static BsDeadDomainsSteps deadDomSteps;
    private static DomainsDictSteps domDictS;
    private static DomainsSteps domS;

    private static String dDead = "111sttestdomainbscheckavailability.ru";
    private static String dWwwDead = "www." + dDead;

    private static String d = "222ndtestdomainbscheckavailability.ru";
    private static String dWww = "www." + d;

    private static List<String> allTestDomainNames = Arrays.asList(dWwwDead, dWww, dDead, d);
    private static List<String> allInitiallyDeadDomains = Arrays.asList(dWwwDead, dDead);
    private static List<DomainsDictRecord> allTestDomains;
    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter(0)
    public List<String> dead;

    @Parameterized.Parameter(1)
    public List<String> alive;

    @Parameterized.Parameters(name = "Домен {0} мертв, домен {1} жив")
    public static Collection data() {
        Object[][] data = new Object[][]{
                {Arrays.asList(dWww), Arrays.asList()},
                {Arrays.asList(d), Arrays.asList()},
                {Arrays.asList(dDead), Arrays.asList()},
                {Arrays.asList(dWwwDead), Arrays.asList()},
                {Arrays.asList(), Arrays.asList(dDead)},
                {Arrays.asList(), Arrays.asList(dWwwDead)},
                {Arrays.asList(), Arrays.asList(d)},
                {Arrays.asList(), Arrays.asList(dWww)}
        };

        return Arrays.asList(data);
    }

    @BeforeClass
    public static void beforeClass() {
        shardId = api.userSteps.clientFakeSteps().getUserShard(LOGIN);
        deadDomSteps = api.userSteps.getDirectJooqDbSteps().useShard(shardId).bsDeadDomainsSteps();
        domDictS = api.userSteps.getDirectJooqDbSteps().domainsDictSteps();
        domS = api.userSteps.getDirectJooqDbSteps().useShard(shardId).domainsSteps();
        allTestDomains = allTestDomainNames.stream()
                .map(name -> domDictS.createDomain(
                        new DomainsDictRecord().setDomain(name)))
                .collect(Collectors.toList());
        allTestDomains.forEach(d -> domS.createDomain(d.getDomainId(), d.getDomain()));
    }

    @Before
    public void before() {

        allTestDomains.forEach(d -> {
            if (allInitiallyDeadDomains.contains(d.getDomain())) {
                deadDomSteps.createBsDeadDomains(new BsDeadDomainsRecord().setDomainId(d.getDomainId()));
            }
        });

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runBsCheckUrlAvailability(shardId, alive, dead);
        aliveIds = domDictS.getDomainId(alive.toArray(new String[alive.size()]));

        deadIds = domDictS.getDomainId(dead.toArray(new String[dead.size()]));

    }

    @Test
    public void checkDeadDomains() {
        // таблица bs_dead_domains
        allTestDomains.forEach(d -> {
            if (aliveIds.contains(d.getDomainId())) {
                assertThat("в таблице bs_dead_domains нет живых доменов"
                        , deadDomSteps.getBsDeadDomain(d.getDomainId())
                        , nullValue());
            } else if (deadIds.contains(d.getDomainId())) {
                assertThat("в таблице bs_dead_domains есть все мертвые домены"
                        , deadDomSteps.getBsDeadDomain(d.getDomainId())
                        , notNullValue());
            } else if (allInitiallyDeadDomains.contains(d.getDomain())) {
                assertThat("в таблице bs_dead_domains остались все изначально мертвые домены которых не было в пришедших живых"
                        , deadDomSteps.getBsDeadDomain(d.getDomainId())
                        , notNullValue());
            } else {
                assertThat("в таблице bs_dead_domains не появилось лишних доменов"
                        , deadDomSteps.getBsDeadDomain(d.getDomainId())
                        , nullValue());
            }
        });
    }


    @After
    public void after() {
        deadDomSteps.deleteBsDeadDomains(allTestDomains.stream().map(DomainsDictRecord::getDomainId)
                .toArray(Long[]::new));
    }

    @AfterClass
    public static void afterClass() {
        Long[] ids = allTestDomains.stream().map(DomainsDictRecord::getDomainId).toArray(Long[]::new);
        domDictS.deleteDomain(ids);
        domS.deleteDomain(ids);
    }

}
