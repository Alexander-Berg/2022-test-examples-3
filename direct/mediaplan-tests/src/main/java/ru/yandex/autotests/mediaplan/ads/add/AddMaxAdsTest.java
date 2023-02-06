package ru.yandex.autotests.mediaplan.ads.add;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_adgroups.Api5AddAdgroupsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_ads.Ad;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_ads.Api5AddAdsResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_ads.ParamsApi5AddAds;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_changes_check_verbose.Api5ChangesCheckVerboseResult;
import ru.yandex.autotests.mediaplan.rules.MediaplanRule;
import ru.yandex.autotests.mediaplan.tags.MasterTags;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.TestFeatures.ADS_ADD;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdGroupsFactory.oneAdgroup;
import static ru.yandex.autotests.mediaplan.datafactories.AddAdsFactory.thousandAd;
import static ru.yandex.autotests.mediaplan.datafactories.AddMediplanFactory.hundredMediaplans;
import static ru.yandex.autotests.mediaplan.datafactories.AddRequestsFactory.hundredRequests;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(ADS_ADD)
@Description("Создание 1000 баннеров для медиаплана")
@Tag(MasterTags.MASTER)
public class AddMaxAdsTest {
    @Rule
    public MediaplanRule mediaplanRule = new MediaplanRule().withParamsApi5AddRequests(hundredRequests())
            .withMediaplans(hundredMediaplans());

    @Test
    public void add1000ads() {
        Long lastUpdateTimestamp = mediaplanRule.getLastUpdateTimestamp();
        List<Long> adgroupIds = new ArrayList<>();
        for (Long mediplanId : mediaplanRule.getMediaplanIds()) {
            Api5AddAdgroupsResult api5AddAdgroupsResult = mediaplanRule.getUserSteps().adGroupsSteps().api5AdGroupsAdd(
                    oneAdgroup().withMediaplanId(mediplanId).withTimestamp(lastUpdateTimestamp)
                            .withClientId(getClient())
            );
            lastUpdateTimestamp = api5AddAdgroupsResult.getTimestamp();
            adgroupIds.add(api5AddAdgroupsResult.getAddResults().get(0).getId());
        }
        ParamsApi5AddAds paramsApi5AddAds = thousandAd();
        for (int i = 0; i < paramsApi5AddAds.getAds().size(); i++) {
            paramsApi5AddAds.getAds().get(0).withAdGroupId(adgroupIds.get(i % adgroupIds.size()));
        }
        Api5AddAdsResult addAdsResult = mediaplanRule.getUserSteps().adsSteps().api5AdsAdd(paramsApi5AddAds.withMediaplanId(mediaplanRule.getMediaplanId())
                .withClientId(getClient())
                .withTimestamp(mediaplanRule.getLastUpdateTimestamp()));
        List<Ad> expectedAds = IntStream.range(0, paramsApi5AddAds.getAds().size())
                .mapToObj(x -> paramsApi5AddAds.getAds().get(x).withId(addAdsResult.getAddResults().get(x).getId())).collect(Collectors.toList());
        Api5ChangesCheckVerboseResult changes = mediaplanRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupIds, getClient(), mediaplanRule.getMediaplanId());

        mediaplanRule.getUserSteps().changesSteps()
                .adGroupChanges(adgroupIds, getClient(), mediaplanRule.getMediaplanId());


        assertThat("Сохраненные баннеры соотвествуют ожиданиям", changes.getModified().getAds(), equalTo(expectedAds));
    }
}
