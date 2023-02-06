package ru.yandex.market.tsum.pipelines.common.jobs.grafana.builder;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.core.environment.Environment;

public class SpokGrafanaDashboardBuilderTest {

    @Test
    public void testNannyDashboardBuilder() {
        String serviceId = "market-planner";
        String dashboardTitle = "Market " + serviceId + " auto dash";
        String solomonServiceId = serviceId + "-auto-dash";
        String solomonProjectId = serviceId;
        List<? extends EntityDescription> nannyEntityDescriptions = Arrays.asList(
            new NannyEntityDescription(
                "testing",
                "testing_market_mstat_planner",
                Arrays.asList("sas", "vla"),
                "marketmstatplanner",
                "market"
            ),
            new NannyEntityDescription(
                "production",
                "production_market_mstat_planner",
                Arrays.asList("sas", "vla"),
                "marketmstatplanner",
                "market"
            )
        );

        SpokGrafanaDashboardBuilder dashboardBuilder = new SpokGrafanaDashboardBuilder(
            567893L,
            "e9OKiPknz",
            dashboardTitle,
            solomonServiceId,
            solomonProjectId,
            List.of(Environment.TESTING, Environment.PRODUCTION),
            "PRODUCTION",
            e -> e.name().toUpperCase()
        )
            .withNannyDeployTimeline("MARKET_STAT_PLANNER_APP", "MARKET_STAT_PLANNER_WEBAPP")
            .addMainRow()
            .addTopHandlesRow()
            .addDescriptionRow(nannyEntityDescriptions)
            .addRpsRow()
            .addErrorsRow()
            .addTimingsRow()
            .addLivenessRow("ping", "monitoring")
            .addHandlesRow();

        JsonElement nannyDashboard = new Gson().toJsonTree(dashboardBuilder.build());
        JsonElement testDashboard = new Gson().fromJson(
            new JsonReader(getResourceAsReader("grafana_nanny_dashboard_test.json")),
            JsonElement.class
        );

        Assert.assertEquals(testDashboard, nannyDashboard);
    }

    private Reader getResourceAsReader(String resource) {
        return new InputStreamReader(Objects.requireNonNull(
            this.getClass().getResourceAsStream(resource)
        ));
    }
}
