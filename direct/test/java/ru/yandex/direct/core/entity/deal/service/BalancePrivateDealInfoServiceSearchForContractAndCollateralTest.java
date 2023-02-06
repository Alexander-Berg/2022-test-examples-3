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
import ru.yandex.direct.core.entity.deal.model.BalanceCollateralProblem;
import ru.yandex.direct.core.entity.deal.model.BalanceContractProblem;
import ru.yandex.direct.core.entity.deal.model.BalancePrivateDealInfo;
import ru.yandex.direct.core.entity.deal.model.BalancePrivateDealSearchResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
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
public class BalancePrivateDealInfoServiceSearchForContractAndCollateralTest {
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

        BalancePrivateDealSearchResult searchResult = service.searchForContractAndCollateral(CLIENT_ID);
        BalancePrivateDealInfo balancePrivateDealInfo = searchResult.getBalancePrivateDealInfo();
        assertThat(balancePrivateDealInfo).isNotNull();
        assertSoftly(softly -> {
            softly.assertThat(balancePrivateDealInfo.getExternalContractId()).isEqualTo(EXTERNAL_CONTRACT_ID);
            softly.assertThat(balancePrivateDealInfo.getContractStartDate()).isEqualTo(LocalDate.now());
            softly.assertThat(balancePrivateDealInfo.getExternalCollateralId()).isEqualTo(EXTERNAL_COLLATERAL_ID);
            softly.assertThat(balancePrivateDealInfo.getDealBasePercent()).isEqualByComparingTo(PERSONAL_DEAL_BASE_PERCENT);
        });

        verifyNoMoreInteractions(balanceClient);
    }

    @Test
    public void missingContract() {
        expectBalanceResponse(emptyList());

        BalancePrivateDealSearchResult searchResult = service.searchForContractAndCollateral(CLIENT_ID);
        assertSoftly(softly -> {
            softly.assertThat(searchResult.getContractProblems()).isEmpty();
            softly.assertThat(searchResult.getSuitableContractCount()).isEqualTo(0);
            softly.assertThat(searchResult.getRelevantContractInfo()).isNull();
            softly.assertThat(searchResult.foundContractAndCollateral()).isFalse();
            softly.assertThat(searchResult.getBalancePrivateDealInfo()).isNull();
        });

        verifyNoMoreInteractions(balanceClient);
    }

    @Test
    public void invalidContract() {
        expectBalanceResponse(constructContract(contract -> contract.setDateOfCancellation(LocalDate.now())),
                constructDefaultCollateral());

        BalancePrivateDealSearchResult searchResult = service.searchForContractAndCollateral(CLIENT_ID);
        assertSoftly(softly -> {
            softly.assertThat(searchResult.getContractProblems())
                    .isEqualTo(singletonList(singleton(BalanceContractProblem.CANCELLED)));
            softly.assertThat(searchResult.getSuitableContractCount()).isEqualTo(0);
            softly.assertThat(searchResult.getRelevantContractInfo()).isNull();
            softly.assertThat(searchResult.foundContractAndCollateral()).isFalse();
            softly.assertThat(searchResult.getBalancePrivateDealInfo()).isNull();
        });

        verifyNoMoreInteractions(balanceClient);
    }

    @Test
    public void twoValidContracts() {
        expectBalanceResponse(asList(
                Pair.of(constructDefaultContract(), singletonList(constructDefaultCollateral())),
                Pair.of(constructDefaultContract(), singletonList(constructDefaultCollateral()))));

        BalancePrivateDealSearchResult searchResult = service.searchForContractAndCollateral(CLIENT_ID);
        assertSoftly(softly -> {
            softly.assertThat(searchResult.getContractProblems()).isEqualTo(asList(emptySet(), emptySet()));
            softly.assertThat(searchResult.getSuitableContractCount()).isEqualTo(2);
            softly.assertThat(searchResult.getRelevantContractInfo()).isNull();
            softly.assertThat(searchResult.foundContractAndCollateral()).isFalse();
            softly.assertThat(searchResult.getBalancePrivateDealInfo()).isNull();
        });

        verifyNoMoreInteractions(balanceClient);
    }

    @Test
    public void contractHasWrongCurrency() {
        expectBalanceResponse(constructContract(contract -> contract.setCurrency(100500L)), constructDefaultCollateral());

        BalancePrivateDealSearchResult searchResult = service.searchForContractAndCollateral(CLIENT_ID);
        assertSoftly(softly -> {
            softly.assertThat(searchResult.getContractProblems()).isEqualTo(singletonList(emptySet()));
            softly.assertThat(searchResult.getSuitableContractCount()).isEqualTo(1);
            softly.assertThat(searchResult.getRelevantContractInfo()).isNotNull();
            softly.assertThat(searchResult.getRelevantContractHasCorrectCurrency()).isFalse();
            softly.assertThat(searchResult.foundContractAndCollateral()).isFalse();
            softly.assertThat(searchResult.getBalancePrivateDealInfo()).isNull();
        });

        verifyNoMoreInteractions(balanceClient);
    }

    @Test
    public void contractHasNullCurrency() {
        expectBalanceResponse(constructContract(contract -> contract.setCurrency(null)), constructDefaultCollateral());

        BalancePrivateDealSearchResult searchResult = service.searchForContractAndCollateral(CLIENT_ID);
        assertSoftly(softly -> {
            softly.assertThat(searchResult.getContractProblems()).isEqualTo(singletonList(emptySet()));
            softly.assertThat(searchResult.getSuitableContractCount()).isEqualTo(1);
            softly.assertThat(searchResult.getRelevantContractInfo()).isNotNull();
            softly.assertThat(searchResult.getRelevantContractHasCorrectCurrency()).isFalse();
            softly.assertThat(searchResult.foundContractAndCollateral()).isFalse();
            softly.assertThat(searchResult.getBalancePrivateDealInfo()).isNull();
        });

        verifyNoMoreInteractions(balanceClient);
    }

    @Test
    public void twoContracts_oneValid() {
        expectBalanceResponse(asList(
                Pair.of(constructContract(contract -> contract.setDateOfCancellation(LocalDate.now())),
                        singletonList(constructDefaultCollateral())),
                Pair.of(constructDefaultContract(), singletonList(constructDefaultCollateral()))));

        BalancePrivateDealSearchResult searchResult = service.searchForContractAndCollateral(CLIENT_ID);
        assertSoftly(softly -> {
            softly.assertThat(searchResult.getContractProblems())
                    .isEqualTo(asList(singleton(BalanceContractProblem.CANCELLED), emptySet()));

            softly.assertThat(searchResult.getSuitableContractCount()).isEqualTo(1);
            softly.assertThat(searchResult.getRelevantContractInfo()).isNotNull();
            softly.assertThat(searchResult.foundContractAndCollateral()).isTrue();

            BalancePrivateDealInfo balancePrivateDealInfo = searchResult.getBalancePrivateDealInfo();
            softly.assertThat(balancePrivateDealInfo).isNotNull();
            softly.assertThat(balancePrivateDealInfo.getExternalContractId()).isEqualTo(EXTERNAL_CONTRACT_ID);
            softly.assertThat(balancePrivateDealInfo.getContractStartDate()).isEqualTo(LocalDate.now());
            softly.assertThat(balancePrivateDealInfo.getExternalCollateralId()).isEqualTo(EXTERNAL_COLLATERAL_ID);
            softly.assertThat(balancePrivateDealInfo.getDealBasePercent()).isEqualByComparingTo(PERSONAL_DEAL_BASE_PERCENT);
        });

        verifyNoMoreInteractions(balanceClient);
    }

    @Test
    public void missingCollateral() {
        expectBalanceResponse(singletonList(Pair.of(constructDefaultContract(), emptyList())));

        BalancePrivateDealSearchResult searchResult = service.searchForContractAndCollateral(CLIENT_ID);
        assertSoftly(softly -> {
            softly.assertThat(searchResult.getContractProblems()).isEqualTo(singletonList(emptySet()));
            softly.assertThat(searchResult.getSuitableContractCount()).isEqualTo(1);
            softly.assertThat(searchResult.getRelevantContractInfo()).isNotNull();
            softly.assertThat(searchResult.getCollateralProblems()).isEmpty();
            softly.assertThat(searchResult.getSuitableCollateralCount()).isEqualTo(0);
            softly.assertThat(searchResult.foundContractAndCollateral()).isFalse();
            softly.assertThat(searchResult.getBalancePrivateDealInfo()).isNull();
        });

        verifyNoMoreInteractions(balanceClient);
    }

    @Test
    public void invalidCollateral() {
        expectBalanceResponse(constructDefaultContract(),
                constructCollateral(collateral -> collateral.setDateOfCancellation(LocalDate.now())));

        BalancePrivateDealSearchResult searchResult = service.searchForContractAndCollateral(CLIENT_ID);
        assertSoftly(softly -> {
            softly.assertThat(searchResult.getContractProblems()).isEqualTo(singletonList(emptySet()));
            softly.assertThat(searchResult.getSuitableContractCount()).isEqualTo(1);
            softly.assertThat(searchResult.getRelevantContractInfo()).isNotNull();
            softly.assertThat(searchResult.getCollateralProblems())
                    .isEqualTo(singletonList(singleton(BalanceCollateralProblem.CANCELLED)));
            softly.assertThat(searchResult.getSuitableCollateralCount()).isEqualTo(0);
            softly.assertThat(searchResult.foundContractAndCollateral()).isFalse();
            softly.assertThat(searchResult.getBalancePrivateDealInfo()).isNull();
        });

        verifyNoMoreInteractions(balanceClient);
    }

    @Test
    public void twoCollaterals_oneValid() {
        PartnerContractCollateralInfo validCollateral = constructDefaultCollateral();
        expectBalanceResponse(singletonList(Pair.of(constructDefaultContract(),
                asList(
                        validCollateral,
                        constructCollateral(collateral -> collateral.setDateOfCancellation(LocalDate.now()))))));

        BalancePrivateDealSearchResult searchResult = service.searchForContractAndCollateral(CLIENT_ID);
        assertSoftly(softly -> {
            softly.assertThat(searchResult.getContractProblems()).isEqualTo(singletonList(emptySet()));
            softly.assertThat(searchResult.getSuitableContractCount()).isEqualTo(1);
            softly.assertThat(searchResult.getRelevantContractInfo()).isNotNull();
            softly.assertThat(searchResult.getCollateralProblems())
                    .isEqualTo(asList(emptySet(), singleton(BalanceCollateralProblem.CANCELLED)));
            softly.assertThat(searchResult.getSuitableCollateralCount()).isEqualTo(1);
            softly.assertThat(searchResult.getRelevantCollateral()).isSameAs(validCollateral);
            softly.assertThat(searchResult.foundContractAndCollateral()).isTrue();
            softly.assertThat(searchResult.getBalancePrivateDealInfo()).isNotNull();
        });

        verifyNoMoreInteractions(balanceClient);
    }

    @Test
    public void twoValidCollaterals() {
        PartnerContractCollateralInfo validCollateral1 =
                constructCollateral(collateral -> collateral.setStartDate(LocalDate.now().minusDays(2)));
        PartnerContractCollateralInfo validCollateral2 =
                constructCollateral(collateral -> collateral.setStartDate(LocalDate.now().minusDays(1)));

        expectBalanceResponse(singletonList(Pair.of(constructDefaultContract(),
                asList(validCollateral1, validCollateral2))));

        BalancePrivateDealSearchResult searchResult = service.searchForContractAndCollateral(CLIENT_ID);
        assertSoftly(softly -> {
            softly.assertThat(searchResult.getContractProblems()).isEqualTo(singletonList(emptySet()));
            softly.assertThat(searchResult.getSuitableContractCount()).isEqualTo(1);
            softly.assertThat(searchResult.getRelevantContractInfo()).isNotNull();
            softly.assertThat(searchResult.getCollateralProblems()).isEqualTo(asList(emptySet(), emptySet()));
            softly.assertThat(searchResult.getSuitableCollateralCount()).isEqualTo(2);
            softly.assertThat(searchResult.getRelevantCollateral()).isNull();
            softly.assertThat(searchResult.foundContractAndCollateral()).isFalse();
            softly.assertThat(searchResult.getBalancePrivateDealInfo()).isNull();
        });

        verifyNoMoreInteractions(balanceClient);
    }

}
