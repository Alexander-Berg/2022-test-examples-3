package ru.yandex.market.mbo.redis.categories;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.utils.RandomTestUtils;

public class CategoryConverterTest {

    private static final int COUNT = 100;
    private static final long SEED = 1;

    private CategoryRedisDataConverter serializer;
    private EnhancedRandom random;

    @Before
    public void setUp() {
        serializer = new CategoryRedisDataConverter();
        random = RandomTestUtils.createNewRandom(SEED);
    }

    @Test
    public void shouldSerializeAndDeserializeIntoEqualObjects() {
        for (int i = 0; i < COUNT; i++) {
            MboParameters.Category before = MboParameters.Category.newBuilder()
                .setHid(random.nextLong())
                .addName(MboParameters.Word.newBuilder()
                    .setName(random.nextObject(String.class))
                    .build())
                .build();
            byte[] bytes = serializer.serialize(before);
            MboParameters.Category after = serializer.deserialize(bytes);
            Assertions.assertThat(after).isEqualTo(before);
        }
    }
}
