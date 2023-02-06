package ru.yandex.autotests.direct.cmd.rights;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@Aqua.Test
@Description("Невозможность изменения визитки другому пользователю")
@Stories(TestFeatures.Rights.SAVE_VCARD_RIGHTS)
@Features(TestFeatures.RIGHTS)
@Tag(CmdTag.SAVE_VCARD)
@Tag(ObjectTag.VCARD)
@Tag(CampTypeTag.TEXT)
public class SaveVCardRightsTest {

    private static final String CLIENT = "at-direct-backend-c";
    private static final String CRIMINAL = "at-direct-cmd-csrf";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();


    @Rule
    public DirectCmdRule cmdRule;


    private TextBannersRule clientBannersRule;
    private TextBannersRule criminalBannersRule;

    private User client;
    private User criminal;


    public SaveVCardRightsTest() {
        client = User.get(CLIENT);
        criminal = User.get(CRIMINAL);

        ContactInfo clientVCard = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class);
        ContactInfo criminalVCard = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class);
        criminalVCard.
                withCompanyName("Рога и копыта Inc").
                withCityCode("921").
                withPhone("5958172");
        clientBannersRule = new TextBannersRule().
                overrideVCardTemplate(clientVCard).
                withUlogin(CLIENT);
        criminalBannersRule = new TextBannersRule().
                overrideVCardTemplate(criminalVCard).
                withUlogin(CRIMINAL);
        cmdRule = DirectCmdRule.defaultRule().withRules(clientBannersRule, criminalBannersRule);
    }


    @Before
    public void before() {
    }

    @Test
    @Description("Невозможность изменения визитки другому пользователю")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9991")
    public void othersVcardSaveRights() {
        Group criminalGroup = getCriminalGroup();
        long criminalVCardId = criminalGroup.getBanners().get(0).getVcardId();

        Group clientGroupBefore = getClientGroup();
        long clientVCardIdBefore = clientGroupBefore.getBanners().get(0).getVcardId();
        ContactInfo clientVCardBefore = cmdRule.cmdSteps().vCardsSteps().getVCard(client.getLogin(),
                clientBannersRule.getCampaignId(), clientBannersRule.getBannerId(), clientVCardIdBefore);

        ContactInfo clientVcardUpdate = clientVCardBefore.clone().
                withCompanyName("Рога и копыта Inc").
                withCityCode("921").
                withPhone("5958172");

        cmdRule.cmdSteps().authSteps().authenticate(criminal);
        cmdRule.cmdSteps().vCardsSteps().saveVCard(
                CRIMINAL,
                criminalBannersRule.getCampaignId(),
                clientBannersRule.getBannerId(),
                criminalVCardId,
                clientVcardUpdate);


        cmdRule.cmdSteps().authSteps().authenticate(client);
        Group clientGroupAfter = getClientGroup();
        long clientVCardIdAfter = clientGroupAfter.getBanners().get(0).getVcardId();
        ContactInfo clientVCardAfter = cmdRule.cmdSteps().vCardsSteps().getVCard(client.getLogin(),
                clientBannersRule.getCampaignId(), clientBannersRule.getBannerId(), clientVCardIdAfter);

        assertThat("после попытки модификации визитки другим клиентом id визитки не изменился " +
                "(визитка не была модифицирована)", clientVCardIdAfter, equalTo(clientVCardIdBefore));
        assertThat("после попытки модификации визитки другим клиентом визитка не изменилась",
                clientVCardAfter, BeanDifferMatcher.beanDiffer(clientVCardBefore));
    }

    private Group getGroup(String login, long campaignId, long groupId, long bannerId) {
        ShowCampMultiEditRequest request = ShowCampMultiEditRequest.
                forSingleBanner(login, campaignId, groupId, bannerId);
        ShowCampMultiEditResponse response = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(request);
        return response.getCampaign().getGroups().get(0);
    }

    private Group getClientGroup() {
        return getGroup(CLIENT, clientBannersRule.getCampaignId(),
                clientBannersRule.getGroupId(), clientBannersRule.getBannerId());
    }

    private Group getCriminalGroup() {
        return getGroup(CRIMINAL, criminalBannersRule.getCampaignId(),
                criminalBannersRule.getGroupId(), criminalBannersRule.getBannerId());
    }
}
