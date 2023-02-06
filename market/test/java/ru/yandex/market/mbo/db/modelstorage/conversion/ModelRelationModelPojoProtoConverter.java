package ru.yandex.market.mbo.db.modelstorage.conversion;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.http.ModelStorage;

/**
 * @author s-ermakov
 */
public class ModelRelationModelPojoProtoConverter {
    private static final long RANDOM_SEED = 2517;
    private static final int TEST_COUNT = 500;

    private EnhancedRandom random;

    @Before
    @SuppressWarnings("checkstyle:magicNumber")
    public void setUp() throws Exception {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(RANDOM_SEED)
            .stringLengthRange(3, 10)
            .collectionSizeRange(1, 5)
            .build();
    }

    @Test
    public void testDoubleConversion() {
        for (int i = 0; i < TEST_COUNT; i++) {
            ModelRelation modelRelation = random.nextObject(ModelRelation.class, "model");
            ModelStorage.Relation proto = ModelProtoConverter.convert(modelRelation);
            ModelRelation modelRelation2 = ModelProtoConverter.convert(proto);

            Assertions.assertThat(modelRelation2).isEqualTo(modelRelation);
        }
    }

    @Test
    public void testCopy() {
        for (int i = 0; i < TEST_COUNT; i++) {
            ModelRelation modelRelation = random.nextObject(ModelRelation.class, "model");
            ModelRelation copy = new ModelRelation(modelRelation);

            Assertions.assertThat(copy).isEqualToComparingFieldByField(modelRelation);
        }
    }
}
