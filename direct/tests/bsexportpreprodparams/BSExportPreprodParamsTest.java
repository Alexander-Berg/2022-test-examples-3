package ru.yandex.autotests.directintapi.tests.bsexportpreprodparams;

import java.util.Collections;

import com.google.gson.Gson;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsrxportpreprodparams.BSExportPreprodParamsResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsrxportpreprodparams.BSExportPreprodParamsService;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 03/02/16.
 * https://st.yandex-team.ru/TESTIRT-8416
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.BS_EXPORT_PREPROD_PARAMS)
@Issue("https://st.yandex-team.ru/DIRECT-49810")
public class BSExportPreprodParamsTest {
    public static final Integer CLIENTID_MOD = 1000;
    public static final Integer CLIENTID_REM = 0;


    private static ApiSteps api = new ApiSteps();
    private static DarkSideSteps darkSideSteps = api.userSteps.getDarkSideSteps();

    @Test
    public void testBsExportPreprodExceptionsDisabled() {
        BSExportPreprodParamsResponse expectedResponse =
                new BSExportPreprodParamsResponse().withClientIdRem(CLIENTID_REM)
                        .withClientIdMod(CLIENTID_MOD).withComment(null)
                        .withExcludedClientIdList(Collections.emptyList());

        BSExportPreprodParamsResponse response = darkSideSteps.getBsExportPreprodParamsSteps().get();
        AllureUtils.addJsonAttachment("Полученный ответ от ручки " + BSExportPreprodParamsService.SERVICE_NAME
                , new Gson().toJson(response));

        assumeThat("получен непустой ответ", response, notNullValue());
        assertThat("получен ожидаемый ответ", response, beanDiffer(expectedResponse)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }
}
