package step;

import java.util.Arrays;
import java.util.stream.Collectors;

import client.LavkaClient;
import dto.responses.lavka.TristeroOrderResponse;
import io.qameta.allure.Step;
import toolkit.Retrier;

public class LavkaSteps {

    private static final LavkaClient LAVKA_CLIENT = new LavkaClient();

    @Step("Формируем заказ в лавке")
    public void makeOrder(Long uid, String personalPhoneId, TristeroOrderResponse orderInfo, String gps) {
        Retrier.retry(() -> LAVKA_CLIENT.makeOrder(
            uid,
            personalPhoneId,
            orderInfo,
            Arrays.stream(gps.split(",")).map(Double::parseDouble).collect(Collectors.toList()))
        );
    }

}
