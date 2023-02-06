package ru.yandex.market.markup2.tasks.supplier_sku_mapping.moderation_toloka;

import java.util.Collection;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.markup2.tasks.supplier_sku_mapping.SupplierOfferDataAttributes;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.SupplierOfferDataItemPayload;
import ru.yandex.market.markup2.utils.OfferTestUtils;
import ru.yandex.market.markup2.utils.mboc.MbocOfferStatus;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;

public class TolokaListenerTest {
    private final TolokaMappingStatusChangeListener listener = new TolokaMappingStatusChangeListener();

    @Test
    public void changeStateTest() {
        TaskDataItemState newState = TaskDataItemState.SENT;
        SupplierOfferDataAttributes attributes = new SupplierOfferDataAttributes(
                1,
                "category",
                "categoryNAme",
                "supName",
                "supType",
                OfferTestUtils.createMatchedOffer(1L, "123"));
        Collection<TaskDataItem<SupplierOfferDataItemPayload, ModerationTolokaResponse>> taskDataItems =
                Collections.singletonList(
                        new TaskDataItem(1L,
                                0L,
                                new SupplierOfferDataItemPayload("", attributes)
                        )
                );

        Collection<MbocOfferStatus> offerStatuses = listener.createOfferStatuses(taskDataItems, newState);

        offerStatuses.forEach(s -> {
            Assert.assertTrue(s.isToloka());
            Assert.assertTrue(s.getState() == TaskDataItemState.SENT);
        });
    }


}
