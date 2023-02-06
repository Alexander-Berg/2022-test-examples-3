package ru.yandex.market.crm.operatorwindow.external;

import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.crm.operatorwindow.domain.delivery.DeliveryInterval;
import ru.yandex.market.crm.operatorwindow.external.platform.RetryPlatformChangeOrderDvkContext;
import ru.yandex.market.crm.operatorwindow.util.DateParsers;
import ru.yandex.market.crm.platform.models.OrderDvk;
import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.jmf.utils.SerializationUtils;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;

public class RetryPlatformChangeOrderDvkContextTest {

    private static final Logger LOG = LoggerFactory.getLogger(RetryPlatformChangeOrderDvkContextTest.class);

    private final ObjectSerializeService serializeService = SerializationUtils.defaultObjectSerializeService();

    @Test
    public void serializeDeserialize() {
        RetryPlatformChangeOrderDvkContext context =
                new RetryPlatformChangeOrderDvkContext(3468350,
                        new DeliveryInterval(
                                DateParsers.parseIsoDate("2019-06-19"),
                                DateParsers.parseIsoDate("2019-06-20"),
                                DateParsers.parseIsoTime("10:01:02"),
                                null), OrderDvk.Source.OCRM_OUTGOING_CAMPAIGN);
        byte[] serialized = serializeService.serialize(context);
        LOG.info("serialized {} : {}", context, CrmStrings.valueOf(serialized));
        RetryPlatformChangeOrderDvkContext deserialized = serializeService.deserialize(
                serialized, RetryPlatformChangeOrderDvkContext.class);

        Assertions.assertEquals(context.getOrderId(), deserialized.getOrderId());
        Assertions.assertEquals(context.getNewDvk().getFromDate(), deserialized.getNewDvk().getFromDate());
        Assertions.assertEquals(context.getNewDvk().getToDate(), deserialized.getNewDvk().getToDate());
        Assertions.assertEquals(context.getNewDvk().getFromTime(), deserialized.getNewDvk().getFromTime());
        Assertions.assertEquals(context.getNewDvk().getToTime(), deserialized.getNewDvk().getToTime());
    }

    @Test
    public void serializeLocalDate() throws JsonProcessingException {
        String dateStr = "2019-06-19";
        LocalDate date = DateParsers.parseIsoDate(dateStr);
        Assertions.assertEquals('"' + dateStr + '"', CrmStrings.valueOf(serializeService.serialize(date)));
    }

}
