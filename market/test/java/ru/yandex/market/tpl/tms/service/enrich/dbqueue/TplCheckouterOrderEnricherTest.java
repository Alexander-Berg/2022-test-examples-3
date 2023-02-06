package ru.yandex.market.tpl.tms.service.enrich.dbqueue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.common.report.model.OfferPicture;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.command.dto.ExternalOrderItemKeyDto;
import ru.yandex.market.tpl.core.domain.order.command.dto.OrderItemPictureDto;
import ru.yandex.market.tpl.tms.service.external.TplCheckouterExternalService;
import ru.yandex.market.tpl.tms.utils.TplExternalUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TplCheckouterOrderEnricherTest {

    public static final int PICRURES_QTY = 3;
    @Mock
    private OrderCommandService orderCommandService;
    @Mock
    private TplCheckouterExternalService tplCheckouterClient;
    @InjectMocks
    private TplCheckouterOrderEnricher tplCheckouterOrderEnricher;

    @Captor
    private ArgumentCaptor<OrderCommand.UpdateItemsPictures> commandCaptor;

    @Test
    public void checkouterEnrich_success() {
        //given
        long orderId = 1L;
        String externalOrderId = "10";

        List<OrderItem> orderItems = List.of(
                buildOrderItem(10L, "shopSku10"),
                buildOrderItem(20L, "shopSku20")
        );

        Order checkouterOrder = buildCorrectCheckouterResponseOrder(orderItems);
        when(tplCheckouterClient.getOrder(externalOrderId)).thenReturn(checkouterOrder);

        //when
        tplCheckouterOrderEnricher.enrich(orderId, externalOrderId);

        //then
        verify(orderCommandService).updateItemsPictures(commandCaptor.capture());

        OrderCommand.UpdateItemsPictures command = commandCaptor.getValue();

        assertThat(command.getItemsPictures().size()).isEqualTo(2L);

        Map<ExternalOrderItemKeyDto, List<OrderItemPictureDto>> itemsPictures = command.getItemsPictures();

        orderItems
                .forEach(orderItem -> {
                    var oiKey =
                            TplExternalUtils.CheckouterOrder.EXTERNAL_ORDER_ITEM_KEY_EXTRACTOR.apply(orderItem);

                    assertThat(itemsPictures.containsKey(oiKey)).isEqualTo(true);
                    assertThat(itemsPictures.get(oiKey).size()).isEqualTo(PICRURES_QTY);

                });
    }

    private Order buildCorrectCheckouterResponseOrder(List<OrderItem> orderItems) {
        var checkouterOrder = new Order();
        checkouterOrder.setItems(orderItems);
        return checkouterOrder;
    }

    private OrderItem buildOrderItem(Long supplierId, String shopSku) {
        OrderItem oi = new OrderItem();

        oi.setSupplierId(supplierId);
        oi.setShopSku(shopSku);

        oi.setOfferItemKey(new OfferItemKey(
                "feedOfferId_" + RandomUtils.nextInt(),
                RandomUtils.nextLong(),
                "bundleId_" + RandomUtils.nextInt())
        );
        oi.setPictures(Stream.iterate(10, i -> i++)
                .map(i -> new OfferPicture(RandomStringUtils.randomAlphanumeric(i)))
                .limit(PICRURES_QTY)
                .collect(Collectors.toList())
        );
        return oi;
    }
}
