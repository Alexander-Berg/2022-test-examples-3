package ru.yandex.market.mbo.cms.api.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.cms.AbstractTest;
import ru.yandex.market.mbo.cms.core.json.api.response.JsonPageApiResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mbo.cms.api.controller.UserContentController.DELETED_DOCUMENTS;

public class UserContentControllerTest  extends AbstractTest {
    private static final long USER_ID = 764301093; //market-cms-test-user
    private final JsonParser jsonParser = new JsonParser();

    @Autowired
    private UserContentController userContentServlet;

    @Test
    public void testProcess() {
        JsonPageApiResponse response = new JsonPageApiResponse(
            userContentServlet.findUserDocuments(USER_ID));
        JsonArray array = jsonParser.parse(response.getJsonObject().toString()).getAsJsonArray();
        assertEquals(2, array.size());

        response = new JsonPageApiResponse(userContentServlet.findUserDocuments(USER_ID));
        array = jsonParser.parse(response.getJsonObject().toString()).getAsJsonArray();
        assertEquals(0, array.size());
    }

    @Test
    public void testDelete() {
        JsonPageApiResponse response = new JsonPageApiResponse(
            userContentServlet.deleteUserDocuments(USER_ID));
        JsonObject object = jsonParser.parse(response.getJsonObject().toString()).getAsJsonObject();
        assertEquals(1, object.size());
        assertTrue(object.keySet().contains(DELETED_DOCUMENTS));
        assertEquals(0, object.get(DELETED_DOCUMENTS).getAsJsonArray().size());
    }
}
