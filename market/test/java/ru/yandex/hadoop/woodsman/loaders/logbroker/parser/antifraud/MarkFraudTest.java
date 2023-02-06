package ru.yandex.hadoop.woodsman.loaders.logbroker.parser.antifraud;

import org.junit.Test;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.fieldmarkers.HasStateAndFilter;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service.MetricsService;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service.partner.PofState;

import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created by oroboros on 21.01.16.
 */
public class MarkFraudTest {
    @Test
    public void mustMarkFraud() {
        HasStateAndFilter record = new HasStateAndFilter() {
            public Integer filter;
            public Integer state;

            @Override
            public Integer getFilter() {
                return filter;
            }

            @Override
            public void setFilter(Integer filter) {
                this.filter = filter;
            }

            @Override
            public Integer getState() {
                return state;
            }

            @Override
            public void setState(Integer state) {
                this.state = state;
            }

            @Override
            public Date getEventtime() {
                return new Date();
            }
        };
        new AbstractDayGapFilter<HasStateAndFilter, Long>(
                mock(MetricsService.class),
                1,
                1,
                1
        ) {
            @Override
            public int getFilterId() {
                return 1;
            }

            @Override
            public boolean isFraud(HasStateAndFilter record) {
                return true;
            }
        }.tryMarkAsFraud(record);

        assertThat(record.getFilter(), is(1));
        assertThat(record.getState(), is(PofState.FLOWFRAUD.state()));
    }
}
