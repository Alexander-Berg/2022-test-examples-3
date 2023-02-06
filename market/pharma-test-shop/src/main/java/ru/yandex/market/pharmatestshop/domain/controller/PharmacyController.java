package ru.yandex.market.pharmatestshop.domain.controller;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.pharmatestshop.domain.cart.CartRequest;
import ru.yandex.market.pharmatestshop.domain.cart.CartResponse;
import ru.yandex.market.pharmatestshop.domain.cart.CartService;
import ru.yandex.market.pharmatestshop.domain.json.TemplateResponse;
import ru.yandex.market.pharmatestshop.domain.order.OrderRequest;
import ru.yandex.market.pharmatestshop.domain.order.OrderResponse;
import ru.yandex.market.pharmatestshop.domain.order.OrderService;
import ru.yandex.market.pharmatestshop.domain.pharmacy.Pharmacy;
import ru.yandex.market.pharmatestshop.domain.pharmacy.PharmacyService;
import ru.yandex.market.pharmatestshop.domain.stock.Stock;
import ru.yandex.market.pharmatestshop.domain.stock.StockDto;
import ru.yandex.market.pharmatestshop.domain.stock.StockService;


@Slf4j
@Validated
@RestController
@RequestMapping(
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@RequiredArgsConstructor
public class PharmacyController {

    CartService cartService;
    PharmacyService pharmacyService;
    OrderService orderService;
    StockService stockService;

    @Autowired
    public PharmacyController(CartService cartService, PharmacyService pharmacyService, OrderService orderService,
                              StockService stockService) {
        this.cartService = cartService;
        this.pharmacyService = pharmacyService;
        this.orderService = orderService;
        this.stockService = stockService;
    }


    //Запрашивает у магазина информацию о товарах в корзине
    @PostMapping(value = {"/{shop_id}/cart"},
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public CartResponse getCart(@PathVariable Long shop_id, @Valid @RequestBody CartRequest requestBody) {

        log.info("POST:/cart; request body: {}", requestBody);
        //Соединяем данные в ответ
        Pharmacy currentPharmacy = pharmacyService.getPharmacy(shop_id);//Получаем информацию о текущем магазине
        if (!checkShopExists(shop_id, currentPharmacy)) {//Проверяем существует ли он
            throw new IllegalArgumentException("No pharmacy with such id: " + shop_id);
        }
        TemplateResponse templateResponse = new TemplateResponse();
        return templateResponse.getCartResponse(cartService.getResponse(requestBody.getCart()), currentPharmacy);

    }

    private boolean checkShopExists(Long shop_id, Pharmacy currentPharmacy) {
        if (currentPharmacy == null) {
            log.error("Can't find pharmacy with id {}", shop_id);
            return false;
        }
        return true;
    }

    //Насктройки магазина
    @GetMapping("/{shop_id}")
    public Pharmacy getPharmacy(@PathVariable Long shop_id) {
        return pharmacyService.getPharmacy(shop_id);
    }

    //Передает заказ магазину и запрашивает у магазина подтверждение принятия заказа.
    @PostMapping("/{shop_id}/order/accept")
    public OrderResponse getAccept(@RequestBody OrderRequest orderRequest) {
        log.info("POST: order/accept; request body: {}", orderRequest);


        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setOrder(orderService.getResponse(orderRequest.getOrder()));
        return orderResponse;

    }

    //Отправка статуса заказа (ответ не предусмотрен)
    @PostMapping("/{shop_id}/order/status")
    public void getStatus(@PathVariable Long shop_id, @RequestBody String orderRequest) {

        log.info("POST: order/status; request body: {}", orderRequest);

       int number= orderService.insertOrUpdateOrderJson(orderRequest, shop_id);

        log.info("POST: status finished successful");
    }


    //Отправка статуса заказа (ответ не предусмотрен)
    @PostMapping("/{shop_id}/stocks")
    public Stock getStock(@RequestBody StockDto orderRequest) {

        log.info("POST: /stocks; request body: {}", orderRequest);

        return stockService.getStock(orderRequest);
    }

}
