package ru.yandex.autotests.mediaplan.adgroups.delete;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.adgroups.AdGroupsPositiveBaseTest;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_changes_check_verbose.Api5ChangesCheckVerboseResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_adgroups.ParamsApi5DeleteAdgroups;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_adgroups.SelectionCriteria;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.ADGROUPS_DELETE;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;


@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADGROUPS_DELETE)
@Issue("https://st.yandex-team.ru/MEDIAPLAN-124")
@Description("Smoke тест для метода Delete сервиса Mediaplans.AdGroups.")
@Tag(MasterTags.MASTER)
@RunWith(Parameterized.class)
public class DeleteAdGroupsPositiveTest extends AdGroupsPositiveBaseTest {
    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule().withAddAdGroupsInputData(addAdGroupsInputData);


    @Test
    @Title("Удаляем группы")
    public void deleteAdGroup() {
        adgroupRule.getUserSteps().adGroupsSteps().api5AdGroupsDelete(
                new ParamsApi5DeleteAdgroups().withMediaplanId(adgroupRule.getMediaplanId())
                        .withClientId(getClient())
                        .withTimestamp(adgroupRule.getLastUpdateTimestamp())
                        .withSelectionCriteria(new SelectionCriteria().withIds(adgroupRule.getAdGroupIds()))
        );
        Api5ChangesCheckVerboseResult changes = adgroupRule.getUserSteps().changesSteps().adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        assumeThat("сохраненных групп 0", changes.getModified().getAdGroups(), hasSize(0));
        assertThat("Удаленные групы те же", adgroupRule.getAdGroupIds(), equalTo(changes.getNotFound().getAdGroupIds()));
    }
}
