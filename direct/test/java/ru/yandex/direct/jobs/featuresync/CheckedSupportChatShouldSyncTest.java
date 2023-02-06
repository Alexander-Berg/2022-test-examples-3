package ru.yandex.direct.jobs.featuresync;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.feature.service.FeatureCache;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtOperator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CheckedSupportChatShouldSyncTest {
    CheckedSupportChatSyncJob job;
    PpcProperty<LocalDateTime> property;
    YtOperator ytOperator = mock(YtOperator.class);

    @SuppressWarnings("unchecked")
    @BeforeEach
    void init() {
        YtProvider ytProvider = mock(YtProvider.class);
        property = mock(PpcProperty.class); // можно было бы объявить сразу, но нужен @SuppressWarnings("unchecked")
        ShardHelper helper = mock(ShardHelper.class);
        FeatureCache featureCache = mock(FeatureCache.class);
        FeatureManagingService service = mock(FeatureManagingService.class);
        job = new CheckedSupportChatSyncJob(ytProvider, property, helper, featureCache, service);
    }

    @Test
    void noTable_shouldNot() {
        when(ytOperator.exists(any())).thenReturn(false);
        boolean answer = job.shouldSync(LocalDateTime.now().minusDays(5L), ytOperator);
        assertFalse(answer);
    }

    @Test
    void noModificationTime_shouldNot() {
        when(ytOperator.exists(any())).thenReturn(true);
        when(ytOperator.readTableRowCount(any())).thenReturn(50_000L);
        LocalDateTime localDateTime = LocalDateTime.now().minusHours(3L);
        when(ytOperator.readTableModificationTime(any())).thenReturn("");
        boolean answer = job.shouldSync(localDateTime, ytOperator);
        assertFalse(answer);
    }

    @Test
    void notEnoughRows_shouldNot() {
        when(ytOperator.exists(any())).thenReturn(true);
        when(ytOperator.readTableRowCount(any())).thenReturn(50L);
        when(ytOperator.readTableModificationTime(any()))
                .thenReturn(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + 'Z');
        boolean answer = job.shouldSync(LocalDateTime.now().minusDays(5L), ytOperator);
        assertFalse(answer);
    }

    @Test
    void synced_shouldNot() { // если по каким-то причинам время модификации оказалось раньше, чем при прошлом чтении
        when(ytOperator.exists(any())).thenReturn(true);
        when(ytOperator.readTableRowCount(any())).thenReturn(50_000L);
        LocalDateTime localDateTime = LocalDateTime.now().minusHours(3L);
        when(ytOperator.readTableModificationTime(any()))
                .thenReturn(localDateTime.minusHours(1L).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + 'Z');
        boolean answer = job.shouldSync(localDateTime, ytOperator);
        assertFalse(answer);
    }

    @Test
    void syncedExactly_shouldNot() {
        when(ytOperator.exists(any())).thenReturn(true);
        when(ytOperator.readTableRowCount(any())).thenReturn(50_000L);
        LocalDateTime localDateTime = LocalDateTime.now().minusHours(3L);
        when(ytOperator.readTableModificationTime(any()))
                .thenReturn(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + 'Z');
        boolean answer = job.shouldSync(localDateTime, ytOperator);
        assertFalse(answer);
    }

    @Test
    void oldSync_should() {
        when(ytOperator.exists(any())).thenReturn(true);
        when(ytOperator.readTableRowCount(any())).thenReturn(50_000L);
        when(ytOperator.readTableModificationTime(any()))
                .thenReturn(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + 'Z');
        boolean answer = job.shouldSync(LocalDateTime.now().minusDays(5L), ytOperator);
        assertTrue(answer);
    }
}
