package ru.yandex.market.archiving;

import java.time.Clock;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.protocol.ProtocolService;

/**
 * Тесты для {@link DatasourceArchiveCommand}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DatasourceArchiveCommandTest extends FunctionalTest {

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private ParamService paramService;

    @Autowired
    private DatasourceInArchivingDao datasourceInArchivingDao;

    @Autowired
    @Qualifier("clock") Clock clock;

    private DatasourceArchiveCommand datasourceArchiveCommand;

    @BeforeEach
    void init() {
        datasourceArchiveCommand = new DatasourceArchiveCommand(
                protocolService,
                paramService,
                datasourceInArchivingDao,
                clock
        );
    }

    @Test
    @DisplayName("Добавление в очередь на архивацию")
    @DbUnitDataSet(
            before = "csv/datasourceArchiveCommand/add.before.csv",
            after = "csv/datasourceArchiveCommand/add.after.csv"
    )
    void testAddQueue() {
        datasourceArchiveCommand.performCommand(Arrays.asList(1001L, 1003L, 1004L));
    }
}
