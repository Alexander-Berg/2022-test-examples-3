package ru.yandex.market.tpl.tms.service.enrich.dbqueue;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.common.report.model.OfferPicture;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.exception.CommandFailedException;
import ru.yandex.market.tpl.core.exception.TplOrderUpdateException;
import ru.yandex.market.tpl.core.service.order.enrich.dbqueue.EnrichOrderDataPayload;
import ru.yandex.market.tpl.core.util.TplUtils;
import ru.yandex.market.tpl.tms.service.external.TplCheckouterExternalService;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;
import ru.yandex.market.tpl.tms.utils.TplExternalUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class EnrichOrderDataServiceTest extends TplTmsAbstractTest {

    private final OrderGenerateService orderGenerateService;
    private final TplCheckouterExternalService checkouterExternalService;
    private final EnrichOrderDataService enrichOrderDataService;
    private final OrderRepository orderRepository;
    private final TransactionTemplate transactionTemplate;

    private Order order;

    @BeforeEach
    void setUp() {
        this.order = orderGenerateService.createOrder();
    }

    @Test
    void enrichOrder_withPictures_success() {
        //given
        EnrichOrderDataPayload payload = new EnrichOrderDataPayload("fake/request", this.order.getId());

        final var checkouterOrder = buildCorrectCheckouterResponseOrder(this.order);

        when(checkouterExternalService.getOrder(this.order.getExternalOrderId()))
                .thenReturn(checkouterOrder);

        //when
        enrichOrderDataService.processPayload(payload);

        //then
        transactionTemplate.execute(status -> {
            Order updatedOrder = orderRepository.findByIdOrThrow(this.order.getId());

            assertThat(updatedOrder.getItems().size()).isGreaterThan(0);
            assertThat(updatedOrder.getItems().size()).isEqualTo(checkouterOrder.getItems().size());

            updatedOrder.getItems()
                    .forEach(tplOrderItem -> {
                        var tplOiKey = TplUtils.TplOrder.EXTERNAL_ORDER_ITEM_KEY_EXTRACTOR.apply(tplOrderItem);

                        List<String> expectedPictureUrls = getExpectedPictureUrlsByOiKey(checkouterOrder, tplOiKey);

                        assertThat(tplOrderItem.getPictures()).hasSameElementsAs(expectedPictureUrls);
                    });
            return null;
        });
    }

    @Test
    void enrichOrder_withPictures_failure() {
        //given
        EnrichOrderDataPayload payload = new EnrichOrderDataPayload("fake/request", this.order.getId());

        final var checkouterOrder = buildEmptyCheckouterOrder();

        when(checkouterExternalService.getOrder(this.order.getExternalOrderId()))
                .thenReturn(checkouterOrder);

        //when
        //then
        Throwable cause = assertThrows(CommandFailedException.class,
                () -> enrichOrderDataService.processPayload(payload))
                .getCause();
        assertEquals(cause.getClass(), TplOrderUpdateException.class);
    }

    @NotNull
    private List<String> getExpectedPictureUrlsByOiKey(ru.yandex.market.checkout.checkouter.order.Order checkouterOrder,
                                    ru.yandex.market.tpl.core.domain.order.command.dto.ExternalOrderItemKeyDto tplOiKey) {
        return checkouterOrder
                .getItems()
                .stream()
                .filter(item -> TplExternalUtils.CheckouterOrder.EXTERNAL_ORDER_ITEM_KEY_EXTRACTOR
                        .apply(item).equals(tplOiKey))
                .map(OrderItem::getPictures)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(OfferPicture::getUrl)
                .collect(Collectors.toList());
    }

    private ru.yandex.market.checkout.checkouter.order.Order buildEmptyCheckouterOrder() {
        return new ru.yandex.market.checkout.checkouter.order.Order();
    }

    private ru.yandex.market.checkout.checkouter.order.Order buildCorrectCheckouterResponseOrder(Order order) {
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();

        List<OrderItem> orderItems = order.getItems()
                .stream()
                .map(orderItem -> {

                    OrderItem oi = new OrderItem();

                    oi.setSupplierId(orderItem.getVendorArticle().getVendorId());
                    oi.setShopSku(orderItem.getVendorArticle().getArticle());
                    oi.setOfferItemKey(new OfferItemKey(
                            "feedOfferId_" + RandomUtils.nextInt(),
                            RandomUtils.nextLong(),
                            "bundleId_" + RandomUtils.nextInt())
                    );

                    oi.setPictures(Stream.iterate(10, i -> i++)
                            .map(i -> new OfferPicture(RandomStringUtils.randomAlphanumeric(i)))
                            .limit(3)
                            .collect(Collectors.toList())
                    );
                    return oi;
                })
                .collect(Collectors.toList());

        checkouterOrder.setItems(orderItems);

        return checkouterOrder;
    }
}
