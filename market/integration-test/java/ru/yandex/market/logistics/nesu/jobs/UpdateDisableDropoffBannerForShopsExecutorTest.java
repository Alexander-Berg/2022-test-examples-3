package ru.yandex.market.logistics.nesu.jobs;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.configuration.properties.PartnerBannersProperties;
import ru.yandex.market.logistics.nesu.jobs.executor.UpdateDisableDropoffBannerForShopsExecutor;
import ru.yandex.market.logistics.nesu.service.dropoff.AbstractDisablingSubtaskTest;
import ru.yandex.market.logistics.nesu.utils.CommonsConstants;
import ru.yandex.market.partner.banners.client.api.TemplateBannersApi;
import ru.yandex.market.partner.banners.client.model.TemplatedBannersDeleteRequestDto;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Проверка работы экзекутора обновления таблицы баннеров.")
class UpdateDisableDropoffBannerForShopsExecutorTest extends AbstractDisablingSubtaskTest {

    @Autowired
    private UpdateDisableDropoffBannerForShopsExecutor executor;

    @Autowired
    private TemplateBannersApi templateBannersApi;

    @Autowired
    private PartnerBannersProperties partnerBannersProperties;

    @BeforeEach
    void onSetup() {
        clock.setFixed(Instant.parse("2021-12-25T17:00:00Z"), CommonsConstants.MSK_TIME_ZONE);
    }

    @AfterEach
    void onFinish() {
        verifyNoMoreInteractions(templateBannersApi);
    }

    @Test
    @DisplayName("Обновление таблицы баннеров.")
    @DatabaseSetup("/jobs/executors/update_disable_dropoff_banner_for_shops/before.xml")
    @ExpectedDatabase(
        value = "/jobs/executors/update_disable_dropoff_banner_for_shops/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateBannerTable() {
        executor.doJob(null);
        verify(templateBannersApi).deleteBannersPost(eq(expectedBannerRequest()));
    }

    @Test
    @DisplayName("Обновление таблицы баннеров без синхронизации с partner-banners.")
    @DatabaseSetup("/jobs/executors/update_disable_dropoff_banner_for_shops/before.xml")
    @ExpectedDatabase(
            value = "/jobs/executors/update_disable_dropoff_banner_for_shops/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateBannerTableNoInteractionWithPartnerBanners() {
        boolean prevDropoffSync = partnerBannersProperties.isDropoffSync();
        partnerBannersProperties.setDropoffSync(false);
        try {
            executor.doJob(null);
        } finally {
            partnerBannersProperties.setDropoffSync(prevDropoffSync);
        }
    }

    @Test
    @DisplayName("Обновление таблицы баннеров, нет затронутых магазинов удалены все заявки.")
    @DatabaseSetup("/jobs/executors/update_disable_dropoff_banner_for_shops/before_no_affected_shops.xml")
    @ExpectedDatabase(
        value = "/jobs/executors/update_disable_dropoff_banner_for_shops/after_no_affected_shops.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateBannerTableNoAffectedShops() {
        executor.doJob(null);
        verify(templateBannersApi).deleteBannersPost(eq(expectedBannerRequest()));
    }

    @Test
    @DisplayName("Обновление таблицы баннеров, нет заявок.")
    @DatabaseSetup("/jobs/executors/update_disable_dropoff_banner_for_shops/empty.xml")
    @ExpectedDatabase(
        value = "/jobs/executors/update_disable_dropoff_banner_for_shops/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateBannerTableNoDisablingRequestNoInteractionWithLms() {
        executor.doJob(null);
    }

    private TemplatedBannersDeleteRequestDto expectedBannerRequest() {
        return new TemplatedBannersDeleteRequestDto()
                .templateId("disabled-dropoff-banner")
                .bannerIds(List.of("disabled-dropoff-banner-1", "disabled-dropoff-banner-3"));
    }

}
