package ru.yandex.travel.integration.spark;

import java.time.Duration;

import io.opentracing.mock.MockTracer;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.integration.spark.responses.CompanyShortReportResponse;
import ru.yandex.travel.integration.spark.responses.EntrepreneurShortReportResponse;

@Slf4j
@Ignore
public class SparkClientLocalTest {

    private static final String SPARK_BASE_URL = "http://webservicefarm.interfax.ru/iFaxWebService/iFaxWebService.asmx";
    private SparkClient sparkClient;

    @Before
    public void setup() {
        AsyncHttpClient sparkAhc = Dsl.asyncHttpClient(Dsl.config().setThreadPoolName("localSparkApiTestAhc"));
        AsyncHttpClientWrapper ahcWrapper = new AsyncHttpClientWrapper(sparkAhc,
                LoggerFactory.getLogger("HttpLogger"), "default", new MockTracer(), null);
        SparkClientProperties properties = SparkClientProperties.builder()
                .baseUrl(SPARK_BASE_URL)
                .httpReadTimeout(Duration.ofSeconds(10))
                .httpRequestTimeout(Duration.ofSeconds(10))
                .login("login")
                .password("password")
                .sessionTtl(Duration.ofMinutes(55))
                .build();
        sparkClient = new SparkClient(ahcWrapper, properties);
    }

    @Test
    public void getCompanyShortReport() {
        CompanyShortReportResponse report = sparkClient.getCompanyShortReportSync("7802742850");
        log.info("Returned response: " + report);
    }

    @Test
    public void getEntrepreneurShortReport() {
        EntrepreneurShortReportResponse report = sparkClient.getEntrepreneurShortReportSync("381101250840");
        log.info("Returned response: " + report);
    }

}
