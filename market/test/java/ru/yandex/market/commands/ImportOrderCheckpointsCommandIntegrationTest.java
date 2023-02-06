package ru.yandex.market.commands;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.AbstractYqlTest;
import ru.yandex.market.core.billing.OrderCheckpointDao;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.mbi.yt.YtUtil;

import static org.mockito.Mockito.mock;

/**
 * Тесты для {@link ImportOrderCheckpointsCommand}
 */
@Disabled("Проверка интеграции с YT - выполнение yql запроса. dev отладка")
class ImportOrderCheckpointsCommandIntegrationTest extends AbstractYqlTest {
    private ImportOrderCheckpointsCommand importOrderCheckpointsCommand;

    @BeforeEach
    void setUp() {
        importOrderCheckpointsCommand = new ImportOrderCheckpointsCommand(
                new YtTemplate(YtUtil.buildYtClusters(
                        List.of("hahn.yt.yandex.net"),
                        getYtToken())),
                mock(OrderCheckpointDao.class),
                mock(NamedParameterJdbcTemplate.class),
                yqlNamedParameterJdbcTemplate(),
                mock(TransactionTemplate.class),
                mock(OrderService.class));
    }

    /**
     * Для запуска теста надо положить продовый токен (не забыть стереть, чтоб не закомитить)
     * <p>
     * Важно! Не yql токен, а yt токен
     */
    private String getYtToken() {
        return "${mbi.robot.yt.token}";
    }

    @DisplayName("Проверяем, что запрос выполняется корректно")
    @Test
    void getOrderCheckpointsByDateRange() {
        System.out.println("start " + LocalDateTime.now());
        var checkpoints = importOrderCheckpointsCommand.getOrderCheckpointsByDateRange(
                LocalDate.parse("2021-10-01"),
                LocalDate.parse("2021-10-10")
        );
        for (var c : checkpoints) {
            System.out.println(c);
        }
        System.out.println("end " + LocalDateTime.now());
    }

    @DisplayName("Проверка чтения из темповой таблички")
    @Test
    void getOrderFromStaticTableByIds() {
        System.out.println("start " + LocalDateTime.now());
        importOrderCheckpointsCommand.importOrdersCheckpointsFromStaticTable(
                "//tmp/lom_returned_checkpoints",
                0L,
                500L,
                false
        );
        System.out.println("end " + LocalDateTime.now());
    }

    @Override
    protected String getUser() {
        return "${mbi.yql.jdbc.username}";
    }

    @Override
    protected String getPassword() {
        return "${mbi.robot.yql.token}";
    }

    @Override
    protected String getYqlUrl() {
        return "jdbc:yql://yql.yandex.net:443";
    }

}
