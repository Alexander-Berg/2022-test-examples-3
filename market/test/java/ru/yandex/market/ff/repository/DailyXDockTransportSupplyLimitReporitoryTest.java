package ru.yandex.market.ff.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.DailyLimit;
import ru.yandex.market.ff.model.entity.DailyLimitIdentifier;
import ru.yandex.market.ff.model.entity.DailyXDockTransportSupplyLimit;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static java.util.Arrays.asList;

public class DailyXDockTransportSupplyLimitReporitoryTest extends IntegrationTest {
    private static final DailyXDockTransportSupplyLimit DAILY_SUPPLY_LIMIT_1 =
        new DailyXDockTransportSupplyLimit(145L, LocalDate.of(2019, 1, 2), 1300L, 40L);

    private static final DailyXDockTransportSupplyLimit DAILY_SUPPLY_LIMIT_2 =
        new DailyXDockTransportSupplyLimit(145L, LocalDate.of(2019, 1, 3), 1400L, 50L);

    private static final DailyXDockTransportSupplyLimit DAILY_SUPPLY_LIMIT_3 =
        new DailyXDockTransportSupplyLimit(145L, LocalDate.of(2019, 1, 4), null, 50L);

    private static final DailyXDockTransportSupplyLimit DAILY_SUPPLY_LIMIT_4 =
        new DailyXDockTransportSupplyLimit(145L, LocalDate.of(2019, 1, 5), 1400L, null);

    @Autowired
    private DailyXDockTransportSupplyLimitRepository dailyXDockTransportSupplyLimitRepository;

    @Test
    @DatabaseSetup("classpath:repository/xdock-transport-supply-limits/before.xml")
    @ExpectedDatabase(
        value = "classpath:repository/xdock-transport-supply-limits/after_save.xml",
        assertionMode = NON_STRICT
    )
    void saveLimits() {
        dailyXDockTransportSupplyLimitRepository.save(DAILY_SUPPLY_LIMIT_2);
        dailyXDockTransportSupplyLimitRepository.save(DAILY_SUPPLY_LIMIT_3);
        dailyXDockTransportSupplyLimitRepository.save(DAILY_SUPPLY_LIMIT_4);
    }

    @Test
    @DatabaseSetup("classpath:repository/xdock-transport-supply-limits/before.xml")
    @ExpectedDatabase(
        value = "classpath:repository/xdock-transport-supply-limits/after_delete.xml",
        assertionMode = NON_STRICT
    )
    void deleteLimits() {
        dailyXDockTransportSupplyLimitRepository.deleteByDailyLimitIdentifier(
            DAILY_SUPPLY_LIMIT_1.getDailyLimitIdentifier()
        );
        dailyXDockTransportSupplyLimitRepository.deleteByDailyLimitIdentifier(
            DAILY_SUPPLY_LIMIT_2.getDailyLimitIdentifier()
        );
    }

    @Test
    @DatabaseSetup("classpath:repository/xdock-transport-supply-limits/before-find-for-all-dates.xml")
    @ExpectedDatabase(value = "classpath:repository/xdock-transport-supply-limits/before-find-for-all-dates.xml",
        assertionMode = NON_STRICT)
    public void findForAllDatesWorksCorrect() {
        LocalDate firstDate = LocalDate.of(2019, 1, 1);
        LocalDate secondDate = LocalDate.of(2019, 1, 2);
        LocalDate thirdDate = LocalDate.of(2019, 1, 4);
        List<DailyXDockTransportSupplyLimit> limitsForDates = dailyXDockTransportSupplyLimitRepository
            .findForAllDates(SupplierType.FIRST_PARTY, 145, asList(firstDate, secondDate, thirdDate));
        assertions.assertThat(limitsForDates.size()).isEqualTo(2);
        Set<LocalDate> dates = limitsForDates.stream()
            .map(DailyLimit::getDailyLimitIdentifier)
            .map(DailyLimitIdentifier::getDate)
            .collect(Collectors.toSet());
        assertions.assertThat(dates).containsAll(asList(firstDate, secondDate));
    }
}
