package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.operation.Insert;
import ru.yandex.market.robot.shared.raw_model.MarketModel;
import ru.yandex.market.robot.shared.raw_model.RawModel;

import java.util.stream.Stream;

import static ru.yandex.market.robot.db.raw_model.tables.Columns.*;

/**
 * @author jkt on 20.12.17.
 */
public class MarketRelationModelTable {

    public static final String NAME = "market_relation_model";

    public static Stream<Insert> entryFor(RawModel model) {
        return model.getMarketRelations().getMarketModels().stream()
            .map(marketModel -> entryFor(model.getId(), marketModel));
    }

    public static Insert entryFor(MarketModel marketModel) {
        return entryFor(marketModel.getId(), marketModel);
    }

    public static Insert entryFor(int modelId, MarketModel marketModel) {
        return entry()
            .row()
            .column(MODEL_ID, modelId)
            .column(MARKET_MODEL_ID, marketModel.getModelId())
            .column(MARKET_MODEL, marketModel.getModelName())
            .column(MARKET_MODIFICATION_ID, marketModel.getModificationId())
            .column(MARKET_MODIFICATION, marketModel.getModificationName())
            .column(STATUS, marketModel.getStatus())
            .column(CREATED, marketModel.isCreated())
            .column(FIRST_VERSION_NUMBER, marketModel.getFirstVersionNumber())
            .column(LAST_VERSION_NUMBER, marketModel.getLastVersionNumber())
            .column(MODEL_CREATED_DATE, marketModel.getModelCreatedDate())
            .column(MODEL_PUBLISHED_DATE, marketModel.getModelPublishDate())
            .column(FIRST_PARAMS_COVERAGE, marketModel.getFirstParamsCoverage())
            .column(LAST_PARAMS_COVERAGE, marketModel.getLastParamsCoverage())
            .column(PICTURES_COUNT, marketModel.getPicturesCount())
            .end()

            .withBinder(new ModelStatusBinder(), STATUS)

            .build();
    }

    public static Insert.Builder entry() {
        return Insert.into(NAME);
    }
}
