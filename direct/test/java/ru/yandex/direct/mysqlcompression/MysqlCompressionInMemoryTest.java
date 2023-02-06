package ru.yandex.direct.mysqlcompression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class MysqlCompressionInMemoryTest {
    private static final Random RANDOM = new Random();
    private static final int MAX_DATA_SIZE = 20 << 20; // 20M

    private final byte[] data;

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

    public MysqlCompressionInMemoryTest(@SuppressWarnings("unused") int size, byte[] data) {
        this.data = data;
    }

    @Test
    public void canUncompressCompressedData() {
        Assert.assertThat("uncompress возвращает исходные данные",
                MysqlCompression.uncompress(MysqlCompression.compress(data)), is(data));
    }
}
