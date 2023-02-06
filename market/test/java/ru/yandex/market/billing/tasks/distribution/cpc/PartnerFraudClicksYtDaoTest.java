package ru.yandex.market.billing.tasks.distribution.cpc;

import java.time.LocalDate;
import java.time.Month;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.billing.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PartnerFraudClicksYtDaoTest extends FunctionalTest {
    @Autowired
    private Yt hahnYt;

    @Mock
    private Cypress cypress;

    @Autowired
    private PartnerFraudClicksYtDao partnerFraudClicksYtDao;

    private static Stream<Arguments> getCheckYtData() {
        return Stream.of(
                Arguments.of(
                        "Таблица существует",
                        LocalDate.of(2020, Month.JANUARY, 1),
                        YPath.simple("//home/antifraud/export/market/partner_clicks_rollback/2020-01-01"),
                        true
                ),
                Arguments.of(
                        "Таблица не существует",
                        LocalDate.of(2020, Month.JANUARY, 1),
                        YPath.simple("//home/antifraud/export/market/partner_clicks_rollback/2019-12-31"),
                        false
                )
        );
    }

    @BeforeEach
    void initYt() {
        when(hahnYt.cypress()).thenReturn(cypress);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getCheckYtData")
    @DisplayName("Проверка наличия таблицы в ыте для даты импорта")
    void verifyYtTablesExist(
            String description,
            LocalDate importDate,
            YPath existingTable,
            boolean exists
    ) {
        when(cypress.exists(existingTable)).thenReturn(exists);

        assertEquals(exists, partnerFraudClicksYtDao.existsYtTable(importDate));
    }
}
