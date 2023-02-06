package ru.yandex.autotests.mediaplan.ads.update;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_ads.ParamsApi5AddAds;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import static ru.yandex.autotests.mediaplan.TestFeatures.ADS_UPDATE;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdsFactory.twoAd;
import static ru.yandex.autotests.mediaplan.datafactories.UpdateAdsFactory.makeOnlyOneAnotherFieldChanged;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;
@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADS_UPDATE)
@Description("Обновление 2 полей одного баннера для медиаплана")
@Tag(MasterTags.MASTER)
public class UpdateAdsTwoTimesInOneRequestTest {
    @Rule
    public AdgroupRule adgroupRule;
    private ParamsApi5AddAds paramsApi5AddAds;

    public UpdateAdsTwoTimesInOneRequestTest() {
        this.paramsApi5AddAds = twoAd();
        adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup()).withAd(paramsApi5AddAds);
    }

    @Test
    public void updateAd() {
        adgroupRule.getAds().get(0).setId(adgroupRule.getAds().get(1).getId());
        adgroupRule.getUserSteps().adsSteps().api5AdsUpdate(makeOnlyOneAnotherFieldChanged(adgroupRule.getAds())
                .withMediaplanId(adgroupRule.getMediaplanId())
                .withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp()));
    }

}
