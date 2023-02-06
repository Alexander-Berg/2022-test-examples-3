package ru.yandex.market.abo.gen;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.market.abo.core.calendar.db.CalendarService;
import ru.yandex.market.abo.core.exception.ExceptionalShopsService;
import ru.yandex.market.abo.core.partner.placement.PartnerPlacementInfoService;
import ru.yandex.market.abo.gen.model.GenId;
import ru.yandex.market.abo.gen.model.GeneratorProfile;
import ru.yandex.market.abo.gen.model.Hypothesis;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.gen.GeneratorManager.ALL_REGIONS_PARAM;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.DROPSHIP_BY_SELLER;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 * @date 07.08.2007
 * Time: 22:10:47
 */
public class GeneratorManagerTest {
    private static final long SHOP_ID = 123;

    @InjectMocks
    private GeneratorManager generatorManager;

    @Mock
    private HypothesisService hypothesisService;
    @Mock
    private GeneratorProfile generatorProfile;
    @Mock
    private HypothesisGenerator hypothesisGenerator;
    @Mock
    private Hypothesis hypothesis;
    @Mock
    private ExceptionalShopsService exceptionalShopsService;
    @Mock
    private RegionService regionService;
    @Mock
    private PartnerPlacementInfoService partnerPlacementInfoService;
    @Mock
    private CalendarService calendarService;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(calendarService.todayIsHolidayOrWeekend()).thenReturn(false);
        when(partnerPlacementInfoService.loadPartners(DROPSHIP_BY_SELLER)).thenReturn(Set.of());
        when(hypothesisService.fromProfile(any(), eq(HypothesisGenerator.class))).thenReturn(hypothesisGenerator);
        when(hypothesisGenerator.generate()).thenReturn(Collections.singletonList(hypothesis));
        when(generatorProfile.getLimit()).thenReturn(10);
        when(generatorProfile.getBooleanValue(ALL_REGIONS_PARAM, false)).thenReturn(true);
        when(hypothesis.getShopId()).thenReturn(SHOP_ID);
    }

    @Test
    public void processFine() {
        generatorManager.process(generatorProfile);
        verify(hypothesisService).createHypothesis(Collections.singletonList(hypothesis));
    }

    @Test
    public void processLimit() {
        when(hypothesisGenerator.generate()).thenReturn(Arrays.asList(hypothesis, hypothesis));
        when(generatorProfile.getLimit()).thenReturn(1);
        generatorManager.process(generatorProfile);
        verify(hypothesisService).createHypothesis(Collections.singletonList(hypothesis));
    }

    @Test
    public void holiday() {
        when(calendarService.todayIsHolidayOrWeekend()).thenReturn(true);
        generatorManager.process(generatorProfile);
        verify(hypothesisService, never()).createHypothesis(anyList());
    }

    @Test
    public void holiday24x7Gen() {
        when(calendarService.todayIsHolidayOrWeekend()).thenReturn(true);
        when(generatorProfile.getId()).thenReturn(GenId.BY_URL);
        generatorManager.process(generatorProfile);
        verify(hypothesisService).createHypothesis(List.of(hypothesis));
    }

    @Test
    public void dsbsShop() {
        when(partnerPlacementInfoService.loadPartners(DROPSHIP_BY_SELLER)).thenReturn(Set.of(SHOP_ID));
        generatorManager.process(generatorProfile);
        verify(hypothesisService).createHypothesis(List.of());
    }

    @Test
    public void dsbsShopGeneratorException() {
        when(partnerPlacementInfoService.loadPartners(DROPSHIP_BY_SELLER)).thenReturn(Set.of(SHOP_ID));
        when(generatorProfile.getId()).thenReturn(GenId.DSBS_CANCELLED_ORDER);
        generatorManager.process(generatorProfile);
        verify(hypothesisService).createHypothesis(List.of(hypothesis));
    }
}
