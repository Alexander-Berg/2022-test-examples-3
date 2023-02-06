package ru.yandex.direct.intapi.entity.showconditions;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.model.CalloutsStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.DisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerImage;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusPhoneFlagModerate;
import ru.yandex.direct.core.entity.banner.model.old.StatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerCreativeInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpcVideoBannerInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestCalloutRepository;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.repository.TestKeywordRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesStatusshow;
import ru.yandex.direct.i18n.Translatable;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.showconditions.model.response.BannerItemResponse;
import ru.yandex.direct.intapi.entity.showconditions.service.BannerStatusService;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.draftTextBanner;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class BannerStatusServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BannerStatusService bannerStatusService;

    @Autowired
    private TestCampaignRepository testCampaignRepository;
    @Autowired
    private TestBannerRepository testBannerRepository;
    @Autowired
    private TestKeywordRepository testKeywordRepository;
    @Autowired
    private TestBannerImageRepository testBannerImageRepository;
    @Autowired
    private TestCalloutRepository testCalloutRepository;
    @Autowired
    private TranslationService translationService;

    private BannerStatusTranslations bst = BannerStatusTranslations.INSTANCE;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    private String translate(Translatable text) {
        return translationService.translate(text);
    }

    private TextBannerInfo createTextBannerByFlags(Flags flags) {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        if (!flags.isMoney()) {
            testCampaignRepository.setCampaignMoney(campaignInfo.getCampaignId(),
                    campaignInfo.getShard(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);
        }
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        Keyword keyword1 = defaultKeyword();
        Keyword keyword2 = defaultKeyword();
        if (flags.isDeclinedSomeKeywords()) {
            keyword1.withStatusModerate(ru.yandex.direct.core.entity.keyword.model.StatusModerate.NO);
            keyword2.withStatusModerate(ru.yandex.direct.core.entity.keyword.model.StatusModerate.NO);
        }
        steps.keywordSteps().createKeyword(adGroupInfo, keyword1);
        steps.keywordSteps().createKeyword(adGroupInfo, keyword2);

        OldTextBanner banner = activeTextBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId());

        // Ссылка
        if (!flags.isHref()) {
            banner.withHref("");
        }

        if (!flags.isBs()) {
            banner.withBsBannerId(0L)
                    .setStatusActive(false);
        }

        banner.withStatusShow(flags.isBannerStatusShow());
        banner.withStatusBsSynced(flags.isBannerStatusBsSynced() ? StatusBsSynced.YES : StatusBsSynced.NO);

        if (flags.isCallout() || flags.getCalloutStatusModerate() != null) {
            Callout callout = steps.calloutSteps().createDefaultCallout(clientInfo);
            List<Long> calloutIds = singletonList(callout.getId());
            banner.withCalloutIds(calloutIds);
            if (flags.getCalloutStatusModerate() != null) {
                testCalloutRepository
                        .updateStatusModerate(clientInfo.getShard(), calloutIds, flags.getCalloutStatusModerate());
            }
        }

        // Визитка
        if (flags.isVcard() || flags.getStatusPhoneFlagModerate() != null) {
            banner.withVcardId(steps.vcardSteps().createFullVcard().getVcardId());
            if (flags.getStatusPhoneFlagModerate() != null) {
                banner.withPhoneFlag(flags.getStatusPhoneFlagModerate());
            } else {
                banner.withPhoneFlag(StatusPhoneFlagModerate.YES);
            }
        }

        // Отображаемая ссылка
        if (flags.getDisplayHrefStatusModerate() != null) {
            banner.withDisplayHrefStatusModerate(flags.getDisplayHrefStatusModerate());
        }

        // Быстрые ссылки
        if (flags.isSiteLinks() || flags.getSitelinksStatusModerate() != null) {
            SitelinkSetInfo sitelinksInfo = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);
            banner.withSitelinksSetId(sitelinksInfo.getSitelinkSetId());
            if (flags.getSitelinksStatusModerate() != null) {
                banner.withStatusSitelinksModerate(flags.getSitelinksStatusModerate());
            }
        }
        // Турболендинг
        if (flags.isTurboLanding() || flags.getTurboLandingStatusModerate() != null) {
            OldBannerTurboLanding turbolanding =
                    steps.turboLandingSteps().createDefaultBannerTurboLanding(clientInfo.getClientId());
            banner.withTurboLandingId(turbolanding.getId())
                    .withTurboLandingStatusModerate(turbolanding.getStatusModerate());
            if (flags.getTurboLandingStatusModerate() != null) {
                banner.setTurboLandingStatusModerate(flags.getTurboLandingStatusModerate());
            }
        }

        // Видеодополнения
        if (flags.isVideoAdditions() || flags.getVideoAdditionsStatusModerate() != null) {
            BannerCreativeInfo bannerCreativeInfo = steps.bannerCreativeSteps().createDefaultTextBannerCreative();

            banner.withCreativeId(bannerCreativeInfo.getCreativeId());
            if (flags.getVideoAdditionsStatusModerate() != null) {
                banner.withCreativeStatusModerate(flags.getVideoAdditionsStatusModerate());
            }
        }

        // Объявление
        if (flags.getBannerStatusModerate() != null) {
            banner.withStatusModerate(flags.getBannerStatusModerate());
        }

        if (flags.getBannerStatusPostModerate() != null) {
            banner.withStatusPostModerate(flags.getBannerStatusPostModerate());
        }

        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo);

        // Картинка
        if (flags.isBannerImage() || flags.getImageStatusModerate() != null) {
            OldBannerImage bannerImage = steps.bannerSteps().createBannerImage(bannerInfo).getBannerImage();
            if (flags.getImageStatusModerate() != null) {
                bannerImage.setStatusModerate(flags.getImageStatusModerate());
                testBannerImageRepository
                        .updateStatusModerate(clientInfo.getShard(), singletonList(bannerInfo.getBannerId()),
                                flags.getImageStatusModerate());
                testBannerImageRepository
                        .updateStatusShow(clientInfo.getShard(), singletonList(bannerInfo.getBannerId()),
                                flags.isBannerImageStatusShow() ? BannerImagesStatusshow.Yes
                                        : BannerImagesStatusshow.No);
            }
            banner.withBannerImage(bannerImage);
        }

        return bannerInfo;
    }

    private String getTextBannerStatus(AbstractBannerInfo bannerInfo) {
        BannerItemResponse bannerItemResponse = bannerStatusService
                .getBannersResponse(clientInfo.getClientId(), singletonList(bannerInfo.getBanner().getId())).get(0);
        return bannerItemResponse.getTextStatus();
    }

    private void createBannerAndAssertStatus(String reason, Flags flags, Translatable... translatable) {
        TextBannerInfo bannerInfo = createTextBannerByFlags(flags);

        String actual = getTextBannerStatus(bannerInfo);
        String expected = String.join(" ", mapList(Arrays.asList(translatable), this::translate)).trim();
        expected = StringUtils.stripEnd(expected, ".");
        assertThat(reason, actual, beanDiffer(expected));

    }

    @Test
    public void draftBanner() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(draftTextBanner(), clientInfo);

        String actual = getTextBannerStatus(bannerInfo);
        String expected = translate(bst.draft()).trim();
        expected = StringUtils.stripEnd(expected, ".");
        assertThat("Баннер черновик", actual, beanDiffer(expected));
    }

    @Test
    public void archivedBanner() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(
                draftTextBanner().withStatusArchived(true),
                clientInfo);

        String actual = getTextBannerStatus(bannerInfo);
        assertThat("Архивный баннер", actual, beanDiffer(""));
    }

    @Test
    public void activeBanner_noText() {

        Flags flags = new Flags();

        createBannerAndAssertStatus("Активный баннер, статус не пишется", flags);
    }

    @Test
    public void activatingIsGoing() {

        Flags flags = new Flags()
                .withBannerStatusShow(false)
                .withBannerStatusBsSynced(false);

        createTextBannerByFlags(flags);

        createBannerAndAssertStatus("Идет активизация", flags, bst.activating());
    }

    @Test
    public void waitModeration() {

        Flags flags = new Flags()
                .withBannerStatusModerate(OldBannerStatusModerate.SENT)
                .withBannerStatusPostModerate(OldBannerStatusPostModerate.SENT);

        createBannerAndAssertStatus("Ожидает модерации", flags, bst.waitModeration());

    }

    @Test
    public void getToShow() {
        Flags flags = new Flags()
                .withMoney(false)
                .withBs(false)
                .withBannerStatusModerate(OldBannerStatusModerate.SENT);

        createBannerAndAssertStatus("Принято к показам", flags, bst.getToShow());
    }

    @Test
    public void getToShow_contactInfoWaitingModeration() {
        Flags flags = new Flags().withStatusPhoneFlagModerate(StatusPhoneFlagModerate.SENT);

        createBannerAndAssertStatus("Объявление принято. Контактная информация ожидает модерации",
                flags,
                bst.adAccepted(), bst.contactInfoWaitModeration());
    }

    @Test
    public void showsAreGoing() {

        Flags flags = new Flags().withBannerStatusModerate(OldBannerStatusModerate.SENT);
        createTextBannerByFlags(flags);

        createBannerAndAssertStatus("Идут показы", flags, bst.showsAreGoing());

    }

    @Test
    public void adAccepted_sitelinksWaitingModeration() {

        Flags flags = new Flags()
                .withMoney(false)
                .withSitelinksStatusModerate(StatusSitelinksModerate.SENT);

        createBannerAndAssertStatus("Объявление принято. Быстрые ссылки ожидают модерации",
                flags,
                bst.adAccepted(), bst.sitelinksWaitModeration());

    }

    @Test
    public void imageWaitingModeration() {

        Flags flags = new Flags()
                .withBannerStatusModerate(OldBannerStatusModerate.YES)
                .withImageStatusModerate(OldStatusBannerImageModerate.SENT);

        createBannerAndAssertStatus("Изображение ожидает модерации", flags, bst.imageWaitModeration());
    }

    @Test
    public void bannerStatusAndImageStatusSent_showsAreGoing() {

        Flags flags = new Flags()
                .withBannerStatusModerate(OldBannerStatusModerate.SENT)
                .withImageStatusModerate(OldStatusBannerImageModerate.SENT);

        createBannerAndAssertStatus("Идут показы", flags, bst.showsAreGoing());
    }

    @Test
    public void displayHrefWainingModerating() {

        Flags flags = new Flags().withDisplayHrefStatusModerate(DisplayHrefStatusModerate.SENT);

        createBannerAndAssertStatus("Отображаемая ссылка ожидает модерации", flags, bst.displayHrefWaitModeration());
    }

    @Test
    public void turboLandingWaitingModeration() {

        Flags flags = new Flags().withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.SENT);

        createBannerAndAssertStatus("Турболендинг ожидает модерации", flags, bst.turboLandingWaitModeration());
    }

    @Test
    public void videoAdditionsWaitingModeration() {

        Flags flags = new Flags().withVideoAdditionsStatusModerate(OldBannerCreativeStatusModerate.SENT);

        createBannerAndAssertStatus("Видеодополнение ожидает модерации", flags, bst.videoAdditionsWaitModeration());
    }

    @Test
    public void cpcVideoWaitingModeration() {
        BannerCreativeInfo bannerCreativeInfo = steps.bannerCreativeSteps().createCpcVideoBannerCreative(clientInfo);

        OldCpcVideoBanner banner = activeCpcVideoBanner(null, null, null)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.SENT)
                .withCreativeId(bannerCreativeInfo.getCreativeId());

        CpcVideoBannerInfo bannerInfo = steps.bannerSteps().createActiveCpcVideoBanner(banner, clientInfo);
        String actual = getTextBannerStatus(bannerInfo);

        String expected = StringUtils.stripEnd(translate(bst.waitModeration()), ".");
        assertThat("креатив с кликовым видео ожидает модерации", actual, beanDiffer(expected));
    }

    @Test
    public void cpmBannerWaitingModeration() {
        BannerCreativeInfo bannerCreativeInfo = steps.bannerCreativeSteps().createCpmBannerCreative(clientInfo);

        OldCpmBanner banner = activeCpmBanner(null, null, null)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.SENT)
                .withCreativeId(bannerCreativeInfo.getCreativeId());

        CpmBannerInfo bannerInfo = steps.bannerSteps().createActiveCpmBanner(banner, clientInfo);
        String actual = getTextBannerStatus(bannerInfo);

        String expected = StringUtils.stripEnd(translate(bst.waitModeration()), ".");
        assertThat("креатив в cpm_banner ожидает модерации", actual, beanDiffer(expected));
    }

    @Test
    public void declinedModeration() {

        Flags flags = new Flags().withBannerStatusModerate(OldBannerStatusModerate.NO);

        createBannerAndAssertStatus("Отклонено модератором", flags, bst.declineModeration());
    }

    @Test
    public void keywordsDeclined() {
        Flags flags = new Flags().withDeclinedSomeKeywords(true);

        createBannerAndAssertStatus("Часть фраз отклонена", flags, bst.keywordsDeclinedModeration());

    }

    @Test
    public void sitelinksDeclined() {

        Flags flags = new Flags().withSitelinksStatusModerate(StatusSitelinksModerate.NO);

        createBannerAndAssertStatus("Быстрые ссылки отклонены", flags, bst.sitelinksDeclinedModeration());
    }

    @Test
    public void imageDeclined() {

        Flags flags = new Flags()
                .withImageStatusModerate(OldStatusBannerImageModerate.NO)
                .withBannerImageStatusShow(true);

        createBannerAndAssertStatus("Изображение отклонено", flags, bst.imageDeclinedModeration());
    }

    @Test
    public void imageStatusShowFalse_noText() {

        Flags flags = new Flags()
                .withImageStatusModerate(OldStatusBannerImageModerate.NO)
                .withBannerImageStatusShow(false);

        createBannerAndAssertStatus("Пользователь запретил показы. Статус не пишется", flags);
    }

    @Test
    public void displayHrefDeclined() {

        Flags flags = new Flags().withDisplayHrefStatusModerate(DisplayHrefStatusModerate.NO);

        createBannerAndAssertStatus("Отображаемая ссылка отклонена", flags, bst.displayHrefDeclinedModeration());
    }

    @Test
    public void turboLandingDeclined() {

        Flags flags = new Flags().withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.NO);

        createBannerAndAssertStatus("Турбо-страница отклонена", flags, bst.turboLandingDeclinedModeration());
    }

    @Test
    public void contactInfoDeclined() {

        Flags flags = new Flags().withStatusPhoneFlagModerate(StatusPhoneFlagModerate.NO);

        createBannerAndAssertStatus("Контактная информация отклонена", flags, bst.contactInfoDeclinedModeration());
    }

    @Test
    public void videoAdditionsDeclined() {

        Flags flags = new Flags().withVideoAdditionsStatusModerate(OldBannerCreativeStatusModerate.NO);

        createBannerAndAssertStatus("Видеодополнение отклонено", flags, bst.videoAdditionsDeclinedModeration());
    }

    @Test
    public void cpcVideoDeclined() {
        BannerCreativeInfo bannerCreativeInfo = steps.bannerCreativeSteps().createCpcVideoBannerCreative(clientInfo);

        OldCpcVideoBanner banner = activeCpcVideoBanner(null, null, null)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NO)
                .withCreativeId(bannerCreativeInfo.getCreativeId());

        CpcVideoBannerInfo bannerInfo = steps.bannerSteps().createActiveCpcVideoBanner(banner, clientInfo);
        String actual = getTextBannerStatus(bannerInfo);

        String expected = StringUtils.stripEnd(translate(bst.declineModeration()), ".");
        assertThat("креатив с кликовым видео отклонен", actual, beanDiffer(expected));
    }

    @Test
    public void cpmBannerDeclined() {
        BannerCreativeInfo bannerCreativeInfo = steps.bannerCreativeSteps().createCpmBannerCreative(clientInfo);

        OldCpmBanner banner = activeCpmBanner(null, null, null)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NO)
                .withCreativeId(bannerCreativeInfo.getCreativeId());

        CpmBannerInfo bannerInfo = steps.bannerSteps().createActiveCpmBanner(banner, clientInfo);
        String actual = getTextBannerStatus(bannerInfo);

        String expected = StringUtils.stripEnd(translate(bst.declineModeration()), ".");
        assertThat("креатив в cpm_banner отклонен", actual, beanDiffer(expected));
    }

    @Test
    public void calloutDeclined() {

        Flags flags = new Flags().withCalloutStatusModerate(CalloutsStatusModerate.NO);

        createBannerAndAssertStatus("Уточнения отклонены", flags, bst.calloutDeclinedModeration());
    }

    @Test
    public void getTextStatus_multiplyDeclined() {

        Flags flags = new Flags()
                .withBs(false)
                .withStatusPhoneFlagModerate(StatusPhoneFlagModerate.NO)
                .withSitelinksStatusModerate(StatusSitelinksModerate.NO)
                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.NO)
                .withVideoAdditionsStatusModerate(OldBannerCreativeStatusModerate.NO)
                .withImageStatusModerate(OldStatusBannerImageModerate.NO)
                .withDisplayHrefStatusModerate(DisplayHrefStatusModerate.NO)
                .withCalloutStatusModerate(CalloutsStatusModerate.NO)
                .withDeclinedSomeKeywords(true);

        TextBannerInfo bannerInfo = createTextBannerByFlags(flags);
        String actual = getTextBannerStatus(bannerInfo);

        List<String> texts = Arrays.asList(translate(bst.contactInfoMultiplyDeclined()),
                translate(bst.keywordsMultiplyDeclined()),
                translate(bst.sitelinksMultiplyDeclined()),
                translate(bst.imageMultiplyDeclined()),
                translate(bst.calloutMultiplyDeclined()),
                translate(bst.displayHrefMultiplyDeclined()),
                translate(bst.videoAdditionsMultiplyDeclined()),
                translate(bst.turboLandingMultiplyDeclined()));
        String expected = translate(bst.multiplyDeclined()) + String.join(", ", texts);

        assertThat("Множественные причины отклонения.", actual, beanDiffer(expected));

    }
}

class Flags {
    private boolean money = true;
    private boolean vcard = false;
    private boolean siteLinks = false;
    private boolean href = true;
    private boolean bs = true;
    private boolean bannerImage = false;
    private boolean bannerImageStatusShow = true;
    private boolean turboLanding = false;
    private boolean declinedSomeKeywords = false;
    private boolean videoAdditions = false;
    private boolean bannerStatusShow = true;
    private boolean bannerStatusBsSynced = true;
    private boolean callout = false;
    private OldBannerStatusModerate bannerStatusModerate;
    private OldBannerStatusPostModerate bannerStatusPostModerate;
    private StatusPhoneFlagModerate statusPhoneFlagModerate;
    private StatusSitelinksModerate sitelinksStatusModerate;
    private DisplayHrefStatusModerate displayHrefStatusModerate;
    private OldStatusBannerImageModerate imageStatusModerate;
    private OldBannerTurboLandingStatusModerate turboLandingStatusModerate;
    private OldBannerCreativeStatusModerate videoAdditionsStatusModerate;
    private OldBannerCreativeStatusModerate cpcVideoStatusModerate;
    private CalloutsStatusModerate calloutStatusModerate;

    public boolean isMoney() {
        return money;
    }

    Flags withMoney(boolean money) {
        this.money = money;
        return this;
    }

    public boolean isVcard() {
        return vcard;
    }

    public Flags withVcard(boolean vcard) {
        this.vcard = vcard;
        return this;
    }

    public boolean isSiteLinks() {
        return siteLinks;
    }

    public Flags withSiteLinks(boolean siteLinks) {
        this.siteLinks = siteLinks;
        return this;
    }

    public boolean isHref() {
        return href;
    }

    public Flags withHref(boolean href) {
        this.href = href;
        return this;
    }

    public boolean isBs() {
        return bs;
    }

    public Flags withBs(boolean bs) {
        this.bs = bs;
        return this;
    }

    public boolean isBannerImage() {
        return bannerImage;
    }

    public Flags withBannerImage(boolean bannerImage) {
        this.bannerImage = bannerImage;
        return this;
    }

    public boolean isBannerImageStatusShow() {
        return bannerImageStatusShow;
    }

    public Flags withBannerImageStatusShow(boolean bannerImageStatusShow) {
        this.bannerImageStatusShow = bannerImageStatusShow;
        return this;
    }

    public boolean isDeclinedSomeKeywords() {
        return declinedSomeKeywords;
    }

    public Flags withDeclinedSomeKeywords(boolean declinedSomeKeywords) {
        this.declinedSomeKeywords = declinedSomeKeywords;
        return this;
    }

    public boolean isVideoAdditions() {
        return videoAdditions;
    }

    public Flags withVideoAdditions(boolean videoAdditions) {
        this.videoAdditions = videoAdditions;
        return this;
    }

    public boolean isBannerStatusShow() {
        return bannerStatusShow;
    }

    public Flags withBannerStatusShow(boolean bannerStatusShow) {
        this.bannerStatusShow = bannerStatusShow;
        return this;
    }

    public boolean isBannerStatusBsSynced() {
        return bannerStatusBsSynced;
    }

    public Flags withBannerStatusBsSynced(boolean bannerStatusBsSynced) {
        this.bannerStatusBsSynced = bannerStatusBsSynced;
        return this;
    }

    public boolean isCallout() {
        return callout;
    }

    public Flags withCallout(boolean callout) {
        this.callout = callout;
        return this;
    }

    public OldBannerStatusModerate getBannerStatusModerate() {
        return bannerStatusModerate;
    }

    public Flags withBannerStatusModerate(OldBannerStatusModerate bannerStatusModerate) {
        this.bannerStatusModerate = bannerStatusModerate;
        return this;
    }

    public OldBannerStatusPostModerate getBannerStatusPostModerate() {
        return bannerStatusPostModerate;
    }

    public Flags withBannerStatusPostModerate(OldBannerStatusPostModerate bannerStatusPostModerate) {
        this.bannerStatusPostModerate = bannerStatusPostModerate;
        return this;
    }

    public StatusPhoneFlagModerate getStatusPhoneFlagModerate() {
        return statusPhoneFlagModerate;
    }

    public Flags withStatusPhoneFlagModerate(StatusPhoneFlagModerate statusPhoneFlagModerate) {
        this.statusPhoneFlagModerate = statusPhoneFlagModerate;
        return this;
    }

    public StatusSitelinksModerate getSitelinksStatusModerate() {
        return sitelinksStatusModerate;
    }

    public Flags withSitelinksStatusModerate(StatusSitelinksModerate sitelinksStatusModerate) {
        this.sitelinksStatusModerate = sitelinksStatusModerate;
        return this;
    }

    public DisplayHrefStatusModerate getDisplayHrefStatusModerate() {
        return displayHrefStatusModerate;
    }

    public Flags withDisplayHrefStatusModerate(DisplayHrefStatusModerate displayHrefStatusModerate) {
        this.displayHrefStatusModerate = displayHrefStatusModerate;
        return this;
    }

    public OldStatusBannerImageModerate getImageStatusModerate() {
        return imageStatusModerate;
    }

    public Flags withImageStatusModerate(OldStatusBannerImageModerate imageStatusModerate) {
        this.imageStatusModerate = imageStatusModerate;
        return this;
    }

    public boolean isTurboLanding() {
        return turboLanding;
    }

    public Flags withTurboLanding(boolean turboLanding) {
        this.turboLanding = turboLanding;
        return this;
    }

    public OldBannerTurboLandingStatusModerate getTurboLandingStatusModerate() {
        return turboLandingStatusModerate;
    }

    public Flags withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate turboLandingStatusModerate) {
        this.turboLandingStatusModerate = turboLandingStatusModerate;
        return this;
    }

    public OldBannerCreativeStatusModerate getVideoAdditionsStatusModerate() {
        return videoAdditionsStatusModerate;
    }

    public Flags withVideoAdditionsStatusModerate(OldBannerCreativeStatusModerate videoAdditionsStatusModerate) {
        this.videoAdditionsStatusModerate = videoAdditionsStatusModerate;
        return this;
    }

    public CalloutsStatusModerate getCalloutStatusModerate() {
        return calloutStatusModerate;
    }

    public Flags withCalloutStatusModerate(CalloutsStatusModerate calloutStatusModerate) {
        this.calloutStatusModerate = calloutStatusModerate;
        return this;
    }


}
