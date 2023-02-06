package ru.yandex.direct.api.v5.entity.negativekeywordsharedsets.delegate;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import com.yandex.direct.api.v5.general.ActionResult;
import com.yandex.direct.api.v5.general.ExceptionNotification;
import com.yandex.direct.api.v5.general.IdsCriteria;
import com.yandex.direct.api.v5.negativekeywordsharedsets.DeleteRequest;
import com.yandex.direct.api.v5.negativekeywordsharedsets.DeleteResponse;
import org.apache.commons.lang3.RandomUtils;
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
import ru.yandex.direct.core.entity.minuskeywordspack.service.MinusKeywordsPacksDeleteOperationFactory;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.DefectTypes.maxElementsPerRequest;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.MAX_LIBRARY_PACKS_COUNT;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@Api5Test
@RunWith(SpringRunner.class)
public class DeleteNegativeKeywordSharedSetsDelegateTest {

    private GenericApiService genericApiService;
    @Autowired
    private MinusKeywordsPacksDeleteOperationFactory minusKeywordsPacksDeleteOperationFactory;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private MinusKeywordsPackRepository repository;
    @Autowired
    private Steps steps;
    private DeleteNegativeKeywordSharedSetsDelegate deleteNegativeKeywordSharedSetsDelegate;
    private ClientInfo client;
    private Long defaultPackId;

    @Before
    public void before() {
        client = steps.clientSteps().createDefaultClient();
        ApiAuthenticationSource apiAuthenticationSource = mock(ApiAuthenticationSource.class);
        when(apiAuthenticationSource.getChiefSubclient()).thenReturn(new ApiUser().withClientId(client.getClientId()));

        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(apiContextHolder, mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class), mock(RequestCampaignAccessibilityCheckerProvider.class));
        deleteNegativeKeywordSharedSetsDelegate = new DeleteNegativeKeywordSharedSetsDelegate(apiAuthenticationSource,
                minusKeywordsPacksDeleteOperationFactory, shardHelper, resultConverter);

        defaultPackId = steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(client).getMinusKeywordPackId();
    }

    @Test
    public void doAction_DeleteOne_Successful() {
        IdsCriteria idsCriteria = new IdsCriteria().withIds(defaultPackId);
        DeleteRequest deleteRequest = new DeleteRequest().withSelectionCriteria(idsCriteria);
        DeleteResponse deleteResponse = genericApiService.doAction(deleteNegativeKeywordSharedSetsDelegate,
                deleteRequest);

        List<ActionResult> deleteResults = deleteResponse.getDeleteResults();
        assertThat(deleteResults).hasSize(1);
        Long id = deleteResults.get(0).getId();

        Map<Long, MinusKeywordsPack> packs = repository.getMinusKeywordsPacks(client.getShard(),
                client.getClientId(), singleton(id));

        assertThat(packs).isEmpty();
    }

    @Test
    public void doAction_DeleteOneExistentAndOneNonexistent_Successful() {
        long nonexistentId = RandomUtils.nextLong(100, Long.MAX_VALUE);

        IdsCriteria idsCriteria = new IdsCriteria().withIds(defaultPackId, nonexistentId);
        DeleteRequest deleteRequest = new DeleteRequest().withSelectionCriteria(idsCriteria);
        DeleteResponse deleteResponse = genericApiService.doAction(deleteNegativeKeywordSharedSetsDelegate,
                deleteRequest);

        List<ActionResult> deleteResults = deleteResponse.getDeleteResults();
        assertThat(deleteResults).hasSize(2);

        Long id = deleteResults.get(0).getId();
        Map<Long, MinusKeywordsPack> packs = repository.getMinusKeywordsPacks(client.getShard(),
                client.getClientId(), singleton(id));
        assertThat(packs).isEmpty();

        assertErrorCode(deleteResults, 8800, 1);
    }

    @Test
    public void doAction_DeleteWithDuplicates_Successful() {
        long duplicateId = defaultPackId + 1;

        IdsCriteria idsCriteria = new IdsCriteria().withIds(defaultPackId, duplicateId, duplicateId);
        DeleteRequest deleteRequest = new DeleteRequest().withSelectionCriteria(idsCriteria);
        DeleteResponse deleteResponse = genericApiService.doAction(deleteNegativeKeywordSharedSetsDelegate,
                deleteRequest);

        List<ActionResult> deleteResults = deleteResponse.getDeleteResults();
        assertThat(deleteResults).hasSize(3);

        Long id = deleteResults.get(0).getId();
        Map<Long, MinusKeywordsPack> packs = repository.getMinusKeywordsPacks(client.getShard(),
                client.getClientId(), singleton(id));
        assertThat(packs).isEmpty();

        assertErrorCode(deleteResults, 9800, 1);
        assertErrorCode(deleteResults, 9800, 2);
    }

    private void assertErrorCode(List<ActionResult> deleteResults, int errorCode, int index) {
        List<ExceptionNotification> errors = deleteResults.get(index).getErrors();
        assertThat(errors)
                .extracting(ExceptionNotification::getCode)
                .containsExactly(errorCode);
    }

    @Test
    public void validateRequest_MaximumValidSize() {
        IdsCriteria idsCriteria =
                new IdsCriteria().withIds(LongStream.range(0, MAX_LIBRARY_PACKS_COUNT).boxed().collect(toList()));

        ValidationResult<DeleteRequest, DefectType> vr = deleteNegativeKeywordSharedSetsDelegate.validateRequest(
                new DeleteRequest().withSelectionCriteria(idsCriteria));

        assertThat(vr).is(hasNoDefects());
    }

    @Test
    public void validateRequest_InvalidSize() {
        IdsCriteria idsCriteria =
                new IdsCriteria().withIds(LongStream.range(0, MAX_LIBRARY_PACKS_COUNT + 1).boxed().collect(toList()));

        ValidationResult<DeleteRequest, DefectType> vr =
                deleteNegativeKeywordSharedSetsDelegate.validateRequest(
                        new DeleteRequest().withSelectionCriteria(idsCriteria));

        assertThat(vr).is(
                hasDefectWith(validationError(
                        path(field("Ids")),
                        maxElementsPerRequest(MAX_LIBRARY_PACKS_COUNT))));
    }

}
