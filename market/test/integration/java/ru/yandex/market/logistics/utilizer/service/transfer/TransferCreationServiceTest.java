package ru.yandex.market.logistics.utilizer.service.transfer;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.ff.client.dto.CreateTransferForm;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTOContainer;
import ru.yandex.market.ff.client.dto.ShopRequestFilterDTO;
import ru.yandex.market.ff.client.dto.TransferDetailsDTO;
import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.repo.UtilizationTransferJpaRepository;

import static org.mockito.ArgumentMatchers.any;

public class TransferCreationServiceTest extends AbstractContextualTest {

    @Autowired
    private TransferCreationService transferCreationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UtilizationTransferJpaRepository utilizationTransferRepository;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/transfer-creation/1/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/transfer-creation/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void allTransfersCreatedAndSavedInUtilizer() {
        runInExternalTransaction(() -> transferCreationService.createUtilizationTransfers(1), false);
        Mockito.verifyZeroInteractions(fulfillmentWorkflowClientApi);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/transfer-creation/2/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/transfer-creation/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void allTransfersCreatedInFfwfButNotSavedInUtilizer() {
        ArgumentCaptor<ShopRequestFilterDTO> filterCaptor = ArgumentCaptor.forClass(ShopRequestFilterDTO.class);
        ShopRequestDTOContainer getResponse = new ShopRequestDTOContainer(1, 1, 3);
        getResponse.addRequest(createFfwfRequestDto("1", 444444));
        getResponse.addRequest(createFfwfRequestDto("2", 444445));
        getResponse.addRequest(createFfwfRequestDto("3", 444446));
        Mockito.when(fulfillmentWorkflowClientApi.getRequests(any())).thenReturn(getResponse);

        runInExternalTransaction(() -> transferCreationService.createUtilizationTransfers(1), false);

        Mockito.verify(fulfillmentWorkflowClientApi).getRequests(filterCaptor.capture());
        ShopRequestFilterDTO filter = filterCaptor.getValue();
        softly.assertThat(filter.getTypes()).isEqualTo(List.of(1203));
        Mockito.verify(fulfillmentWorkflowClientApi, Mockito.never()).createTransferRequest(any());
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/transfer-creation/3/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/transfer-creation/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void savedInUtilizerAfterExceptionWhenSendingToFfwf() {
        Mockito.when(fulfillmentWorkflowClientApi.getRequests(any())).thenReturn(new ShopRequestDTOContainer(1, 1, 0));
        Mockito.when(fulfillmentWorkflowClientApi.createTransferRequest(any())).thenThrow(new RuntimeException());
        runInExternalTransaction(() -> transferCreationService.createUtilizationTransfers(1), true);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/transfer-creation/4/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/transfer-creation/4/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void someTransfersShouldBeSentToFfwf() {

        jdbcTemplate.execute("alter sequence utilization_transfer_id_seq restart with 3");

        ShopRequestDTOContainer getResponse = new ShopRequestDTOContainer(1, 1, 2);
        getResponse.addRequest(createFfwfRequestDto("1", 444444));
        getResponse.addRequest(createFfwfRequestDto("2", 444445));
        Mockito.when(fulfillmentWorkflowClientApi.getRequests(any())).thenReturn(getResponse);
        Mockito.when(fulfillmentWorkflowClientApi.createTransferRequest(any())).thenAnswer(invocation -> {
            CreateTransferForm createTransferForm = invocation.getArgument(0);
            TransferDetailsDTO response = new TransferDetailsDTO();
            response.setTransferId(444444 + Long.parseLong(createTransferForm.getExternalOperationId()));
            return response;
        });

        runInExternalTransaction(() -> transferCreationService.createUtilizationTransfers(1), false);

        Mockito.verify(fulfillmentWorkflowClientApi, Mockito.times(3)).createTransferRequest(any());

        utilizationTransferRepository.findAll()
                .forEach(transfer -> softly.assertThat(transfer.getTransferId()).isNotNull());
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/transfer-creation/5/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/transfer-creation/5/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void retryWhenTransfersFailedOnFirstAttempt() {
        TransferDetailsDTO createdTransfer = new TransferDetailsDTO();
        createdTransfer.setTransferId(444444L);
        Mockito.when(fulfillmentWorkflowClientApi.getRequests(any())).thenReturn(new ShopRequestDTOContainer());
        Mockito.when(fulfillmentWorkflowClientApi.createTransferRequest(any())).thenReturn(createdTransfer);

        runInExternalTransaction(() -> transferCreationService.createUtilizationTransfers(1), false);

        Mockito.verify(fulfillmentWorkflowClientApi).createTransferRequest(any());
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/transfer-creation/6/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/transfer-creation/6/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void transferNotSentInCaseCountForAllItemsIsZero() {
        runInExternalTransaction(() -> transferCreationService.createUtilizationTransfers(1), false);

        Mockito.verify(fulfillmentWorkflowClientApi, Mockito.never()).createTransferRequest(any());
        Mockito.verify(fulfillmentWorkflowClientApi, Mockito.never()).getRequests(any());
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/transfer-creation/7/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/transfer-creation/7/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void createTransferWhenThereIsDifferentStockTypeInOtherUtilizationCycle() {
        TransferDetailsDTO createdTransfer = new TransferDetailsDTO();
        createdTransfer.setTransferId(444444L);
        Mockito.when(fulfillmentWorkflowClientApi.getRequests(any())).thenReturn(new ShopRequestDTOContainer());
        Mockito.when(fulfillmentWorkflowClientApi.createTransferRequest(any())).thenReturn(createdTransfer);

        runInExternalTransaction(() -> transferCreationService.createUtilizationTransfers(1), false);

        Mockito.verify(fulfillmentWorkflowClientApi).createTransferRequest(any());
    }

    private ShopRequestDTO createFfwfRequestDto(String externalRequestId,
                                                long ffwfId) {
        ShopRequestDTO request = new ShopRequestDTO();
        request.setId(ffwfId);
        request.setExternalRequestId(externalRequestId);
        return request;
    }
}
