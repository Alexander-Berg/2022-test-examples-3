package ru.yandex.direct.core.entity.deal.service.validation;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.deal.model.StatusDirect;
import ru.yandex.direct.core.entity.deal.service.DealTransitionsService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

public class DealTransitionsServiceUnavailableStatusesTest {
    private DealTransitionsService dealTransitionsService;

    @Before
    public void before() {
        dealTransitionsService = new DealTransitionsService();
    }

    @Parameterized.Parameter(value = 0)
    public StatusDirect beforeChange;

    @Parameterized.Parameter(value = 1)
    public StatusDirect changeOn;

    @Parameterized.Parameters(name = "ActionType {0}")
    public static Collection<Object> params() {
        return Arrays.asList(new Object[]{StatusDirect.RECEIVED, StatusDirect.RECEIVED},
                new Object[]{StatusDirect.RECEIVED, StatusDirect.ARCHIVED},
                new Object[]{StatusDirect.ACTIVE, StatusDirect.ACTIVE},
                new Object[]{StatusDirect.ACTIVE, StatusDirect.RECEIVED},
                new Object[]{StatusDirect.ACTIVE, StatusDirect.ARCHIVED},
                new Object[]{StatusDirect.COMPLETED, StatusDirect.COMPLETED},
                new Object[]{StatusDirect.COMPLETED, StatusDirect.ACTIVE},
                new Object[]{StatusDirect.COMPLETED, StatusDirect.RECEIVED});
    }

    @Test
    public void unavailableStatuses() {
        assertThat(dealTransitionsService.getAvailableStatuses(beforeChange), not(hasItem(changeOn)));
    }
}
