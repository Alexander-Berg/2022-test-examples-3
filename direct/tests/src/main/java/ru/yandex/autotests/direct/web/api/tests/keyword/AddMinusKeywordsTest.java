package ru.yandex.autotests.direct.web.api.tests.keyword;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.web.api.core.DirectRule;
import ru.yandex.autotests.direct.web.api.features.TestFeatures;
import ru.yandex.autotests.direct.web.api.features.tags.Tags;
import ru.yandex.autotests.direct.web.api.models.AddAdGroupMinusKeywordsRequestItem;
import ru.yandex.autotests.direct.web.api.models.AddCampaignMinusKeywordsRequestItem;
import ru.yandex.autotests.direct.web.api.models.AddMinusKeywordsRequest;
import ru.yandex.autotests.direct.web.api.models.WebSuccessResponse;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Добавление минус-фраз на кампанию и группу объявлений")
@Stories(TestFeatures.MinusKeyword.ADD)
@Features(TestFeatures.MINUS_KEYWORD)
@Tag(TrunkTag.YES)
@Tag(Tags.MINUS_KEYWORD)
@Tag(Tags.CAMPAIGN)
@Tag(Tags.AD_GROUP)
public class AddMinusKeywordsTest {

    private static final String CLIENT_LOGIN = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    private TextBannersRule textBannersRule = new TextBannersRule().withUlogin(CLIENT_LOGIN);

    private DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(textBannersRule)
            .as(CLIENT_LOGIN);
    @Rule
    public DirectRule directRule = DirectRule.defaultRule()
            .withRules(cmdRule)
            .as(CLIENT_LOGIN);

    @Test
    public void addValidAdGroupMinusKeywordsAndExpectSuccessfulResponse() {
        AddMinusKeywordsRequest request = new AddMinusKeywordsRequest()
                .withAdGroupMinusKeywords(singletonList(createAddAdGroupMinusKeywordsItem(textBannersRule.getGroupId())))
                .withCampaignMinusKeywords(null);

        WebSuccessResponse response =
                directRule.webApiSteps().minusKeywordSteps().addMinusKeywords(request, CLIENT_LOGIN);

        assertThat("в ответе флаг success должен быть равен true", response.getSuccess(), is(true));
    }

    @Test
    public void addValidCampaignMinusKeywordsAndExpectSuccessfulResponse() {
        AddMinusKeywordsRequest request = new AddMinusKeywordsRequest()
                .withAdGroupMinusKeywords(null)
                .withCampaignMinusKeywords(singletonList(createAddCampaignMinusKeywordsItem(textBannersRule.getCampaignId())));

        WebSuccessResponse response =
                directRule.webApiSteps().minusKeywordSteps().addMinusKeywords(request, CLIENT_LOGIN);

        assertThat("в ответе флаг success должен быть равен true", response.getSuccess(), is(true));
    }

    @Test
    public void addValidAdGroupAndCampaignMinusKeywordsAndExpectSuccessfulResponse() {
        AddMinusKeywordsRequest request = new AddMinusKeywordsRequest()
                .withAdGroupMinusKeywords(singletonList(createAddAdGroupMinusKeywordsItem(textBannersRule.getGroupId())))
                .withCampaignMinusKeywords(singletonList(createAddCampaignMinusKeywordsItem(textBannersRule.getCampaignId())));

        WebSuccessResponse response =
                directRule.webApiSteps().minusKeywordSteps().addMinusKeywords(request, CLIENT_LOGIN);

        assertThat("в ответе флаг success должен быть равен true", response.getSuccess(), is(true));
    }

    @Test
    public void addInvalidMinusKeywordsAndExpectFailedResponse() {
        AddAdGroupMinusKeywordsRequestItem invalidItem = new AddAdGroupMinusKeywordsRequestItem()
                .withId(textBannersRule.getGroupId())
                .withMinusKeywords(asList(randomKeyword(), "**#^@*\""));

        AddMinusKeywordsRequest request = new AddMinusKeywordsRequest()
                .withAdGroupMinusKeywords(singletonList(invalidItem))
                .withCampaignMinusKeywords(singletonList(createAddCampaignMinusKeywordsItem(textBannersRule.getCampaignId())));

        WebSuccessResponse response =
                directRule.webApiSteps().minusKeywordSteps().addMinusKeywords(request, CLIENT_LOGIN);

        assertThat("в ответе флаг success должен быть равен false", response.getSuccess(), is(false));
    }

    private static AddAdGroupMinusKeywordsRequestItem createAddAdGroupMinusKeywordsItem(Long id) {
        return new AddAdGroupMinusKeywordsRequestItem()
                .withId(id)
                .withMinusKeywords(asList(randomKeyword(), randomKeyword()));
    }

    private static AddCampaignMinusKeywordsRequestItem createAddCampaignMinusKeywordsItem(Long id) {
        return new AddCampaignMinusKeywordsRequestItem()
                .withId(id)
                .withMinusKeywords(asList(randomKeyword(), randomKeyword()));
    }

    private static String randomKeyword() {
        return "минус-фраза " + RandomStringUtils.randomAlphanumeric(5);
    }
}
