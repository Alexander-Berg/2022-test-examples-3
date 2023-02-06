package ru.yandex.market.checkout.helpers.utils.configuration;

import java.util.List;

import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;

public class MockConfiguration {

    private ReportGeneratorParameters reportParameters;
    private ReportGeneratorParameters deliveryRegionReportParameters;
    private List<SSItemAmount> stockStorageResponse;
    private StockStorageMockType stockStorageFreezeMockType;
    private List<DeliveryResponse> pushApiDeliveryResponses;
    private boolean shouldMockStockStorageGetAmountResponse = true;
    private boolean acceptOrder = true;
    private Boolean isShopAdmin = null;

    public ReportGeneratorParameters getReportParameters() {
        return reportParameters;
    }

    public void setReportParameters(ReportGeneratorParameters reportParameters) {
        this.reportParameters = reportParameters;
    }

    public ReportGeneratorParameters getDeliveryRegionReportParameters() {
        return deliveryRegionReportParameters;
    }

    public void setDeliveryRegionReportParameters(ReportGeneratorParameters deliveryRegionReportParameters) {
        this.deliveryRegionReportParameters = deliveryRegionReportParameters;
    }

    public List<SSItemAmount> getStockStorageResponse() {
        return stockStorageResponse;
    }

    public void setStockStorageResponse(List<SSItemAmount> stockStorageResponse) {
        this.stockStorageResponse = stockStorageResponse;
    }

    public StockStorageMockType getStockStorageFreezeMockType() {
        return stockStorageFreezeMockType;
    }

    public void setStockStorageFreezeMockType(StockStorageMockType stockStorageFreezeMockType) {
        this.stockStorageFreezeMockType = stockStorageFreezeMockType;
    }

    public List<DeliveryResponse> getPushApiDeliveryResponses() {
        return pushApiDeliveryResponses;
    }

    public void setPushApiDeliveryResponses(List<DeliveryResponse> pushApiDeliveryResponses) {
        this.pushApiDeliveryResponses = pushApiDeliveryResponses;
    }

    public boolean shouldMockStockStorageGetAmountResponse() {
        return shouldMockStockStorageGetAmountResponse;
    }

    public void setShouldMockStockStorageGetAmountResponse(boolean shouldMockStockStorageGetAmountResponse) {
        this.shouldMockStockStorageGetAmountResponse = shouldMockStockStorageGetAmountResponse;
    }

    public boolean isAcceptOrder() {
        return acceptOrder;
    }

    public void setAcceptOrder(boolean acceptOrder) {
        this.acceptOrder = acceptOrder;
    }

    public Boolean isShopAdmin() {
        return isShopAdmin;
    }

    public void setShopAdmin(boolean shopAdmin) {
        isShopAdmin = shopAdmin;
    }

    public enum StockStorageMockType {
        OK, ERROR, PREORDER_OK, PREORDER_ERROR, NO_STOCKS, REQUEST_TIMEOUT, NO
    }
}
