package ru.yandex.reminders.util;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.bender.Bender;
import ru.yandex.misc.bender.annotation.BenderBindAllFields;
import ru.yandex.misc.bender.config.BenderConfiguration;
import ru.yandex.misc.bender.config.CustomMarshallerUnmarshallerFactoryUtils;
import ru.yandex.misc.bender.parse.BenderJsonParser;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class ValueOrObjectBindingTest {

    @BenderBindAllFields
    public static class TextAndCode {
        @BindValueNodeHere
        private String value;
        private Option<Integer> code;
    }

    @BenderBindAllFields
    public static class Root {
        private Option<TextAndCode> holder;
    }

    @Test
    public void test() {
        BenderConfiguration configuration = BenderConfiguration.defaultConfiguration();
        configuration = new BenderConfiguration(
                configuration.getSettings(),
                CustomMarshallerUnmarshallerFactoryUtils.combine(
                        new ValueOrObjectMarshallerUnmarshallerFactory(),
                        configuration.getMarshallerUnmarshallerFactory()));

        BenderJsonParser<Root> parser = Bender.jsonParser(Root.class, configuration);
        Root parsed;

        parsed = parser.parseJson("{}");
        Assert.none(parsed.holder);

        parsed = parser.parseJson("{\"holder\":null}");
        Assert.none(parsed.holder);

        parsed = parser.parseJson("{\"holder\":\"Text\"}");
        Assert.some(parsed.holder);
        Assert.equals("Text", parsed.holder.get().value);
        Assert.none(parsed.holder.get().code);

        parsed = parser.parseJson("{\"holder\":{\"value\":\"Text\",\"code\":3}}");
        Assert.some(parsed.holder);
        Assert.equals("Text", parsed.holder.get().value);
        Assert.some(3, parsed.holder.get().code);
    }
}
