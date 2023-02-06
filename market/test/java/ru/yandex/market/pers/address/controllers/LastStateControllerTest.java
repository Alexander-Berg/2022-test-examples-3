package ru.yandex.market.pers.address.controllers;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.address.config.TestClient;
import ru.yandex.market.pers.address.controllers.model.LastStateDto;
import ru.yandex.market.pers.address.controllers.model.PaymentMethod;
import ru.yandex.market.pers.address.controllers.model.PaymentType;
import ru.yandex.market.pers.address.model.identity.Identity;
import ru.yandex.market.pers.address.util.BaseWebTest;
import ru.yandex.market.pers.address.util.Utils;

public class LastStateControllerTest extends BaseWebTest {

    private static final String USER_ID = "0";
    private static final String USER_TYPE = "uid";
    private static final Identity<?> IDENTITY = Utils.createIdentity(USER_ID, USER_TYPE);
    private static final String PAYMENT_OPTION_ID = "{6F9619FF-8B86-D011-B42D-00CF4FC964FF}";
    private static final String CONTACT_ID = "contact_id";

    @Autowired
    private TestClient testClient;

    @Test
    public void shouldSaveNewLastState() throws Exception {
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(IDENTITY));

        LastStateDto rq = buildDefault().build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto response = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rq, response);
    }

    @Test
    public void shouldSaveNewLastStateWithNullParams() throws Exception {
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(IDENTITY));

        LastStateDto rq = buildWithNullValues();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto response = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rq, response);
    }

    @Test
    public void shouldSaveRewriteLastState() throws Exception {
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(IDENTITY));

        LastStateDto rq = buildDefault().build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto responseBeforeUpdate = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rq, responseBeforeUpdate);

        LastStateDto rqUpdate = new LastStateDto.Builder()
                .setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .setPaymentType(PaymentType.POSTPAID)
                .setPaymentOptionId(PAYMENT_OPTION_ID)
                .setContactId("123")
                .build();
        testClient.addLastState(rqUpdate, IDENTITY);
        LastStateDto responseAfterUpdate = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rqUpdate, responseAfterUpdate);
    }

    @Test
    public void shouldSaveNewLastStateTwoUsers() throws Exception {
        Identity<?> user1 = Utils.createIdentity("1", "uid");
        Identity<?> user2 = Utils.createIdentity("2", "uid");
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(user1));
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(user2));

        LastStateDto rqUser1 = buildDefault().setContactId("contact_1").build();
        testClient.addLastState(rqUser1, user1);

        LastStateDto rqUser2 = new LastStateDto.Builder()
                .setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY)
                .setPaymentType(PaymentType.POSTPAID)
                .setPaymentOptionId(PAYMENT_OPTION_ID)
                .setContactId("contact_2")
                .build();
        testClient.addLastState(rqUser2, user2);

        LastStateDto responseUser1 = testClient.getLastState(user1);
        Assertions.assertEquals(rqUser1, responseUser1);

        LastStateDto responseUser2 = testClient.getLastState(user2);
        Assertions.assertEquals(rqUser2, responseUser2);
    }

    @Test
    public void shouldUpdateLastState() throws Exception {
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(IDENTITY));

        LastStateDto rq = buildDefault().build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto responseBeforeUpdate = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rq, responseBeforeUpdate);

        LastStateDto rqUpdate = new LastStateDto.Builder()
                .setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .setPaymentType(PaymentType.POSTPAID)
                .setPaymentOptionId(PAYMENT_OPTION_ID)
                .setContactId("123")
                .build();
        testClient.updateLastState(rqUpdate, IDENTITY);
        LastStateDto responseAfterUpdate = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rqUpdate, responseAfterUpdate);
    }

    @Test
    public void shouldUpdateLastStateWithNullVal() throws Exception {
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(IDENTITY));

        LastStateDto rq = buildDefault().build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto responseBeforeUpdate = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rq, responseBeforeUpdate);

        LastStateDto rqUpdate = buildWithNullValues();
        testClient.updateLastState(rqUpdate, IDENTITY);
        LastStateDto responseAfterUpdate = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rqUpdate, responseAfterUpdate);
    }

    @Test
    public void shouldDeleteLastState() throws Exception {
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(IDENTITY));

        LastStateDto rq = buildDefault().build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto responseBeforeDelete = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rq, responseBeforeDelete);

        testClient.deleteLastState(IDENTITY);
        LastStateDto responseAfterDelete = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(buildWithNullValues(), responseAfterDelete);
    }

    @Test
    public void shouldUpdateWithoutLastState() throws Exception {
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(IDENTITY));

        LastStateDto rq = buildDefault().build();
        testClient.updateLastState(rq, IDENTITY);
        LastStateDto rs = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rq, rs);
    }

    private LastStateDto.Builder buildDefault() {
        return new LastStateDto.Builder()
                .setPaymentMethod(PaymentMethod.YANDEX)
                .setPaymentType(PaymentType.PREPAID)
                .setPaymentOptionId(PAYMENT_OPTION_ID)
                .setContactId(CONTACT_ID);
    }

    @Test
    public void shouldSaveNewLastStateWithParcels() throws Exception {
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(IDENTITY));
        String parcelInfo = getParcelsInfoJson();
        LastStateDto rq = buildDefault().setParcelsInfo(parcelInfo).build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto response = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rq, response);
    }

    @Test
    public void shouldSaveNewLastStateWithEmptyParcels() throws Exception {
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(IDENTITY));
        String parcelInfo = "{\"parcels\": []}";
        LastStateDto rq = buildDefault().setParcelsInfo(parcelInfo).build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto response = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rq, response);
    }

    @Test
    public void shouldSaveNewLastStateWithAdditionalProp() throws Exception {
        String parcelInfo = getParcelsInfoJson();
        String parcelInfoWithAddProp = parcelInfo.substring(0, parcelInfo.length() - 3) + ", \"addProp\": \"x\"}]}";
        LastStateDto rq =
                buildDefault().setParcelsInfo(parcelInfoWithAddProp).build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto response = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(parcelInfo, response.getParcelsInfo());
    }

    @Test
    public void shouldUpdateParcelsInfo() throws Exception {
        String parcelInfo = "{\"parcels\": []}";
        String parcelInfoUpdate = getParcelsInfoJson();
        LastStateDto rq = buildDefault().setParcelsInfo(parcelInfo).build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto rqUpdate = buildDefault().setParcelsInfo(parcelInfoUpdate).build();
        testClient.updateLastState(rqUpdate, IDENTITY);
        LastStateDto response = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rqUpdate, response);
    }

    @Test
    public void shouldUpdateParcelsInfoWithDefault() throws Exception {
        String parcelsInfoJson = getParcelsInfoJsonWithDeliveryFeatureDefault();
        LastStateDto rq = buildDefault().setParcelsInfo(parcelsInfoJson).build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto response = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(parcelsInfoJson, response.getParcelsInfo());
    }

    @Test
    public void shouldUpdateParcelsInfoDeliveryFeatureEmpty() throws Exception {
        String parcelsInfoJson = getParcelsInfoJsonWithDeliveryFeatureEmpty();
        LastStateDto rq = buildDefault().setParcelsInfo(parcelsInfoJson).build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto response = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(getParcelsInfoJsonWithDeliveryFeatureDefault(), response.getParcelsInfo());
    }

    @Test
    public void shouldGetLastStateWithPaymentTinkoffInstallments() throws Exception {
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(IDENTITY));
        PaymentMethod paymentMethod = PaymentMethod.TINKOFF_INSTALLMENTS;

        LastStateDto rq = buildDefault().setPaymentMethod(paymentMethod).build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto response = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(paymentMethod, response.getPaymentMethod());
    }

    @Test
    public void shouldSavePresetGlobal() throws Exception {
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(IDENTITY));
        String presetGlobal = getPresetGlobalJson();
        LastStateDto rq = buildDefault().setPresetGlobal(presetGlobal).build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto response = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rq, response);
    }

    @Test
    public void shouldSaveEmptyPresetGlobal() throws Exception {
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(IDENTITY));
        String presetGlobal = "{}";
        LastStateDto rq = buildDefault().setPresetGlobal(presetGlobal).build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto response = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rq, response);
    }

    @Test
    public void shouldNotSaveNullPresetGlobal() throws Exception {
        Assertions.assertEquals(buildWithNullValues(), testClient.getLastState(IDENTITY));
        LastStateDto rq = buildDefault().setPresetGlobal(getPresetGlobalJson()).build();
        testClient.addLastState(rq, IDENTITY);
        LastStateDto rqUpdate = buildDefault().setPresetGlobal(null).build();
        testClient.updateLastState(rqUpdate, IDENTITY);
        LastStateDto response = testClient.getLastState(IDENTITY);
        Assertions.assertEquals(rq, response);
    }


    @Nonnull
    private String getPresetGlobalJson() {
        return "{\"outletId\": \"outletId\", \"addressId\": \"addressId\", \"deliveryType\": \"PICKUP\"}";
    }

    @Nonnull
    private String getParcelsInfoJson() {
        return "{\"parcels\": [{\"label\": \"label\", \"outletId\": \"outletId\", \"addressId\": " +
                "\"addressId\", \"deliveryType\": \"PICKUP\", \"intervalDate\": \"23-03-2021_24-03-2021\", " +
                "\"intervalTime\": \"14:00_15:00\", \"deliveryFeature\": \"ON_DEMAND\"}]}";
    }

    @Nonnull
    private String getParcelsInfoJsonWithDeliveryFeatureDefault() {
        return "{\"parcels\": [{\"label\": \"label\", \"outletId\": \"outletId\", \"addressId\": " +
                "\"addressId\", \"deliveryType\": \"PICKUP\", \"intervalDate\": \"23-03-2021_24-03-2021\", " +
                "\"intervalTime\": \"14:00_15:00\", \"deliveryFeature\": \"DEFAULT\"}]}";
    }

    @Nonnull
    private String getParcelsInfoJsonWithDeliveryFeatureEmpty() {
        return "{\"parcels\": [{\"label\": \"label\", \"outletId\": \"outletId\", \"addressId\": " +
                "\"addressId\", \"deliveryType\": \"PICKUP\", \"intervalDate\": \"23-03-2021_24-03-2021\", " +
                "\"intervalTime\": \"14:00_15:00\", \"deliveryFeature\": \"\"}]}";
    }

    private LastStateDto buildWithNullValues() {
        return new LastStateDto(null, null, null, null, null, null);
    }


}
