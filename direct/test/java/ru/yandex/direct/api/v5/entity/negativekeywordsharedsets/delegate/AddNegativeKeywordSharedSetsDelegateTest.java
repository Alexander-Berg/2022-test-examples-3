package ru.yandex.direct.api.v5.entity.negativekeywordsharedsets.delegate;

import java.util.List;

import com.yandex.direct.api.v5.general.ActionResult;
import com.yandex.direct.api.v5.general.ActionResultBase;
import com.yandex.direct.api.v5.general.ExceptionNotification;
import com.yandex.direct.api.v5.negativekeywordsharedsets.AddRequest;
import com.yandex.direct.api.v5.negativekeywordsharedsets.AddResponse;
import com.yandex.direct.api.v5.negativekeywordsharedsets.NegativeKeywordSharedSetAddItem;
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
import ru.yandex.direct.core.entity.minuskeywordspack.service.MinusKeywordsPacksAddOperationFactory;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.validation.result.ValidationResult;

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
public class AddNegativeKeywordSharedSetsDelegateTest {

    private GenericApiService genericApiService;
    @Autowired
    private MinusKeywordsPacksAddOperationFactory minusKeywordsPacksAddOperationFactory;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private Steps steps;
    private AddNegativeKeywordSharedSetsDelegate addNegativeKeywordSharedSetsDelegate;

    @Before
    public void before() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        ApiAuthenticationSource apiAuthenticationSource = mock(ApiAuthenticationSource.class);
        when(apiAuthenticationSource.getChiefSubclient()).thenReturn(new ApiUser().withClientId(client.getClientId()));

        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(apiContextHolder, mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class), mock(RequestCampaignAccessibilityCheckerProvider.class));
        addNegativeKeywordSharedSetsDelegate = new AddNegativeKeywordSharedSetsDelegate(apiAuthenticationSource,
                minusKeywordsPacksAddOperationFactory, shardHelper, resultConverter);
    }

    @Test
    public void doAction_AddOne_Successful() {
        AddResponse addResponse = genericApiService.doAction(addNegativeKeywordSharedSetsDelegate, new AddRequest()
                .withNegativeKeywordSharedSets(new NegativeKeywordSharedSetAddItem()
                        .withName("New set")
                        .withNegativeKeywords("это минус фраза", "купить авто премиум класса")));
        List<ActionResult> addResults = addResponse.getAddResults();
        assertThat(addResults).hasSize(1).doesNotContainNull();
    }

    @Test
    public void doAction_AddSeveral_Successful() {
        AddResponse addResponse = genericApiService.doAction(addNegativeKeywordSharedSetsDelegate, new AddRequest()
                .withNegativeKeywordSharedSets(new NegativeKeywordSharedSetAddItem()
                                .withName("New set")
                                .withNegativeKeywords("это минус фраза", "купить авто премиум класса"),
                        new NegativeKeywordSharedSetAddItem()
                                .withName("Second set")
                                .withNegativeKeywords("второй набор", "раз два три четыре")));
        List<ActionResult> addResults = addResponse.getAddResults();
        assertThat(addResults).hasSize(2).doesNotContainNull();
    }

    @Test
    public void doAction_InvalidName_ValidationError() {
        AddResponse addResponse = genericApiService.doAction(addNegativeKeywordSharedSetsDelegate, new AddRequest()
                .withNegativeKeywordSharedSets(new NegativeKeywordSharedSetAddItem()
                        .withName("")
                        .withNegativeKeywords("это минус фраза", "купить авто премиум класса")));
        List<ActionResult> addResults = addResponse.getAddResults();
        assertThat(addResults)
                .flatExtracting(ActionResultBase::getErrors)
                .extracting(ExceptionNotification::getCode)
                .containsExactly(5003);
    }

    @Test
    public void doAction_InvalidNegativeKeywords_ValidationError() {
        AddResponse addResponse = genericApiService.doAction(addNegativeKeywordSharedSetsDelegate, new AddRequest()
                .withNegativeKeywordSharedSets(new NegativeKeywordSharedSetAddItem()
                        .withName("New set")
                        .withNegativeKeywords("это минус фраза", "aa bb cc dd ee ff gg hh")));
        List<ActionResult> addResults = addResponse.getAddResults();
        assertThat(addResults)
                .flatExtracting(ActionResultBase::getErrors)
                .extracting(ExceptionNotification::getCode)
                .containsExactly(5002);
    }

    @Test
    public void validateRequest_MaximumValidSize() {
        ValidationResult<AddRequest, DefectType> vr =
                addNegativeKeywordSharedSetsDelegate.validateRequest(new AddRequest()
                        .withNegativeKeywordSharedSets(mapList(intRange(0, MAX_LIBRARY_PACKS_COUNT),
                                i -> new NegativeKeywordSharedSetAddItem())));
        assertThat(vr).is(hasNoDefects());
    }

    @Test
    public void validateRequest_InvalidSize() {
        ValidationResult<AddRequest, DefectType> vr =
                addNegativeKeywordSharedSetsDelegate.validateRequest(new AddRequest()
                        .withNegativeKeywordSharedSets(mapList(intRange(0, MAX_LIBRARY_PACKS_COUNT + 1),
                                i -> new NegativeKeywordSharedSetAddItem())));
        assertThat(vr).is(
                hasDefectWith(validationError(
                        path(field(AddRequest.PropInfo.NEGATIVE_KEYWORD_SHARED_SETS.propertyName)),
                        maxElementsPerRequest(MAX_LIBRARY_PACKS_COUNT))));
    }
}
