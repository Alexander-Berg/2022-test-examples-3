package ru.yandex.market.mbo.db.modelstorage.conversion;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinitionBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.collection.ListRandomizer;
import io.github.benas.randombeans.randomizers.collection.MapRandomizerExt;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageTestUtil;
import ru.yandex.market.mbo.db.params.ParameterProtoConverter;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.PickerImage;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.ParameterValueHypothesis;
import ru.yandex.market.mbo.randomizers.ParameterValueRandomizer;
import ru.yandex.market.mbo.randomizers.ParameterValuesRandomizer;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.util.List;
import java.util.Map;

import static ru.yandex.market.mbo.export.MboParameters.ValueType;
import static ru.yandex.market.mbo.export.MboParameters.Word;

/**
 * @author s-ermakov
 */
public class CommonModelPojoProtoConversionTest {

    private static final long RANDOM_SEED = 314159265L;

    private EnhancedRandom random;

    @Before
    @SuppressWarnings("checkstyle:magicNumber")
    public void setUp() throws Exception {

        ParameterValueRandomizer parameterValueRandomizer = new ParameterValueRandomizer(
            EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .exclude(PickerImage.class)
                .seed(RANDOM_SEED)
                .build()
        );
        ParameterValueRandomizer parameterValueLinkRandomizer = new ParameterValueRandomizer(
            EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .seed(RANDOM_SEED)
                .build()
        );
        ParameterValuesRandomizer parameterValuesRandomizer = new ParameterValuesRandomizer(
            parameterValueRandomizer,
            EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .exclude(PickerImage.class)
                .seed(RANDOM_SEED)
                .build()
        );

        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .exclude(CommonModel.class)
            .seed(RANDOM_SEED)
            .stringLengthRange(3, 10)
            .collectionSizeRange(1, 5)
            .overrideDefaultInitialization(true)
            // custom randomizers
            .randomize(FieldDefinitionBuilder.field().named("parameterValuesMap").ofType(Map.class).get(),
                MapRandomizerExt.aNewMapRandomizer(ParameterValues::getParamId, parameterValuesRandomizer, 5))
            .randomize(FieldDefinitionBuilder.field().named("parameterValues").ofType(Map.class).get(),
                MapRandomizerExt.aNewMapRandomizer(ParameterValue::getParamId, parameterValueRandomizer, 3))
            .randomize(FieldDefinitionBuilder.field().named("parameterValues").ofType(List.class).get(),
                ListRandomizer.aNewListRandomizer(parameterValueRandomizer, 3))
            .randomize(FieldDefinitionBuilder.field().named("parameterValuesLinks").ofType(List.class).get(),
                ListRandomizer.aNewListRandomizer(parameterValueLinkRandomizer, 3))
            .randomize(FieldDefinitionBuilder.field().ofType(ParameterValue.class).get(), parameterValueRandomizer)

            .build();
    }

    @Test
    public void testConversionToPojo() {
        CommonModel commonModel = random.nextObject(CommonModel.class);

        ModelStorage.Model proto = ModelProtoConverter.convert(commonModel);
        CommonModel commonModel2 = ModelProtoConverter.convert(proto);

        // тяжело сравнивать pojo элементы, поэтому конвертируем в прото
        ModelStorage.Model proto2 = ModelProtoConverter.convert(commonModel2);
        ModelStorageTestUtil.Diff diff = ModelStorageTestUtil.generateDiff(proto, proto2);
        diff.assertEquals();
    }

    @Test
    public void testCopy() {
        CommonModel commonModel = random.nextObject(CommonModel.class);
        CommonModel copy = new CommonModel(commonModel);

        // тяжело сравнивать pojo элементы, поэтому конвертируем в прото
        ModelStorage.Model proto1 = ModelProtoConverter.convert(commonModel);
        ModelStorage.Model proto2 = ModelProtoConverter.convert(copy);
        ModelStorageTestUtil.Diff diff = ModelStorageTestUtil.generateDiff(proto1, proto2);
        diff.assertEquals();
    }

    @Test
    public void testClearPublishedField() {
        CommonModel guruModel = random.nextObject(CommonModel.class,
            "clusterizerOffers", "pinnedOffers", "throwedOffers", "trashedOffers");
        guruModel.setCurrentType(CommonModel.Source.GURU);
        CommonModel cluster = random.nextObject(CommonModel.class,
            "clusterizerOffers", "pinnedOffers", "throwedOffers", "trashedOffers");
        cluster.setCurrentType(CommonModel.Source.CLUSTER);
        CommonModel generatedModel = random.nextObject(CommonModel.class,
            "clusterizerOffers", "pinnedOffers", "throwedOffers", "trashedOffers");
        generatedModel.setCurrentType(CommonModel.Source.GENERATED);

        ModelStorage.Model proto = ModelProtoConverter.convert(guruModel).toBuilder()
            .clearPublished()
            .build();
        CommonModel converted = ModelProtoConverter.convert(proto);
        Assertions.assertThat(converted.isPublished()).isFalse();

        proto = ModelProtoConverter.convert(cluster).toBuilder()
            .clearPublished()
            .build();
        converted = ModelProtoConverter.convert(proto);
        Assertions.assertThat(converted.isPublished()).isTrue();

        proto = ModelProtoConverter.convert(generatedModel).toBuilder()
            .clearPublished()
            .build();
        converted = ModelProtoConverter.convert(proto);
        Assertions.assertThat(converted.isPublished()).isTrue();

        // тяжело сравнивать pojo элементы, поэтому конвертируем в прото
        ModelStorage.Model proto2 = ModelProtoConverter.convert(converted);
        ModelStorageTestUtil.Diff diff = ModelStorageTestUtil.generateDiff(proto, proto2);
        diff.assertEquals();
    }

    @Test
    @SuppressWarnings("checkstyle:magicNumber")
    public void testConvertParameterValueHypothesis() {
        ModelStorage.Model protoModel = ModelStorage.Model.newBuilder()
            .addParameterValueHypothesis(ParameterValueHypothesis.newBuilder()
                .setXslName("xsl-name-1").setParamId(1L).setValueType(ValueType.ENUM)
                .addStrValue(word("str")))
            .addParameterValueHypothesis(ParameterValueHypothesis.newBuilder()
                .setXslName("xsl-name-2").setParamId(2L).setValueType(ValueType.NUMERIC_ENUM)
                .addStrValue(word("str21")))
            .addParameterValueHypothesis(ParameterValueHypothesis.newBuilder()
                .setXslName("xsl-name-2").setParamId(2L).setValueType(ValueType.NUMERIC_ENUM)
                .addStrValue(word("str22")))
            .addParameterValueHypothesis(ParameterValueHypothesis.newBuilder()
                .setXslName("xsl-name-3").setParamId(3L).setValueType(ValueType.ENUM)
                .addStrValue(word("str31"))
                .addStrValue(word("str32")))
            .build();

        CommonModel commonModel = ModelProtoConverter.convert(protoModel);

        MboAssertions.assertThat(commonModel).getParameterValuesHypothesis(1L).values("str");
        MboAssertions.assertThat(commonModel).getParameterValuesHypothesis(2L).values("str21", "str22");
        MboAssertions.assertThat(commonModel).getParameterValuesHypothesis(3L).values("str31", "str32");

        ModelStorage.Model convertedProtoModel = ModelProtoConverter.convert(commonModel);
        Assertions.assertThat(convertedProtoModel.getParameterValueHypothesisList())
            .containsExactlyInAnyOrder(
                ParameterValueHypothesis.newBuilder()
                    .setXslName("xsl-name-1").setParamId(1L).setValueType(ValueType.ENUM)
                    .addStrValue(word("str")).build(),
                ParameterValueHypothesis.newBuilder()
                    .setXslName("xsl-name-2").setParamId(2L).setValueType(ValueType.NUMERIC_ENUM)
                    .addStrValue(word("str21")).build(),
                ParameterValueHypothesis.newBuilder()
                    .setXslName("xsl-name-2").setParamId(2L).setValueType(ValueType.NUMERIC_ENUM)
                    .addStrValue(word("str22")).build(),
                ParameterValueHypothesis.newBuilder()
                    .setXslName("xsl-name-3").setParamId(3L).setValueType(ValueType.ENUM)
                    .addStrValue(word("str31")).build(),
                ParameterValueHypothesis.newBuilder()
                    .setXslName("xsl-name-3").setParamId(3L).setValueType(ValueType.ENUM)
                    .addStrValue(word("str32")).build()
            );
    }

    private Word word(String value) {
        return ParameterProtoConverter.convert(WordUtil.defaultWord(value));
    }
}
