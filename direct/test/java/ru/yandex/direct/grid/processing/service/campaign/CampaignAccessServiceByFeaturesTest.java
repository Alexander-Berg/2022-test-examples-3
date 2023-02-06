package ru.yandex.direct.grid.processing.service.campaign;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.bs.export.queue.service.BsExportQueueService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.core.AllowedTypesCampaignAccessibilityChecker;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter;

import static java.util.Collections.emptySet;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignAccessService.GRID_EDITABLE_SOURCES;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignAccessService.GRID_SUPPORTED_TYPES;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignAccessService.GRID_VISIBLE_SOURCES;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignAccessService.GRID_VISIBLE_TYPES;
import static ru.yandex.direct.utils.FunctionalUtils.mapSet;

public class CampaignAccessServiceByFeaturesTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);

    @Mock
    private BsExportQueueService bsExportQueueService;

    @Mock
    private FeatureService featureService;

    @Mock
    private CampaignAccessHelper campaignAccessHelper;

    @InjectMocks
    private CampaignAccessService campaignAccessService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getAccessibleCampaignTypes_featuresDisabled() {
        doReturn(emptySet())
                .when(featureService).getEnabledForClientId(eq(CLIENT_ID));

        AllowedTypesCampaignAccessibilityChecker expected = new AllowedTypesCampaignAccessibilityChecker(
                mapSet(GRID_SUPPORTED_TYPES.getGlobal(), CampaignDataConverter::toCampaignType),
                mapSet(GRID_VISIBLE_TYPES.getGlobal(), CampaignDataConverter::toCampaignType),
                GRID_EDITABLE_SOURCES.getGlobal(),
                GRID_VISIBLE_SOURCES.getGlobal());
        AllowedTypesCampaignAccessibilityChecker actual = campaignAccessService.getCampaignAccessibilityChecker(CLIENT_ID);

        assertThat(actual, beanDiffer(expected));
    }

    @Test
    public void getAccessibleCampaignTypes_featuresEnabled() {
        Set<String> allFeatures = mapSet(EnumSet.allOf(FeatureName.class), FeatureName::getName);
        doReturn(allFeatures)
                .when(featureService).getEnabledForClientId(eq(CLIENT_ID));

        ImmutableSet<GdCampaignType> supportedTypes = ImmutableSet.<GdCampaignType>builder()
                .addAll(GRID_SUPPORTED_TYPES.getGlobal())
                .addAll(GRID_SUPPORTED_TYPES.getByFeature().values())
                .build();

        ImmutableSet<GdCampaignType> visibleTypes = ImmutableSet.<GdCampaignType>builder()
                .addAll(GRID_VISIBLE_TYPES.getGlobal())
                .addAll(GRID_VISIBLE_TYPES.getByFeature().values())
                .build();

        AllowedTypesCampaignAccessibilityChecker expected = new AllowedTypesCampaignAccessibilityChecker(
                mapSet(supportedTypes, CampaignDataConverter::toCampaignType),
                mapSet(visibleTypes, CampaignDataConverter::toCampaignType),
                GRID_EDITABLE_SOURCES.getGlobal(),
                GRID_VISIBLE_SOURCES.getGlobal());

        AllowedTypesCampaignAccessibilityChecker actual = campaignAccessService.getCampaignAccessibilityChecker(CLIENT_ID);

        assertThat(actual, beanDiffer(expected));
    }
}
