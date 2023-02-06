package ru.yandex.market.tpl.carrier.driver.client;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.common.retrofit.RetrofitUtils;
import ru.yandex.market.javaframework.clients.config.ClientsAutoConfiguration;
import ru.yandex.market.javaframework.main.config.ClientServicesPropertiesConfiguration;
import ru.yandex.market.javaframework.main.config.TraceConfiguration;
import ru.yandex.market.javaframework.main.processors.YamlPropertiesEnvironmentOverridingProcessor;
import ru.yandex.market.tpl.carrier.driver.config.external.YandexMagistralApiConfigOverride;
import ru.yandex.market.tpl.common.web.tvm.DummyTvmClient;
import ru.yandex.mj.generated.client.yandex_magistral.api.YandexMagistralApiClient;
import ru.yandex.mj.generated.client.yandex_magistral.model.GetFlightsListByDriverResponse;
import ru.yandex.mj.generated.client.yandex_magistral.model.GetTokenDto;
import ru.yandex.mj.generated.client.yandex_magistral.model.GetTransportResponse;
import ru.yandex.mj.generated.client.yandex_magistral.model.OrderDto;
import ru.yandex.passport.tvmauth.TvmClient;

@ActiveProfiles("production")
@Disabled
@SpringBootTest
@TestPropertySource(
        properties = {
                "environment=production",
                "mj.trace.module=MARKET_TPL_CARRIER_DRIVER",
                "external.zora.proxy-enabled=false",
                "external.zora.host=go.zora.yandex.net",
                "external.zora.port=1080",
                "external.zora.tvm-client-id=2023123",
                "external.zora.service-name=market-carrier-driver"
        }
)
public class YaMagistralClientTest {

    @Autowired
    private YandexMagistralApiClient yandexMagistralApiClient;

    private String login = System.getenv("LOGIN");
    private String password = System.getenv("PASSWORD");


    @Test
    void shouldGetToken() {
        GetTokenDto getTokenDto = getToken();

        Assertions.assertThat(getTokenDto.getIsSuccess()).isTrue();
        Assertions.assertThat(getTokenDto.getMessage()).isNotNull();
    }

    private GetTokenDto getToken() {
        return RetrofitUtils.getResult(yandexMagistralApiClient.accountGetTokenPost(
                login, password
        ).schedule());
    }

    @Test
    void shouldGetFlights() {
        var token = getToken();
        Assertions.assertThat(token.getIsSuccess()).isTrue();
        Assertions.assertThat(token.getMessage()).isNotNull();

        GetFlightsListByDriverResponse flights =
                RetrofitUtils.getResult(yandexMagistralApiClient.flightsGetListByDriverPost(
                        "Bearer " + token.getMessage()
                ).schedule());

        Assertions.assertThat(flights.getIsSuccess()).isTrue();
        Assertions.assertThat(flights.getEntity()).isNotEmpty();
    }

    @Test
    void shouldGetOnlineFlights() {
        var token = getToken();
        Assertions.assertThat(token.getIsSuccess()).isTrue();
        Assertions.assertThat(token.getMessage()).isNotNull();

        var flights = RetrofitUtils.getResult(yandexMagistralApiClient.onlineFlightsGetListByDriverPost(
                "Bearer " + token.getMessage()
        ).schedule());

        Assertions.assertThat(flights.getIsSuccess()).isTrue();
        Assertions.assertThat(flights.getEntity()).isNotEmpty();
        System.out.println(flights);
    }

    @Test
    void shouldGetTransportData() {
        var token = getToken();
        Assertions.assertThat(token.getIsSuccess()).isTrue();
        Assertions.assertThat(token.getMessage()).isNotNull();

        String flightId = "28ea7fa5703e36479e605a56d806b987";
        String orderId = "91d28bc747bba51fde81248c3d021d76";

        GetTransportResponse transports = RetrofitUtils.getResult(yandexMagistralApiClient.flightsGetTransportDataPost(
                "Bearer " + token.getMessage(), flightId
        ).schedule());

        Assertions.assertThat(transports.getIsSuccess()).isTrue();

        Assertions.assertThat(Optional.of(transports.getEntity()).stream()
                        .flatMap(t -> t.getTransportCells().stream())
                        .flatMap(t -> t.getOrders().stream())
                        .filter(o -> o.getId().equals(orderId))
                        .map(OrderDto::getAmount)
                        .findAny()).isPresent();

    }

    @ImportAutoConfiguration(ClientsAutoConfiguration.class)
    @Import({
            YandexMagistralApiConfigOverride.class,

            ClientServicesPropertiesConfiguration.class,
            YamlPropertiesEnvironmentOverridingProcessor.class,
            TraceConfiguration.class,
    })
    @Configuration
    public static class YaMagistralClientConfiguration {
        @Bean
        public TvmClient tvmClient() {
            return new DummyTvmClient() {
                @Override
                public String getServiceTicketFor(int clientId) {
                    return System.getenv("TICKET");
                }

            };
        }
    }
}
