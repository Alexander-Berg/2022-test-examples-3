package ru.yandex.market.notification.sms.model;

import java.util.Random;

import org.junit.Test;

import ru.yandex.market.notification.common.model.TextNotificationContent;
import ru.yandex.market.notification.common.model.address.UserUidAddress;
import ru.yandex.market.notification.sms.model.address.PhoneNumberAddress;
import ru.yandex.market.notification.test.util.DataSerializerUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для сериализации/десериализации моделей для работы с SMS.
 *
 * @author Vladislav Bauer
 */
public class SmsDataSerialTest {

    @Test
    public void testPhoneNumberAddress() {
        final String number = "12345";
        final PhoneNumberAddress address = PhoneNumberAddress.create(number);
        final byte[] data = DataSerializerUtils.serialize(address);

        final PhoneNumberAddress restoredObject = DataSerializerUtils.deserialize(data, PhoneNumberAddress.class);
        assertThat(restoredObject.getNumber(), equalTo(number));
    }

    @Test
    public void testUserUidAddress() {
        final Long uid = new Random().nextLong();
        final UserUidAddress address = UserUidAddress.create(uid);
        final byte[] data = DataSerializerUtils.serialize(address);

        final UserUidAddress restoredObject = DataSerializerUtils.deserialize(data, UserUidAddress.class);
        assertThat(restoredObject.getUid(), equalTo(uid));
    }

    @Test
    public void testTextNotificationContent() {
        final String content = "content text";
        final TextNotificationContent textNotificationContent = TextNotificationContent.create(content);
        final byte[] data = DataSerializerUtils.serialize(textNotificationContent);

        final TextNotificationContent restoredObject =
            DataSerializerUtils.deserialize(data, TextNotificationContent.class);

        assertThat(restoredObject.getText(), equalTo(content));
    }

}
