package ru.yandex.autotests.mediaplan.adgroups.delete;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_adgroups.Api5DeleteAdgroupsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_adgroups.ParamsApi5DeleteAdgroups;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_adgroups.SelectionCriteria;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.rules.MediaplanRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.ADGROUPS_DELETE;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdsFactory.oneAd;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADGROUPS_DELETE)
@Description("Удаление групп с необязательными полям -- баннеры")
@Tag(MasterTags.MASTER)
@RunWith(Parameterized.class)
public class DeleteAdGroupsWithOptionalAdsNegativeTest extends DeleteAdGroupsOptianalBaseTest {

    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule().withAddAdGroupsInputData(addAdGroupsInputData).withAd(oneAd());

    @Test
    @Title("Удаляем несуществующую никогда группу")
    public void deleteNonExistentAdGroup() {
        Api5DeleteAdgroupsResult deleteAdGroup = adgroupRule.getUserSteps().adGroupsSteps().api5AdGroupsDelete(
                new ParamsApi5DeleteAdgroups().withMediaplanId(adgroupRule.getMediaplanId())
                        .withClientId(MediaplanRule.getClient())
                        .withTimestamp(adgroupRule.getLastUpdateTimestamp())
                        .withSelectionCriteria(new SelectionCriteria().withIds(Collections.singletonList(12312312l)))
        );
        assertThat("удаляем группу с баннерами", deleteAdGroup.getDeleteResults().get(0).getErrors(),
                not(hasSize(0)));
    }

}
