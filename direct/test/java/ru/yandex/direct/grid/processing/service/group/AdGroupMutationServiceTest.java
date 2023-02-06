package ru.yandex.direct.grid.processing.service.group;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateCpmAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateMcBannerAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateMobileContentAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdatePerformanceAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroup;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class AdGroupMutationServiceTest {

    @Autowired
    Steps steps;

    @Autowired
    AdGroupMutationService serviceUnderTest;

    private ClientId clientId;
    private Long operatorUid;
    private Long clientUid;

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        operatorUid = clientInfo.getUid();
        clientUid = clientInfo.getUid();
    }

    @Test
    public void updateAdGroup_success_onEmptyItemList() {
        GdUpdateTextAdGroup input = new GdUpdateTextAdGroup()
                .withUpdateItems(emptyList());
        GdUpdateAdGroupPayload result = serviceUnderTest
                .updateTextAdGroup(UidAndClientId.of(clientUid, clientId), operatorUid, input);
        assertThat(result.getValidationResult())
                .as("validationResult")
                .isNull();
        assertThat(result.getUpdatedAdGroupItems())
                .as("updatedAdGroupItems")
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void updatePerformanceAdGroups_success_onEmptyItemList() {
        GdUpdatePerformanceAdGroup input = new GdUpdatePerformanceAdGroup()
                .withUpdateItems(emptyList());
        GdUpdateAdGroupPayload result = serviceUnderTest.updatePerformanceAdGroups(clientId, operatorUid, input);
        assertThat(result.getValidationResult())
                .as("validationResult")
                .isNull();
        assertThat(result.getUpdatedAdGroupItems())
                .as("updatedAdGroupItems")
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void updateCpmAdGroups_success_onEmptyItemList() {
        GdUpdateCpmAdGroup input = new GdUpdateCpmAdGroup()
                .withUpdateCpmAdGroupItems(emptyList());
        GdUpdateAdGroupPayload result = serviceUnderTest.saveCpmAdGroups(clientId, operatorUid, input);
        assertThat(result.getValidationResult())
                .as("validationResult")
                .isNull();
        assertThat(result.getUpdatedAdGroupItems())
                .as("updatedAdGroupItems")
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void updateMcBannerAdGroup_success_onEmptyItemList() {
        GdUpdateMcBannerAdGroup input = new GdUpdateMcBannerAdGroup()
                .withUpdateItems(emptyList());
        GdUpdateAdGroupPayload result = serviceUnderTest.updateMcBannerAdGroup(clientId, operatorUid, input);
        assertThat(result.getValidationResult())
                .as("validationResult")
                .isNull();
        assertThat(result.getUpdatedAdGroupItems())
                .as("updatedAdGroupItems")
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void updateMobileContentAdGroup_success_onEmptyItemList() {
        GdUpdateMobileContentAdGroup input = new GdUpdateMobileContentAdGroup()
                .withUpdateItems(emptyList());
        GdUpdateAdGroupPayload result = serviceUnderTest.updateMobileContentAdGroup(clientId, operatorUid, input);
        assertThat(result.getValidationResult())
                .as("validationResult")
                .isNull();
        assertThat(result.getUpdatedAdGroupItems())
                .as("updatedAdGroupItems")
                .isNotNull()
                .isEmpty();
    }
}
