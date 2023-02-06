package ru.yandex.market.abo.core.premod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.no_placement.NoPlacementManager;
import ru.yandex.market.abo.core.premod.helper.ShopDataItemHelper;
import ru.yandex.market.abo.core.premod.model.PremodCheckType;
import ru.yandex.market.abo.core.premod.model.PremodItem;
import ru.yandex.market.abo.core.premod.model.PremodItemStatus;
import ru.yandex.market.abo.core.premod.model.PremodItemType;
import ru.yandex.market.abo.core.premod.model.PremodProblem;
import ru.yandex.market.abo.core.premod.model.PremodProblemTypeId;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.premod.model.PremodTicketStatus;
import ru.yandex.market.core.abo.AboCutoff;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author komarovns
 * @date 24.07.2020
 */
public class PremodProblemManagerTest {
    private static final long TICKET_ID = 1;
    private static final long SHOP_ID = 2;
    private static final long USER_ID = 3;
    private static final long ITEM_ID = 101;
    private static final int PROBLEM_TYPE = 1337;

    @InjectMocks
    private PremodProblemManager premodProblemManager;
    @Mock
    private PremodManager premodManager;
    @Mock
    private PremodTicketService premodTicketService;
    @Mock
    private PremodProblemService premodProblemService;
    @Mock
    private PremodItemService premodItemService;
    @Mock
    private ShopDataItemHelper shopInfoItemHelper;
    @Mock
    private NoPlacementManager noPlacementManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        PremodTicket premodTicket = mock(PremodTicket.class);
        when(premodTicket.getId()).thenReturn(TICKET_ID);
        when(premodTicket.getStatus()).thenReturn(PremodTicketStatus.NEW);
        when(premodTicket.getShopId()).thenReturn(SHOP_ID);
        when(premodTicket.getTestingType()).thenReturn(PremodCheckType.CPC_PREMODERATION);
        when(premodTicketService.loadPremodTicket(TICKET_ID)).thenReturn(premodTicket);
        when(premodTicketService.loadPremodTicketByItemId(ITEM_ID)).thenReturn(premodTicket);
    }

    @Test
    void addPremodProblemsTest_setStatus_FAILED() {
        List<PremodProblem> premodProblems = Arrays.asList(
                problem(PremodProblemTypeId.OGRN_REQUEST),
                problem(PremodProblemTypeId.REQUIRED_DOCUMENTS),
                problem(PremodProblemTypeId.NO_OFFERS_IN_PRICE_LIST)
        );
        var item = item(PremodItemType.OTHER);
        when(premodProblemService.loadPremodProblemsByItem(item.getId())).thenReturn(premodProblems);
        premodProblemManager.addPremodProblems(item, premodProblems, null);
        verify(item).setStatus(PremodItemStatus.FAILED);
        verify(premodManager).updatePremodItem(item);

    }

    @Test
    void addNoPlacementProblem() {
        var item = item(PremodItemType.SHOP_INFO_COLLECTED);
        var problems = List.of(problem(PremodProblemTypeId.CANNOT_PLACE_CURSED));
        when(premodProblemService.loadPremodProblemsByItem(item.getId())).thenReturn(problems);
        when(premodItemService.loadPremodItemsByTicket(anyLong())).thenReturn(List.of());

        int noPlacementReasonId = 1;
        premodProblemManager.addPremodProblems(item, problems, noPlacementReasonId);

        verify(noPlacementManager).addRecordForPremoderation(SHOP_ID, noPlacementReasonId, AboCutoff.COMMON_QUALITY, USER_ID, TICKET_ID);
    }

    @Test
    void addPremodProblemsTest_setStatus_NEED_INFO() {
        List<PremodProblem> premodProblems = List.of(
                problem(PremodProblemTypeId.OGRN_REQUEST),
                problem(PremodProblemTypeId.REQUIRED_DOCUMENTS)
        );
        var item = item(PremodItemType.OTHER);
        when(premodProblemService.loadPremodProblemsByItem(item.getId())).thenReturn(premodProblems);
        premodProblemManager.addPremodProblems(item, premodProblems, null);
        verify(item).setStatus(PremodItemStatus.NEED_INFO);
        verify(premodManager).updatePremodItem(item);
    }

    @ParameterizedTest
    @CsvSource({
            "SHOP_INFO_COLLECTED, false, true",
            "SHOP_INFO_COLLECTED, true, false",
            "INFO_IN_PARTNER_COINCIDE, false, false",
            "INFO_IN_PARTNER_COINCIDE, true, false"
    })
    void deletePremodProblemsTest(PremodItemType itemType, boolean hasAnotherProblems, boolean shopInfoItemHelperCall) {
        when(premodProblemService.loadPremodProblemsByItem(ITEM_ID))
                .thenReturn(hasAnotherProblems ? List.of(new PremodProblem(ITEM_ID, 0, 0, null)) : List.of());

        var item = item(itemType);
        premodProblemManager.deletePremodProblems(item, List.of(PROBLEM_TYPE));

        verify(premodProblemService).deleteByItemAndTypes(ITEM_ID, List.of(PROBLEM_TYPE));
        if (hasAnotherProblems) {
            verify(item, never()).setStatus(any());
            verify(premodManager, never()).updatePremodItem(any());
        } else {
            verify(item).setStatus(PremodItemStatus.NEW);
            verify(premodManager).updatePremodItem(item);
        }
        verify(shopInfoItemHelper, shopInfoItemHelperCall ? times(1) : never()).onShopInfoAddedWithNewData(anyLong());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deletePremodProblemsTest_NEED_INFO(boolean hasOnlyNeedInfoProblems) {
        var leftProblems = new ArrayList<>(List.of(problem(PremodProblemTypeId.OGRN_REQUEST)));
        if (!hasOnlyNeedInfoProblems) {
            leftProblems.add(problem(PremodProblemTypeId.NO_OFFERS_IN_PRICE_LIST));
        }
        when(premodProblemService.loadPremodProblemsByItem(ITEM_ID)).thenReturn(leftProblems);

        var item = item(PremodItemType.OTHER);
        premodProblemManager.deletePremodProblems(item, List.of(PROBLEM_TYPE));

        var callCount = hasOnlyNeedInfoProblems ? 1 : 0;
        verify(item, times(callCount)).setStatus(PremodItemStatus.NEED_INFO);
        verify(premodManager, times(callCount)).updatePremodItem(item);
    }


    private static PremodItem item(PremodItemType type) {
        var item = mock(PremodItem.class);
        when(item.getId()).thenReturn(ITEM_ID);
        when(item.getTicketId()).thenReturn(TICKET_ID);
        when(item.getType()).thenReturn(type);
        return item;
    }

    private static PremodProblem problem(int typeId) {
        var problem = mock(PremodProblem.class);
        when(problem.getTypeId()).thenReturn(typeId);
        when(problem.getUid()).thenReturn(USER_ID);
        return problem;
    }
}
