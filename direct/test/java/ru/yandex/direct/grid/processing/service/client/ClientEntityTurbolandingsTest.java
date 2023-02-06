package ru.yandex.direct.grid.processing.service.client;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.client.GdClientSearchRequest;
import ru.yandex.direct.grid.processing.model.cliententity.GdTurbolanding;
import ru.yandex.direct.grid.processing.model.cliententity.GdTurbolandingFilter;
import ru.yandex.direct.grid.processing.model.cliententity.GdTurbolandingsContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.service.client.converter.ClientDataConverter.toGdTurbolanding;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientEntityTurbolandingsTest {
    @Autowired
    ClientEntityDataService clientEntityDataService;

    @Autowired
    Steps steps;

    private TurboLanding turbolanding;
    private TurboLanding anotherTurbolanding;
    private GdClientInfo clientInfo;
    private GdTurbolanding gdTurbolanding;
    private GdTurbolanding gdAnotherTurbolanding;
    private GdTurbolandingsContainer container;

    @Before
    public void init() {
        UserInfo user = steps.userSteps().createDefaultUser();
        turbolanding = steps.turboLandingSteps().createDefaultTurboLanding(user.getClientInfo().getClientId());
        anotherTurbolanding = steps.turboLandingSteps().createDefaultTurboLanding(user.getClientInfo().getClientId());
        clientInfo = new GdClientInfo().withShard(user.getShard())
                .withId(user.getClientInfo().getClientId().asLong());
        gdTurbolanding = toGdTurbolanding(turbolanding);
        gdAnotherTurbolanding = toGdTurbolanding(anotherTurbolanding);
        container = defaultGdTurbolandingsContainer();

    }

    @Test
    public void testClientTurbolandings_noFilter() {
        check(ImmutableSet.of(gdTurbolanding, gdAnotherTurbolanding));
    }

    @Test
    public void testClientTurbolandings_filterUnexistingTurbolandingId() {
        container.getFilter().setTurbolandingIdIn(ImmutableSet.of(Long.MAX_VALUE));

        check(Collections.emptySet());
    }

    @Test
    public void testClientTurbolandings_filterExistingTurbolandingId() {
        container.getFilter().setTurbolandingIdIn(ImmutableSet.of(turbolanding.getId()));

        check(ImmutableSet.of(gdTurbolanding));
    }

    @Test
    public void testClientTurbolandings_filterSeveralExistingTurbolandingIds() {
        container.getFilter().setTurbolandingIdIn(ImmutableSet.of(turbolanding.getId(), anotherTurbolanding.getId()));

        check(ImmutableSet.of(gdTurbolanding, gdAnotherTurbolanding));
    }

    @Test
    public void testClientTurbolandings_filterUnexistingAndExistingTurbolandingIds() {
        container.getFilter().setTurbolandingIdIn(ImmutableSet.of(turbolanding.getId(), Long.MAX_VALUE));

        check(ImmutableSet.of(gdTurbolanding));
    }


    private void check(Collection<GdTurbolanding> expected) {
        List<GdTurbolanding> gdTurbolandings = clientEntityDataService
                .getClientTurbolandings(clientInfo, container);

        assertThat(gdTurbolandings).hasSize(expected.size());
        assertThat(gdTurbolandings).hasSameElementsAs(expected);
    }

    private GdTurbolandingsContainer defaultGdTurbolandingsContainer() {
        return new GdTurbolandingsContainer()
                .withSearchBy(new GdClientSearchRequest()
                        .withId(clientInfo.getId()))
                .withFilter(new GdTurbolandingFilter());
    }
}
