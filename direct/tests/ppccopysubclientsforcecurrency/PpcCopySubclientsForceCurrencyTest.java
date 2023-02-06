package ru.yandex.autotests.directintapi.tests.ppccopysubclientsforcecurrency;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ClientsAllowCreateScampBySubclient;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ClientsWorkCurrency;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.logic.ppc.Clients;
import ru.yandex.autotests.directapi.logic.ppc.ForceCurrencyConvert;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 15.02.16.
 * https://st.yandex-team.ru/TESTIRT-8496
 */
@Aqua.Test()
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.PPC_COPY_SUBCLIENTS_FORCE_CURRENCY)
@Description("Проверка скрипта ppcCopySubclientsForceCurrency.pl")
@Issue("https://st.yandex-team.ru/DIRECT-48282")
public class PpcCopySubclientsForceCurrencyTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private static int shard;
    private static String acceptedAt;
    private static long agencyClientId;
    private static long subclient1ClientId;
    private static long subclient2ClientId;
    private static long subclientFreeClientId;

    private static final String AGENCY = "at-agency-force-conv";
    private static final String SUBCLIENT1 = "at-subclient-force-convert1";
    private static final String SUBCLIENT2 = "at-subclient-force-convert2";
    private static final String SUBCLIENT_FREE = "at-subclient-force-conv-free";

    private static final String PATTERN = "yyyy'-'MM'-'dd HH':'mm':'ss";


    @BeforeClass
    public static void prepareClients() {
        shard = darkSideSteps.getClientFakeSteps().getUserShard(AGENCY);

        agencyClientId = Long.valueOf(api.userSteps.clientFakeSteps().getClientData(AGENCY).getClientID());
        subclient1ClientId = Long.valueOf(api.userSteps.clientFakeSteps().getClientData(SUBCLIENT1).getClientID());
        subclient2ClientId = Long.valueOf(api.userSteps.clientFakeSteps().getClientData(SUBCLIENT2).getClientID());
        subclientFreeClientId =
                Long.valueOf(api.userSteps.clientFakeSteps().getClientData(SUBCLIENT_FREE).getClientID());

        acceptedAt = DateTime.now().toString(PATTERN) + ".0";

        darkSideSteps.getDBSteps().getForceCurrencyConvertSteps()
                .deleteForceCurrencyConvertByClientIdIfExist(agencyClientId, shard);
        darkSideSteps.getDBSteps().getForceCurrencyConvertSteps()
                .deleteForceCurrencyConvertByClientIdIfExist(subclient1ClientId, shard);
        darkSideSteps.getDBSteps().getForceCurrencyConvertSteps()
                .deleteForceCurrencyConvertByClientIdIfExist(subclient2ClientId, shard);
        darkSideSteps.getDBSteps().getForceCurrencyConvertSteps()
                .deleteForceCurrencyConvertByClientIdIfExist(subclientFreeClientId, shard);

        ForceCurrencyConvert forceCurrencyConvert = new ForceCurrencyConvert();
        forceCurrencyConvert.setClientId(agencyClientId);
        forceCurrencyConvert.setAcceptedAt(acceptedAt);
        darkSideSteps.getDBSteps().getForceCurrencyConvertSteps().saveForceCurrencyConvert(forceCurrencyConvert, shard);

        api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .clientsSteps().setClientsWorkCurrency(agencyClientId, ClientsWorkCurrency.YND_FIXED);
        api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .clientsSteps().setClientsWorkCurrency(subclient1ClientId, ClientsWorkCurrency.YND_FIXED);
        api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .clientsSteps().setClientsWorkCurrency(subclient2ClientId, ClientsWorkCurrency.YND_FIXED);
        api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .clientsSteps().setClientsWorkCurrency(subclientFreeClientId, ClientsWorkCurrency.YND_FIXED);
        api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .clientsSteps().setFree(subclientFreeClientId, ClientsAllowCreateScampBySubclient.Yes);

        darkSideSteps.getRunScriptSteps().runPpcCopySubclientsForceCurrency(AGENCY);
    }

    @Test
    public void checkForceCurrencyConvertForSubclient1() {
        ForceCurrencyConvert forceCurrencyConvert =
                darkSideSteps.getDBSteps().getForceCurrencyConvertSteps()
                        .getForceCurrencyConvert(subclient1ClientId, shard);
        ForceCurrencyConvert expectedForceCurrencyConvert = new ForceCurrencyConvert();
        expectedForceCurrencyConvert.setClientId(subclient1ClientId);
        expectedForceCurrencyConvert.setAcceptedAt(acceptedAt);
        assertThat("субклиент попал в ppc.force_currency_convert",
                forceCurrencyConvert, beanDiffer(expectedForceCurrencyConvert));
    }

    @Test
    public void checkForceCurrencyConvertForSubclient2() {
        ForceCurrencyConvert forceCurrencyConvert =
                darkSideSteps.getDBSteps().getForceCurrencyConvertSteps()
                        .getForceCurrencyConvert(subclient2ClientId, shard);
        ForceCurrencyConvert expectedForceCurrencyConvert = new ForceCurrencyConvert();
        expectedForceCurrencyConvert.setClientId(subclient2ClientId);
        expectedForceCurrencyConvert.setAcceptedAt(acceptedAt);
        assertThat("субклиент попал в ppc.force_currency_convert",
                forceCurrencyConvert, beanDiffer(expectedForceCurrencyConvert));
    }

    @Test
    public void checkForceCurrencyConvertForSubclientFree() {
        ForceCurrencyConvert forceCurrencyConvert =
                darkSideSteps.getDBSteps().getForceCurrencyConvertSteps()
                        .getForceCurrencyConvert(subclientFreeClientId, shard);
        assertThat("субклиент со свободой не попал в ppc.force_currency_convert", forceCurrencyConvert, nullValue());
    }
}
