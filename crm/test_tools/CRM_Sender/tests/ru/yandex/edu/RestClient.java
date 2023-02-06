package ru.yandex.edu;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.core.CrmApi;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nasyrov on 15.04.2016.
 */
public class RestClient {

    @Test
    public void test() throws Exception {
        JsonNode o = Unirest
                .get("http://localhost/space/info")
                .asJson()
                .getBody();
        JSONObject v1 = o.getObject();
        Assert.assertTrue(v1.getBoolean("testMode"));
        //Object v2 = v1.getBoolean("user.id");
    }


    @Test
    public void testJsonPath() throws Exception {
        String json = Unirest
                .get("http://localhost/space/info")
                .asString()
                .getBody();
        Long u = JsonPath.read(json, "user.id");
        Assert.assertTrue(u > 0);
    }

    // http://unirest.io/java.html

    @Test
    public void testRestArgs() throws Exception {
        String json = Unirest
                .get("http://localhost/space/view/support/ticket")
                .queryString("id", 4801)
                .asString()
                .getBody();
        Long u = JsonPath.read(json, "owner.id");
        Assert.assertTrue(u > 0);
    }

    @Test
    public void testApiCore() throws Exception {
//        String data = getData("/view/support/ticket", new HashMap() {{
//            put("id", 4801);
//        }});
//
        String data = getData("/view/support/ticket",
                "id", 946);
        System.out.print(data);
        Long u = JsonPath.read(data, "owner.id");
        Assert.assertTrue(u > 0);


//        String data = getData("/view/support/ticket/list/my");

    }

    private String getData(String path, Map<String, Object> parameters) throws Exception {
        String json = Unirest
                //.get("http://localhost/space" + path)
                .get("https://tcrm-myt.yandex-team.ru/space" + path)
                //.header("")
                .queryString(parameters)
                .asString()
                .getBody();

        return json;
    }

    private String getData(String path, Object...args) throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        for (int i = 0; i < args.length; i+=2) {
            parameters.put(args[i].toString(), args[i+1]);
        }

        String json = Unirest
                //.get("http://localhost/space" + path)
                .get("http://tcrm-myt.yandex-team.ru/space" + path)
                        .header("Cookie", "devcrm=m-salina")
                .queryString(parameters)
                .asString()
                .getBody();

        return json;
    }

    @Test
    public void crmApiTest() throws Exception {
        DocumentContext data = new CrmApi("m-salina").getData("/info");
        Long u = data.read("user.id");
        Assert.assertTrue(u > 0);
    }
}
