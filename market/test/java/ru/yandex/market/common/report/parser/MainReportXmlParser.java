package ru.yandex.market.common.report.parser;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.xml.parser.AttributeHelpingElementListener;
import ru.yandex.common.util.xml.parser.AttributesHelper;
import ru.yandex.common.util.xml.parser.BigDecimalElementValueSetter;
import ru.yandex.common.util.xml.parser.ElementOrientedSAXHandler;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.parser.xml.MainMarketReportXmlParser;
import ru.yandex.market.common.report.parser.xml.MainMarketReportXmlParserSettings;

import java.math.BigDecimal;

/**
 * @author kukabara
 */
public class MainReportXmlParser extends MainMarketReportXmlParser<MainMarketReportXmlParserSettings, FoundOffer> {

    public MainReportXmlParser(MainMarketReportXmlParserSettings xmlSettings) {
        super(xmlSettings);

        addFeedIdListener();
        addFeedCategoryIdListener();
        addHyperCategIdListener();
        addHyperIdListener();
        addNameWithDelimListener();
        addDescriptionListener();
        addPriceListener();
        addCpcPriceListener();
        addCpaPriceListener();
        addCpaListener();
        addShopIdListener();
        addShopNameListener();
        addShopOfferIdListener();
        addShopCategoryIdListener();
        addUrlHashListener();
        addClassifierMagicIdListener();
        addDirectUrlListener();
        addWareMd5Listener();
        addWarrantyListener();
        addSalesNotesListener();
        addOriginCountryListener();
        addHyperCategNameListener();

        // Delivery
        addDeliveryListeners();
        addLocalDeliveryPrice();
        addOnStockListener();
        addPriorityRegionListener();
        addLocalDeliveryListener();

        // Promo
        addPromoTypeListener();

        setPromotedByVendorListenerAdded(true);

        // quantity-limit
        addQuantityLimitListener();
    }

    private void addCpcPriceListener() {
        addElementValueListener(
            getXmlSettings().getPathOffer() + "/" + getTagShopCpcPrice(),
            new BigDecimalElementValueSetter() {
                @Override
                public void setValue(BigDecimal value) {
                    onShopPrice(value);
                }
            });
        addElementListener(
            getXmlSettings().getPathOffer() + "/" + getTagShopCpcPrice(),
            new AttributeHelpingElementListener() {
                public void onOpen(ElementOrientedSAXHandler handler, AttributesHelper attr) {
                    Currency currency = getCurrency(attr.getString(getXmlSettings().getAttrCurrency()));
                    if (currency != null) {
                        onShopCurrency(currency);
                    }
                }
            });
    }

    private String getTagShopCpcPrice() {
        return "price_orig";
    }
}

