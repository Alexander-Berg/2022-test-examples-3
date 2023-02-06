package ru.yandex.market.core.cutoff;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.framework.context.AbstractUniContextHelper;
import ru.yandex.market.mbi.lock.LockService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author wadim
 */
class DbCutoffServiceTest {
    private final DbCutoffService dbCutoffService = new DbCutoffService();
    private LockService lockService;
    private AbstractUniContextHelper uniContextHelper;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @BeforeEach
    void setUp() {
        lockService = mock(LockService.class);
        uniContextHelper = mock(AbstractUniContextHelper.class);
        namedParameterJdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        dbCutoffService.setLockService(lockService);
        dbCutoffService.setCpaUniContextHelper(uniContextHelper);
        dbCutoffService.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
    }

    @Test
    void testOpenCutoff() {
        CutoffInfo ci = new CutoffInfo(1, 2, CutoffType.CPA_CPC, null, null);
        when(uniContextHelper.calculate(any(), isNull())).thenReturn(List.<CutoffInfo>of(), ci);

        CutoffInfo cutoffInfo = dbCutoffService.openCutoff(1L, CutoffType.CPA_CPC, 2L, "comment");

        assertThat(cutoffInfo).isSameAs(ci);
        verify(lockService).lock("cutoff_1");
        verify(uniContextHelper, times(2)).getSandboxMode();
        verify(uniContextHelper, times(2)).calculate(any(), isNull());
        verifyNoMoreInteractions(lockService, uniContextHelper);
    }
}
