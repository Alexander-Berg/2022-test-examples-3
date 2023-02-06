package ru.yandex.autotests.direct.httpclient.campaigns.saveNewCamp;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.SaveCampParameters;
import ru.yandex.autotests.direct.httpclient.data.campaigns.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.httpclient.util.mappers.cmdToAnotherCmdMappers.SaveCampParametersToEditCampMapping;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.util.beanmapper.BeanMapper.map;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

@Aqua.Test
@Description("Проверка сохранения параметров контроллера saveNewCamp при создании новой кампании")
@Stories(TestFeatures.Campaigns.SAVE_NEW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@RunWith(Parameterized.class)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(TrunkTag.YES)
@Tag(OldTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SaveNewCampParametersTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    @Parameterized.Parameter(value = 0)
    public String templateName;
    @Parameterized.Parameter(value = 1)
    public String description;


    private User client = User.get("at-direct-backend-c");
    private Long campaignId;
    private SaveCampParameters saveCampParameters;
    private EditCampResponse expectedEditCampResponse;

    @Parameterized.Parameters(name = "Параметры кампании: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"defaultSaveCampParameters", "Кампания с настройками по-умолчанию"},
                {"SaveCampParametersWithContactInfo", "Кампания с Единой контактной информацией"},
                {"mobileSaveCampParameters", "Мобильная компания с настройками по-умолчанию"}
        });
    }

    @Before
    public void before() {


        cmdRule.oldSteps().onPassport().authoriseAs(client.getLogin(), client.getPassword());
        PropertyLoader<SaveCampParameters> propertyLoader = new PropertyLoader<>(SaveCampParameters.class);
        saveCampParameters = propertyLoader.getHttpBean(templateName);
        saveCampParameters.getJsonStartegy().setIsNetStop("0");
        expectedEditCampResponse = map(saveCampParameters, EditCampResponse.class,
                new SaveCampParametersToEditCampMapping());
    }

    @After
    public void deleteCampaign() {
        if (campaignId != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(client.getLogin(), campaignId);
        }
    }

    @Test
    @Description("Сохраняем кампанию и проверяем результат сохранения через ответ контроллера editCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10348")
    public void saveNewCampTest() {
        CSRFToken csrfToken = getCsrfTokenFromCocaine(client.getPassportUID());
        DirectResponse saveNewCampResponse = cmdRule.oldSteps().clientSteps().saveNewCampaign(csrfToken, saveCampParameters);
        campaignId = Long.valueOf(saveNewCampResponse.getParameterFromRedirect("cid"));
        EditCampResponse editCampResponse =
                cmdRule.oldSteps().onEditCamp().getEditCampResponse(String.valueOf(campaignId), client.getLogin());
        assertThat("Ответ контроллера совпадает с ожидаемым", editCampResponse,
                beanEquivalent(expectedEditCampResponse));
    }
}
