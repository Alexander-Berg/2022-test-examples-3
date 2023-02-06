package ru.yandex.market.logistics.nesu.service.dropoff;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.partner.banners.client.api.TemplateBannersApi;
import ru.yandex.market.partner.banners.client.model.TemplateBannerRequestDisplayConditionsDto;
import ru.yandex.market.partner.banners.client.model.TemplateParamDto;
import ru.yandex.market.partner.banners.client.model.TemplatedBannerGenerationRequestDto;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DisplayName("Тест на выполнение подзадачи отключения дропоффа. Создание баннера.")
public class CreateDropoffDisablingBannerExecutorTest extends AbstractDisablingSubtaskTest {

    @Autowired
    private TemplateBannersApi templateBannersApi;

    @Nonnull
    private TemplatedBannerGenerationRequestDto getRequest(
        Long requestId,
        String date,
        String address,
        List<Long> partnerList
    ) {
        return new TemplatedBannerGenerationRequestDto()
            .templateId(partnerBannersProperties.getDropoffTemplateId())
            .bannerId(getBannerId(requestId))
            .addTemplateParamsItem(
                new TemplateParamDto()
                    .name("date")
                    .value(date)
            )
            .addTemplateParamsItem(
                new TemplateParamDto()
                    .name("address")
                    .value(address)
            )
            .displayConditions(
                new TemplateBannerRequestDisplayConditionsDto()
                    .supplierIds(partnerList)
            );
    }

    @Test
    @DatabaseSetup(value = "/service/dropoff/before/create_banner.xml")
    @ExpectedDatabase(
        value = "/service/dropoff/after/create_banner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное создание баннера.")
    void successCreateBanner() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));

        verify(templateBannersApi).generateBannerPost(
            eq(getRequest(
                100L,
                "8 декабря",
                "test_dropoff",
                List.of(11L, 22L, 33L)
            ))
        );
    }

    @Test
    @DatabaseSetup(
        value = "/service/dropoff/before/create_banner_subtask_no_affected_shops.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/dropoff/after/create_banner_subtask_no_affected_shops.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Генерация баннера не происходит без магазинов.")
    void errorCreateBannerNoAffectedShops() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));
    }
}
