package ru.yandex.market.api.partner.controllers.offers.mapping;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.core.offer.mapping.error.MappingConstraintCode;

import static org.junit.Assert.assertNotNull;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class ErrorCodesMappingTest {

    @DisplayName("Тест проверяет, что не забыли добавить новую валидацию в транслятор ошибок.")
    @Test
    public void testErrorCodesMapping() {
        for (MappingConstraintCode mappingConstraintCode : MappingConstraintCode.values()) {
            assertNotNull(OfferMappingEntriesController.ERROR_CODES.get(mappingConstraintCode));
        }
    }
}
