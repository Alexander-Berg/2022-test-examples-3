package ru.yandex.market.notification.simple.model.type;

import java.util.Random;

import org.junit.Test;

import ru.yandex.market.notification.test.model.AbstractModelTest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.notification.simple.model.type.CodeNotificationType.extractCode;

/**
 * Unit-тесты для {@link CodeNotificationType}.
 *
 * @author Vladislav Bauer
 */
public class CodeNotificationTypeTest extends AbstractModelTest {

    private static final Random RANDOM = new Random();


    @Test
    public void testConstruction() {
        final  long code = generateCode();
        final CodeNotificationType type = new CodeNotificationType(code);

        assertThat(type.getCode(), equalTo(code));
        assertThat(type.toString(), not(isEmptyOrNullString()));
    }

    @Test
    public void testBasicMethods() {
        final  long code = generateCode();
        final CodeNotificationType type = new CodeNotificationType(code);
        final CodeNotificationType sameType = new CodeNotificationType(code);
        final CodeNotificationType otherType = new CodeNotificationType(code + 1);

        checkBasicMethods(type, sameType, otherType);
    }

    @Test
    public void testExtractCode() {
        final long code = generateCode();

        assertThat(extractCode(new CodeNotificationType(code)), equalTo(code));
        assertThat(extractCode(null), nullValue());
    }


    private long generateCode() {
        return RANDOM.nextLong();
    }

}
