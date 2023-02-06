package ru.yandex.autotests.directintapi.tests.metrica.newtransport.exportrightsuidtocid;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.ExportRightsUidToCidRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.tsv.ExportRightsUidToCidResponse;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.model.Uids;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 16.11.15.
 * https://st.yandex-team.ru/TESTIRT-7726
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.EXPORT_RIGHTS_UID_TO_CID)
@Description("Проверка ручки ExportRightsUidToCid при запросе с несколькими cid'ами")
@Issue("https://st.yandex-team.ru/DIRECT-47326")
public class ExportRightsUidToSeveralCidsTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    Long cid;
    Long cidElse;

    @Step("Подготовка данных для теста")
    @Before()
    public void preapareData() {
        api.as(Logins.MANAGER_DEFAULT);
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign(Logins.LOGIN_FOR_CHF);
        api.userSteps.campaignFakeSteps().setRandomOrderID(cid.intValue());
        cidElse = api.userSteps.campaignSteps().addDefaultTextCampaign(Logins.LOGIN_FOR_EUR);
        api.userSteps.campaignFakeSteps().setRandomOrderID(cidElse.intValue());
    }

    @Test
    public void getUidsByCids() {
        ExportRightsUidToCidResponse response = api.userSteps.getDarkSideSteps().getExportRightsUidToCidSteps()
                .exportRightsUidToCid(new ExportRightsUidToCidRequest().withCids(cid, cidElse));
        // Ожидаем в ответе всех менеджеров из дефолтной idm-группы (idm_group_id=1000)
        List<ExportRightsUidToCidResponse.UidCidPair> expectedPairs =
                Arrays.asList(
                        new ExportRightsUidToCidResponse.UidCidPair(Uids.MANAGER_UID, cid),
                        new ExportRightsUidToCidResponse.UidCidPair(Uids.MANAGER_UID, cidElse),
                        new ExportRightsUidToCidResponse.UidCidPair(Uids.TEAMLEADER_UID, cid),
                        new ExportRightsUidToCidResponse.UidCidPair(Uids.TEAMLEADER_UID, cidElse),
                        new ExportRightsUidToCidResponse.UidCidPair(Uids.SUPER_TEAMLEADER_UID, cid),
                        new ExportRightsUidToCidResponse.UidCidPair(Uids.SUPER_TEAMLEADER_UID, cidElse),
                        new ExportRightsUidToCidResponse.UidCidPair(Uids.TRANSFER_MANAGER_UID, cid),
                        new ExportRightsUidToCidResponse.UidCidPair(Uids.TRANSFER_MANAGER_UID, cidElse)
                );
        Comparator<ExportRightsUidToCidResponse.UidCidPair> comparator =
                (pair1, pair2) -> {
                    int compareByUid = pair1.getUid().compareTo(pair2.getUid());
                    if (compareByUid != 0) {
                        return compareByUid;
                    }
                    return pair1.getCid().compareTo(pair2.getCid());
                };
        List<ExportRightsUidToCidResponse.UidCidPair> actualPairs = response.getUidCidPairs();
        actualPairs.sort(comparator);
        expectedPairs.sort(comparator);
        assertThat("вернулись правильные пары uid - cid", actualPairs, beanDiffer(expectedPairs));
    }
}
