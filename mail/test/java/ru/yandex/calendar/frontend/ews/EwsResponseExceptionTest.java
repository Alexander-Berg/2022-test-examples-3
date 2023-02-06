package ru.yandex.calendar.frontend.ews;

import com.microsoft.schemas.exchange.services._2006.messages.ResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.types.UnindexedFieldURIType;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class EwsResponseExceptionTest extends CalendarTestBase {

    @Test
    public void getXmlValues() {
        ResponseMessageType.MessageXml xml = new ResponseMessageType.MessageXml();
        xml.getAny().add(EwsUtils.createUnindexedFieldPath(UnindexedFieldURIType.CALENDAR_UID));
        xml.getAny().add(EwsUtils.createUnindexedFieldPath(UnindexedFieldURIType.CALENDAR_START));

        Assert.equals(Cf.list(
                "PathToUnindexedFieldType[fieldURI=CALENDAR_UID]",
                "PathToUnindexedFieldType[fieldURI=CALENDAR_START]"), EwsErrorResponseException.getValues(xml));

        Assert.isEmpty(EwsErrorResponseException.getValues(null));
    }
}
