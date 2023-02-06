package ru.yandex.autotests.directintapi.tests.metrica.newtransport.exportrightsuidtocid;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.ExportRightsUidToCidRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.tsv.ExportRightsUidToCidResponse;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by buhter on 02.12.15.
 * https://st.yandex-team.ru/TESTIRT-7910
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.EXPORT_RIGHTS_UID_TO_CID)
@Description("Проверка ручки ExportRightsUidToCid при запросе с указанием минимального и/или максимального cid")
@Issue("https://st.yandex-team.ru/DIRECT-49030")
public class ExportRightsUidToCidsWithCidsLimitsTest {
    public static Long MAX_CID = 1000L;

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Test
    public void getUidsByCidsWithMaxCid() {
        api.as(Logins.MANAGER_DEFAULT);
        ExportRightsUidToCidResponse response = api.userSteps.getDarkSideSteps().getExportRightsUidToCidSteps()
                .exportRightsUidToCid(new ExportRightsUidToCidRequest().withMaxCid(MAX_CID));
        assumeThat("получен ответ", response, notNullValue());
        assumeThat("в полученном ответе есть записи", response.getUidCidPairs(), iterableWithSize(greaterThan(0)));
        response.getUidCidPairs().forEach(uidCidPair ->
                assertThat("cid меньше, чем заданный максимальный - " + MAX_CID, uidCidPair.getCid()
                        , lessThanOrEqualTo(MAX_CID)
                )
        );
    }

    @Test
    public void getUidsByCidsWithZeroMaxCid() {
        api.as(Logins.MANAGER_DEFAULT);
        ExportRightsUidToCidResponse response = api.userSteps.getDarkSideSteps().getExportRightsUidToCidSteps()
                .exportRightsUidToCid(new ExportRightsUidToCidRequest().withMaxCid(0L));
        assumeThat("получен ответ", response, notNullValue());
        assertThat("в полученном ответе нет записей", response.getUidCidPairs(), iterableWithSize(equalTo(0)));
    }

    @Test
    public void getUidsByCidsWithNegativeMaxCid() {
        api.as(Logins.MANAGER_DEFAULT);
        ExportRightsUidToCidResponse response = api.userSteps.getDarkSideSteps().getExportRightsUidToCidSteps()
                .exportRightsUidToCid(new ExportRightsUidToCidRequest().withMaxCid(-1L));
        assumeThat("получен ответ", response, notNullValue());
        assertThat("в полученном ответе нет записей", response.getUidCidPairs(), iterableWithSize(equalTo(0)));
    }

    @Test
    public void getUidsByCidsWithMinCid() {
        api.as(Logins.MANAGER_DEFAULT);
        Long minCid = api.userSteps.campaignSteps().addDefaultTextCampaign(Logins.CLIENT_FREE_YE_DEFAULT);
        Long anotherCid = api.userSteps.campaignSteps().addDefaultTextCampaign(Logins.CLIENT_FREE_YE_DEFAULT);
        api.userSteps.campaignFakeSteps().setRandomOrderID(minCid.intValue());
        api.userSteps.campaignFakeSteps().setRandomOrderID(anotherCid.intValue());
        ExportRightsUidToCidResponse response = api.userSteps.getDarkSideSteps().getExportRightsUidToCidSteps()
                .exportRightsUidToCid(new ExportRightsUidToCidRequest().withMinCid(minCid));
        assumeThat("получен ответ", response, notNullValue());
        assumeThat("в полученном ответе есть записи", response.getUidCidPairs(), iterableWithSize(greaterThan(0)));
        response.getUidCidPairs().forEach(uidCidPair ->
                assertThat("cid больше, чем заданный минимальный - " + minCid, uidCidPair.getCid()
                        , greaterThanOrEqualTo(minCid)
                )
        );
    }

    @Test
    public void getUidsByCidsWithMinCidAndMaxCid() {
        Long minCid = MAX_CID - 100L;
        api.as(Logins.MANAGER_DEFAULT);
        ExportRightsUidToCidResponse response = api.userSteps.getDarkSideSteps().getExportRightsUidToCidSteps()
                .exportRightsUidToCid(new ExportRightsUidToCidRequest().withMaxCid(MAX_CID).withMinCid(minCid));
        assumeThat("получен ответ", response, notNullValue());
        assumeThat("в полученном ответе есть записи", response.getUidCidPairs(), iterableWithSize(greaterThan(0)));
        response.getUidCidPairs().forEach(uidCidPair ->
                assertThat("cid в промежутке между " + minCid + " и " + MAX_CID, uidCidPair.getCid()
                        , allOf(greaterThanOrEqualTo(minCid), lessThanOrEqualTo(MAX_CID))
                )
        );
    }

    @Test
    public void getUidsByCidsWithNegativeMinCidAndMaxCid() {
        Long minCid = -100L;
        api.as(Logins.MANAGER_DEFAULT);
        ExportRightsUidToCidResponse response = api.userSteps.getDarkSideSteps().getExportRightsUidToCidSteps()
                .exportRightsUidToCid(new ExportRightsUidToCidRequest().withMaxCid(MAX_CID).withMinCid(minCid));
        assumeThat("получен ответ", response, notNullValue());
        assumeThat("в полученном ответе есть записи", response.getUidCidPairs(), iterableWithSize(greaterThan(0)));
        response.getUidCidPairs().forEach(uidCidPair ->
                assertThat("cid в промежутке между " + minCid + " и " + MAX_CID, uidCidPair.getCid()
                        , allOf(greaterThanOrEqualTo(minCid), lessThanOrEqualTo(MAX_CID))
                )
        );
    }

    @Test
    public void getUidsByCidsWithUnexistingMinCid() {
        api.as(Logins.MANAGER_DEFAULT);
        Long minCid = api.userSteps.campaignSteps().addDefaultTextCampaign(Logins.CLIENT_FREE_YE_DEFAULT)
                + 100000L;
        ExportRightsUidToCidResponse response = api.userSteps.getDarkSideSteps().getExportRightsUidToCidSteps()
                .exportRightsUidToCid(new ExportRightsUidToCidRequest().withMaxCid(MAX_CID).withMinCid(minCid));
        assumeThat("получен ответ", response, notNullValue());
        assertThat("в полученном ответе нет записей", response.getUidCidPairs(), iterableWithSize(equalTo(0)));
    }
}
