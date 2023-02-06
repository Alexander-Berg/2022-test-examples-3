package ru.yandex.market.billing.tool;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.msapi.clicks.Binder;

import static org.mockito.Mockito.when;

/**
 * @author snoop
 */
@RunWith(MockitoJUnitRunner.class)
public class AgedDateBinderTest {

    private static final int DAYS_LIMIT = 90;

    @Mock
    private Binder<Date> delegate;
    @Mock
    private EnvironmentService environmentService;

    private AgedDateBinder binder;

    @Before
    public void setUp() {
        final String paramName = "paramName";
        binder = new AgedDateBinder(delegate, environmentService, paramName);
        when(environmentService.getIntValue(paramName)).thenReturn(DAYS_LIMIT);
        binder.init();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void now_after_boundary() {
        check(this::now);
    }

    @Test
    public void one_hour_after_boundary() {
        check(() -> now().minusDays(DAYS_LIMIT).plusHours(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void one_hour_before_boundary() {
        check(() -> now().minusDays(DAYS_LIMIT).minusDays(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void far_away_from_boundary() {
        check(() -> now().minusDays(DAYS_LIMIT * 2));
    }

    private void check(Supplier<ZonedDateTime> supplier) {
        binder.checkAfter(Date.from(supplier.get().toInstant()));
    }

    private ZonedDateTime now() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault());
    }

}