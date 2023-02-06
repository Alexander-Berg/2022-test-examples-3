package ru.yandex.market.mbi.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.api.config.FunctionalTest;

@DbUnitDataSet(before = "solomon/SamovarEnvironment.before.csv")
public class SolomonAlertControllerTest extends FunctionalTest {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String endpoint = "/solomon/alert";

    @Test
    @DbUnitDataSet(after = {"solomon/SamovarEnvironmentOk.after.csv",
            "solomon/SamovarEnvironment.before.csv"})
    public void testSolomonOkStatus() {
        String requestBody = StringTestUtil.getString(this.getClass(), "solomon/SamovarOk.request.json");
        restTemplate.postForEntity(baseUrl() + endpoint, createRequest(requestBody), Void.TYPE);
    }

    @Test
    @DbUnitDataSet(after = {"solomon/SamovarEnvironmentWarn.after.csv",
            "solomon/SamovarEnvironment.before.csv"})
    public void testSolomonWarnStatus() {
        String requestBody = StringTestUtil.getString(this.getClass(), "solomon/SamovarWarn.request.json");
        restTemplate.postForEntity(baseUrl() + endpoint, createRequest(requestBody), Void.TYPE);
    }

    @Test
    @DbUnitDataSet(after = {"solomon/SamovarEnvironmentAlarm.after.csv",
            "solomon/SamovarEnvironment.before.csv"})
    public void testSolomonAlarmStatus() {
        String requestBody = StringTestUtil.getString(this.getClass(), "solomon/SamovarAlarm.request.json");
        restTemplate.postForEntity(baseUrl() + endpoint, createRequest(requestBody), Void.TYPE);
    }

    private HttpEntity<String> createRequest(String solomonAlertJson) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(solomonAlertJson, headers);
    }
}
