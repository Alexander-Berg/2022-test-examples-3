package ru.yandex.autotests.mediaplan.sitelinks.add;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_sitelinks.ParamsApi5AddSitelinks;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.mediaplan.datafactories.AddSiteLinkFactory.oneSiteLinksSets;
import static ru.yandex.autotests.mediaplan.datafactories.AddSiteLinkFactory.twoSiteLinksSets;
import static ru.yandex.autotests.mediaplan.TestFeatures.SITELINKS_ADD;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(SITELINKS_ADD)
@Issue("https://st.yandex-team.ru/MEDIAPLAN-124")
@Description("Создание ссылки для медиаплана")
@RunWith(Parameterized.class)
public class AddSiteLinkPositiveTest {
    @Parameterized.Parameter(value = 0)
    public String text;

    @Parameterized.Parameter(value = 1)
    public ParamsApi5AddSitelinks paramsApi5AddSiteLinks;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Одна визитная каротчка", oneSiteLinksSets()},
                {"Две визитные карточки", twoSiteLinksSets()},
        });
    }

    @Rule
    public AdgroupRule adgroupRule = new AdgroupRule();

    @Test
    public void addSiteLinks(){
        adgroupRule.getUserSteps().sitelinksSteps().api5SitelinksAdd(paramsApi5AddSiteLinks
                .withMediaplanId(adgroupRule.getMediaplanId()).withClientId(getClient()).withTimestamp(adgroupRule.getLastUpdateTimestamp()));
    }
}
