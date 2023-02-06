package ru.yandex.chemodan.app.lentaloader.test;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.joda.time.base.AbstractInstant;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function0;
import ru.yandex.bolts.function.Function0V;
import ru.yandex.chemodan.app.dataapi.api.data.field.DataField;
import ru.yandex.chemodan.app.lentaloader.cool.generator.ThemeDefinition;
import ru.yandex.chemodan.app.lentaloader.cool.generator.WordMatch;
import ru.yandex.chemodan.app.lentaloader.cool.utils.TermDefinition;
import ru.yandex.chemodan.app.lentaloader.cool.utils.TermLanguageDefinition;
import ru.yandex.chemodan.app.lentaloader.lenta.LentaBlockRecord;
import ru.yandex.chemodan.app.lentaloader.lenta.LentaRecordType;
import ru.yandex.chemodan.mpfs.MpfsFileInfo;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.bender.Bender;
import ru.yandex.misc.bender.config.BenderConfiguration;
import ru.yandex.misc.bender.config.CustomMarshallerUnmarshallerFactoryBuilder;
import ru.yandex.misc.bender.parse.simpleType.SimpleTypeUnmarshallerSupport;
import ru.yandex.misc.bender.serialize.BenderJsonWriter;
import ru.yandex.misc.bender.serialize.simpleType.SimpleTypeMarshallerSupport;
import ru.yandex.misc.db.embedded.sandbox.SandboxResourcesRule;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class TestUtils {

    public static void withFixedNow(Instant now, Function0V function) {
        withFixedNow(now, function.asFunction0ReturnNull());
    }

    public static <T> T withFixedNow(Instant now, Function0<T> function) {
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());
        try {
            return function.apply();

        } finally {
            DateTimeUtils.setCurrentMillisSystem();
        }
    }

    public static LentaBlockRecord consBlock(LentaRecordType type, MapF<String, DataField> specific) {
        return consBlock(type, MoscowTime.instant(2016, 9, 26, 20, 45), specific);
    }

    public static LentaBlockRecord consBlock(LentaRecordType type, Instant mtime, MapF<String, DataField> specific) {
        return new LentaBlockRecord(
                "id", 1, type, "group",
                Option.of(MoscowTime.instant(2016, 9, 26, 20, 30)), mtime,
                Option.of(MoscowTime.instant(2016, 9, 27, 20, 30)), specific);
    }

    public static <T> ListF<? extends T> parseTestData(SandboxResourcesRule rule, Class<? extends T> testDataClass,
            BenderConfiguration configuration, String filename)
    {
        return Bender.jsonParser(testDataClass, configuration)
                .parseListJson(rule.getExtractedResourceDir().child(filename));
    }

    public static BenderConfiguration createBenderConfiguration() {
        CustomMarshallerUnmarshallerFactoryBuilder marshallerUnmarshallerFactoryBuilder =
                MpfsFileInfo.mpfsFileMetaMarshallerUnmarshallerFactoryBuilder
                        .add(DateTime.class, new JodaDateAsMillisJsonBenderMarshaller(DateTime.class),
                                new JodaDateAsMillisJsonBenderUnmarshaller(DateTime.class));
        return new BenderConfiguration(
                BenderConfiguration.defaultSettings(),
                marshallerUnmarshallerFactoryBuilder.build());
    }

    public static ThemeDefinition createNatureThemeDefinition() {
        ListF<WordMatch> words = Cf.list(new WordMatch("природа", 3804), new WordMatch("nature", 4244));
        TermLanguageDefinition russianLanguageDefinition = new TermLanguageDefinition("природа", "природы", "о", "природе",
                "природой", "природу", "на природу", Option.empty());
        TermLanguageDefinition turkishLanguageDefinition = new TermLanguageDefinition("tabiat", "doğa", "doğa", "hakkında",
                "", "", "", Option.empty());
        TermLanguageDefinition englishLanguageDefinition = new TermLanguageDefinition("nature", "of nature", "of", "nature",
                "", "", "", Option.empty());
        TermLanguageDefinition ukrainianLanguageDefinition = new TermLanguageDefinition("природа", "природи", "про",
                "природу", "", "", "", Option.empty());
        return new ThemeDefinition("nature", words,
                new TermDefinition(Cf.map(Language.RUSSIAN, russianLanguageDefinition,
                        Language.TURKISH, turkishLanguageDefinition,
                        Language.ENGLISH, englishLanguageDefinition,
                        Language.UKRAINIAN, ukrainianLanguageDefinition)), true, Option.of(Boolean.FALSE),
                Option.of(Boolean.FALSE), Option.of(Boolean.FALSE), Option.empty());
    }

    private static class JodaDateAsMillisJsonBenderMarshaller extends SimpleTypeMarshallerSupport {

        private final Class<? extends AbstractInstant> timeClass;

        public JodaDateAsMillisJsonBenderMarshaller(Class<? extends AbstractInstant> timeClass) {
            this.timeClass = timeClass;
        }

        @Override
        protected String toStringValueForXml(Object o) {
            throw new IllegalStateException("XML is not supported");
        }

        @Override
        protected void writeJson(BenderJsonWriter json, Object o) {
            json.writeNumber(timeClass.cast(o).getMillis());
        }
    }

    private static class JodaDateAsMillisJsonBenderUnmarshaller extends SimpleTypeUnmarshallerSupport {

        private final Class<? extends AbstractInstant> timeClass;

        public JodaDateAsMillisJsonBenderUnmarshaller(Class<? extends AbstractInstant> timeClass) {
            this.timeClass = timeClass;
        }

        @Override
        protected Object convert(String o) {
            try {
                return timeClass.getConstructor(long.class).newInstance(Long.parseLong(o));
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

}
