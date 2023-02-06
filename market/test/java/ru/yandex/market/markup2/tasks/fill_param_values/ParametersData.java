package ru.yandex.market.markup2.tasks.fill_param_values;

import ru.yandex.market.markup2.utils.ParameterTestUtils;
import ru.yandex.market.mbo.export.MboParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 29.06.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
class ParametersData {
    private ParametersData() {

    }

    static List<MboParameters.Parameter> generateParameters() {
        List<MboParameters.Parameter> parameters = new ArrayList<>();

        MboParameters.Parameter parameter =
            ParameterTestUtils.createParameterBuilder(1, MboParameters.ValueType.BOOLEAN, "first")
                .setCommentForOperator("comment1")
                .setDescription("description1")
                .setMandatoryForSignature(true)
                .addOption(ParameterTestUtils.createOption(1, "TRUE"))
                .addOption(ParameterTestUtils.createOption(2, "FALSE"))
                .addName(ParameterTestUtils.createWord("first_name"))
                .build();

        parameters.add(parameter);

        parameter = ParameterTestUtils.createParameterBuilder(2, MboParameters.ValueType.ENUM, "second")
            .setMandatoryForSignature(false)
            .addOption(ParameterTestUtils.createOption(1, "FIRST"))
            .addOption(ParameterTestUtils.createOption(2, "SECOND"))
            .addOption(ParameterTestUtils.createOption(3, "THIRD"))
            .addName(ParameterTestUtils.createWord("second_name"))
            .build();

        parameters.add(parameter);

        parameter = ParameterTestUtils.createParameterBuilder(3, MboParameters.ValueType.NUMERIC, "third")
            .setCommentForOperator("comment3")
            .setDescription("description3")
            .setMinValue(1)
            .setMaxValue(5)
            .setMandatoryForSignature(true)
            .setUnit(
                ParameterTestUtils.unit(1, ParameterTestUtils.measure(1, "diagonal"), "inches"))
            .addName(ParameterTestUtils.createWord("third_name"))
            .build();

        parameters.add(parameter);

        return parameters;
    }

    static List<MboParameters.Parameter> generateParametersWithString() {
        List<MboParameters.Parameter> parameters = generateParameters();

        parameters.add(ParameterTestUtils.createParameterBuilder(4, MboParameters.ValueType.STRING, "string")
            .setCommentForOperator("commentStr")
            .setDescription("descriptionString")
            .addName(ParameterTestUtils.createWord("string_param"))
            .setGuruType(MboParameters.GuruType.GURU_TYPE_STRING)
            .build());

        parameters.add(ParameterTestUtils.createParameterBuilder(5, MboParameters.ValueType.STRING, "text")
            .setCommentForOperator("commentText")
            .setDescription("descriptionText")
            .addName(ParameterTestUtils.createWord("text_param"))
            .setGuruType(MboParameters.GuruType.GURU_TYPE_TEXT)
            .build());

        return parameters;
    }
}
