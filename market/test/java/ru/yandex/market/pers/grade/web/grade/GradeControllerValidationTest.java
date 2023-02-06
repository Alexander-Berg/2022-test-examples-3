package ru.yandex.market.pers.grade.web.grade;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author korolyov
 *         22.09.17
 */
public class GradeControllerValidationTest extends GradeControllerBaseTest {

    @Test
    public void photoNotEmptyExistsTest() throws Exception {
        String body = ADD_MODEL_GRADE_BODY.replaceAll("\"groupId\": \"abcde\"", "\"groupId\": \"\"");
        body = body.replaceAll("\"imageName\": \"iuirghreg\"", "\"imageName\": \"\"");
        String response = addModelGrade("UID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("'photos[0].groupId': rejected value []"));
        assertTrue(response.contains("'photos[0].imageName': rejected value []"));
    }

    @Test
    public void modelOrProductIdUidExistsTest() throws Exception {
        String body = ADD_MODEL_GRADE_BODY.replaceAll("\"product\"[^}]*},", "");
        String response = addModelGrade("UID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("ModelIdValid"));
    }

    @Test
    public void modelOrProductIdYandexUidExistsTest() throws Exception {
        String body = ADD_MODEL_GRADE_BODY.replaceAll("\"product\"[^}]*},", "");
        String response = addModelGrade("YANDEXUID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("ModelIdValid"));
    }

    @Test
    public void modelOrProductIdMoreThanZeroUidTest() throws Exception {
        String body = ADD_MODEL_GRADE_BODY.replace(String.valueOf(MODEL_ID), "0");
        String response = addModelGrade("UID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("ModelIdValid"));
    }

    @Test
    public void modelOrProductIdMoreThanZeroYandexUidTest() throws Exception {
        String body = ADD_MODEL_GRADE_BODY.replace(String.valueOf(MODEL_ID), "0");
        String response = addModelGrade("YANDEXUID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("ModelIdValid"));
    }

    @Test
    public void averageGradeValidUidTest() throws Exception {
        String body = ADD_MODEL_GRADE_BODY.replace("\"averageGrade\": 5", "\"averageGrade\": 10");
        String response = addModelGrade("UID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("field 'averageGrade': rejected value [10]"));
    }

    @Test
    public void averageGradeValidYandexUidTest() throws Exception {
        String body = ADD_MODEL_GRADE_BODY.replace("\"averageGrade\": 5", "\"averageGrade\": 10");
        String response = addModelGrade("YANDEXUID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("field 'averageGrade': rejected value [10]"));
    }

    @Test
    public void shopIdExistsUidTest() throws Exception {
        String body = ADD_SHOP_GRADE_BODY.replaceAll("\"shop\"[^}]*},", "");
        String response = addShopGrade("UID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("field 'shopDto': rejected value [null]"));
    }

    @Test
    public void shopIdExistsYandexUidTest() throws Exception {
        String body = ADD_SHOP_GRADE_BODY.replaceAll("\"shop\"[^}]*},", "");
        String response = addShopGrade("YANDEXUID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("field 'shopDto': rejected value [null]"));
    }

    @Test
    public void shopIdMoreThanZeroUidTest() throws Exception {
        String body = ADD_SHOP_GRADE_BODY.replaceAll(String.valueOf(SHOP_ID), "0");
        String response = addShopGrade("UID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("on field 'shopDto.id': rejected value [0]"));
    }

    @Test
    public void shopIdMoreThanZeroYandexUidTest() throws Exception {
        String body = ADD_SHOP_GRADE_BODY.replaceAll(String.valueOf(SHOP_ID), "0");
        String response = addShopGrade("YANDEXUID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("on field 'shopDto.id': rejected value [0]"));
    }

    @Test
    public void orderIdUidLengthTest() throws Exception {
        String bigOrderId = String.join("", Collections.nCopies(51, "a"));
        String body = ADD_SHOP_GRADE_BODY.replaceAll(ORDER_ID, bigOrderId);
        String response = addShopGrade("UID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("Field error in object 'shopGradeRequestDto' on field 'orderId'"));
    }

    @Test
    public void orderIdYandexUidLengthTest() throws Exception {
        String bigOrderId = String.join("", Collections.nCopies(51, "a"));
        String body = ADD_SHOP_GRADE_BODY.replaceAll(ORDER_ID, bigOrderId);
        String response = addShopGrade("YANDEXUID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("Field error in object 'shopGradeRequestDto' on field 'orderId'"));
    }

    @Test
    public void proLengthPositiveTest() throws Exception {
        String longText = String.join("", Collections.nCopies(2000, "a"));
        String body = ADD_MODEL_GRADE_BODY.replace(PRO_MODEL_GRADE, longText);
        addModelGrade("UID", String.valueOf(FAKE_USER), body, status().isOk());
    }

    @Test
    public void proLengthNegativeTest() throws Exception {
        String longText = String.join("", Collections.nCopies(2001, "a"));
        String body = ADD_MODEL_GRADE_BODY.replace(PRO_MODEL_GRADE, longText);
        String response = addModelGrade("UID", String.valueOf(FAKE_USER), body, status().is4xxClientError());
        assertTrue(response.contains("Field error in object 'modelGradeRequestDto' on field 'pro'"));
    }
}
