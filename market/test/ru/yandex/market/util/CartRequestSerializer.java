package ru.yandex.market.util;

import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.common.report.ColorUtils;
import ru.yandex.market.checkout.pushapi.client.util.AbstractAutowiringXmlSerializer;
import ru.yandex.market.shopadminstub.model.CartRequest;

import java.io.IOException;

/**
 * TODO: Удалить, когда перейдем на анноташки.
 */
@Component
public class CartRequestSerializer extends AbstractAutowiringXmlSerializer<CartRequest> {
    @Override
    public void serializeXml(CartRequest value, PrimitiveXmlWriter writer) throws IOException {
        writer.startNode("cart");
        writer.setAttribute("currency", value.getCurrency());
        writer.setAttribute("delivery-currency", value.getDeliveryCurrency());
        writer.setAttribute("fulfilment", value.isFulfilment());
        writer.setAttribute("rgb", ColorUtils.mapCheckouterColorToReport(value.getRgb()));
        if (value.getContext() != null) {
            writer.setAttribute("context", value.getContext());
        }
        if (value.hasCertificate()) {
            writer.setAttribute("hasCertificate", value.hasCertificate());
        }
        if (value.getExperiments() != null) {
            writer.setAttribute("experiments", value.getExperiments());
        }

        if (value.getItems() != null) {
            ItemsSerializer.writeItems(writer, value.getItems().values());
        }

        writeDelivery(value, writer);

        writer.endNode();
    }

    private void writeDelivery(CartRequest value, PrimitiveXmlWriter writer) throws IOException {
        writer.startNode("delivery");
        writer.startNode("region");
        writer.setAttribute("id", value.getRegionId());
        writer.endNode();
        writer.endNode();
    }
}
