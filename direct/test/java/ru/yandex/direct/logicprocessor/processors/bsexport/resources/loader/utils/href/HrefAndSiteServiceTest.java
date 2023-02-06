package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.BannerTextFormatter;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.parameterizer.BsHrefParametrizingService;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.parameterizer.ReplacingParams;
import ru.yandex.direct.logicprocessor.processors.bsexport.utils.CampaignNameTransliterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HrefAndSiteServiceTest {

    private BsHrefParametrizingService bsHrefParametrizingService;
    private CampaignNameTransliterator campaignNameTransliterator;
    private HrefAndSiteService hrefAndSiteService;

    @BeforeEach
    void before() {
        var bannerTextFormatter = new BannerTextFormatter();
        this.bsHrefParametrizingService = mock(BsHrefParametrizingService.class);
        this.campaignNameTransliterator = mock(CampaignNameTransliterator.class);
        this.hrefAndSiteService = new HrefAndSiteService(bannerTextFormatter, bsHrefParametrizingService,
                campaignNameTransliterator);
    }

    @Test
    void test() {
        var href = "https://my-site.ru";
        var domain = "site.ru";
        var campaignName = "test";
        var banner = new TextBanner().withId(1L).withAdGroupId(2L).withCampaignId(3L)
                .withHref(href)
                .withDomain(domain);
        when(bsHrefParametrizingService.parameterize(eq(href), any())).thenReturn(href);
        when(campaignNameTransliterator.translit(eq(campaignName))).thenReturn(campaignName);

        var campaign = new TextCampaign().withId(3L).withName("test").withType(CampaignType.TEXT)
                .withCurrency(CurrencyCode.RUB);
        var hrefAndSite = hrefAndSiteService.extract(banner, campaign);
        var expectedHrefAndSite = new HrefAndSite(href, domain, domain);
        assertThat(hrefAndSite).isEqualToComparingFieldByFieldRecursively(expectedHrefAndSite);
    }

    /**
     * Тест проверяет, что при отсутсвии протокола в ссылке, будет добавлен дефолтный протокол
     */
    @Test
    void hrefWithoutProtocolTest() {
        var href = "my-site.ru";
        var expectedHref = "http://my-site.ru";
        var domain = "site.ru";
        var campaignName = "test";
        var banner = new TextBanner().withId(1L).withAdGroupId(2L).withCampaignId(3L)
                .withHref(href)
                .withDomain(domain);
        when(bsHrefParametrizingService.parameterize(eq(expectedHref), any())).thenReturn(expectedHref);
        when(campaignNameTransliterator.translit(eq(campaignName))).thenReturn(campaignName);

        var campaign = new TextCampaign().withId(3L).withName("test").withType(CampaignType.TEXT)
                .withCurrency(CurrencyCode.RUB);
        var hrefAndSite = hrefAndSiteService.extract(banner, campaign);
        var expectedHrefAndSite = new HrefAndSite(expectedHref, domain, domain);
        assertThat(hrefAndSite).isEqualToComparingFieldByFieldRecursively(expectedHrefAndSite);
    }

    /**
     * Тест проверяет, что если домен, указанный на баннере, не валидный, то будет взят домен в ascii из ссылки,
     * а порт, параметры и протокол будут обрезаны
     */
    @Test
    void invalidDomainTest() {
        var href = "https://мой-сайт.ru:8080/path?param=param1";
        var expectedHref = "https://xn----8sbzclmxk.ru:8080/path?param=param1";
        var domain = "sit*e.ru";
        var domainFromHref = "xn----8sbzclmxk.ru";
        var campaignName = "test";
        var banner = new TextBanner().withId(1L).withAdGroupId(2L).withCampaignId(3L)
                .withHref(href)
                .withDomain(domain);
        when(bsHrefParametrizingService.parameterize(eq(expectedHref), any())).thenReturn(expectedHref);
        when(campaignNameTransliterator.translit(eq(campaignName))).thenReturn(campaignName);

        var campaign = new TextCampaign().withId(3L).withName("test").withType(CampaignType.TEXT)
                .withCurrency(CurrencyCode.RUB);
        var hrefAndSite = hrefAndSiteService.extract(banner, campaign);
        var expectedHrefAndSite = new HrefAndSite(expectedHref, domainFromHref, domainFromHref);
        assertThat(hrefAndSite).isEqualToComparingFieldByFieldRecursively(expectedHrefAndSite);
    }

    /**
     * Тест проверяет, что если домен в ссылке не в ascii, то он будет переведен в putycode
     */
    @Test
    void russianDomainInHrefTest() {
        var href = "https://мой-сайт.ru:8080/путь?param=ку";
        var domain = "my-site.ru";
        var expectedHref = "https://xn----8sbzclmxk.ru:8080/путь?param=ку";
        var campaignName = "test";
        var banner = new TextBanner().withId(1L).withAdGroupId(2L).withCampaignId(3L)
                .withHref(href)
                .withDomain(domain);
        when(bsHrefParametrizingService.parameterize(eq(expectedHref), any())).thenReturn(expectedHref);
        when(campaignNameTransliterator.translit(eq(campaignName))).thenReturn(campaignName);

        var campaign = new TextCampaign().withId(3L).withName("test").withType(CampaignType.TEXT)
                .withCurrency(CurrencyCode.RUB);
        var hrefAndSite = hrefAndSiteService.extract(banner, campaign);
        var expectedHrefAndSite = new HrefAndSite(expectedHref, domain, domain);
        assertThat(hrefAndSite).isEqualToComparingFieldByFieldRecursively(expectedHrefAndSite);
    }

    /**
     * Тест проверяет, что если домен в баннере не в ascii, то он будет переведен в putycode
     */
    @Test
    void russianDomainTest() {
        var href = "http://my-site.ru";
        var domain = "мой-сайт.ru";
        var domainAscii = "xn----8sbzclmxk.ru";
        var campaignName = "test";
        var banner = new TextBanner().withId(1L).withAdGroupId(2L).withCampaignId(3L)
                .withHref(href)
                .withDomain(domain);
        when(bsHrefParametrizingService.parameterize(eq(href), any())).thenReturn(href);
        when(campaignNameTransliterator.translit(eq(campaignName))).thenReturn(campaignName);

        var campaign = new TextCampaign().withId(3L).withName("test").withType(CampaignType.TEXT)
                .withCurrency(CurrencyCode.RUB);
        var hrefAndSite = hrefAndSiteService.extract(banner, campaign);
        var expectedHrefAndSite = new HrefAndSite(href, domain, domainAscii);
        assertThat(hrefAndSite).isEqualToComparingFieldByFieldRecursively(expectedHrefAndSite);
    }

    /**
     * Тест проверяет, что если ссылка или домен отсутствуют, то сервис вернет пустые строки
     */
    @Test
    void nullHrefAndDomainTest() {
        var campaignName = "test";
        var banner = new TextBanner().withId(1L).withAdGroupId(2L).withCampaignId(3L)
                .withHref(null)
                .withDomain(null);
        when(bsHrefParametrizingService.parameterize(eq(""), any())).thenReturn("");
        when(campaignNameTransliterator.translit(eq(campaignName))).thenReturn(campaignName);

        var campaign = new TextCampaign().withId(3L).withName("test").withType(CampaignType.TEXT)
                .withCurrency(CurrencyCode.RUB);
        var hrefAndSite = hrefAndSiteService.extract(banner, campaign);
        var expectedHrefAndSite = new HrefAndSite("", "", "");
        assertThat(hrefAndSite).isEqualToComparingFieldByFieldRecursively(expectedHrefAndSite);
    }

    /**
     * Тест проверяет, что если домен на русском языке удоблетворяет условию на длинну домена, а после перевода в
     * putycode - нет, то такой домен будет признан невалыдным и возьмется домен из ссылки
     */
    @Test
    void russianTooLongDomainTest() {
        var href = "http://my-site.ru";
        var domain = "ааааааааааааааааааааааааааааааааааааааааааааааааааааааааааааа.рф";
        var domainFromHref = "my-site.ru";
        var campaignName = "test";
        var banner = new TextBanner().withId(1L).withAdGroupId(2L).withCampaignId(3L)
                .withHref(href)
                .withDomain(domain);
        when(bsHrefParametrizingService.parameterize(eq(href), any())).thenReturn(href);
        when(campaignNameTransliterator.translit(eq(campaignName))).thenReturn(campaignName);

        var campaign = new TextCampaign().withId(3L).withName("test").withType(CampaignType.TEXT)
                .withCurrency(CurrencyCode.RUB);
        var hrefAndSite = hrefAndSiteService.extract(banner, campaign);
        var expectedHrefAndSite = new HrefAndSite(href, domainFromHref, domainFromHref);
        assertThat(hrefAndSite).isEqualToComparingFieldByFieldRecursively(expectedHrefAndSite);
    }

    /**
     * Тест проверяет, что если ссылка невалидная, то она будет заменена на пустую строку
     */
    @Test
    void invalidHrefTest() {
        var invalidHref = "https://my-site*.ru";
        var domain = "my-site.ru";
        var campaignName = "test";
        var banner = new TextBanner().withId(1L).withAdGroupId(2L).withCampaignId(3L)
                .withHref(invalidHref)
                .withDomain(domain);
        when(bsHrefParametrizingService.parameterize(eq(""), any())).thenReturn("");
        when(campaignNameTransliterator.translit(eq(campaignName))).thenReturn(campaignName);

        var campaign = new TextCampaign().withId(3L).withName("test").withType(CampaignType.TEXT)
                .withCurrency(CurrencyCode.RUB);
        var hrefAndSite = hrefAndSiteService.extract(banner, campaign);
        var expectedHrefAndSite = new HrefAndSite("", domain, domain);
        assertThat(hrefAndSite).isEqualToComparingFieldByFieldRecursively(expectedHrefAndSite);
    }

    /**
     * Тест проверяет правильность параметров для замены
     */
    @Test
    void replacingParamsTest() {
        var href = "https://my-site.ru";
        var domain = "my-site.ru";
        var campaignName = "текст";
        var campaignNameLat = "tekst";
        var banner = new TextBanner().withId(1L).withAdGroupId(2L).withCampaignId(3L)
                .withHref(href)
                .withDomain(domain);
        var replacingParamsCaptor = ArgumentCaptor.forClass(ReplacingParams.class);
        when(bsHrefParametrizingService.parameterize(eq(href), replacingParamsCaptor.capture())).thenReturn(href);

        when(campaignNameTransliterator.translit(eq(campaignName))).thenReturn(campaignNameLat);

        var strategy = (DbStrategy) new DbStrategy().withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA).withStrategyData(
                new StrategyData().withAvgCpa(new BigDecimal("12.34")));
        var campaign = new TextCampaign().withId(3L).withName(campaignName).withType(CampaignType.TEXT)
                .withCurrency(CurrencyCode.USD).withStrategy(strategy);
        hrefAndSiteService.extract(banner, campaign);
        var expectedReplacingParams = ReplacingParams.builder()
                .withBid(1L)
                .withPid(2L)
                .withCid(3L)
                .withCampaignType(CampaignType.TEXT)
                .withCampaignName(campaignName)
                .withCampaignNameLat(campaignNameLat)
                .withCampaignCurrency(CurrencyCode.USD)
                .withCampaignStrategy(strategy)
                .build();

        assertThat(replacingParamsCaptor.getAllValues().get(0))
                .isEqualToComparingFieldByFieldRecursively(expectedReplacingParams);
    }

}
