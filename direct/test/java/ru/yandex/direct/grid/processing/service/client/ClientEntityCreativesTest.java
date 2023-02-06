package ru.yandex.direct.grid.processing.service.client;

import java.util.Collections;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.client.GdClientSearchRequest;
import ru.yandex.direct.grid.processing.model.cliententity.GdCreativeFilter;
import ru.yandex.direct.grid.processing.model.cliententity.GdCreativeType;
import ru.yandex.direct.grid.processing.model.cliententity.GdCreativesContainer;
import ru.yandex.direct.grid.processing.model.cliententity.GdCreativesContext;
import ru.yandex.direct.grid.processing.model.cliententity.GdTypedCreative;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.testing.data.TestCreatives.fullCreative;
import static ru.yandex.direct.grid.processing.service.client.converter.ClientEntityConverter.toGdCreativeImplementation;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientEntityCreativesTest {
    @Autowired
    ClientEntityDataService clientEntityDataService;

    @Autowired
    Steps steps;

    private CreativeInfo canvasCreative;
    private CreativeInfo videoCreative;
    private GdClientInfo clientInfo;
    private GdTypedCreative gdCanvasCreative;
    private GdTypedCreative gdVideoAdditionCreative;
    private GdCreativesContainer container;

    @Before
    public void init() {
        UserInfo user = steps.userSteps().createDefaultUser();
        canvasCreative = steps.creativeSteps().addDefaultCanvasCreative(user.getClientInfo());
        videoCreative = steps.creativeSteps().createCreative(
                fullCreative(user.getClientInfo().getClientId(), RandomNumberUtils.nextPositiveLong()).withId(null),
                user.getClientInfo());

        clientInfo = new GdClientInfo().withShard(user.getShard())
                .withId(user.getClientInfo().getClientId().asLong());
        gdCanvasCreative = toGdCreativeImplementation(canvasCreative.getCreative());
        gdVideoAdditionCreative = toGdCreativeImplementation(videoCreative.getCreative());
        container = defaultGdCreativesContainer();

    }

    @Test
    public void testClientCreatives_noFilter() {
        check(defaultGdCreativesContext()
                .withCreativeIds(ImmutableSet.of(canvasCreative.getCreativeId(), videoCreative.getCreativeId()))
                .withTotalCount(2)
                .withRowset(asList(gdCanvasCreative, gdVideoAdditionCreative)));
    }

    @Test
    public void testClientCreatives_filterUnexistingCreativeId() {
        container.getFilter().setCreativeIdIn(ImmutableSet.of(Long.MAX_VALUE));
        check(defaultGdCreativesContext());
    }

    @Test
    public void testClientCreatives_filterUnexistingCreativeType() {
        container.getFilter().setCreativeTypeIn(ImmutableSet.of(GdCreativeType.CPC_VIDEO_CREATIVE));
        check(defaultGdCreativesContext());
    }

    @Test
    public void testClientCreatives_filterExistingCreativeId() {
        container.getFilter().setCreativeIdIn(ImmutableSet.of(canvasCreative.getCreativeId()));

        check(defaultGdCreativesContext()
                .withCreativeIds(ImmutableSet.of(canvasCreative.getCreativeId()))
                .withTotalCount(1)
                .withRowset(Collections.singletonList(gdCanvasCreative)));
    }

    @Test
    public void testClientCreatives_filterExistingCreativeType() {
        container.getFilter().setCreativeTypeIn(ImmutableSet.of(GdCreativeType.VIDEO_ADDITION));

        check(defaultGdCreativesContext()
                .withCreativeIds(ImmutableSet.of(videoCreative.getCreativeId()))
                .withTotalCount(1)
                .withRowset(Collections.singletonList(gdVideoAdditionCreative)));
    }

    @Test
    public void testClientCreatives_limitNoOffset() {
        container.getLimitOffset().withLimit(1).withOffset(0);

        check(defaultGdCreativesContext()
                .withCreativeIds(ImmutableSet.of(canvasCreative.getCreativeId(), videoCreative.getCreativeId()))
                .withTotalCount(2)
                .withRowset(Collections.singletonList(gdCanvasCreative)));
    }

    @Test
    public void testClientCreatives_limitWithOffset() {
        container.getLimitOffset().withLimit(1).withOffset(1);

        check(defaultGdCreativesContext()
                .withCreativeIds(ImmutableSet.of(canvasCreative.getCreativeId(), videoCreative.getCreativeId()))
                .withTotalCount(2)
                .withRowset(Collections.singletonList(gdVideoAdditionCreative)));
    }

    @Test
    public void testClientCreatives_filterUnexistingAndExistingCreativeIds() {
        container.getFilter().setCreativeIdIn(ImmutableSet.of(canvasCreative.getCreativeId(), Long.MAX_VALUE));

        check(defaultGdCreativesContext()
                .withCreativeIds(ImmutableSet.of(canvasCreative.getCreativeId()))
                .withTotalCount(1)
                .withRowset(Collections.singletonList(gdCanvasCreative)));
    }

    private void check(GdCreativesContext expected) {
        GdCreativesContext gdCreativesContext = clientEntityDataService.getClientCreatives(clientInfo, container);

        assertSoftly(assertions -> {
            assertions.assertThat(gdCreativesContext.getTotalCount()).isEqualTo(expected.getTotalCount());
            assertions.assertThat(gdCreativesContext.getCreativeIds()).hasSameElementsAs(expected.getCreativeIds());
            assertions.assertThat(gdCreativesContext.getRowset()).hasSameElementsAs(expected.getRowset());
        });
    }

    private GdCreativesContainer defaultGdCreativesContainer() {
        return new GdCreativesContainer()
                .withSearchBy(new GdClientSearchRequest()
                        .withId(clientInfo.getId()))
                .withFilter(new GdCreativeFilter()
                        .withCreativeIdIn(Collections.emptySet())
                        .withCreativeTypeIn(Collections.emptySet()))
                .withLimitOffset(new GdLimitOffset());
    }

    private GdCreativesContext defaultGdCreativesContext() {
        return new GdCreativesContext()
                .withRowset(Collections.emptyList())
                .withCreativeIds(Collections.emptySet())
                .withTotalCount(0);
    }

}
