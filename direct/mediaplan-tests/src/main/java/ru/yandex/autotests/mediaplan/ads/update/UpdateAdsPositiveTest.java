package ru.yandex.autotests.mediaplan.ads.update;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_ads.ParamsApi5AddAds;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.mediaplan.TestFeatures.ADS_UPDATE;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdsFactory.*;
import static ru.yandex.autotests.mediaplan.datafactories.UpdateAdsFactory.makeOnlyOneFieldChanged;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADS_UPDATE)
@Description("Обновление баннеров для медиаплана")
@Tag(MasterTags.MASTER)
@RunWith(Parameterized.class)
public class UpdateAdsPositiveTest {

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
    public ParamsApi5AddAds paramsApi5AddAds;

    public UpdateAdsPositiveTest(String text, ParamsApi5AddAds paramsApi5AddAds) {
        this.paramsApi5AddAds = paramsApi5AddAds;
        adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup()).withAd(paramsApi5AddAds);
    }

    @Test
    public void updateAds(){
        adgroupRule.getUserSteps().adsSteps().api5AdsUpdate(makeOnlyOneFieldChanged(adgroupRule.getAds())
                .withMediaplanId(adgroupRule.getMediaplanId())
                .withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp()));
    }
}
