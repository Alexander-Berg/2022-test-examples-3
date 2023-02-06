package ru.yandex.autotests.direct.cmd.groups.dynamic;

import java.util.Collections;

import com.google.gson.Gson;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AdgroupsDynamicStatusblgenerated;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AdgroupsDynamicRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * https://st.yandex-team.ru/TESTIRT-7909
 */
@Aqua.Test
@Description("Изменение статуса группы StatusBlGenerated при изменении черновой группы")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
@Tag(TrunkTag.YES)
public class DtoStatusBlGenerateDraftTest extends DtoBaseTest {
    @Override
    protected void createGroupAndGetIds() {
        createGroup();
        getCreatedIds();
    }

    @Test
    @Description("Проверка статуса StatusBlGenerated сразу после создания")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9783")
    public void firstStatus() {
        AdgroupsDynamicRecord actualAdgroupDynamic =
                TestEnvironment.newDbSteps().useShard(shard).adgroupsDynamicSteps().getAdgroupsDynamic(groupId);

        assertThat("Статус генерации дто  соотвествует ожиданиям",
                actualAdgroupDynamic.getStatusblgenerated(), equalTo(AdgroupsDynamicStatusblgenerated.No));
    }

    @Test
    @Description("Проверка статуса StatusBlGenerated черновика после смены домена")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9781")
    public void changeDomain() {
        savingGroup.setMainDomain("vk.com");
        savingGroup.setAdGroupID(String.valueOf(groupId));
        savingGroup.getBanners().get(0).withBid(bannerId);

        AdgroupsDynamicRecord actualAdgroupDynamic = saveGroup(savingGroup);
        assertThat("Статус генeрции дто соответствует ожиданиям",
                actualAdgroupDynamic.getStatusblgenerated(),
                equalTo(AdgroupsDynamicStatusblgenerated.No));
    }

    @Test
    @Description("Проверка статуса StatusBlGenerated черновика после изменений условий нацелевания")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9782")
    public void changeTargetCondition() {
        savingGroup.getDynamicConditions().get(0).setPrice(11.1f);

        savingGroup.setAdGroupID(String.valueOf(groupId));
        savingGroup.getBanners().get(0).withBid(bannerId);

        AdgroupsDynamicRecord actualAdgroupDynamic = saveGroup(savingGroup);

        assertThat("Статус генeрции дто соответствует ожиданиям",
                actualAdgroupDynamic.getStatusblgenerated(),
                equalTo(AdgroupsDynamicStatusblgenerated.No));
    }

    private AdgroupsDynamicRecord saveGroup(Group savingGroup) {
        groupRequest.setAdgroupIds(Collections.singletonList(groupId));
        groupRequest.setBids(Collections.singletonList(bannerId));
        groupRequest.setJsonGroups(new Gson().toJson(new Group[]{savingGroup}));

        cmdRule.cmdSteps().groupsSteps().postSaveDynamicAdGroups(groupRequest);

        return TestEnvironment.newDbSteps().useShard(shard).adgroupsDynamicSteps().getAdgroupsDynamic(groupId);
    }
}
