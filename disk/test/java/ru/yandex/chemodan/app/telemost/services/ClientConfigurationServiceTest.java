package ru.yandex.chemodan.app.telemost.services;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.exceptions.TelemostRuntimeException;

public class ClientConfigurationServiceTest extends TelemostBaseContextTest {

    private static final String V1_CONFIGURATION = "{\n" +
            "    \"v1\": {\n" +
            "        \"configuration\": \"v1_value\"\n" +
            "    }\n" +
            "}";

    private static final String V2_CONFIGURATION = "{\n" +
            "    \"v2\": {\n" +
            "        \"configuration\": \"v2_value\"\n" +
            "    }\n" +
            "}";

    private static final String FULL_CONFIGURATION = "{\n" +
            "    \"v1\": {\n" +
            "        \"configuration\": \"v1_value\"\n" +
            "    },\n" +
            "    \"v2\": {\n" +
            "        \"configuration\": \"v2_value\"\n" +
            "    }\n" +
            "}";

    @Autowired
    private ClientConfigurationService clientConfigurationService;

    @Test
    public void testV1ClientConfiguration() {
        clientConfigurationService.addNewClientConfiguration(V1_CONFIGURATION, "testUser", Option.empty());
        MapF<String, Object> v1Configuration = clientConfigurationService.getV1ClientConfiguration();
        Assert.assertNotNull(v1Configuration);
        Assert.assertTrue(v1Configuration.containsKeyTs("configuration"));
        Assert.assertEquals("v1_value", v1Configuration.getTs("configuration"));
        Assert.assertFalse(clientConfigurationService.getV2ClientConfiguration().isNotEmpty());
    }

    @Test
    public void testV2ClientConfiguration() {
        clientConfigurationService.addNewClientConfiguration(V2_CONFIGURATION, "testUser", Option.empty());
        MapF<String, Object> v2Configuration = clientConfigurationService.getV2ClientConfiguration();
        Assert.assertNotNull(v2Configuration);
        Assert.assertTrue(v2Configuration.containsKeyTs("configuration"));
        Assert.assertEquals("v2_value", v2Configuration.getTs("configuration"));
        Assert.assertFalse(clientConfigurationService.getV1ClientConfiguration().isNotEmpty());
    }

    @Test
    public void testVFullClientConfiguration() {
        clientConfigurationService.addNewClientConfiguration(FULL_CONFIGURATION, "testUser", Option.empty());
        MapF<String, Object> v1Configuration = clientConfigurationService.getV1ClientConfiguration();
        Assert.assertNotNull(v1Configuration);
        Assert.assertTrue(v1Configuration.containsKeyTs("configuration"));
        Assert.assertEquals("v1_value", v1Configuration.getTs("configuration"));
        MapF<String, Object> v2Configuration = clientConfigurationService.getV2ClientConfiguration();
        Assert.assertNotNull(v2Configuration);
        Assert.assertTrue(v2Configuration.containsKeyTs("configuration"));
        Assert.assertEquals("v2_value", v2Configuration.getTs("configuration"));
    }

    @Test(expected = TelemostRuntimeException.class)
    public void testInvalidJson() {
        clientConfigurationService.addNewClientConfiguration("bla-bla-bla", "testUser", Option.empty());
    }
}
