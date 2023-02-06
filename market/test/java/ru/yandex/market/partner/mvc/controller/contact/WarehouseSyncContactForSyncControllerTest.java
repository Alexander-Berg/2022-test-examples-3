package ru.yandex.market.partner.mvc.controller.contact;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.mvc.controller.contact.model.ContactForChangesRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static ru.yandex.market.common.test.util.JsonTestUtil.assertEquals;

class WarehouseSyncContactForSyncControllerTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "warehouseSyncContactForSyncControllerTest.before.csv")
    public void getContactsForSync() {
         var businessId = 100L;
         var response = FunctionalTestHelper.post(
                 baseUrl + "businesses/" + businessId + "/contacts/for-change?_user_id=10",
                  new ContactForChangesRequest(List.of(1L, 2L, 3L, 4L)));
         var expected = "[" +
                 "{" +
                 "\"id\":4," +
                 "\"firstName\":\"D\"," +
                 "\"lastName\":\"D\"," +
                 "\"emails\":[" +
                        "\"тест@yandex.ru\"" +
                        "]" +
                 "},{" +
                 "\"id\":3," +
                 "\"firstName\":\"C\"," +
                 "\"lastName\":\"C\"," +
                 "\"emails\":[" +
                        "\"qwe@qwe.qwe\"" +
                        "]" +
                 "},{" +
                 "\"id\":2," +
                 "\"firstName\":\"B\"," +
                 "\"lastName\":\"B\"," +
                 "\"emails\":[" +
                        "\"test@yandex.ru\"" +
                        "]" +
                 "},{" +
                 "\"id\":1," +
                 "\"firstName\":\"A\"," +
                 "\"lastName\":\"A\"," +
                 "\"emails\":[" +
                        "\"yndx-mpi-1646410978181@yandex.ru\"," +
                        "\"qwe@qwe.qwe\"" +
                        "]" +
                 "}]";
         assertEquals(response, expected);
     }
}
