package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupUpdateOperationParams;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTree;

@ParametersAreNonnullByDefault
public class AdGroupsUpdateOperationCpmYndxFrontpageTestBase {

    @Autowired
    protected AdGroupsUpdateOperationFactory adGroupsUpdateOperationFactory;

    static ModelChanges<CpmYndxFrontpageAdGroup> modelChangesWithName(AdGroup adGroup, String newName) {
        ModelChanges<CpmYndxFrontpageAdGroup> modelChanges = new ModelChanges<>(adGroup.getId(),
                CpmYndxFrontpageAdGroup.class);
        modelChanges.process(newName, AdGroup.NAME);
        return modelChanges;
    }

    static ModelChanges<CpmYndxFrontpageAdGroup> modelChangesWithPriority(CpmYndxFrontpageAdGroup adGroup,
                                                                          Long newPriority) {
        ModelChanges<CpmYndxFrontpageAdGroup> modelChanges = new ModelChanges<>(adGroup.getId(),
                CpmYndxFrontpageAdGroup.class);
        modelChanges.process(newPriority, CpmYndxFrontpageAdGroup.PRIORITY);
        return modelChanges;
    }

    static ModelChanges<CpmYndxFrontpageAdGroup> modelChangesWithGeo(AdGroup adGroup, List<Long> newGeo) {
        ModelChanges<CpmYndxFrontpageAdGroup> modelChanges = new ModelChanges<>(adGroup.getId(),
                CpmYndxFrontpageAdGroup.class);
        modelChanges.process(newGeo, CpmYndxFrontpageAdGroup.GEO);
        return modelChanges;
    }

    protected AdGroupsUpdateOperation createUpdateOperation(ClientInfo clientInfo, GeoTree geoTree,
                                                            List<ModelChanges<AdGroup>> modelChangesList) {
        return adGroupsUpdateOperationFactory.newInstance(
                Applicability.FULL,
                modelChangesList,
                AdGroupUpdateOperationParams.builder()
                        .withModerationMode(ModerationMode.DEFAULT)
                        .withValidateInterconnections(true)
                        .build(),
                geoTree,
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                clientInfo.getUid(),
                clientInfo.getClientId(),
                clientInfo.getShard());
    }

}
