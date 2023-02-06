package ru.yandex.market.ff.service.yt;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.enums.ReturnReasonType;
import ru.yandex.market.ff.model.returns.ReturnItemFromCheckouterYtDto;
import ru.yandex.market.ff.model.returns.ReturnUnitFromCheckouterYtComplexKey;
import ru.yandex.market.ff.util.YqlTablesInPgUtils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class YtJdbcServiceTest extends IntegrationTest {

    @Autowired
    private YtJdbcService ytJdbcService;

    @Autowired
    @Qualifier("yqlAutoClusterNamedJdbcTemplate")
    private NamedParameterJdbcTemplate yqlJdbcTemplate;

    @BeforeEach
    public void setUp() {
        YqlTablesInPgUtils.recreateTables(yqlJdbcTemplate);
    }

    @Test
    public void selectCheckouterReturnsDataFromYtSuccess() {
        YqlTablesInPgUtils.insertIntoTrack(yqlJdbcTemplate, "2", "box1");
        YqlTablesInPgUtils.insertIntoReturn(yqlJdbcTemplate, 555, 666, 2);
        YqlTablesInPgUtils.insertIntoReturnDelivery(yqlJdbcTemplate, 2, 123);
        YqlTablesInPgUtils.insertIntoReturnItem(yqlJdbcTemplate, 1001, 666, 555, 1111, 2, "reason1", 0);

        YqlTablesInPgUtils.insertIntoTrack(yqlJdbcTemplate, "3", "box1");
        YqlTablesInPgUtils.insertIntoReturn(yqlJdbcTemplate, 556, 667, 3);
        YqlTablesInPgUtils.insertIntoReturnDelivery(yqlJdbcTemplate, 3, 124);
        YqlTablesInPgUtils.insertIntoReturnItem(yqlJdbcTemplate, 1002, 667, 556, 1112, 3, "reason2", 1);

        YqlTablesInPgUtils.insertIntoTrack(yqlJdbcTemplate, "4", "box2");
        YqlTablesInPgUtils.insertIntoReturn(yqlJdbcTemplate, 557, 668, 4);
        YqlTablesInPgUtils.insertIntoReturnDelivery(yqlJdbcTemplate, 4, 123);
        YqlTablesInPgUtils.insertIntoReturnItem(yqlJdbcTemplate, 1003, 668, 557, 1115, 1, "reason0", 2);

        Map<ReturnUnitFromCheckouterYtComplexKey, List<ReturnItemFromCheckouterYtDto>> result = ytJdbcService
                .selectCheckouterReturnsDataFromYt(Set.of("box1"), 123);

        assertions.assertThat(result).hasSize(1);
        List<ReturnItemFromCheckouterYtDto> items = result
                .get(new ReturnUnitFromCheckouterYtComplexKey("555", "box1", "666"));
        assertions.assertThat(items).hasSize(1);
        assertions.assertThat(items.get(0)).isEqualTo(new ReturnItemFromCheckouterYtDto(1111,
                ReturnReasonType.BAD_QUALITY, "reason1", 2));
    }
}
