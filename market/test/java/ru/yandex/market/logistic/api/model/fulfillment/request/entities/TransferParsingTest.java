package ru.yandex.market.logistic.api.model.fulfillment.request.entities;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.utils.ParsingTest;

public class TransferParsingTest extends ParsingTest<Transfer> {

    public static final ImmutableMap<String, Object> TRANSFER_VALUES = ImmutableMap.<String, Object>builder()
        .put("inboundId", new ResourceId("inboundYandexId", "inboundFulfillmentId"))
        .put("transferId", new ResourceId("transferYandexId", "transferFulfillmentId"))
        .put("from", StockType.ACCEPTANCE)
        .put("to", StockType.FIT)
        .put("transferItems", Arrays.asList(new TransferItem(new UnitId("1", 2L, "AAA"), 10)))
        .build();

    public TransferParsingTest() {
        super(Transfer.class, "fixture/request/entities/transfer.xml");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return TRANSFER_VALUES;
    }
}
