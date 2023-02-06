package ru.yandex.market.api.util.httpclient.clients;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import ru.yandex.market.api.personal.DataTypes;
import ru.yandex.market.api.personal.PersonalMultiTypesRequestable;
import ru.yandex.market.api.util.json.JsonSerializer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class PersonalTestClient extends AbstractFixedConfigurationTestClient {

    JsonSerializer jsonSerializer;

    protected PersonalTestClient(JsonSerializer jsonSerializer) {
        super("personal");
        this.jsonSerializer = jsonSerializer;

    }

    public void store(DataTypes dataType,
                      String valToStore,
                      boolean validate) {
        JSONObject request = new JSONObject();
        request.put("id", "123");
        request.put("value", "123");
        configure(x -> x
                .serverMethod(String.format("v1/%s/store", dataType))
                .post())
                .ok()
                .body(request.toString().getBytes(StandardCharsets.UTF_8));
    }

    public void multiTypesStore(List<PersonalMultiTypesRequestable> items) {
        JSONObject request = new JSONObject();
        JSONArray array = new JSONArray();
        items.forEach(i -> array.put(i.toPersonalRequestFormat()));

        request.put("items", array);
        configure(x -> x
                .serverMethod("/v1/multi_types/store")
                .post())
                .ok()
                .body(request.toString().getBytes(StandardCharsets.UTF_8));
    }
}
