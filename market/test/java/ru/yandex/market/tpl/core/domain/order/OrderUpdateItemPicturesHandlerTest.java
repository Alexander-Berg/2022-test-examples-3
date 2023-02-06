package ru.yandex.market.tpl.core.domain.order;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.order.command.dto.ExternalOrderItemKeyDto;
import ru.yandex.market.tpl.core.domain.order.command.dto.OrderItemPictureDto;
import ru.yandex.market.tpl.core.exception.TplOrderUpdateException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OrderUpdateItemPicturesHandlerTest {

    public static final String KEY_VENDOR_ARTICLE = "article";
    public static final long KEY_VENDOR_ID = 1L;

    @Test
    void handleUpdateItemPictures_success() {
        //given
        Order order = buildOrder();

        List<OrderItemPictureDto> expectedPictures = List.of(
                OrderItemPictureDto.of("newPic1"),
                OrderItemPictureDto.of("newPic2"),
                OrderItemPictureDto.of("newPic3"));

        Map<ExternalOrderItemKeyDto, List<OrderItemPictureDto>> itemsPictures = Map.of(
                ExternalOrderItemKeyDto.of(KEY_VENDOR_ID, KEY_VENDOR_ARTICLE), expectedPictures);

        //when
        OrderUpdateItemPicturesHandler handler = new OrderUpdateItemPicturesHandler(itemsPictures);
        handler.handle(order);

        //then
        assertThat(order.getItems().size()).isEqualTo(1);
        OrderItem orderItem = order.getItems().get(0);

        assertNotNull(orderItem.getPictures());
        assertThat(orderItem.getPictures().size()).isEqualTo(expectedPictures.size());
    }

    @Test
    void handleUpdateItemPictures_failure() {
        //given
        Order order = buildOrder();

        List<OrderItemPictureDto> expectedPictures = List.of(
                OrderItemPictureDto.of("newPic1"),
                OrderItemPictureDto.of("newPic2"),
                OrderItemPictureDto.of("newPic3"));

        Map<ExternalOrderItemKeyDto, List<OrderItemPictureDto>> itemsPictures = Map.of(
                ExternalOrderItemKeyDto.of(KEY_VENDOR_ID, KEY_VENDOR_ARTICLE), expectedPictures,
                ExternalOrderItemKeyDto.of(0, "FAIL_KEY_VENDOR_ARTICLE"), List.of());

        //when
        OrderUpdateItemPicturesHandler handler = new OrderUpdateItemPicturesHandler(itemsPictures);


        //then
        assertThrows(TplOrderUpdateException.class, () -> handler.handle(order));
    }

    private Order buildOrder() {
        Order order = new Order();
        order.setId(111L);
        order.setItems(Collections.singletonList(
                buildOrderItem(KEY_VENDOR_ARTICLE, KEY_VENDOR_ID, List.of(
                        "pic1",
                        "pic2"
                ))));
        return order;
    }

    private OrderItem buildOrderItem(String article, Long vendorId, List<String> pictures) {
        VendorArticle vendorArticle = new VendorArticle();
        vendorArticle.setArticle(article);
        vendorArticle.setVendorId(vendorId);

        OrderItem orderItem = new OrderItem();
        orderItem.setVendorArticle(vendorArticle);
        orderItem.setPictures(pictures);

        return orderItem;
    }
}
