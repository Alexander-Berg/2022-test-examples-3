package ru.yandex.direct.api.v5.entity.negativekeywordsharedsets.delegate;

import java.util.Arrays;
import java.util.List;

import com.yandex.direct.api.v5.general.ActionResult;
import com.yandex.direct.api.v5.general.ExceptionNotification;
import com.yandex.direct.api.v5.negativekeywordsharedsets.NegativeKeywordSharedSetUpdateItem;
import com.yandex.direct.api.v5.negativekeywordsharedsets.UpdateRequest;
import com.yandex.direct.api.v5.negativekeywordsharedsets.UpdateResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.minuskeywordspack.service.MinusKeywordsPacksUpdateOperationFactory;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.DefectTypes.maxElementsPerRequest;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.MAX_LIBRARY_PACKS_COUNT;
import static ru.yandex.direct.utils.FunctionalUtils.intRange;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@Api5Test
@RunWith(SpringRunner.class)
public class UpdateNegativeKeywordSharedSetsDelegateTest {

    private static final String NEW_NAME1 = "new pack name1";
    private static final String NEW_NAME2 = "new pack name2";
    private static final List<String> NEGATIVE_KEYWORDS_1 = singletonList("купить авто");
    private static final List<String> NEGATIVE_KEYWORDS_2 = singletonList("купить телефон");

    private GenericApiService genericApiService;
    @Autowired
    private MinusKeywordsPacksUpdateOperationFactory minusKeywordsPacksUpdateOperationFactory;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private MinusKeywordsPackRepository repository;
    @Autowired
    private Steps steps;
    private UpdateNegativeKeywordSharedSetsDelegate updateNegativeKeywordSharedSetsDelegate;
    private MinusKeywordsPackInfo pack1;
    private MinusKeywordsPackInfo pack2;
    private ClientInfo client;

    @Before
    public void before() {
        client = steps.clientSteps().createDefaultClient();
        ApiAuthenticationSource apiAuthenticationSource = mock(ApiAuthenticationSource.class);
        when(apiAuthenticationSource.getChiefSubclient()).thenReturn(new ApiUser().withClientId(client.getClientId()));
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(apiContextHolder, mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class), mock(RequestCampaignAccessibilityCheckerProvider.class));
        updateNegativeKeywordSharedSetsDelegate = new UpdateNegativeKeywordSharedSetsDelegate(apiAuthenticationSource,
                minusKeywordsPacksUpdateOperationFactory, shardHelper, resultConverter);

        pack1 = steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(client);
        pack2 = steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(client);
    }

    @Test
    public void doAction_UpdateOne_Successful() {
        UpdateResponse updateResponse = genericApiService.doAction(updateNegativeKeywordSharedSetsDelegate,
                new UpdateRequest()
                        .withNegativeKeywordSharedSets(createUpdateItem(pack1.getMinusKeywordPackId(), NEW_NAME1,
                                NEGATIVE_KEYWORDS_1)));
        List<ActionResult> updateResults = updateResponse.getUpdateResults();
        assertThat(updateResults)
                .hasSize(1)
                .extracting(ActionResult::getId)
                .doesNotContainNull();
        ActionResult actionResult = updateResults.get(0);
        assertPack(actionResult.getId(), NEW_NAME1, NEGATIVE_KEYWORDS_1);
    }

    @Test
    public void doAction_UpdateOnlyName_NameChangedAndMinusKeywordsNotChanged() {
        NegativeKeywordSharedSetUpdateItem updateItem = createUpdateItem(pack1.getMinusKeywordPackId(), NEW_NAME1,
                null);

        UpdateResponse updateResponse = genericApiService.doAction(updateNegativeKeywordSharedSetsDelegate,
                new UpdateRequest().withNegativeKeywordSharedSets(updateItem));

        List<ActionResult> updateResults = updateResponse.getUpdateResults();
        assertThat(updateResults)
                .hasSize(1)
                .extracting(ActionResult::getId)
                .doesNotContainNull();
        ActionResult actionResult = updateResults.get(0);
        List<String> sourceMinusKeywords = pack1.getMinusKeywordsPack().getMinusKeywords();
        assertPack(actionResult.getId(), NEW_NAME1, sourceMinusKeywords);
    }

    @Test
    public void doAction_UpdateSeveral_Successful() {
        UpdateResponse updateResponse = genericApiService.doAction(updateNegativeKeywordSharedSetsDelegate,
                new UpdateRequest().withNegativeKeywordSharedSets(
                        createUpdateItem(pack1.getMinusKeywordPackId(), NEW_NAME1, NEGATIVE_KEYWORDS_1),
                        createUpdateItem(pack2.getMinusKeywordPackId(), NEW_NAME2, NEGATIVE_KEYWORDS_2)));

        List<ActionResult> updateResults = updateResponse.getUpdateResults();
        assertThat(updateResults)
                .hasSize(2)
                .extracting(ActionResult::getId)
                .doesNotContainNull();

        assertPack(updateResults.get(0).getId(), NEW_NAME1, NEGATIVE_KEYWORDS_1);
        assertPack(updateResults.get(1).getId(), NEW_NAME2, NEGATIVE_KEYWORDS_2);
    }

    @Test
    public void doAction_InvalidName_ValidationError() {
        NegativeKeywordSharedSetUpdateItem updateItem = createUpdateItem(pack1.getMinusKeywordPackId(), "",
                NEGATIVE_KEYWORDS_1);

        UpdateResponse updateResponse = genericApiService.doAction(updateNegativeKeywordSharedSetsDelegate,
                new UpdateRequest().withNegativeKeywordSharedSets(updateItem));
        List<ActionResult> updateResults = updateResponse.getUpdateResults();
        assertThat(updateResults).hasSize(1);
        assertErrorCode(updateResults, 5003, 0);
    }

    @Test
    public void doAction_InvalidNegativeKeywords_ValidationError() {

        NegativeKeywordSharedSetUpdateItem updateItem = createUpdateItem(pack1.getMinusKeywordPackId(), NEW_NAME1,
                Arrays.asList("это минус фраза", "aa bb cc dd ee " + "ff gg hh"));

        UpdateResponse updateResponse = genericApiService.doAction(updateNegativeKeywordSharedSetsDelegate,
                new UpdateRequest().withNegativeKeywordSharedSets(updateItem));

        List<ActionResult> updateResults = updateResponse.getUpdateResults();
        assertThat(updateResults).hasSize(1);
        assertErrorCode(updateResults, 5002, 0);
    }

    @Test
    public void doAction_UpdateWithDuplicates_ValidationError() {

        Long minusKeywordPackId = pack1.getMinusKeywordPackId();

        NegativeKeywordSharedSetUpdateItem okItem = createUpdateItem(minusKeywordPackId, NEW_NAME1,
                NEGATIVE_KEYWORDS_1);

        NegativeKeywordSharedSetUpdateItem duplicateItem1 = createUpdateItem(pack1.getMinusKeywordPackId() + 1,
                NEW_NAME1, NEGATIVE_KEYWORDS_1);

        NegativeKeywordSharedSetUpdateItem duplicateItem2 = createUpdateItem(pack1.getMinusKeywordPackId() + 1,
                NEW_NAME1, NEGATIVE_KEYWORDS_1);


        UpdateResponse updateResponse = genericApiService.doAction(updateNegativeKeywordSharedSetsDelegate,
                new UpdateRequest()
                        .withNegativeKeywordSharedSets(okItem, duplicateItem1, duplicateItem2));

        List<ActionResult> updateResults = updateResponse.getUpdateResults();

        assertThat(updateResults).hasSize(3);

        ActionResult actionResult = updateResults.get(0);
        assertThat(actionResult).extracting(ActionResult::getId).isNotNull();
        assertPack(actionResult.getId(), NEW_NAME1, NEGATIVE_KEYWORDS_1);

        assertErrorCode(updateResults, 9800, 1);
        assertErrorCode(updateResults, 9800, 2);
    }

    private NegativeKeywordSharedSetUpdateItem createUpdateItem(Long minusKeywordPackId, String newName1,
                                                                List<String> negativeKeywords1) {
        return new NegativeKeywordSharedSetUpdateItem()
                .withId(minusKeywordPackId)
                .withName(newName1)
                .withNegativeKeywords(negativeKeywords1);
    }

    private void assertErrorCode(List<ActionResult> updateResults, int errorCode, int index) {
        List<ExceptionNotification> errors = updateResults.get(index).getErrors();
        assertThat(errors)
                .extracting(ExceptionNotification::getCode)
                .containsExactly(errorCode);
    }

    @Test
    public void validateRequest_MaximumValidSize() {
        ValidationResult<UpdateRequest, DefectType> vr =
                updateNegativeKeywordSharedSetsDelegate.validateRequest(new UpdateRequest()
                        .withNegativeKeywordSharedSets(mapList(intRange(0, MAX_LIBRARY_PACKS_COUNT),
                                i -> new NegativeKeywordSharedSetUpdateItem())));
        assertThat(vr).is(hasNoDefects());
    }

    @Test
    public void validateRequest_InvalidSize() {
        ValidationResult<UpdateRequest, DefectType> vr =
                updateNegativeKeywordSharedSetsDelegate.validateRequest(new UpdateRequest()
                        .withNegativeKeywordSharedSets(mapList(intRange(0, MAX_LIBRARY_PACKS_COUNT + 1),
                                i -> new NegativeKeywordSharedSetUpdateItem())));
        assertThat(vr).is(
                hasDefectWith(validationError(
                        path(field(UpdateRequest.PropInfo.NEGATIVE_KEYWORD_SHARED_SETS.propertyName)),
                        maxElementsPerRequest(MAX_LIBRARY_PACKS_COUNT))));
    }

    private void assertPack(Long actualPackId, String newName, List<String> newMinusWords) {
        MinusKeywordsPack actualPack = repository.getMinusKeywordsPacks(client.getShard(), client.getClientId(),
                singleton(actualPackId)
        ).get(actualPackId);

        assertThat(actualPack.getName()).isEqualTo(newName);
        assertThat(actualPack.getMinusKeywords()).containsExactlyInAnyOrderElementsOf(newMinusWords);
    }

}
