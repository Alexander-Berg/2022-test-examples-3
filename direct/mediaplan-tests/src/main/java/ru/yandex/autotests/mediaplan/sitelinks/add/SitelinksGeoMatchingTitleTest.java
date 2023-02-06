package ru.yandex.autotests.mediaplan.sitelinks.add;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.model.RegionIDValues;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_sitelinks.ParamsApi5AddSitelinks;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static ru.yandex.autotests.mediaplan.datafactories.AddSiteLinkFactory.kazahTitle;
import static ru.yandex.autotests.mediaplan.datafactories.AddSiteLinkFactory.turkishTitle;
import static ru.yandex.autotests.mediaplan.datafactories.AddSiteLinkFactory.ukrainTitle;
import static ru.yandex.autotests.mediaplan.TestFeatures.MEDIAPLANS_ADD;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(MEDIAPLANS_ADD)
@Issue("https://st.yandex-team.ru/MEDIAPLAN-124")
@Description("Проверка несоответствия языка в заголовке сайтлинков и геотаргетинге группы")
@RunWith(Parameterized.class)
public class SitelinksGeoMatchingTitleTest {
    private ParamsApi5AddSitelinks paramsApi5AddSiteLinks;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Добавить сайтлинк с украинским заголовком в русское объявление",
                        ukrainTitle(),
                        RegionIDValues.RUSSIA.getId().longValue(),
                },
                {"Добавить сайтлинк с турецким заголовком в русское объявление",
                        turkishTitle(),
                        RegionIDValues.RUSSIA.getId().longValue(),
                },
                {"Добавить сайтлинк с казахским заголовком в русское объявление",
                        kazahTitle(),
                        RegionIDValues.RUSSIA.getId().longValue(),
                }
        });
    }
    @Rule
    public AdgroupRule adgroupRule;

    public SitelinksGeoMatchingTitleTest(String text, ParamsApi5AddSitelinks paramsApi5AddSiteLinks, Long groupRegionId){
        this.paramsApi5AddSiteLinks = paramsApi5AddSiteLinks;
        adgroupRule = new AdgroupRule();
        adgroupRule.getParamsApi5AddAdgroups().getAdGroups().get(0).withRegionIds(Collections.singletonList(groupRegionId));
    }
    @Test
    public void addSiteLinks(){
        //тут должна быть какая-то ошибка
        adgroupRule.getUserSteps().sitelinksSteps().api5SitelinksAdd(paramsApi5AddSiteLinks
                .withMediaplanId(adgroupRule.getMediaplanId()).withClientId(getClient()).withTimestamp(adgroupRule.getLastUpdateTimestamp()));
    }

}
