package ru.yandex.market.global.index.domain.api;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.index.BaseLocalTest;
import ru.yandex.market.global.index.api.FormsApiService;
import ru.yandex.mj.generated.server.model.ShopCatalogUpdateDto;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FormApiLocalTest extends BaseLocalTest {
    private final FormsApiService formsApiService;

    @Test
    public void testUploadFeed() {
        formsApiService.apiV2CatalogUpdatePost(11441388L, new ShopCatalogUpdateDto()
            .url("https://docs.google.com/spreadsheets/d/e/2PACX-1vTu5JQ75Kh10hsU_qkzz8atwaBYUeByaXG7Qyrn7gTaL8lboxO6dS3VvMR0o8LCyI1T0Ewgmdf3d5EL/pub?output=csv")
            .replace("да")
        );
    }
}
