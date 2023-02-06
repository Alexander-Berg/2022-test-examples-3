package ru.yandex.calendar.logic.event;

import java.util.List;

import lombok.val;
import one.util.streamex.IntStreamEx;
import org.junit.jupiter.api.Test;

import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.resource.ResourceInfo;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.ResourceParticipantInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.calendar.logic.event.EventRoutines.willHaveResourcesAfterUpdate;

public class EventRoutinesSmallTest {
    @Test
    public void willHaveResourcesAfterAddResources() {
        assertThat(evaluateWillHaveAttachedResourcesAfterUpdates(0, true)).isTrue();
    }

    @Test
    public void willHaveResourcesAfterRemovePartOfResources() {
        assertThat(evaluateWillHaveAttachedResourcesAfterUpdates(2, false, 0)).isTrue();
    }

    @Test
    public void willNotHaveResourcesAfterRemoveAllResources() {
        assertThat(evaluateWillHaveAttachedResourcesAfterUpdates(2, false, 0, 1)).isFalse();
    }

    @Test
    public void willHaveResourcesAfterRemoveAllAndAddNewResources() {
        assertThat(evaluateWillHaveAttachedResourcesAfterUpdates(2, true, 0, 1)).isTrue();
    }

    @Test
    public void willHaveResourcesAfterNoChangesInANotEmptyList() {
        assertThat(evaluateWillHaveAttachedResourcesAfterUpdates(2, false)).isTrue();
    }

    @Test
    public void willNotHaveResourcesAfterNoChangesInEmptyList() {
        assertThat(evaluateWillHaveAttachedResourcesAfterUpdates(0, false)).isFalse();
    }

    /**
     * Evaluate a test case for {@link EventRoutines#willHaveResourcesAfterUpdate(List, List, boolean)} and return
     * its response.
     * @param count initial amount of meeting rooms
     * @param wasNewResources whether there was some new resources or not
     * @param removedResourcesIndexes indexes of resources to remove during update
     * @return response of {@link EventRoutines#willHaveResourcesAfterUpdate(List, List, boolean)} for this test-case
     */
    private boolean evaluateWillHaveAttachedResourcesAfterUpdates(int count, boolean wasNewResources, int... removedResourcesIndexes) {
        val resources = prepareResourcesList(count);
        val resourcesToRemove = IntStreamEx.of(removedResourcesIndexes)
                .mapToObj(resources::get)
                .map(this::wrapIntoParticipantInfo)
                .toList();
        return willHaveResourcesAfterUpdate(resources, resourcesToRemove, wasNewResources);
    }

    /**
     * @param count amount of resources
     * @return list of created resources
     */
    private List<ResourceInfo> prepareResourcesList(int count) {
        val office = new Office();
        office.setId(1L);

        return IntStreamEx.range(count)
                .mapToObj(this::createResource)
                .map(resource -> new ResourceInfo(resource, office))
                .toList();
    }

    private Resource createResource(long id) {
        val r = new Resource();
        r.setId(id);
        return r;
    }

    /**
     * @param resourceInfo information about resource
     * @return information about participant based on the given resource
     */
    private ParticipantInfo wrapIntoParticipantInfo(ResourceInfo resourceInfo) {
        val participant = mock(ResourceParticipantInfo.class);
        doReturn(ParticipantId.resourceId(resourceInfo.getResourceId())).when(participant).getId();
        return participant;
    }

}
