package ru.yandex.direct.core.entity.deal.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
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

import ru.yandex.direct.balance.client.model.response.PartnerContractClientInfo;
import ru.yandex.direct.balance.client.model.response.PartnerContractCollateralInfo;
import ru.yandex.direct.balance.client.model.response.PartnerContractContractInfo;
import ru.yandex.direct.balance.client.model.response.PartnerContractInfo;
import ru.yandex.direct.balance.client.model.response.PartnerContractPersonInfo;
import ru.yandex.direct.core.entity.deal.model.BalanceContractProblem;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.deal.model.BalanceContractProblem.CANCELLED;
import static ru.yandex.direct.core.entity.deal.model.BalanceContractProblem.EXPIRED;
import static ru.yandex.direct.core.entity.deal.model.BalanceContractProblem.NOT_RELATED_TO_DIRECT;
import static ru.yandex.direct.core.entity.deal.model.BalanceContractProblem.NOT_SIGNED;
import static ru.yandex.direct.core.entity.deal.model.BalanceContractProblem.NOT_STARTED_YET;
import static ru.yandex.direct.core.entity.deal.model.BalanceContractProblem.SUSPENDED;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(Parameterized.class)
public class BalancePrivateDealInfoServiceFindContractProblemsTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private BalancePrivateDealInfoService service;

    private PartnerContractInfo contractInfo;
    private Set<BalanceContractProblem> expectedContractProblems;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"OK", constructContractInfo(contract -> {
                }), emptySet()},

                // NOT_RELATED_TO_DIRECT
                {"NOT_RELATED_TO_DIRECT: empty services",
                        constructContractInfo(contract -> contract.setServices(emptySet())),
                        singleton(NOT_RELATED_TO_DIRECT)},
                {"NOT_RELATED_TO_DIRECT: different service",
                        constructContractInfo(contract -> contract.setServices(singleton(100500L))),
                        singleton(NOT_RELATED_TO_DIRECT)},
                {"NOT_RELATED_TO_DIRECT: Direct + different service",
                        constructContractInfo(contract -> contract.setServices(new HashSet<>(asList(7L, 100500L)))),
                        emptySet()},

                // NOT_STARTED_YET
                {"NOT_STARTED_YET: starts yesterday",
                        constructContractInfo(contract -> contract.setStartDate(LocalDate.now().minusDays(1))),
                        emptySet()},
                {"NOT_STARTED_YET: starts today",
                        constructContractInfo(contract -> contract.setStartDate(LocalDate.now())),
                        emptySet()},
                {"NOT_STARTED_YET: starts tomorrow",
                        constructContractInfo(contract -> contract.setStartDate(dateInFuture())),
                        singleton(NOT_STARTED_YET)},

                // EXPIRED
                {"EXPIRED: expired yesterday",
                        constructContractInfo(contract -> contract.setFinishDate(LocalDate.now().minusDays(1))),
                        singleton(EXPIRED)},
                {"EXPIRED: expires today",
                        constructContractInfo(contract -> contract.setFinishDate(LocalDate.now())),
                        singleton(EXPIRED)},
                {"EXPIRED: expires tomorrow",
                        constructContractInfo(contract -> contract.setFinishDate(dateInFuture())),
                        emptySet()},
                {"EXPIRED: expired yesterday, prolonged",
                        constructContractInfoWithCollateral(
                                contract -> contract.setFinishDate(LocalDate.now().minusDays(1)),
                                collateral -> collateral.setFinishDate(dateInFuture())),
                        emptySet()},

                // NOT_SIGNED
                {"NOT_SIGNED: signed and signed by fax",
                        constructContractInfo(contract -> {
                            contract.setDateOfSigning(LocalDate.now());
                            contract.setDateOfSigningByFax(LocalDate.now());
                        }),
                        emptySet()},
                {"NOT_SIGNED: signed, not signed by fax",
                        constructContractInfo(contract -> {
                            contract.setDateOfSigning(LocalDate.now());
                            contract.setDateOfSigningByFax((LocalDate) null);
                        }),
                        emptySet()},
                {"NOT_SIGNED: not signed, signed by fax",
                        constructContractInfo(contract -> {
                            contract.setDateOfSigning((LocalDate) null);
                            contract.setDateOfSigningByFax(LocalDate.now());
                        }),
                        emptySet()},
                {"NOT_SIGNED: not signed, not signed by fax",
                        constructContractInfo(contract -> {
                            contract.setDateOfSigning((LocalDate) null);
                            contract.setDateOfSigningByFax((LocalDate) null);
                        }),
                        singleton(NOT_SIGNED)},

                // CANCELLED
                {"CANCELLED: not cancelled",
                        constructContractInfo(contract -> contract.setDateOfCancellation((LocalDate) null)),
                        emptySet()},
                {"CANCELLED: cancelled",
                        constructContractInfo(contract -> contract.setDateOfCancellation(LocalDate.now())),
                        singleton(CANCELLED)},

                // SUSPENDED
                {"SUSPENDED: not suspended",
                        constructContractInfo(contract -> contract.setDateOfSuspension((LocalDate) null)),
                        emptySet()},
                {"SUSPENDED: suspended",
                        constructContractInfo(contract -> contract.setDateOfSuspension(LocalDate.now())),
                        singleton(SUSPENDED)},

        });
    }

    private static PartnerContractInfo constructContractInfo(Consumer<PartnerContractContractInfo> initializer) {
        return new PartnerContractInfo(
                new PartnerContractClientInfo(),
                emptyList(),
                constructContract(initializer),
                new PartnerContractPersonInfo());
    }

    /**
     * Возвращает дату в будущем
     */
    private static LocalDate dateInFuture() {
        // Прибавляем 2 дня, а не 1, чтобы не мигать, если тесты инициализируются близко к 00:00 и долго бегут
        return LocalDate.now().plusDays(2);
    }

    private static PartnerContractContractInfo constructContract(Consumer<PartnerContractContractInfo> initializer) {
        PartnerContractContractInfo result = new PartnerContractContractInfo();
        result.setServices(singleton(7L));
        result.setStartDate(LocalDate.now());
        result.setDateOfSigning(LocalDate.now());
        result.setCurrency(810L);
        initializer.accept(result);

        return result;
    }

    private static PartnerContractInfo constructContractInfoWithCollateral(
            Consumer<PartnerContractContractInfo> contractInitializer,
            Consumer<PartnerContractCollateralInfo> collateralInitializer) {
        PartnerContractCollateralInfo collateral = new PartnerContractCollateralInfo();
        collateral.setStartDate(LocalDate.now());
        collateral.setDateOfSigning(LocalDate.now());
        collateralInitializer.accept(collateral);

        return new PartnerContractInfo(
                new PartnerContractClientInfo(),
                singletonList(collateral),
                constructContract(contractInitializer),
                new PartnerContractPersonInfo());

    }

    public BalancePrivateDealInfoServiceFindContractProblemsTest(
            @SuppressWarnings("unused") String description,
            PartnerContractInfo contractInfo,
            Set<BalanceContractProblem> expectedContractProblems) {
        this.contractInfo = contractInfo;
        this.expectedContractProblems = expectedContractProblems;
    }

    @Test
    public void findContractProblems() {
        Set<BalanceContractProblem> contractProblems = service.findContractProblems(contractInfo);
        assertThat(contractProblems).isEqualTo(expectedContractProblems);
    }
}
