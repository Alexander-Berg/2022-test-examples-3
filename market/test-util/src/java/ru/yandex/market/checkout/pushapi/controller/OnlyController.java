package ru.yandex.market.checkout.pushapi.controller;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.ping.PushApiPingChecker;
import ru.yandex.market.checkout.pushapi.service.shop.ApiService;
import ru.yandex.market.checkout.pushapi.service.shop.ValidateService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Controller
public class OnlyController {
    
    private static final Logger log = Logger.getLogger(OnlyController.class);

    private ValidateService validateService;
    private ApiService apiService;
    private PushApiPingChecker pushApiPingChecker;

    @Autowired
    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    @Autowired
    public void setValidateService(ValidateService validateService) {
        this.validateService = validateService;
    }

    @Autowired
    public void setPushApiPingChecker(PushApiPingChecker pushApiPingChecker) {
        this.pushApiPingChecker = pushApiPingChecker;
    }

    @RequestMapping(value = "/shops/{shopId}/cart", method = RequestMethod.POST)
    public @ResponseBody CartResponse cart(
        @PathVariable long shopId,
        @RequestBody Cart cart,
        @RequestParam(value = "sandbox", defaultValue = "0") boolean sandbox,
        @RequestParam(value = "uid", defaultValue = "0") long uid
    ) {
        validateService.validateCart(cart);
        log.info("requested /shops/" + shopId + "/cart?sandbox=" + sandbox + " with " + cart);
        CartResponse response = apiService.cart(shopId, uid, cart, sandbox);
        log.info("response /shops/" + shopId + "/cart?sandbox=" + sandbox + ", DATA: " + response);
        return response;
    }
    
    @RequestMapping(value = "/shops/{shopId}/order/accept", method = RequestMethod.POST)
    public @ResponseBody OrderResponse orderAccept(
        @PathVariable long shopId,
        @RequestBody Order order,
        @RequestParam(value = "sandbox", defaultValue = "false") boolean sandbox
    ) {
        validateService.validateOrder(order);
        log.info("requested /shops/" + shopId + "/order/accept?sandbox=" + sandbox + " with " + order);
        return apiService.orderAccept(shopId, order, sandbox);
    }
    
    @RequestMapping(value = "/shops/{shopId}/order/status", method = RequestMethod.POST)
    public @ResponseBody void orderStatus(
        @PathVariable long shopId,
        @RequestBody Order statusChange,
        @RequestParam(value = "sandbox", defaultValue = "0") boolean sandbox
    ) {
        validateService.validateStatusChange(statusChange);
        log.info("requested /shops/" + shopId + "/order/status?sandbox=" + sandbox + " with " + statusChange);
        apiService.orderStatus(shopId, statusChange, sandbox);
    }
    
    @RequestMapping(value = "/shops/{shopId}/settings", method = RequestMethod.POST)
    public @ResponseBody void settings(@PathVariable long shopId, @RequestBody Settings settings) {
        validateService.validateSettings(settings);
        log.info("settings = " + settings);
     
        apiService.settings(shopId, settings);
    }

    @RequestMapping(value = "/shops/{shopId}/settings", method = RequestMethod.GET)
    public @ResponseBody Settings getSettings(@PathVariable long shopId) {
        return apiService.getSettings(shopId);
    }

    @RequestMapping(value = "/shops/{shopId}/cart/wrong-token", method = RequestMethod.POST)
    public @ResponseBody void wrongToken(
        @PathVariable long shopId,
        @RequestBody Cart cart,
        @RequestParam(value = "sandbox", defaultValue = "0") boolean sandbox,
        @RequestParam(value = "uid", defaultValue = "0") long uid
    ) {
        validateService.validateCart(cart);
        log.info("requested /shops/" + shopId + "/cart/wrong-token?sandbox=" + sandbox + " with " + cart);
        apiService.wrongTokenCart(shopId, uid, cart, sandbox);
    }

    @RequestMapping("/ping")
    public void ping(HttpServletResponse response) throws IOException {
        response.setContentType("text/plain; charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        final PrintWriter writer = response.getWriter();
        writer.write(pushApiPingChecker.check());
        writer.flush();
    }
    
}
