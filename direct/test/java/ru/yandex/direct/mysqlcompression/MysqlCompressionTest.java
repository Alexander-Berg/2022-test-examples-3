package ru.yandex.direct.mysqlcompression;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.mysql.MySQLInstance;
import ru.yandex.direct.test.mysql.DirectMysqlDb;
import ru.yandex.direct.test.mysql.TestMysqlConfig;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class MysqlCompressionTest {

    private static final Random RANDOM = new Random();
    private static final int MAX_DATA_SIZE = 2 << 20; // 2M

    private final byte[] data;

    private static Connection conn;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        Collection<Object[]> result = new ArrayList<>();
        result.add(new Object[]{0, new byte[0]});

        for (int i = 0; ; i++) {
            int arraySize = (int) Math.floor(Math.pow(1.3, i));
            if (arraySize > MAX_DATA_SIZE) {
                break;
            }

            byte[] bytes = new byte[arraySize];
            RANDOM.nextBytes(bytes);
            result.add(new Object[]{arraySize, bytes});
        }

        return result;
    }

    @BeforeClass
    public static void init() throws Exception {
        MySQLInstance mysql = new DirectMysqlDb(TestMysqlConfig.directConfig()).start();
        conn = mysql.connect();
        conn.setCatalog("mysql");
    }

    public MysqlCompressionTest(@SuppressWarnings("unused") int size, byte[] data) {
        this.data = data;
    }

    @Test
    public void compressProducesSameResult() throws Exception {
        PreparedStatement statement = conn.prepareStatement("select compress(?)");
        statement.setBytes(1, data);

        ResultSet resultSet = statement.executeQuery();

        assumeThat("sql запрос вернул результат", resultSet.next(), is(true));
        assertThat("compress возвращает правильный результат", MysqlCompression.compress(data),
                is(resultSet.getBytes(1)));
    }

    @Test
    public void mysqlCanUncompressCompressedResult() throws Exception {
        PreparedStatement statement = conn.prepareStatement("select uncompress(?)");
        statement.setBytes(1, MysqlCompression.compress(data));

        ResultSet resultSet = statement.executeQuery();

        assumeThat("sql запрос вернул результат", resultSet.next(), is(true));
        assertThat("uncompress возвращает правильный результат", resultSet.getBytes(1), is(data));
    }

    @Test
    public void canUncompressMysqlCompressedResult() throws Exception {
        PreparedStatement statement = conn.prepareStatement("select compress(?)");
        statement.setBytes(1, data);

        ResultSet resultSet = statement.executeQuery();

        assumeThat("sql запрос вернул результат", resultSet.next(), is(true));
        assertThat("uncompress возвращает исходные данные",
                MysqlCompression.uncompress(resultSet.getBytes(1)), is(data));
    }

    @Test
    public void canUncompressCompressedData() {
        assertThat("uncompress возвращает исходные данные",
                MysqlCompression.uncompress(MysqlCompression.compress(data)), is(data));
    }
}
