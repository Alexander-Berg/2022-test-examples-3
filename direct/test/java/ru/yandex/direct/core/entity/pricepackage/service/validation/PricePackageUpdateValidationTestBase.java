package ru.yandex.direct.core.entity.pricepackage.service.validation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageUpdateOperationFactory;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestUsers;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static ru.yandex.direct.core.testing.data.TestPricePackages.anotherPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.operation.Applicability.PARTIAL;

public class PricePackageUpdateValidationTestBase {

    @Autowired
    protected Steps steps;

    @Autowired
    private PricePackageUpdateOperationFactory updateOperationFactory;

    protected User priceManager;
    protected User priceApprover;
    protected User support;

    @Before
    public void before() {
        priceManager = TestUsers.generateNewUser()
                .withCanManagePricePackages(true)
                .withUid(1L);
        priceApprover = TestUsers.generateNewUser()
                .withCanApprovePricePackages(true)
                .withUid(2L);
        support = TestUsers.generateNewUser()
                .withRole(RbacRole.SUPPORT)
                .withUid(3L);
        steps.sspPlatformsSteps().addSspPlatforms(defaultPricePackage().getAllowedSsp());
        steps.sspPlatformsSteps().addSspPlatforms(anotherPricePackage().getAllowedSsp());
    }

    protected PricePackage activePricePackageWithStatusApprove(StatusApprove statusApprove) {
        return steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                        .withStatusApprove(statusApprove)
                        .withIsArchived(false)
                        .withCurrency(CurrencyCode.RUB))
                .getPricePackage();
    }

    protected PricePackage activePricePackageWithIsArchived(boolean isArchived) {
        return steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                        .withStatusApprove(StatusApprove.YES)
                        .withIsArchived(isArchived)
                        .withCurrency(CurrencyCode.RUB))
                .getPricePackage();
    }

    protected PricePackage activePricePackageWithTargetingsCustom(TargetingsCustom targetingsCustom) {
        return steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                        .withStatusApprove(StatusApprove.NEW)
                        .withIsArchived(false)
                        .withCurrency(CurrencyCode.RUB)
                        .withTargetingsCustom(targetingsCustom))
                .getPricePackage();
    }

    protected Optional<MassResult<Long>> validate(User operator, ModelChanges<PricePackage> modelChanges,
                                                  PricePackage pricePackage) {
        return validate(operator, modelChanges, pricePackage.getLastUpdateTime());
    }

    protected Optional<MassResult<Long>> validate(User operator, ModelChanges<PricePackage> modelChanges,
                                                  LocalDateTime lastUpdateTime) {
        return validate(operator, List.of(modelChanges), List.of(lastUpdateTime));
    }

    protected Optional<MassResult<Long>> validate(User operator, List<ModelChanges<PricePackage>> modelChanges,
                                                  List<LocalDateTime> lastUpdateTime) {
        return updateOperationFactory.newInstance(PARTIAL, modelChanges, lastUpdateTime, operator)
                .prepare();
    }

}
