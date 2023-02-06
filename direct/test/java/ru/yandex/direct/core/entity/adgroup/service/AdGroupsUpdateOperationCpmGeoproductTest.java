package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupUpdateOperationParams;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefectIds;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateOperationCpmGeoproductTest {

    public static final String NEW_NAME = "главная";

    @Autowired
    protected Steps steps;

    @Autowired
    protected AdGroupRepository adGroupRepository;

    @Autowired
    protected GeoTreeFactory geoTreeFactory;

    @Autowired
    private AdGroupsUpdateOperationFactory adGroupsUpdateOperationFactory;

    private GeoTree geoTree;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        geoTree = geoTreeFactory.getGlobalGeoTree();
    }

    @Test
    public void prepareAndApply_ChangeOnlyName_NoError() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultCpmGeoproductAdGroup(clientInfo);
        ModelChanges<AdGroup> modelChanges = modelChangesWithName(adGroupInfo.getAdGroup());
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(true));

        AdGroup realAdGroup =
                adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(result.get(0).getResult())).get(0);
        assertThat(realAdGroup.getName(), equalTo(NEW_NAME));
    }

    @Test
    public void prepareAndApply_ChangeMinusKeywords_Error() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultCpmGeoproductAdGroup(clientInfo);
        ModelChanges<AdGroup> modelChanges = modelChangesWithMinusKeywords(adGroupInfo.getAdGroup());
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(0),
                field("minusKeywords")), AdGroupDefectIds.Gen.MINUS_KEYWORDS_NOT_ALLOWED)));
    }

    private ModelChanges<AdGroup> modelChangesWithName(AdGroup adGroup) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        modelChanges.process(NEW_NAME, AdGroup.NAME);
        return modelChanges;
    }

    private ModelChanges<AdGroup> modelChangesWithMinusKeywords(AdGroup adGroup) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        modelChanges.process(singletonList("abc"), AdGroup.MINUS_KEYWORDS);
        return modelChanges;
    }

    private AdGroupsUpdateOperation createUpdateOperation(List<ModelChanges<AdGroup>> modelChangesList) {
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
