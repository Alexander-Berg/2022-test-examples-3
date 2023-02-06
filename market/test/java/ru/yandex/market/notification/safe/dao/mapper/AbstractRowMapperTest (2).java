package ru.yandex.market.notification.safe.dao.mapper;

import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.sql.SQLException;

import javax.annotation.Nonnull;
import javax.sql.rowset.serial.SerialClob;

import org.junit.jupiter.api.Disabled;

import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Базовый класс для тестирования {@link org.springframework.jdbc.core.RowMapper}.
 *
 * @author Vladislav Bauer
 */
@Disabled
abstract class AbstractRowMapperTest {
    static void checkBinaryData(
            @Nonnull PersistentBinaryData actualData,
            @Nonnull PersistentBinaryData expectedData
    ) {
        assertThat(actualData.getData(), equalTo(expectedData.getData()));
        assertThat(actualData.getType(), equalTo(expectedData.getType()));
    }

    @Nonnull
    static PersistentBinaryData createBinaryData() {
        return new PersistentBinaryData(new byte[]{1, 2, 3}, "test");
    }

    @Nonnull
    static Clob createClob(@Nonnull PersistentBinaryData data) throws SQLException {
        return new SerialClob(new String(data.getData(), StandardCharsets.UTF_8).toCharArray());
    }
}
