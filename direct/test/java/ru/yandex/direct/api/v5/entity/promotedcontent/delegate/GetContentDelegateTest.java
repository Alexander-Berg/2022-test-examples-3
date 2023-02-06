package ru.yandex.direct.api.v5.entity.promotedcontent.delegate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.promotedcontent.AddTypeEnum;
import com.yandex.direct.api.v5.promotedcontent.GetRequest;
import com.yandex.direct.api.v5.promotedcontent.GetResponse;
import com.yandex.direct.api.v5.promotedcontent.PromotedContentFieldEnum;
import com.yandex.direct.api.v5.promotedcontent.PromotedContentGetItem;
import com.yandex.direct.api.v5.promotedcontent.PromotedContentSelectionCriteria;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.configuration.ApiConfiguration;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.testing.configuration.Api5TestingConfiguration;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_IDS_COUNT;
import static ru.yandex.direct.api.v5.entity.promotedcontent.PromotedContentDefectTypes.wrongContentType;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.DefectTypes.maxIdsInSelection;
import static ru.yandex.direct.api.v5.validation.DefectTypes.missedParamsInSelection;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ContextHierarchy({
        @ContextConfiguration(classes = Api5TestingConfiguration.class),
        @ContextConfiguration(classes = GetContentDelegateTest.OverridingConfiguration.class)
})
@Api5Test
@RunWith(SpringRunner.class)
public class GetContentDelegateTest {
    private final static long CLIENT_ID = 13371337;

    @Autowired
    private GetContentDelegate delegate;

    @Configuration
    static class OverridingConfiguration {
        @Bean(name = ApiConfiguration.API_AUTHENTICATION_SOURCE)
        @Primary
        public ApiAuthenticationSource apiAuthenticationSource() {
            ApiAuthenticationSource authentication = mock(ApiAuthenticationSource.class, Answers.RETURNS_DEEP_STUBS);
            when(authentication.getChiefSubclient().getClientId().asLong()).thenReturn(CLIENT_ID);
            return authentication;
        }
    }

    @Test
    public void manyIds_validateRequest_validationError() {
        var validationResult = delegate.validateRequest(new GetRequest().withSelectionCriteria(
                new PromotedContentSelectionCriteria().withIds(
                        LongStream.range(0, DEFAULT_MAX_IDS_COUNT + 1).boxed().collect(Collectors.toList())))
        );
        assertThat(validationResult).is( hasDefectWith(
                validationError(path(field("SelectionCriteria"), field("Ids")), maxIdsInSelection())));
    }

    @Test
    public void nullType_validateRequest_validationError() {
        var validationResult = delegate.validateRequest(new GetRequest().withSelectionCriteria(
                new PromotedContentSelectionCriteria().withTypes((AddTypeEnum) null))
        );
        assertThat(validationResult).is(hasDefectWith(
                validationError(path(field("SelectionCriteria"), field("Types"), index(0)), wrongContentType())));
    }

    @Test
    public void emptySelectionCriteria_validateRequest_validationError() {
        var validationResult = delegate.validateRequest(new GetRequest().withSelectionCriteria(
                new PromotedContentSelectionCriteria())
        );
        assertThat(validationResult).is(hasDefectWith(
                validationError(path(field("SelectionCriteria")), missedParamsInSelection("Ids, Types"))));
    }

    @Test
    public void validSelectionCriteria_validateRequest_Success() {
        var validationResult = delegate.validateRequest(new GetRequest().withSelectionCriteria(
                new PromotedContentSelectionCriteria().withTypes(AddTypeEnum.COLLECTION).withIds(1L, 2L))
        );
        assertThat(validationResult).is(hasNoDefects());
    }

    @Test
    public void convertResponse_FieldsUrlIsAvailable() {
        List<ContentPromotionContent> contentList = singletonList(collectionForTest());
        Set<PromotedContentFieldEnum> requestedFields = Set.of(PromotedContentFieldEnum.IS_AVAILABLE,
                PromotedContentFieldEnum.URL);
        GetResponse getResponse = delegate.convertGetResponse(contentList, requestedFields, null);
        assertThat(getResponse.getPromotedContent()).hasSize(1);
        PromotedContentGetItem getItem = getResponse.getPromotedContent().get(0);
        assertThat(getItem.getId()).isNull();
        assertThat(getItem.getType()).isNull();
        assertThat(getItem.getIsAvailable()).isEqualTo(YesNoEnum.YES);
        assertThat(getItem.getUrl()).isEqualTo("something");
    }

    @Test
    public void convertResponse_FieldsUrlId() {
        List<ContentPromotionContent> contentList = singletonList(collectionForTest());
        Set<PromotedContentFieldEnum> requestedFields = Set.of(PromotedContentFieldEnum.ID,
                PromotedContentFieldEnum.URL);
        GetResponse getResponse = delegate.convertGetResponse(contentList, requestedFields, null);
        assertThat(getResponse.getPromotedContent()).hasSize(1);
        PromotedContentGetItem getItem = getResponse.getPromotedContent().get(0);
        assertThat(getItem.getId()).isEqualTo(123L);
        assertThat(getItem.getType()).isNull();
        assertThat(getItem.getIsAvailable()).isNull();
        assertThat(getItem.getUrl()).isEqualTo("something");
    }

    private static ContentPromotionContent collectionForTest() {
        return new ContentPromotionContent()
                .withClientId(CLIENT_ID)
                .withType(ContentPromotionContentType.COLLECTION)
                .withId(123L)
                .withUrl("something")
                .withIsInaccessible(false);
    }
}
