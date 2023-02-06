package ru.yandex.autotests.directintapi.tests.ppccopysubclientsforcecurrency;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ClientsAllowCreateScampBySubclient;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ClientsWorkCurrency;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ForceCurrencyConvertRecord;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.direct.db.utils.JooqRecordDifferMatcher.recordDiffer;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

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
    public static final ApiSteps api = new ApiSteps().version(104).wsdl(APIPort_PortType.class);

    @ClassRule
    public static final SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private static Timestamp acceptedAt;
    private static long subclient1ClientId;
    private static long subclient2ClientId;
    private static long subclientFreeClientId;

    private static final String AGENCY = "at-agency-force-conv";
    private static final String SUBCLIENT1 = "at-subclient-force-convert1";
    private static final String SUBCLIENT2 = "at-subclient-force-convert2";
    private static final String SUBCLIENT_FREE = "at-subclient-force-conv-free";

    @BeforeClass
    public static void prepareClients() {
        int shard = darkSideSteps.getClientFakeSteps().getUserShard(AGENCY);

        // тест будет падать, если у субклиентов нет ни одной кампании, поэтому добавляем им по кампании
        api.userSteps.campaignSteps().addDefaultTextCampaign(SUBCLIENT1);
        api.userSteps.campaignSteps().addDefaultTextCampaign(SUBCLIENT2);

        long agencyClientId = Long.parseLong(api.userSteps.clientFakeSteps().getClientData(AGENCY).getClientID());
        subclient1ClientId = Long.parseLong(api.userSteps.clientFakeSteps().getClientData(SUBCLIENT1).getClientID());
        subclient2ClientId = Long.parseLong(api.userSteps.clientFakeSteps().getClientData(SUBCLIENT2).getClientID());
        subclientFreeClientId =
                Long.parseLong(api.userSteps.clientFakeSteps().getClientData(SUBCLIENT_FREE).getClientID());

        acceptedAt = Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        darkSideSteps.getDirectJooqDbSteps().useShard(shard).forceCurrencyConvertSteps()
                .deleteForceCurrencyConvert(agencyClientId);
        darkSideSteps.getDirectJooqDbSteps().forceCurrencyConvertSteps()
                .deleteForceCurrencyConvert(subclient1ClientId);
        darkSideSteps.getDirectJooqDbSteps().forceCurrencyConvertSteps()
                .deleteForceCurrencyConvert(subclient2ClientId);
        darkSideSteps.getDirectJooqDbSteps().forceCurrencyConvertSteps()
                .deleteForceCurrencyConvert(subclientFreeClientId);

        ForceCurrencyConvertRecord forceCurrencyConvertRecord = new ForceCurrencyConvertRecord()
                .setClientid(agencyClientId)
                .setAcceptedAt(acceptedAt);

        darkSideSteps.getDirectJooqDbSteps().forceCurrencyConvertSteps()
                .createForceCurrencyConvert(forceCurrencyConvertRecord);

        api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .clientsSteps().setClientsWorkCurrency(agencyClientId, ClientsWorkCurrency.YND_FIXED);
        api.userSteps.getDirectJooqDbSteps()
                .clientsSteps().setClientsWorkCurrency(subclient1ClientId, ClientsWorkCurrency.YND_FIXED);
        api.userSteps.getDirectJooqDbSteps()
                .clientsSteps().setClientsWorkCurrency(subclient2ClientId, ClientsWorkCurrency.YND_FIXED);
        api.userSteps.getDirectJooqDbSteps()
                .clientsSteps().setClientsWorkCurrency(subclientFreeClientId, ClientsWorkCurrency.YND_FIXED);
        api.userSteps.getDirectJooqDbSteps()
                .clientsSteps().setFree(subclientFreeClientId, ClientsAllowCreateScampBySubclient.Yes);

        darkSideSteps.getRunScriptSteps().runPpcCopySubclientsForceCurrency(AGENCY);
    }

    @Test
    public void checkForceCurrencyConvertForSubclient1() {
        ForceCurrencyConvertRecord forceCurrencyConvertRecord =
                darkSideSteps.getDirectJooqDbSteps().forceCurrencyConvertSteps()
                        .getForceCurrencyConvert(subclient1ClientId);
        ForceCurrencyConvertRecord expectedForceCurrencyConvertRecord = new ForceCurrencyConvertRecord()
                .setClientid(subclient1ClientId)
                .setAcceptedAt(acceptedAt);
        assertThat("субклиент попал в ppc.force_currency_convert",
                forceCurrencyConvertRecord, recordDiffer(expectedForceCurrencyConvertRecord));
    }

    @Test
    public void checkForceCurrencyConvertForSubclient2() {
        ForceCurrencyConvertRecord forceCurrencyConvertRecord =
                darkSideSteps.getDirectJooqDbSteps().forceCurrencyConvertSteps()
                        .getForceCurrencyConvert(subclient2ClientId);
        ForceCurrencyConvertRecord expectedForceCurrencyConvertRecord = new ForceCurrencyConvertRecord()
                .setClientid(subclient2ClientId)
                .setAcceptedAt(acceptedAt);
        assertThat("субклиент попал в ppc.force_currency_convert",
                forceCurrencyConvertRecord, recordDiffer(expectedForceCurrencyConvertRecord));
    }

    @Test
    public void checkForceCurrencyConvertForSubclientFree() {
        ForceCurrencyConvertRecord forceCurrencyConvertRecord =
                darkSideSteps.getDirectJooqDbSteps().forceCurrencyConvertSteps()
                        .getForceCurrencyConvert(subclientFreeClientId);
        assertThat("субклиент со свободой не попал в ppc.force_currency_convert", forceCurrencyConvertRecord, nullValue());
    }
}
