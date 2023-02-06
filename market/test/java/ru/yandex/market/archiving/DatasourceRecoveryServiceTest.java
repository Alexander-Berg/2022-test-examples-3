package ru.yandex.market.archiving;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тесты для {@link DatasourceRecoveryService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DatasourceRecoveryServiceTest extends FunctionalTest {

    @Autowired
    private DatasourceRecoveryService datasourceRecoveryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Восстановление магазина")
    @DbUnitDataSet(before = "csv/datasourceRecoveryService/only_partner.before.csv", after = "csv/datasourceRecoveryService/only_partner.after.csv")
    void testOnlyPartner() throws Exception {
        insertArchivedData(10267144L, "only_partner");
        datasourceRecoveryService.recovery(10267144L, false);
    }

    private void insertArchivedData(long entityId, final String file) throws Exception {
        final String s = IOUtils.toString(getClass().getResourceAsStream("xml/DatasourceRecoveryService." + file + ".xml"), StandardCharsets.UTF_8);
        jdbcTemplate.update("insert into shops_web.datasource_archiving (partner_id, data, archiving_date) values (?, ?, sysdate)", entityId, s);
    }
}
