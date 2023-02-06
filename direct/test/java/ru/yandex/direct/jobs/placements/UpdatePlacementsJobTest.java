package ru.yandex.direct.jobs.placements;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.yql.response.YqlResultBuilder;
import ru.yandex.yql.response.YqlResultSet;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.jobs.placements.PlacementsJobTestUtils.FIELDS_NUM;
import static ru.yandex.direct.jobs.placements.PlacementsJobTestUtils.FIELD_NAMES;
import static ru.yandex.direct.jobs.placements.PlacementsJobTestUtils.FIELD_TYPES;

class UpdatePlacementsJobTest {

    private YqlResultSet resultSet;

    @BeforeEach
    void prepareResultSet() {
        String ytOutdoorBlocks =
                "[{\"Id\":12, \"Width\":\"1000.0\",\"Height\":\"500.0\",\"MinDuration\":7.5," +
                        "\"Resolution\":\"100x200\", \"Sizes\":[], "
                        + "\"GPS\":\"55.705534,37.563996\",\"FacilityType\":1,\"Direction\":\"180\",\"Address\":\"ул." +
                        " Мира\","
                        + "\"PhotoList\":[{\"formats\": [{\"height\":1000, \"width\":2500, \"path\":\"/some/path\"}]}],"
                        + "\"BlockCaption\":\"Test block caption of 12L\"}]";
        String ytIndoorBlocks =
                "[{\"Id\":13, \"FacilityType\":\"2\",\"ZoneCategory\":\"1\",\"Resolution\":\"1280x200\", \"Sizes\":[], "
                        + "\"GPS\":\"56,38\",\"AspectRatio\":[],\"Address\":\"ул. Мира 2\","
                        + "\"PhotoList\":[{\"formats\": [{\"height\":120, \"width\":80, \"path\":\"/some/path\"}]}]}]";
        this.resultSet = YqlResultBuilder.builder(FIELDS_NUM)
                .names(FIELD_NAMES)
                .types(FIELD_TYPES)
                .addRow("1", "unknown", "hightech.fm", "HighTech.FM", "false", "false", "false", "mirror1.ru", "[]",
                        "1", "hightech")
                .addRow("3", "outdoor", "rusoutdoor.ru", "RusOutdoor", "false", "false", "true", "",
                        ytOutdoorBlocks, "4", "rusoutdoor")
                .addRow("7", "indoor", "rusindoor.ru", "RusIndoor", "false", "true", "false", "",
                        ytIndoorBlocks, "19", "rusindoor")
                .build();
    }

    @Test
    void validateResultColumnOrder_success() throws Exception {
        new UpdatePlacementsJob().validateResultColumnOrder(resultSet);
    }

    @Test
    void validateResultColumnOrder_wrongOrder() {
        YqlResultSet resultSet = mock(YqlResultSet.class);
        asList(QueryField.values())
                .forEach(field -> when(resultSet.asColNum(field.name)).thenReturn(field.position + 1));
        assertThatThrownBy(() -> new UpdatePlacementsJob().validateResultColumnOrder(resultSet))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void validateResultColumnOrder_missingField() {
        YqlResultSet resultSet = mock(YqlResultSet.class);
        when(resultSet.asColNum(anyString())).thenCallRealMethod();
        assertThatThrownBy(() -> new UpdatePlacementsJob().validateResultColumnOrder(resultSet))
                .isInstanceOf(SQLException.class);
    }
}
