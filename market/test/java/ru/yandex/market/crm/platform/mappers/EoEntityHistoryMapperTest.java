package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.crm.platform.models.EoEntityHistory;
import ru.yandex.market.crm.util.CrmStrings;

public class EoEntityHistoryMapperTest {

    private final EoEntityHistoryMapper mapper = new EoEntityHistoryMapper();

    @Test
    public void doParse() throws Exception {
        String message = "{\"gid\":\"entityHistory@2384\",\"action\":\"CREATE\",\"entity\":{\"id\":2384," +
                "\"process\":\"create\",\"gid\":\"entityHistory@2384\",\"creationTime\":\"2021-03-25T11:22:56.123+05:00" +
                "\",\"author\":null,\"authorUid\":null,\"anImport\":null,\"description\":\"Объект создан\"," +
                "\"metaclass\":\"entityHistory\",\"entity\":\"serviceTime@2602\",\"automationRule\":null}," +
                "\"timestamp\":\"2021-03-25T11:22:56.622698+05:00\"}";

        List<EoEntityHistory> result = mapper.apply(CrmStrings.getBytes(message));

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());

        EoEntityHistory value = result.get(0);
        Assert.assertEquals("entityHistory@2384", value.getGid());
        Assert.assertEquals("Должны правильно распарсить '2021-03-25 11:22:56 +0500'", 1616653376123l,
                value.getCreationTime());
        Assert.assertEquals("serviceTime@2602", value.getGroup());
        Assert.assertEquals(parse("{\"id\":2384,\"process\":\"create\",\"gid\":\"entityHistory@2384\"," +
                "\"creationTime\":\"2021-03-25T11:22:56.123+05:00\",\"author\":null,\"authorUid\":null,\"anImport\":null," +
                "\"description\":\"Объект создан\",\"metaclass\":\"entityHistory\",\"entity\":\"serviceTime@2602\"," +
                "\"automationRule\":null}").toString(), parse(value.getEntity()).toString());

    }

    private JSONObject parse(String value) throws Exception {
        return new JSONObject(new JSONTokener(value));
    }

}
