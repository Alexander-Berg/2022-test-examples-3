package ru.yandex.travel.api.services.orders.happy_page;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import ru.yandex.travel.api.services.orders.OrderType;
import ru.yandex.travel.api.services.orders.happy_page.model.CrossSaleBlock;
import ru.yandex.travel.api.services.orders.happy_page.model.CrossSaleBlockType;
import ru.yandex.travel.api.services.orders.happy_page.model.HappyPageOrder;
import ru.yandex.travel.api.services.orders.happy_page.model.HotelHappyPageOrder;
import ru.yandex.travel.commons.http.apiclient.HttpApiRetryableException;
import ru.yandex.travel.testing.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class HappyPageBlockCollectorTest {
    private static final UUID TEST_ID = UUID.fromString("0-0-0-0-123");

    private final HappyPageBlockCollector blockCollector = new HappyPageBlockCollector();

    @Test
    public void testSuccessfulCompletion() {
        CompletableFuture<HappyPageOrder> info = CompletableFuture.supplyAsync(() -> {
            TestUtils.sleep(Duration.ofSeconds(2));
            return HotelHappyPageOrder.builder()
                    .id(TEST_ID)
                    .build();
        });

        CompletableFuture<CrossSaleBlock> block = CompletableFuture.completedFuture(
                CrossSaleBlock.builder()
                        .blockType(CrossSaleBlockType.PROMO)
                        .build()
        );

        var responseFuture = blockCollector.getHappyPageResponseFuture(info.join(), OrderType.HOTEL, List.of(block), Duration.ofSeconds(3));

        var response = responseFuture.join();
        assertThat(response.getOrderType()).isEqualByComparingTo(OrderType.HOTEL);
        assertThat(((HotelHappyPageOrder) response.getOrder()).getId()).isEqualTo(TEST_ID);
        assertThat(response.getCrossSale().getBlocks().size()).isEqualTo(1);
        assertThat(response.getCrossSale().getBlocks().get(0).getBlockType()).isEqualByComparingTo(CrossSaleBlockType.PROMO);
    }

    @Test
    public void testSuccessfulCompletionWithTimeout() {
        CompletableFuture<HappyPageOrder> info = CompletableFuture.completedFuture(
                HotelHappyPageOrder.builder()
                        .id(TEST_ID)
                        .build()
        );

        CompletableFuture<CrossSaleBlock> block = CompletableFuture.supplyAsync(() -> {
            TestUtils.sleep(Duration.ofSeconds(5));
            return CrossSaleBlock.builder()
                    .blockType(CrossSaleBlockType.PROMO)
                    .build();
        });

        var responseFuture = blockCollector.getHappyPageResponseFuture(info.join(), OrderType.HOTEL, List.of(block), Duration.ofSeconds(1));

        var response = responseFuture.join();
        assertThat(response.getOrderType()).isEqualByComparingTo(OrderType.HOTEL);
        assertThat(((HotelHappyPageOrder) response.getOrder()).getId()).isEqualTo(TEST_ID);
        assertThat(response.getCrossSale().getBlocks().size()).isEqualTo(0);
    }

    @Test
    public void testSuccessfulCompletionWithCrossSaleBlockError() {
        CompletableFuture<HappyPageOrder> info = CompletableFuture.completedFuture(
                HotelHappyPageOrder.builder()
                        .id(TEST_ID)
                        .build()
        );

        CompletableFuture<CrossSaleBlock> block = CompletableFuture.supplyAsync(() -> {
            throw new HttpApiRetryableException("test error");
        });
        CompletableFuture<CrossSaleBlock> block2 = CompletableFuture.supplyAsync(() -> CrossSaleBlock.builder()
                .blockType(CrossSaleBlockType.PROMO)
                .build());

        var responseFuture = blockCollector.getHappyPageResponseFuture(info.join(), OrderType.HOTEL, List.of(block, block2), Duration.ofSeconds(3));

        var response = responseFuture.join();
        assertThat(response.getOrderType()).isEqualByComparingTo(OrderType.HOTEL);
        assertThat(((HotelHappyPageOrder) response.getOrder()).getId()).isEqualTo(TEST_ID);
        assertThat(response.getCrossSale().getBlocks().size()).isEqualTo(1);
    }
}
