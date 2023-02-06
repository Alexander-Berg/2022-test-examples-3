package ru.yandex.direct.api.v5.entity.negativekeywordsharedsets.delegate;

import java.util.List;
import java.util.stream.LongStream;

import com.yandex.direct.api.v5.general.IdsCriteria;
import com.yandex.direct.api.v5.general.LimitOffset;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.negativekeywordsharedsets.GetRequest;
import com.yandex.direct.api.v5.negativekeywordsharedsets.GetResponse;
import com.yandex.direct.api.v5.negativekeywordsharedsets.NegativeKeywordSharedSetFieldEnum;
import com.yandex.direct.api.v5.negativekeywordsharedsets.NegativeKeywordSharedSetGetItem;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.negativekeywordsharedsets.validation.GetNegativeKeywordSharedSetsValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.api.v5.validation.DefectTypes;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.yandex.direct.api.v5.general.IdsCriteria.PropInfo.IDS;
import static com.yandex.direct.api.v5.negativekeywordsharedsets.GetRequest.PropInfo.SELECTION_CRITERIA;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.MAX_LIBRARY_PACKS_COUNT;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@Api5Test
@RunWith(SpringRunner.class)
public class GetNegativeKeywordSharedSetsDelegateTest {

    private ClientInfo clientInfo;
    private GenericApiService genericApiService;
    private GetNegativeKeywordSharedSetsDelegate delegate;
    private GetRequest getRequest;

    @Autowired
    private MinusKeywordsPackRepository packRepository;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;
    @Autowired
    private Steps steps;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createClient(new ClientInfo());
        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientInfo.getClientId()));
        GetNegativeKeywordSharedSetsValidationService validationService =
                new GetNegativeKeywordSharedSetsValidationService();
        delegate = new GetNegativeKeywordSharedSetsDelegate(auth, packRepository, shardHelper, new PropertyFilter(),
                validationService);

        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(apiContextHolder, mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class), mock(RequestCampaignAccessibilityCheckerProvider.class));

        getRequest = new GetRequest()
                .withFieldNames(NegativeKeywordSharedSetFieldEnum.values());
    }

    @Test
    public void doAction_GetAll_EmptyResult() {
        GetResponse getResponse = genericApiService.doAction(delegate, getRequest);
        List<NegativeKeywordSharedSetGetItem> actualList = getResponse.getNegativeKeywordSharedSets();
        assertThat(actualList).isEmpty();
    }

    @Test
    public void doAction_GetOne_Successful() {
        createLibraryPack();
        MinusKeywordsPack expectedPack = createLibraryPack();

        IdsCriteria selectionCriteria = new IdsCriteria().withIds(expectedPack.getId());
        getRequest.withSelectionCriteria(selectionCriteria);

        GetResponse getResponse = genericApiService.doAction(delegate, getRequest);
        List<NegativeKeywordSharedSetGetItem> actualList = getResponse.getNegativeKeywordSharedSets();

        assertThat(actualList).hasSize(1);
        assertPacksEquals(expectedPack, actualList.get(0), YesNoEnum.NO);
    }

    @Test
    public void doAction_GetAll_Successful() {
        MinusKeywordsPack expectedPack1 = createLibraryPack("name1");
        MinusKeywordsPack expectedPack2 = createLibraryPack("name2");
        linkToAnyAdGroup(expectedPack2.getId());

        GetResponse getResponse = genericApiService.doAction(delegate, getRequest);
        List<NegativeKeywordSharedSetGetItem> actualList = getResponse.getNegativeKeywordSharedSets();

        assertThat(actualList).hasSize(2);
        assertPacksEquals(expectedPack1, actualList.get(0), YesNoEnum.NO);
        assertPacksEquals(expectedPack2, actualList.get(1), YesNoEnum.YES);
    }

    @Test
    public void doAction_GetWithFieldNames_Successful() {
        MinusKeywordsPack expectedPack = createLibraryPack("name1");

        getRequest = new GetRequest().withFieldNames(NegativeKeywordSharedSetFieldEnum.ID,
                NegativeKeywordSharedSetFieldEnum.NAME,
                NegativeKeywordSharedSetFieldEnum.NEGATIVE_KEYWORDS);
        GetResponse getResponse = genericApiService.doAction(delegate, getRequest);
        List<NegativeKeywordSharedSetGetItem> actualList = getResponse.getNegativeKeywordSharedSets();

        assertThat(actualList)
                .extracting(this::toCoreMinusKeywordsPack)
                .singleElement()
                .usingRecursiveComparison()
                .ignoringFields(MinusKeywordsPack.HASH.name())
                .isEqualTo(expectedPack);
    }

    @Test
    public void doAction_GetAllWithLimit_Successful() {
        MinusKeywordsPack expectedPack2 = createLibraryPack("name2");
        createLibraryPack("name1");
        createLibraryPack("name3");

        LimitOffset limitOffset = new LimitOffset()
                .withOffset(1L)
                .withLimit(1L);
        getRequest.withPage(limitOffset);

        GetResponse getResponse = genericApiService.doAction(delegate, getRequest);
        List<NegativeKeywordSharedSetGetItem> actualList = getResponse.getNegativeKeywordSharedSets();

        assertThat(actualList).hasSize(1);
        assertPacksEquals(expectedPack2, actualList.get(0), YesNoEnum.NO);
    }

    @Test
    public void validateRequest_MaxIdsInSelection_ValidationError() {
        List<Long> tooMuchMwIds = LongStream.range(0, MAX_LIBRARY_PACKS_COUNT + 1).boxed().collect(toList());
        IdsCriteria idsCriteria = new IdsCriteria().withIds(tooMuchMwIds);

        getRequest.withSelectionCriteria(idsCriteria);
        ValidationResult<GetRequest, DefectType> vr = delegate.validateRequest(getRequest);
        Path path = path(field(SELECTION_CRITERIA.schemaName.toString()), field(IDS.schemaName.toString()));
        MatcherAssert.assertThat(vr, hasDefectWith(validationError(path, DefectTypes.maxIdsInSelection())));
    }


    private void assertPacksEquals(MinusKeywordsPack expectedPack, NegativeKeywordSharedSetGetItem actualItem,
                                   YesNoEnum expectAssociated) {
        MinusKeywordsPack actualPack = toCoreMinusKeywordsPack(actualItem);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(actualPack)
                .usingRecursiveComparison()
                .ignoringFields(MinusKeywordsPack.HASH.name())
                .isEqualTo(expectedPack);
        sa.assertThat(actualItem)
                .extracting(NegativeKeywordSharedSetGetItem::getAssociated)
                .isEqualTo(expectAssociated);
        sa.assertAll();
    }

    private MinusKeywordsPack toCoreMinusKeywordsPack(NegativeKeywordSharedSetGetItem item) {
        return new MinusKeywordsPack()
                .withId(item.getId())
                .withName(item.getName())
                .withMinusKeywords(item.getNegativeKeywords())
                .withIsLibrary(true);
    }

    private void linkToAnyAdGroup(Long packId) {
        Long adGroupId = steps.adGroupSteps().createDefaultAdGroup().getAdGroupId();
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(clientInfo.getShard(), packId, adGroupId);
    }

    private MinusKeywordsPack createLibraryPack() {
        return createLibraryPack("default");
    }

    private MinusKeywordsPack createLibraryPack(String name) {
        MinusKeywordsPack defaultPack = libraryMinusKeywordsPack().withName(name);
        return steps.minusKeywordsPackSteps().createMinusKeywordsPack(defaultPack, clientInfo).getMinusKeywordsPack();
    }
}
