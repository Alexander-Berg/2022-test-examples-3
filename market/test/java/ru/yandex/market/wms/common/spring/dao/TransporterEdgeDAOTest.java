package ru.yandex.market.wms.common.spring.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.DatabaseSchema;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.implementation.TransporterEdgeDAO;
import ru.yandex.market.wms.common.spring.dto.TransporterEdgeDto;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class TransporterEdgeDAOTest extends IntegrationTest {
    @Autowired
    private TransporterEdgeDAO transporterEdgeDAO;

    /*
     * Три ячейки выхода зоны отправки и две ячейки входа зоны получения, объединенные транспортером.
     * Всего 6 комбинаций.
     * */
    @Test
    @DatabaseSetup(value = "/db/dao/transporter-loc/1/db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/transporter-loc/1/db.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseSetup(value = "/db/dao/transporter-loc/1/db.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/db/dao/transporter-loc/1/db.xml", connection = "enterpriseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void getPathsReturnsSingleTransporterAndMultipleInputOutputLocs() {
        List<TransporterEdgeDto> result = transporterEdgeDAO.getPaths("STATION 1", "STATION 2", DatabaseSchema.WMWHSE1);
        assertions.assertThat(result.size()).isEqualTo(6);
        assertions.assertThat(result.stream().map(m -> m.getTransporter().getTransporterId()).distinct().count()).
                isEqualTo(1);
        assertions.assertThat(result.stream().map(m -> m.getSource().getLoc()).distinct().count()).isEqualTo(3);
        assertions.assertThat(result.stream().map(m -> m.getDestination().getLoc()).distinct().count()).isEqualTo(2);
    }

    /*
     * Путь не найден, т.к. не заведены ячейки выхода транспортера
     * */
    @Test
    @DatabaseSetup(value = "/db/dao/transporter-loc/2/db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/transporter-loc/2/db.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseSetup(value = "/db/dao/transporter-loc/2/db.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/db/dao/transporter-loc/2/db.xml", connection = "enterpriseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void getPathsReturnsNoPathsBecauseOfNoOutputLocs() {
        List<TransporterEdgeDto> result = transporterEdgeDAO.getPaths("STATION 3", "STATION 2", DatabaseSchema.WMWHSE1);
        assertions.assertThat(result.size() == 0).isTrue();
    }

    /*
     * Доставка возможна по любому из двух имеющихся транспортеров.
     * Для первого - 2 ячейки на вход, 2 на выход.
     * Для второго - 2 ячейки на вход, 1 на выход.
     * */
    @Test
    @DatabaseSetup(value = "/db/dao/transporter-loc/3/db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/transporter-loc/3/db.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseSetup(value = "/db/dao/transporter-loc/3/db.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/db/dao/transporter-loc/3/db.xml", connection = "enterpriseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void getPathsReturnsMultipleTransporters() {
        List<TransporterEdgeDto> result = transporterEdgeDAO.getPaths("ST 1A", "ST 1B", DatabaseSchema.WMWHSE1);

        assertions.assertThat(result.size()).isEqualTo(6);
        assertions.assertThat(result.stream().map(m -> m.getTransporter().getTransporterId()).distinct().count())
                .isEqualTo(2);

        assertions.assertThat(result.stream().filter(f -> f.getTransporter().getTransporterId().equals("FFA-11025"))
                .map(m -> m.getSource().getLoc()).distinct().count()).isEqualTo(2);
        assertions.assertThat(result.stream().filter(f -> f.getTransporter().getTransporterId().equals("FFA-11025"))
                .map(m -> m.getDestination().getLoc()).distinct().count()).isEqualTo(2);

        assertions.assertThat(result.stream().filter(f -> f.getTransporter().getTransporterId().equals("FEU-2000"))
                .map(m -> m.getSource().getLoc()).distinct().count()).isEqualTo(2);
        assertions.assertThat(result.stream().filter(f -> f.getTransporter().getTransporterId().equals("FEU-2000"))
                .map(m -> m.getDestination().getLoc()).distinct().count()).isEqualTo(1);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/transporter-loc/4/db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/transporter-loc/4/db.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseSetup(value = "/db/dao/transporter-loc/4/db.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/db/dao/transporter-loc/4/db.xml", connection = "enterpriseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void getPathsReturnsMultipleSourceCellsSingleDestinationCell() {
        List<TransporterEdgeDto> result = transporterEdgeDAO.getPaths("ST 1A", "ST 1B", DatabaseSchema.WMWHSE1);
        assertions.assertThat(result.stream().map(m -> m.getSource().getLoc()).distinct().count()).isEqualTo(2);
        assertions.assertThat(result.stream().map(m -> m.getDestination().getLoc()).distinct().count()).isEqualTo(1);
    }
}

