package ru.yandex.market.core.security.checker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.security.DefaultCampaignable;
import ru.yandex.market.security.model.Authority;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author wadim
 */
public class InpostPromoAccessCheckerTest {
    private InpostPromoAccessChecker checker;

    @Mock
    private ParamService paramService;
    @Mock
    private CampaignService campaignService;
    @Mock
    private RegionService regionService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        checker = new InpostPromoAccessChecker(paramService, campaignService, regionService);
    }

    @Test
    public void testCheckedTyped_NullCampaign() throws Exception {
        assertFalse(checker.checkTyped(new DefaultCampaignable(1, 2, 3), new Authority()));

        verify(campaignService).getCampaign(1L);
        verifyNoMoreInteractions(campaignService, paramService, regionService);
    }

    @Test
    public void testCheckedTyped_NullRegionId() throws Exception {
        CampaignInfo campaignInfo = new CampaignInfo(10, 20);
        when(campaignService.getCampaign(1)).thenReturn(campaignInfo);

        assertFalse(checker.checkTyped(new DefaultCampaignable(1, 2, 3), new Authority()));

        verify(campaignService).getCampaign(1L);
        verify(paramService).getParamNumberValue(ParamType.LOCAL_DELIVERY_REGION, campaignInfo.getDatasourceId());
        verifyNoMoreInteractions(campaignService, paramService, regionService);
    }
}
