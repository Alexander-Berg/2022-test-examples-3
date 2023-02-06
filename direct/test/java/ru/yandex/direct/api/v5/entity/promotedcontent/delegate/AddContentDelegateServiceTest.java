package ru.yandex.direct.api.v5.entity.promotedcontent.delegate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.promotedcontent.AddRequest;
import com.yandex.direct.api.v5.promotedcontent.AddResponse;
import com.yandex.direct.api.v5.promotedcontent.AddTypeEnum;
import com.yandex.direct.api.v5.promotedcontent.PromotedContentAddItem;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
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
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.contentpromotion.ContentPromotionService;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.promotedcontent.PromotedContentDefectTypes.wrongContentType;
import static ru.yandex.direct.api.v5.validation.DefectTypes.emptyValue;
import static ru.yandex.direct.api.v5.validation.DefectTypes.invalidValue;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@Api5Test
@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class AddContentDelegateServiceTest {
    private static final String URL = "https://service-url.ru";
    private static final String METADATA = "{\"service_url\": \"https://service-url.ru\"}";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private ContentPromotionRepository contentPromotionRepository;
    @Autowired
    private ContentPromotionService contentPromotionService;
    @Autowired
    private Steps steps;

    private GenericApiService genericApiService;
    private AddContentDelegate delegate;
    private ApiAuthenticationSource auth;

    ClientId clientId;

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
        clientId = clientInfo.getClientId();

        auth = mock(ApiAuthenticationSource.class);
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));

        delegate = new AddContentDelegate(auth, resultConverter, apiContextHolder, contentPromotionService);
    }

    @Parameterized.Parameters(name = "{0}")
    private static Collection<Object[]> serviceAndEda() {
        return Arrays.asList(new Object[][]{
                {AddTypeEnum.SERVICE, ContentPromotionContentType.SERVICE},
                {AddTypeEnum.EDA, ContentPromotionContentType.EDA},
        });
    }

    @Test
    @Parameters(method = "serviceAndEda")
    public void doAction_ContentCreated(AddTypeEnum addTypeEnum, ContentPromotionContentType expectedContentType) {
        servicesApplication();
        AddRequest request = new AddRequest()
                .withPromotedContent(new PromotedContentAddItem()
                        .withType(addTypeEnum)
                        .withUrl(URL));
        AddResponse response = genericApiService.doAction(delegate, request);

        assumeThat(response.getAddResults(), hasSize(1));

        long contentId = response.getAddResults().get(0).getId();
        List<ContentPromotionContent> contents = contentPromotionRepository
                .getContentPromotion(clientId, List.of(contentId));
        ContentPromotionContent expectedContent = new ContentPromotionContent()
                .withId(contentId)
                .withClientId(clientId.asLong())
                .withType(expectedContentType)
                .withUrl(URL)
                .withIsInaccessible(false)
                .withMetadata(METADATA);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(contents).hasSize(1);
            softly.assertThat(contents.get(0)).isEqualToIgnoringNullFields(expectedContent);
        });
    }

    @Test
    public void doAction_NotAllowedApp_ContentNotCreated() {
        AddRequest request = new AddRequest()
                .withPromotedContent(new PromotedContentAddItem()
                        .withType(AddTypeEnum.SERVICE)
                        .withUrl(URL));
        AddResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAddResults()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getId()).isNull();
            softly.assertThat(response.getAddResults().get(0).getErrors()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getErrors().get(0).getCode())
                    .isEqualTo(wrongContentType().getCode());
        });
    }

    @Test
    public void doAction_App_WithoutType_ContentNotCreated() {
        servicesApplication();
        AddRequest request = new AddRequest()
                .withPromotedContent(new PromotedContentAddItem()
                        .withUrl(URL));
        AddResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAddResults()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getId()).isNull();
            softly.assertThat(response.getAddResults().get(0).getErrors()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getErrors().get(0).getCode())
                    .isEqualTo(wrongContentType().getCode());
        });
    }

    @Test
    @Parameters(method = "serviceAndEda")
    public void doAction_App_WithoutUrl_ContentNotCreated(AddTypeEnum addTypeEnum,
                                                      ContentPromotionContentType expectedContentType) {
        servicesApplication();
        AddRequest request = new AddRequest()
                .withPromotedContent(new PromotedContentAddItem()
                        .withType(addTypeEnum));
        AddResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAddResults()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getId()).isNull();
            softly.assertThat(response.getAddResults().get(0).getErrors()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getErrors())
                    .matches(t -> t.stream().anyMatch(er -> er.getCode().equals(invalidValue().getCode())));
        });
    }

    @Test
    @Parameters(method = "serviceAndEda")
    public void doAction_App_WithBlankUrl_ContentNotCreated(AddTypeEnum addTypeEnum,
                                                            ContentPromotionContentType expectedContentType) {
        servicesApplication();
        AddRequest request = new AddRequest()
                .withPromotedContent(new PromotedContentAddItem()
                        .withType(addTypeEnum)
                        .withUrl("   "));
        AddResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAddResults()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getId()).isNull();
            softly.assertThat(response.getAddResults().get(0).getErrors()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getErrors())
                    .matches(t -> t.stream().anyMatch(er -> er.getCode().equals(emptyValue().getCode())));
        });
    }

    private void servicesApplication() {
        when(auth.isServicesApplication()).thenReturn(true);
    }
}
