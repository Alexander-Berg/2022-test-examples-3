package ru.yandex.direct.internaltools.tools.productrestrictions;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.product.model.ProductRestriction;
import ru.yandex.direct.core.entity.product.service.ProductService;
import ru.yandex.direct.core.testing.CloneTestUtil;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductRestrictionDifferTest {

    /**
     * Проверка, что текстовое представление ProductRestrictionKey уникально
     * для каждой записи из файла.
     */
    @Test
    public void renderProductRestrictionKeyTest() {
        List<ProductRestriction> prList = ProductService.readProductRestrictionFile();

        Set<String> seenKeys = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (var pr : prList) {
            String key = ProductRestrictionDiffer.renderProductRestrictionKey(
                    ProductService.calculateUniqueProductRestrictionKey(pr)
            );
            if (!seenKeys.add(key)) {
                duplicates.add(key);
            }
        }

        assertThat(duplicates).isEmpty();
    }

    /**
     * Проверка, что диффилка выводит в JSON все поля объекта, и поэтому может поймать их различие
     */
    @Test
    public void getProductRestrictionDiffTest() throws IllegalAccessException {
        var differ = new ProductRestrictionDiffer();
        List<Field> allFields = CloneTestUtil.getAllFields(ProductRestriction.class);

        String nullConditionJson =
                "[{\"name\":\"correction_traffic\",\"availableAny\":false,\"required\":false,\"values\":[]}]";
        for (var field : allFields) {
            if (field.getName().equals("conditions")) {
                // в тесте это поле равно null, и когда диффилка это видит, она десериализует поле conditionJson
                // поэтому достаточно проверки диффилки на восприимчивость к полю conditionJson
                continue;
            }

            var nullRecord = new ProductRestriction()
                    .withId(0L).withGroupType(AdGroupType.BASE).withPublicNameKey("").withProductId(0L)
                    .withConditionJson(nullConditionJson);
            var modifiedRecord = new ProductRestriction()
                    .withId(0L).withGroupType(AdGroupType.BASE).withPublicNameKey("").withProductId(0L)
                    .withConditionJson(nullConditionJson);
            field.setAccessible(true);
            field.set(modifiedRecord, getDifferentValue(field));

            var diff = differ.getProductRestrictionDiff(singletonList(nullRecord), singletonList(modifiedRecord));
            assertThat(diff).withFailMessage("Diff is indifferent to " + field.getName()).isNotEmpty();
        }
    }

    private Object getDifferentValue(Field field) {
        switch (field.getName()) {
            case "id":
                return 1L;
            case "groupType":
                return AdGroupType.CPM_OUTDOOR;
            case "publicNameKey":
                return "hello world";
            case "productId":
                return 2L;
            case "conditionJson":
                return "[{\"name\":\"correction_traffic\",\"availableAny\":true,\"required\":false,\"values\":[]}]";
            default:
                return CloneTestUtil.generateRandomValue(field);
        }
    }
}
