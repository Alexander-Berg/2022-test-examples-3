package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.bind.Binder;
import com.ninja_squad.dbsetup.operation.Insert;
import ru.yandex.market.robot.shared.clusterizer.CategorySettingsDto;
import ru.yandex.market.robot.shared.clusterizer.title_utils.TitleProcessorDto;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.CATEGORY_ID;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.SETTINGS;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.SOURCES;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.TYPE;

/**
 * @author jkt on 19.12.17.
 */
public class TitleProcessorTable {

    public static final String NAME = "title_processor";

    public static final String SOURCES_DELIMITER = "|";

    public static Insert.RowBuilder entryFor(TitleProcessorDto titleProcessor) {
        return insertInto(NAME)
            .columns(TYPE, SETTINGS, CATEGORY_ID, SOURCES)
            .withBinder(new SourcesBinder(), SOURCES)
            .row()
            .column(TYPE, titleProcessor.getType())
            .column(SETTINGS, titleProcessor.getSettings())
            .column(SOURCES, titleProcessor.getSources());
    }

    public static Stream<Insert> entryFor(int categoryId, TitleProcessorDto... titleProcessors) {
        return Stream.of(titleProcessors)
            .map(titleProcessor ->
                TitleProcessorTable.entryFor(titleProcessor)
                    .column(Columns.CATEGORY_ID, categoryId)
                    .end()
                    .build()
            );

    }

    public static Stream<Insert> entryFor(CategorySettingsDto categorySettings) {
        return entryFor(
            categorySettings.getCategoryId(),
            categorySettings.getTitleProcessors().stream().toArray(TitleProcessorDto[]::new));
    }

    public static class SourcesBinder implements Binder {

        @Override
        public void bind(PreparedStatement statement, int param, Object value) throws SQLException {
            statement.setObject(param, sourcesString(value));
        }


        private String sourcesString(Object value) throws SQLException {
            if (!(value instanceof Collection)) {
                throw new IllegalStateException(value + " is not a collection. Can not generate aliases data");
            }

            Collection<?> aliases = (Collection<?>) value;

            return aliases.stream()
                .map(Objects::toString)
                .collect(Collectors.joining(SOURCES_DELIMITER));
        }
    }
}
