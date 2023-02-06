package ru.yandex.autotests.direct.cmd.groups.mobile;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.interest.TargetInterests;
import ru.yandex.autotests.direct.cmd.data.interest.TargetInterestsFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.retargeting.RetargetingHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.retargetings.RetargetingsErrorsResource;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Негативная проверка несохранения интересов в мобильных-группах")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.GROUP)
@Tag(ObjectTag.TARGET_INTERESTS)
@Tag(CampTypeTag.MOBILE)
@Tag(TrunkTag.YES)
@Tag(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Tag(TestFeatures.GROUPS)
public class SaveMobileTextAdGroupWithInterestNegativeTest {
    protected static String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private MobileBannersRule bannersRule = new MobileBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private Long categoryId;

    @Before
    public void before() {
        categoryId = RetargetingHelper.getRandomTargetCategoryId();
    }

    @Test
    @Description("сохраняем группу с 2 интересами с одним id")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10650")
    public void addToGroupTwoInterests() {
        Group group = bannersRule.getGroup();
        group.setCampaignID(bannersRule.getCampaignId().toString());
        group.getBanners().stream().forEach(b -> {
            b.withAdType("text");
            b.withCid(bannersRule.getCampaignId());
        });

        List<TargetInterests> interests = new ArrayList<>();
        interests.add(TargetInterestsFactory.defaultTargetInterest(categoryId));

        interests.add(TargetInterestsFactory.defaultTargetInterest(categoryId));

        group.setTargetInterests(interests);

        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        ErrorResponse error = cmdRule.cmdSteps().groupsSteps().postSaveMobileAdGroupsInvalidData(groupRequest);


        assertThat("Ошибка соотвествует ожиданиям", error.getError(),
                equalTo(RetargetingsErrorsResource.INTEREST_CATEGORY_MUST_BE_NOT_UNIQUE.toString())
        );
    }

    //этот тест должен заработать чуть позднее
    @Test
    @Ignore
    @Description("удаляем фразу и ретаргетинг")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10651")
    public void deletePhaseAndRetargeting() {
        Group group = bannersRule.getCurrentGroup();
        group.setPhrases(new ArrayList<>());
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(group, bannersRule.getMediaType());
        GroupsParameters firstGroupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);

        bannersRule.saveGroup(firstGroupRequest);
        group.setTargetInterests(new ArrayList<>());
        GroupsParameters secondGroupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        ErrorResponse error = cmdRule.cmdSteps().groupsSteps().postSaveMobileAdGroupsInvalidData(secondGroupRequest);
        assertThat("Ошибка соотвествует ожиданиям", error.getError(),
                equalTo(RetargetingsErrorsResource.AT_LEAST_ONE_CONDITION_MUST_BE.toString()));

    }
}
