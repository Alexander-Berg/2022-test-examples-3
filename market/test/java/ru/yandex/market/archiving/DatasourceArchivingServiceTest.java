package ru.yandex.market.archiving;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.google.common.base.Preconditions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.archiving.exception.DatasourceArchivingException;
import ru.yandex.market.archiving.model.DatasourceArchive;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.archive.ArchivingService;
import ru.yandex.market.core.archive.DatabaseModelService;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Тесты для {@link DatasourceArchivingService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DatasourceArchivingServiceTest extends FunctionalTest {

    @Autowired
    private DatasourceArchivingService datasourceArchivingService;

    @Autowired
    private DatabaseModelService databaseModelService;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private HistoryService historyService;

    @Test
    @DisplayName("Архивация магазина")
    @DbUnitDataSet(
            before = "csv/datasourceArchivingService/only_partner.before.csv",
            after = "csv/datasourceArchivingService/only_partner.after.csv"
    )
    void testOnlyPartner() {
        datasourceArchivingService.archive(List.of(1002L));
    }

    @Test
    @DisplayName("Провал архивации по проценту ошибок")
    @DbUnitDataSet
    void testPercentFailure() {
        // Количество моков пугает, но иначе тест может получиться слишком сложным
        // Суть в том, что пытаемся сохранить 100 магазинов. 80 сохраняем удачно, для других бросаем исключения
        // Таким образом набираем нужный процент ошибок
        DatasourceInArchivingDao datasourceInArchivingDao = mock(DatasourceInArchivingDao.class);
        DatasourceArchivingDao datasourceArchivingDao = mock(DatasourceArchivingDao.class);
        doAnswer(invocation -> {
            long id = invocation.getArgument(0);
            return new DatasourceArchive(id, LocalDateTime.now(), "xml");
        }).when(datasourceArchivingDao).get(anyLong());

        DatasourceArchivingService service = new DatasourceArchivingService(databaseModelService,
                mock(ArchivingService.class),
                protocolService,
                historyService,
                datasourceArchivingDao,
                datasourceInArchivingDao,
                mock(JdbcTemplate.class),
                environmentService
        );

        // Будем бросать исключение для id > 81, чтобы набрать нужный процент ошибок
        AtomicInteger failurePercent = new AtomicInteger(81);
        environmentService.setValue("datasource.archiving.failure_percent", failurePercent.toString());
        doAnswer(invocation -> {
            long id = invocation.getArgument(0);
            Preconditions.checkState(id < failurePercent.get(), "Test fail");
            return null;
        }).when(datasourceInArchivingDao).delete(anyLong());

        List<Long> ids = LongStream.rangeClosed(1, 100).boxed().collect(Collectors.toList());

        assertThatExceptionOfType(DatasourceArchivingException.class)
                .isThrownBy(() -> service.archive(ids));

        // Проверяем, что если бы успешных архиваций было на 1 больше, то исключения бы не было
        failurePercent.incrementAndGet();
        service.archive(ids);
    }
}
