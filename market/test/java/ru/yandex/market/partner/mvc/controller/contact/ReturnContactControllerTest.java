package ru.yandex.market.partner.mvc.controller.contact;

import java.util.List;
import java.util.Objects;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReturnContactControllerTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "ReturnContactControllerTest.getContact.csv")
    public void testGetReturnContactSuccess() {
        var expected = "{\"returnContacts\":" +
                "[{" +
                "\"partnerId\": 1, " +
                "\"address\": \"Ларек Митино\", " +
                "\"phoneNumber\": \"+79151002030\", " +
                "\"type\": \"FEEDBACK\", " +
                "\"isEnabled\": true, " +
                "\"workSchedule\": \"расписание\"" +
                "}," +
                "{" +
                "\"partnerId\": 1, " +
                "\"address\": \"Ларек Митино\", " +
                "\"comments\": \"Позвонить шаурмастеру\", " +
                "\"companyName\": \"Шаурмячная\", " +
                "\"jobPosition\": \"Шаурмэн\", " +
                "\"email\": \"egor@yandex.ru\", " +
                "\"lastName\": \"Андреев\", " +
                "\"firstName\": \"Егор\", " +
                "\"middleName\": \"Александрович\", " +
                "\"phoneNumber\": \"+79151002030\", " +
                "\"isEnabled\": true, " +
                "\"type\": \"PERSON\"" +
                "}" +
                "]}";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "partner/1/contacts/return");
        String body = Objects.requireNonNull(response.getBody());
        JSONObject result = new JSONObject(body).getJSONObject("result");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONAssert.assertEquals(expected, result, true);
    }

    @Test
    @DbUnitDataSet(before = "ReturnContactControllerTest.getContact.csv")
    public void testGetReturnContactEmpty() {
        var expected = "{\"returnContacts\": []}";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "partner/3/contacts/return");
        String body = Objects.requireNonNull(response.getBody());
        JSONObject result = new JSONObject(body).getJSONObject("result");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONAssert.assertEquals(expected, result, true);
    }

    @Test
    @DbUnitDataSet(before = "ReturnContactControllerTest.getContact.csv",
            after = "ReturnContactControllerTest.putContact.csv")
    public void testGetReturnContactUpdate() {
        ReturnContactDto personContact = new ReturnContactDto();
        personContact.setPartnerId(1);
        personContact.setEmail("new@yandex.ru");
        personContact.setFirstName("Иван");
        personContact.setLastName("Иванов");
        personContact.setMiddleName("Иванович");
        personContact.setPhoneNumber("+79998877666");
        personContact.setType(ReturnContactType.PERSON);
        personContact.setAddress("Красная площадь");
        personContact.setCompanyName("Музей");
        personContact.setJobPosition("Завхоз");
        personContact.setComments("Вах какой контакт");
        personContact.setIsEnabled(Boolean.TRUE);

        ReturnContactDto feedbackContact = new ReturnContactDto();
        feedbackContact.setPartnerId(1);
        feedbackContact.setPhoneNumber("+799988771111");
        feedbackContact.setAddress("новый адрес");
        feedbackContact.setWorkSchedule("новое расписание");
        feedbackContact.setType(ReturnContactType.FEEDBACK);
        feedbackContact.setIsEnabled(Boolean.TRUE);

        ReturnContactDtoList returnContactDtoList = new ReturnContactDtoList();
        returnContactDtoList.setReturnContacts(List.of(personContact, feedbackContact));
        FunctionalTestHelper.put(baseUrl + "partner/1/contacts/return?uid=1", returnContactDtoList);
    }
}
