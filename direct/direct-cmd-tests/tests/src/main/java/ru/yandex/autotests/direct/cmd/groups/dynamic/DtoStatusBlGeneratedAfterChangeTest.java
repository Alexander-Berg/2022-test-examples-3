package ru.yandex.autotests.direct.cmd.groups.dynamic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
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
import static ru.yandex.autotests.direct.utils.matchers.BeanEqualsAssert.assertThat;

/**
 * https://st.yandex-team.ru/TESTIRT-7909
 */
@Aqua.Test
@Description("Изменение статуса группы StatusBlGenerated при изменении группы")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class DtoStatusBlGeneratedAfterChangeTest extends DtoBaseTest {

    @Parameterized.Parameter
    public AdgroupsDynamicStatusblgenerated savedStatus;

    @Parameterized.Parameter(1)
    public AdgroupsDynamicStatusblgenerated expectedStatus;

    @Parameterized.Parameters(name = "Параметры : выставленный статус {0}, ожидаемый статус {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {AdgroupsDynamicStatusblgenerated.No, AdgroupsDynamicStatusblgenerated.Processing},
                {AdgroupsDynamicStatusblgenerated.Yes, AdgroupsDynamicStatusblgenerated.Yes},
                {AdgroupsDynamicStatusblgenerated.Processing, AdgroupsDynamicStatusblgenerated.Processing},
        });
    }

    @Test
    @Description("Изменение домена")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9784")
    public void changeDomain() {

        TestEnvironment.newDbSteps().useShard(shard).adgroupsDynamicSteps().setStatusBlGenerated(groupId, savedStatus);

        savingGroup.setMainDomain("vk.com");
        savingGroup.setAdGroupID(String.valueOf(groupId));
        savingGroup.getBanners().get(0).withBid(bannerId);
        AdgroupsDynamicRecord actualAdgroupDynamic = saveGroup(savingGroup);
        assertThat("Статус генeрции дто соответствует ожиданиям",
                actualAdgroupDynamic.getStatusblgenerated(),
                equalTo(expectedStatus));
    }

    @Test
    @Description("Изменение условий нацелевания")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9785")
    public void changeTargetCondition() {
        TestEnvironment.newDbSteps().useShard(shard).adgroupsDynamicSteps().setStatusBlGenerated(groupId, savedStatus);
        savingGroup.getDynamicConditions().get(0).setPrice(11.1f);

        savingGroup.setAdGroupID(String.valueOf(groupId));
        savingGroup.getBanners().get(0).withBid(bannerId);
        AdgroupsDynamicRecord actualAdgroupDynamic = saveGroup(savingGroup);
        assertThat("Статус генeрции дто соответствует ожиданиям",
                actualAdgroupDynamic.getStatusblgenerated(),
                equalTo(expectedStatus));
    }

    private AdgroupsDynamicRecord saveGroup(Group savingGroup) {
        groupRequest.setAdgroupIds(Collections.singletonList(groupId));
        groupRequest.setBids(Collections.singletonList(bannerId));
        groupRequest.setJsonGroups(new Gson().toJson(new Group[]{savingGroup}));

        cmdRule.cmdSteps().groupsSteps().postSaveDynamicAdGroups(groupRequest);

        return TestEnvironment.newDbSteps().useShard(shard).adgroupsDynamicSteps().getAdgroupsDynamic(groupId);
    }
}
