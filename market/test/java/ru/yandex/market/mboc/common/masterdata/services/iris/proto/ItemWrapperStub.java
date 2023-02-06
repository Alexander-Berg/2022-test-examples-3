package ru.yandex.market.mboc.common.masterdata.services.iris.proto;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapper;

/**
 * Stub for testing of abstract ItemWrapper functionality.
 *
 * @author dmserebr
 * @date 30/03/2020
 */
public class ItemWrapperStub extends ItemWrapper {
    public ItemWrapperStub() {
        super();
    }

    public ItemWrapperStub(MdmIrisPayload.Item item) {
        super(item);
    }
}
