package ru.yandex.direct.dbutil.sharding;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.dbutil.testing.DbUtilTest;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapper;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;
import ru.yandex.direct.dbutil.wrapper.SimpleDb;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@DbUtilTest
@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
public class ShardedValuesGeneratorTest {

    @Autowired
    DatabaseWrapperProvider databaseWrapperProvider;

    @Autowired
    private ShardedValuesGenerator shardedValuesGenerator;

    @Test
    public void generateValuesTest() throws SQLException {
        int sizeInput = 10;
        ShardKey key = ShardKey.CID;
        List<Long> ids = longValues(shardedValuesGenerator.runMultiGeneration(key.getTable(),
                key.getValueField(),
                repeat(Long.valueOf(12345), sizeInput)));


        Set<Long> setIds = new HashSet<>(ids);
        assertEquals(sizeInput, setIds.size());

        ppcdict().getDslContext()
                .deleteFrom(key.getTable())
                .where(key.getKeyField().in(ids))
                .execute();

    }

    private static List<Long> longValues(Iterable<? extends Number> numbers) {
        return mapList(numbers, Number::longValue);
    }

    private DatabaseWrapper ppcdict() {
        return databaseWrapperProvider.get(SimpleDb.PPCDICT);
    }

    private static <T> List<T> repeat(T obj, int times) {
        return Stream.generate(() -> obj).limit(times).collect(toList());
    }

}
