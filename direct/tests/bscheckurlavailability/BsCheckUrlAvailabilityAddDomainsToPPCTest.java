package ru.yandex.autotests.directintapi.tests.bscheckurlavailability;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.DomainsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppcdict.tables.records.DomainsDictRecord;
import ru.yandex.autotests.direct.db.steps.BsDeadDomainsSteps;
import ru.yandex.autotests.direct.db.steps.DomainsDictSteps;
import ru.yandex.autotests.direct.db.steps.DomainsSteps;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


@Aqua.Test
@Title("Проверка успешной записи доменов скриптом bsCheckUrlAvailability.pl")
@Issue("https://st.yandex-team.ru/TESTIRT-11700")
@RunWith(Parameterized.class)
public class BsCheckUrlAvailabilityAddDomainsToPPCTest {
    @ClassRule
    public static final SemaphoreRule SEMAPHORE = Semaphore.getSemaphore();
    private static final String LOGIN = Logins.PPC_FEED_TO_BANNER_LAND_4;
    @ClassRule
    public static final ApiSteps API = new ApiSteps().version(104).as(LOGIN);
    private static DomainsDictSteps domDictS;
    private static DomainsSteps domS;
    private static BsDeadDomainsSteps deadDomSteps;
    private static int shardId;
    @Rule
    public Trashman trasher = new Trashman(API);
    @Parameterized.Parameter
    public String domainSt;
    private DomainsDictRecord domain;

    @Parameterized.Parameters(name = "Домен {0}")
    public static Collection data() {
        Object[][] data = new Object[][]{
                {"testdomainbscheckavailabilityadddomaintoppc.org"},
                {"www.testdomainbscheckavailabilityadddomaintoppc.org"}
        };

        return Arrays.asList(data);
    }


    @BeforeClass
    public static void beforeClass() {
        shardId = API.userSteps.clientFakeSteps().getUserShard(LOGIN);
        domDictS = API.userSteps.getDirectJooqDbSteps().domainsDictSteps();
        domS = API.userSteps.getDirectJooqDbSteps().useShard(shardId).domainsSteps();
        deadDomSteps = API.userSteps.getDirectJooqDbSteps().useShard(shardId).bsDeadDomainsSteps();
    }

    @Before
    public void before() {
        domain = domDictS.createDomain(new DomainsDictRecord().setDomain(domainSt));
    }

    @Test
    public void test() {

        API.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runBsCheckUrlAvailability(shardId, Collections.emptyList(), Collections.singletonList(domainSt));

        DomainsRecord d = domS.getDomain(domainSt);
        assertThat("Домен присутствующий в ppcdict.domains_dict записался в таблицу ppc.dict"
                , d, notNullValue());


        assertThat("Домен присутствующий в ppcdict.domains_dict записался в таблицу ppc.dict с тем же id"
                , d.getDomainId(), equalTo(domain.getDomainId()));
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    @After
    public void after() {
        deadDomSteps.deleteBsDeadDomains(domain.getDomainId());
        domS.deleteDomain(domain.getDomainId());
        domDictS.deleteDomain(domain.getDomainId());
    }

}
