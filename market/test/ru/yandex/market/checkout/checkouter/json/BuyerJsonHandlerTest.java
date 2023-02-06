package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.Buyer;

public class BuyerJsonHandlerTest extends AbstractJsonHandlerTestBase {

    public static final String JSON =
            "{\n" +
                    "  \"id\": \"id\",\n" +
                    "  \"uid\": 123,\n" +
                    "  \"muid\": 123412341234,\n" +
                    "  \"uuid\": \"111111111111\",\n" +
                    "  \"ip\": \"127.0.0.1\",\n" +
                    "  \"regionId\": 213,\n" +
                    "  \"lastName\": \"lastName\",\n" +
                    "  \"firstName\": \"firstName\",\n" +
                    "  \"middleName\": \"middleName\",\n" +
                    "  \"personalFullNameId\": \"149c57f6efdbe393aa5878c1b4d22006\",\n" +
                    "  \"phone\": \"+74952234562\",\n" +
                    "  \"personalPhoneId\": \"0123456789abcdef0123456789abcdef\",\n" +
                    "  \"email\": \"asd@gmail.com\",\n" +
                    "  \"personalEmailId\": \"51e7897da4fa5ec326206b1908fbc43d\",\n" +
                    "  \"dontCall\": true,\n" +
                    "  \"assessor\": false,\n" +
                    "  \"bindKey\": \"bind.key\",\n" +
                    "  \"beenCalled\": true,\n" +
                    "  \"unredImportantEvents\": 123,\n" +
                    "  \"ipRegionId\": 2,\n " +
                    "  \"userAgent\": \"chrome\",\n " +
                    "  \"yandexUid\": \"yandexuid\"\n " +
                    "}\n";

    @Test
    public void serialize() throws Exception {
        Buyer buyer = EntityHelper.getBuyer();

        String json = write(buyer);

        checkJson(json, "$." + Names.ID, EntityHelper.ID_STRING);
        checkJson(json, "$." + Names.Buyer.UID, (int) EntityHelper.UID);
        checkJson(json, "$." + Names.Buyer.MUID, EntityHelper.MUID);
        checkJson(json, "$." + Names.Buyer.MUID_STRING, String.valueOf(EntityHelper.MUID));
        checkJson(json, "$." + Names.Buyer.UUID, EntityHelper.UUID);
        checkJson(json, "$." + Names.Buyer.IP, EntityHelper.IP);
        checkJson(json, "$." + Names.REGION_ID, (int) EntityHelper.DELIVERY_REGION_ID);
        checkJson(json, "$." + Names.Buyer.LAST_NAME, EntityHelper.LAST_NAME);
        checkJson(json, "$." + Names.Buyer.FIRST_NAME, EntityHelper.FIRST_NAME);
        checkJson(json, "$." + Names.Buyer.MIDDLE_NAME, EntityHelper.MIDDLE_NAME);
        checkJson(json, "$." + Names.Buyer.PERSONAL_FULL_NAME_ID, EntityHelper.PERSONAL_FULL_NAME_ID);
        checkJson(json, "$." + Names.Buyer.PHONE, EntityHelper.PHONE);
        checkJson(json, "$." + Names.Buyer.PERSONAL_PHONE_ID, EntityHelper.PERSONAL_PHONE_ID);
        checkJson(json, "$." + Names.Buyer.EMAIL, EntityHelper.EMAIL);
        checkJson(json, "$." + Names.Buyer.PERSONAL_EMAIL_ID, EntityHelper.PERSONAL_EMAIL_ID);
        checkJson(json, "$." + Names.Buyer.DO_NOT_CALL, EntityHelper.DONT_CALL);
        checkJson(json, "$." + Names.Buyer.ASSESSOR, EntityHelper.ASSESSOR);
        checkJson(json, "$." + Names.Buyer.BIND_KEY, EntityHelper.BIND_KEY);
        checkJson(json, "$." + Names.Buyer.BEEN_CALLED, EntityHelper.BEEN_CALLED);
        checkJson(json, "$." + Names.Buyer.UNREAD_IMPORTANT_EVENTS, (int) EntityHelper.UNREAD_IMPORTANT_EVENTS);
        checkJson(json, "$." + Names.Buyer.USER_AGENT, EntityHelper.USER_AGENT);
        checkJson(json, "$." + Names.Buyer.YANDEX_UID, EntityHelper.YANDEX_UID);
    }

    @Test
    public void deserialize() throws Exception {
        Buyer buyer = read(Buyer.class, JSON);

        Assertions.assertEquals(EntityHelper.ID_STRING, buyer.getId());
        Assertions.assertEquals(EntityHelper.UID, buyer.getUid().longValue());
        Assertions.assertEquals(EntityHelper.MUID, buyer.getMuid().longValue());
        Assertions.assertEquals(EntityHelper.UUID, buyer.getUuid());
        Assertions.assertEquals(EntityHelper.IP, buyer.getIp());
        Assertions.assertEquals(EntityHelper.DELIVERY_REGION_ID, buyer.getRegionId().longValue());
        Assertions.assertEquals(2L, buyer.getIpRegionId().longValue());
        Assertions.assertEquals(EntityHelper.LAST_NAME, buyer.getLastName());
        Assertions.assertEquals(EntityHelper.FIRST_NAME, buyer.getFirstName());
        Assertions.assertEquals(EntityHelper.MIDDLE_NAME, buyer.getMiddleName());
        Assertions.assertEquals(EntityHelper.PERSONAL_FULL_NAME_ID, buyer.getPersonalFullNameId());
        Assertions.assertEquals(EntityHelper.PHONE, buyer.getPhone());
        Assertions.assertEquals(EntityHelper.PERSONAL_PHONE_ID, buyer.getPersonalPhoneId());
        Assertions.assertEquals(EntityHelper.EMAIL, buyer.getEmail());
        Assertions.assertEquals(EntityHelper.PERSONAL_EMAIL_ID, buyer.getPersonalEmailId());
        Assertions.assertEquals(EntityHelper.DONT_CALL, buyer.isDontCall());
        Assertions.assertEquals(EntityHelper.ASSESSOR, buyer.getAssessor());
        Assertions.assertEquals("yandexuid", buyer.getYandexUid());
        Assertions.assertEquals("chrome", buyer.getUserAgent());
        Assertions.assertEquals(2L, buyer.getIpRegionId().longValue());
        Assertions.assertEquals(EntityHelper.BIND_KEY, buyer.getBindKey());
        Assertions.assertEquals(EntityHelper.BEEN_CALLED, buyer.isBeenCalled());
        Assertions.assertEquals(EntityHelper.UNREAD_IMPORTANT_EVENTS, buyer.getUnreadImportantEvents().longValue());
    }

}
