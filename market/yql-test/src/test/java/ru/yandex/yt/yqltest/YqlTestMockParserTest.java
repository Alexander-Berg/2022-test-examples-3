package ru.yandex.yt.yqltest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.type_info.TiType;
import ru.yandex.yt.yqltest.impl.YqlTestMock;
import ru.yandex.yt.yqltest.impl.YqlTestMockDir;
import ru.yandex.yt.yqltest.impl.YqlTestMockParser;
import ru.yandex.yt.yqltest.impl.YqlTestMockTable;
import ru.yandex.yt.yqltestable.YqlTestable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.08.2021
 */
public class YqlTestMockParserTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testParser() {
        YqlTestMock mock = YqlTestMockParser.parseFiles(
            YPath.cypressRoot(),
            "/yql/test_query.mock"
        );

        YqlTestMockDir dirMock = mock.getDirMockMap().get("//home/market/production");
        YqlTestMockTable dirMockTable = dirMock.getTables().get("tablename");
        assertEquals(1, mock.getDirMockMap().size());
        assertEquals(1, dirMock.getTables().size());
        assertEquals(Map.of(
            "model_id", TiType.int64(),
            "yandexuid", TiType.string(),
            "passportuid", TiType.string(),
            "timestamp", TiType.uint64()
        ), dirMockTable.getSchema().buildSchemaMap());
        assertEquals(toNodes(List.of(
            Map.of(
                "model_id", 1,
                "yandexuid", "some",
                "passportuid", "123",
                "timestamp", 1630415828000L
            ),
            Map.of(
                "model_id", 2,
                "yandexuid", "some",
                "passportuid", "124",
                "timestamp", 1630415829000L
            ),
            Map.of(
                "model_id", 5,
                "yandexuid", "some",
                "passportuid", "127"
            )
        )), dirMockTable.getData());

        assertEquals(1, mock.getTableMockMap().size());
        YqlTestMockTable tableMock = mock.getTableMockMap().get("//home/cdc/market/_YT_ENV_/tablename");
        assertEquals(Map.of(
            "state", TiType.int16(),
            "resource_id", TiType.int64(),
            "verified", TiType.int16(),
            "cr_time", TiType.utf8()
        ), tableMock.getSchema().buildSchemaMap());
        assertEquals(toNodes(List.of(
            Map.of(
                "state", 0,
                "cr_time", "2021-08-31T13:17:08+00:00"
            ),
            Map.of(
                "state", 0,
                "resource_id", 123456789101112L,
                "verified", 1
            )
        )), tableMock.getData());
    }

    @Test
    public void testParserComplex() {
        YqlTestMock mock = YqlTestMockParser.parseFiles(
            YPath.cypressRoot(),
            "/yql/test_query.mock",
            "/yql/test_query_2.mock"
        );

        // dir is same
        YqlTestMockDir dirMock = mock.getDirMockMap().get("//home/market/production");
        YqlTestMockTable dirMockTable = dirMock.getTables().get("tablename");
        assertEquals(1, mock.getDirMockMap().size());
        assertEquals(1, dirMock.getTables().size());
        assertEquals(Map.of(
            "model_id", TiType.int64(),
            "yandexuid", TiType.string(),
            "passportuid", TiType.string(),
            "timestamp", TiType.uint64()
        ), dirMockTable.getSchema().buildSchemaMap());

        // tables now 2, old is overwritten
        assertEquals(2, mock.getTableMockMap().size());
        assertEquals(Map.of(
            "state", TiType.int16(),
            "resource_id", TiType.int64(),
            "verified", TiType.int16()
        ), mock.getTableMockMap().get("//home/cdc/market/_YT_ENV_/tablename").getSchema().buildSchemaMap());
        assertEquals(toNodes(List.of(
            Map.of(
                "state", 0,
                "resource_id", 123,
                "verified", 1
            ),
            Map.of(
                "state", 0,
                "resource_id", 456,
                "verified", 0
            )
        )), mock.getTableMockMap().get("//home/cdc/market/_YT_ENV_/tablename").getData());

        assertEquals(Map.of(
            "state", TiType.int16(),
            "resource_id", TiType.int64()
        ), mock.getTableMockMap().get("//home/cdc/market/_YT_ENV_/tablename_more").getSchema().buildSchemaMap());
        assertEquals(toNodes(List.of(
            Map.of(
                "state", 0,
                "resource_id", 1
            )
        )), mock.getTableMockMap().get("//home/cdc/market/_YT_ENV_/tablename_more").getData());
    }

    @Test
    public void testParserComplexYql() {
        YqlTestMock mock = YqlTestMockParser.parseFiles(
            YPath.cypressRoot(),
            "/yql/complex_query.mock"
        );

        mock.getDirMockMap().get("//home/market/production/pers-grade/tables/grade").setMockPath("//mock_1");
        mock.getTableMockMap().get("//home/cdc/market/_YT_ENV_/tablename").setMockPath("//mock_2");
        mock.getTableMockMap().get("//home/cdc/market/_YT_ENV_/tablename_2").setMockPath("//mock_3");
        mock.getVarTableMockMap().get("//tmp/yqltest/mocked_var/model_rating").setMockPath("//mock_4");
        mock.getVarTableMockMap().get("//tmp/yqltest/mocked_var/grade_count").setMockPath("//mock_5");

        String yql = mock.mockYql(YqlTestable.readFile("/yql/complex_query.sql"));

        assertEquals(YqlTestable.readFile("/yql/complex_query_expected.sql"), yql);
    }

    private List<JsonNode> toNodes(List<?> data) {
        return data.stream()
            .map(item -> {
                try {
                    return MAPPER.readTree(MAPPER.writeValueAsString(item));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());
    }
}
