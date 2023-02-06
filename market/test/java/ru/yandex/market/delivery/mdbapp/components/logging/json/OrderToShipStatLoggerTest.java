package ru.yandex.market.delivery.mdbapp.components.logging.json;

import java.util.List;

public class OrderToShipStatLoggerTest
    extends AbstractJsonLoggerTest<OrderToShipStatLogger.OrderToShipStats, OrderToShipStatLogger> {

    public static final int UNPROCESSED_COUNT = 123456;
    public static final int OUTDATED_COUNT = 345678;
    public static final int PROCESSING_IMPOSSIBLE_COUNT = 567890;

    public OrderToShipStatLoggerTest() {
        super(OrderToShipStatLogger.OrderToShipStats.class);
    }

    @Override
    protected OrderToShipStatLogger createLogger() {
        return new OrderToShipStatLogger();
    }

    @Override
    protected List<OrderToShipStatLogger.OrderToShipStats> recordForTest() {
        return List.of(new OrderToShipStatLogger.OrderToShipStats()
            .setUnprocessedCount(UNPROCESSED_COUNT)
            .setOutdatedCount(OUTDATED_COUNT)
            .setProcessingImpossibleCount(PROCESSING_IMPOSSIBLE_COUNT)
        );
    }

    protected String[] getFilePath() {
        return new String[]{"json-reports", "order-to-ship-stat-json.log"};
    }
}
