package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api;

public class ApiSteps {
    private static final OrderSteps order = new OrderSteps();
    private static final TransferSteps transfer = new TransferSteps();
    private static final StocksSteps stocks = new StocksSteps();
    private static final InboundSteps inbound = new InboundSteps();
    private static final OutboundSteps outbound = new OutboundSteps();
    private static final IrisSteps iris = new IrisSteps();
    private static final AutostartSteps autostart = new AutostartSteps();
    private static final InventorizationSteps inventorization = new InventorizationSteps();
    private static final ReplenishmentSteps replenishment = new ReplenishmentSteps();

    public ApiSteps() {
    }

    public static OrderSteps Order() {
        return order;
    }

    public static TransferSteps Transfer() {
        return transfer;
    }

    public static StocksSteps Stocks() {
        return stocks;
    }

    public static InboundSteps Inbound() {
        return inbound;
    }

    public static OutboundSteps Outbound() {
        return outbound;
    }

    public static IrisSteps Iris() {
        return iris;
    }

    public static AutostartSteps Autostart() {
        return autostart;
    }

    public static InventorizationSteps Inventorization() {
        return inventorization;
    }

    public static ReplenishmentSteps replenishmentSteps() {
        return replenishment;
    }

}
