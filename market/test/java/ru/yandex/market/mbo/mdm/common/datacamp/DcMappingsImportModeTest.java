package ru.yandex.market.mbo.mdm.common.datacamp;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class DcMappingsImportModeTest extends MdmBaseDbTestClass {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;
    @Autowired
    private StorageKeyValueService skv;

    @Test
    public void testAutoInsertWorks() {
        Assertions.assertThat(skv.getValue(
            MdmProperties.DC_MAPPINGS_IMPORT_MODE,
            DcMappingsImportMode.SAME_TOPIC_WITH_OFFERS,
            DcMappingsImportMode.class
        )).isEqualTo(DcMappingsImportMode.SAME_TOPIC_WITH_OFFERS);

        for (var value : DcMappingsImportMode.values()) {
            skv.putValue(MdmProperties.DC_MAPPINGS_IMPORT_MODE, value);
            Assertions.assertThat(skv.getValue(MdmProperties.DC_MAPPINGS_IMPORT_MODE, DcMappingsImportMode.class))
                .isEqualTo(value);
        }
    }

    @Test
    public void testManualInsertWorks() {
        jdbc.update("insert into mdm.storage_key_value values (:key, :value)",
            Map.of("key", MdmProperties.DC_MAPPINGS_IMPORT_MODE, "value", "\"BOTH_TOPICS\""));
        Assertions.assertThat(skv.getValue(MdmProperties.DC_MAPPINGS_IMPORT_MODE, DcMappingsImportMode.class))
            .isEqualTo(DcMappingsImportMode.BOTH_TOPICS);
    }
}
