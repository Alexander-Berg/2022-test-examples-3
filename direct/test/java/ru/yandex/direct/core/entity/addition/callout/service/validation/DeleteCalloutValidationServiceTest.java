package ru.yandex.direct.core.entity.addition.callout.service.validation;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.core.entity.banner.type.banneradditions.BannerAdditionsRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.addition.callout.service.validation.CalloutDefinitions.adExtensionIsInUse;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class DeleteCalloutValidationServiceTest {

    private static final int SHARD = 1;
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);

    private DeleteCalloutValidationService deleteCalloutValidationService;

    private CalloutRepository calloutRepository;
    private BannerAdditionsRepository bannerAdditionsRepository;

    private Callout defaultCallout1;
    private Callout defaultCallout2;

    @Before
    public void before() {
        calloutRepository = mock(CalloutRepository.class);
        defaultCallout1 = new Callout()
                .withClientId(CLIENT_ID.asLong())
                .withId(1L)
                .withDeleted(false);
        defaultCallout2 = new Callout()
                .withClientId(CLIENT_ID.asLong())
                .withId(2L)
                .withDeleted(false);
        when(calloutRepository.getClientExistingCallouts(SHARD, CLIENT_ID))
                .thenReturn(Arrays.asList(defaultCallout1, defaultCallout2));

        bannerAdditionsRepository = mock(BannerAdditionsRepository.class);
        when(bannerAdditionsRepository.getLinkedBannersAdditions(eq(SHARD), anyCollection()))
                .thenReturn(singleton(2L));

        deleteCalloutValidationService = new DeleteCalloutValidationService(calloutRepository,
                bannerAdditionsRepository);
    }


    @Test
    public void validateDelete_positiveValidationResult() {
        ValidationResult<List<Long>, Defect> actual =
                deleteCalloutValidationService.validateDelete(SHARD, CLIENT_ID, singletonList(1L));

        assertThat(actual, hasNoDefectsDefinitions());
    }

    @Test
    public void validateDelete_ListNull() {
        ValidationResult<List<Long>, Defect> actual =
                deleteCalloutValidationService.validateDelete(SHARD, CLIENT_ID, null);

        assertThat(actual, hasDefectWithDefinition(validationError(path(), notNull())));
    }

    @Test
    public void validateDelete_IdNotPositive() {
        ValidationResult<List<Long>, Defect> actual =
                deleteCalloutValidationService.validateDelete(SHARD, CLIENT_ID, singletonList(-1L));

        assertThat(actual, hasDefectWithDefinition(validationError(path(index(0)), validId())));
    }

    @Test
    public void validateDelete_IdNull() {
        ValidationResult<List<Long>, Defect> actual =
                deleteCalloutValidationService.validateDelete(SHARD, CLIENT_ID, singletonList(null));

        assertThat(actual, hasDefectWithDefinition(validationError(path(index(0)), notNull())));
    }

    @Test
    public void validateDelete_IdInUse() {
        ValidationResult<List<Long>, Defect> actual =
                deleteCalloutValidationService.validateDelete(SHARD, CLIENT_ID, singletonList(2L));

        assertThat(actual, hasDefectWithDefinition(validationError(path(index(0)), adExtensionIsInUse())));
    }

    @Test
    public void validateDelete_IdAnotherClient() {
        ValidationResult<List<Long>, Defect> actual =
                deleteCalloutValidationService.validateDelete(SHARD, CLIENT_ID, singletonList(3L));

        assertThat(actual, hasDefectWithDefinition(validationError(path(index(0)), objectNotFound())));
    }


}
