package ru.yandex.market.dashboard;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.auth.PassportClient;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;

/**
 * curl -X "POST" --cookie "Session_id=..." -H "Content-Type: application/json" -H "Authorization: Bearer your-token" https://grafana.yandex-team.ru/api/dashboards/db -d @dashboard.json
 *
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 13/05/15
 */
@Ignore
public class GrafanaDashboardTest {

    @Test
    public void testGenerateJson() throws Exception {
        List<String> timingMetricsFol = Arrays.asList(
            "one_min.logshatter-fol.timings.0_90",
            "one_min.logshatter-fol.timings.0_95",
            "one_min.logshatter-fol.timings.0_97",
            "one_min.logshatter-fol.timings.0_99"
        );
        List<String> timingMetricsSas = Arrays.asList(
            "one_min.logshatter-sas.timings.0_90",
            "one_min.logshatter-sas.timings.0_95",
            "one_min.logshatter-sas.timings.0_97",
            "one_min.logshatter-sas.timings.0_99"
        );

        DashboardGraph timingsGraphFol = new DashboardGraph("timings-fol", timingMetricsFol);
        DashboardGraph timingsGraphSal = new DashboardGraph("timings-sas", timingMetricsSas);

        List<String> countMetricsFol = Arrays.asList(
            "one_min.logshatter-fol.output-rps.TOTAL",
            "one_min.logshatter-fol.read-lps.TOTAL"
        );
        List<String> countMetricsSas = Arrays.asList(
            "one_min.logshatter-sas.output-rps.TOTAL",
            "one_min.logshatter-sas.read-lps.TOTAL"
        );

        DashboardGraph countGraphFol = new DashboardGraph("count-fol", countMetricsFol);
        DashboardGraph countGraphSas = new DashboardGraph("count-sas", countMetricsSas);

        DashboardRow rowFol = new DashboardRow(Arrays.asList(countGraphFol, timingsGraphFol));
        DashboardRow rowSas = new DashboardRow(Arrays.asList(countGraphSas, timingsGraphSal));

        GrafanaDashboard dashboard = new GrafanaDashboard(
            "dev-test", "test", "market", Arrays.asList(rowFol, rowSas), "market", "clickphite"
        );

//        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("/Users/andreevdm/tmp/ls.json"));
//        writer.write(dashboard.asJson().toString());
//        writer.close();

        System.out.println(dashboard.asJson().toString());

        GrafanaDashboardUploader uploader = new GrafanaDashboardUploader();
        uploader.setUploadUrl("https://grafana.yandex-team.ru/api/dashboards/db");
        uploader.setToken("eyJrIjoiNUlETTE4cXVSVTVITVFuWkFwTEtGRVJveVpmbUFrSTUiLCJuIjoiYW5kcmVldmRtIiwiaWQiOjF9");
        uploader.setLogin("zomb-market-infra");
        uploader.setPassword("6MLP(tbv");

        PassportClient passportClient = new PassportClient();
        passportClient.setPassportUrl("https://passport.yandex-team.ru");
        passportClient.afterPropertiesSet();

        uploader.setPassportClient(passportClient);

        uploader.afterPropertiesSet();

        uploader.getAndUpload(dashboard);
    }
}