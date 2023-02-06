package ru.yandex.direct.teststeps.service;

import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.testing.steps.PricePackageSteps;

import static ru.yandex.direct.core.testing.data.TestPricePackages.pricePackageForFrontendTestSteps;

@Service
@ParametersAreNonnullByDefault
public class PricePackageStepsService {

    private final PricePackageSteps pricePackageSteps;

    @Autowired
    public PricePackageStepsService(PricePackageSteps pricePackageSteps) {
        this.pricePackageSteps = pricePackageSteps;
    }

    public Long createPricePackage(@Nullable Long clientId,
                                   Boolean allowExpandedDesktopCreative,
                                   @Nullable Boolean isApproved,
                                   @Nullable Set<AdGroupType> availableAdGroupTypesNullable,
                                   Boolean isDraftApproveAllowed) {
        StatusApprove statusApprove = StatusApprove.YES;
        if (isApproved != null && !isApproved) {
            statusApprove = StatusApprove.NEW;
        }
        Set<AdGroupType> availableAdGroupTypes = Set.of(AdGroupType.CPM_YNDX_FRONTPAGE);
        if (availableAdGroupTypesNullable != null) {
            availableAdGroupTypes = availableAdGroupTypesNullable;
        }
        return pricePackageSteps.createPricePackage(
                pricePackageForFrontendTestSteps(clientId,
                        allowExpandedDesktopCreative,
                        statusApprove,
                        availableAdGroupTypes,
                        isDraftApproveAllowed))
                .getPricePackageId();
    }

    public void deletePricePackage(Long packageId) {
        pricePackageSteps.deletePricePackage(packageId);
    }

}
