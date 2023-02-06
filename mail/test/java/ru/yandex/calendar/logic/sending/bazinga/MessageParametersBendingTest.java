package ru.yandex.calendar.logic.sending.bazinga;

import lombok.AllArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.sending.param.MessageParameters;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.util.rr.CalendarRandomValueGenerator;
import ru.yandex.misc.reflection.ClassX;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
@AllArgsConstructor
@RunWith(Parameterized.class)
public class MessageParametersBendingTest {

    @Parameterized.Parameters(name = "{0}")
    public static ListF<Object[]> data() {
        return MailType.R.valuesList().map(MailType::getDataClass).stableUnique()
                .filterNot(ClassX::isAbstract).map(c -> new Object[] { c.getSimpleName(), c });
    }

    private final String name;
    private final ClassX<MessageParameters> clazz;

    @Test
    public void reparse() {
        MessageParameters params = CalendarRandomValueGenerator.R.randomValue(clazz);

        String serialized = new String(MessageParameters.serializer.serializeJson(params));
        MessageParameters parsed = MessageParameters.parser.parseJson(serialized);

        Assert.equals(serialized, new String(MessageParameters.serializer.serializeJson(parsed)));
    }
}
