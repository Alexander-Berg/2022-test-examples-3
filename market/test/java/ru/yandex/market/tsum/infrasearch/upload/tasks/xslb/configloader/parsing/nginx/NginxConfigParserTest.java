package ru.yandex.market.tsum.infrasearch.upload.tasks.xslb.configloader.parsing.nginx;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.tsum.infrasearch.upload.tasks.xslb.configloader.model.general.ConfigFile;
import ru.yandex.market.tsum.infrasearch.upload.tasks.xslb.configloader.model.nginx.NginxInfo;

import java.io.IOException;
import java.util.Optional;

import static ru.yandex.market.tsum.core.TestResourceLoader.getTestResourceAsString;

public class NginxConfigParserTest {
    private Optional<NginxInfo> nginxInfo;

    @Test
    public void testPoorParsing() throws IOException {
        prepareNginxInfo("infrasearch/upload/tasks/xslb/mslb/nginx/nginx_without_service_name.yaml");
        Assert.assertFalse(nginxInfo.isPresent());
    }

    @Test
    public void testParsingSimple() throws IOException {
        prepareNginxInfo("infrasearch/upload/tasks/xslb/mslb/nginx/nginx_simple.yaml");
        Assert.assertTrue(nginxInfo.isPresent());
        Assert.assertEquals(17053, nginxInfo.get().getUpstreamPort());
        Assert.assertEquals("service", nginxInfo.get().getServiceName());
        Assert.assertEquals(17053, nginxInfo.get().getPort());
    }

    @Test
    public void testParsingWithoutTemplate() throws IOException {
        prepareNginxInfo("infrasearch/upload/tasks/xslb/mslb/nginx/nginx_without_template.yaml");
        Assert.assertFalse(nginxInfo.isPresent());
    }

    private void prepareNginxInfo(String configName) throws IOException {
        String yaml = getTestResourceAsString(configName);
        nginxInfo = NginxConfigParser.getNginxInfoFromConfig(new ConfigFile("yaml", yaml));
    }
}
