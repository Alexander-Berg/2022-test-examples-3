package ru.yandex.direct.api.v5.entity.adextensions.converter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.yandex.direct.api.v5.adextensions.AdExtensionFieldEnum;
import com.yandex.direct.api.v5.adextensions.AdExtensionsSelectionCriteria;
import com.yandex.direct.api.v5.adextensions.GetRequest;
import com.yandex.direct.api.v5.adextensiontypes.AdExtensionStateSelectionEnum;
import com.yandex.direct.api.v5.adextensiontypes.AdExtensionTypeEnum;
import com.yandex.direct.api.v5.general.ExtensionStatusSelectionEnum;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.addition.callout.container.CalloutSelection;
import ru.yandex.direct.core.entity.addition.callout.model.CalloutsStatusModerate;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;

public class GetRequestConverterExtractSelectionCriteriaTest {
    private static final List<Long> ID_LIST = Arrays.asList(1L, 2L, 3L);
    private static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(2017, 1, 1, 4, 1, 1);
    private static final String DATE_TIME_STRING = "2017-01-01T01:01:01Z";

    private GetRequestConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = new GetRequestConverter(MOSCOW_TIMEZONE);
    }

    @Test
    public void extractSelectionCriteria_MinimalSelectionCriteria_CheckConvertedValue() {
        GetRequest req = createMinimalGetRequest();
        CalloutSelection selection = converter.extractSelectionCriteria(req);
        assertThat(selection)
                .usingRecursiveComparison()
                .isEqualTo(createMinimalExpectedCalloutSelection());
    }

    @Test
    public void extractSelectionCriteria_AllFieldFilled_CheckConvertedValue() {
        GetRequest req = createGetRequest();
        CalloutSelection selection = converter.extractSelectionCriteria(req);
        assertThat(selection)
                .usingRecursiveComparison()
                .isEqualTo(new CalloutSelection()
                        .withIds(ID_LIST)
                        .withStatuses(EnumSet.allOf(CalloutsStatusModerate.class))
                        .withDeleted(null)
                        .withLastChangeGreaterOrEqualThan(LOCAL_DATE_TIME)
                );
    }

    @Test
    public void extractSelectionCriteria_StateEmptyAndIdsEmpty_SelectDeletedShouldBeFalse() {
        GetRequest req = createMinimalGetRequest();
        CalloutSelection selection = converter.extractSelectionCriteria(req);
        assertThat(selection.getDeleted()).isFalse();
    }

    @Test
    public void extractSelectionCriteria_StateEmptyAndIdsDefined_SelectDeletedShouldBeNull() {
        GetRequest req = createMinimalGetRequest();
        req.getSelectionCriteria().setIds(ID_LIST);
        CalloutSelection selection = converter.extractSelectionCriteria(req);
        assertThat(selection.getDeleted()).isNull();
    }

    @Test
    public void extractSelectionCriteria_StateOn_SelectDeletedShouldBeFalse() {
        GetRequest req = createMinimalGetRequest();
        req.getSelectionCriteria().setStates(singletonList(AdExtensionStateSelectionEnum.ON));
        CalloutSelection selection = converter.extractSelectionCriteria(req);
        assertThat(selection.getDeleted()).isFalse();
    }

    @Test
    public void extractSelectionCriteria_StateDeleted_SelectDeletedShouldBeTrue() {
        GetRequest req = createMinimalGetRequest();
        req.getSelectionCriteria().setStates(singletonList(AdExtensionStateSelectionEnum.DELETED));
        CalloutSelection selection = converter.extractSelectionCriteria(req);
        assertThat(selection.getDeleted()).isTrue();
    }

    @Test
    public void extractSelectionCriteria_StatusModeration_CheckStatusModerateExtending() {
        GetRequest req = createMinimalGetRequest();
        req.getSelectionCriteria().setStatuses(singletonList(ExtensionStatusSelectionEnum.MODERATION));
        CalloutSelection selection = converter.extractSelectionCriteria(req);
        assertThat(selection.getStatuses()).containsExactlyInAnyOrder(CalloutsStatusModerate.SENT,
                CalloutsStatusModerate.SENDING, CalloutsStatusModerate.READY);
    }

    private GetRequest createGetRequest() {
        return new GetRequest()
                .withSelectionCriteria(
                        new AdExtensionsSelectionCriteria()
                                .withIds(ID_LIST)
                                .withTypes(EnumSet.allOf(AdExtensionTypeEnum.class))
                                .withStates(EnumSet.allOf(AdExtensionStateSelectionEnum.class))
                                .withStatuses(EnumSet.allOf(ExtensionStatusSelectionEnum.class))
                                .withModifiedSince(DATE_TIME_STRING)
                )
                .withFieldNames(AdExtensionFieldEnum.ID);
    }

    private GetRequest createMinimalGetRequest() {
        return new GetRequest()
                .withSelectionCriteria(new AdExtensionsSelectionCriteria())
                .withFieldNames(AdExtensionFieldEnum.ID);
    }

    private CalloutSelection createMinimalExpectedCalloutSelection() {
        return new CalloutSelection()
                .withIds(emptyList())
                .withStatuses(emptySet())
                .withDeleted(Boolean.FALSE);
    }
}
