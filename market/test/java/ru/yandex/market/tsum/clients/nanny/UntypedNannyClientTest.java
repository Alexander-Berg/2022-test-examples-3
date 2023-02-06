package ru.yandex.market.tsum.clients.nanny;

import java.util.Optional;

import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.tsum.clients.nanny.service.untypedservice.InfoAttrs;
import ru.yandex.market.tsum.clients.nanny.service.untypedservice.UntypedNannyService;

import static org.junit.Assert.assertEquals;


/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 01/09/2017
 */
public class UntypedNannyClientTest {
    private static final String TEMPLATE_SERVICE = "testing_market_template_service_for_java_iva";
    private static final String TEST_CATEGORY = "/market/tsum/test";

    private final UntypedNannyClient untypedNannyClient = new UntypedNannyClient(
        "https://nanny.yandex-team.ru/", "AQAD-qJSJwR8AAABT2AmPI5EfUS_k4lv3Mw2nz0"
    );

    @Test
    @Ignore
    public void getServiceObject() throws Exception {
        Optional<UntypedNannyService> service = untypedNannyClient.getService(TEMPLATE_SERVICE);
        Assert.assertTrue(service.isPresent());
        Assert.assertEquals(TEMPLATE_SERVICE, service.get().getServiceName());
        Assert.assertNotNull(service.get().getAuthAttrs().getContent());
        Assert.assertNotNull(service.get().getInfoAttrs().getContent());
        Assert.assertNotNull(service.get().getRuntimeAttrs().getContent());
    }

    @Test
    @Ignore
    public void copyService() throws Exception {
        String serviceName = "testing_market_copied_service";
        UntypedNannyService copiedService = untypedNannyClient.copyService(
            TEMPLATE_SERVICE,
            serviceName,
            TEST_CATEGORY
        );
        Assert.assertEquals(serviceName, copiedService.getServiceName());
    }

    @Test
    @Ignore
    public void postService() throws Exception {
        String serviceName = "testing_market_created_service";
        Optional<UntypedNannyService> templateService = untypedNannyClient.getService(TEMPLATE_SERVICE);
        Assert.assertTrue(templateService.isPresent());
        UntypedNannyService newService = templateService.get();
        newService.setServiceName(serviceName);
        newService.getInfoAttrs().setCategory(TEST_CATEGORY);
        UntypedNannyService createdService = untypedNannyClient.postService(newService, "TSUM Test");
        Assert.assertEquals(serviceName, createdService.getServiceName());
    }

    @Test
    public void prepareAttributeRequest() throws Exception {
        JsonObject content = new JsonObject();
        content.addProperty("category", "/market/front/desktop/");

        JsonObject infoAttrsJsonObject = new JsonObject();
        infoAttrsJsonObject.addProperty("_id", "1234");
        infoAttrsJsonObject.add("content", content);

        InfoAttrs infoAttrs = new InfoAttrs(infoAttrsJsonObject);
        JsonObject request = untypedNannyClient.prepareAttributeRequest(infoAttrs, "test");

        assertEquals("/market/front/desktop/", request.getAsJsonObject("content")
            .getAsJsonPrimitive("category").getAsString()
        );
    }
}
