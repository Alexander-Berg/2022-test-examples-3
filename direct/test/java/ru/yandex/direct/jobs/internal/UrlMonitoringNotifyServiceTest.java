package ru.yandex.direct.jobs.internal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Lists;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignSimple;
import ru.yandex.direct.core.entity.internalads.model.BannerUnreachableUrl;
import ru.yandex.direct.jobs.internal.model.StructureOfBannerIds;
import ru.yandex.direct.jobs.internal.model.StructureOfUnavailableBanners;
import ru.yandex.direct.jobs.internal.utils.InfoForUrlNotificationsGetter;
import ru.yandex.direct.mail.MailMessage;
import ru.yandex.direct.mail.MailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.direct.core.entity.internalads.repository.BannersUnreachableUrlYtRepository.createBannerUnreachableUrl;

@ParametersAreNonnullByDefault
public class UrlMonitoringNotifyServiceTest {

    @Mock
    private MailSender mailSender;

    @Spy
    @InjectMocks
    private UrlMonitoringNotifyService notifyService;

    @Captor
    private ArgumentCaptor<MailMessage> mailMessageCaptor;

    @BeforeEach
    void initTestData() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void checkNotifyBannersStopped_EmptyBanners() {
        notifyService.notifyBannersStopped(Collections.emptyList(), true);

        verifyZeroInteractions(mailSender);
    }

    @Test
    void checkNotifyBannersStopped_StopBannersEnabled() {
        notifyService.notifyBannersStopped(structureUnavailableBanners(getBannerUnreachableUrlsTestData()), true);

        checkBannersStopped(true);
    }

    @Test
    void checkNotifyBannersStopped_StopBannersDisabled() {
        notifyService.notifyBannersStopped(structureUnavailableBanners(getBannerUnreachableUrlsTestData()), false);

        checkBannersStopped(false);
    }

    @Test
    void checkBannersNotDisable_EmptyBanners() {
        notifyService.notifyBannersNotDisable(Collections.emptyList(),
                getIdsNotDisablePredicate(), getUrlsNotDisablePredicate(), getErrorsNotDisablePredicate());

        verifyZeroInteractions(mailSender);
    }

    @Test
    void checkBannersNotDisable() {
        notifyService.notifyBannersNotDisable(
                structureUnavailableBanners(getBannerUnreachableUrlsTestData()),
                getIdsNotDisablePredicate(), getUrlsNotDisablePredicate(), getErrorsNotDisablePredicate()
        );

        checkNotifyBanners(List.of(
                        "bannerId: 1322837, url: https://direct.ru, reason: 500" +
                                " - idsNotDisable=false, urlsNotDisable=true, errorsNotDisable=false",


                        "bannerId: 4, url: https://ya.ru, reason: 404" +
                                " - idsNotDisable=true, urlsNotDisable=true, errorsNotDisable=false",
                        "bannerId: 1322837, url: https://ya.ru, reason: 404" +
                                " - idsNotDisable=false, urlsNotDisable=true, errorsNotDisable=false",

                        "bannerId: 4, url: https://link.ru, reason: 404" +
                                " - idsNotDisable=true, urlsNotDisable=false, errorsNotDisable=false",
                        "bannerId: 5, url: https://link.ru, reason: 404" +
                                " - idsNotDisable=true, urlsNotDisable=false, errorsNotDisable=false",
                        "bannerId: 333, url: https://link.ru, reason: 404" +
                                " - idsNotDisable=false, urlsNotDisable=false, errorsNotDisable=false",

                        "bannerId: 999, url: https://sweets.ru, reason: 999" +
                                " - idsNotDisable=false, urlsNotDisable=false, errorsNotDisable=true",

                        "bannerId: 101, url: https://batman.ru, reason: 123" +
                                " - idsNotDisable=true, urlsNotDisable=true, errorsNotDisable=false"
                ),
                "Отфильтрованные баннеры:");
    }

    private void checkBannersStopped(boolean isStopBannersEnabled) {
        checkNotifyBanners(List.of(
                "bannerId: 1322837, url: https://direct.ru, reason: 500",

                "bannerId: 1322837, url: https://ya.ru, reason: 404",
                "bannerId: 4, url: https://ya.ru, reason: 404",

                "bannerId: 4, url: https://link.ru, reason: 404",
                "bannerId: 5, url: https://link.ru, reason: 404",
                "bannerId: 333, url: https://link.ru, reason: 404",

                "bannerId: 999, url: https://sweets.ru, reason: 999",

                "bannerId: 101, url: https://batman.ru, reason: 123"
        ), (isStopBannersEnabled ?
                "Остановлены баннеры с недоступными урлами" :
                "Остановлены баннеры с недоступными урлами (на самом деле нет)"
        ));
    }

    @Test
    void checkNotifyBannersEnabled_EmptyBanner() {
        notifyService.notifyBannersEnabled(Collections.emptyList());

        verifyZeroInteractions(mailSender);
    }

    @Test
    void checkNotifyBannersEnabled() {
        List<StructureOfBannerIds> structures = List.of(
                new StructureOfBannerIds(
                        2L, "camp2", Map.of(
                        22L, List.of(11L, 22L, 122221L)
                )),
                new StructureOfBannerIds(
                        1L, "camp1", Map.of(
                        2L, List.of(1L, 2L, 122L),
                        5L, List.of(4L, 5L)
                ))
        );

        notifyService.notifyBannersEnabled(structures);

        checkNotifyBanners(List.of(
                        "Кампания №1 \"camp1\"",
                        "Шаблон 2: 1, 2, 122",
                        "Шаблон 5: 4, 5",
                        "Кампания №2 \"camp2\"",
                        "Шаблон 22: 11, 22, 122221"
                ),
                "Включены баннеры, остановленные урл-мониторингом"
        );
    }

    @Test
    void checkOrderOfEnabledBanners() {
        List<StructureOfBannerIds> structures = List.of(
                new StructureOfBannerIds(
                        2L, "camp2", Map.of(
                        22L, List.of(11L, 3L, 22L)
                )),
                new StructureOfBannerIds(
                        3L, "camp3", Map.of(
                        123L, List.of(4L)
                )),
                new StructureOfBannerIds(
                        1L, "camp1", Map.of(
                        5L, List.of(4L, 5L),
                        2L, Lists.reverse(List.of(1L, 2L, 122L))
                ))
        );

        notifyService.notifyBannersEnabled(structures);

        checkOrder(List.of(
                "№1", "camp1", "Шаблон 2", "1, 2, 122", "Шаблон 5", "4, 5",
                "№2", "camp2", "Шаблон 22", "3", "11", "22",
                "№3", "camp3", "Шаблон 123", "4"
        ));
    }

    private void checkNotifyBanners(List<String> expectedMessageBody, String expectedSubject) {
        verify(mailSender).send(mailMessageCaptor.capture());

        assertThat(mailMessageCaptor.getValue().getMessageBody())
                .contains(expectedMessageBody);

        assertThat(mailMessageCaptor.getValue().getSubject())
                .isEqualTo(expectedSubject);
    }

    private void checkOrder(List<String> orderMarkers) {
        verify(mailSender).send(mailMessageCaptor.capture());
        Pattern orderedMessageBodyPattern = Pattern.compile(StreamEx.of(orderMarkers).joining("(.|\n)*"));
        assertThat(mailMessageCaptor.getValue().getMessageBody())
                .containsPattern(orderedMessageBodyPattern);
    }

    private static List<BannerUnreachableUrl> getBannerUnreachableUrlsTestData() {
        return List.of(
                createBannerUnreachableUrl(1322837L, "https://direct.ru", "500"),

                createBannerUnreachableUrl(1322837L, "https://ya.ru", "404"),
                createBannerUnreachableUrl(4L, "https://ya.ru", "404"),

                createBannerUnreachableUrl(5L, "https://link.ru", "404"),
                createBannerUnreachableUrl(4L, "https://link.ru", "404"),
                createBannerUnreachableUrl(333L, "https://link.ru", "404"),

                createBannerUnreachableUrl(999L, "https://sweets.ru", "999"),

                createBannerUnreachableUrl(101L, "https://batman.ru", "123")
        );
    }

    private static Map<Long, CampaignSimple> getCampaignsTestData() {
        return StreamEx.of(List.of(
                createCampaign(11L, "camp11"),
                createCampaign(22L, "camp22"),
                createCampaign(33L, "camp33")
        )).toMap(CampaignSimple::getId, Function.identity());
    }

    private static Map<Long, Map<Long, List<Long>>> getStructureOfBanners() {
        return Map.of(
                11L, Map.of(
                        1L, List.of(1322837L, 333L, 4L),
                        2L, List.of(5L)
                ),
                22L, Map.of(
                        4L, List.of(999L)
                ),
                33L, Map.of(
                        5L, List.of(101L)
                )
        );
    }

    private static Predicate<BannerUnreachableUrl> getIdsNotDisablePredicate() {
        Set<Long> notDisableIds = Set.of(4L, 5L, 101L);
        return banner -> notDisableIds.contains(banner.getId());
    }

    private static Predicate<BannerUnreachableUrl> getUrlsNotDisablePredicate() {
        Set<String> notDisableUrls = Set.of("https://ya.ru", "direct", "bat");
        return banner -> StreamEx.of(notDisableUrls)
                .anyMatch(url -> banner.getUrl().contains(url));
    }

    private static Predicate<BannerUnreachableUrl> getErrorsNotDisablePredicate() {
        return banner -> banner.equals(createBannerUnreachableUrl(999L, "https://sweets.ru", "999"));
    }

    private static List<StructureOfUnavailableBanners> structureUnavailableBanners(List<BannerUnreachableUrl> banners) {
        Set<Long> ids = StreamEx.of(banners).map(BannerUnreachableUrl::getId).toSet();
        List<InternalBanner> internalBanners = getInternalBanners(ids);
        Map<Long, String> campaignNames =
                EntryStream.of(getCampaignsTestData()).mapValues(CampaignSimple::getName).toMap();
        List<StructureOfBannerIds> idStructures =
                InfoForUrlNotificationsGetter.structureBanners(internalBanners, campaignNames);
        Map<Long, List<BannerUnreachableUrl>> bannerUrlsByBannerId = StreamEx.of(banners)
                .mapToEntry(BannerUnreachableUrl::getId, Function.identity())
                .grouping();
        return StreamEx.of(idStructures)
                .map(structure -> new StructureOfUnavailableBanners(
                        structure.getCampaignId(),
                        structure.getCampaignName(),
                        InfoForUrlNotificationsGetter.replaceBannerIdsWithUrls(structure.getBannersByTemplateId(),
                                bannerUrlsByBannerId)
                ))
                .toList();
    }

    private static List<InternalBanner> getInternalBanners(Set<Long> targetIds) {
        Map<Long, Map<Long, List<Long>>> structure = getStructureOfBanners();
        LinkedList<InternalBanner> internalBanners = new LinkedList<>();

        EntryStream.of(structure)
                .forKeyValue((campaignId, bannerIdsByTemplateId) -> EntryStream.of(bannerIdsByTemplateId)
                        .forKeyValue((templateId, bannerIds) -> StreamEx.of(bannerIds)
                                .forEach(id -> {
                                    if (!targetIds.contains(id)) {
                                        return;
                                    }
                                    internalBanners.add(createInternalBanner(id, campaignId, templateId));
                                    targetIds.remove(id);
                                })));
        return internalBanners;
    }

    private static CampaignSimple createCampaign(Long id, String name) {
        return new Campaign()
                .withId(id)
                .withName(name);
    }

    private static InternalBanner createInternalBanner(long id, long campaignId, long tempalteId) {
        return new InternalBanner()
                .withId(id)
                .withCampaignId(campaignId)
                .withTemplateId(tempalteId);
    }
}
