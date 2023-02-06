package ru.yandex.market.marketId.enums;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketId.FunctionalTest;
import ru.yandex.market.marketId.model.entity.LegalInfoStatusEntity;
import ru.yandex.market.marketId.repository.LegalInfoStatusRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тест статусов Юр. инфо")
class LegalInfoStatusEnumTest extends FunctionalTest {

    @Autowired
    private LegalInfoStatusRepository legalInfoStatusRepository;

    @ParameterizedTest
    @MethodSource("args")
    @DisplayName("Тест статусов юр. инфо")
    void testLegalInfoStatus(LegalInfoStatus status) {
        Optional<LegalInfoStatusEntity> legalInfoStatus = legalInfoStatusRepository.findById(status);
        assertTrue(legalInfoStatus.isPresent());
    }

    @Test
    @DisplayName("Проверить что в enum и БД одинаковое кол-во полей")
    void testLegalInfoTypeLength() {
        assertEquals(legalInfoStatusRepository.findAll().size(), LegalInfoStatus.values().length);
    }

    static Stream<Arguments> args() {
        return Stream.of(LegalInfoStatus.values()).map(Arguments::of);
    }
}
