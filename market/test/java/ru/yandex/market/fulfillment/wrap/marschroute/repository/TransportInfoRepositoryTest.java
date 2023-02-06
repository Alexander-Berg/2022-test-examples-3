package ru.yandex.market.fulfillment.wrap.marschroute.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.TransportInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Тест репозитория {@link TransportInfoRepository}.
 */
class TransportInfoRepositoryTest extends RepositoryTest {

    @Autowired
    private TransportInfoRepository repository;

    /**
     * Начальное состояние таблицы реестра транспорта - пустая.
     *
     * <p>Сценарий:</p>
     * <ul>
     * <li>Создаем запись о транспорте (все поля заполнены).</li>
     * <li>Сохраняем информацию о транспорте.</li>
     * </ul>
     * <p>Ожидаемое состояние после выполнения: В базе появится запись с указанной информацией о транспорте.</p>
     */
    @Test
    @DatabaseSetup("classpath:repository/transport_info/1/setup.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "classpath:repository/transport_info/1/expected.xml")
    void testProduceOnFilledDatabaseInsert() {
        List<TransportInfo> infos = Collections.singletonList(createInfo1());
        repository.saveAll(infos);
    }

    /**
     * Начальное состояние таблицы реестра транспорта - имеется одна запись.
     *
     * <p>Сценарий:</p>
     * <ul>
     * <li>Создаем другую запись о транспорте (заполнены только обязательные поля).</li>
     * <li>Сохраняем информацию о транспорте.</li>
     * </ul>
     * <p>Ожидаемое состояние после выполнения: В базе старая запись останется, появится новая с указанной информацией
     * о транспорте.</p>
     */
    @Test
    @DatabaseSetup("classpath:repository/transport_info/2/setup.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "classpath:repository/transport_info/2/expected.xml")
    void testProduceOnFilledDatabaseInsertOnlyRequiredAttributes() {
        List<TransportInfo> infos = Lists.newArrayList(createInfo2());
        repository.saveAll(infos);
    }

    /**
     * Начальное состояние таблицы реестра транспорта - имеется два записи.
     *
     * <p>Сценарий:</p>
     * <ul>
     * <li>Удаляем все записи за 2018.04.05.</li>
     * </ul>
     * <p>Ожидаемое состояние после выполнения: В базе останется запись за 2019.04.05.</p>
     */
    @Test
    @DatabaseSetup("classpath:repository/transport_info/3/setup.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "classpath:repository/transport_info/3/expected.xml")
    void testProduceOnFilledDatabaseDelete() {
        final LocalDateTime dateFrom = LocalDate.of(2018, 4, 5).atStartOfDay();
        final LocalDateTime dateTo = dateFrom.plusDays(1);
        repository.deleteByDateArrive(dateFrom, dateTo);
    }

    private TransportInfo createInfo1() {
        TransportInfo info = new TransportInfo();
        info.setTicketId(1);
        info.setDateArrive(LocalDateTime.parse("2018-04-05T18:55:56"));
        info.setNumberPlate("777");
        info.setTransportType(4);
        info.setPurpose(1);
        info.setStatus(16);
        info.setDateStatus(LocalDateTime.parse("2018-04-05T19:36:33"));
        info.setDateGate(LocalDateTime.parse("2018-04-05T20:23:17"));
        info.setDateComplete(LocalDateTime.parse("2018-04-05T21:45:18"));
        info.setDocsIds("1523,6478,2884,2226");
        info.setOrdersIds("8473,2562,7842,745");
        info.setActNumber("28611235O - 210141");
        return info;
    }

    private TransportInfo createInfo2() {
        TransportInfo info = new TransportInfo();
        info.setTicketId(2);
        info.setDateArrive(LocalDateTime.parse("2019-04-05T19:23:19"));
        info.setNumberPlate("392");
        info.setTransportType(3);
        info.setPurpose(2);
        info.setStatus(24);
        return info;
    }
}
