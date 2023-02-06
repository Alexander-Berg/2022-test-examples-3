package ru.yandex.market.checkout.pushapi.service;

import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;

import java.util.UUID;

@Component
public class RandomizeTokenService {

    public Settings randomizeToken(Settings settings) {
        return new Settings(
            settings.getUrlPrefix(),
            UUID.randomUUID().toString().replace("-", ""),
            settings.getDataType(),
            settings.getAuthType(),
            settings.getFingerprint()
        );
    }

}
