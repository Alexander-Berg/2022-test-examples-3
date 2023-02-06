package ru.yandex.market.checkout.checkouter.order.v2.multicart.actualize;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.MultiCartV2RequestMapper;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.AddressRequest;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.BuyerRequest;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.CartItemRequest;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.CartRequest;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.MultiCartRequest;
import ru.yandex.market.checkout.providers.v2.multicart.request.BuyerRequestProvider;
import ru.yandex.market.checkout.providers.v2.multicart.request.CartItemRequestProvider;
import ru.yandex.market.checkout.providers.v2.multicart.request.MultiCartRequestProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MultiCartV2RequestMapperTest {

    @Test
    public void mapperMultiCartTest() {
        MultiCartRequest multiCartRequest = MultiCartRequestProvider.buildRequest().build();
        MultiCart multiCart = MultiCartV2RequestMapper.mapToMultiCart(multiCartRequest);
        assertEquals(multiCartRequest.getSelectedCashbackOption(), multiCart.getSelectedCashbackOption());
        assertEquals(multiCartRequest.getCoinIdsToUse(), multiCart.getCoinIdsToUse());
    }

    @Test
    public void mapperBuyerTest() {
        BuyerRequest requestBuyer = BuyerRequestProvider.buildBuyer()
                .withPersonalPhoneId(":1q2w3e")
                .withPersonalEmailId("vrvrfbdrg")
                .withPersonalFullNameId("rgerg4grg")
                .withAddress(AddressRequest.builder()
                        .withPersonalAddressId("gbkmfgbljnrt")
                        .withPersonalGpsId("rgegrgd")
                        .build())
                .build();
        MultiCartRequest multiCartRequest = MultiCartRequest.builder()
                .withCarts(List.of(CartRequest.builder().withShopId(12L).build()))
                .withBuyer(requestBuyer).build();
        MultiCart actualMultiCart = MultiCartV2RequestMapper.mapToMultiCart(multiCartRequest);
        Buyer actualBuyer = actualMultiCart.getBuyer();
        assertEquals(requestBuyer.getUserId(), actualBuyer.getUid());
        assertEquals(requestBuyer.getIp(), actualBuyer.getIp());
        assertEquals(requestBuyer.getRegionId(), actualBuyer.getRegionId());
        assertEquals(requestBuyer.getLastName(), actualBuyer.getLastName());
        assertEquals(requestBuyer.getFirstName(), actualBuyer.getFirstName());
        assertEquals(requestBuyer.getMiddleName(), actualBuyer.getMiddleName());
        assertEquals(requestBuyer.getPhone(), actualBuyer.getPhone());
        assertEquals(requestBuyer.getEmail(), actualBuyer.getEmail());
        assertEquals(requestBuyer.getAssessor(), actualBuyer.getAssessor());
        assertEquals(requestBuyer.getIpRegionId(), actualBuyer.getIpRegionId());
        assertEquals(requestBuyer.getUserAgent(), actualBuyer.getUserAgent());
        assertEquals(requestBuyer.getBindKey(), actualBuyer.getBindKey());
        assertEquals(requestBuyer.getUnreadImportantEvents(), actualBuyer.getUnreadImportantEvents());
        assertEquals(requestBuyer.getUserEsiaToken(), actualBuyer.getUserEsiaToken());
        assertEquals(requestBuyer.getPersonalEmailId(), actualBuyer.getPersonalEmailId());
        assertEquals(requestBuyer.getPersonalPhoneId(), actualBuyer.getPersonalPhoneId());
        assertEquals(requestBuyer.getPersonalFullNameId(), actualBuyer.getPersonalFullNameId());

        Address buyerAddress = actualMultiCart.getCarts().get(0).getDelivery().getBuyerAddress();
        assertNotNull(requestBuyer.getAddress());
        assertNotNull(buyerAddress);

        assertEquals(requestBuyer.getAddress().getPersonalAddressId(), buyerAddress.getPersonalAddressId());
        assertEquals(requestBuyer.getAddress().getPersonalGpsId(), buyerAddress.getPersonalGpsId());
    }

    @Test
    public void mapperOrderTest() {
        BuyerRequest requestBuyer = BuyerRequestProvider.buildBuyer().build();
        MultiCartRequest multiCartRequest = MultiCartRequest.builder()
                .withBuyer(requestBuyer)
                .withCarts(List.of(CartRequest.builder()
                        .withLabel("labelDefault")
                        .withShopId(414L)
                        .build()))
                .build();
        MultiCart multiCart = MultiCartV2RequestMapper.mapToMultiCart(multiCartRequest);
        assertEquals(1, multiCart.getCarts().size());
        CartRequest cartRequest = multiCartRequest.getCarts().get(0);
        Order cart = multiCart.getCarts().get(0);
        assertEquals(cartRequest.getLabel(), cart.getLabel());
        assertEquals(cartRequest.getShopId(), cart.getShopId());
    }

    @Test
    public void mapperOrderItemTest() {
        BuyerRequest requestBuyer = BuyerRequestProvider.buildBuyer().build();
        MultiCartRequest multiCartRequest = MultiCartRequest.builder()
                .withBuyer(requestBuyer)
                .withCarts(List.of(CartRequest.builder()
                        .withShopId(414L)
                        .withItems(List.of(CartItemRequestProvider.buildItem().build()))
                        .build()))
                .build();
        MultiCart multiCart = MultiCartV2RequestMapper.mapToMultiCart(multiCartRequest);
        assertEquals(1, multiCart.getCarts().size());
        assertEquals(1, multiCart.getCarts().get(0).getItems().size());
        OrderItem item = multiCart.getCarts().get(0).getItems().iterator().next();
        CartItemRequest itemRequest = multiCartRequest.getCarts().get(0).getItems().get(0);
        assertEquals(itemRequest.getLabel(), item.getLabel());
        assertEquals(itemRequest.getBuyerPrice(), item.getBuyerPrice());
        assertEquals(itemRequest.getFeedId(), item.getFeedId());
        assertEquals(itemRequest.getOfferId(), item.getOfferId());
        assertEquals(itemRequest.getBundleId(), item.getBundleId());
        assertEquals(itemRequest.getCount(), item.getCount());
    }
}
