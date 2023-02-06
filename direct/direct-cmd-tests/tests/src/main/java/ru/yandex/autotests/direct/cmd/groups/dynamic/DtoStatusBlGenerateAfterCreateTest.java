package ru.yandex.autotests.direct.cmd.groups.dynamic;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
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
@Description("Проверка, что динамические баннеры при создании компании обладают статусом StatusBlGenerated = 'идет обработка'")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class DtoStatusBlGenerateAfterCreateTest extends DtoBaseTest {
    @Test
    @Description("Проверка статуса StatusBlGenerated сразу после создания")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9780")
    public void firstStatus() {
        AdgroupsDynamicRecord actualAdgroupDynamic = TestEnvironment.newDbSteps().useShard(shard)
                .adGroupsSteps().getAdgroupsDynamic(groupId);

        assertThat("Статус генерации дто не соотвествует ожиданиям",
                actualAdgroupDynamic.getStatusblgenerated(), equalTo(AdgroupsDynamicStatusblgenerated.Processing));
    }
}
