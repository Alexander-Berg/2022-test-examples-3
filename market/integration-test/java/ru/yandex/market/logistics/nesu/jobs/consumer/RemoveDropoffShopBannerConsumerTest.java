package ru.yandex.market.logistics.nesu.jobs.consumer;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.model.RemoveDropoffShopBannerPayload;
import ru.yandex.market.partner.banners.client.api.TemplateBannersApi;
import ru.yandex.market.partner.banners.client.model.TemplatedBannersDeletePartnersRequestDto;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ParametersAreNonnullByDefault
@DisplayName("Удаление партнерского баннера на отключении связки с дропоффом")
class RemoveDropoffShopBannerConsumerTest extends AbstractContextualTest {

    private static final long SHOP_ID = 800L;

    @Autowired
    private TemplateBannersApi templateBannersApi;

    @Autowired
    private RemoveDropoffShopBannerConsumer removeDropoffShopBannerConsumer;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(templateBannersApi);
    }

    @Test
    @DisplayName("Отсутствуют данные для удаления баннеров")
    void noDataToRemove() {
        removeDropoffShopBannerConsumer.execute(createTask());
    }

    @Test
    @DisplayName("Партнерский баннер успешно удален")
    @DatabaseSetup("/jobs/consumer/remove_dropoff_shop_banner/before/dropoff_banner.xml")
    @ExpectedDatabase(
            value = "/jobs/consumer/remove_dropoff_shop_banner/after/dropoff_banner_removed.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successRemove() {
        removeDropoffShopBannerConsumer.execute(createTask());

        verify(templateBannersApi).deleteBannerPartnersPost(eq(
            new TemplatedBannersDeletePartnersRequestDto()
                .bannerIds(List.of("disabled-dropoff-banner-100"))
                .partnerIds(List.of(SHOP_ID))
                .templateId("disabled-dropoff-banner")
        ));
    }

    @Nonnull
    private Task<RemoveDropoffShopBannerPayload> createTask() {
        return new Task<>(
                new QueueShardId("1"),
                new RemoveDropoffShopBannerPayload(REQUEST_ID, SHOP_ID),
                1,
                clock.instant().atZone(DateTimeUtils.MOSCOW_ZONE),
                null,
                null
        );
    }

}
