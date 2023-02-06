package ru.yandex.market.pharmatestshop.domain.client;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pharmatestshop.domain.pharmacy.Pharmacy;
import ru.yandex.market.pharmatestshop.domain.pharmacy.PharmacyService;

@Slf4j
@Component
public class MarketClient {

    private static final String MARKET_TESTING_URL = "https://api.partner.market.fslb.yandex.ru";
    @Value("${pharma-test-shop.oauth_token}")
    private static String oauth_token;
    @Value("${pharma-test-shop.oauth_client_id}")
    private static String oauth_client_id;

    final PharmacyService pharmacyService;

    @Autowired
    public MarketClient(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @Transactional
    public void putStatus(String orderJsonStatus, long orderId, long shopId) {
        try {
            //todo when web will be created
            Pharmacy pharmacy = pharmacyService.getPharmacy(shopId);
            String oauth_token = pharmacy.getOauthToken();
            String oauth_client_id = pharmacy.getOauthClientId();
            String campaignId = pharmacy.getCampaignId();

            String finalUrl = MARKET_TESTING_URL + "/campaigns/" + campaignId + "/orders/" + orderId + "/status";


            Map<String, List<String>> header = Map.of("Authorization",
                    List.of("OAuth oauth_token=\"" + oauth_token + "\", " +
                            "oauth_client_id=\"" + oauth_client_id + "\""));

            RestTemplate restTemplate = new RestTemplate();

            restTemplate.exchange(finalUrl, HttpMethod.PUT, new HttpEntity<>(
                    orderJsonStatus, new LinkedMultiValueMap<>(header)), Void.class);

//            restTemplate.put(finalUrl,
//                    new HttpEntity<>(
//                            orderJsonStatus, new LinkedMultiValueMap<>(header)), Void.class);
//
        } catch (RuntimeException e) {
            log.error("postForEntity error: " + e.getMessage());
        }
    }
}

