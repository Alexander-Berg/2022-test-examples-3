package ru.yandex.market.mbo.db.modelstorage.index;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.bolts.collection.Tuple2;

import java.util.Arrays;
import java.util.List;


/**
 * @author apluhin
 * @created 11/20/20
 */
public class MboIndexesFilterTest {

    @Test
    public void testMatchCountOperations() {
        int matchCount = MboIndexesFilter.newFilter().setCategoryId(1L).setParentIdExists(true).matchCount(
            Arrays.asList(
                Tuple2.tuple(GenericField.CATEGORY_ID, Operation.IN),
                Tuple2.tuple(GenericField.PARENT_ID, Operation.EXIST),
                Tuple2.tuple(GenericField.VENDOR_ID, Operation.IN)
            )
        );
        Assertions.assertThat(matchCount).isEqualTo(2);
    }

    @Test
    public void testCorrectFilter() {
        boolean isCorrect = MboIndexesFilter.newFilter().setCategoryId(1L).setParentIdExists(true).correctFilter(
            Arrays.asList(
                Tuple2.tuple(GenericField.CATEGORY_ID, Operation.IN),
                Tuple2.tuple(GenericField.PARENT_ID, Operation.EXIST),
                Tuple2.tuple(GenericField.VENDOR_ID, Operation.IN)));
        Assertions.assertThat(isCorrect).isEqualTo(true);
    }

    @Test
    public void testCorrectFilterFailed() {
        boolean isCorrect = MboIndexesFilter.newFilter().setCategoryId(1L).setParentIdExists(true).correctFilter(
            Arrays.asList(
                Tuple2.tuple(GenericField.CATEGORY_ID, Operation.IN),
                Tuple2.tuple(GenericField.VENDOR_ID, Operation.IN)));
        Assertions.assertThat(isCorrect).isEqualTo(false);
    }

    @Test
    public void testContainsOperation() {
        boolean isContainsOperation = MboIndexesFilter.newFilter()
            .setIsSku(true).containsOperation(GenericField.IS_SKU, Operation.EQ);
        Assertions.assertThat(isContainsOperation).isEqualTo(true);
    }

    @Test
    public void testGetOperationForField() {
        List<OperationContainer> containers = MboIndexesFilter.newFilter()
            .setModelId(1L)
            .setModelIdCursor(1L)
            .getOperationForField(GenericField.MODEL_ID);
        Assertions.assertThat(containers.get(0))
            .isEqualTo(new OperationContainer(GenericField.MODEL_ID, 1L, Operation.IN));
        Assertions.assertThat(containers.get(1))
            .isEqualTo(new OperationContainer(GenericField.MODEL_ID, 1L, Operation.GT));
    }
}
