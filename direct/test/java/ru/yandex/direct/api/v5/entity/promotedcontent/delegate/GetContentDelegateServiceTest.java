package ru.yandex.direct.api.v5.entity.promotedcontent.delegate;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.promotedcontent.AddTypeEnum;
import com.yandex.direct.api.v5.promotedcontent.GetRequest;
import com.yandex.direct.api.v5.promotedcontent.GetResponse;
import com.yandex.direct.api.v5.promotedcontent.GetTypeEnum;
import com.yandex.direct.api.v5.promotedcontent.PromotedContentFieldEnum;
import com.yandex.direct.api.v5.promotedcontent.PromotedContentSelectionCriteria;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.entity.ApiValidationException;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Api5Test
@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class GetContentDelegateServiceTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private ContentPromotionRepository contentPromotionRepository;
    @Autowired
    private Steps steps;

    private GenericApiService genericApiService;
    private GetContentDelegate delegate;
    private ApiAuthenticationSource auth;

    private static Long serviceContentId;
    private static Long edaContentId;

    @Before
    public void before() {
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(
                apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class));

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();

        auth = mock(ApiAuthenticationSource.class);
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));

        delegate = new GetContentDelegate(auth, contentPromotionRepository, mock(PropertyFilter.class));

        serviceContentId = steps.contentPromotionSteps()
                .createContentPromotionContent(clientId, ContentPromotionContentType.SERVICE)
                .getId();
        edaContentId = steps.contentPromotionSteps()
                .createContentPromotionContent(clientId, ContentPromotionContentType.EDA)
                .getId();
        steps.contentPromotionSteps().createContentPromotionContent(clientId,
                ContentPromotionContentType.COLLECTION);
    }


    @Parameterized.Parameters(name = "{0}")
    private static Collection<Object[]> serviceAndEda() {
        return Arrays.asList(new Object[][]{
                {AddTypeEnum.SERVICE, (Supplier<Long>) () -> serviceContentId, GetTypeEnum.SERVICE},
                {AddTypeEnum.EDA, (Supplier<Long>) () -> edaContentId, GetTypeEnum.EDA},
        });
    }

    @Test
    @Parameters(method = "serviceAndEda")
    @TestCaseName("doAction_ServicesApp_SelectById_ContentReturned with content type {0}")
    public void doAction_ServicesApp_SelectById_ContentReturned(AddTypeEnum addTypeEnum,
                                                                Supplier<Long> contentId,
                                                                GetTypeEnum getTypeEnum) {
        servicesApplication();
        GetRequest request = new GetRequest()
                .withFieldNames(PromotedContentFieldEnum.ID)
                .withSelectionCriteria(new PromotedContentSelectionCriteria().withIds(contentId.get()));
        GetResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getPromotedContent()).hasSize(1);
            softly.assertThat(response.getPromotedContent().get(0).getId()).isEqualTo(contentId.get());
        });
    }

    @Test
    public void doAction_NotAppropriateApp_SelectById_ContentNotReturned() {
        GetRequest request = new GetRequest()
                .withFieldNames(PromotedContentFieldEnum.ID)
                .withSelectionCriteria(new PromotedContentSelectionCriteria().withIds(serviceContentId));
        GetResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getPromotedContent()).isEmpty();
    }

    @Test
    @Parameters(method = "serviceAndEda")
    @TestCaseName("doAction_ServicesApp_SelectByType_ContentReturned with content type {0}")
    public void doAction_ServicesApp_SelectByType_ContentReturned(AddTypeEnum addTypeEnum,
                                                                  Supplier<Long> contentId,
                                                                  GetTypeEnum getTypeEnum) {
        servicesApplication();
        GetRequest request = new GetRequest()
                .withFieldNames(PromotedContentFieldEnum.ID, PromotedContentFieldEnum.TYPE)
                .withSelectionCriteria(new PromotedContentSelectionCriteria().withTypes(addTypeEnum));
        GetResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getPromotedContent()).hasSize(1);
            softly.assertThat(response.getPromotedContent().get(0).getId()).isEqualTo(contentId.get());
            softly.assertThat(response.getPromotedContent().get(0).getType()).isEqualTo(getTypeEnum);
        });
    }

    @Test(expected = ApiValidationException.class)
    public void doAction_NotServicesApp_SelectByType_ExceptionIsThrown() {
        GetRequest request = new GetRequest()
                .withFieldNames(PromotedContentFieldEnum.ID)
                .withSelectionCriteria(new PromotedContentSelectionCriteria().withTypes(AddTypeEnum.SERVICE));
        GetResponse response = genericApiService.doAction(delegate, request);
    }

    private void servicesApplication() {
        when(auth.isServicesApplication()).thenReturn(true);
    }
}
