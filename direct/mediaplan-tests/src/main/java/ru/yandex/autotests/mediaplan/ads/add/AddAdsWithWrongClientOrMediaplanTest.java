package ru.yandex.autotests.mediaplan.ads.add;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_ads.Api5AddAdsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_ads.ParamsApi5AddAds;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.ADS_ADD;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdsFactory.oneAd;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADS_ADD)
@Description("Создание баннеров для чужого медиаплана или клиента")
@Tag(MasterTags.MASTER)
public class AddAdsWithWrongClientOrMediaplanTest {
    private ParamsApi5AddAds paramsApi5AddAds = oneAd();
    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup());

    @Test
    @Description("Добавляем чужому клиенту баннеры")
    public void addAdsToOtherClient() {
        paramsApi5AddAds.getAds().stream().map(x -> x.withAdGroupId(adgroupRule.getAdGroupId()))
                .collect(Collectors.toList());
        Api5AddAdsResult addAdsResult = adgroupRule.getUserSteps().adsSteps()
                .api5AdsAdd(paramsApi5AddAds.withMediaplanId(adgroupRule.getMediaplanId())
                        .withClientId(2l)
                        .withTimestamp(adgroupRule.getLastUpdateTimestamp()));
        assertThat("Сохраненные баннеры соотвествуют ожиданиям",
                addAdsResult.getAddResults().stream().filter(x -> !x.getErrors().isEmpty()).collect(Collectors.toList()),
                not(hasSize(0)));
    }

    @Test
    @Description("Добавляем чужому клиенту баннеры")
    public void addAdsToOtherMediplan() {
        paramsApi5AddAds.getAds().stream().map(x -> x.withAdGroupId(adgroupRule.getAdGroupId())).collect(Collectors.toList());
        Api5AddAdsResult addAdsResult = adgroupRule.getUserSteps().adsSteps().api5AdsAdd(paramsApi5AddAds.withMediaplanId(adgroupRule.getMediaplanId() - 1)
                .withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp()));
        assertThat("Сохраненные баннеры соотвествуют ожиданиям",
                addAdsResult.getAddResults().stream().filter(x -> !x.getErrors().isEmpty()).collect(Collectors.toList()),
                not(hasSize(0)));
    }
}
