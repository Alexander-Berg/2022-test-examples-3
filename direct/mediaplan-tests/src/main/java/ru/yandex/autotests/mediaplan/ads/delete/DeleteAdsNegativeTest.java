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
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.ADS_DELETE;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdsFactory.twoAd;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADS_DELETE)
@Description("Удаление несуществуюхих баннеров для медиаплана")
@Tag(MasterTags.MASTER)
@RunWith(Parameterized.class)
public class DeleteAdsNegativeTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Два баннера", twoAd()},
        });
    }
    @Rule
    public AdgroupRule adgroupRule;

    public DeleteAdsNegativeTest(String text, ParamsApi5AddAds paramsApi5AddAds) {
        adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup()).withAd(paramsApi5AddAds);
    }

    @Test
    public void delete2ExistsAdAndOneNotAds(){
        List<Long> adsIds = adgroupRule.getAdIds();
        adsIds.add(999999l);
        Api5DeleteAdsResult results = adgroupRule.getUserSteps().adsSteps().api5AdsDelete(new ParamsApi5DeleteAds()
                .withMediaplanId(adgroupRule.getMediaplanId())
                .withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp())
                .withSelectionCriteria(new SelectionCriteria().withAdGroupId(adgroupRule.getAdGroupId()).withIds(adsIds)));
        assertThat("несуществующий баннер при удалении вызвал ошибку",
                results.getDeleteResults().stream().filter(x->x.getErrors().isEmpty()).collect(Collectors.toList()),
                hasSize(2));

    }
    @Test
    public void deleteAds2Times(){
        //тут ошибки вобще не пишутся
        List<Long> adsIds = adgroupRule.getAdIds();
        adgroupRule.getUserSteps().adsSteps().api5AdsDelete(new ParamsApi5DeleteAds()
                .withMediaplanId(adgroupRule.getMediaplanId())
                .withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp())
                .withSelectionCriteria(new SelectionCriteria().withAdGroupId(adgroupRule.getAdGroupId()).withIds(adsIds)));

        adgroupRule.getUserSteps().adsSteps().api5AdsDelete(new ParamsApi5DeleteAds()
                .withMediaplanId(adgroupRule.getMediaplanId())
                .withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp())
                .withSelectionCriteria(new SelectionCriteria().withAdGroupId(adgroupRule.getAdGroupId()).withIds(adsIds)));

    }

}
