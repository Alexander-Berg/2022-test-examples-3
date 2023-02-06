package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.operation.Insert;
import ru.yandex.market.robot.shared.raw_model.RawModel;
import ru.yandex.market.robot.shared.raw_model.Recommendation;

import java.util.stream.Stream;

import static ru.yandex.market.robot.db.raw_model.tables.Columns.*;

/**
 * @author jkt on 20.12.17.
 */
public class ModelRecommendationTable {

    public static final String NAME = "model_recommendation";

    public static Stream<Insert> entryFor(RawModel model) {
        return model.getRecommendations().stream()
            .map(recommendation -> entryFor(model.getId(), recommendation));
    }

    public static Insert entryFor(int modelId, Recommendation recommendation) {
        return Insert.into(NAME)
            .row()
            .column(MODEL_ID, modelId)
            .column(Columns.NAME, recommendation.getName())
            .column(TYPE, recommendation.getTypeName())
            .column(SUPPLEMENT, recommendation.isSupplement())
            .column(REC_RAW_ID, recommendation.getRawId())
            .column(REC_MARKET_MODEL_ID, recommendation.getMarketModelId())
            .end()

            .build();
    }
}
