package ru.yandex.direct.core.entity.pricepackage.service.validation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jdk.jfr.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.pricepackage.service.validation.defects.PricePackageDefects.pricePackageIsExpired;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PricePackageUpdateValidationClientsTest extends PricePackageUpdateValidationTestBase {

    private ClientInfo client;

    @Before
    public void before() {
        super.before();
        client = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
    }

    @Test
    public void managerCanNotChangeNewPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NEW);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceManager, modelChanges, pricePackage);
        assertForbiddenToChangeDefects(result);
    }

    @Test
    public void managerCanNotChangeWaitingPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.WAITING);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceManager, modelChanges, pricePackage);
        assertForbiddenToChangeDefects(result);
    }

    @Test
    public void managerCanNotChangeRejectedPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NO);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceManager, modelChanges, pricePackage);
        assertForbiddenToChangeDefects(result);
    }

    @Test
    @Description("Менеджер может привязать клиентов, если пакет заапрувлен")
    public void managerCanChangeApprovedPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.YES);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceManager, modelChanges, pricePackage);
        assertNoDefects(result);
    }

    @Test
    public void approverCanNotChangeNewPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NEW);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceApprover, modelChanges, pricePackage);
        assertForbiddenToChangeDefects(result);
    }

    @Test
    public void approverCanNotChangeWaitingPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.WAITING);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceApprover, modelChanges, pricePackage);
        assertForbiddenToChangeDefects(result);
    }

    @Test
    public void approverCanNotChangeRejectedPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NO);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceApprover, modelChanges, pricePackage);
        assertForbiddenToChangeDefects(result);
    }

    @Test
    public void approverCanNotChangeApprovedPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.YES);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceApprover, modelChanges, pricePackage);
        assertForbiddenToChangeDefects(result);
    }

    @Test
    public void supportCanNotChangeNewPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NEW);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(support, modelChanges, pricePackage);
        assertForbiddenToChangeDefects(result);
    }

    @Test
    public void supportCanNotChangeWaitingPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.WAITING);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(support, modelChanges, pricePackage);
        assertForbiddenToChangeDefects(result);
    }

    @Test
    public void supportCanNotChangeRejectedPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.NO);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(support, modelChanges, pricePackage);
        assertForbiddenToChangeDefects(result);
    }

    @Test
    @Description("Саппорт может привязать клиентов, если пакет заапрувлен")
    public void supportCanChangeApprovedPackage() {
        var pricePackage = activePricePackageWithStatusApprove(StatusApprove.YES);
        var modelChanges = modelChanges(pricePackage);
        var result = validate(support, modelChanges, pricePackage);
        assertNoDefects(result);
    }

    @Test
    public void expiredPackageCanNotBeChanged() {
        var pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withDateStart(LocalDate.now().minusMonths(2))
                .withDateEnd(LocalDate.now().minusMonths(1))
                .withStatusApprove(StatusApprove.YES)
                .withCurrency(CurrencyCode.RUB))
                .getPricePackage();
        var modelChanges = modelChanges(pricePackage);
        var result = validate(priceManager, modelChanges, pricePackage);
        assertDefect(result, pricePackageIsExpired());
    }

    private ModelChanges<PricePackage> modelChanges(PricePackage pricePackage) {
        return ModelChanges.build(pricePackage, PricePackage.CLIENTS, List.of(allowedPricePackageClient(client)));
    }

    private void assertNoDefects(Optional<MassResult<Long>> result) {
        assertThat(result).isEmpty();
    }

    private void assertDefect(Optional<MassResult<Long>> result, Defect<?> defect) {
        assertThat(result).isNotEmpty();

        var validationResult = result.get().get(0).getValidationResult();
        assertThat(validationResult).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(PricePackage.CLIENTS)), defect))));
    }

    private void assertForbiddenToChangeDefects(Optional<MassResult<Long>> result) {
        assertThat(result).isNotEmpty();

        var validationResult = result.get().get(0).getValidationResult();
        assertThat(validationResult).is(matchedBy(hasDefectWithDefinition(
                validationError(path(), forbiddenToChange()))));
    }

}
