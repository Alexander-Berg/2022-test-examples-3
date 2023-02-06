package ru.yandex.market.partner.mvc.controller.cpc;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.core.cpc.CPC;
import ru.yandex.market.core.cpc.CpcState;
import ru.yandex.market.core.cpc.CpcStateService;
import ru.yandex.market.core.cutoff.CutoffService;
import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.ds.info.DatasourceInformationService;
import ru.yandex.market.core.ds.info.UniShopInformation;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static org.mockito.Mockito.when;

/**
 * Тесты на логику работы {@link CpcStateController}.
 *
 * @author fbokovikov
 */
@RunWith(MockitoJUnitRunner.class)
public class CpcStateControllerTest {

    private static final long SHOP_ID = 774L;

    @Mock
    private CutoffService cutoffService;

    @Mock
    private ParamService paramService;

    @Mock
    private DatasourceInformationService datasourceInformationService;

    @Mock
    private PartnerDefaultRequestHandler.PartnerHttpServRequest request;

    private CpcStateController controller;

    @Before
    public void init() {
        when(request.getDatasourceId()).thenReturn(SHOP_ID);
        CpcStateService cpcStateService =
                new CpcStateService(cutoffService, datasourceInformationService);
        controller = new CpcStateController(cpcStateService);
    }

    /**
     * Тест на включенность магазина по CPC - нет катоффов и CPC_ENABLED = true.
     */
    @Test
    public void testCpcOk() {
        when(cutoffService.getCutoffsByDatasource(SHOP_ID)).thenReturn(Collections.emptyMap());
        CpcState cpcState = controller.getCpcState(request);
        System.out.println(cpcState);
        statesEquals(cpcState, new CpcState(CPC.REAL, Collections.emptySet(), true, false));
    }

    /**
     * Тест на включенность магазина по CPC - есть катоф CutoffType.CPC_PARTNER катоффов и CPC_ENABLED = false.
     */
    @Test
    public void testCpcPartner() {
        when(cutoffService.getCutoffsByDatasource(SHOP_ID))
                .thenReturn(
                        ImmutableMap.of(
                                CutoffType.FINANCE,
                                new CutoffInfo(1, SHOP_ID, CutoffType.FINANCE, new Date(), null),
                                CutoffType.CPC_PARTNER,
                                new CutoffInfo(2, SHOP_ID, CutoffType.CPC_PARTNER, new Date(), null)));
        CpcState cpcState = controller.getCpcState(request);
        System.out.println(cpcState);
        statesEquals(cpcState, new CpcState(CPC.NONE, EnumSet.of(CutoffType.FINANCE, CutoffType.CPC_PARTNER), true,
                false));
    }

    /**
     * Тест на выключенность магазина по CPC из-за CPC_ENABLED.
     */
    @Test
    public void testCpcDisabledParam() {
        when(cutoffService.getCutoffsByDatasource(SHOP_ID)).thenReturn(Collections.emptyMap());
        when(cutoffService.countClosedCutoffs(SHOP_ID, CutoffType.FORTESTING)).thenReturn(1);
        CpcState cpcState = controller.getCpcState(request);
        statesEquals(cpcState, new CpcState(CPC.REAL, Collections.emptySet(), true, true));
    }

    /**
     * Тест на выключенность магазина из-за отключний по CPC.
     */
    @Test
    public void testCpcDisabledCutoffs() {
        when(cutoffService.getCutoffsByDatasource(SHOP_ID)).thenReturn(
                ImmutableMap.of(
                        CutoffType.QUALITY_PINGER, new CutoffInfo(1, CutoffType.QUALITY_PINGER)
                )
        );
        CpcState cpcState = controller.getCpcState(request);
        statesEquals(cpcState, new CpcState(CPC.NONE, ImmutableSet.of(CutoffType.QUALITY_PINGER), true, false));
    }

    /**
     * Тест на выключенность магазина с невозможностью отправиться на модерацию из-за FATAL-отключения.
     */
    @Test
    public void testCpcDisabledFatalCutoffs() {
        when(cutoffService.getCutoffsByDatasource(SHOP_ID)).thenReturn(
                ImmutableMap.of(
                        CutoffType.COMMON_QUALITY, new CutoffInfo(1, CutoffType.COMMON_QUALITY)
                )
        );
        CpcState cpcState = controller.getCpcState(request);
        statesEquals(cpcState, new CpcState(CPC.NONE, ImmutableSet.of(CutoffType.COMMON_QUALITY), false, false));
    }

    @Test
    public void testCanNotSwitchToOnMissedCpcParam() {
        when(cutoffService.getCutoffsByDatasource(SHOP_ID)).thenReturn(
                ImmutableMap.of(
                        CutoffType.TECHNICAL_NEED_INFO, new CutoffInfo(1, CutoffType.TECHNICAL_NEED_INFO)
                )
        );
        when(datasourceInformationService.getMissedDatasourceInfo(SHOP_ID, ShopProgram.CPC)).thenReturn(
                Collections.singletonList(new UniShopInformation("phone"))
        );
        CpcState cpcState = controller.getCpcState(request);
        statesEquals(cpcState, new CpcState(CPC.NONE, ImmutableSet.of(CutoffType.TECHNICAL_NEED_INFO), false, false));
    }

    private void statesEquals(CpcState cpcState1, CpcState cpcState2) {
        Assert.assertEquals(cpcState2.getCpc(), cpcState1.getCpc());
        Assert.assertEquals(cpcState2.isCanSwitchToOn(), cpcState1.isCanSwitchToOn());
        Assert.assertEquals(new HashSet<>(cpcState2.getCpcCutoffs()), new HashSet<>(cpcState1.getCpcCutoffs()));
        Assert.assertEquals(cpcState2.isPassedModerationOnce(), cpcState1.isPassedModerationOnce());
    }

}
