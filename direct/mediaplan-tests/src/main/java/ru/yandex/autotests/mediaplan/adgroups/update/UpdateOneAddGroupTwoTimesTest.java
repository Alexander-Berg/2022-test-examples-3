package ru.yandex.autotests.mediaplan.adgroups.update;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_adgroups.AdGroup;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_adgroups.NegativeKeywords;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_changes_check_verbose.Api5ChangesCheckVerboseResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_update_adgroups.ParamsApi5UpdateAdgroups;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.ADGROUPS_UPDATE;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.UpdateAdGroupsFactory.makeOneRequiredFieldNullChanged;
import static ru.yandex.autotests.mediaplan.datafactories.UpdateAdGroupsFactory.makeOnlyOneRequiredFieldNullChanged;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@Tag(MasterTags.MASTER)
@ru.yandex.qatools.allure.annotations.Features(ADGROUPS_UPDATE)
@Description("Обновление 2 полей одной группы")
public class UpdateOneAddGroupTwoTimesTest {

    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup());

    @Test
    public void updateTwoFieldsAdGroup() {
        Api5ChangesCheckVerboseResult changes = adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        List<AdGroup> adgroupList = new ArrayList<>();
        adgroupList.add(new AdGroup()
                .withId(adgroupRule.getAdGroupId())
                .withName("new_name")
                .withRegionIds(null)
        );
        adgroupList.add(
                new AdGroup()
                        .withId(adgroupRule.getAdGroupId())
                        .withNegativeKeywords(new NegativeKeywords().withItems(Collections.singletonList("asd")))
        );
        AdGroup expectedAdGroup = new AdGroup()
                .withId(adgroupRule.getAdGroupId())
                .withName("new_name")
                .withRegionIds(changes.getModified().getAdGroups().get(0).getRegionIds())
                .withNegativeKeywords(changes.getModified().getAdGroups().get(0).getNegativeKeywords());

        adgroupRule.getUserSteps().adGroupsSteps().api5AdGroupsUpdate(
                new ParamsApi5UpdateAdgroups()
                        .withAdGroups(adgroupList)
                        .withMediaplanId(adgroupRule.getMediaplanId())
                        .withClientId(getClient())
                        .withTimestamp(adgroupRule.getLastUpdateTimestamp()));
        Api5ChangesCheckVerboseResult actualChanges = adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        assertThat("Обновленные группы соответсвуют ожиданиям", actualChanges.getModified().getAdGroups().get(0),
                equalTo(expectedAdGroup)
        );
    }
//негативный
    @Test
    public void updateAdGroupToNull() {
        Api5ChangesCheckVerboseResult changes = adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        List<AdGroup> expected = makeOneRequiredFieldNullChanged(changes.getModified().getAdGroups()).getAdGroups();
        adgroupRule.getUserSteps().adGroupsSteps().api5AdGroupsUpdate(
                makeOnlyOneRequiredFieldNullChanged(changes.getModified().getAdGroups())
                        .withMediaplanId(adgroupRule.getMediaplanId()).withClientId(getClient())
                        .withTimestamp(adgroupRule.getLastUpdateTimestamp()));
        Api5ChangesCheckVerboseResult actualChanges = adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        assertThat("Обновленные группы соответсвуют ожиданиям", actualChanges.getModified().getAdGroups(),
                equalTo(expected)
        );
    }
}
