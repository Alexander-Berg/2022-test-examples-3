package ru.yandex.market.checkout.pushapi.mbi;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.shop.entity.AllSettings;

public class MbiApiRestClient implements MbiApi {
    
    private String server;
    private RestTemplate restTemplate;

    @Required
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Required
    public void setServer(String server) {
        this.server = server;
    }

    @Override
    public AllSettings loadSettings() {
        return restTemplate.getForObject(server + "/push-api-settings", AllSettings.class);
    }

    @Override
    public Settings loadSettings(long shopId) {
        final AllSettings allSettings = restTemplate.getForObject(
            server + "/push-api-settings?shop_id=" + shopId, AllSettings.class
        );
        if(allSettings.containsKey(shopId)) {
            return allSettings.get(shopId);
        } else {
            throw new RuntimeException(
                "filtered settings doesn't contain settings for shop "
                    + shopId + ": " + allSettings.keySet()
            );
        }
    }
}
