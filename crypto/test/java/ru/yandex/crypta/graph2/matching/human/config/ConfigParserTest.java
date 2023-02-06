package ru.yandex.crypta.graph2.matching.human.config;

import java.util.List;

import org.junit.Test;

import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeType;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class ConfigParserTest {

    @Test
    public void parseEdgeTypes() {
        List<String> configEdgeTypes = List.of("mm_device_id_uuid_app-metrica_mm");
        List<TEdgeType> result = ConfigParser.parseEdgeTypes(configEdgeTypes);

        assertEquals(1, result.size());
        TEdgeType parsed = result.get(0);
        assertEquals(EIdType.MM_DEVICE_ID, parsed.getId1Type());
        assertEquals(EIdType.UUID, parsed.getId2Type());
        assertEquals(ESourceType.APP_METRICA, parsed.getSourceType());
        assertEquals(ELogSourceType.METRIKA_MOBILE_LOG, parsed.getLogSource());

    }

    @Test
    public void notParseEdgeTypes() {
        List<String> configEdgeTypes = List.of("mm_device_id_uuid_app_metrica_mm");
        List<TEdgeType> result = ConfigParser.parseEdgeTypes(configEdgeTypes);

        assertEquals(0, result.size());

    }
}
