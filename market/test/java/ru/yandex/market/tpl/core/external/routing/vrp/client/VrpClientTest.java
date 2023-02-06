package ru.yandex.market.tpl.core.external.routing.vrp.client;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.tpl.common.web.config.TplProfiles;
import ru.yandex.market.tpl.core.config.TplTestConfigurations;
import ru.yandex.market.tpl.core.config.external.VrpRoutingConfiguration;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpResponse;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpResponseRoutes;
import ru.yandex.market.tpl.core.external.routing.vrp.model.RouteNode;
import ru.yandex.market.tpl.core.external.routing.vrp.model.TaskInfo;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettingsProvider;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.global.GlobalSettingsProvider;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.tag.RoutingOrderTagProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author ungomma
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "external.vrp.apiUrl=https://courier.yandex.ru/vrs/api/v1/",
        "external.vrp.apiKey=whatever"
})
@ContextConfiguration(classes = {
        VrpRoutingConfiguration.class,
        TplTestConfigurations.ClockConfig.class
})
@ActiveProfiles(TplProfiles.TESTS)
class VrpClientTest {

    @Autowired
    private VrpClientImpl vrpClient;
    @MockBean
    private GlobalSettingsProvider configurationProviderAdapter;
    @MockBean
    private RoutingOrderTagProvider routingOrderTagQueryService;
    @MockBean
    private DepotSettingsProvider sortingCenterPropertyService;

    @Test
    void shouldParseResponseJson() throws Exception {
        MockRestServiceServer server = MockRestServiceServer.bindTo(vrpClient.restTemplate).build();

        server.expect(once(),
                requestTo(Matchers.startsWith("https://courier.yandex.ru/vrs/api/v1/result/svrp/65246109-83502184" +
                        "-3e5dfcee-43e34512?apikey="))
        )
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ClassPathResource("vrp/vrp_good_response.json"),
                        MediaType.APPLICATION_JSON));

        TaskInfo taskInfo = vrpClient.getTaskResult(VrpClient.ApiType.SVRP, "65246109-83502184-3e5dfcee-43e34512");

        MvrpResponse result = taskInfo.getResult();
        assertThat(result).isNotNull();
        assertThat(result.getSolverStatus()).isEqualTo(MvrpResponse.SolverStatusEnum.SOLVED);
        assertThat(result.getDroppedLocations()).isEmpty();

        assertThat(result.getRoutes()).hasSize(1);
        assertThat(result.getRoutes().get(0).getRoute()).hasSize(6);
        assertThat(result.getRoutes())
                .flatExtracting(MvrpResponseRoutes::getRoute)
                .extracting(RouteNode::getArrivalTimeS)
                .doesNotContainNull();

        assertThat(result.getVehicles()).hasSize(1);

    }

}
