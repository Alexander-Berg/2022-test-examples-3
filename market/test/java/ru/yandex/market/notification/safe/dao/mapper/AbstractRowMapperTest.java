package ru.yandex.market.notification.safe.dao.mapper;

import java.sql.Clob;
import java.sql.SQLException;

import javax.annotation.Nonnull;
import javax.sql.rowset.serial.SerialClob;

import org.junit.Ignore;

import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.test.util.DataSerializerUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Базовый класс для тестирования {@link org.springframework.jdbc.core.RowMapper}.
 *
 * @author Vladislav Bauer
 */
@Ignore
abstract class AbstractRowMapperTest {

    void checkBinaryData(
        @Nonnull final PersistentBinaryData actualData, @Nonnull final PersistentBinaryData expectedData
    ) {
        assertThat(actualData.getData(), equalTo(expectedData.getData()));
        assertThat(actualData.getType(), equalTo(expectedData.getType()));
    }

    @Nonnull
    PersistentBinaryData createBinaryData() {
        return new PersistentBinaryData(new byte[] { 1, 2, 3 }, "test");
    }

    @Nonnull
    Clob createClob(@Nonnull final PersistentBinaryData data) throws SQLException {
        final byte[] bytes = data.getData();
        final String value = DataSerializerUtils.toString(bytes);

        return new SerialClob(value.toCharArray());
    }

}
