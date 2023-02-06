package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;

public class ClientInfoJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        ClientInfo clientInfo = new ClientInfo(ClientRole.SHOP_USER, 123L, 234L);

        String json = write(clientInfo);

        checkJson(json, "$.role", ClientRole.SHOP_USER.name());
        checkJson(json, "$.uid", 123);
        checkJson(json, "$.id", 234);
        checkJson(json, "$.shopId", 234);
    }

    @Test
    public void serializeBusiness() throws Exception {
        ClientInfo clientInfo = new ClientInfo(ClientRole.BUSINESS, 345L);

        String json = write(clientInfo);

        checkJson(json, "$.role", ClientRole.BUSINESS.name());
        checkJson(json, "$.id", 345);
        checkJson(json, "$.businessId", 345);
    }

    @Test
    public void serializeBusinessUser() throws Exception {
        ClientInfo clientInfo = new ClientInfo(ClientRole.BUSINESS_USER, 123L, 234L, 345L);

        String json = write(clientInfo);

        checkJson(json, "$.role", ClientRole.BUSINESS_USER.name());
        checkJson(json, "$.uid", 123);
        checkJson(json, "$.id", 123);
        checkJson(json, "$.shopId", 234);
        checkJson(json, "$.businessId", 345);
    }

    @Test
    public void deserialize() throws Exception {
        String json = "{ \"role\": \"USER\", \"uid\": 234234 }";

        ClientInfo clientInfo = read(ClientInfo.class, json);

        Assertions.assertEquals(ClientRole.USER, clientInfo.getRole());
        Assertions.assertEquals(234234L, clientInfo.getId().longValue());
        Assertions.assertEquals(234234L, clientInfo.getUid().longValue());
        Assertions.assertNull(clientInfo.getShopId());
    }

    @Test
    public void deserializeBusiness() throws Exception {
        String json = "{ \"role\": \"BUSINESS\", \"businessId\": 345 }";
        ClientInfo clientInfo = read(ClientInfo.class, json);

        Assertions.assertEquals(ClientRole.BUSINESS, clientInfo.getRole());
        Assertions.assertEquals(345L, clientInfo.getId().longValue());
        Assertions.assertEquals(345L, clientInfo.getBusinessId().longValue());
    }

    @Test
    public void deserializeBusinessUser() throws Exception {
        String json = "{ \"role\": \"BUSINESS_USER\",  \"uid\": 123, \"businessId\": 345 }";
        ClientInfo clientInfo = read(ClientInfo.class, json);

        Assertions.assertEquals(ClientRole.BUSINESS_USER, clientInfo.getRole());
        Assertions.assertEquals(123L, clientInfo.getUid().longValue());
        Assertions.assertEquals(123L, clientInfo.getId().longValue());
        Assertions.assertEquals(345L, clientInfo.getBusinessId().longValue());
    }

    @Test
    public void deserialize2() throws Exception {
        String json = "{ \"role\": \"USER\", \"id\": 234234 }";

        ClientInfo clientInfo = read(ClientInfo.class, json);

        Assertions.assertEquals(ClientRole.USER, clientInfo.getRole());
        Assertions.assertEquals(234234L, clientInfo.getId().longValue());
        Assertions.assertEquals(234234L, clientInfo.getUid().longValue());
        Assertions.assertNull(clientInfo.getShopId());
    }

    @Test
    public void deserialize3() throws Exception {
        String json = "{ \"role\": \"SHOP\", \"id\": 234234 }";

        ClientInfo clientInfo = read(ClientInfo.class, json);

        Assertions.assertEquals(ClientRole.SHOP, clientInfo.getRole());
        Assertions.assertEquals(234234L, clientInfo.getId().longValue());
        Assertions.assertEquals(234234L, clientInfo.getShopId().longValue());
        Assertions.assertNull(clientInfo.getUid());
    }

    @Test
    public void deserialize4() throws Exception {
        String json = "{ \"role\": \"SHOP_USER\", \"uid\": 123, \"shopId\": 456 }";

        ClientInfo clientInfo = read(ClientInfo.class, json);

        Assertions.assertEquals(ClientRole.SHOP_USER, clientInfo.getRole());
        Assertions.assertEquals(123L, clientInfo.getUid().longValue());
        Assertions.assertEquals(456L, clientInfo.getId().longValue());
        Assertions.assertEquals(456L, clientInfo.getShopId().longValue());

    }


}
