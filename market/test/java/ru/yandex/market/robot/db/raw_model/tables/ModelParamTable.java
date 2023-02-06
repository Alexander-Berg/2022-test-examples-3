package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.operation.Insert;
import ru.yandex.market.robot.shared.raw_model.RawModel;

import java.util.stream.Stream;

import static com.ninja_squad.dbsetup.generator.ValueGenerators.sequence;
import static com.ninja_squad.dbsetup.generator.ValueGenerators.stringSequence;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.*;

/**
 * @author jkt on 20.12.17.
 */
public class ModelParamTable {

    public static final String NAME = "model_param";


    public static Stream<Insert> entryAsInternalParamFor(RawModel model) {
        return entryFor(model, true);
    }

    public static Stream<Insert> entryAsExternalParamFor(RawModel model) {
        return entryFor(model, false);
    }


    public static Stream<Insert> entryFor(RawModel model, boolean internal) {
        return model.getParams().stream()
            .map(param ->
                Insert.into(NAME)
                    .row()
                    .column(MODEL_ID, model.getId())
                    .column(FIRST_VERSION_NUMBER, model.getFirstVersionNumber())
                    .column(LAST_VERSION_NUMBER, model.getLastVersionNumber())
                    .column(INTERNAL, internal)
                    .column(Columns.NAME, param.getName())
                    .column(VALUE, param.getValue())
                    .column(UNIT, param.getUnit())
                    .column(INDEX, param.getIndex())
                    .end()

                    .withGeneratedValue(MARKET_PARAM_ID, sequence())
                    .withGeneratedValue(MARKET_VALUE, stringSequence("market_value_"))

                    .build()
            );
    }
}
