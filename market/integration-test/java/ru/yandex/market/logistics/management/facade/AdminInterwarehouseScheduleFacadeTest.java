package ru.yandex.market.logistics.management.facade;

import java.time.LocalTime;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.converter.AdminInterwarehouseScheduleConverter;
import ru.yandex.market.logistics.management.domain.dto.front.enums.FrontDayOfWeek;
import ru.yandex.market.logistics.management.domain.dto.front.schedule.AdminInterwarehouseScheduleType;
import ru.yandex.market.logistics.management.domain.dto.front.schedule.WhWhScheduleDetailDto;
import ru.yandex.market.logistics.management.domain.dto.front.schedule.WhWhScheduleNewDto;
import ru.yandex.market.logistics.management.domain.entity.InterwarehouseSchedule;
import ru.yandex.market.logistics.management.repository.InterwarehouseScheduleRepository;

public class AdminInterwarehouseScheduleFacadeTest extends AbstractContextualTest {

    @Autowired
    private AdminInterwarehouseScheduleFacade facade;

    @Autowired
    private InterwarehouseScheduleRepository repository;

    @Autowired
    private AdminInterwarehouseScheduleConverter converter;

    @Autowired
    private TransactionTemplate template;

    @Test
    @DatabaseSetup("/data/repository/interwarehouse_schedule/interwarehouse_schedules.xml")
    void update() {
        template.execute(tx -> {
            WhWhScheduleDetailDto dto = converter.convert(repository.findById(1L).get(), 1L);
            dto.setPallets(7);
            dto.setTimeFrom(LocalTime.of(11, 0));
            facade.updateSchedule(dto, 1L);

            InterwarehouseSchedule updated = repository.findById(1L).get();

            softly.assertThat(updated.getPallets()).isEqualTo(7);
            softly.assertThat(updated.getTimeFrom()).isEqualTo(LocalTime.of(11, 0));
            return null;
        });
    }

    @Test
    @DatabaseSetup("/data/repository/interwarehouse_schedule/interwarehouse_schedules.xml")
    void updateInvalidInterval() {
        WhWhScheduleDetailDto updated = template.execute(tx -> {
            WhWhScheduleDetailDto dto = converter.convert(repository.findById(1L).get(), 1L);
            dto.setTimeFrom(LocalTime.of(15, 0));

            return dto;
        });
        softly.assertThatThrownBy(() -> facade.updateSchedule(updated, 1L));
    }

    @Test
    @DatabaseSetup("/data/repository/interwarehouse_schedule/interwarehouse_schedules.xml")
    void delete() {
        facade.deleteSchedules(Set.of(1L, 2L));
        softly.assertThat(repository.findAll()).isEmpty();
    }

    @Test
    @DatabaseSetup("/data/repository/interwarehouse_schedule/interwarehouse_schedules.xml")
    @ExpectedDatabase(
        value = "/data/repository/interwarehouse_schedule/after/after_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void create() {
        WhWhScheduleNewDto dto = new WhWhScheduleNewDto()
            .setDay(FrontDayOfWeek.FRIDAY)
            .setInterval("09:00-18:00")
            .setPallets(10)
            .setPointFrom(1L)
            .setType(AdminInterwarehouseScheduleType.XDOC_TRANSPORT)
            .setPointTo(2L);

        facade.createSchedule(dto, 1L);
    }

    @Test
    @DisplayName("Partners are not owners of logistics points")
    @DatabaseSetup("/data/repository/interwarehouse_schedule/interwarehouse_schedules.xml")
    void createInvalidLogisticsPoint() {
        WhWhScheduleNewDto dto = new WhWhScheduleNewDto()
            .setDay(FrontDayOfWeek.FRIDAY)
            .setInterval("09:00-18:00")
            .setPallets(10)
            .setPointFrom(2L)
            .setType(AdminInterwarehouseScheduleType.XDOC_TRANSPORT)
            .setPointTo(1L);

        softly.assertThatThrownBy(() -> facade.createSchedule(dto, 1L));
    }

}
