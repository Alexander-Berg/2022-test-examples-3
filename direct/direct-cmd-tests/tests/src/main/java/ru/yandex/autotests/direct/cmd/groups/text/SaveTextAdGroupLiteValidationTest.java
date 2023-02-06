package ru.yandex.autotests.direct.cmd.groups.text;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.banners.BannerErrorsEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Troubleshooting:
 * Если тест падает - нужно проверить этот урл в таблице redirect_check_dict (она в базе ts:monitor)
 */
@Aqua.Test
@Description("Загрузка уточнений к баннерам через эксель: правильное сохранение порядка дополнений")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Tag(ObjectTag.GROUP)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CampTypeTag.TEXT)
@Feature(TestFeatures.GROUPS)
public class SaveTextAdGroupLiteValidationTest {
    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    private static final String WRONG_HREF = "dsfsdlgljsdfgjdfgjldfjg.com";
    @ClassRule
    public static DirectCmdRule directCmdClassRule = DirectCmdRule.defaultClassRule();

    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private Group expectedGroup;

    @Before
    public void before() {
        setExpectedGroup();
    }

    @Test
    @Description("Проверка ошибки при редактировании текстов объявлений с неверной ссылкой")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9855")
    public void bannerWithInvalidHrefTest() {
        expectedGroup.getBanners().get(0).setHref(WRONG_HREF);
        check(BannerErrorsEnum.UNAVAILABLE_URL.toString());
    }

    private void check(String errorText) {
        ErrorResponse errorResponse = cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroupsInvalidData(
                GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), expectedGroup).withIsLight("1"));
        assertThat("ошибка соотвествует ожидаемой", errorResponse.getError(), equalTo(errorText));
    }

    private void setExpectedGroup() {
        expectedGroup = bannersRule.getGroup()
                .withAdGroupID(String.valueOf(bannersRule.getGroupId()))
                .withRetargetings(null)
                .withMinusWords(null)
                .withPhrases(null);
        expectedGroup.getBanners().get(0).withBid(bannersRule.getBannerId());
    }
}
