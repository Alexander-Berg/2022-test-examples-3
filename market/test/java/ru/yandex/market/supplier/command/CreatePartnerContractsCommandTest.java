package ru.yandex.market.supplier.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.api.cpa.checkout.AsyncCheckouterService;
import ru.yandex.market.api.cpa.yam.dao.PrepayRequestDao;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.partner.contract.PartnerContractService;
import ru.yandex.market.core.protocol.MockProtocolService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class CreatePartnerContractsCommandTest {

    @Mock
    private PartnerContractService supplierContractService;

    @Mock
    private AsyncCheckouterService asyncCheckouterService;

    @Mock
    private PrepayRequestDao prepayRequestDao;

    private CreateSupplierContractsCommand createSupplierContractsCommand;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(prepayRequestDao.find(any())).thenReturn(allRequests());

        createSupplierContractsCommand = new CreateSupplierContractsCommand(
                new MockProtocolService(),
                supplierContractService,
                prepayRequestDao,
                asyncCheckouterService);

    }

    @Test
    void shouldCreateMissingContracts() {
        CommandInvocation commandInvocation = new CommandInvocation("create-supplier-contracts",
                new String[]{"all"},
                Collections.emptyMap());
        createSupplierContractsCommand.executeCommand(commandInvocation,
                mock(Terminal.class, withSettings().defaultAnswer(RETURNS_MOCKS)));

        verify(supplierContractService, times(2)).createContracts(
                anyLong(), anyLong(), any(PrepayRequest.class), eq(false));
        verify(asyncCheckouterService).pushPartnerSettingsToCheckout(eq(requestsToHandle().stream()
                .map(PrepayRequest::getDatasourceId)
                .collect(Collectors.toSet())));
    }

    private List<PrepayRequest> requestsToHandle() {
        return ImmutableList.of(
                new PrepayRequest(1, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.COMPLETED, 1),
                new PrepayRequest(2, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.COMPLETED, 2)
        );
    }

    private List<PrepayRequest> allRequests() {
        PrepayRequest request = new PrepayRequest(3, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.COMPLETED, 3);
        request.setSellerClientId(1L);
        request.setPersonId(1L);
        request.setContractId(1L);

        List<PrepayRequest> allRequests = new ArrayList<>();
        allRequests.add(request);
        allRequests.addAll(requestsToHandle());

        return allRequests;
    }
}
