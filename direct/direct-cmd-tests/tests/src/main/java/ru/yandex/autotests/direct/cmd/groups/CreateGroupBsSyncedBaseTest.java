package ru.yandex.autotests.direct.cmd.groups;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.DemographyCondition;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.DemographyMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.RetargetingMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.FeaturesTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.data.campaigns.adjustment.DemographyAgeEnum;
import ru.yandex.autotests.direct.httpclient.data.campaigns.adjustment.DemographyGenderEnum;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.direct.cmd.images.mobile.SaveMobileAdGroupsEditingImageTest.CLIENT;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

//Task: TESTIRT-9418.
@Tag(FeaturesTag.BS_SYNCED)
public abstract class CreateGroupBsSyncedBaseTest {
    protected final static String VALID_MULTIPLIER = "120";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected BannersRule bannersRule;

    @Before
    public void before() {
        BsSyncedHelper.moderateCamp(cmdRule, bannersRule.getCampaignId());
        BsSyncedHelper.syncCamp(cmdRule, bannersRule.getCampaignId());

        assumeThat(
                "статус синхронизации соответствует ожиданиям",
                getCurrentGroupStatusBsSynced(bannersRule.getGroupId()),
                equalTo(StatusBsSynced.YES.toString())
        );
    }

    @Description("изменение геотаргетинга, проверка статуса синхронизации группы")
    public void changeGeoTargetingCheckGroupStatus() {
        Group group = bannersRule.getCurrentGroup().withGeo(Geo.GERMANY.getGeo());
        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }


    @Description("Сохранение без изменения группы проверка статуса синхронизации баннеров")
    public void changeNothingCheckBannerStatus() {
        Group group = bannersRule.getCurrentGroup();

        saveAndCheckGroup(group, equalTo(StatusBsSynced.YES.toString()));
    }

    @Description("изменение минус-слов на группу")
    public void changeMinusWordsCheckGroup() {
        Group group =
                bannersRule.getCurrentGroup().withMinusWords(singletonList(RandomStringUtils.randomAlphabetic(20)));

        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }


    @Description("Изменение демографических коэффициентов на группу")
    public void changeDemographKOnGroup() {
        Group group = bannersRule.getCurrentGroup()
                .withAdGroupID(String.valueOf(bannersRule.getGroupId()));
        group.setHierarchicalMultipliers(new HierarchicalMultipliers()
                .withDemographyMultiplier(new DemographyMultiplier()
                        .withEnabled(0)
                        .withConditions(getDemographyConditions())
                )
        );
        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }

    @Description("Изменение ретаргетинговых коэффициентов на группу")
    public void changeRetargetingKOnGroup() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));
        cmdRule.apiSteps().retargetingSteps().addConditionsForUser(CLIENT, 1);
        Group group = bannersRule.getCurrentGroup()
                .withAdGroupID(String.valueOf(bannersRule.getGroupId()));
        group.setHierarchicalMultipliers(new HierarchicalMultipliers()
                .withRetargetingMultiplier(RetargetingMultiplier.
                        getDefaultRetargetingMultiplier(
                                String.valueOf(
                                        cmdRule.apiSteps().retargetingSteps().getRetargetingConditions(CLIENT)[0]),
                                "100")
                )
        );

        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }


    protected String getCurrentGroupStatusBsSynced(Long adGroupId) {
        return TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps().getPhrases(adGroupId)
                .getStatusbssynced().getLiteral();
    }

    protected String getCurrentBannerStatusBsSynced(Long bannerId) {
        return TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps().getBanner(bannerId)
                .getStatusbssynced().getLiteral();
    }

    protected void saveAndCheckGroup(Group group, Matcher matcher) {
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(group, bannersRule.getMediaType());
        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        bannersRule.saveGroup(groupRequest);
        assertThat(
                "статус синхронизации соответcтвует ожиданиям",
                getCurrentGroupStatusBsSynced(bannersRule.getGroupId()),
                matcher
        );
    }

    protected void saveAndCheckBanner(Group group, Matcher matcher) {
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(group, bannersRule.getMediaType());
        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        bannersRule.saveGroup(groupRequest);
        assertThat(
                "статус синхронизации соответcтвует ожиданиям",
                getCurrentBannerStatusBsSynced(bannersRule.getBannerId()),
                matcher
        );
    }


    private List<DemographyCondition> getDemographyConditions() {
        List<DemographyCondition> demographyConditionList = new ArrayList<>();
        demographyConditionList.add(new DemographyCondition()
                .withGender(DemographyGenderEnum.FEMALE.getKey())
                .withAge(DemographyAgeEnum.BETWEEN_0_AND_17.getKey())
                .withMultiplierPct(VALID_MULTIPLIER));
        return demographyConditionList;
    }

    protected abstract String getClient();
}
