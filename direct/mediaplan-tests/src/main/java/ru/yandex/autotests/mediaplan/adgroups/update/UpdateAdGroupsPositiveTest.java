package ru.yandex.autotests.mediaplan.adgroups.update;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.adgroups.AdGroupsPositiveBaseTest;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_adgroups.AdGroup;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_changes_check_verbose.Api5ChangesCheckVerboseResult;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.ADGROUPS_UPDATE;
import static ru.yandex.autotests.mediaplan.datafactories.UpdateAdGroupsFactory.makeOneFieldChanged;
import static ru.yandex.autotests.mediaplan.datafactories.UpdateAdGroupsFactory.makeOnlyOneFieldChanged;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADGROUPS_UPDATE)
@Description("Smoke тест для метода Update сервиса Mediaplans.AdGroups.")
@Tag(MasterTags.MASTER)
@RunWith(Parameterized.class)
public class UpdateAdGroupsPositiveTest extends AdGroupsPositiveBaseTest {
    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule().withAddAdGroupsInputData(addAdGroupsInputData);

    @Test
    public void updateAdGroup() {
        Api5ChangesCheckVerboseResult changes = adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        List<AdGroup> expected = makeOneFieldChanged(changes.getModified().getAdGroups()).getAdGroups();
        adgroupRule.getUserSteps().adGroupsSteps().api5AdGroupsUpdate(
                makeOnlyOneFieldChanged(changes.getModified().getAdGroups()).withMediaplanId(adgroupRule.getMediaplanId()).withClientId(getClient())
                        .withTimestamp(adgroupRule.getLastUpdateTimestamp()));
        Api5ChangesCheckVerboseResult actualChanges = adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());
        assertThat("Обновленные группы соответсвуют ожиданиям", actualChanges.getModified().getAdGroups(),
                equalTo(expected)
        );
    }
}
