package ru.yandex.market.mbo.db;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.common.util.db.IdGenerator;
import ru.yandex.market.mbo.gwt.models.params.Unit;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by annaalkh on 05.07.17.
 */
@RunWith(MockitoJUnitRunner.class)
public class MeasureServiceTest {

    @Mock
    NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    IdGenerator idGenerator;

    @Mock
    TransactionTemplate transactionTemplate;

    private MeasureService measureService;

    @Before
    public void setUp() {
        measureService = new MeasureService(jdbcTemplate, idGenerator, transactionTemplate);
    }

    @Test
    public void getAllUnits() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                RowCallbackHandler callback = ((RowCallbackHandler) invocation.getArgument(1));
                ResultSet rs1 = getMockResultSet();
                callback.processRow(rs1);
                return null;
            }
        }).when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class));

        List<Unit> resultUnits = measureService.getAllUnitsWithoutMeasures();
        assertEquals(1, resultUnits.size());
        assertEquals(1, resultUnits.get(0).getAliases().size());
        assertEquals(1, resultUnits.get(0).getMeasureId());
    }


    private ResultSet getMockResultSet() throws Throwable {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(anyString())).thenReturn(1L);
        when(rs.getInt(anyString())).thenReturn(1);
        when(rs.getBigDecimal(anyString())).thenReturn(BigDecimal.ONE);
        when(rs.getBoolean(anyString())).thenReturn(true);
        when(rs.getString(anyString())).thenReturn("Test");
        return rs;
    }
}
