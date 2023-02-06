package ru.yandex.autotests.mediaplan.ads.add;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_ads.Ad;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_ads.Api5AddAdsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_ads.ParamsApi5AddAds;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_changes_check_verbose.Api5ChangesCheckVerboseResult;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.ADS_ADD;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdsFactory.*;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADS_ADD)
@Description("Создание баннеров для медиаплана")
@Tag(MasterTags.MASTER)
@RunWith(Parameterized.class)
public class AddAdsPositiveTest {
    @Parameterized.Parameter(value = 1)
    public ParamsApi5AddAds paramsApi5AddAds;

    @Parameterized.Parameter(value = 0)
    public String text;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Один баннер", oneAd()},
                {"Два баннера", twoAd()},
                {"50 объявлений", fiftyAd()},
        });
    }



    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule().withAddAdGroupsInputData(oneAdgroup());

    @Test
    @Description("Добавляем баннер")
    public void addAds() {
        paramsApi5AddAds.getAds().stream().map(x -> x.withAdGroupId(adgroupRule.getAdGroupId())).collect(Collectors.toList());
        Api5AddAdsResult addAdsResult = adgroupRule.getUserSteps().adsSteps().api5AdsAdd(paramsApi5AddAds.withMediaplanId(adgroupRule.getMediaplanId())
                .withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp()));
        List<Ad> expectedAds = IntStream.range(0, paramsApi5AddAds.getAds().size())
                .mapToObj(x -> paramsApi5AddAds.getAds().get(x).withId(addAdsResult.getAddResults().get(x).getId())).collect(Collectors.toList());
        Api5ChangesCheckVerboseResult changes = adgroupRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupRule.getAdGroupIds(), getClient(), adgroupRule.getMediaplanId());

        assertThat("Сохраненные баннеры соотвествуют ожиданиям", changes.getModified().getAds(), equalTo(expectedAds));
    }

}
