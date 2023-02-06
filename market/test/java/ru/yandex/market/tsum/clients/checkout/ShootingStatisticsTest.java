package ru.yandex.market.tsum.clients.checkout;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

public class ShootingStatisticsTest {
    private static final String SHOOTING_STATISTICS_JSON = "{\"deliveryTimings\":{\"duration80\":100.0," +
        "\"duration90\":200.0,\"duration95\":400.0,\"duration100\":1000.0},\"orders\":{\"created\":1000," +
        "\"notCreated\":5,\"coinApplied\":100,\"promoCodeApplied\":250,\"cashbackApplied\":150,\"flashApplied\":200," +
        "\"distributionToCount\":[{\"distribution\":[1,1,1],\"count\":10},{\"distribution\":[2,2],\"count\":15}]}}\n";

    @Test
    public void deserializationOk() {
        ShootingStatistics shootingStatistics = CheckouterApiClient.GSON.fromJson(SHOOTING_STATISTICS_JSON,
            ShootingStatistics.class);
        Assert.assertEquals(expectedShootingStatistics(), shootingStatistics);
    }

    private static ShootingStatistics expectedShootingStatistics() {
        ShootingStatistics stat = new ShootingStatistics();
        DeliveryTimings deliveryTimings = new DeliveryTimings();
        deliveryTimings.setDuration80(100);
        deliveryTimings.setDuration90(200);
        deliveryTimings.setDuration95(400);
        deliveryTimings.setDuration100(1000);
        stat.setDeliveryTimings(deliveryTimings);

        Orders orders = new Orders();
        orders.setCreated(1000L);
        orders.setNotCreated(5L);
        orders.setCoinApplied(100L);
        orders.setCashbackApplied(150L);
        orders.setFlashApplied(200L);
        orders.setPromoCodeApplied(250);
        orders.setDistributionToCount(Arrays.asList(
            toDistributionCount(10, 1, 1, 1),
            toDistributionCount(15, 2, 2)
        ));

        stat.setOrders(orders);
        return stat;
    }

    private static Orders.DistributionCountTuple toDistributionCount(int count, Integer... distribution) {
        Orders.DistributionCountTuple d = new Orders.DistributionCountTuple();
        d.setDistribution(Arrays.stream(distribution).map(Long::valueOf).collect(Collectors.toList()));
        d.setCount(count);
        return d;
    }
}
