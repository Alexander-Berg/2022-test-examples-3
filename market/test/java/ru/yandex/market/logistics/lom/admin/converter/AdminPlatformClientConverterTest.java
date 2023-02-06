package ru.yandex.market.logistics.lom.admin.converter;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;

public class AdminPlatformClientConverterTest extends AbstractTest {
    private final AdminPlatformClientConverter converter = new AdminPlatformClientConverter();

    @DisplayName("Для всех платформенных клиентов существует админский мапинг")
    @ParameterizedTest
    @EnumSource(PlatformClient.class)
    void toAdminAllValues(PlatformClient platformClient) {
        var client = converter.toAdmin(platformClient);
        softly.assertThat(Optional.ofNullable(client))
            .hasValueSatisfying(c -> softly.assertThat(c.name()).isEqualTo(platformClient.name()));
    }
}
