package ru.yandex.direct.organizations.swagger;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.altay.model.language.LanguageOuterClass.Language;
import ru.yandex.direct.organizations.swagger.model.CompanyUrl;
import ru.yandex.direct.organizations.swagger.model.CompanyUrlType;
import ru.yandex.direct.organizations.swagger.model.LocalizedString;
import ru.yandex.direct.organizations.swagger.model.PubApiCompaniesData;
import ru.yandex.direct.organizations.swagger.model.PubApiCompany;
import ru.yandex.direct.organizations.swagger.model.PubApiCompanyData;
import ru.yandex.direct.organizations.swagger.model.TycoonRubricDefinition;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.yandex.direct.organizations.swagger.OrganizationInfoConverters.updateLink;

public class OrganizationInfoConvertersTest {

    @Test
    public void convertCompanyUrls_oneValid() {
        String url = "http://ya.ru";
        List<CompanyUrl> urls = List.of(new CompanyUrl().type(CompanyUrlType.MAIN).value(url));
        List<String> result = OrganizationInfoConverters.convertCompanyUrls(urls);
        assertEquals(List.of(url), result);
    }

    @Test
    public void convertCompanyUrls_oneHidden() {
        String url = "http://ya.ru";
        List<CompanyUrl> urls = List.of(new CompanyUrl().type(CompanyUrlType.MAIN).hide(true).value(url));
        List<String> result = OrganizationInfoConverters.convertCompanyUrls(urls);
        assertEquals(emptyList(), result);
    }

    @Test
    public void convertCompanyUrls_oneNull() {
        List<CompanyUrl> urls = List.of(new CompanyUrl().type(CompanyUrlType.MAIN).value(null));
        List<String> result = OrganizationInfoConverters.convertCompanyUrls(urls);
        assertEquals(emptyList(), result);
    }

    @Test
    public void convertCompanyUrls_oneValidAndOneHidden() {
        String url = "http://ya.ru";
        String urlHidden = "http://hidden.ru";
        List<CompanyUrl> urls = List.of(
                new CompanyUrl().type(CompanyUrlType.MAIN).value(url),
                new CompanyUrl().type(CompanyUrlType.MAIN).hide(true).value(urlHidden));
        List<String> result = OrganizationInfoConverters.convertCompanyUrls(urls);
        assertEquals(List.of(url), result);
    }

    @Test
    public void getRubric_oneValid() {
        String expected = "тест";
        String language = Language.RU.name();
        PubApiCompanyData data = createCompanyDataWithRubricDefs(language, expected);

        assertEquals(expected, OrganizationInfoConverters.getRubric(data, language));
    }

    @Test
    public void getRubric_nonForSelectedLang_getFirst() {
        String expected = "тест";
        String language = Language.RU.name();
        PubApiCompanyData data = createCompanyDataWithRubricDefs(language, expected);

        assertEquals(expected, OrganizationInfoConverters.getRubric(data, Language.EN.name()));
    }

    @Test
    public void getRubric_twoValid_merge() {
        String expectedFirst = "тест";
        String expectedSecond = "тест2";
        String language = Language.RU.name();
        PubApiCompanyData data = createCompanyDataWithRubricDefs(language, expectedFirst, expectedSecond);

        String expected = expectedFirst + ", " + expectedSecond;
        assertEquals(expected, OrganizationInfoConverters.getRubric(data, language));
    }

    @Nonnull
    private PubApiCompanyData createCompanyDataWithRubricDefs(String language, String... defs) {
        TycoonRubricDefinition rubricDef = new TycoonRubricDefinition();
        for (String def : defs) {
            rubricDef.addNamesItem(new LocalizedString().locale(language).value(def));
        }
        Map<String, TycoonRubricDefinition> rubricDefs = Map.of("test", rubricDef);
        PubApiCompanyData data = new PubApiCompanyData();
        data.setRubricDefs(rubricDefs);
        return data;
    }

    @Test
    public void updateLink_singleOrganization_nullLink() {
        PubApiCompanyData data = new PubApiCompanyData().lkLink(null);

        PubApiCompanyData result = updateLink(data);
        assertNull(result.getLkLink());
    }

    @Test
    public void updateLink_singleOrganization_validLink() {
        String lkLink = "http://ya.ru";
        String expectedLkLink = lkLink + "?source=direct";
        PubApiCompanyData data = new PubApiCompanyData().lkLink(lkLink);

        PubApiCompanyData result = updateLink(data);
        assertEquals(expectedLkLink, result.getLkLink());
    }

    @Test
    public void updateLink_multipleOrganizations_empty_invalid_sevenValid() {
        List<String> lkLinks = List.of("", ":invalid", "https://ya.ru/path/", "http://ya.ru/path?",
                "http://ya.ru/path?query=text", "http://ya.ru/path?query=text&", "http://ya.ru/path?query=text#param");
        List<String> expectedLkLinks = List.of("", ":invalid", "https://ya.ru/path/?source=direct",
                "http://ya.ru/path?&source=direct", "http://ya.ru/path?query=text&source=direct",
                "http://ya.ru/path?query=text&&source=direct", "http://ya.ru/path?query=text&source=direct#param");
        List<PubApiCompany> companies = StreamEx.of(lkLinks)
                .map(l -> new PubApiCompany().lkLink(l))
                .toList();
        PubApiCompaniesData data = new PubApiCompaniesData();
        data.setCompanies(companies);

        PubApiCompaniesData result = updateLink(data);
        List<String> resultLkLinks = StreamEx.of(result.getCompanies()).map(c -> c.getLkLink()).toList();
        assertArrayEquals(expectedLkLinks.toArray(), resultLkLinks.toArray());
    }
}
