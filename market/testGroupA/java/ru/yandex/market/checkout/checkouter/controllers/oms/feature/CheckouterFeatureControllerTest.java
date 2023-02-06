package ru.yandex.market.checkout.checkouter.controllers.oms.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.checkout.application.AbstractContainerTestBase;
import ru.yandex.market.checkout.checkouter.controllers.service.PublicSupportController;
import ru.yandex.market.checkout.checkouter.controllers.service.SupportController;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolver;
import ru.yandex.market.checkout.checkouter.feature.PutFeatureRequest;
import ru.yandex.market.checkout.checkouter.feature.type.NamedFeature;
import ru.yandex.market.checkout.checkouter.feature.type.NamedFeatureTypeRegister;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;

import static ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType.FISCAL_AGENT_TYPE_ENABLED;
import static ru.yandex.market.checkout.checkouter.feature.type.common.IntegerFeatureType.MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT;

public class CheckouterFeatureControllerTest extends AbstractContainerTestBase {
    @Autowired
    PublicSupportController publicSupportController;
    @Autowired
    SupportController supportController;
    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    CheckouterProperties checkouterProperties;
    @Autowired
    private CheckouterFeatureResolver checkouterFeatureResolver;

    @Test
    public void pojoToStringWithoutErrors() {
        var response = testRestTemplate.getForEntity("/features", String.class);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotEquals("[]", response.getBody());
    }

    @Test
    public void getCommonFeatures() {
        featuresByViews(publicSupportController.getCommonFeatures(null, null),
                NamedFeatureTypeRegister::commonFeatureByName);
    }

    @Test
    public void getLoggingFeatures() {
        featuresByViews(publicSupportController.getLoggingFeatures(null, null),
                NamedFeatureTypeRegister::loggingFeatureByName);
    }

    @Test
    public void getPermanentFeatures() {
        featuresByViews(publicSupportController.getPermanentFeatures(null, null),
                NamedFeatureTypeRegister::permanentFeatureByName);
    }

    private void featuresByViews(List<CheckouterFeatureView> features, Function<String,
            NamedFeature<?>> featureFromRegister) {
        Assertions.assertFalse(features.isEmpty());
        var exceptions = new ArrayList<Exception>();
        features.forEach(featureView -> {
            try {
                featureFromRegister.apply(featureView.getFeatureName());
            } catch (Exception e) {
                exceptions.add(e);
            }
        });
        Assertions.assertTrue(exceptions.isEmpty(), exceptions.toString());
    }

    @Test
    public void putFeature() {
        var switchWithWhiteListValue = new SwitchWithWhitelist<>(true, Set.of(9223372036854775805L,
                9223372036854775806L, 9223372036854775807L));
        var payload = new Gson().toJson(switchWithWhiteListValue);
        var request = new PutFeatureRequest(payload, "Somebody", "Some reason");

        supportController.putFeature(FISCAL_AGENT_TYPE_ENABLED.getName(), request);

        Assertions.assertEquals(switchWithWhiteListValue,
                checkouterFeatureResolver.getAsTargetType(FISCAL_AGENT_TYPE_ENABLED, SwitchWithWhitelist.class));
    }

    @Test
    public void getFeatureHistories() {
        checkouterFeatureResolver.writeValue(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT, 1);

        var history = publicSupportController.getHistory(null, null);
        Assertions.assertTrue(history.size() >= 1);
    }

    @Test
    public void getHistories() {
        checkouterFeatureResolver.writeValue(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT, 1);
        checkouterFeatureResolver.writeValue(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT, 2);
        checkouterFeatureResolver.writeValue(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT, 3);

        var history = publicSupportController.getFeatureHistory(
                MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT.getName(), null, null);
        Assertions.assertTrue(history.size() >= 3);
    }

    @Test
    public void code400ForBadFeatureValue() {
        var switchWithWhiteListValue = new SwitchWithWhitelist<>(true, Set.of(9223372036854775805L,
                9223372036854775806L, 9223372036854775807L));

        var goodFeatureValue = new Gson().toJson(switchWithWhiteListValue);
        var badFeatureValue = goodFeatureValue + "}}";

        Assertions.assertEquals(HttpStatus.OK, doPutRequest(goodFeatureValue));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, doPutRequest(badFeatureValue));
    }

    private HttpStatus doPutRequest(String featureValue) {
        var request = new PutFeatureRequest(featureValue, "Somebody", "Some reason");
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");

        return testRestTemplate.exchange("/features/" + FISCAL_AGENT_TYPE_ENABLED.getName(), HttpMethod.PUT,
                new HttpEntity<>(request, headers), String.class).getStatusCode();
    }
}
