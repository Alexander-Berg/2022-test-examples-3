package ru.yandex.direct.grid.processing.util.findandreplace;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.grid.model.findandreplace.ReplaceRule;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefDomainInstruction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerHrefDomainReplaceRuleTest {

    private static final String NEW_DOMAIN = "test.ru";
    private static final String PROTOCOL = "http://";
    private static final String ORIGINAL_DOMAIN = "www.udochka2.ohota-24.ru";
    private static final String REST_PART =
            "/#reviews?utm_source=rsya&utm_medium=cpc&utm_campaign={campaign_id}&utm_content=bs2_{ad_id}&utm_term=удочка";

    @Autowired
    private BannersUrlHelper bannersUrlHelper;

    @Test
    public void simpleDomainTest() {
        String simpleDomain = "ya.ru";
        String originalHref = PROTOCOL + simpleDomain + REST_PART;
        String expectedHref = PROTOCOL + NEW_DOMAIN + REST_PART;

        executeAndAssertHrefEquals(Collections.singletonList(simpleDomain), NEW_DOMAIN, originalHref, expectedHref);
    }

    @Test
    public void complexDomainTest() {
        String originalHref = PROTOCOL + ORIGINAL_DOMAIN + REST_PART;
        String expectedHref = PROTOCOL + NEW_DOMAIN + REST_PART;

        executeAndAssertHrefEquals(Collections.singletonList(ORIGINAL_DOMAIN), NEW_DOMAIN, originalHref, expectedHref);
    }

    @Test
    public void regexpEscapeTest() {
        String newDomain = "h.tp";
        String originalHref = PROTOCOL + ORIGINAL_DOMAIN + REST_PART;
        String expectedHref = PROTOCOL + newDomain + REST_PART;

        executeAndAssertHrefEquals(Collections.singletonList(ORIGINAL_DOMAIN), newDomain, originalHref, expectedHref);
    }

    @Test
    public void httpsProtocolTest() {
        String protocol = "https://";
        String originalHref = protocol + ORIGINAL_DOMAIN + REST_PART;
        String expectedHref = protocol + NEW_DOMAIN + REST_PART;

        executeAndAssertHrefEquals(Collections.singletonList(ORIGINAL_DOMAIN), NEW_DOMAIN, originalHref, expectedHref);
    }

    @Test
    public void urlEncodingTest() throws UnsupportedEncodingException {
        String restPart = "/?paramName=" + URLEncoder.encode("тест", "UTF-8");
        String originalHref = PROTOCOL + ORIGINAL_DOMAIN + restPart;
        String expectedHref = PROTOCOL + NEW_DOMAIN + restPart;

        executeAndAssertHrefEquals(Collections.singletonList(ORIGINAL_DOMAIN), NEW_DOMAIN, originalHref, expectedHref);
    }

    @Test
    public void punycodeDomainTest() {
        String originalDomain = "xn--h1alffa9f.xn--p1ai";
        String newDomain = "где.жз";
        String originalHref = PROTOCOL + originalDomain + REST_PART;
        String expectedHref = PROTOCOL + newDomain + REST_PART;

        executeAndAssertHrefEquals(Collections.singletonList(originalDomain), newDomain, originalHref, expectedHref);
    }

    @Test
    public void rusDomainTest() {
        String originalDomain = "я.рф";
        String newDomain = "абв.гд";
        String originalHref = PROTOCOL + originalDomain + REST_PART;
        String expectedHref = PROTOCOL + newDomain + REST_PART;

        executeAndAssertHrefEquals(Collections.singletonList(originalDomain), newDomain, originalHref, expectedHref);
    }

    @Test
    public void thirdLevelDomainTest() {
        String originalDomain = "third.yandex.ru";
        String originalHref = PROTOCOL + originalDomain + REST_PART;
        String expectedHref = PROTOCOL + NEW_DOMAIN + REST_PART;

        executeAndAssertHrefEquals(Collections.singletonList(originalDomain), NEW_DOMAIN, originalHref, expectedHref);
    }

    @Test
    public void notChangePortTest() {
        String originalDomain = "yandex.ru";
        String originalHref = PROTOCOL + originalDomain + ":8080" + REST_PART;
        String expectedHref = PROTOCOL + NEW_DOMAIN + ":8080" + REST_PART;

        executeAndAssertHrefEquals(Collections.singletonList(originalDomain), NEW_DOMAIN, originalHref, expectedHref);
    }

    @Test
    public void domainWithoutRestPartTest() {
        String originalHref = PROTOCOL + ORIGINAL_DOMAIN;
        String expectedHref = PROTOCOL + NEW_DOMAIN;

        executeAndAssertHrefEquals(Collections.singletonList(ORIGINAL_DOMAIN), NEW_DOMAIN, originalHref, expectedHref);
    }

    @Test
    public void hrefInParamTest() {
        String restPart = "/paramName=" + PROTOCOL + ORIGINAL_DOMAIN + REST_PART;
        String originalHref = PROTOCOL + ORIGINAL_DOMAIN + restPart;
        String expectedHref = PROTOCOL + NEW_DOMAIN + restPart;

        executeAndAssertHrefEquals(Collections.singletonList(ORIGINAL_DOMAIN), NEW_DOMAIN, originalHref, expectedHref);
    }

    @Test
    public void multipleDomainsTest() {
        String originalHref = PROTOCOL + ORIGINAL_DOMAIN + REST_PART;
        String expectedHref = PROTOCOL + NEW_DOMAIN + REST_PART;
        List<String> searchDomains = Arrays.asList("test1.ru", "test2.ru", ORIGINAL_DOMAIN);

        executeAndAssertHrefEquals(searchDomains, NEW_DOMAIN, originalHref, expectedHref);

    }

    @Test
    public void ignoreDomainInParamsTest() {
        String searchingDomain = "ya.ru";
        String restPart = "/paramName=" + PROTOCOL + searchingDomain + REST_PART;
        String originalHref = PROTOCOL + ORIGINAL_DOMAIN + restPart;

        executeAndAssertHrefEquals(List.of(searchingDomain), NEW_DOMAIN, originalHref, originalHref);
    }

    @Test
    public void domainNotFountTest() {
        String originalHref = PROTOCOL + ORIGINAL_DOMAIN + REST_PART;

        executeAndAssertHrefEquals(List.of("notFound.ru"), NEW_DOMAIN, originalHref, originalHref);
    }

    private void executeAndAssertHrefEquals(List<String> searchDomains, String newDomain, String originalHref,
                                            String expectedHref) {
        GdFindAndReplaceAdsHrefDomainInstruction instruction = new GdFindAndReplaceAdsHrefDomainInstruction();
        instruction.setSearch(searchDomains);
        instruction.setReplace(newDomain);
        ReplaceRule replaceRule = new BannerHrefDomainReplaceRule(instruction, bannersUrlHelper);

        String result = replaceRule.apply(originalHref);
        assertEquals("Href should be equals", expectedHref, result);
    }

    private void executeAndAssertNull(String searchDomain, String newDomain, String originalHref) {
        GdFindAndReplaceAdsHrefDomainInstruction instruction = new GdFindAndReplaceAdsHrefDomainInstruction();
        instruction.setSearch(Collections.singletonList(searchDomain));
        instruction.setReplace(newDomain);
        ReplaceRule replaceRule = new BannerHrefDomainReplaceRule(instruction, bannersUrlHelper);

        String result = replaceRule.apply(originalHref);
        assertNull("Href should not change", result);
    }

}
