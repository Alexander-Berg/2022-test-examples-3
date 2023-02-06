package ru.yandex.direct.core.entity.mobilecontent.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import one.util.streamex.LongStreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncItem;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncPriority;
import ru.yandex.direct.core.entity.bs.resync.queue.repository.BsResyncQueueRepository;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.StatusIconModerate;
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository;
import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.entity.notification.container.MobileContentMonitoringNotification;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.misc.dataSize.DataSize;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.mobilecontent.service.UpdateMobileContentService.RESYNC_TO_BS_CHUNK_SIZE;
import static ru.yandex.direct.core.entity.mobilecontent.service.UpdateMobileContentService.UNKNOWN_APP_NAME;

@CoreTest
@RunWith(MockitoJUnitRunner.class)
public class UpdateMobileContentServiceTest {
    private static final long CONTENT_ID = 15L;
    private static final long UID = 42L;
    private static final long CAMPAIGN_ID = 1L;
    private static final String SUPPORTED_NAME = "name";
    private static final String SUPPORTED_NEW_NAME = "newname";
    private static final String UNSUPPORTED_NAME = "\uD86E\uDC28";
    @Mock
    private MobileContentRepository mobileContentRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BsResyncQueueRepository bsResyncQueueRepository;
    @InjectMocks
    private UpdateMobileContentService service;

    private MobileContent mobileContent;
    private List<AppliedChanges<MobileContent>> changes;

    @Before
    public void setUp() {
        this.mobileContent = createMobileContent();
        ModelChanges<MobileContent> mc = new ModelChanges<>(CONTENT_ID, MobileContent.class);
        mc.process(true, MobileContent.IS_AVAILABLE);
        mc.process("3.1", MobileContent.MIN_OS_VERSION);
        changes = Collections.singletonList(mc.applyTo(this.mobileContent));
    }

    @Test
    public void sendAvailabilityNotifications() {
        MobileContentMonitoringNotification notification = new MobileContentMonitoringNotification(UID,
                "test",
                MobileContentMonitoringNotification.State.ALIVE,
                Collections.singletonList(MobileContentMonitoringNotification.Record.builder()
                        .withCampaignId(CAMPAIGN_ID)
                        .withFio("test")
                        .withMobileContentId(CONTENT_ID)
                        .withStoreContentHref("test")
                        .withUid(UID)
                        .build()));
        when(mobileContentRepository
                .getContentAvailabilityNotifications(anyInt(), eq(singletonMap(CONTENT_ID, true))))
                .thenReturn(Collections.singletonList(notification));
        doNothing().when(notificationService).addNotification(notification);
        service.sendAvailabilityNotifications(1, changes);

        verify(mobileContentRepository, atLeastOnce())
                .getContentAvailabilityNotifications(eq(1), eq(singletonMap(CONTENT_ID, true)));
        verify(notificationService, atLeastOnce()).addNotification(notification);
    }

    @Test
    public void resyncChangesToBs() {
        BsResyncItem item = new BsResyncItem(BsResyncPriority.ON_MOBILE_CONTENT_CHANGED.value(),
                CAMPAIGN_ID, 2L, 3L);
        when(mobileContentRepository.getMobileContentForResync(anyInt(), eq(singletonList(CONTENT_ID))))
                .thenReturn(singletonList(item));
        when(bsResyncQueueRepository.addToResync(anyInt(), eq(singletonList(item)))).thenReturn(1);
        service.resyncChangesToBs(1, changes);

        verify(mobileContentRepository, atLeastOnce())
                .getMobileContentForResync(eq(1), eq(singletonList(CONTENT_ID)));
        verify(bsResyncQueueRepository, atLeastOnce())
                .addToResync(eq(1), eq(singletonList(item)));
    }

    @Test
    public void resyncChangesToBs_UpdatedByChunks() {
        List<BsResyncItem> itemsToUpdate = LongStreamEx.range(RESYNC_TO_BS_CHUNK_SIZE + 1)
                .mapToObj(l -> new BsResyncItem(BsResyncPriority.ON_MOBILE_CONTENT_CHANGED.value(),
                        CAMPAIGN_ID, l, 3L))
                .toList();
        when(mobileContentRepository.getMobileContentForResync(anyInt(), eq(singletonList(CONTENT_ID))))
                .thenReturn(itemsToUpdate);
        when(bsResyncQueueRepository.addToResync(anyInt(), anyCollection())).thenReturn(1);
        service.resyncChangesToBs(1, changes);

        verify(mobileContentRepository, atLeastOnce())
                .getMobileContentForResync(eq(1), eq(singletonList(CONTENT_ID)));
        verify(bsResyncQueueRepository, times(2))
                .addToResync(eq(1), anyCollection());
    }

    @Test
    public void collectChanges_WithoutChanges() {
        YTreeMapNode key = MobileContentYtHelper.createLookupKey("test", "ru");

        Multimap<YTreeMapNode, MobileContent> orig = ArrayListMultimap.create();
        orig.put(key, createMobileContent().withIsAvailable(true));
        Map<YTreeMapNode, MobileContent> fromYt = singletonMap(key, createMobileContent());

        List<AppliedChanges<MobileContent>> result = service.collectChanges(orig, fromYt, LocalDateTime.now());

        assertThat(result).hasSize(1);
        SoftAssertions.assertSoftly(softly -> {
            var changes = result.get(0);
            softly.assertThat(changes.changed(MobileContent.STORE_REFRESH_TIME))
                    .as("changed STORE_REFRESH_TIME").isTrue();
            softly.assertThat(changes.changed(MobileContent.MODIFY_TIME))
                    .as("changed MODIFY_TIME").isFalse();
            softly.assertThat(changes.getNewValue(MobileContent.STATUS_BS_SYNCED))
                    .as("value of STATUS_BS_SYNCED").isEqualTo(StatusBsSynced.YES);
            softly.assertThat(changes.getNewValue(MobileContent.STATUS_ICON_MODERATE))
                    .as("value of STATUS_ICON_MODERATE").isEqualTo(StatusIconModerate.YES);
            softly.assertThat(changes.getNewValue(MobileContent.IS_AVAILABLE))
                    .as("value of IS_AVAILABLE").isTrue();
        });
    }

    @Test
    public void collectChanges_WithoutSignificantChanges() {
        YTreeMapNode key = MobileContentYtHelper.createLookupKey("test", "ru");

        Multimap<YTreeMapNode, MobileContent> orig = ArrayListMultimap.create();
        orig.put(key, createMobileContent().withIsAvailable(true).withAppSize(DataSize.fromMegaBytes(3)));
        Map<YTreeMapNode, MobileContent> fromYt = singletonMap(key,
                createMobileContent().withAppSize(DataSize.fromMegaBytes(2)));

        List<AppliedChanges<MobileContent>> result = service.collectChanges(orig, fromYt, LocalDateTime.now());

        assertThat(result).hasSize(1);
        SoftAssertions.assertSoftly(softly -> {
            var changes = result.get(0);
            softly.assertThat(changes.changed(MobileContent.STORE_REFRESH_TIME))
                    .as("changed STORE_REFRESH_TIME").isTrue();
            softly.assertThat(changes.changed(MobileContent.MODIFY_TIME))
                    .as("changed MODIFY_TIME").isTrue();
            softly.assertThat(changes.getNewValue(MobileContent.STATUS_BS_SYNCED))
                    .as("value of STATUS_BS_SYNCED").isEqualTo(StatusBsSynced.YES);
            softly.assertThat(changes.getNewValue(MobileContent.STATUS_ICON_MODERATE))
                    .as("value of STATUS_ICON_MODERATE").isEqualTo(StatusIconModerate.YES);
            softly.assertThat(changes.getNewValue(MobileContent.IS_AVAILABLE))
                    .as("value of IS_AVAILABLE").isTrue();
        });
    }

    @Test
    public void collectChanges_StatusIconModerate() {
        String newIconHash = "xx3xx2x4xx62";

        YTreeMapNode key = MobileContentYtHelper.createLookupKey("test", "ru");

        Multimap<YTreeMapNode, MobileContent> orig = ArrayListMultimap.create();
        orig.put(key, createMobileContent().withIsAvailable(true));
        Map<YTreeMapNode, MobileContent> fromYt = singletonMap(key, createMobileContent().withIconHash(newIconHash));

        List<AppliedChanges<MobileContent>> result = service.collectChanges(orig, fromYt, LocalDateTime.now());

        assertThat(result).hasSize(1);
        SoftAssertions.assertSoftly(softly -> {
            var changes = result.get(0);
            softly.assertThat(changes.changed(MobileContent.STORE_REFRESH_TIME))
                    .as("changed STORE_REFRESH_TIME").isTrue();
            softly.assertThat(changes.changed(MobileContent.MODIFY_TIME))
                    .as("changed MODIFY_TIME").isTrue();
            softly.assertThat(changes.getNewValue(MobileContent.STATUS_BS_SYNCED))
                    .as("value of STATUS_BS_SYNCED").isEqualTo(StatusBsSynced.YES);
            softly.assertThat(changes.getNewValue(MobileContent.STATUS_ICON_MODERATE))
                    .as("value of STATUS_ICON_MODERATE").isEqualTo(StatusIconModerate.READY);
            softly.assertThat(changes.getNewValue(MobileContent.IS_AVAILABLE))
                    .as("value of IS_AVAILABLE").isTrue();
        });
    }

    @Test
    public void collectChanges_StatusBsSynced() {
        String newMinOsVersion = "5.0";

        YTreeMapNode key = MobileContentYtHelper.createLookupKey("test", "ru");

        Multimap<YTreeMapNode, MobileContent> orig = ArrayListMultimap.create();
        orig.put(key, createMobileContent().withIsAvailable(true));
        Map<YTreeMapNode, MobileContent> fromYt = singletonMap(key,
                createMobileContent().withMinOsVersion(newMinOsVersion));

        List<AppliedChanges<MobileContent>> result = service.collectChanges(orig, fromYt, LocalDateTime.now());

        assertThat(result).hasSize(1);
        SoftAssertions.assertSoftly(softly -> {
            var changes = result.get(0);
            softly.assertThat(changes.changed(MobileContent.STORE_REFRESH_TIME))
                    .as("changed STORE_REFRESH_TIME").isTrue();
            softly.assertThat(changes.changed(MobileContent.MODIFY_TIME))
                    .as("changed MODIFY_TIME").isTrue();
            softly.assertThat(changes.getNewValue(MobileContent.STATUS_BS_SYNCED))
                    .as("value of STATUS_BS_SYNCED").isEqualTo(StatusBsSynced.NO);
            softly.assertThat(changes.getNewValue(MobileContent.STATUS_ICON_MODERATE))
                    .as("value of STATUS_ICON_MODERATE").isEqualTo(StatusIconModerate.YES);
            softly.assertThat(changes.getNewValue(MobileContent.IS_AVAILABLE))
                    .as("value of IS_AVAILABLE").isTrue();
        });
    }

    @Test
    public void collectChanges_ChangeDownloads() {
        YTreeMapNode key = MobileContentYtHelper.createLookupKey("test", "ru");

        Multimap<YTreeMapNode, MobileContent> orig = ArrayListMultimap.create();
        orig.put(key, createMobileContent().withIsAvailable(true));
        Map<YTreeMapNode, MobileContent> fromYt = singletonMap(key, createMobileContent().withDownloads(100L));

        List<AppliedChanges<MobileContent>> result = service.collectChanges(orig, fromYt, LocalDateTime.now());

        assertThat(result).hasSize(1);
        SoftAssertions.assertSoftly(softly -> {
            var changes = result.get(0);
            softly.assertThat(changes.changed(MobileContent.DOWNLOADS))
                    .as("changed DOWNLOADS").isTrue();
            softly.assertThat(changes.changed(MobileContent.STORE_REFRESH_TIME))
                    .as("changed STORE_REFRESH_TIME").isTrue();
            softly.assertThat(changes.changed(MobileContent.MODIFY_TIME))
                    .as("changed MODIFY_TIME").isTrue();
            softly.assertThat(changes.getNewValue(MobileContent.STATUS_BS_SYNCED))
                    .as("value of STATUS_BS_SYNCED").isEqualTo(StatusBsSynced.NO);
        });
    }

    @Test
    public void collectChanges_ChangeScreens() {
        YTreeMapNode key = MobileContentYtHelper.createLookupKey("test", "ru");

        Multimap<YTreeMapNode, MobileContent> orig = ArrayListMultimap.create();
        orig.put(key, createMobileContent().withIsAvailable(true));
        Map<YTreeMapNode, MobileContent> fromYt = singletonMap(key, createMobileContent().withScreens(
                List.of(Map.of("width", "100", "path", "bbbb", "height", "100"))
        ));

        List<AppliedChanges<MobileContent>> result = service.collectChanges(orig, fromYt, LocalDateTime.now());

        assertThat(result).hasSize(1);
        SoftAssertions.assertSoftly(softly -> {
            var changes = result.get(0);
            softly.assertThat(changes.changed(MobileContent.SCREENS))
                    .as("changed SCREENS").isTrue();
            softly.assertThat(changes.changed(MobileContent.STORE_REFRESH_TIME))
                    .as("changed STORE_REFRESH_TIME").isTrue();
            softly.assertThat(changes.changed(MobileContent.MODIFY_TIME))
                    .as("changed MODIFY_TIME").isTrue();
            softly.assertThat(changes.getNewValue(MobileContent.STATUS_BS_SYNCED))
                    .as("value of STATUS_BS_SYNCED").isEqualTo(StatusBsSynced.NO);
        });
    }

    @Test
    public void ignoreUnsupportedNames_correctNewName_setsNewName() {
        changes.get(0).modify(MobileContent.NAME, SUPPORTED_NEW_NAME);
        service.ignoreUnsupportedNames(changes);
        assertEquals(SUPPORTED_NEW_NAME, changes.get(0).getNewValue(MobileContent.NAME));
    }

    @Test
    public void ignoreUnsupportedNames_incorrectNewName_revertsToOldName() {
        changes.get(0).modify(MobileContent.NAME, UNSUPPORTED_NAME);
        service.ignoreUnsupportedNames(changes);
        assertEquals(SUPPORTED_NAME, changes.get(0).getNewValue(MobileContent.NAME));
    }

    @Test
    public void ignoreUnsupportedNames_incorrectNewName_setsUnknownAppName() {
        mobileContent = new MobileContent().withId(CONTENT_ID).withName(null);
        ModelChanges<MobileContent> mc = new ModelChanges<>(CONTENT_ID, MobileContent.class);
        mc.process(UNSUPPORTED_NAME, MobileContent.NAME);
        changes = Collections.singletonList(mc.applyTo(mobileContent));
        service.ignoreUnsupportedNames(changes);
        assertEquals(UNKNOWN_APP_NAME, changes.get(0).getNewValue(MobileContent.NAME));
    }

    private MobileContent createMobileContent() {
        return new MobileContent()
                .withId(CONTENT_ID)
                .withName(SUPPORTED_NAME)
                .withMinOsVersion("3.0")
                .withStatusBsSynced(StatusBsSynced.YES)
                .withStatusIconModerate(StatusIconModerate.YES)
                .withTriesCount(0);
    }
}
