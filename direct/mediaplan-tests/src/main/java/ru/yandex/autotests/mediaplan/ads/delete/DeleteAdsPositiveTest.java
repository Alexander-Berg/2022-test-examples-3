package ru.yandex.autotests.mediaplan.ads.delete;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_ads.ParamsApi5AddAds;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_ads.Api5DeleteAdsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_ads.ParamsApi5DeleteAds;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_ads.SelectionCriteria;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.ADS_DELETE;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdsFactory.*;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADS_DELETE)
@Description("Удаление баннеров для медиаплана")
@Tag(MasterTags.MASTER)
@RunWith(Parameterized.class)
public class DeleteAdsPositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Один баннер", oneAd()},
                {"Два баннера", twoAd()},
                {"50 объявлений", fiftyAd()},
        });
    }
    @Rule
    public AdgroupRule adgroupRule;
    private ParamsApi5AddAds paramsApi5AddAds;

    public DeleteAdsPositiveTest(String text, ParamsApi5AddAds paramsApi5AddAds) {
        this.paramsApi5AddAds = paramsApi5AddAds;
        adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup()).withAd(paramsApi5AddAds);
    }

    @Test
    public void deleteAds(){
        Api5DeleteAdsResult deleteResult = adgroupRule.getUserSteps().adsSteps().api5AdsDelete(new ParamsApi5DeleteAds()
                .withMediaplanId(adgroupRule.getMediaplanId())
                .withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp())
                .withSelectionCriteria(new SelectionCriteria().withAdGroupId(adgroupRule.getAdGroupId()).withIds(adgroupRule.getAdIds())));
        assertThat("", deleteResult.getDeleteResults(), hasSize(paramsApi5AddAds.getAds().size()));
    }
}
