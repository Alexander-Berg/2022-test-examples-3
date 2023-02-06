package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;

public class ChangeRequestPatchRequestJsonDeserializerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserializeCancellationRequestPayloadTest() throws Exception {
        var json = "{\"status\":\"NEW\",\"message\":\"message\",\"payload\":{\"type\":\"CANCELLATION\"," +
                "\"@class\":\"ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestPayload\"," +
                "\"substatus\":\"USER_CHANGED_MIND\"," +
                "\"notes\":\"notes\",\"missingItems\":[{\"id\":1,\"count\":2},{\"id\":2,\"count\":3}]," +
                "\"confirmationReason\":\"DELIVERED\"}}";
        var jsonWithoutClassField = "{\"status\":\"NEW\",\"message\":\"message\"," +
                "\"payload\":{\"substatus\":\"USER_CHANGED_MIND\",\"notes\":\"notes\",\"type\":\"CANCELLATION\"," +
                "\"missingItems\":[{\"id\":1," +
                "\"count\":2},{\"id\":2,\"count\":3}],\"confirmationReason\":\"DELIVERED\"}}";

        var deserialized = read(ChangeRequestPatchRequest.class, json);
        var deserializedWithoutClassField = read(ChangeRequestPatchRequest.class, jsonWithoutClassField);
        // Проверяем что обьект десериализованный из json'а без поля @class
        // эквивалентен обьекту десериализованному из json'а с полем @class
        Assertions.assertEquals(deserializedWithoutClassField, deserialized);
        // Проверям что обьект сериализуется в json с полем @class
        checkJson(write(deserializedWithoutClassField),
                "$." + Names.ChangeRequest.PAYLOAD + ".@class",
                deserializedWithoutClassField.getPayload().getClass().getName());
    }

    @Test
    public void deserializeDeliveryDatesRequestPayloadTest() throws Exception {
        var json = "{\"status\":\"APPLIED\",\"message\":\"message\",\"payload\":{\"type\":\"DELIVERY_DATES\"," +
                "\"@class\":\"ru.yandex.market.checkout.checkouter.order.changerequest" +
                ".DeliveryDatesChangeRequestPayload\"," +
                "\"shipmentDate\":\"14-06-2021\",\"reason\":\"USER_MOVED_DELIVERY_DATES\"," +
                "\"fromDate\":\"14-06-2021\",\"toDate\":\"15-06-2021\",\"interval\":{\"fromTime\":\"17:52:30" +
                ".234253\",\"toTime\":\"19:52:30.23428\"},\"packagingTime\":\"2021-06-14T14:52:30.233885Z\"}}";
        var jsonWithoutClassField = "{\"status\":\"APPLIED\",\"message\":\"message\"," +
                "\"payload\":{\"type\":\"DELIVERY_DATES\",\"shipmentDate\":\"14-06-2021\"," +
                "\"reason\":\"USER_MOVED_DELIVERY_DATES\"," +
                "\"fromDate\":\"14-06-2021\",\"toDate\":\"15-06-2021\",\"interval\":{\"fromTime\":\"17:52:30" +
                ".234253\",\"toTime\":\"19:52:30.23428\"},\"packagingTime\":\"2021-06-14T14:52:30.233885Z\"}}";

        var deserialized = read(ChangeRequestPatchRequest.class, json);
        var deserializedWithoutClassField = read(ChangeRequestPatchRequest.class, jsonWithoutClassField);

        // Проверяем что обьект десериализованный из json'а без поля @class
        // эквивалентен обьекту десериализованному из json'а с полем @class
        Assertions.assertEquals(deserializedWithoutClassField, deserialized);
        // Проверям что обьект сериализуется в json с полем @class
        checkJson(write(deserializedWithoutClassField),
                "$." + Names.ChangeRequest.PAYLOAD + ".@class",
                deserializedWithoutClassField.getPayload().getClass().getName());
    }

    @Test
    public void deserializeDeliveryOptionsRequestPayloadTest() throws Exception {
        var json = "{\"status\":\"PROCESSING\",\"message\":\"message\",\"payload\":{\"type\":\"DELIVERY_OPTION\"," +
                "\"@class\":\"ru.yandex.market.checkout.checkouter.order.changerequest" +
                ".DeliveryOptionChangeRequestPayload\",\"shipmentDate\":\"14-06-2021\"," +
                "\"reason\":\"USER_MOVED_DELIVERY_DATES\",\"fromDate\":\"14-06-2021\",\"toDate\":\"15-06-2021\"," +
                "\"interval\":{\"fromTime\":\"18:13:06.632397\",\"toTime\":\"20:13:06.632414\"}," +
                "\"packagingTime\":\"2021-06-14T15:13:06.632039Z\",\"deliveryServiceId\":123}}";
        var deserialized = read(ChangeRequestPatchRequest.class, json);

        var jsonWithoutClassField = "{\"status\":\"PROCESSING\",\"message\":\"message\"," +
                "\"payload\":{\"type\":\"DELIVERY_OPTION\",\"shipmentDate\":\"14-06-2021\"," +
                "\"reason\":\"USER_MOVED_DELIVERY_DATES\",\"fromDate\":\"14-06-2021\",\"toDate\":\"15-06-2021\"," +
                "\"interval\":{\"fromTime\":\"18:13:06.632397\",\"toTime\":\"20:13:06.632414\"}," +
                "\"packagingTime\":\"2021-06-14T15:13:06.632039Z\",\"deliveryServiceId\":123}}";
        var deserializedWithoutClassField = read(ChangeRequestPatchRequest.class, jsonWithoutClassField);

        // Проверяем что обьект десериализованный из json'а без поля @class
        // эквивалентен обьекту десериализованному из json'а с полем @class
        Assertions.assertEquals(deserializedWithoutClassField, deserialized);
        // Проверям что обьект сериализуется в json с полем @class
        checkJson(write(deserializedWithoutClassField),
                "$." + Names.ChangeRequest.PAYLOAD + ".@class",
                deserializedWithoutClassField.getPayload().getClass().getName());
    }

    @Test
    public void deserializeItemsRemovalRequestPayloadTest() throws Exception {
        var json = "{\"status\":\"PROCESSING\",\"payload\":{\"type\":\"ITEMS_REMOVAL\",\"@class\":\"ru.yandex.market" +
                ".checkout.checkouter.order.changerequest.itemsremoval.ItemsRemovalChangeRequestPayload\"," +
                "\"updatedItems\":[{\"feedId\":1,\"offerId\":\"offerId\",\"feedCategoryId\":\"feedCategoryId\"," +
                "\"offerName\":\"offerName\",\"count\":3,\"hasSnapshot\":false,\"loyaltyProgramPartner\":false," +
                "\"preorder\":false}],\"updatedParcels\":[],\"reason\":\"ITEMS_NOT_FOUND\"}}";
        var deserialized = read(ChangeRequestPatchRequest.class, json);

        var jsonWithoutClassField = "{\"status\":\"PROCESSING\",\"payload\":{\"type\":\"ITEMS_REMOVAL\"," +
                "\"updatedItems\":[{\"feedId\":1,\"offerId\":\"offerId\",\"feedCategoryId\":\"feedCategoryId\"," +
                "\"offerName\":\"offerName\",\"count\":3,\"hasSnapshot\":false,\"loyaltyProgramPartner\":false," +
                "\"preorder\":false}],\"updatedParcels\":[],\"reason\":\"ITEMS_NOT_FOUND\"}}";
        var deserializedWithoutClassField = read(ChangeRequestPatchRequest.class, jsonWithoutClassField);

        // Проверяем что обьект десериализованный из json'а без поля @class
        // эквивалентен обьекту десериализованному из json'а с полем @class
        Assertions.assertEquals(deserializedWithoutClassField, deserialized);
        // Проверям что обьект сериализуется в json с полем @class
        checkJson(write(deserializedWithoutClassField),
                "$." + Names.ChangeRequest.PAYLOAD + ".@class",
                deserializedWithoutClassField.getPayload().getClass().getName());
    }

    @Test
    public void deserializeParcelCancelRequestPayloadTest() throws Exception {
        var json = "{\"status\":\"REJECTED\",\"message\":\"message\",\"payload\":{\"type\":\"PARCEL_CANCELLATION\"," +
                "\"@class\":\"ru.yandex.market.checkout.checkouter.order.changerequest.parcel" +
                ".ParcelCancelChangeRequestPayload\",\"parcelId\":123,\"substatus\":\"USER_CHANGED_MIND\"," +
                "\"notes\":\"notes\"}}";
        var deserialized = read(ChangeRequestPatchRequest.class, json);

        var jsonWithoutClassField = "{\"status\":\"REJECTED\",\"message\":\"message\"," +
                "\"payload\":{\"type\":\"PARCEL_CANCELLATION\",\"parcelId\":123,\"substatus\":\"USER_CHANGED_MIND\"," +
                "\"notes\":\"notes\"}}";
        var deserializedWithoutClassField = read(ChangeRequestPatchRequest.class, jsonWithoutClassField);

        // Проверяем что обьект десериализованный из json'а без поля @class
        // эквивалентен обьекту десериализованному из json'а с полем @class
        Assertions.assertEquals(deserializedWithoutClassField, deserialized);
        // Проверям что обьект сериализуется в json с полем @class
        checkJson(write(deserializedWithoutClassField),
                "$." + Names.ChangeRequest.PAYLOAD + ".@class",
                deserializedWithoutClassField.getPayload().getClass().getName());
    }

    @Test
    public void deserializePaymentMethodChangeRequestPayloadTest() throws Exception {
        var json = "{\"status\":\"INVALID\",\"message\":\"message\",\"payload\":{\"type\":\"PAYMENT_METHOD\"," +
                "\"@class\":\"ru.yandex.market.checkout.checkouter.order.changerequest.PaymentChangeRequestPayload\"," +
                "\"paymentMethod\":\"SBP\"}}";
        var deserialized = read(ChangeRequestPatchRequest.class, json);

        var jsonWithoutClassField = "{\"status\":\"INVALID\",\"message\":\"message\"," +
                "\"payload\":{\"type\":\"PAYMENT_METHOD\",\"paymentMethod\":\"SBP\"}}";
        var deserializedWithoutClassField = read(ChangeRequestPatchRequest.class, jsonWithoutClassField);

        // Проверяем что обьект десериализованный из json'а без поля @class
        // эквивалентен обьекту десериализованному из json'а с полем @class
        Assertions.assertEquals(deserializedWithoutClassField, deserialized);
        // Проверям что обьект сериализуется в json с полем @class
        checkJson(write(deserializedWithoutClassField),
                "$." + Names.ChangeRequest.PAYLOAD + ".@class",
                deserializedWithoutClassField.getPayload().getClass().getName());
    }

    @Test
    public void deserializeRecipientChangeRequestPayloadTest() throws Exception {
        var json = "{\"status\":\"NEW\",\"payload\":{\"type\":\"RECIPIENT\", \"@class\":\"ru.yandex.market.checkout" +
                ".checkouter.order.changerequest.RecipientChangeRequestPayload\"," +
                "\"recipientName\":{\"firstName\":\"firstName\",\"middleName\":\"middleName\"," +
                "\"lastName\":\"ластНэйм\"},\"phone\":\"+79123456789\"}}";
        var deserialized = read(ChangeRequestPatchRequest.class, json);

        var jsonWithoutClassField = "{\"status\":\"NEW\",\"payload\":{\"type\":\"RECIPIENT\"," +
                "\"recipientName\":{\"firstName\":\"firstName\",\"middleName\":\"middleName\"," +
                "\"lastName\":\"ластНэйм\"},\"phone\":\"+79123456789\"}}";
        var deserializedWithoutClassField = read(ChangeRequestPatchRequest.class, jsonWithoutClassField);

        // Проверяем что обьект десериализованный из json'а без поля @class
        // эквивалентен обьекту десериализованному из json'а с полем @class
        Assertions.assertEquals(deserializedWithoutClassField, deserialized);
        // Проверям что обьект сериализуется в json с полем @class
        checkJson(write(deserializedWithoutClassField),
                "$." + Names.ChangeRequest.PAYLOAD + ".@class",
                deserializedWithoutClassField.getPayload().getClass().getName());
    }

    @Test
    public void deserializeStorageLimitDateChangeRequestPayloadTest() throws Exception {
        var json = "{\"status\":\"NEW\",\"payload\":{\"type\":\"STORAGE_LIMIT_DATE\", \"@class\":\"ru.yandex.market" +
                ".checkout" +
                ".checkouter.order.changerequest.storage.StorageLimitDateChangeRequestPayload\"," +
                "\"newDate\":\"2021-08-09\",\"oldDate\":\"2021-08-01\"}}";
        var deserialized = read(ChangeRequestPatchRequest.class, json);

        var jsonWithoutClassField = "{\"status\":\"NEW\",\"payload\":{\"type\":\"STORAGE_LIMIT_DATE\"," +
                "\"newDate\":\"2021-08-09\",\"oldDate\":\"2021-08-01\"}}";
        var deserializedWithoutClassField = read(ChangeRequestPatchRequest.class, jsonWithoutClassField);

        // Проверяем что обьект десериализованный из json'а без поля @class
        // эквивалентен обьекту десериализованному из json'а с полем @class
        Assertions.assertEquals(deserializedWithoutClassField, deserialized);
        // Проверям что обьект сериализуется в json с полем @class
        checkJson(write(deserializedWithoutClassField),
                "$." + Names.ChangeRequest.PAYLOAD + ".@class",
                deserializedWithoutClassField.getPayload().getClass().getName());
    }

    @Test
    public void deserializeUnknownChangeRequestPayloadTest() throws Exception {
        var jsonWithoutClassField = "{\"status\":\"NEW\"," +
                "\"message\":\"message\"," +
                "\"payload\":null}";
        var json = "{\"status\":\"NEW\"," +
                "\"message\":\"message\"," +
                "\"payload\":{\"@class\":\"ru.yandex.market.checkout.checkouter.order" +
                ".changerequest.UnknownChangeRequestPayload\",\"type\":\"UNKNOWN\"}}";


        var deserialized = read(ChangeRequestPatchRequest.class, json);

        var deserializedWithoutClassField = read(ChangeRequestPatchRequest.class, jsonWithoutClassField);

        // Проверяем что обьект десериализованный из json'а без поля @class
        // эквивалентен обьекту десериализованному из json'а с полем @class
        Assertions.assertEquals(deserializedWithoutClassField, deserialized);
        // Проверям что обьект сериализуется в json с полем @class
        checkJson(write(deserializedWithoutClassField),
                "$." + Names.ChangeRequest.PAYLOAD + ".@class",
                deserializedWithoutClassField.getPayload().getClass().getName());
    }
}
