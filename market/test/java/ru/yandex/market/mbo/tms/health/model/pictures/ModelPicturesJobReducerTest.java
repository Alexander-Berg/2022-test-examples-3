package ru.yandex.market.mbo.tms.health.model.pictures;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * @author s-ermakov
 */
@SuppressWarnings({"checkstyle:LineLength", "checkstyle:MagicNumber"})
public class ModelPicturesJobReducerTest {

    private final Random random = new Random();

    @Test
    public void testEmptyCategoryMapDeserialization() throws Exception {
        Multimap<Long, CategoryParam> expectedMap = createCategoryParamMap(0, 0, 0);

        String categoriesStr = SerializeUtils.serializeCategories(expectedMap);
        Multimap<Long, CategoryParam> actualMap = SerializeUtils.deserializeCategories(categoriesStr);

        assertCategoriesMap(expectedMap, actualMap);
    }

    @Test
    public void testCategoryWithEmptyParamsMapDeserialization() throws Exception {
        Multimap<Long, CategoryParam> expectedMap = createCategoryParamMap(10, 0, 0);

        String categoriesStr = SerializeUtils.serializeCategories(expectedMap);
        Multimap<Long, CategoryParam> actualMap = SerializeUtils.deserializeCategories(categoriesStr);

        assertCategoriesMap(expectedMap, actualMap);
    }

    @Test
    public void testCategoryWithEmptyOptionsMapDeserialization() throws Exception {
        Multimap<Long, CategoryParam> expectedMap = createCategoryParamMap(10, 10, 0);

        String categoriesStr = SerializeUtils.serializeCategories(expectedMap);
        Multimap<Long, CategoryParam> actualMap = SerializeUtils.deserializeCategories(categoriesStr);

        assertCategoriesMap(expectedMap, actualMap);
    }

    @Test
    public void testCategoryRandomMapDeserialization() throws Exception {
        Multimap<Long, CategoryParam> expectedMap = createCategoryParamMap(10, 10, 10);

        String categoriesStr = SerializeUtils.serializeCategories(expectedMap);
        Multimap<Long, CategoryParam> actualMap = SerializeUtils.deserializeCategories(categoriesStr);

        assertCategoriesMap(expectedMap, actualMap);
    }

    private Multimap<Long, CategoryParam> createCategoryParamMap(int categoriesBound, int paramsBound, int optionsBound) {
        Multimap<Long, CategoryParam> categoryParamMultimap = ArrayListMultimap.create();
        int categoriesCount = categoriesBound == 0 ? 0 : random.nextInt(categoriesBound);
        for (int i = 0; i < categoriesCount; i++) {
            long categoryId = random.nextLong();
            List<CategoryParam> categoryParams = new ArrayList<>();
            int paramsCount = paramsBound == 0 ? 0 : random.nextInt(paramsBound);
            for (int j = 0; j < paramsCount; j++) {
                CategoryParam categoryParam = new Parameter();
                categoryParam.setId(random.nextLong());

                int optionsCount = optionsBound == 0 ? 0 : random.nextInt(optionsBound);
                for (int z = 0; z < optionsCount; z++) {
                    Option option = new OptionImpl(random.nextLong());
                    categoryParam.addOption(option);
                }
                categoryParams.add(categoryParam);
            }
            categoryParamMultimap.putAll(categoryId, categoryParams);
        }
        return categoryParamMultimap;
    }

    private void assertCategoriesMap(Multimap<Long, CategoryParam> expectedMap, Multimap<Long, CategoryParam> actualMap) {
        Assert.assertEquals(expectedMap.keySet(), actualMap.keySet());
        for (long categoryId : expectedMap.keySet()) {
            Collection<CategoryParam> expectedParams = expectedMap.get(categoryId);
            Collection<CategoryParam> actualParams = actualMap.get(categoryId);

            Assert.assertEquals(expectedParams.size(), actualParams.size());
            for (CategoryParam categoryParam : expectedParams) {
                CategoryParam actualParam = actualParams.stream()
                    .filter(p -> p.getId() == categoryParam.getId())
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Actual map don't contain param with id " + categoryParam.getId()));

                List<Option> expectedOptions = categoryParam.getOptions();
                List<Option> actualOptions = actualParam.getOptions();

                Assert.assertEquals(expectedOptions.size(), actualOptions.size());
                for (Option option : expectedOptions) {
                    Option actualOption = actualOptions.stream()
                        .filter(o -> o.getId() == option.getId())
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Actual map don't contain option with id " + option.getId()));
                }
            }
        }
    }
}
