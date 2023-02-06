package stockstorage.testdata;

import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

public class TestService {

    public String getXml() {
        return IntegrationTestUtils.extractFileContent("test-xml.xml");
    }

    public String getJson() {
        return IntegrationTestUtils.extractFileContent("test-json.json");
    }
}
