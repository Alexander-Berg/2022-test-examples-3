package ru.yandex.market.util;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.pushapi.client.util.AbstractAutowiringXmlSerializer;
import ru.yandex.market.common.report.indexer.model.OfferDetails;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import java.io.IOException;
import java.util.Date;

@Component
public class OfferDetailsSerializer extends AbstractAutowiringXmlSerializer<OfferDetails> {
    @Override
    public void serializeXml(OfferDetails value, PrimitiveXmlWriter writer) throws IOException {
        writer.startNode("feed");

        writer.startNode("updated");
        writer.addValue(getXmlValue(value.getUpdated()));
        writer.endNode();

        writer.startNode("last_checked");
        writer.addValue(getXmlValue(value.getLastChecked()));
        writer.endNode();

        writer.startNode("id");
        writer.addValue(String.valueOf(value.getFeedId()));
        writer.endNode();

        writer.startNode("session_name");
        writer.addValue(value.getFeedSession());
        writer.endNode();

        writer.startNode("entry");

        writer.startNode("price");
        writer.addValue(Double.toString(value.getPrice()));
        writer.endNode();

        writer.startNode("onstock");
        writer.addValue(String.valueOf(value.isOnStock() ? 1 : 0));
        writer.endNode();

        if (CollectionUtils.isNotEmpty(value.getLocalDelivery())) {
            writer.startNode("local-delivery");
            for (LocalDeliveryOption localDeliveryOption : value.getLocalDelivery()) {
                writer.startNode("delivery-option");
                writer.setAttribute("cost", localDeliveryOption.getCost());
                writer.setAttribute("currency", localDeliveryOption.getCurrency().name());
                if (localDeliveryOption.getDayFrom() != null) {
                    writer.setAttribute("day-from", localDeliveryOption.getDayFrom());
                }
                if (localDeliveryOption.getDayTo() != null) {
                    writer.setAttribute("day-to", localDeliveryOption.getDayTo());
                }
                if (localDeliveryOption.getOrderBefore() != null) {
                    writer.setAttribute("order-before", localDeliveryOption.getOrderBefore());
                }
                writer.endNode();
            }
            writer.endNode();
        }

        if (value.getShopPrice() != null) {
            writer.startNode("shop_price");
            writer.addValue(value.getShopPrice().toString());
            writer.endNode();
        }

        if (value.getShopCurrency() != null) {
            writer.startNode("shop_currency");
            writer.addValue(value.getShopCurrency().name());
            writer.endNode();
        }

        if (value.getDelivery() != null) {
            writer.startNode("delivery");
            writer.addValue(Boolean.toString(value.getDelivery()));
            writer.endNode();
        }

        writer.endNode();

        if (value.getPickup() != null) {
            writer.startNode("pickup");
            writer.addValue(Boolean.toString(value.getPickup()));
            writer.endNode();
        }

        writer.endNode();
    }

    private String getXmlValue(Date value) {
        return value.toInstant().toString();
    }
}
