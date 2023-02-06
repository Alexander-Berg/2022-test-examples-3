package ru.yandex.market.tpl.carrier.tms.dbqueue.account;

import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.CarrierUserQueryService;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserFacade;
import ru.yandex.market.tpl.carrier.core.domain.user.UserSource;
import ru.yandex.market.tpl.carrier.core.domain.user.data.UserData;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.mj.generated.client.taximeter_v2.api.TaximeterV2ApiClient;
import ru.yandex.mj.generated.client.taximeter_v2.model.ContractorCreateResponse;
import ru.yandex.mj.generated.client.taximeter_v2.model.ContractorUpdateResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.carrier.core.domain.company.Company.DEFAULT_COMPANY_NAME;

@TmsIntTest
@RequiredArgsConstructor(onConstructor_=@Autowired)
public class SyncWithTaxiAccountServiceTest {

    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserFacade userFacade;
    private final TestUserHelper testUserHelper;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final CarrierUserQueryService carrierUserQueryService;

    @Autowired
    private TaximeterV2ApiClient taximeterApiClient;

    private Company company;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.TAXIMETER_USER_SYNC_VERSION, 2);

        company = testUserHelper.findOrCreateCompany(DEFAULT_COMPANY_NAME);
    }

    @Test
    void shouldPersistTaxiIdInUser() {
        // create user
        String phone = "+79272403522";
        var userData = UserData.builder()
                .phone(phone)
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .source(UserSource.CARRIER)
                .build();
        User createdUser = userFacade.createUser(userData, company);

        // sync with taxi
        String expectedYaProParkId = "PARK-ID-1";
        String expectedYaProDriverId = "DRIVER-ID-1";
        String expectedTaxiId = expectedYaProParkId + "_" + expectedYaProDriverId;

        ExecuteCall<ContractorCreateResponse, RetryStrategy> callCreate = Mockito.mock(ExecuteCall.class);
        Mockito.when(callCreate.schedule())
                .thenReturn(CompletableFuture.completedFuture(new ContractorCreateResponse().contractorId(expectedTaxiId)));

        String idempotencyToken = createdUser.getDsmId();
        Mockito.when(taximeterApiClient.internalV1PlatformContractorsPost(
                        Mockito.anyString(),
                        Mockito.eq(idempotencyToken),
                        Mockito.any())
                )
                .thenReturn(callCreate);

        dbQueueTestUtil.assertQueueHasSize(QueueType.SYNC_WITH_TAXI_ACCOUNT, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.SYNC_WITH_TAXI_ACCOUNT);
        dbQueueTestUtil.clear(QueueType.SYNC_WITH_TAXI_ACCOUNT);

        // check user receive taxi id
        var withNewTaxiId = carrierUserQueryService.findByIdOrThrow(createdUser.getId());
        assertThat(withNewTaxiId.getTaxiId()).isEqualTo(expectedTaxiId);
        assertThat(withNewTaxiId.getYaProParkId()).isEqualTo(expectedYaProParkId);
        assertThat(withNewTaxiId.getYaProDriverId()).isEqualTo(expectedYaProDriverId);

        // update phone on existing user
        User updatedUser = userFacade.updateUser(
                createdUser.getId(),
                UserData.builder()
                        .phone("+79851234567")
                        .firstName("Ололош")
                        .lastName("Ололоев")
                        .patronymic("Ололоевич")
                        .source(UserSource.CARRIER)
                        .build(),
                company
        );

        // check sync task
        ExecuteCall<ContractorUpdateResponse, RetryStrategy> callCreate2 = Mockito.mock(ExecuteCall.class);
        Mockito.when(callCreate2.schedule())
                .thenReturn(CompletableFuture.completedFuture(new ContractorUpdateResponse().contractorId(expectedTaxiId)));

        Mockito.when(taximeterApiClient.internalV1PlatformContractorsPut(Mockito.anyString(), Mockito.eq(expectedTaxiId), Mockito.any()))
                .thenReturn(callCreate2);

        dbQueueTestUtil.assertQueueHasSize(QueueType.SYNC_WITH_TAXI_ACCOUNT, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.SYNC_WITH_TAXI_ACCOUNT);

    }
}
