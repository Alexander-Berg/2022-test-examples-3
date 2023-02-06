package ru.yandex.market.mbi.api.controller.param;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.tags.Components;
import ru.yandex.market.tags.Features;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Функциональные тесты ручки /checked-param-shops.
 *
 * @author Vadim Lyalin
 */
@Tag(Components.MBI_API)
@Tag(Components.MBI_API_CLIENT)
@Tag(Features.DB_INTEGRATION)
public class CheckedParamShopsFunctionalTest extends FunctionalTest {

    @Test
    @DisplayName("Запрашиваем для невалидного id программы")
    @DbUnitDataSet(before = "CheckedParamShops.before.csv")
    void getIncorrectStatus() {
        Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.getCheckedParamShops(-1, ParamCheckStatus.DONT_WANT)
        );
    }
}
