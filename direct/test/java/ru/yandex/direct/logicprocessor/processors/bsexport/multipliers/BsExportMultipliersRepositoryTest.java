package ru.yandex.direct.logicprocessor.processors.bsexport.multipliers;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.bsexport.repository.BsExportMultipliersRepository;
import ru.yandex.direct.core.entity.campaign.container.CampaignWithAutobudget;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsAutobudget;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.libs.timetarget.TimeTargetUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BsExportMultipliersRepositoryTest {
    @Autowired
    private DslContextProvider dslContextProvider;

    private FeatureService featureService;
    private BsExportMultipliersRepository bsExportMultipliersRepository;
    private CampaignRepository campaignRepository;

    @BeforeEach
    public void before() {
        campaignRepository = mock(CampaignRepository.class);
        featureService = mock(FeatureService.class);

        bsExportMultipliersRepository = new BsExportMultipliersRepository(dslContextProvider, campaignRepository,
                featureService);

        final var campaigns = List.of(
            new CampaignWithAutobudget(1L, TimeTargetUtils.timeTarget24x7(), CampaignsAutobudget.Yes, ClientId.fromLong(1L)),
            new CampaignWithAutobudget(2L, TimeTargetUtils.timeTarget24x7(), CampaignsAutobudget.No, ClientId.fromLong(1L))
        );

        when(campaignRepository.getCampaignsAutobudgetData(ArgumentMatchers.anyInt(), ArgumentMatchers.anyCollection()))
                .thenReturn(campaigns);
    }

    @Test
    public void campaignAllowRateCorrectionsForAllStrategiesDisabled() {
        this.setCampaignAllowRateCorrectionsForAllStrategiesValue(false);

        final var result = bsExportMultipliersRepository.getCampaignsTimeTargetWithoutAutobudget(0, List.of(1L, 2L));
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.keySet()).isEqualTo(Set.of(2L));
    }

    @Test
    public void someTimeTargetIsNull() {
        final var campaigns = List.of(
                new CampaignWithAutobudget(1L, TimeTargetUtils.timeTarget24x7(), CampaignsAutobudget.No, ClientId.fromLong(1L)),
                new CampaignWithAutobudget(2L, null, CampaignsAutobudget.No, ClientId.fromLong(1L))
        );

        when(campaignRepository.getCampaignsAutobudgetData(ArgumentMatchers.anyInt(), ArgumentMatchers.anyCollection()))
                .thenReturn(campaigns);

        final var result = bsExportMultipliersRepository.getCampaignsTimeTargetWithoutAutobudget(0, List.of(1L, 2L));
        assertThat(result.size()).isEqualTo(1);
    }

    private void setCampaignAllowRateCorrectionsForAllStrategiesValue(Boolean value) {
        final var map = new HashMap<ClientId, Boolean>();
        map.put(ClientId.fromLong(1L), value);

        when(featureService.isEnabledForClientIds(ArgumentMatchers.any(), ArgumentMatchers.eq(FeatureName.CAMPAIGN_ALLOW_RATE_CORRECTIONS_FOR_ALL_STRATEGIES)))
                .thenReturn(map);
    }
}
