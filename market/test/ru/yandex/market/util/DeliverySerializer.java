package ru.yandex.market.util;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import ru.yandex.market.checkout.common.xml.AbstractXmlSerializer;
import ru.yandex.market.shopadminstub.model.Delivery;

import static ru.yandex.market.shopadminstub.model.DeliveryDates.DATE_PATTERN;

public class DeliverySerializer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    private DeliverySerializer() {
        throw new UnsupportedOperationException();
    }

    public static void write(AbstractXmlSerializer.PrimitiveXmlWriter writer,
                             Delivery delivery) throws IOException {
        if (delivery != null) {
            writer.startNode("delivery");
            writer.setAttribute("delivery-partner-type", delivery.getDeliveryPartnerType());
            if (delivery.getRegion() != null) {
                writer.startNode("region");
                writer.setAttribute("id", delivery.getRegion().getId());
                writer.endNode();
            }
            if (delivery.getDeliveryDates() != null) {
                writer.startNode("dates");
                writer.setAttribute("from-date",
                        DATE_TIME_FORMATTER.format(delivery.getDeliveryDates().getFromDate()));
                if (delivery.getDeliveryDates().getToDate() != null) {
                    writer.setAttribute("to-date",
                            DATE_TIME_FORMATTER.format(delivery.getDeliveryDates().getToDate()));
                }
                writer.endNode();
            }
            writer.endNode();
        }
    }
}
