package ru.yandex.market.billing.fulfillment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.junit.jupiter.api.Test;

import ru.yandex.market.billing.fulfillment.SalesReportNotificationExecutor.WarehouseStock;
import ru.yandex.market.core.delivery.DeliveryServiceInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

public class SalesReportMailInfoConverterTest {

    @Test
    void checkConvertion() {
        SalesReportMailInfoConverter converter = new SalesReportMailInfoConverter();
        Element result = converter.convert(predefinedMailInfo());
        checkRootNode(result);
        checkWarehousesNode(result.getChild("warehouse-stocks"));
    }

    private SalesReportNotificationExecutor.SalesReportMailInfo predefinedMailInfo() {
        SalesReportNotificationExecutor.SalesReportMailInfo mailInfo =
                new SalesReportNotificationExecutor.SalesReportMailInfo();

        mailInfo.setSupplierName("Supplo");
        mailInfo.setCampaignId(100L);
        mailInfo.setOrderedSkus(10L);
        mailInfo.setOrderedItems(100L);
        mailInfo.setOrderedItemsCost(new BigDecimal("2000"));
        mailInfo.setSkuCountInStock(30L);
        mailInfo.setItemCountInStock(50L);
        mailInfo.setSkuCountToSupply(100L);
        mailInfo.setItemCountToSupply(50L);

        mailInfo.setWarehouseStocks(
                List.of(createWarehouse(147, "Яндекс.Маркет (Ростов-на-Дону)"),
                        createWarehouse(171, "Яндекс.Маркет (Томилино)"))
        );

        return mailInfo;
    }

    private WarehouseStock createWarehouse(long id, String name) {
        WarehouseStock stock = new WarehouseStock(new DeliveryServiceInfo(id, name));
        stock.addOrderedItems(10, new BigDecimal("500"), "sku-1");
        stock.addOrderedItems(10, new BigDecimal("700"), "sku-2");
        stock.addItemsInStock(20, new BigDecimal("100"), "sku-3");
        stock.addItemsInStock(30, new BigDecimal("40"), "sku-4");
        stock.addSuggestItems(100, new BigDecimal("1000"), "sku-5");
        stock.addSuggestItems(30, new BigDecimal("200"), "sku-6");
        return stock;
    }

    private void checkRootNode(Element root) {
        assertThat(root.getName(), is("sales-report-mail-info"));
        assertThat(root.getChildren().size(), is(10));
        List<String> nodeNames = new ArrayList<>();
        root.getChildren().forEach(node -> nodeNames.add(((Element) node).getName()));
        assertThat(nodeNames, containsInAnyOrder(
                "supplier-name", "campaign-id",
                "ordered-skus", "ordered-items", "ordered-items-cost",
                "sku-count-in-stock", "item-count-in-stock",
                "sku-count-to-supply", "item-count-to-supply",
                "warehouse-stocks"));
        assertThat(root.getChild("ordered-items-cost").getContent(0).getValue(), is("2 000"));
    }

    private void checkWarehousesNode(Element warehousesNode) {
        assertThat(warehousesNode.getName(), is("warehouse-stocks"));
        assertThat(warehousesNode.getChildren().size(), is(2));
        Element stock = warehousesNode.getChild("warehouse-stock");
        assertThat(stock.getChildren().size(), is(10));
        List<String> nodeNames = new ArrayList<>();
        stock.getChildren().forEach(node -> nodeNames.add(((Element) node).getName()));
        assertThat(nodeNames, containsInAnyOrder(
                "warehouse-name",
                "ordered-skus", "ordered-items", "ordered-cost",
                "skus-in-stock", "items-in-stock", "items-in-stock-cost",
                "suggest-skus", "suggest-items", "suggest-cost"));
        assertThat(stock.getChild("items-in-stock-cost").getContent(0).getValue(), is("3 200"));
    }
}
