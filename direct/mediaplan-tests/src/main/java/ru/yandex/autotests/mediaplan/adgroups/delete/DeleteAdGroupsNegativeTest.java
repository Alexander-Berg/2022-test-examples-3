package ru.yandex.autotests.mediaplan.adgroups.delete;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_changes_check_verbose.Api5ChangesCheckVerboseResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_adgroups.Api5DeleteAdgroupsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_adgroups.ParamsApi5DeleteAdgroups;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_adgroups.SelectionCriteria;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.ADGROUPS_DELETE;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.twoSameAdgroups;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;
@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADGROUPS_DELETE)
@Description("Некорректное удаление групп")
@Tag(MasterTags.MASTER)
public class DeleteAdGroupsNegativeTest {
    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule().withAddAdGroupsInputData(twoSameAdgroups());

    @Test
    @Title("Удаляем удаленную группу")
    public void deleteDeletedAdGroup() {
        adgroupRule.getUserSteps().adGroupsSteps().api5AdGroupsDelete(
                new ParamsApi5DeleteAdgroups().withMediaplanId(adgroupRule.getMediaplanId())
                        .withClientId(getClient())
                        .withTimestamp(adgroupRule.getLastUpdateTimestamp())
                        .withSelectionCriteria(new SelectionCriteria().withIds(adgroupRule.getAdGroupIds()))
        );
        Api5ChangesCheckVerboseResult changes = adgroupRule.getUserSteps().changesSteps().adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        adgroupRule.setLastUpdateTimestamp(changes.getTimestamp());
        Api5DeleteAdgroupsResult deleteAdGroup = adgroupRule.getUserSteps().adGroupsSteps().api5AdGroupsDelete(
                new ParamsApi5DeleteAdgroups().withMediaplanId(adgroupRule.getMediaplanId())
                        .withClientId(getClient())
                        .withTimestamp(adgroupRule.getLastUpdateTimestamp())
                        .withSelectionCriteria(new SelectionCriteria().withIds(adgroupRule.getAdGroupIds()))
        );
        assertThat("Удаляем 2 удаленных группы", deleteAdGroup.getDeleteResults().get(0).getErrors(),
                not(hasSize(0)));
    }

    @Test
    @Title("Удаляем несуществующую никогда группу")
    public void deleteNonExistentAdGroup() {
        Api5DeleteAdgroupsResult deleteAdGroup = adgroupRule.getUserSteps().adGroupsSteps().api5AdGroupsDelete(
                new ParamsApi5DeleteAdgroups().withMediaplanId(adgroupRule.getMediaplanId())
                        .withClientId(getClient())
                        .withTimestamp(adgroupRule.getLastUpdateTimestamp())
                        .withSelectionCriteria(new SelectionCriteria().withIds(Collections.singletonList(123123l)))
        );
        assertThat("удаляем несуществующую группу", deleteAdGroup.getDeleteResults().get(0).getErrors(),
                not(hasSize(0)));
    }

    @Test
    @Title("Удаляем 10 тысяч групп")
    public void deleteTenThousandsAdgroups() {
        //пока не сделать
    }
}
