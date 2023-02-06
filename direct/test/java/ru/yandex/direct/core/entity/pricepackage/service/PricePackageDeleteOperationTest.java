package ru.yandex.direct.core.entity.pricepackage.service;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.operation.Applicability.FULL;
import static ru.yandex.direct.operation.Applicability.PARTIAL;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.inconsistentState;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PricePackageDeleteOperationTest {

    @Autowired
    public PricePackageDeleteOperationFactory operationFactory;

    @Autowired
    private PricePackageRepository pricePackageRepository;

    @Autowired
    private Steps steps;

    @Test
    public void notApprovedPackage_success() {
        var notApprovedPricePackage = steps.pricePackageSteps().createNewPricePackage().getPricePackage();

        var operation = createDeleteOperation(notApprovedPricePackage);
        var result = operation.prepareAndApply();

        assertThat(result, isFullySuccessful());
        assertPricePackageDeleted(notApprovedPricePackage.getId());

    }

    @Test
    public void approvedPackage_error() {
        var approvedPricePackage = steps.pricePackageSteps().createApprovedPricePackage().getPricePackage();

        var operation = createDeleteOperation(approvedPricePackage);
        var result = operation.prepareAndApply();

        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0)), inconsistentState())));
    }

    @Test
    public void nonexistentPackage_ignored() {
        var notApprovedPricePackage = steps.pricePackageSteps().createNewPricePackage().getPricePackage();

        long nonExistentId = notApprovedPricePackage.getId() + 100;
        var operation = operationFactory.newInstance(PARTIAL, List.of(nonExistentId, notApprovedPricePackage.getId()));
        var result = operation.prepareAndApply();

        assertThat(result, isSuccessful());
        assertPricePackageDeleted(notApprovedPricePackage.getId());
    }

    private PricePackageDeleteOperation createDeleteOperation(PricePackage pricePackage) {
        return operationFactory.newInstance(FULL, List.of(pricePackage.getId()));
    }

    private void assertPricePackageDeleted(Long pricePackageId) {
        Map<Long, PricePackage> packages = pricePackageRepository.getPricePackages(List.of(pricePackageId));
        assertTrue(packages.isEmpty());
    }
}
