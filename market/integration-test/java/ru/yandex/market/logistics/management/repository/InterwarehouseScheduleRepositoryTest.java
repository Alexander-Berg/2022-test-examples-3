package ru.yandex.market.logistics.management.repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.InterwarehouseSchedule;
import ru.yandex.market.logistics.management.domain.entity.LogisticsPoint;
import ru.yandex.market.logistics.management.domain.entity.PartnerTransport;
import ru.yandex.market.logistics.management.domain.entity.type.InterwarehouseScheduleType;

public class InterwarehouseScheduleRepositoryTest extends AbstractContextualTest {

    @Autowired
    private InterwarehouseScheduleRepository repository;

    @Autowired
    private LogisticsPointRepository logisticsPointRepository;

    @Autowired
    private PartnerTransportRepository partnerTransportRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DatabaseSetup("/data/repository/interwarehouse_schedule/interwarehouse_schedules.xml")
    void findAllTest() {
        transactionTemplate.execute(tx -> {
            LogisticsPoint firstPoint = logisticsPointRepository.findById(1L).get();
            LogisticsPoint secondPoint = logisticsPointRepository.findById(2L).get();
            PartnerTransport transport = partnerTransportRepository.findById(2L).get();

                InterwarehouseSchedule firstSchedule = new InterwarehouseSchedule()
                    .setId(1L)
                    .setLogisticsPointFrom(firstPoint)
                    .setLogisticsPointTo(secondPoint)
                    .setDay(1)
                    .setTimeFrom(LocalTime.of(10, 0))
                    .setTimeTo(LocalTime.of(13, 0))
                    .setType(InterwarehouseScheduleType.XDOC_TRANSPORT)
                    .setPallets(5);

                InterwarehouseSchedule secondSchedule = new InterwarehouseSchedule()
                    .setId(2L)
                    .setLogisticsPointFrom(firstPoint)
                    .setLogisticsPointTo(secondPoint)
                    .setDay(5)
                    .setTimeFrom(LocalTime.of(9, 0))
                    .setTimeTo(LocalTime.of(17, 0))
                    .setType(InterwarehouseScheduleType.LINEHAUL)
                    .setTransport(transport)
                    .setPallets(3);

                List<InterwarehouseSchedule> schedules = repository.findAll();

                softly.assertThat(schedules).containsExactlyInAnyOrder(firstSchedule, secondSchedule);
                return null;
            }
        );
    }

    @Test
    @DatabaseSetup(
        value = {
            "/data/repository/interwarehouse_schedule/interwarehouse_schedules.xml",
            "/data/repository/interwarehouse_schedule/additional_schedules.xml"
        }
    )
    void findByPartnerRelation() {
        transactionTemplate.execute(tx -> {
            LogisticsPoint firstPoint = logisticsPointRepository.findById(5L).get();
            LogisticsPoint secondPoint = logisticsPointRepository.findById(6L).get();
            PartnerTransport transport = partnerTransportRepository.findById(2L).get();


            InterwarehouseSchedule firstSchedule = new InterwarehouseSchedule()
                .setId(3L)
                .setLogisticsPointFrom(firstPoint)
                .setLogisticsPointTo(secondPoint)
                .setDay(4)
                .setTimeFrom(LocalTime.of(0, 0))
                .setTimeTo(LocalTime.of(6, 0))
                .setType(InterwarehouseScheduleType.XDOC_TRANSPORT)
                .setTransport(transport)
                    .setPallets(10);

                InterwarehouseSchedule secondSchedule = new InterwarehouseSchedule()
                    .setId(4L)
                    .setLogisticsPointFrom(firstPoint)
                    .setLogisticsPointTo(secondPoint)
                    .setDay(7)
                    .setTimeFrom(LocalTime.of(7, 0))
                    .setTimeTo(LocalTime.of(11, 0))
                    .setType(InterwarehouseScheduleType.XDOC_TRANSPORT)
                    .setPallets(6);

                Set<InterwarehouseSchedule> schedules = repository.getByPartners(5L, 6L);

                softly.assertThat(schedules).containsExactlyInAnyOrder(firstSchedule, secondSchedule);
                return null;
            }
        );
    }
}
