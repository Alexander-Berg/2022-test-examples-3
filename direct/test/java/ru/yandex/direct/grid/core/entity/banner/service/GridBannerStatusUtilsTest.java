package ru.yandex.direct.grid.core.entity.banner.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.model.CalloutsStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBanner;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerBannerImagesStatusModerate;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerBannersCreativeStatusModerate;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerBannersDisplayHrefStatusModerate;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerCreative;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerDisplayHref;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerImage;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerImageStatusModerate;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerPrimaryStatus;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerResources;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerStatus;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerStatusBsSynced;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerStatusModerate;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerStatusPostModerate;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerStatusSitelinksModerate;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerStatusVCardModeration;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerTurboLanding;
import ru.yandex.direct.grid.core.entity.banner.model.GdiImagesImage;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(Parameterized.class)
public class GridBannerStatusUtilsTest {

    /**
     * Новый баннер, прошедший модерацию и несинхронизированный с БК
     */
    private static GdiBanner banner() {
        return new GdiBanner()
                .withBannerType(BannersBannerType.text)
                .withStatusShow(true)
                .withStatusActive(false)
                .withStatusArchived(false)
                .withStatusModerate(GdiBannerStatusModerate.YES)
                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                .withTurbolanding(new GdiBannerTurboLanding()
                        .withStatusModerate(BannerTurboLandingStatusModerate.YES))
                .withDisplayHref(new GdiBannerDisplayHref()
                        .withStatusModerate(GdiBannerBannersDisplayHrefStatusModerate.YES))
                .withImage(new GdiImagesImage().withStatusModerate(GdiBannerImageStatusModerate.YES))
                .withBannerImage(new GdiBannerImage().withStatusModerate(GdiBannerBannerImagesStatusModerate.YES))
                .withCreative(new GdiBannerCreative().withStatusModerate(GdiBannerBannersCreativeStatusModerate.YES))
                .withSitelinksSetId(RandomNumberUtils.nextPositiveLong())
                .withStatusSitelinksModerate(GdiBannerStatusSitelinksModerate.YES)
                .withVcardId(RandomNumberUtils.nextPositiveLong())
                .withPhoneFlag(GdiBannerStatusVCardModeration.YES)
                .withStatusMonitoringDomainStop(false)
                .withStatusBsSynced(GdiBannerStatusBsSynced.NO)
                .withCallouts(List.of(new Callout().withStatusModerate(CalloutsStatusModerate.YES)));
    }

    private static GdiBannerStatus status() {
        return new GdiBannerStatus()
                .withRejectedOnModeration(false)
                .withPreviousVersionShown(false)
                .withPlacementPagesRequired(false)
                .withHasInactiveResources(Collections.emptySet())
                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE);
    }

    @Parameterized.Parameter
    public GdiBanner banner;

    @Parameterized.Parameter(1)
    public GdiBannerStatus expectedStatus;

    @Parameterized.Parameter(2)
    public String testCaseDescription;

    @Parameterized.Parameters(name = "desc = {2}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        banner()
                                .withStatusArchived(true),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ARCHIVED),
                        "ARCHIVED.1 баннер заархивирован"
                },
                {
                        banner()
                                .withStatusShow(false),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.MANUALLY_SUSPENDED),
                        "MANUALLY_SUSPENDED.1 баннер остановлен пользователем"
                },
                {
                        banner()
                                .withStatusMonitoringDomainStop(true),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.TEMPORARILY_SUSPENDED),
                        "TEMPORARILY_SUSPENDED.1 остановлен мониторингом доменов"
                },
                {
                        banner()
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true),

                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE),
                        "ACTIVE.1 - баннер синхронизирован с БК"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.NO)
                                .withPhoneFlag(GdiBannerStatusVCardModeration.YES),
                        status() //Todo(pashkus): нужна дополнительная информация, т.к баннер активен, но еще нет
                                // показов.
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE),
                        "ACTIVE.2.1 - новый баннер прошел модерацию, но еще не отправлен в БК (BsSync=No)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.SENDING)
                                .withPhoneFlag(GdiBannerStatusVCardModeration.YES),
                        status() //Todo(pashkus): нужна дополнительная информация, т.к баннер активен, но еще нет
                                // показов.
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE),
                        "ACTIVE.2.2 - новый баннер прошел модерацию, но еще не отправлен в БК (BsSync=Sending)"
                },
                {
                        banner()
                                .withStatusActive(true)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.NO),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withPreviousVersionShown(true),
                        "ACTIVE.3.1 - обновленный баннер прошел модерацию, но еще не отправлен в БК (BsSync=No)"

                },
                {
                        banner()
                                .withStatusActive(true)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.SENDING),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withPreviousVersionShown(true),
                        "ACTIVE.3.2 - обновленный баннер прошел модерацию, но еще не отправлен в БК (BsSync=Sending)"

                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withPhoneFlag(GdiBannerStatusVCardModeration.NO),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withHasInactiveResources(Set.of(GdiBannerResources.VCARD)),
                        "ACTIVE.4.1 - новый баннер частично промодерирован. Визитка отклонена. (BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.NO)
                                .withPhoneFlag(GdiBannerStatusVCardModeration.NO),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withHasInactiveResources(Set.of(GdiBannerResources.VCARD)),
                        "ACTIVE.4.2 - новый баннер частично промодерирован; визитка отклонена; (BsSync=No)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.SENDING)
                                .withPhoneFlag(GdiBannerStatusVCardModeration.NO),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withHasInactiveResources(Set.of(GdiBannerResources.VCARD)),
                        "ACTIVE.4.3 - новый баннер частично промодерирован; визитка отклонена; (BsSync=Sending)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withStatusSitelinksModerate(GdiBannerStatusSitelinksModerate.NEW),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE),
                        "ACTIVE.5.1 - обновленный баннер частично промодерирован. Сайтлинки в черновиках. (BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withStatusSitelinksModerate(GdiBannerStatusSitelinksModerate.SENT),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE),
                        "ACTIVE.5.2 - обновленный баннер частично промодерирован. Сайтлинки на модерации. (BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withStatusSitelinksModerate(GdiBannerStatusSitelinksModerate.NO),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withHasInactiveResources(Set.of(GdiBannerResources.SITELINKS)),
                        "ACTIVE.5.3 - обновленный баннер частично промодерирован. Сайтлинки отклонены. (BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.NO)
                                .withStatusActive(true)
                                .withStatusSitelinksModerate(GdiBannerStatusSitelinksModerate.NO),
                        status()
                                .withPreviousVersionShown(true)
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withHasInactiveResources(Set.of(GdiBannerResources.SITELINKS)),
                        "ACTIVE.5.3.1 - обновленный баннер частично промодерирован. Сайтлинки отклонены. (BsSync=No)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withBannerImage(new GdiBannerImage()
                                .withStatusModerate(GdiBannerBannerImagesStatusModerate.NO)),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withHasInactiveResources(Set.of(GdiBannerResources.BANNER_IMAGES)),
                        "ACTIVE.5.4 - обновленный баннер частично промодерирован. ТГО картинка отклонена. (BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withImage(new GdiImagesImage()
                                .withStatusModerate(GdiBannerImageStatusModerate.NO)),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withHasInactiveResources(Set.of(GdiBannerResources.IMAGES)),
                        "ACTIVE.5.5 - обновленный баннер частично промодерирован. Картинка отклонена. (BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withCreative(new GdiBannerCreative()
                                .withStatusModerate(GdiBannerBannersCreativeStatusModerate.NO)),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withHasInactiveResources(Set.of(GdiBannerResources.CREATIVES)),
                        "ACTIVE.5.6 - обновленный баннер частично промодерирован. Креатив отклонен. (BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withTurbolanding(new GdiBannerTurboLanding()
                                .withStatusModerate(BannerTurboLandingStatusModerate.NO)),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withHasInactiveResources(Set.of(GdiBannerResources.TURBOLANDINGS)),
                        "ACTIVE.5.4 - обновленный баннер частично промодерирован. Турболендинг отклонен. (BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withDisplayHref(new GdiBannerDisplayHref()
                                .withStatusModerate(GdiBannerBannersDisplayHrefStatusModerate.NO)),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withHasInactiveResources(Set.of(GdiBannerResources.DISPLAY_HREF)),
                        "ACTIVE.6.1 - обновленный баннер частично промодерирован. Отображаемая ссылка отклонена. " +
                                "(BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withDisplayHref(new GdiBannerDisplayHref()
                                .withStatusModerate(GdiBannerBannersDisplayHrefStatusModerate.SENT)),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE),
                        "ACTIVE.6.2 - обновленный баннер частично промодерирован. Отображаемая ссылка на модерации. " +
                                "(BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withPhoneFlag(GdiBannerStatusVCardModeration.NO),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withHasInactiveResources(Set.of(GdiBannerResources.VCARD)),
                        "ACTIVE.7.1 - обновленный баннер частично промодерирован. Визитка отклонена. " +
                                "(BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withPhoneFlag(GdiBannerStatusVCardModeration.SENT),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE),
                        "ACTIVE.7.2 - обновленный баннер частично промодерирован. Визитка на модерации. " +
                                "(BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withCallouts(List.of(new Callout().withStatusModerate(CalloutsStatusModerate.NO))),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE)
                                .withHasInactiveResources(Set.of(GdiBannerResources.CALLOUTS)),
                        "ACTIVE.8.1 - обновленный баннер частично промодерирован. Уточнение отклонено. " +
                                "(BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withCallouts(List.of(new Callout().withStatusModerate(CalloutsStatusModerate.SENT))),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.ACTIVE),
                        "ACTIVE.8.2 - обновленный баннер частично промодерирован. Уточнение на модерации. " +
                                "(BsSync=Yes)"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.NO),
                        status()
                                .withRejectedOnModeration(true)
                                .withPrimaryStatus(GdiBannerPrimaryStatus.MODERATION_REJECTED),
                        "MODERATION_REJECTED.1 - новый баннер отклонен на модерации."
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.NO),
                        status()
                                .withRejectedOnModeration(true)
                                .withPrimaryStatus(GdiBannerPrimaryStatus.MODERATION_REJECTED),
                        "MODERATION_REJECTED.2 - обновленный баннер отклонен на модерации. Идут показы пред. версии "
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.NO)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.REJECTED)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.NO)
                                .withStatusActive(true),
                        status()
                                .withRejectedOnModeration(true)
                                .withPreviousVersionShown(true) //Todo(pashkus):
                                .withPrimaryStatus(GdiBannerPrimaryStatus.MODERATION_REJECTED),
                        "MODERATION_REJECTED.3 - баннер отклонен на постмодерации. Идут показы пред. версии (но будут" +
                                " остановлены)"
                },
                {
                        banner()
                                .withBannerType(BannersBannerType.performance)
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withCreative(new GdiBannerCreative()
                                .withStatusModerate(GdiBannerBannersCreativeStatusModerate.NO)),
                        status()
                                .withRejectedOnModeration(true)
                                .withPrimaryStatus(GdiBannerPrimaryStatus.MODERATION_REJECTED)
                                .withHasInactiveResources(Set.of(GdiBannerResources.CREATIVES)),
                        "MODERATION_REJECTED.4 - баннер частично промодерирован. Креатив для смартов отклонен."
                },
                {
                        banner()
                                .withBannerType(BannersBannerType.image_ad)
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withCreative(new GdiBannerCreative()
                                .withStatusModerate(GdiBannerBannersCreativeStatusModerate.NO)),
                        status()
                                .withRejectedOnModeration(true)
                                .withPrimaryStatus(GdiBannerPrimaryStatus.MODERATION_REJECTED)
                                .withHasInactiveResources(Set.of(GdiBannerResources.CREATIVES)),
                        "MODERATION_REJECTED.5 - баннер частично промодерирован. Креатив для ГО отклонен."
                },
                {
                        banner()
                                .withBannerType(BannersBannerType.image_ad)
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withImage(new GdiImagesImage().withStatusModerate(GdiBannerImageStatusModerate.NO)),
                        status()
                                .withRejectedOnModeration(true)
                                .withPrimaryStatus(GdiBannerPrimaryStatus.MODERATION_REJECTED)
                                .withHasInactiveResources(Set.of(GdiBannerResources.IMAGES)),
                        "MODERATION_REJECTED.6 - баннер частично промодерирован. Картнка для ГО отклонена."
                },
                {
                        banner()
                                .withBannerType(BannersBannerType.mcbanner)
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withImage(new GdiImagesImage().withStatusModerate(GdiBannerImageStatusModerate.NO)),
                        status()
                                .withRejectedOnModeration(true)
                                .withPrimaryStatus(GdiBannerPrimaryStatus.MODERATION_REJECTED)
                                .withHasInactiveResources(Set.of(GdiBannerResources.IMAGES)),
                        "MODERATION_REJECTED.7 - баннер частично промодерирован. Картнка для MCBANNER отклонена."
                },
                {
                        banner()
                                .withBannerType(BannersBannerType.cpc_video)
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withCreative(new GdiBannerCreative()
                                .withStatusModerate(GdiBannerBannersCreativeStatusModerate.NO)),
                        status()
                                .withRejectedOnModeration(true)
                                .withPrimaryStatus(GdiBannerPrimaryStatus.MODERATION_REJECTED)
                                .withHasInactiveResources(Set.of(GdiBannerResources.CREATIVES)),
                        "MODERATION_REJECTED.8 - баннер частично промодерирован. Креатив для CPC_VIDEO отклонен."
                },
                {
                        banner()
                                .withBannerType(BannersBannerType.cpm_banner)
                                .withStatusModerate(GdiBannerStatusModerate.YES)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                                .withStatusBsSynced(GdiBannerStatusBsSynced.YES)
                                .withStatusActive(true)
                                .withCreative(new GdiBannerCreative()
                                .withStatusModerate(GdiBannerBannersCreativeStatusModerate.NO)),
                        status()
                                .withRejectedOnModeration(true)
                                .withPrimaryStatus(GdiBannerPrimaryStatus.MODERATION_REJECTED)
                                .withHasInactiveResources(Set.of(GdiBannerResources.CREATIVES)),
                        "MODERATION_REJECTED.8 - баннер частично промодерирован. Креатив для cmp отклонен."
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.SENDING)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.REJECTED),
                        //todo: посмотреть, так не бывает
                        status()
                                .withRejectedOnModeration(false)
                                .withPrimaryStatus(GdiBannerPrimaryStatus.MODERATION),
                        "MODERATION.1 - баннер отправлен на модерацию"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.READY)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.NO)
                                .withStatusSitelinksModerate(GdiBannerStatusSitelinksModerate.READY)
                                .withStatusActive(false),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.MODERATION),
                        "MODERATION.2 - новый баннер отправлен на модерацию"
                },
                {
                        banner()
                                .withStatusModerate(GdiBannerStatusModerate.NEW)
                                .withStatusPostModerate(GdiBannerStatusPostModerate.NO),
                        status()
                                .withPrimaryStatus(GdiBannerPrimaryStatus.DRAFT),
                        "DRAFT - баннер создан, но не отправлен на модерацию"
                },


        });
    }


    @Test
    public void testExtractStatus() {
        GdiBannerStatus status = GridBannerStatusUtils.extractBannerStatus(banner);

        assertThat(status)
                .is(matchedBy(beanDiffer(expectedStatus)));
    }
}
