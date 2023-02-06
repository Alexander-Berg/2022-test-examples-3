package ru.yandex.direct.core.entity.page.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@ExtendWith(SpringExtension.class)
class PageServiceTest {

    private static final String INTERNAL_PAGE_DOMAIN_1 = "yandex.ru";
    private static final String INTERNAL_PAGE_DOMAIN_2 = "com.edadeal.android";
    private static final String NON_INTERNAL_PAGE_DOMAIN_1 = "google.com";
    private static final String NON_INTERNAL_PAGE_DOMAIN_2 = "youtube.com";

    private static final String INTERNAL_PAGE_DOMAIN_WITH_CAPITAL_LETTERS_1 = "com.s-g-i.Edadeal";
    private static final String INTERNAL_PAGE_DOMAIN_WITH_CAPITAL_LETTERS_2 = "mobi.mgeek.TunnyBrowser";
    private static final String INTERNAL_PAGE_DOMAIN_WITH_CAPITAL_LETTERS_3 = "ru.AutoRu";
    private static final String INTERNAL_PAGE_DOMAIN_WITH_CAPITAL_LETTERS_4 = "ru.yandex.mobile.MetricaSample";

    @Autowired
    private PageService pageService;

    @Test
    void getInternalPagesDomains_NoDomainIsInternal_ReturnsEmptyList() {
        assertThat(pageService.getInternalPagesDomains(List.of(NON_INTERNAL_PAGE_DOMAIN_1, NON_INTERNAL_PAGE_DOMAIN_2)))
                .isEmpty();
    }

    @Test
    void getInternalPagesDomains_SomeDomainsAreInternal_ReturnsCorrectDomains() {
        assertThat(pageService.getInternalPagesDomains(List.of(INTERNAL_PAGE_DOMAIN_1, INTERNAL_PAGE_DOMAIN_2,
                NON_INTERNAL_PAGE_DOMAIN_1, NON_INTERNAL_PAGE_DOMAIN_2)))
                .containsExactlyInAnyOrder(INTERNAL_PAGE_DOMAIN_1, INTERNAL_PAGE_DOMAIN_2);
    }

    @Test
    void getInternalPagesDomains_AllDomainsAreInternal_ReturnsCorrectDomains() {
        assertThat(pageService.getInternalPagesDomains(List.of(INTERNAL_PAGE_DOMAIN_1, INTERNAL_PAGE_DOMAIN_2)))
                .containsExactlyInAnyOrder(INTERNAL_PAGE_DOMAIN_1, INTERNAL_PAGE_DOMAIN_2);
    }

    @Test
    void getInternalPagesDomains_InternalDomainsUpperCased_ReturnsCorrectDomains() {
        assertThat(pageService.getInternalPagesDomains(
                List.of(INTERNAL_PAGE_DOMAIN_1.toUpperCase(),
                        INTERNAL_PAGE_DOMAIN_2.toUpperCase())))
                .containsExactlyInAnyOrder(INTERNAL_PAGE_DOMAIN_1.toUpperCase(), INTERNAL_PAGE_DOMAIN_2.toUpperCase());
    }

    @Test
    void getInternalPagesDomains_InternalDomainsWithCapitalLetters_ReturnsCorrectDomains() {
        assertThat(pageService.getInternalPagesDomains(
                List.of(INTERNAL_PAGE_DOMAIN_WITH_CAPITAL_LETTERS_1,
                        INTERNAL_PAGE_DOMAIN_WITH_CAPITAL_LETTERS_2,
                        INTERNAL_PAGE_DOMAIN_WITH_CAPITAL_LETTERS_3,
                        INTERNAL_PAGE_DOMAIN_WITH_CAPITAL_LETTERS_4)))
                .containsExactlyInAnyOrder(
                        INTERNAL_PAGE_DOMAIN_WITH_CAPITAL_LETTERS_1,
                        INTERNAL_PAGE_DOMAIN_WITH_CAPITAL_LETTERS_2,
                        INTERNAL_PAGE_DOMAIN_WITH_CAPITAL_LETTERS_3,
                        INTERNAL_PAGE_DOMAIN_WITH_CAPITAL_LETTERS_4);
    }
}
