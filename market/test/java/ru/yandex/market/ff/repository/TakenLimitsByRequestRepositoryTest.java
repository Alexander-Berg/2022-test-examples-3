package ru.yandex.market.ff.repository;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.DailyLimitsType;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.bo.TakenSupplyLimits;
import ru.yandex.market.ff.model.bo.TakenSupplyLimitsForDate;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.TakenLimitsByRequest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.core.supplier.model.SupplierType.FIRST_PARTY;

public class TakenLimitsByRequestRepositoryTest extends IntegrationTest {

    private static final Set<RequestType> REQUEST_TYPES = EnumSet.of(RequestType.SUPPLY, RequestType.SHADOW_SUPPLY);
    private static final Set<DailyLimitsType> LIMITS_TYPES = EnumSet.of(DailyLimitsType.SUPPLY);

    @Autowired
    private TakenLimitsByRequestRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/taken-limits-by-request/before.xml")
    @ExpectedDatabase(value = "classpath:repository/taken-limits-by-request/before.xml", assertionMode = NON_STRICT)
    public void getTakenLimitsWhenItExistsTest() {
        LocalDate date = LocalDate.of(2019, 11, 11);
        TakenSupplyLimits takenLimits =
                repository.getTakenLimits(100, null, date, FIRST_PARTY, REQUEST_TYPES, LIMITS_TYPES);
        assertions.assertThat(takenLimits.getTakenItems()).isEqualTo(205);
        assertions.assertThat(takenLimits.getTakenPallets()).isEqualTo(13);
    }

    @Test
    @DatabaseSetup("classpath:repository/taken-limits-by-request/before.xml")
    @ExpectedDatabase(value = "classpath:repository/taken-limits-by-request/before.xml", assertionMode = NON_STRICT)
    public void getTakenLimitsWhenItNotExists() {
        LocalDate date = LocalDate.of(2019, 10, 11);
        TakenSupplyLimits takenLimits =
                repository.getTakenLimits(100, null, date, FIRST_PARTY, REQUEST_TYPES, LIMITS_TYPES);
        assertions.assertThat(takenLimits.getTakenItems()).isEqualTo(0);
        assertions.assertThat(takenLimits.getTakenPallets()).isEqualTo(0);
    }

    @Test
    @DatabaseSetup("classpath:repository/taken-limits-by-request/before-with-requested-date-at-midnight.xml")
    @ExpectedDatabase(value = "classpath:repository/taken-limits-by-request/before-with-requested-date-at-midnight.xml",
            assertionMode = NON_STRICT)
    public void getTakenLimitsWhenRequestAtMidnight() {
        LocalDate date = LocalDate.of(2019, 11, 10);
        TakenSupplyLimits takenLimits =
                repository.getTakenLimits(100, null, date, FIRST_PARTY, REQUEST_TYPES, LIMITS_TYPES);
        assertions.assertThat(takenLimits.getTakenItems()).isEqualTo(0);
        assertions.assertThat(takenLimits.getTakenPallets()).isEqualTo(0);

        date = LocalDate.of(2019, 11, 11);
        takenLimits =
                repository.getTakenLimits(100, null, date, FIRST_PARTY, REQUEST_TYPES, LIMITS_TYPES);
        assertions.assertThat(takenLimits.getTakenItems()).isEqualTo(5);
        assertions.assertThat(takenLimits.getTakenPallets()).isEqualTo(2);
    }

    @Test
    @DatabaseSetup("classpath:repository/taken-limits-by-request/before-xdoc-request.xml")
    @ExpectedDatabase(value = "classpath:repository/taken-limits-by-request/before-xdoc-request.xml",
            assertionMode = NON_STRICT)
    public void getTakenLimitsWhenRequestOnXDocTest() {
        LocalDate date = LocalDate.of(2019, 11, 11);
        TakenSupplyLimits takenLimits =
                repository.getTakenLimits(100, null, date, FIRST_PARTY, REQUEST_TYPES, LIMITS_TYPES);
        assertions.assertThat(takenLimits.getTakenItems()).isEqualTo(205);
        assertions.assertThat(takenLimits.getTakenPallets()).isEqualTo(13);
    }

    @Test
    @DatabaseSetup("classpath:repository/taken-limits-by-request/before.xml")
    @ExpectedDatabase(value = "classpath:repository/taken-limits-by-request/before.xml", assertionMode = NON_STRICT)
    public void getTakenLimitsForDateWhenItExistsTest() {
        LocalDate from = LocalDate.of(2019, 11, 11);
        LocalDate to = from.plusDays(1);
        List<TakenSupplyLimitsForDate> takenLimitsForDate =
                repository.getTakenLimitsForDates(100, null, List.of(from, to),
                        FIRST_PARTY, REQUEST_TYPES, LIMITS_TYPES);
        assertions.assertThat(takenLimitsForDate.size()).isEqualTo(3);
        TakenSupplyLimitsForDate firstLimit =
                new TakenSupplyLimitsForDate(0L, 5L, 2L, LocalDate.of(2019, 11, 11));
        TakenSupplyLimitsForDate secondLimit =
                new TakenSupplyLimitsForDate(0L, 200L, 11L, LocalDate.of(2019, 11, 11));
        TakenSupplyLimitsForDate thirdLimit =
                new TakenSupplyLimitsForDate(0L, 1100L, 12L, LocalDate.of(2019, 11, 12));
        assertions.assertThat(takenLimitsForDate).containsExactlyInAnyOrder(firstLimit, secondLimit, thirdLimit);
    }

    @Test
    @DatabaseSetup("classpath:repository/taken-limits-by-request/before.xml")
    @ExpectedDatabase(value = "classpath:repository/taken-limits-by-request/before.xml", assertionMode = NON_STRICT)
    public void getTakenLimitsForDateWhenItNotExists() {
        LocalDate date = LocalDate.of(2019, 10, 11);
        List<TakenSupplyLimitsForDate> takenLimitsForDate =
                repository.getTakenLimitsForDates(100, null, List.of(date), FIRST_PARTY, REQUEST_TYPES, LIMITS_TYPES);
        assertions.assertThat(takenLimitsForDate.size()).isEqualTo(0);
    }

    @Test
    @DatabaseSetup("classpath:repository/taken-limits-by-request/before-with-requested-date-at-midnight.xml")
    @ExpectedDatabase(value = "classpath:repository/taken-limits-by-request/before-with-requested-date-at-midnight.xml",
            assertionMode = NON_STRICT)
    public void getTakenLimitsForDateWhenRequestedDateIsMidnight() {
        LocalDate date = LocalDate.of(2019, 11, 10);
        List<TakenSupplyLimitsForDate> takenLimitsForDate =
                repository.getTakenLimitsForDates(100, null, List.of(date),
                        FIRST_PARTY, REQUEST_TYPES, LIMITS_TYPES);
        assertions.assertThat(takenLimitsForDate.size()).isEqualTo(0);

        date = LocalDate.of(2019, 11, 11);
        takenLimitsForDate =
                repository.getTakenLimitsForDates(100, null, List.of(date),
                        FIRST_PARTY, REQUEST_TYPES, LIMITS_TYPES);
        TakenSupplyLimitsForDate limit =
                new TakenSupplyLimitsForDate(0L, 5L, 2L, LocalDate.of(2019, 11, 11));
        assertions.assertThat(takenLimitsForDate.size()).isEqualTo(1);
        assertions.assertThat(takenLimitsForDate).containsExactly(limit);
    }

    @Test
    @DatabaseSetup("classpath:repository/taken-limits-by-request/before.xml")
    @ExpectedDatabase(value = "classpath:repository/taken-limits-by-request/before.xml", assertionMode = NON_STRICT)
    public void findByRequestIdWorksCorrect() {
        LocalDate date = LocalDate.of(2019, 11, 11);
        Optional<TakenLimitsByRequest> maybeLimits = repository.findByRequestIdAndLimitDateInRequests(1, date);
        assertions.assertThat(maybeLimits).isPresent();
        TakenLimitsByRequest limits = maybeLimits.get();
        assertions.assertThat(limits.getSupplierType()).isEqualTo(FIRST_PARTY);
        assertions.assertThat(limits.getTakenItems()).isEqualTo(5);
        assertions.assertThat(limits.getTakenPallets()).isEqualTo(2);

        maybeLimits = repository.findByRequestIdAndLimitDateInRequests(100, date);
        assertions.assertThat(maybeLimits).isNotPresent();
    }

    @Test
    @DatabaseSetup("classpath:repository/taken-limits-by-request/before.xml")
    @ExpectedDatabase(value = "classpath:repository/taken-limits-by-request/after-delete.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteByRequestIdWorksCorrect() {
        repository.deleteByRequestId(1);
    }

    @Test
    @DatabaseSetup("classpath:repository/taken-limits-by-request/before.xml")
    @ExpectedDatabase(value = "classpath:repository/taken-limits-by-request/before.xml", assertionMode = NON_STRICT)
    public void addLimitOnExistingDateIsProhibited() {
        LocalDate limitDate = LocalDate.of(2019, 11, 11);
        TakenLimitsByRequest limit = getTakenLimitsByRequest(limitDate);
        assertions.assertThatThrownBy(() -> repository.save(limit))
                .hasCauseInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DatabaseSetup("classpath:repository/taken-limits-by-request/before.xml")
    @ExpectedDatabase(value = "classpath:repository/taken-limits-by-request/after-add-new.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void addLimitOnNonExistingDateIsSuccessful() {
        LocalDate limitDate = LocalDate.of(2019, 11, 10);
        TakenLimitsByRequest limit = getTakenLimitsByRequest(limitDate);
        var savedLimit = repository.save(limit);

        assertions.assertThat(savedLimit).isEqualTo(limit);
    }

    @NotNull
    private TakenLimitsByRequest getTakenLimitsByRequest(LocalDate limitDate) {
        TakenLimitsByRequest limit = new TakenLimitsByRequest();
        limit.setSupplierType(FIRST_PARTY);
        limit.setTakenItems(10L);
        limit.setTakenPallets(100L);
        limit.setShopRequest(getShopRequest());
        limit.setLimitDate(limitDate);
        return limit;
    }

    private ShopRequest getShopRequest() {
        var shopRequest = new ShopRequest();
        shopRequest.setId(1L);
        return shopRequest;
    }
}
