package ru.yandex.direct.jobs.featuresync;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.feature.container.FeatureTextIdToClientIdState;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.feature.service.FeatureCache;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.result.ResultState;
import ru.yandex.direct.ytwrapper.client.YtProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.jobs.featuresync.CheckedSupportChatSyncJob.FEATURE_NAME;

class CheckedSupportChatProcessRowsTest {
    CheckedSupportChatSyncJob job;
    PpcProperty<LocalDateTime> property;
    FeatureManagingService service;
    ShardHelper helper;
    List<ClientId> clientIds =
            List.of(ClientId.fromLong(1L), ClientId.fromLong(2L), ClientId.fromLong(3L), ClientId.fromLong(4L));

    @SuppressWarnings("unchecked")
    @BeforeEach
    void init() {
        YtProvider ytProvider = mock(YtProvider.class);
        property = mock(PpcProperty.class); // можно было бы объявить сразу, но нужен @SuppressWarnings("unchecked")
        helper = mock(ShardHelper.class);
        FeatureCache featureCache = mock(FeatureCache.class);
        service = mock(FeatureManagingService.class);
        when(service.switchFeaturesStateForClientIds(anyList()))
                .thenReturn(new Result<>(List.of(), null, ResultState.SUCCESSFUL));
        job = new CheckedSupportChatSyncJob(ytProvider, property, helper, featureCache, service);
    }

    @Test
    void allExist_turnOn_requestAll() {
        when(helper.getExistingClientIdsList(clientIds, false)).thenReturn(clientIds);
        job.processRowsTurnOn(clientIds);
        List<FeatureTextIdToClientIdState> expected = clientIds.stream().map(c -> new FeatureTextIdToClientIdState()
                .withState(FeatureState.ENABLED).withClientId(c).withTextId(FEATURE_NAME)
        ).collect(Collectors.toList());
        verify(service).switchFeaturesStateForClientIds(argThat(t -> t.containsAll(expected)));
    }

    @Test
    void someExist_turnOff_requestExisting() {
        List<ClientId> existing = List.of(ClientId.fromLong(1L), ClientId.fromLong(2L), ClientId.fromLong(4L));
        when(helper.getExistingClientIdsList(clientIds, false)).thenReturn(existing);
        job.processRowsTurnOff(clientIds);
        List<FeatureTextIdToClientIdState> expected = existing.stream().map(c -> new FeatureTextIdToClientIdState()
                .withState(FeatureState.DISABLED).withClientId(c).withTextId(FEATURE_NAME)
        ).collect(Collectors.toList());
        verify(service).switchFeaturesStateForClientIds(argThat(t -> t.containsAll(expected)));
    }

    @Test
    void noneExist_turnOff_doNothing() {
        List<ClientId> existing = List.of();
        when(helper.getExistingClientIdsList(clientIds, false)).thenReturn(existing);
        job.processRowsTurnOff(clientIds);
        verify(service, never()).switchFeaturesStateForClientIds(any());
    }
}
