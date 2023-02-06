package ru.yandex.direct.grid.processing.service.group.mutation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRegions;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRegionsAction;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.service.group.mutation.UpdateAdGroupRegionsMutationService.getNewRegionIds;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@ParametersAreNonnullByDefault
public class UpdateAdGroupRegionsMutationServiceGetNewRegionsTest {

    private GdUpdateAdGroupRegions input;
    private Long adGroupId;
    private Map<Long, List<Long>> actualAdGroupRegions;
    private GeoTree geoTree;

    @Before
    public void initTestData() {
        geoTree = mock(GeoTree.class);
        adGroupId = RandomNumberUtils.nextPositiveLong();
        actualAdGroupRegions = Map.of(adGroupId, List.of(Region.RUSSIA_REGION_ID));
        input = new GdUpdateAdGroupRegions()
                .withRegionIds(List.of(Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID, Region.BELGOROD_OBLAST_REGION_ID));
    }


    @Test
    public void getNewRegionIds_withReplaceAction() {
        input.setAction(GdUpdateAdGroupRegionsAction.REPLACE);

        List<Long> newRegionIds = getNewRegionIds(adGroupId, actualAdGroupRegions, input, geoTree);

        assertThat(newRegionIds)
                .is(matchedBy(beanDiffer(input.getRegionIds())));
        verifyZeroInteractions(geoTree);
    }

    @Test
    public void getNewRegionIds_withAddAction() {
        input.setAction(GdUpdateAdGroupRegionsAction.ADD);
        List<Long> expectedNewRegionIds = List.of(RandomNumberUtils.nextPositiveLong());
        doReturn(expectedNewRegionIds)
                .when(geoTree).includeRegions(actualAdGroupRegions.get(adGroupId), input.getRegionIds());

        List<Long> newRegionIds = getNewRegionIds(adGroupId, actualAdGroupRegions, input, geoTree);

        assertThat(newRegionIds)
                .is(matchedBy(beanDiffer(expectedNewRegionIds)));
        verify(geoTree).includeRegions(actualAdGroupRegions.get(adGroupId), input.getRegionIds());
    }

    @Test
    public void getNewRegionIds_withRemoveAction() {
        input.setAction(GdUpdateAdGroupRegionsAction.REMOVE);
        List<Long> expectedNewRegionIds = List.of(RandomNumberUtils.nextPositiveLong());
        doReturn(expectedNewRegionIds)
                .when(geoTree).excludeRegions(actualAdGroupRegions.get(adGroupId), input.getRegionIds());

        List<Long> newRegionIds = getNewRegionIds(adGroupId, actualAdGroupRegions, input, geoTree);

        assertThat(newRegionIds)
                .is(matchedBy(beanDiffer(expectedNewRegionIds)));
        verify(geoTree).excludeRegions(actualAdGroupRegions.get(adGroupId), input.getRegionIds());
    }

    @Test
    public void getNewRegionIds_forNotExistAdGroup() {
        input.setAction(GdUpdateAdGroupRegionsAction.REPLACE);
        actualAdGroupRegions = Collections.emptyMap();

        List<Long> newRegionIds = getNewRegionIds(adGroupId, actualAdGroupRegions, input, geoTree);

        assertThat(newRegionIds)
                .is(matchedBy(beanDiffer(Collections.emptyList())));
        verifyZeroInteractions(geoTree);
    }

}
