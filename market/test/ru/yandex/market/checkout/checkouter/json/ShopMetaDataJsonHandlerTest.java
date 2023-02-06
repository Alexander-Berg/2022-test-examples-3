package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.pay.legacy.PaymentSubMethod;
import ru.yandex.market.checkout.checkouter.shop.ActualDeliveryRegionalCalculationRule;
import ru.yandex.market.checkout.checkouter.shop.DeliveryReceiptNeedType;
import ru.yandex.market.checkout.checkouter.shop.MigrationMapping;
import ru.yandex.market.checkout.checkouter.shop.OrderVisibility;
import ru.yandex.market.checkout.checkouter.shop.PaymentArticle;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.PrescriptionManagementSystem;
import ru.yandex.market.checkout.checkouter.shop.ShopActualDeliveryRegionalSettings;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.shop.ActualDeliveryRegionalCalculationRule.CalculationRule.OFFER_WITH_DELIVERY_CALC;

/**
 * @author Denis Chernyshov <zoom@yandex-team.ru>
 */
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/checkouter-serialization.xml",
        "classpath:/shop-metadata-json-handler-test.xml"
})
public class ShopMetaDataJsonHandlerTest {

    @Autowired
    HttpMessageConverter converter;

    @Test
    public void shouldDeserializeWhenVersionIsNotDefined() throws IOException {
        String json = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        assertEquals(ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build(), value);
    }

    @Test
    public void shouldSerializeZeroVersion() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MONEY\"," +
                "\"articles\":null," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":false" +
                "}";
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        String bodyAsString = outputMessage.getBodyAsString();
        System.out.println(bodyAsString);
        JSONAssert.assertEquals(expectedJson, bodyAsString, true);
    }

    @Test
    public void shouldSerializePushApiActualization() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"businessId\":3," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MONEY\"," +
                "\"articles\":null," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":true," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":false" +
                "}";
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withBusinessId(3)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .withPushApiActualization(true)
                .build();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        String bodyAsString = outputMessage.getBodyAsString();
        System.out.println(bodyAsString);
        JSONAssert.assertEquals(expectedJson, bodyAsString, true);
    }

    @Test
    public void shouldDeserializePushApiActualization() throws IOException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"businessId\":3," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":\"3\"," +
                "\"articles\":null," +
                "\"orderVisibility\":{}," +
                "\"isPushApiActualization\":true," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        assertEquals(ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withBusinessId(3)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withYaMoneyId("3")
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .withOrderVisibilityMap(emptyMap())
                .withPushApiActualization(true)
                .build(), value);
    }

    @Test
    public void shouldSerializePaymentControlEnabled() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MONEY\"," +
                "\"articles\":null," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":true," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":false" +
                "}";
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .withPaymentControlEnabled(true)
                .build();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        String bodyAsString = outputMessage.getBodyAsString();
        System.out.println(bodyAsString);
        JSONAssert.assertEquals(expectedJson, bodyAsString, true);
    }

    @Test
    public void shouldDeserializePaymentControlEnabled() throws IOException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":\"3\"," +
                "\"articles\":null," +
                "\"orderVisibility\":{}," +
                "\"paymentControlEnabled\":true," +
                "\"supplierFastReturnEnabled\":false" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        assertEquals(ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withYaMoneyId("3")
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .withOrderVisibilityMap(emptyMap())
                .withPaymentControlEnabled(true)
                .build(), value);
    }

    @Test
    public void shouldSerializeFreeLiftingEnabled() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MONEY\"," +
                "\"articles\":null," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":true," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":false" +
                "}";
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .withFreeLiftingEnabled(true)
                .build();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        String bodyAsString = outputMessage.getBodyAsString();
        System.out.println(bodyAsString);
        JSONAssert.assertEquals(expectedJson, bodyAsString, true);
    }

    @Test
    public void shouldDeserializeFreeLiftingEnabled() throws IOException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":\"3\"," +
                "\"articles\":null," +
                "\"orderVisibility\":{}," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"freeLiftingEnabled\":true," +
                "\"cartRequestTurnedOff\":false" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        assertEquals(ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withYaMoneyId("3")
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .withOrderVisibilityMap(emptyMap())
                .withFreeLiftingEnabled(true)
                .build(), value);
    }

    @Test
    public void shouldDeserializeYaMoneyId() throws IOException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":\"3\"," +
                "\"articles\":null" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        assertEquals(ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withYaMoneyId("3")
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build(), value);
    }

    @Test
    public void shouldDeserializePrepayType() throws IOException {
        String json = "{" +
                "\"versionNo\":2," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":\"3\"," +
                "\"prepayType\":\"YANDEX_MARKET\"," +
                "\"articles\":null" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        assertEquals(
                ShopMetaDataBuilder.create()
                        .withCampaiginId(1)
                        .withClientId(2)
                        .withSandboxClass(PaymentClass.OFF)
                        .withProdClass(PaymentClass.SHOP)
                        .withYaMoneyId("3")
                        .withPrepayType(PrepayType.YANDEX_MARKET)
                        .build(),
                value
        );
    }

    @Test
    public void shouldSerializePrepayType() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":\"3\"," +
                "\"prepayType\":\"YANDEX_MARKET\"," +
                "\"articles\":null," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":false" +
                "}";
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withYaMoneyId("3")
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .build();
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        String actualStr = outputMessage.getBodyAsString();
        System.out.println(actualStr);
        JSONAssert.assertEquals(expectedJson, actualStr, true);
    }

    @Test
    public void shouldSerializeYaMoneyId() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":\"3\"," +
                "\"prepayType\":\"YANDEX_MONEY\"," +
                "\"articles\":null," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":false" +
                "}";
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withYaMoneyId("3")
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build();
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        String actualStr = outputMessage.getBodyAsString();
        System.out.println(actualStr);
        JSONAssert.assertEquals(expectedJson, actualStr, true);
    }

    @Test
    public void shouldDeserializeEmptyArticles() throws IOException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"articles\":[]" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        assertEquals(ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withArticles(new PaymentArticle[0])
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build(), value);
    }

    @Test
    public void shouldSerializeEmptyArticles() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MONEY\"," +
                "\"articles\":[]," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":false" +
                "}";
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withArticles(new PaymentArticle[0])
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build();
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        JSONAssert.assertEquals(expectedJson, outputMessage.getBodyAsString(), true);
    }

    @Test
    public void shouldDeserializeFilledArticles() throws IOException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"articles\":[{\"articleId\":\"5\",\"paymentSubMethod\":\"BANK_CARD\"}]" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        ShopMetaData expected = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withArticles(new PaymentArticle[]{new PaymentArticle("5", PaymentSubMethod.BANK_CARD, null)})
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build();

        assertEquals(expected, value);
    }

    @Test
    public void shouldSerializeFilledArticles() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MONEY\"," +
                "\"articles\":[{\"articleId\":\"5\",\"paymentSubMethod\":\"BANK_CARD\",\"scid\":null}]," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":false" +
                "}";
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withArticles(new PaymentArticle[]{new PaymentArticle("5", PaymentSubMethod.BANK_CARD, null)})
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build();
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        JSONAssert.assertEquals(expectedJson, outputMessage.getBodyAsString(), true);
    }

    @Test
    public void shouldDeserializeFilledActualDeliveryRegionalSettings() throws IOException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"actualDeliveryRegionalSettings\":[{\"regionId\":213,\"isPushApiActualization\":false}]," +
                "\"actualDeliveryRegionalCalculationRules\":[{\"regionId\":213," +
                "\"rule\":\"OFFER_WITH_DELIVERY_CALC\"}]" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        ShopMetaData expected = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withActualDeliveryRegionalSettings(new ShopActualDeliveryRegionalSettings[]{
                        new ShopActualDeliveryRegionalSettings(213, false)
                })
                .withActualDeliveryRegionalCalculationRule(List.of(
                        new ActualDeliveryRegionalCalculationRule(213, OFFER_WITH_DELIVERY_CALC)
                ))
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build();

        assertEquals(expected, value);
    }

    @Test
    public void shouldSerializeFilledActualDeliveryRegionalSettings() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MONEY\"," +
                "\"articles\":null," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[{\"regionId\":213,\"isPushApiActualization\":false}]," +
                "\"actualDeliveryRegionalCalculationRules\":[{\"regionId\":213," +
                "\"rule\":\"OFFER_WITH_DELIVERY_CALC\"}]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":false" +
                "}";
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withActualDeliveryRegionalSettings(new ShopActualDeliveryRegionalSettings[]{
                        new ShopActualDeliveryRegionalSettings(213, false)
                })
                .withActualDeliveryRegionalCalculationRule(List.of(
                        new ActualDeliveryRegionalCalculationRule(213, OFFER_WITH_DELIVERY_CALC)
                ))
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build();
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        JSONAssert.assertEquals(expectedJson, outputMessage.getBodyAsString(), true);
    }

    @Test
    public void shouldDeserialize2FilledArticles() throws IOException, JSONException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"articles\":[{\"articleId\":\"5\",\"paymentSubMethod\":\"BANK_CARD\",\"scid\":null}, " +
                "{\"articleId\":\"6\",\"paymentSubMethod\":\"YA_MONEY\",\"scid\":null}]" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        ShopMetaData expected = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withArticles(new PaymentArticle[]{
                        new PaymentArticle("5", PaymentSubMethod.BANK_CARD, null),
                        new PaymentArticle("6", PaymentSubMethod.YA_MONEY, null)
                })
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build();
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(expected, MediaType.parseMediaType("json/application"), outputMessage);
        assertEquals(expected, value);
    }

    @Test
    public void shouldSerialize2FilledArticles() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MONEY\"," +
                "\"articles\":[{\"articleId\":\"5\",\"paymentSubMethod\":\"BANK_CARD\",\"scid\":null}, " +
                "{\"articleId\":\"6\",\"paymentSubMethod\":\"YA_MONEY\",\"scid\":null}]," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":false" +
                "}";
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withArticles(new PaymentArticle[]{
                        new PaymentArticle("5", PaymentSubMethod.BANK_CARD, null),
                        new PaymentArticle("6", PaymentSubMethod.YA_MONEY, null)
                })
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build();
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        JSONAssert.assertEquals(expectedJson, outputMessage.getBodyAsString(), true);
    }

    @Test
    public void shouldDeserializeUnknownPaymentSubMethodToUnknownEnum() throws IOException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"articles\":[{\"articleId\":\"5\",\"paymentSubMethod\":\"BANK___CARD\",\"scid\":null}, " +
                "{\"articleId\":\"6\",\"paymentSubMethod\":\"YA_MONEY\",\"scid\":null}]" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        ShopMetaData expected = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withArticles(new PaymentArticle[]{
                        new PaymentArticle("5", PaymentSubMethod.UNKNOWN, null),
                        new PaymentArticle("6", PaymentSubMethod.YA_MONEY, null)
                })
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build();
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(expected, MediaType.parseMediaType("json/application"), outputMessage);
        assertEquals(expected, value);
    }

    @Test
    public void shouldSerializeUnknownPaymentSubMethodToUnknownEnum() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MONEY\"," +
                "\"articles\":[{\"articleId\":\"5\",\"paymentSubMethod\":\"UNKNOWN\",\"scid\":\"123\"}, " +
                "{\"articleId\":\"6\",\"paymentSubMethod\":\"YA_MONEY\",\"scid\":null}]," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":false" +
                "}";
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withArticles(new PaymentArticle[]{
                        new PaymentArticle("5", PaymentSubMethod.UNKNOWN, "123"),
                        new PaymentArticle("6", PaymentSubMethod.YA_MONEY, null)
                })
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build();
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        JSONAssert.assertEquals(expectedJson, outputMessage.getBodyAsString(), true);
    }

    @Test
    public void okIfNewFieldsAreAbsent() throws IOException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        converter.read(ShopMetaData.class, inputMessage);
    }

    @Test
    public void shouldDeserializeOrderVisibility() throws IOException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":\"3\"," +
                "\"articles\":null," +
                "\"orderVisibility\":{\"BUYER_EMAIL\":false, \"DELIVERY_ADDRESS\":false}" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        assertEquals(ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withYaMoneyId("3")
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .withOrderVisibilityMap(ImmutableMap.<OrderVisibility, Boolean>builder()
                        .put(OrderVisibility.BUYER_EMAIL, false)
                        .put(OrderVisibility.DELIVERY_ADDRESS, false)
                        .build())
                .build(), value);
    }

    @Test
    public void shouldDeserializeEmptyOrderVisibility() throws IOException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":\"3\"," +
                "\"articles\":null," +
                "\"orderVisibility\":{}" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        assertEquals(ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withYaMoneyId("3")
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .withOrderVisibilityMap(emptyMap())
                .build(), value);
    }

    @Test
    public void shouldSerializeOrderVisibility() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MARKET\"," +
                "\"articles\":null," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"orderVisibility\":{\"BUYER_EMAIL\":true, \"DELIVERY_ADDRESS\":true}," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":false" +
                "}";
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withOrderVisibilityMap(ImmutableMap.<OrderVisibility, Boolean>builder()
                        .put(OrderVisibility.BUYER_EMAIL, true)
                        .put(OrderVisibility.DELIVERY_ADDRESS, true)
                        .build())
                .build();
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        JSONAssert.assertEquals(expectedJson, outputMessage.getBodyAsString(), true);
    }

    @Test
    public void shouldSerializeEmptyOrderVisibility() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MARKET\"," +
                "\"articles\":null," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"orderVisibility\":{}," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":false" +
                "}";
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withOrderVisibilityMap(emptyMap())
                .build();
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        JSONAssert.assertEquals(expectedJson, outputMessage.getBodyAsString(), true);
    }

    @Test
    public void shouldSerializeChangedDeliveryReceiptNeedAndPrescriptionEnabled() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MONEY\"," +
                "\"articles\":[{\"articleId\":\"5\",\"paymentSubMethod\":\"UNKNOWN\",\"scid\":\"123\"}, " +
                "{\"articleId\":\"6\",\"paymentSubMethod\":\"YA_MONEY\",\"scid\":null}]," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":DONT_CREATE_DELIVERY_RECEIPT_EXCLUDE_DELIVERY," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":MEDICATA," +
                "\"selfEmployed\":false" +
                "}";
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withArticles(new PaymentArticle[]{
                        new PaymentArticle("5", PaymentSubMethod.UNKNOWN, "123"),
                        new PaymentArticle("6", PaymentSubMethod.YA_MONEY, null)
                })
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .withDeliveryReceiptNeedType(DeliveryReceiptNeedType.DONT_CREATE_DELIVERY_RECEIPT_EXCLUDE_DELIVERY)
                .withPrescriptionManagementSystem(PrescriptionManagementSystem.MEDICATA)
                .build();
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        JSONAssert.assertEquals(expectedJson, outputMessage.getBodyAsString(), true);
    }

    @Test
    public void shouldDeserializeMigrationMapping() throws IOException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":\"3\"," +
                "\"articles\":null," +
                "\"orderVisibility\":{}," +
                "\"migrationMapping\":{\"donorPartnerId\" : 1 , \"donorWarehouseId\" : 2}" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        assertEquals(ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withYaMoneyId("3")
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .withOrderVisibilityMap(emptyMap())
                .withMigrationMapping(new MigrationMapping(1L, 2L))
                .build(), value);
    }

    @Test
    public void shouldSerializeMigrationMapping() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MARKET\"," +
                "\"articles\":null," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"orderVisibility\":{}," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"migrationMapping\":{\"donorPartnerId\" : 1 , \"donorWarehouseId\" : 2}," +
                "\"selfEmployed\":false" +
                "}";
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withOrderVisibilityMap(emptyMap())
                .withMigrationMapping(new MigrationMapping(1L, 2L))
                .build();
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        JSONAssert.assertEquals(expectedJson, outputMessage.getBodyAsString(), true);
    }

    @Test
    public void shouldDeserializeSelfEmployed() throws IOException {
        String json = "{" +
                "\"versionNo\":1," +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":\"3\"," +
                "\"articles\":null," +
                "\"orderVisibility\":{}," +
                "\"selfEmployed\":true" +
                "}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        ShopMetaData value = (ShopMetaData) converter.read(ShopMetaData.class, inputMessage);
        assertEquals(ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withYaMoneyId("3")
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .withOrderVisibilityMap(emptyMap())
                .withSelfEmployed(Boolean.TRUE)
                .build(), value);
    }

    @Test
    public void shouldSerializeSelfEmployed() throws IOException, JSONException {
        String expectedJson = "{" +
                "\"campaignId\":1," +
                "\"clientId\":2," +
                "\"sandboxClass\":\"OFF\", " +
                "\"prodClass\":\"SHOP\"," +
                "\"yaMoneyId\":null," +
                "\"prepayType\":\"YANDEX_MONEY\"," +
                "\"articles\":null," +
                "\"inn\":null," +
                "\"phoneNumber\":null," +
                "\"agencyCommission\":null," +
                "\"isOrderAutoAcceptEnabled\":false," +
                "\"isPushApiActualization\":false," +
                "\"isPushApiActualizationRegional\":false," +
                "\"supplierFastReturnEnabled\":false," +
                "\"actualDeliveryRegionalSettings\":[]," +
                "\"actualDeliveryRegionalCalculationRules\":[]," +
                "\"freeLiftingEnabled\":false," +
                "\"cartRequestTurnedOff\":false," +
                "\"deliveryReceiptNeedType\":CREATE_DELIVERY_RECEIPT," +
                "\"paymentControlEnabled\":false," +
                "\"prescriptionManagementSystem\":NONE," +
                "\"selfEmployed\":true" +
                "}";
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        ShopMetaData data = ShopMetaDataBuilder.create()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .withSelfEmployed(Boolean.TRUE)
                .build();
        converter.write(data, MediaType.parseMediaType("json/application"), outputMessage);
        String bodyAsString = outputMessage.getBodyAsString();
        System.out.println(bodyAsString);
        JSONAssert.assertEquals(expectedJson, bodyAsString, true);
    }
}
