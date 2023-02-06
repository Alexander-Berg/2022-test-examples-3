package ru.yandex.autotests.mediaplan.adgroups.update;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_changes_check_verbose.Api5ChangesCheckVerboseResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_adgroups.ParamsApi5DeleteAdgroups;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_adgroups.SelectionCriteria;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_update_adgroups.Api5UpdateAdgroupsResult;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.ADGROUPS_UPDATE;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.UpdateAdGroupsFactory.makeOneFieldChanged;
import static ru.yandex.autotests.mediaplan.datafactories.UpdateAdGroupsFactory.makeOnlyOneFieldChanged;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADGROUPS_UPDATE)
@Tag(MasterTags.MASTER)
@Description("Обновление несуществующих групп")
public class UpdateAdGroupsNegativeTest {
    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup());

    @Test
    @Description("Обновление удаленной группы")
    public void updateDeletedAdGroup() {
        Api5ChangesCheckVerboseResult changes = adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        adgroupRule.getUserSteps().adGroupsSteps().api5AdGroupsDelete(
                new ParamsApi5DeleteAdgroups().withMediaplanId(adgroupRule.getMediaplanId())
                        .withClientId(getClient())
                        .withTimestamp(adgroupRule.getLastUpdateTimestamp())
                        .withSelectionCriteria(new SelectionCriteria().withIds(adgroupRule.getAdGroupIds()))
        );

        makeOneFieldChanged(changes.getModified().getAdGroups()).getAdGroups();
        Api5UpdateAdgroupsResult updateAdgroupsResult = adgroupRule.getUserSteps().adGroupsSteps().api5AdGroupsUpdate(
                makeOnlyOneFieldChanged(changes.getModified().getAdGroups()).withMediaplanId(adgroupRule.getMediaplanId()).withClientId(getClient())
                        .withTimestamp(adgroupRule.getLastUpdateTimestamp()));
        adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        assertThat("Обновленные группы соответсвуют ожиданиям", updateAdgroupsResult.getUpdateResults().get(0).getErrors(),
                not(hasSize(0))
        );
    }

    @Test
    @Description("Обновление несуществующей группы")
    public void updateUnExistsAdGroup() {
        Api5ChangesCheckVerboseResult changes = adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        changes.getModified().getAdGroups().get(0).withId(12312312l);
        makeOneFieldChanged(changes.getModified().getAdGroups()).getAdGroups();
        Api5UpdateAdgroupsResult updateAdgroupsResult = adgroupRule.getUserSteps().adGroupsSteps().api5AdGroupsUpdate(
                makeOnlyOneFieldChanged(changes.getModified().getAdGroups()).withMediaplanId(adgroupRule.getMediaplanId()).withClientId(getClient())
                        .withTimestamp(adgroupRule.getLastUpdateTimestamp()));
        adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        assertThat("Обновленные группы соответсвуют ожиданиям", updateAdgroupsResult.getUpdateResults().get(0).getErrors(),
                not(hasSize(0))
        );
    }
}
