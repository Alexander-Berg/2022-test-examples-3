package ru.yandex.market.deepmind.common.services.category;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.deepmind.common.category.CategoryConverter;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.tree.ExportTovarTree;

public class CategoryConverterTest {
    private static final long SEED = "MBO-17156".hashCode();
    private static final int ITERATIONS = 1000;

    private Random random;

    @Before
    public void setUp() {
        random = new Random(SEED);
    }

    @Test
    public void testConvert() {
        for (int i = 0; i < ITERATIONS; i++) {
            MboParameters.Word title = MboParameters.Word.newBuilder()
                .setName(UUID.randomUUID().toString()).build();

            ExportTovarTree.TovarCategory dumpCategory = ExportTovarTree.TovarCategory.newBuilder()
                .setHid(random.nextInt())
                .setParentHid(random.nextInt())
                .addAllVendorGoodContentExclusion(List.of(random.nextLong(), random.nextLong()))
                .addName(title)
                .build();

            Category mbocCategory = CategoryConverter.convert(dumpCategory);
            Assertions.assertThat(dumpCategory.getHid()).isEqualTo(mbocCategory.getCategoryId());
            Assertions.assertThat(dumpCategory.getParentHid()).isEqualTo(mbocCategory.getParentCategoryId());
            Assertions.assertThat(dumpCategory.getName(0).getName()).isEqualTo(mbocCategory.getName());
        }
    }
}
