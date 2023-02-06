package ru.yandex.market.marketId.enums;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.id.LegalInfoType;
import ru.yandex.market.marketId.FunctionalTest;
import ru.yandex.market.marketId.model.entity.LegalInfoTypeEntity;
import ru.yandex.market.marketId.repository.LegalInfoTypeRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalInfoTypeEnumTest extends FunctionalTest {

    @Autowired
    private LegalInfoTypeRepository legalInfoTypeRepository;

    @ParameterizedTest
    @MethodSource("args")
    @DisplayName("Тест полей для юр. инфо")
    void testLegalInfoType(LegalInfoType type) {
        Optional<LegalInfoTypeEntity> legalInfoType = legalInfoTypeRepository.findById(type);
        assertTrue(legalInfoType.isPresent());
    }

    @Test
    @DisplayName("Проверить что в enum и БД одинаковое кол-во полей")
    void testLegalInfoTypeLength() {
        assertEquals(legalInfoTypeRepository.findAll().size(), LegalInfoType.values().length-1);
    }

    static Stream<Arguments> args() {
        return Stream.of(LegalInfoType.values()).filter(v -> v != LegalInfoType.UNRECOGNIZED).map(Arguments::of);
    }
}
