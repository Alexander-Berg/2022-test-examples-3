package ru.yandex.direct.core.entity.deal.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.balance.client.model.response.PartnerContractCollateralInfo;
import ru.yandex.direct.core.entity.deal.model.BalanceCollateralProblem;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.deal.model.BalanceCollateralProblem.CANCELLED;
import static ru.yandex.direct.core.entity.deal.model.BalanceCollateralProblem.NOT_SIGNED;
import static ru.yandex.direct.core.entity.deal.model.BalanceCollateralProblem.NOT_STARTED_YET;
import static ru.yandex.direct.core.entity.deal.model.BalanceCollateralProblem.WRONG_TYPE;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(Parameterized.class)
public class BalancePrivateDealInfoServiceFindCollateralProblemsTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private BalancePrivateDealInfoService service;

    private PartnerContractCollateralInfo balanceCollateral;
    private Set<BalanceCollateralProblem> expectedCollateralProblems;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"OK", constructCollateral(collateral -> {
                }), emptySet()},

                // WRONG_TYPE
                {"WRONG_TYPE: correct type",
                        constructCollateral(collateral -> collateral.setCollateralTypeId(1070L)),
                        emptySet()},
                {"WRONG_TYPE: another type",
                        constructCollateral(collateral -> collateral.setCollateralTypeId(100500L)),
                        singleton(WRONG_TYPE)},

                // NOT_STARTED_YET
                {"NOT_STARTED_YET: starts yesterday",
                        constructCollateral(contract -> contract.setStartDate(LocalDate.now().minusDays(1))),
                        emptySet()},
                {"NOT_STARTED_YET: starts today",
                        constructCollateral(contract -> contract.setStartDate(LocalDate.now())),
                        emptySet()},
                {"NOT_STARTED_YET: starts in future",
                        constructCollateral(contract -> contract.setStartDate(dateInFuture())),
                        singleton(NOT_STARTED_YET)},

                // NOT_SIGNED
                {"NOT_SIGNED: signed and signed by fax",
                        constructCollateral(contract -> {
                            contract.setDateOfSigning(LocalDate.now());
                            contract.setDateOfSigningByFax(LocalDate.now());
                        }),
                        emptySet()},
                {"NOT_SIGNED: signed, not signed by fax",
                        constructCollateral(contract -> {
                            contract.setDateOfSigning(LocalDate.now());
                            contract.setDateOfSigningByFax((LocalDate) null);
                        }),
                        emptySet()},
                {"NOT_SIGNED: not signed, signed by fax",
                        constructCollateral(contract -> {
                            contract.setDateOfSigning((LocalDate) null);
                            contract.setDateOfSigningByFax(LocalDate.now());
                        }),
                        emptySet()},
                {"NOT_SIGNED: not signed, not signed by fax",
                        constructCollateral(contract -> {
                            contract.setDateOfSigning((LocalDate) null);
                            contract.setDateOfSigningByFax((LocalDate) null);
                        }),
                        singleton(NOT_SIGNED)},

                // CANCELLED
                {"CANCELLED: not cancelled",
                        constructCollateral(contract -> contract.setDateOfCancellation((LocalDate) null)),
                        emptySet()},
                {"CANCELLED: cancelled",
                        constructCollateral(contract -> contract.setDateOfCancellation(LocalDate.now())),
                        singleton(CANCELLED)},
        });
    }

    /**
     * Возвращает дату в будущем
     */
    private static LocalDate dateInFuture() {
        // Прибавляем 2 дня, а не 1, чтобы не мигать, если тесты инициализируются близко к 00:00 и долго бегут
        return LocalDate.now().plusDays(2);
    }

    private static PartnerContractCollateralInfo constructCollateral(
            Consumer<PartnerContractCollateralInfo> initializer) {
        PartnerContractCollateralInfo result = new PartnerContractCollateralInfo();
        result.setCollateralTypeId(1070L);
        result.setStartDate(LocalDate.now());
        result.setDateOfSigning(LocalDate.now());
        initializer.accept(result);
        return result;
    }

    public BalancePrivateDealInfoServiceFindCollateralProblemsTest(
            @SuppressWarnings("unused") String description,
            PartnerContractCollateralInfo balanceCollateral,
            Set<BalanceCollateralProblem> expectedCollateralProblems) {
        this.balanceCollateral = balanceCollateral;
        this.expectedCollateralProblems = expectedCollateralProblems;
    }

    @Test
    public void findContractProblems() {
        Set<BalanceCollateralProblem> collateralProblems = service.findCollateralProblems(balanceCollateral);
        assertThat(collateralProblems).isEqualTo(expectedCollateralProblems);
    }
}
