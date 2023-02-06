package ru.yandex.market.delivery.transport_manager.service.startrek;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.commons.io.Charsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.StartrekIssue;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.CustomCsvSerializer;
import ru.yandex.market.delivery.transport_manager.service.ticket.service.StReturnDropoffTicketService;

@DatabaseSetup(value = "/repository/health/dbqueue/empty.xml", connection = "dbUnitDatabaseConnectionDbQueue")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class StReturnDropoffTicketServiceTest extends AbstractContextualTest {

    @Autowired
    private StReturnDropoffTicketService ticketService;

    @Autowired
    private TransportationMapper transportationMapper;

    @Autowired
    private CustomCsvSerializer customCsvSerializer;

    @Test
    @DatabaseSetup("/repository/startrek/for_return_dropoffs.xml")
    @ExpectedDatabase(
        value = "/repository/startrek/dbqueue/return_dropoff_ticket.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test() throws IOException {
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Class> classCaptor = ArgumentCaptor.forClass(Class.class);

        List<Transportation> transportations = transportationMapper.getByIds(Set.of(1L, 2L));
        ticketService.createCourierInfoTicket(LocalDate.of(2020, 9, 28), transportations);

        Mockito.verify(customCsvSerializer).serialize(
            listCaptor.capture(), classCaptor.capture(), Mockito.eq(Charsets.UTF_8), Mockito.eq(true)
        );

        softly.assertThat(classCaptor.getValue()).isEqualTo(StReturnDropoffTicketService.ReturnDropoffCsvRow.class);

        softly.assertThat(listCaptor.getValue()).containsExactlyInAnyOrder(
            new StReturnDropoffTicketService.ReturnDropoffCsvRow()
                .setScBagId("BAG-1")
                .setScCellId("VOZ_1")
                .setCarModel(null)
                .setCarNumber("M320KT")
                .setCourierName("Алибеков Алибек")
                .setCourierUID(444L)
                .setDate(LocalDate.of(2020, 9, 28))
                .setPhone("88005553535")
                .setDropoffName("DROPOFF-1")
                .setDropoffAddress("Москва, Льва Толстого, 10"),
            new StReturnDropoffTicketService.ReturnDropoffCsvRow()
                .setScBagId("BAG-2")
                .setScCellId("VOZ_2")
                .setCarModel(null)
                .setCarNumber("M320KT")
                .setCourierName("Алибеков Алибек")
                .setCourierUID(444L)
                .setDate(LocalDate.of(2020, 9, 28))
                .setPhone("88005553535")
                .setDropoffName("DROPOFF-1")
                .setDropoffAddress("Москва, Льва Толстого, 10"),
            new StReturnDropoffTicketService.ReturnDropoffCsvRow()
                .setScBagId("BAG-3")
                .setScCellId("VOZ_3")
                .setCarModel(null)
                .setCarNumber("A681PK")
                .setCourierName("Иванов Иван Иванович")
                .setCourierUID(555L)
                .setDate(LocalDate.of(2020, 9, 28))
                .setPhone("+79771234567")
                .setDropoffName("DROPOFF-2")
                .setDropoffAddress("Москва, Новинский бульвар, 8")
        );
    }

    @Test
    @DatabaseSetup("/repository/startrek/for_return_dropoffs.xml")
    @ExpectedDatabase(
        value = "/repository/startrek/dbqueue/return_dropoff_update_ticket.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdate() throws IOException {
        List<Transportation> transportations = transportationMapper.getByIds(Set.of(1L, 2L));
        StartrekIssue existing = new StartrekIssue().setId(1L);
        ticketService.updateCourierInfoTicket(
            existing, 111L, LocalDate.of(2020, 9, 28), transportations
        );

    }
}
