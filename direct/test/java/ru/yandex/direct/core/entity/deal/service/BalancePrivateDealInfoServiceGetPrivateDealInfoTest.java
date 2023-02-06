package ru.yandex.direct.core.entity.deal.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.model.response.PartnerContractClientInfo;
import ru.yandex.direct.balance.client.model.response.PartnerContractCollateralInfo;
import ru.yandex.direct.balance.client.model.response.PartnerContractContractInfo;
import ru.yandex.direct.balance.client.model.response.PartnerContractInfo;
import ru.yandex.direct.balance.client.model.response.PartnerContractPersonInfo;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.deal.model.BalancePrivateDealInfo;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class BalancePrivateDealInfoServiceGetPrivateDealInfoTest {
    private static final Long CLIENT_ID = 123L;
    private static final String EXTERNAL_CONTRACT_ID = "2017/16";
    private static final String EXTERNAL_COLLATERAL_ID = "007";
    private static final BigDecimal PERSONAL_DEAL_BASE_PERCENT = BigDecimal.valueOf(15L);

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private DirectConfig directConfig;

    @Mock
    private BalanceClient balanceClient;

    private BalancePrivateDealInfoService service;

    private void expectBalanceResponse(
            List<Pair<PartnerContractContractInfo, List<PartnerContractCollateralInfo>>> results) {
        when(balanceClient.getPartnerContracts(eq(CLIENT_ID), isNull(), any()))
                .thenReturn(results.stream()
                        .map(pair -> new PartnerContractInfo(
                                new PartnerContractClientInfo(),
                                pair.getRight(),
                                pair.getLeft(),
                                new PartnerContractPersonInfo()))
                        .collect(toList()));
    }

    private void expectBalanceResponse(PartnerContractContractInfo contract,
                                       List<PartnerContractCollateralInfo> collaterals) {
        expectBalanceResponse(singletonList(Pair.of(contract, collaterals)));
    }

    private void expectBalanceResponse(PartnerContractContractInfo contract,
                                       PartnerContractCollateralInfo collateral) {
        expectBalanceResponse(contract, singletonList(collateral));
    }

    private static PartnerContractContractInfo constructContract(Consumer<PartnerContractContractInfo> initializer) {
        PartnerContractContractInfo result = new PartnerContractContractInfo();
        result.setExternalContractId(EXTERNAL_CONTRACT_ID);
        result.setServices(singleton(7L));
        result.setStartDate(LocalDate.now());
        result.setDateOfSigning(LocalDate.now());
        result.setCurrency(810L);
        initializer.accept(result);
        return result;
    }

    private PartnerContractContractInfo constructDefaultContract() {
        return constructContract(contract -> {
        });
    }

    private static PartnerContractCollateralInfo constructCollateral(
            Consumer<PartnerContractCollateralInfo> initializer) {
        PartnerContractCollateralInfo result = new PartnerContractCollateralInfo();
        result.setExternalCollateralId(EXTERNAL_COLLATERAL_ID);
        result.setCollateralTypeId(1070L);
        result.setStartDate(LocalDate.now());
        result.setDateOfSigning(LocalDate.now());
        result.setPersonalDealBasePercent(PERSONAL_DEAL_BASE_PERCENT.toString());
        initializer.accept(result);
        return result;
    }

    private PartnerContractCollateralInfo constructDefaultCollateral() {
        return constructCollateral(collateral -> {
        });
    }

    @Before
    public void setUp() {
        when(directConfig.getDuration("balance.get_partner_contracts_request_timeout"))
                .thenReturn(Duration.ofSeconds(300));
        service = new BalancePrivateDealInfoService(balanceClient, 7L, 1070L, 810L, directConfig);
    }

    @Test
    public void everythingFine() {
        expectBalanceResponse(constructDefaultContract(), constructDefaultCollateral());

        BalancePrivateDealInfo balancePrivateDealInfo = service.getBalancePrivateDealInfo(CLIENT_ID);
        assertThat(balancePrivateDealInfo).isNotNull();
        assertSoftly(softly -> {
            softly.assertThat(balancePrivateDealInfo.getExternalContractId()).isEqualTo(EXTERNAL_CONTRACT_ID);
            softly.assertThat(balancePrivateDealInfo.getContractStartDate()).isEqualTo(LocalDate.now());
            softly.assertThat(balancePrivateDealInfo.getExternalCollateralId()).isEqualTo(EXTERNAL_COLLATERAL_ID);
            softly.assertThat(balancePrivateDealInfo.getDealBasePercent()).isEqualTo(PERSONAL_DEAL_BASE_PERCENT);
        });

        verifyNoMoreInteractions(balanceClient);
    }

    @Test(expected = IllegalStateException.class)
    public void missingContract() {
        expectBalanceResponse(emptyList());
        service.getBalancePrivateDealInfo(CLIENT_ID);
    }

}
