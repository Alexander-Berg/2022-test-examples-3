package ru.yandex.market.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.curator.shaded.com.google.common.base.Throwables;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.xml.SimpleXmlWriter;
import ru.yandex.market.common.report.indexer.model.OfferDetails;
import ru.yandex.market.common.report.model.CurrencyConvertResult;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.parser.json.CurrencyConvertMarketReportJsonParserSettings;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.shopadminstub.model.OrderAcceptRequest;
import ru.yandex.market.shopadminstub.model.StocksRequest;
import ru.yandex.market.shopadminstub.model.inventory.Inventory;
import ru.yandex.market.shopadminstub.model.inventory.InventoryDelivery;
import ru.yandex.market.shopadminstub.model.inventory.InventoryItem;
import ru.yandex.market.shopadminstub.serde.InventoryJsonHandler;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

@Component
public class TestSerializationService {
    private final MappingJackson2XmlHttpMessageConverter xmlConverter;
    private final CartRequestSerializer cartRequestSerializer;
    private final OrderAcceptRequestSerializer orderAcceptRequestSerializer;
    private final OfferDetailsSerializer offerDetailsSerializer;
    private final CurrencyConvertMarketReportJsonParserSettings currencyConvertParserSettings
            = new CurrencyConvertMarketReportJsonParserSettings();

    public TestSerializationService(MappingJackson2XmlHttpMessageConverter xmlConverter,
                                    CartRequestSerializer cartRequestSerializer,
                                    OrderAcceptRequestSerializer orderAcceptRequestSerializer,
                                    OfferDetailsSerializer offerDetailsSerializer) {
        this.xmlConverter = xmlConverter;
        this.cartRequestSerializer = cartRequestSerializer;
        this.orderAcceptRequestSerializer = orderAcceptRequestSerializer;
        this.offerDetailsSerializer = offerDetailsSerializer;
    }

    public <T> String serializeXml(T obj) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();

        if (obj instanceof CartRequest) {
            return serializeCartRequest((CartRequest) obj);
        } else if (obj instanceof OrderAcceptRequest) {
            return serializeOrderAcceptRequest((OrderAcceptRequest) obj);
        } else if (obj instanceof OfferDetails) {
            return serializeOfferDetails((OfferDetails) obj);
        }

        xmlConverter.write(obj, MediaType.APPLICATION_XML, mockHttpOutputMessage);

        return mockHttpOutputMessage.getBodyAsString();
    }

    private String serializeOrderAcceptRequest(OrderAcceptRequest obj) throws IOException {
        StringWriter stringWriter = new StringWriter();
        SimpleXmlWriter writer = new SimpleXmlWriter(stringWriter);

        orderAcceptRequestSerializer.serializeXml(obj, writer);

        return stringWriter.toString();
    }

    private String serializeCartRequest(CartRequest obj) throws IOException {
        StringWriter stringWriter = new StringWriter();
        SimpleXmlWriter writer = new SimpleXmlWriter(stringWriter);

        cartRequestSerializer.serializeXml(obj, writer);

        return stringWriter.toString();
    }

    private String serializeOfferDetails(OfferDetails obj) throws IOException {
        StringWriter stringWriter = new StringWriter();
        SimpleXmlWriter writer = new SimpleXmlWriter(stringWriter);

        offerDetailsSerializer.serializeXml(obj, writer);

        return stringWriter.toString();
    }

    public String serializeJson(CurrencyConvertResult currencyConvertResult) {
        try {
            CurrencyConvertMarketReportJsonParserSettings settings = this.currencyConvertParserSettings;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(settings.getCurrencyFrom(), currencyConvertResult.getCurrencyFrom().name());
            jsonObject.put(settings.getCurrencyTo(), currencyConvertResult.getCurrencyTo().name());
            jsonObject.put(settings.getValue(), currencyConvertResult.getValue().toString());
            jsonObject.put(settings.getConvertedValue(), currencyConvertResult.getConvertedValue().toString());
            jsonObject.put(settings.getRenderedConvertedValue(), currencyConvertResult.getRenderedConvertedValue().toString());
            jsonObject.put(settings.getRenderedValue(), currencyConvertResult.getRenderedValue().toString());
            return jsonObject.toString();
        } catch (JSONException ex) {
            throw Throwables.propagate(ex);
        }
    }

    public String serializeJson(Inventory inventory) {
        try {
            JSONObject result = new JSONObject();

            JSONArray items = new JSONArray();
            for (Map.Entry<FeedOfferId, InventoryItem> inventoryItemEntry : inventory.getInventory().entrySet()) {
                InventoryItem inventoryItem = inventoryItemEntry.getValue();
                JSONObject item = new JSONObject();
                item.put("feedId", inventoryItem.getFeedId());
                item.put("offerId", inventoryItem.getOfferId());
                item.put("price", inventoryItem.getPrice());
                item.put("count", inventoryItem.getCount());
                items.put(item);
            }
            result.put(InventoryJsonHandler.INVENTORY_ATTRIBUTE_NAME, items);

            JSONArray deliveryOptions = new JSONArray();
            for (InventoryDelivery delivery : inventory.getDelivery()) {
                JSONObject deliveryObject = new JSONObject();
                deliveryObject.put("id", delivery.getId());
                deliveryObject.put("type", delivery.getType());
                deliveryObject.put("name", delivery.getName());
                deliveryObject.put("price", delivery.getPrice());
                deliveryObject.put("dayFrom", delivery.getFromDateOffset());
                deliveryObject.put("dayTo", delivery.getToDateOffset());

                if (CollectionUtils.isNotEmpty(delivery.getOutlets())) {
                    JSONArray outlets = new JSONArray();
                    delivery.getOutlets().forEach(outlets::put);
                    deliveryObject.put("outlets", delivery.getOutlets());
                }
                deliveryOptions.put(deliveryObject);
            }
            result.put(InventoryJsonHandler.DELIVERY_ATTRIBUTE_NAME, deliveryOptions);

            JSONArray paymentMethods = new JSONArray();
            for (PaymentMethod paymentMethod : inventory.getPaymentMethods()) {
                paymentMethods.put(paymentMethod.name());
            }
            result.put(InventoryJsonHandler.PAYMENT_METHODS_ATTRIBUTE_NAME, paymentMethods);

            return result.toString();
        } catch (JSONException jsonEx) {
            throw Throwables.propagate(jsonEx);
        }
    }

    public String serializeJson(StocksRequest stocksRequest) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("warehouseId", stocksRequest.getWarehouseId());

        if (CollectionUtils.isNotEmpty(stocksRequest.getSkus())) {
            JSONArray skuArray = new JSONArray();
            for (String sku : stocksRequest.getSkus()) {
                skuArray.put(sku);
            }
            jsonObject.put("skus", skuArray);
        }
        return jsonObject.toString();
    }
}
