package ru.yandex.cs.placement.tms.offer.documents;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.cs.billing.campaign.model.CampaignInfo;
import ru.yandex.cs.billing.cutoff.model.DsCutoff;
import ru.yandex.cs.billing.cutoff.model.DsCutoffType;
import ru.yandex.cs.billing.cutoff.service.CutoffService;
import ru.yandex.cs.billing.datasource.model.Datasource;
import ru.yandex.vendor.csbilling.CsBillingApiClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.cs.billing.CsBillingCoreConstants.ANALYTICS_SERVICE_ID;
import static ru.yandex.cs.billing.CsBillingCoreConstants.VENDOR_SERVICE_ID;

/**
 * Unit tests for {@link CheckOfferDocumentsExpirationExecutor}.
 *
 * @author fbokovikov
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckOfferDocumentsExpirationExecutorTest {

    private static final long DEFAULT_UID = -1;

    private static final DsCutoff CUTOFF_1 = dsCutoff(10L, VENDOR_SERVICE_ID);
    private static final DsCutoff CUTOFF_2 = dsCutoff(15L, VENDOR_SERVICE_ID);
    private static final DsCutoff CUTOFF_3 = dsCutoff(20L, VENDOR_SERVICE_ID);
    private static final DsCutoff CUTOFF_4 = dsCutoff(100L, ANALYTICS_SERVICE_ID);

    private static final List<DsCutoff> EXISTED_CUTOFFS = Arrays.asList(CUTOFF_1, CUTOFF_2, CUTOFF_3);

    private static final DsCutoff CUTOFF_5 = dsCutoff(50L, ANALYTICS_SERVICE_ID);

    private static final List<DsCutoff> ACTUAL_CUTOFFS = Arrays.asList(CUTOFF_1, CUTOFF_2, CUTOFF_5);

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private CsBillingApiClient csBillingApiClient;

    @Mock
    private CutoffService cutoffService;

    @InjectMocks
    private CheckOfferDocumentsExpirationExecutor checkOfferDocumentsExpirationExecutor;

    @Test
    public void doJob() {
        when(cutoffService.findActive(VENDOR_SERVICE_ID, DsCutoffType.OFFER_DOCUMENTS))
                .thenReturn(EXISTED_CUTOFFS);

        when(cutoffService.findActive(ANALYTICS_SERVICE_ID, DsCutoffType.OFFER_DOCUMENTS))
                .thenReturn(Arrays.asList(CUTOFF_4));
        List<Datasource> disabledDatasources = ACTUAL_CUTOFFS.stream()
                .map(c -> new Datasource(c.getServiceId(), c.getDatasourceId()))
                .collect(Collectors.toList());
        when(jdbcTemplate.query(
                argThat(sql -> sql.contains("WITH active_docs AS ( ")),
                eq(EmptySqlParameterSource.INSTANCE),
                any(RowMapper.class))
        ).thenReturn(disabledDatasources);
        disabledDatasources.forEach(this::mockDatasource);
        checkOfferDocumentsExpirationExecutor.doJob(Mockito.mock(JobExecutionContext.class));
        verify(csBillingApiClient)
                .postDatasourceCutoff(50L, 206, DsCutoffType.OFFER_DOCUMENTS, null, DEFAULT_UID);
        verify(csBillingApiClient)
                .removeDatasourceCutoff(argThat(c -> c.getDatasourceId() == 100L), eq(206), eq(DEFAULT_UID));
        verify(csBillingApiClient)
                .removeDatasourceCutoff(argThat(c -> c.getDatasourceId() == 20L), eq(132), eq(DEFAULT_UID));
        disabledDatasources.forEach(ds -> {
            Mockito.verify(csBillingApiClient).getCurrentCampaignByDatasource(ds.getDatasourceId(), ds.getServiceId());
        });
        verifyNoMoreInteractions(csBillingApiClient);

    }

    private void mockDatasource(Datasource datasource) {
        when(csBillingApiClient.getCurrentCampaignByDatasource(datasource.getDatasourceId(), datasource.getServiceId()))
                .thenReturn(campaignInfo(datasource.getDatasourceId(), datasource.getServiceId()));
    }

    private static DsCutoff dsCutoff(long datasourceId, int serviceId) {
        DsCutoff dsCutoff = new DsCutoff();
        dsCutoff.setDatasourceId(datasourceId);
        dsCutoff.setServiceId(serviceId);
        dsCutoff.setType(DsCutoffType.OFFER_DOCUMENTS);
        return dsCutoff;
    }

    private static Optional<CampaignInfo> campaignInfo(long datasourceId, int serviceId) {
        CampaignInfo campaignInfo = new CampaignInfo();
        campaignInfo.setDatasourceId(datasourceId);
        campaignInfo.setServiceId(serviceId);
        campaignInfo.setOffer(true);
        return Optional.of(campaignInfo);
    }
}
