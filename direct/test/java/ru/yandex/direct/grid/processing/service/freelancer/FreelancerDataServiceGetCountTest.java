package ru.yandex.direct.grid.processing.service.freelancer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.freelancer.container.FreelancersQueryFilter;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerService;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FreelancerDataServiceGetCountTest {

    @Mock
    FreelancerService freelancerService;
    @InjectMocks
    FreelancerDataService freelancerDataService;

    @Captor
    private ArgumentCaptor<FreelancersQueryFilter> filterCaptor;
    @Captor
    private ArgumentCaptor<LimitOffset> limitOffsetCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getFreelancersCount_callCoreService() {
        when(freelancerService.getFreelancers(any(FreelancersQueryFilter.class), any(LimitOffset.class)))
                .thenReturn(emptyList());

        int actual = freelancerDataService.getFreelancersCount();

        int expected = 0;
        assertThat(actual).isEqualTo(expected);
        verify(freelancerService).getFreelancers(filterCaptor.capture(), limitOffsetCaptor.capture());
        assertThat(filterCaptor.getValue()).isEqualToComparingFieldByField(FreelancersQueryFilter.activeFreelancers());
        assertThat(limitOffsetCaptor.getValue()).isEqualTo(LimitOffset.maxLimited());
    }

}
