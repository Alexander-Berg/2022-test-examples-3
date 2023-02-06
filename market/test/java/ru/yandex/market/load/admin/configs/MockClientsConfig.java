package ru.yandex.market.load.admin.configs;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import retrofit2.Response;

import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.abc.AbcApiClient;
import ru.yandex.market.abc.models.AbcPerson;
import ru.yandex.market.abc.models.AbcRole;
import ru.yandex.market.abc.models.AbcServiceMember;
import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.load.admin.client.CheckouterClients;
import ru.yandex.market.load.admin.client.SimplifiedTsumApiClient;
import ru.yandex.market.load.admin.client.solomon.SolomonApiClient;
import ru.yandex.market.load.admin.client.solomon.dto.DataResultDto;
import ru.yandex.market.load.admin.clients.SimplifiedTsumApiClientTestImpl;
import ru.yandex.market.load.admin.service.ShootingTicketStatus;
import ru.yandex.market.lom.LomApiClient;
import ru.yandex.market.lom.ShootingOrdersReadyToCancel;
import ru.yandex.mj.generated.client.checkouter.api.CheckouterApiClient;
import ru.yandex.mj.generated.client.checkouter.model.DeliveryTimings;
import ru.yandex.mj.generated.client.checkouter.model.DoubleHolder;
import ru.yandex.mj.generated.client.checkouter.model.Orders;
import ru.yandex.mj.generated.client.checkouter.model.ShootingOrderRef;
import ru.yandex.mj.generated.client.checkouter.model.ShootingStatistics;
import ru.yandex.mj.generated.client.staff.api.StaffApiClient;
import ru.yandex.mj.generated.client.staff.model.PersonsResponse;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.StatusRef;
import ru.yandex.startrek.client.model.Transition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@Configuration
@EnableAutoConfiguration
public class MockClientsConfig {

    @Bean
    @Primary
    public SimplifiedTsumApiClient simplifiedTsumApiClient() {
        final SimplifiedTsumApiClientTestImpl client = new SimplifiedTsumApiClientTestImpl();
        return spy(client);
    }

    @Bean
    public StaffApiClient staffApiClient() {
        final StaffApiClient client = mock(StaffApiClient.class);
        final PersonsResponse personsResponse = mock(PersonsResponse.class);
        final ExecuteCall<PersonsResponse, RetryStrategy> executeCall = mockClientCall(personsResponse);
        when(client.v3PersonsGet(anyString(), anyString(), anyInt())).thenReturn(executeCall);
        return client;
    }

    @Bean
    public AbcApiClient abcApiClient() {
        final AbcApiClient abc = mock(AbcApiClient.class);
        final AbcServiceMember member = new AbcServiceMember();
        final AbcPerson person = new AbcPerson();
        person.setLogin("user-name");
        member.setPerson(person);
        final AbcRole role = mock(AbcRole.class);
        when(role.getCode()).thenReturn("developer");
        member.setRole(role);
        when(abc.getAbcServiceMembers(anyString())).then((args) -> Stream.of(member));
        return abc;
    }

    @Bean
    public CheckouterClients checkouterClients() {
        CheckouterApiClient client = mock(CheckouterApiClient.class);
        mockShootingStatisticsResponse(client);
        mockShootingErrorPercentResponse(client);
        mockShootingOrdersCancelGet(client);
        mockShootingOrdersGet(client);
        return new CheckouterClients(client, client);
    }

    @Bean
    public LomApiClient prodLomApiClient() {
        return mockLomApiClient();
    }

    @Bean
    public LomApiClient testingLomApiClient() {
        return mockLomApiClient();
    }

    @Bean
    public SolomonApiClient solomonApiClient() {
        SolomonApiClient client = mock(SolomonApiClient.class);
        DataResultDto dataResultDto = new DataResultDto();
        dataResultDto.setVector(new DataResultDto[0]);
        when(client.getData(ArgumentMatchers.anyString(), anyMap(), any(Instant.class), any(Instant.class)))
                .thenReturn(dataResultDto);
        return client;
    }

    @Bean
    public Issues startrekIssues(IssuesTracker issuesTracker) {
        final Issues issues = mock(Issues.class);
        when(issues.create(any(IssueCreate.class), anyBoolean()))
            .then(inv -> {
                final String key = "MARKETLOAD-" + issuesTracker.getStore().size();
                Issue issue = new IssueMocker(key).createMock();
                issuesTracker.getStore().put(key, issue);
                issuesTracker.onSave();
                return issue;
            });
        when(issues.get(ArgumentMatchers.anyString())).then(inv -> {
            Object key = inv.getArgument(0);
            return issuesTracker.getStore().get(key);
        });
        return issues;
    }

    @Bean
    public IssuesTracker issuesStore() {
        IssuesTracker mock = mock(IssuesTracker.class);
        Map<String, Issue> store = new ConcurrentHashMap<>();
        when(mock.getStore()).thenReturn(store);
        return mock;
    }

    @NotNull
    private LomApiClient mockLomApiClient() {
        final LomApiClient client = mock(LomApiClient.class);
        final ShootingOrdersReadyToCancel ordersReadyToCancel = mock(ShootingOrdersReadyToCancel.class);
        when(client.getShootingOrdersReadyToCancel(
                any(Instant.class), any(Instant.class), anyLong(), anyLong(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(ordersReadyToCancel);
        mock(ShootingOrdersReadyToCancel.class);
        return client;
    }

    private void mockShootingStatisticsResponse(CheckouterApiClient client) {
        ShootingStatistics shootingStatistics = new ShootingStatistics();
        Orders orders = new Orders()
                .created(0)
                .notCreated(0)
                .cashbackApplied(0)
                .coinApplied(0)
                .promoCodeApplied(0)
                .flashApplied(0)
                .distributionToCount(Collections.emptyList());
        shootingStatistics.setOrders(orders);

        DeliveryTimings deliveryTimings = new DeliveryTimings();
        shootingStatistics.setDeliveryTimings(deliveryTimings);

        final ExecuteCall<ShootingStatistics, RetryStrategy> executeCall = mockClientCall(shootingStatistics);
        when(client.shootingStatisticsGet(anyString(), anyString())).thenReturn(executeCall);
    }

    private void mockShootingErrorPercentResponse(CheckouterApiClient client) {
        final ExecuteCall<DoubleHolder, RetryStrategy> executeCall = mockClientCall(new DoubleHolder().value(0.0));
        when(client.shootingErrorPercentGet(anyString(), anyString())).thenReturn(executeCall);
    }

    private void mockShootingOrdersCancelGet(CheckouterApiClient client) {
        final ExecuteCall<List<ShootingOrderRef>, RetryStrategy> executeCall = mockClientCall(Collections.emptyList());
        when(client.shootingOrdersCancelGet(anyInt(), anyInt(), anyString(), anyString())).thenReturn(executeCall);
    }

    private void mockShootingOrdersGet(CheckouterApiClient client) {
        final ExecuteCall<List<ShootingOrderRef>, RetryStrategy> executeCall = mockClientCall(Collections.emptyList());
        when(client.shootingOrdersGet(anyString(), anyInt(), anyInt())).thenReturn(executeCall);
    }

    @NotNull
    private <T> ExecuteCall<T, RetryStrategy> mockClientCall(T result) {
        ExecuteCall<T, RetryStrategy> executeCall = mock(ExecuteCall.class);
        CompletableFuture<T> future = mock(CompletableFuture.class);
        when(future.join()).thenReturn(result);
        when(executeCall.schedule()).thenReturn(future);
        final CompletableFuture<Response<T>> responseFuture = mock(CompletableFuture.class);
        when(responseFuture.join()).thenReturn(Response.success(result));
        when(executeCall.scheduleResponse()).thenReturn(responseFuture);
        return executeCall;
    }

    @RequiredArgsConstructor
    @Getter
    private static class IssueMocker {
        final String key;
        volatile String status = "open";
        StatusRef getStatus() {
            StatusRef mock = mock(StatusRef.class);
            when(mock.getKey()).then(inv -> this.status);
            return mock;
        }

        List<Transition> getTransitions() {
            if (ShootingTicketStatus.OPEN.getIssueKey().equals(status)) {
                return Arrays.asList(
                        mockTransition(ShootingTicketStatus.IN_PROGRESS.getTransitionKey()),
                        mockTransition(ShootingTicketStatus.NEED_INFO.getTransitionKey()));
            }
            if (ShootingTicketStatus.IN_PROGRESS.getIssueKey().equals(status)) {
                return Arrays.asList(
                        mockTransition(ShootingTicketStatus.CLOSED.getTransitionKey()),
                        mockTransition(ShootingTicketStatus.NEED_INFO.getTransitionKey()));
            }
            return Collections.emptyList();
        }

        Transition mockTransition(String id) {
            Transition mock = mock(Transition.class);
            when(mock.getId()).thenReturn(id);
            when(mock.toString()).thenReturn("transition: " + id);
            return mock;
        }

        List<Transition> executeTransitions(Transition transition) {
            status = ShootingTicketStatus.fromTransitionKey(transition.getId()).getIssueKey();
            return getTransitions();
        }

        Issue createMock() {
            Issue mock = mock(Issue.class);
            when(mock.getKey()).thenReturn(key);
            when(mock.getStatus()).then(inv -> getStatus());
            when(mock.getTransitions()).then(inv -> new ArrayListF<>(getTransitions()));
            when(mock.executeTransition(any(Transition.class))).then(
                    inv -> new ArrayListF<>(executeTransitions(inv.getArgument(0))));
            return mock;
        }
    }

    public interface IssuesTracker {
        void onSave();
        Map<String, Issue> getStore();
    }
}
