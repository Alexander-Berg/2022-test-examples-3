package ru.yandex.direct.api.v5.entity.promotedcontent.delegate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.promotedcontent.AddRequest;
import com.yandex.direct.api.v5.promotedcontent.AddTypeEnum;
import com.yandex.direct.api.v5.promotedcontent.PromotedContentAddItem;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Answers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.contentpromotion.ContentPromotionService;
import ru.yandex.direct.core.entity.contentpromotion.ContentPromotionSingleObjectRequest;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.api.v5.entity.promotedcontent.Constants.MAX_COLLECTION_ELEMENTS_PER_ADD;
import static ru.yandex.direct.api.v5.entity.promotedcontent.Constants.MAX_EDA_ELEMENTS_PER_ADD;
import static ru.yandex.direct.api.v5.entity.promotedcontent.Constants.MAX_ELEMENTS_PER_ADD;
import static ru.yandex.direct.api.v5.entity.promotedcontent.Constants.MAX_SERVICE_ELEMENTS_PER_ADD;
import static ru.yandex.direct.api.v5.entity.promotedcontent.Constants.MAX_VIDEO_ELEMENTS_PER_ADD;
import static ru.yandex.direct.api.v5.entity.promotedcontent.PromotedContentDefectTypes.maxContentPerAddRequest;
import static ru.yandex.direct.api.v5.entity.promotedcontent.PromotedContentDefectTypes.wrongContentType;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.DefectTypes.absentElementInArray;
import static ru.yandex.direct.api.v5.validation.DefectTypes.emptyValue;
import static ru.yandex.direct.api.v5.validation.DefectTypes.invalidValue;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@Api5Test
@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class AddContentDelegateTest {
    private final long clientId = 13371337;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApiAuthenticationSource authentication;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApiContextHolder apiContextHolder;

    @Mock
    ContentPromotionService contentPromotionService;
    @Autowired
    ResultConverter resultConverter;

    private AddContentDelegate delegate;

    @Before
    public void before() {
        initMocks(this);
        when(authentication.getChiefSubclient().getClientId().asLong()).thenReturn(clientId);
        when(apiContextHolder.get().getApiLogRecord()).thenReturn(null);
        //when(contentPromotionService.addContentPromotion())
        delegate = new AddContentDelegate(authentication, resultConverter, apiContextHolder, contentPromotionService);
    }

    @Parameterized.Parameters(name = "{0}")
    private static Collection<Object[]> allPromotionTypesWithMaxValue() {
        return Arrays.asList(new Object[][]{
                {AddTypeEnum.COLLECTION, MAX_COLLECTION_ELEMENTS_PER_ADD},
                {AddTypeEnum.VIDEO, MAX_VIDEO_ELEMENTS_PER_ADD},
                {AddTypeEnum.SERVICE, MAX_SERVICE_ELEMENTS_PER_ADD},
                {AddTypeEnum.EDA, MAX_EDA_ELEMENTS_PER_ADD},
        });
    }

    @Test
    public void singleValidContent_validateRequest_Success() {
        var validationResult = delegate.validateRequest(new AddRequest().withPromotedContent(
                createPromotedContent(AddTypeEnum.VIDEO, 1)));
        assertThat(validationResult).is(hasNoDefects());
    }

    @Test
    public void nullAsAContent_validateRequest_validationError() {
        var validationResult = delegate.validateRequest(new AddRequest().withPromotedContent(
                (PromotedContentAddItem) null));
        assertThat(validationResult).is(hasDefectWith(
                validationError(path(field("promotedContent")), absentElementInArray())));
    }

    @Test
    public void manyVideoRequests_validateRequest_validationError() {
        var validationResult = delegate.validateRequest(new AddRequest().withPromotedContent(
                createPromotedContent(AddTypeEnum.VIDEO, MAX_ELEMENTS_PER_ADD + 1)
        ));
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(validationResult).is(hasDefectWith(validationError(path(field("promotedContent")),
                maxContentPerAddRequest(MAX_ELEMENTS_PER_ADD))));
        sa.assertThat(validationResult).is(hasDefectWith(validationError(path(field("promotedContent")),
                maxContentPerAddRequest(MAX_VIDEO_ELEMENTS_PER_ADD, AddTypeEnum.VIDEO))));
        sa.assertAll();
    }

    @Test
    public void manyServiceRequests_validateRequest_validationError() {
        var validationResult = delegate.validateRequest(new AddRequest().withPromotedContent(
                createPromotedContent(AddTypeEnum.SERVICE, MAX_ELEMENTS_PER_ADD + 1)
        ));
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(validationResult).is(hasDefectWith(validationError(path(field("promotedContent")),
                maxContentPerAddRequest(MAX_ELEMENTS_PER_ADD))));
        sa.assertThat(validationResult).is(hasDefectWith(validationError(path(field("promotedContent")),
                maxContentPerAddRequest(MAX_SERVICE_ELEMENTS_PER_ADD, AddTypeEnum.SERVICE))));
        sa.assertAll();
    }

    @Test
    @Parameters(method = "allPromotionTypesWithMaxValue")
    public void manyObjects_validateRequest_validationError(AddTypeEnum addTypeEnum, int maxValue) {
        var validationResult = delegate.validateRequest(new AddRequest()
                .withPromotedContent(createPromotedContent(addTypeEnum, maxValue + 1)));
        assertThat(validationResult).is(hasDefectWith(validationError(path(field("promotedContent")),
                maxContentPerAddRequest(maxValue, addTypeEnum))));
    }

    @Test
    @Parameters(method = "allPromotionTypesWithMaxValue")
    public void manyObjects_validateRequest_Success(AddTypeEnum addTypeEnum, int maxValue) {
        var validationResult = delegate.validateRequest(new AddRequest()
                .withPromotedContent(createPromotedContent(addTypeEnum, maxValue)));
        assertThat(validationResult).is(hasNoDefects());
    }

    @Test
    public void nullAsContentType_validateInternalRequest_validationError() {
        List<ContentPromotionSingleObjectRequest> requests = singletonList(new ContentPromotionSingleObjectRequest()
                .withRequestId("0")
                .withUrl("someUrl"));
        var validationResult = delegate.validateInternalRequest(requests);
        assertThat(validationResult).is(hasDefectWith(
                validationError(path(index(0), field("type")), wrongContentType())));
    }

    @Test
    public void nullAsUrl_validateInternalRequest_validationError() {
        List<ContentPromotionSingleObjectRequest> requests = singletonList(new ContentPromotionSingleObjectRequest()
                .withRequestId("0")
                .withContentType(ContentPromotionContentType.VIDEO));
        var validationResult = delegate.validateInternalRequest(requests);
        assertThat(validationResult).is(hasDefectWith(
                validationError(path(index(0), field("url")), invalidValue())));
    }

    @Test
    public void blankUrl_validateInternalRequest_validationError() {
        List<ContentPromotionSingleObjectRequest> requests = singletonList(new ContentPromotionSingleObjectRequest()
                .withRequestId("0")
                .withContentType(ContentPromotionContentType.VIDEO)
                .withUrl(" "));
        var validationResult = delegate.validateInternalRequest(requests);
        assertThat(validationResult).is(hasDefectWith(
                validationError(path(index(0), field("url")), emptyValue())));
    }

    @Test
    public void singleValidContent_validateInternalRequest_Success() {
        List<ContentPromotionSingleObjectRequest> requests = singletonList(new ContentPromotionSingleObjectRequest()
                .withRequestId("0")
                .withUrl("someUrl")
                .withContentType(ContentPromotionContentType.COLLECTION));
        var validationResult = delegate.validateInternalRequest(requests);
        assertThat(validationResult).is(hasNoDefects());

    }

    private List<PromotedContentAddItem> createPromotedContent(AddTypeEnum type, int size) {
        return Stream.generate(() -> new PromotedContentAddItem().withType(type).withUrl("someurl"))
                .limit(size)
                .collect(Collectors.toList());
    }
}
