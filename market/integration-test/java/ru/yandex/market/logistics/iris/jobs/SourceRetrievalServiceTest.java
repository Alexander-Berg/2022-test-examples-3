package ru.yandex.market.logistics.iris.jobs;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.iris.jobs.JobsUtils.reflectiveCacheEvict;
import static ru.yandex.market.logistics.management.entity.type.PartnerStatus.ACTIVE;
import static ru.yandex.market.logistics.management.entity.type.PartnerStatus.INACTIVE;
import static ru.yandex.market.logistics.management.entity.type.PartnerStatus.TESTING;

public class SourceRetrievalServiceTest extends AbstractContextualTest {

    @Autowired
    private SourceRetrievalService sourceRetrievalService;

    @After
    public void clearCache() {
        reflectiveCacheEvict(sourceRetrievalService);
    }

    /**
     * Проверяет, что мы единожды обращаемся к сервису доставки при пересчете кэша.
     */
    @Test
    public void touchDeliveryOnlyOnceOnRecompute() {
        doReturn(Collections.singletonList(getPartnerResponse()))
            .when(lmsClient).searchPartners(any(SearchPartnerFilter.class));

        sourceRetrievalService.recomputeCache();
        verify(lmsClient, times(1)).searchPartners(any(SearchPartnerFilter.class));
    }

    /**
     * Проверяет, что при наличии кэша мы не будем обращаться лишний раз в сервис доставки.
     */
    @Test
    public void doNotTouchDeliveryIfWeHaveOkInfo() {
        doReturn(Collections.singletonList(getPartnerResponse()))
            .when(lmsClient).searchPartners(any(SearchPartnerFilter.class));

        sourceRetrievalService.recomputeCache();
        assertSourcesArePresent(sourceRetrievalService.getActiveSources());
        assertSourcesArePresent(sourceRetrievalService.getActiveSources());
        verify(lmsClient).searchPartners(any(SearchPartnerFilter.class));
    }

    /**
     * Проверяет что закешируются только склады с включенным синком.
     */
    @Test
    public void retrieveOnlySyncEnabledFulfillments() {
        doReturn(Arrays.asList(
            getPartnerResponse(),
            PartnerResponse.newBuilder()
                .id(2L)
                .partnerType(PartnerType.DROPSHIP)
                .name("Dropship")
                .stockSyncEnabled(false)
                .build()
        ))
            .when(lmsClient).searchPartners(any(SearchPartnerFilter.class));

        sourceRetrievalService.init();
        assertions().assertThat(sourceRetrievalService.getActiveSources())
            .hasSize(1)
            .containsOnly(Source.of("1", SourceType.WAREHOUSE));
        verify(lmsClient).searchPartners(any(SearchPartnerFilter.class));
    }

    /**
     * Проверяет что с null stockSyncEnabled and autoSwitchStockSync не закешируются
     */
    @Test
    public void retrieveOnlySyncEnabledNotNull() {
        doReturn(ImmutableList.of(
            getPartnerResponse(),
            PartnerResponse.newBuilder()
                .id(2L)
                .partnerType(PartnerType.DROPSHIP)
                .name("Dropship")
                .stockSyncEnabled(true)
                .build(),
            PartnerResponse.newBuilder()
                .id(3L)
                .partnerType(PartnerType.FULFILLMENT)
                .name("Fulfillment")
                .autoSwitchStockSyncEnabled(true)
                .build()
        ))
            .when(lmsClient).searchPartners(any(SearchPartnerFilter.class));

        sourceRetrievalService.init();
        assertions().assertThat(sourceRetrievalService.getActiveSources())
            .hasSize(1)
            .containsOnly(Source.of("1", SourceType.WAREHOUSE));
    }

    /**
     * Проверяет, что если была успешная попытка обратиться за Source в сервис доставки, то даже, если последняя падает,
     * вернутся последние успешные результаты.
     */
    @Test
    public void useLastOKCache() {
        doReturn(Collections.singletonList(getPartnerResponse()))
            .when(lmsClient).searchPartners(any(SearchPartnerFilter.class));

        sourceRetrievalService.init();
        assertSourcesArePresent(sourceRetrievalService.getActiveSources());
        verify(lmsClient, times(1)).searchPartners(any(SearchPartnerFilter.class));
        doThrow(new RuntimeException("Error getting sources")).when(lmsClient).searchPartners(any(SearchPartnerFilter.class));
        sourceRetrievalService.init();
        assertSourcesArePresent(sourceRetrievalService.getActiveSources());
        verify(lmsClient).searchPartners(any(SearchPartnerFilter.class));
    }

    /**
     * Проверяет что не неактивные партнеры с включенным синком будут отфильтрованы
     */
    @Test
    public void retrieveOnlyActive() {
        doReturn(ImmutableList.of(
                getPartnerResponse(),
                PartnerResponse.newBuilder()
                        .id(2L)
                        .partnerType(PartnerType.DROPSHIP)
                        .name("Dropship")
                        .korobyteSyncEnabled(true)
                        .status(TESTING)
                        .build(),
                PartnerResponse.newBuilder()
                        .id(3L)
                        .partnerType(PartnerType.FULFILLMENT)
                        .name("Fulfillment")
                        .korobyteSyncEnabled(true)
                        .status(INACTIVE)
                        .build()
        ))
                .when(lmsClient).searchPartners(any(SearchPartnerFilter.class));

        sourceRetrievalService.init();
        assertions().assertThat(sourceRetrievalService.getActiveSources())
                .hasSize(2)
                .containsOnly(Source.of("1", SourceType.WAREHOUSE), Source.of("2", SourceType.WAREHOUSE));
    }

    private void assertSourcesArePresent(Set<Source> sources) {
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(sources).isNotEmpty();
            softAssertions.assertThat(sources.iterator().next()).isNotNull();
        });
    }

    private PartnerResponse getPartnerResponse() {
        return PartnerResponse.newBuilder()
            .id(1L)
            .partnerType(PartnerType.FULFILLMENT)
            .name("MARKET_ROSTOV")
            .status(ACTIVE)
            .korobyteSyncEnabled(true)
            .build();
    }
}
