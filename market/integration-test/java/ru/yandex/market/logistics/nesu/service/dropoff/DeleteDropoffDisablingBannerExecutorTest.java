package ru.yandex.market.logistics.nesu.service.dropoff;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.partner.banners.client.api.TemplateBannersApi;
import ru.yandex.market.partner.banners.client.model.TemplatedBannersDeleteRequestDto;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DisplayName("Тест на выполнение подзадачи отключения дропоффа. Удаление баннера.")
public class DeleteDropoffDisablingBannerExecutorTest extends AbstractDisablingSubtaskTest {

    @Autowired
    private TemplateBannersApi templateBannersApi;

    @Nonnull
    private TemplatedBannersDeleteRequestDto getRequest(Long requestId) {
        return new TemplatedBannersDeleteRequestDto()
            .templateId(partnerBannersProperties.getDropoffTemplateId())
            .bannerIds(List.of(partnerBannersProperties.getDropoffBannerId(requestId)));
    }

    @Test
    @DatabaseSetup(value = "/service/dropoff/before/delete_banner.xml")
    @ExpectedDatabase(
        value = "/service/dropoff/after/delete_banner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное удаление баннера.")
    void successCreateBanner() {
        boolean sync = partnerBannersProperties.isDropoffSync();
        partnerBannersProperties.setDropoffSync(true);
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));

        verify(templateBannersApi).deleteBannersPost(
            eq(getRequest(
                100L
            ))
        );

        partnerBannersProperties.setDropoffSync(sync);
    }
}
