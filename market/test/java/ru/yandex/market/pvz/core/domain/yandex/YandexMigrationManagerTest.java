package ru.yandex.market.pvz.core.domain.yandex;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DOCUMENTS_ISSUED_BY_YANDEX_SINCE;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class YandexMigrationManagerTest {

    private final ConfigurationGlobalCommandService configurationGlobalCommandService;
    private final YandexMigrationManager yandexMigrationManager;

    @Test
    void testIsYandex() {
        configurationGlobalCommandService.setValue(DOCUMENTS_ISSUED_BY_YANDEX_SINCE, "2020-01-15");

        assertThat(yandexMigrationManager.isYandex(LocalDate.of(2020, 1, 14))).isFalse();
        assertThat(yandexMigrationManager.isYandex(LocalDate.of(2020, 1, 15))).isTrue();
        assertThat(yandexMigrationManager.isYandex(LocalDate.of(2020, 1, 16))).isTrue();
    }

    @Test
    void testNullIsYandexMarket() {
        configurationGlobalCommandService.resetValue(null, DOCUMENTS_ISSUED_BY_YANDEX_SINCE);
        assertThat(yandexMigrationManager.isYandex(LocalDate.of(2020, 1, 16))).isFalse();
    }

    @Test
    void testGetOrgName() {
        configurationGlobalCommandService.setValue(DOCUMENTS_ISSUED_BY_YANDEX_SINCE, "2020-01-15");

        assertThat(yandexMigrationManager.getOrgName(LocalDate.of(2020, 1, 14)))
                .isEqualTo(YandexMigrationManager.YANDEX_MARKET_ORGANIZATION);

        assertThat(yandexMigrationManager.getOrgName(LocalDate.of(2020, 1, 16)))
                .isEqualTo(YandexMigrationManager.YANDEX_ORGANIZATION);
    }

}
