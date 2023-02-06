package ru.yandex.market.volva.tank.generators;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import lombok.SneakyThrows;

import ru.yandex.market.volva.tank.TankAmmo;
import ru.yandex.market.volva.tank.headers.HeaderFactory;

/**
 * @author dzvyagin
 */
public class SimpleRequestGenerator implements RequestGenerator {

    private final Collection<HeaderFactory> headerFactories;
    private final Random random = new Random();
    private final VolvaRoundGenerator volvaRoundGenerator = new VolvaRoundGenerator();

    public SimpleRequestGenerator(Collection<HeaderFactory> headerFactories) {
        this.headerFactories = headerFactories;
        headerFactories.forEach(HeaderFactory::init);
    }


    @Override
    public TankAmmo generate() {
        return generateVolvaAmmo();
    }

    @SneakyThrows
    private TankAmmo generateVolvaAmmo() {
        VolvaRoundGenerator.Round round = volvaRoundGenerator.generateVolvaRound();
        return fromRound(round);
    }


    private TankAmmo fromRound(VolvaRoundGenerator.Round round) {
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
        if (body != null) {
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
