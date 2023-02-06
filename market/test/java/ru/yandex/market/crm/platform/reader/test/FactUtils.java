package ru.yandex.market.crm.platform.reader.test;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.platform.common.UidTypes;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.common.FactsColumns;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;

/**
 * @author apershukov
 */
public final class FactUtils {

    public static void assertRow(String id,
                                 UidType idType,
                                 @Nullable Long timestamp,
                                 @Nullable String factId,
                                 Message fact,
                                 Parser<?> parser,
                                 YTreeMapNode row) throws InvalidProtocolBufferException {
        assertEquals(id, row.getString(FactsColumns.ID));
        assertEquals(UidTypes.value(idType), row.getString(FactsColumns.ID_TYPE));
        if (timestamp != null) {
            assertEquals(timestamp.longValue(), row.getLong(FactsColumns.TIMESTAMP));
        }
        if (factId != null) {
            assertEquals(factId, row.getString(FactsColumns.FACT_ID));
        }
        assertEquals(fact, parser.parseFrom(row.getBytes("fact")));
    }
}
