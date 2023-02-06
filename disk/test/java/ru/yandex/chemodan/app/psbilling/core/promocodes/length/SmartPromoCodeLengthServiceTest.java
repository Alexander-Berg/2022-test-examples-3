package ru.yandex.chemodan.app.psbilling.core.promocodes.length;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.chemodan.app.psbilling.core.dao.promocodes.PromoCodeDao;
import ru.yandex.misc.test.Assert;

public class SmartPromoCodeLengthServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private SmartPromoCodeLengthService service;
    @Mock
    private PromoCodeDao promoCodeDao;

    @Before
    public void setUp() throws Exception {
        this.service = new SmartPromoCodeLengthService(promoCodeDao);
    }

    @Test
    public void calcLength() {
        String prefix = "123";
        Mockito.when(promoCodeDao.calculateNumOccupied(Mockito.eq(prefix), Mockito.anyLong())).thenReturn(0L);
        Assert.equals(10L, service.calcLength(prefix, 1L, 10));
    }

    @Test
    public void calcLengthWithOccupied() {
        String prefix = "123";
        Mockito.when(promoCodeDao.calculateNumOccupied(Mockito.eq(prefix), Mockito.anyLong())).thenReturn(Long.MAX_VALUE);
        Assert.equals(19L, service.calcLength(prefix, 1L, 10));
    }

    @Test
    public void calcLengthWithNumAttempt() {
        String prefix = "123";
        Mockito.when(promoCodeDao.calculateNumOccupied(Mockito.eq(prefix), Mockito.anyLong())).thenReturn(0L);
        Assert.assertThrows(() -> service.calcLength(prefix, 1L, 1), IllegalStateException.class);
    }
}
