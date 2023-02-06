package ru.yandex.autotests.mediaplan.sitelinks.delete;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_add_sitelinks.ParamsApi5AddSitelinks;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_sitelinks.Api5DeleteSitelinksResult;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_sitelinks.ParamsApi5DeleteSitelinks;
import ru.yandex.autotests.mediaplan.datacontainersauto.api5_delete_sitelinks.SelectionCriteria;
import ru.yandex.autotests.mediaplan.rules.AdgroupRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.mediaplan.datafactories.AddSiteLinkFactory.oneSiteLinksSets;
import static ru.yandex.autotests.mediaplan.datafactories.AddSiteLinkFactory.twoSiteLinksSets;
import static ru.yandex.autotests.mediaplan.TestFeatures.SITELINKS_DELETE;
import static ru.yandex.autotests.mediaplan.rules.MediaplanRule.getClient;


@Aqua.Test
@ru.yandex.qatools.allure.annotations.Features(SITELINKS_DELETE)
@Issue("https://st.yandex-team.ru/MEDIAPLAN-124")
@Description("Удаление ссылки из медиаплана")
@RunWith(Parameterized.class)
public class PositiveDeleteSitelinksTest {
    private ParamsApi5AddSitelinks paramsApi5AddSiteLinks;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Одна ссылка", oneSiteLinksSets()},
                {"Две ссылки", twoSiteLinksSets()},
        });
    }

    @Rule
    public AdgroupRule adgroupRule;

    public PositiveDeleteSitelinksTest(String text, ParamsApi5AddSitelinks paramsApi5AddSitelinks) {
        this.paramsApi5AddSiteLinks = paramsApi5AddSitelinks;
        adgroupRule = new AdgroupRule().withSitelinks(paramsApi5AddSiteLinks);
    }

    @Test
    public void deleteSiteLinks() {
        Api5DeleteSitelinksResult api5DeleteSitelinksResult = adgroupRule.getUserSteps().sitelinksSteps().api5SitelinksDelete(new ParamsApi5DeleteSitelinks()
                .withMediaplanId(adgroupRule.getMediaplanId()).withClientId(getClient())
                .withTimestamp(adgroupRule.getLastUpdateTimestamp()).withSelectionCriteria(new SelectionCriteria()
                        .withIds(adgroupRule.getSitelinksIds())));
        assertThat("Ссылки удалились", api5DeleteSitelinksResult.getDeleteResults(), hasSize(paramsApi5AddSiteLinks.getSitelinksSets().size()));
    }
}
