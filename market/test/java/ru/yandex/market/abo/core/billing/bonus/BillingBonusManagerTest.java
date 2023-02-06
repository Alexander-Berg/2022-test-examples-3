package ru.yandex.market.abo.core.billing.bonus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.assessor.AssessorService;

import static java.time.Month.APRIL;
import static java.time.Month.MARCH;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 * @date 07.06.18.
 */
public class BillingBonusManagerTest {

    private static final int YEAR = 2018;

    @InjectMocks
    private BillingBonusManager billingBonusManager;

    @Mock
    private BillingBonusService billingBonusService;

    @Mock
    private AssessorService assessorService;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testLoadBonusesByUserAndMonth() {
        when(billingBonusService.load(1L, YEAR, APRIL)).thenReturn(Collections.singletonList(
                new BillingBonus(1L, YEAR, APRIL, 10)
        ));

        List<BillingBonus> bonuses = billingBonusManager.loadBonuses(1L, YEAR, APRIL);
        assertEquals(1, bonuses.size());
        verify(assessorService, never()).loadBilledAssessors();
    }

    @Test
    public void testLoadBonusesByUserAndYear() {
        when(billingBonusService.load(1L, 2018, null)).thenReturn(Arrays.asList(
                new BillingBonus(1L, YEAR, APRIL, 10),
                new BillingBonus(1L, YEAR, MARCH, 5)
        ));

        List<BillingBonus> bonuses = billingBonusManager.loadBonuses(1L, 2018, null);
        assertEquals(2, bonuses.size());
        verify(assessorService, never()).loadBilledAssessors();
    }

    @Test
    public void testLoadBonusesByMonth() {
        when(billingBonusService.load(null, YEAR, APRIL)).thenReturn(new ArrayList<>(Collections.singletonList(
                new BillingBonus(1L, YEAR, APRIL, 10)
        )));
        when(assessorService.loadBilledAssessors()).thenReturn(Arrays.asList(1L, 2L, 3L));


        List<BillingBonus> bonuses = billingBonusManager.loadBonuses(null, YEAR, APRIL);

        verify(assessorService, times(1)).loadBilledAssessors();
        assertEquals(3, bonuses.size());
        assertEquals(Arrays.asList(1L, 2L, 3L), bonuses.stream().map(BillingBonus::getUserId).collect(toList()));

        assertEquals(new BillingBonus(1L, YEAR, APRIL, 10), bonuses.get(0));
        assertEquals(new BillingBonus(2L, YEAR, APRIL, 0), bonuses.get(1));
        assertEquals(new BillingBonus(3L, YEAR, APRIL, 0), bonuses.get(2));
    }
}
