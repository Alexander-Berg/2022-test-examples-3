package ru.yandex.direct.api.v5.entity.adextensions.delegate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;

import com.yandex.direct.api.v5.adextensions.AdExtensionGetItem;
import com.yandex.direct.api.v5.adextensions.GetResponse;
import com.yandex.direct.api.v5.adextensiontypes.AdExtensionTypeEnum;
import com.yandex.direct.api.v5.general.StateEnum;
import com.yandex.direct.api.v5.general.StatusEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.adextensions.container.GetFieldName;
import ru.yandex.direct.api.v5.entity.adextensions.container.InternalGetResponse;
import ru.yandex.direct.api.v5.entity.adextensions.converter.GetRequestConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.model.CalloutsStatusModerate;
import ru.yandex.direct.core.entity.addition.callout.service.CalloutService;
import ru.yandex.direct.core.entity.banner.type.banneradditions.BannerAdditionsRepository;
import ru.yandex.direct.core.entity.client.service.checker.ClientAccessCheckerTypeSupportFacade;
import ru.yandex.direct.core.entity.moderationreason.service.ModerationReasonService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class GetAdExtensionsDelegateConvertGetResponseMethodTest {
    public static final long CALLOUT_ID = 22L;
    public static final String STATUS_CLARIFICATION = "xxx";
    public static final String CALLOUT_TEXT = "aaaa";
    public static final long LIMITED_BY = 734L;
    private GetAdExtensionsDelegate getDelegate;

    @Before
    public void setUp() throws Exception {
        getDelegate = new GetAdExtensionsDelegate(
                mock(CalloutService.class),
                mock(ShardHelper.class),
                mock(BannerAdditionsRepository.class),
                mock(ModerationReasonService.class),
                mock(ApiAuthenticationSource.class),
                new PropertyFilter(),
                mock(GetRequestConverter.class),
                mock(ClientAccessCheckerTypeSupportFacade.class));
    }

    @Test
    public void convertGetResponse_Empty() throws Exception {
        GetResponse getResponse = getDelegate.convertGetResponse(Collections.emptyList(), Collections.emptySet(), null);
        assertThat(getResponse, beanDiffer(new GetResponse()));
    }

    @Test
    public void convertGetResponse_LimitBy() throws Exception {
        GetResponse getResponse = getDelegate.convertGetResponse(
                Collections.emptyList(), Collections.emptySet(), LIMITED_BY);
        assertThat(getResponse, beanDiffer(new GetResponse().withLimitedBy(LIMITED_BY)));
    }

    @Test
    public void convertGetResponse_Full() throws Exception {
        InternalGetResponse response = createInternalGetResponse();
        GetResponse getResponse = getDelegate.convertGetResponse(
                singletonList(response), EnumSet.allOf(GetFieldName.class), null);
        assertThat(getResponse, beanDiffer(new GetResponse().withAdExtensions(
                singletonList(
                        new AdExtensionGetItem()
                                .withId(CALLOUT_ID)
                                .withType(AdExtensionTypeEnum.CALLOUT)
                                .withState(StateEnum.DELETED)
                                .withStatus(StatusEnum.ACCEPTED)
                                .withAssociated(YesNoEnum.NO)
                                .withStatusClarification(STATUS_CLARIFICATION)
                                .withCallout(new com.yandex.direct.api.v5.adextensiontypes.Callout()
                                        .withCalloutText(CALLOUT_TEXT))
                )
        )));
    }

    @Test
    public void convertGetResponse_IdField() throws Exception {
        InternalGetResponse response = createInternalGetResponse();
        GetResponse getResponse = getDelegate.convertGetResponse(
                singletonList(response), singleton(GetFieldName.ID), null);
        assertThat(getResponse, beanDiffer(new GetResponse().withAdExtensions(
                singletonList(new AdExtensionGetItem().withId(CALLOUT_ID))
        )));
    }

    @Test
    public void convertGetResponse_CalloutTextField() throws Exception {
        InternalGetResponse response = createInternalGetResponse();
        GetResponse getResponse = getDelegate.convertGetResponse(
                singletonList(response), EnumSet.of(GetFieldName.CALLOUT, GetFieldName.CALLOUT_TEXT), null);
        assertThat(getResponse, beanDiffer(new GetResponse().withAdExtensions(
                singletonList(new AdExtensionGetItem().withCallout(
                        new com.yandex.direct.api.v5.adextensiontypes.Callout().withCalloutText(CALLOUT_TEXT)))
        )));
    }

    private InternalGetResponse createInternalGetResponse() {
        Callout callout = new Callout()
                .withId(CALLOUT_ID)
                .withClientId(33L)
                .withText(CALLOUT_TEXT)
                .withStatusModerate(CalloutsStatusModerate.YES)
                .withCreateTime(LocalDateTime.of(1212, 12, 12, 12, 12, 12))
                .withDeleted(Boolean.TRUE);
        return new InternalGetResponse(callout)
                .withStatusClarification(STATUS_CLARIFICATION)
                .withAssociated(Boolean.FALSE);
    }

}
