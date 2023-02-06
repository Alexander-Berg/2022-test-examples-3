package ru.yandex.autotests.mediaplan.adgroups.add;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.adgroups.AdGroupsPositiveBaseTest;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_adgroups.AddResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_adgroups.Api5AddAdgroupsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_changes_check_verbose.Api5ChangesCheckVerboseResult;
import ru.yandex.autotests.mediaplan.rules.MediaplanRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.ADGROUPS_ADD;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADGROUPS_ADD)
@Description("Создание групп баннеров для медиаплана")
@RunWith(Parameterized.class)
@Tag(MasterTags.MASTER)
public class AddAdGroupsPositiveTest extends AdGroupsPositiveBaseTest {

    @Rule
    public MediaplanRule mediaplanRule = new MediaplanRule();

    @Test
    @Title("Добавление группы")
    public void addAdGroup() {
        Long timestamp = mediaplanRule.getLastUpdateTimestamp();
        Api5AddAdgroupsResult adGroups = mediaplanRule.getUserSteps().adGroupsSteps().api5AdGroupsAdd(
                addAdGroupsInputData
                        .withMediaplanId(mediaplanRule.getMediaplanId())
                        .withTimestamp(timestamp)
                        .withClientId(getClient())
        );
        List<Long> ids = adGroups.getAddResults().stream()
                .map(AddResult::getId)
                .collect(Collectors.toList());

        Api5ChangesCheckVerboseResult changes = mediaplanRule.getUserSteps().changesSteps().adGroupChanges(ids, getClient(), mediaplanRule.getMediaplanId());
        changes.getModified().withAdGroups(changes.getModified().getAdGroups().stream()
                .map(x -> !x.getNegativeKeywords().getItems().isEmpty() ? x.withId(null) : x.withNegativeKeywords(null).withId(null))
                .collect(Collectors.toList()));
        assertThat("Сохраненные группы соотвествуют ожиданиям", changes.getModified().getAdGroups(), is(addAdGroupsInputData.getAdGroups()));
    }

}
