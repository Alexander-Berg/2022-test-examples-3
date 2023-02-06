package ru.yandex.market.tsum.clients.nanny.service.untypedservice;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 05/09/2017
 */
public class InfoAttrsTest {
    private JsonObject infoAttrsContentObject;
    private InfoAttrs infoAttrs;

    @Before
    public void setUp() throws Exception {
        JsonObject jsonObject = new Gson().fromJson(
            Resources.toString(Resources.getResource("nanny/infoAttrs.json"), Charsets.UTF_8), JsonObject.class
        );
        infoAttrsContentObject = jsonObject.getAsJsonObject("content");
        infoAttrs = new InfoAttrs(jsonObject);
    }

    @Test
    public void addRecipeParam() {
        infoAttrs.addRecipeParam("force", true);
        JsonObject paramObject = infoAttrsContentObject.getAsJsonObject("recipes")
            .getAsJsonArray("content")
            .get(0)
            .getAsJsonObject()
            .getAsJsonArray("context")
            .get(2)
            .getAsJsonObject();

        assertEquals(paramObject.getAsJsonPrimitive("key").getAsString(), "force");
        assertEquals(paramObject.getAsJsonPrimitive("value").getAsString(), "true");
    }


    @Test
    public void disableTicketIntegration() throws Exception {
        assertTrue(infoAttrsContentObject.has("tickets_integration"));
        infoAttrs.disableTicketIntegration();
        assertFalse(infoAttrsContentObject.has("tickets_integration"));
    }

    @Test
    public void setCategory() throws Exception {
        infoAttrs.setCategory("/market/front/touch");
        assertEquals("/market/front/touch", infoAttrsContentObject
            .getAsJsonPrimitive("category").getAsString()
        );

    }

    @Test
    public void disableDiskQuotasParam() {
        infoAttrs.disableDiskQuotasParam();
        assertEquals(
            "DISABLED",
            infoAttrsContentObject.getAsJsonObject("disk_quotas").get("policy").getAsString()
        );
    }

}
