package ru.yandex.market.ff.repository;

import java.time.LocalDate;
import java.util.Collections;
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
import ru.yandex.market.ff.model.entity.DailySupplyLimit;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.util.Arrays.asList;

/**
 * @author avetokhin 15.02.19.
 */
class DailySupplyLimitRepositoryTest extends IntegrationTest {

    private static final DailySupplyLimit DAILY_SUPPLY_LIMIT_1 = dailySupplyLimit(
            new DailyLimitIdentifier(145, LocalDate.of(2019, 1, 2), SupplierType.THIRD_PARTY), 1300L, 40L);

    private static final DailySupplyLimit DAILY_SUPPLY_LIMIT_2 = dailySupplyLimit(
            new DailyLimitIdentifier(145, LocalDate.of(2019, 1, 3), SupplierType.THIRD_PARTY), 1400L, 50L);

    private static final DailySupplyLimit DAILY_SUPPLY_LIMIT_3 = dailySupplyLimit(
            new DailyLimitIdentifier(145, LocalDate.of(2019, 1, 4), SupplierType.THIRD_PARTY), null, 50L);

    private static final DailySupplyLimit DAILY_SUPPLY_LIMIT_4 = dailySupplyLimit(
            new DailyLimitIdentifier(145, LocalDate.of(2019, 1, 5), SupplierType.FIRST_PARTY), 1400L, null);

    @Autowired
    private DailySupplyLimitRepository dailySupplyLimitRepository;

    private static DailySupplyLimit dailySupplyLimit(DailyLimitIdentifier identifier, Long items, Long pallets) {
        DailySupplyLimit limit = new DailySupplyLimit();
        limit.setDailyLimitIdentifier(identifier);
        limit.setItemsCount(items);
        limit.setPalletsCount(pallets);
        return limit;
    }

    @Test
    @DatabaseSetup("classpath:repository/limit/before.xml")
    @ExpectedDatabase(value = "classpath:repository/limit/after_save.xml", assertionMode = NON_STRICT_UNORDERED)
    void saveLimits() {
        dailySupplyLimitRepository.save(DAILY_SUPPLY_LIMIT_2);
        dailySupplyLimitRepository.save(DAILY_SUPPLY_LIMIT_3);
        dailySupplyLimitRepository.save(DAILY_SUPPLY_LIMIT_4);
    }

    @Test
    @DatabaseSetup("classpath:repository/limit/before.xml")
    @ExpectedDatabase(value = "classpath:repository/limit/after_delete.xml", assertionMode = NON_STRICT)
    void deleteLimits() {
        dailySupplyLimitRepository.deleteByDailyLimitIdentifierAndDestinationServiceId(
                DAILY_SUPPLY_LIMIT_1.getDailyLimitIdentifier(), DAILY_SUPPLY_LIMIT_1.getDestinationServiceId()
        );
        dailySupplyLimitRepository.deleteByDailyLimitIdentifierAndDestinationServiceId(
                DAILY_SUPPLY_LIMIT_2.getDailyLimitIdentifier(), DAILY_SUPPLY_LIMIT_2.getDestinationServiceId()
        );
        dailySupplyLimitRepository.deleteByDailyLimitIdentifierAndDestinationServiceId(
                DAILY_SUPPLY_LIMIT_1.getDailyLimitIdentifier(), 171L
        );
    }

    @Test
    @DatabaseSetup("classpath:repository/limit/before-find-for-all-dates.xml")
    @ExpectedDatabase(value = "classpath:repository/limit/before-find-for-all-dates.xml",
        assertionMode = NON_STRICT)
    public void findForAllDatesWorksCorrect() {
        LocalDate firstDate = LocalDate.of(2019, 1, 1);
        LocalDate secondDate = LocalDate.of(2019, 1, 2);
        LocalDate thirdDate = LocalDate.of(2019, 1, 4);
        List<DailySupplyLimit> limitsForDates = dailySupplyLimitRepository
            .findForAllDates(SupplierType.THIRD_PARTY, 145, null, asList(firstDate, secondDate, thirdDate));
        assertions.assertThat(limitsForDates.size()).isEqualTo(2);
        Set<LocalDate> dates = limitsForDates.stream()
            .map(DailyLimit::getDailyLimitIdentifier)
            .map(DailyLimitIdentifier::getDate)
            .collect(Collectors.toSet());
        assertions.assertThat(dates).containsAll(asList(firstDate, secondDate));
    }


    @Test
    @DatabaseSetup("classpath:repository/limit/before.xml")
    void
    findAllByDailyLimitIdentifierInTest() {
        List<DailySupplyLimit> supplyLimits = dailySupplyLimitRepository.findAllByDailyLimitIdentifierIn(
                List.of(
                        DAILY_SUPPLY_LIMIT_1.getDailyLimitIdentifier(),
                        DAILY_SUPPLY_LIMIT_2.getDailyLimitIdentifier()
                ));

        assertions.assertThat(supplyLimits.size()).isEqualTo(2);
    }

    @Test
    @DatabaseSetup("classpath:repository/limit/" +
            "before-find-for-all-dates-with-destination-service.xml")
    public void findForAllDatesWithDestinationServiceIdWorksCorrect() {
        LocalDate firstDate = LocalDate.of(2019, 1, 1);
        LocalDate secondDate = LocalDate.of(2019, 1, 2);
        LocalDate thirdDate = LocalDate.of(2019, 1, 3);
        LocalDate fourthDate = LocalDate.of(2019, 1, 4);
        List<LocalDate> requestDate = List.of(firstDate, secondDate, thirdDate, fourthDate);
        List<DailySupplyLimit> limitsForDates = dailySupplyLimitRepository
                .findForAllDates(SupplierType.FIRST_PARTY, 145, 172L, requestDate);
        assertions.assertThat(limitsForDates.size()).isEqualTo(2);
        Set<LocalDate> dates = limitsForDates.stream()
                .map(DailyLimit::getDailyLimitIdentifier)
                .map(DailyLimitIdentifier::getDate)
                .collect(Collectors.toSet());
        assertions.assertThat(dates).containsAll(asList(firstDate, fourthDate));
    }

    @Test
    @DatabaseSetup("classpath:repository/limit/" +
            "before-find-for-all-dates-with-destination-service-2.xml")
    public void findForAllDatesWithDestinationServiceIdWhenNullWorksCorrect() {
        LocalDate thirdDate = LocalDate.of(2019, 1, 3);

        List<LocalDate> requestDate = List.of(thirdDate);
        List<DailySupplyLimit> limitsForDates = dailySupplyLimitRepository
                .findForAllDates(SupplierType.FIRST_PARTY, 145, null, requestDate);
        assertions.assertThat(limitsForDates.size()).isEqualTo(1);
        Set<LocalDate> dates = limitsForDates.stream()
                .map(DailyLimit::getDailyLimitIdentifier)
                .map(DailyLimitIdentifier::getDate)
                .collect(Collectors.toSet());
        assertions.assertThat(dates).containsAll(Collections.singletonList(thirdDate));
    }

    @Test
    @DatabaseSetup("classpath:repository/limit/" +
            "before-find-for-all-dates-with-destination-service-2.xml")
    public void findForAllDatesWithDestinationServiceIdWhenNotNullWorksCorrect() {
        LocalDate thirdDate = LocalDate.of(2019, 1, 3);

        List<LocalDate> requestDate = List.of(thirdDate);
        List<DailySupplyLimit> limitsForDates = dailySupplyLimitRepository
                .findForAllDates(SupplierType.FIRST_PARTY, 145, 173L, requestDate);
        assertions.assertThat(limitsForDates.size()).isEqualTo(1);
        Set<LocalDate> dates = limitsForDates.stream()
                .map(DailyLimit::getDailyLimitIdentifier)
                .map(DailyLimitIdentifier::getDate)
                .collect(Collectors.toSet());
        assertions.assertThat(dates).containsAll(Collections.singletonList(thirdDate));
    }

}
