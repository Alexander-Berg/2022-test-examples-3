package ru.yandex.mbo.tool.jira.MBO7331;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.catalogue.model.transfer.ModelRelationGroup;
import ru.yandex.market.mbo.db.transfer.ModelTransferWorkerHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author dmserebr
 * @date 20.06.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MoveModelsHelperTest {
    @Test
    public void testGetFilledSkuParams() {
        ModelRelationGroup modelRelationGroup1 = new ModelRelationGroup(
            CommonModelBuilder.newBuilder(1L, 10L, 100L)
                .parameterValues(30L, "guruParam1", "guruParamValue1")
            .getModel(),
            Collections.emptyList(),
            Collections.singletonList(
                CommonModelBuilder.newBuilder(2L, 10L, 100L)
                    .parameterValues(31L, "skuParam1", "skuParamValue1")
                    .parameterValues(32L, "skuParam2", "skuParamValue2")
                .getModel()),
            Collections.emptyList()
        );

        ModelRelationGroup modelRelationGroup2 = new ModelRelationGroup(
            CommonModelBuilder.newBuilder(3L, 10L, 200L)
                .parameterValues(33L, "guruParam2", "guruParamValue2")
                .getModel(),
            Collections.emptyList(),
            Arrays.asList(
                CommonModelBuilder.newBuilder(4L, 10L, 200L)
                    .parameterValues(31L, "skuParam1", "skuParamValue2")
                .getModel(),
                CommonModelBuilder.newBuilder(5L, 10L, 200L)
                    .parameterValues(34L, "skuParam3", 1000L, 1001L)
                    .parameterValues(32L, "skuParam2", "skuParamValue4")
                .getModel()),
            Collections.emptyList()
        );

        Collection<String> filledSkuParams = ModelTransferWorkerHelper.getFilledSkuParams(
            Arrays.asList(modelRelationGroup1, modelRelationGroup2));

        Assertions.assertThat(filledSkuParams).containsExactlyInAnyOrder(
            "vendor", "skuParam1", "skuParam2", "skuParam3");
    }
}
