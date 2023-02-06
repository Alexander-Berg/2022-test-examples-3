package ru.yandex.market.antifraud.orders.tanking.generators;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import lombok.SneakyThrows;

import ru.yandex.market.antifraud.orders.tanking.TankAmmo;
import ru.yandex.market.antifraud.orders.tanking.headers.HeaderFactory;


/**
 * @author dzvyagin
 */
public class SimpleRequestGenerator implements RequestGenerator {

    private final Collection<HeaderFactory> headerFactories;
    private final Random random = new Random();
    private final AntifraudRoundGenerator antifraudRoundGenerator = new AntifraudRoundGenerator();

    private final List<String> queries = Arrays.asList(
            "pillow",
            "xiaomi",
            "huawei",
            "watch",
            "hdd",
            "glasses"
    );

    public SimpleRequestGenerator(Collection<HeaderFactory> headerFactories) {
        this.headerFactories = headerFactories;
        headerFactories.forEach(HeaderFactory::init);
    }


    @Override
    public TankAmmo generate() {
        int pick = random.nextInt(6);
        switch (pick) {
            case 0:
                return generateOrderAmmo();
            default:
                return generateLoyaltyAmmo();
        }
    }

    @SneakyThrows
    private TankAmmo generateLoyaltyAmmo() {
        AntifraudRoundGenerator.Round round = antifraudRoundGenerator.generateLoyaltyRound();
        return fromRound(round);
    }

    @SneakyThrows
    private TankAmmo generateOrderAmmo() {
        AntifraudRoundGenerator.Round round = antifraudRoundGenerator.generateOrderRound();
        return fromRound(round);
    }

    private TankAmmo fromRound(AntifraudRoundGenerator.Round round){
        return generateAmmo(round.getLabel(),
                round.getMethod() + " " + round.getUrl() + " " + round.getHttpVersion(),
                round.getBody());
    }

    private TankAmmo generateAmmo(String id, String request) {
        return generateAmmo(id, request, null);
    }

    private TankAmmo generateAmmo(String id, String request, String body) {
        TankAmmo ammo = new TankAmmo();
        ammo.setId(" " + id);
        ammo.setRequest(request);
        ammo.setBody(body);
        Map<String, String> headers = generateHeaders();
        if (body != null){
            headers.put("Content-Length", String.valueOf(body.getBytes().length));
        }
        ammo.setHeaders(headers);
        return ammo;
    }

    private Map<String, String> generateHeaders() {
        Map<String, String> headersMap = new HashMap<>();
        headerFactories.forEach(hf -> headersMap.put(hf.getHeaderName(), hf.getHeader(null)));
        return headersMap;
    }

}
