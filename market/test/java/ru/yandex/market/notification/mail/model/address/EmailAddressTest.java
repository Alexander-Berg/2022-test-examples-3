package ru.yandex.market.notification.mail.model.address;

import java.util.UUID;

import org.junit.Test;

import ru.yandex.market.notification.mail.model.address.EmailAddress.Type;
import ru.yandex.market.notification.test.model.AbstractModelTest;
import ru.yandex.market.notification.test.util.DataSerializerUtils;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для  {@link EmailAddress}.
 *
 * @author Vladislav Bauer
 */
public class EmailAddressTest extends AbstractModelTest {

    private static final String EMAIL = "vbauer@yandex-team.ru";
    private static final Type TYPE = Type.TO;


    @Test
    public void testConstruction() {
        final String email = "vbauer@yandex-team.ru";
        final Type type = Type.TO;
        final EmailAddress address = EmailAddress.create(email, type);

        assertThat(address.getEmail(), equalTo(email));
        assertThat(address.getType(), equalTo(type));
    }

    @Test
    public void testBasicMethods() {
        final String email = generateEmail();
        final EmailAddress address = EmailAddress.create(email, Type.TO);
        final EmailAddress sameAddress = EmailAddress.create(email, Type.TO);
        final EmailAddress otherAddress1 = EmailAddress.create(email, Type.FROM);
        final EmailAddress otherAddress2 = EmailAddress.create(generateEmail(), Type.TO);

        checkBasicMethods(address, sameAddress, otherAddress1);
        checkBasicMethods(address, sameAddress, otherAddress2);
    }

    @Test
    public void testTypes() {
        checkEnum(Type.class, 5);

        assertThatCode(Type.FROM, 1);
        assertThatCode(Type.REPLY_TO, 2);
        assertThatCode(Type.TO, 3);
        assertThatCode(Type.CC, 4);
        assertThatCode(Type.BCC, 5);
    }

    @Test
    public void testSerialization() {
        final EmailAddress data = EmailAddress.create(EMAIL, TYPE);
        final String serialized = DataSerializerUtils.serializeToString(data);

        assertThat(serialized, containsString(EMAIL));
    }

    @Test
    public void testDeserialization() throws Exception {
        final EmailAddress data = DataSerializerUtils.deserializeFromResource(EmailAddress.class);

        assertThat(data.getType(), equalTo(TYPE));
        assertThat(data.getEmail(), equalTo(EMAIL));
    }


    private String generateEmail() {
        final String name = UUID.randomUUID().toString();
        return name + "@yandex-team.ru";
    }

}
