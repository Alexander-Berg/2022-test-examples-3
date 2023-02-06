package ru.yandex.direct.core.entity.deal.service.validation;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.deal.model.StatusDirect;
import ru.yandex.direct.core.entity.deal.service.DealTransitionsService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

@RunWith(Parameterized.class)
public class DealTransitionsServiceTest {
    private DealTransitionsService dealTransitionsService;

    @Before
    public void before() {
        dealTransitionsService = new DealTransitionsService();
    }

    @Parameterized.Parameter(value = 0)
    public StatusDirect beforeChange;

    @Parameterized.Parameter(value = 1)
    public StatusDirect changeOn;

    @Parameterized.Parameters()
    public static Collection<Object> params() {
        return Arrays.asList(new Object[]{StatusDirect.RECEIVED, StatusDirect.COMPLETED},
                new Object[]{StatusDirect.RECEIVED, StatusDirect.ACTIVE},
                new Object[]{StatusDirect.ACTIVE, StatusDirect.COMPLETED},
                new Object[]{StatusDirect.COMPLETED, StatusDirect.ARCHIVED});
    }

    @Test
    public void availableStatuses() {
        assertThat(dealTransitionsService.getAvailableStatuses(beforeChange), hasItem(changeOn));
    }
}
