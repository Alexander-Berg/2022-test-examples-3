package ru.yandex.market.global.checkout.api;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.global.checkout.BaseApiTest;
import ru.yandex.market.global.common.util.configuration.ConfigurationService;
import ru.yandex.mj.generated.server.model.ConfigurationRow;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ConfigurationApiServiceTest extends BaseApiTest {

    private final ConfigurationApiService configurationApiService;

    private final ConfigurationService configurationService;

    @Test
    public void testEmpty() {
        ResponseEntity<List<ConfigurationRow>> rsp = configurationApiService.apiV1ConfigurationGetByKeyGet(List.of());
        Assertions.assertThat(rsp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(rsp.getBody()).isEmpty();
    }

    @Test
    public void testSuccess() {
        String testKey = "test123";
        String testValue = "value-value-value-value-value-value-value-value";
        String testKey2 = "test124";
        String testValue2 = "value-value-value-value-value-value-value-value5";
        configurationService.deleteValue(testKey);
        configurationService.deleteValue(testKey2);
        configurationService.insertValue(testKey, testValue);
        configurationService.insertValue(testKey2, testValue2);

        ResponseEntity<List<ConfigurationRow>> rsp = configurationApiService.apiV1ConfigurationGetByKeyGet(
                List.of(testKey, testKey2));
        Assertions.assertThat(rsp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(rsp.getBody()).containsExactly(
                new ConfigurationRow().key(testKey).value(testValue),
                new ConfigurationRow().key(testKey2).value(testValue2)
        );
    }

    @Test
    public void testNotFound() {
        String testKey = "test123";
        configurationService.deleteValue(testKey);

        ResponseEntity<List<ConfigurationRow>> rsp = configurationApiService.apiV1ConfigurationGetByKeyGet(
                List.of(testKey));
        Assertions.assertThat(rsp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(rsp.getBody()).containsExactly(
                new ConfigurationRow().key(testKey).value(null));
    }
}
