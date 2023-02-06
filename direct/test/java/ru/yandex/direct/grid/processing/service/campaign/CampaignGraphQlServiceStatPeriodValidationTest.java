package ru.yandex.direct.grid.processing.service.campaign;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.promoextension.PromoExtensionRepository;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.model.GdStatPreset;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContext;
import ru.yandex.direct.grid.processing.model.client.GdClient;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.service.cache.GridCacheService;
import ru.yandex.direct.grid.processing.service.promoextension.PromoExtensionsDataLoader;
import ru.yandex.direct.grid.processing.service.shortener.GridShortenerService;
import ru.yandex.direct.grid.processing.service.validation.GridValidationResultConversionService;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.service.validation.GridValidationService.MAX_FROM_STAT_PERIOD;
import static ru.yandex.direct.grid.processing.service.validation.GridValidationService.MAX_TO_STAT_PERIOD;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdClientAutoOverdraftInfo;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(Parameterized.class)
public class CampaignGraphQlServiceStatPeriodValidationTest {
    private static final int TEST_SHARD = 1;
    private static final long TEST_ID = 2;

    private static final String TEST_LOGIN = "login";
    private static final LocalDate TEST_DATE = LocalDate.now().minusMonths(1);
    private static final Instant TEST_INSTANT = TEST_DATE.atStartOfDay().toInstant(ZoneOffset.UTC);

    private static final GridGraphQLContext TEST_CONTEXT = new GridGraphQLContext(null)
            .withInstant(TEST_INSTANT)
            .withFetchedFieldsReslover(FetchedFieldsResolverCoreUtil.buildFetchedFieldsResolver(true));
    private static final GdClient GD_CLIENT = new GdClient()
            .withChiefLogin(TEST_LOGIN)
            .withInfo(new GdClientInfo()
                    .withShard(TEST_SHARD)
                    .withId(TEST_ID)
                    .withAutoOverdraftInfo(defaultGdClientAutoOverdraftInfo()));

    @Mock
    private GridCacheService gridCacheService;

    @Mock
    private GridValidationResultConversionService validationResultConversionService;

    @InjectMocks
    private GridValidationService gridValidationService;

    @InjectMocks
    private CpmPriceCampaignInfoService cpmPriceCampaignInfoService;

    @Mock
    private CampaignInfoService campaignsInfoService;

    @Mock
    private GridShortenerService shortenerService;

    @Mock
    private PromoExtensionsDataLoader promoExtensionsDataLoader;

    @Mock
    private PromoExtensionRepository promoExtensionRepository;

    @Mock
    private FeatureService featureService;

    @Captor
    private ArgumentCaptor<GdCampaignsContainer> inputCaptor;

    private CampaignsGraphQlService campaignsGraphQlService;
    private GdCampaignsContainer input;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(new GdValidationResult())
                .when(validationResultConversionService).buildGridValidationResult(any(), any());
        doReturn(new GdCampaignsContext().withRowset(Collections.emptyList()))
                .when(gridCacheService).getResultAndSaveToCache(any(), any(), any(), any());

        campaignsGraphQlService = new CampaignsGraphQlService(gridCacheService, campaignsInfoService,
                shortenerService, gridValidationService, cpmPriceCampaignInfoService, featureService,
                promoExtensionsDataLoader);
    }

    @Parameterized.Parameter
    public String inputDescription;

    @Parameterized.Parameter(1)
    public GdStatRequirements statRequirementsInput;

    @Parameterized.Parameter(2)
    public GdStatRequirements statRequirementsExpected;

    @Parameterized.Parameter(3)
    public Class<? extends Throwable> exceptionClass;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        "preset=LAST_30DAYS, from=null, to=null",
                        new GdStatRequirements()
                                .withPreset(GdStatPreset.LAST_30DAYS),
                        new GdStatRequirements()
                                .withFrom(TEST_DATE.minusDays(29))
                                .withTo(TEST_DATE),
                        null,
                },
                {
                        "preset=null, from=TEST_DATE, to=TEST_DATE",
                        new GdStatRequirements()
                                .withFrom(TEST_DATE)
                                .withTo(TEST_DATE),
                        new GdStatRequirements()
                                .withFrom(TEST_DATE)
                                .withTo(TEST_DATE),
                        null,
                },
                {
                        "preset=null, from=TEST_DATE, to=TEST_DATE-3d",
                        new GdStatRequirements()
                                .withFrom(TEST_DATE)
                                .withTo(TEST_DATE.plusDays(3)),
                        new GdStatRequirements()
                                .withFrom(TEST_DATE)
                                .withTo(TEST_DATE.plusDays(3)),
                        null,
                },
                {
                        "preset=LAST_30DAYS, from=TEST_DATE, to=null",
                        new GdStatRequirements()
                                .withPreset(GdStatPreset.LAST_30DAYS)
                                .withFrom(TEST_DATE),
                        null,
                        GridValidationException.class,
                },
                {
                        "preset=null, from=null, to=null",
                        new GdStatRequirements(),
                        null,
                        GridValidationException.class,
                },
                {
                        "preset=null, from=TEST_DATE, to=null",
                        new GdStatRequirements()
                                .withFrom(TEST_DATE),
                        null,
                        GridValidationException.class,
                },
                {
                        "preset=null, from=null, to=TEST_DATE",
                        new GdStatRequirements()
                                .withTo(TEST_DATE),
                        null,
                        GridValidationException.class,
                },
                {
                        "preset=null, from=TEST_DATE, to=TEST_DATE-10d",
                        new GdStatRequirements()
                                .withFrom(TEST_DATE)
                                .withTo(TEST_DATE.minusDays(10)),
                        null,
                        GridValidationException.class,
                },
                {
                        "Invalid input — FROM before period limit",
                        new GdStatRequirements()
                                .withFrom(TEST_DATE.minusDays(MAX_FROM_STAT_PERIOD.toDays() + 1))
                                .withTo(TEST_DATE.minusDays(10)),
                        null,
                        GridValidationException.class,
                },
                {
                        "Invalid input — FROM and TO before period limit",
                        new GdStatRequirements()
                                .withFrom(TEST_DATE.minusDays(MAX_FROM_STAT_PERIOD.toDays() + 1))
                                .withTo(TEST_DATE.minusDays(MAX_TO_STAT_PERIOD.toDays() + 1)),
                        null,
                        GridValidationException.class,
                },
        });
    }

    @Before
    public void initTestData() {
        input = CampaignTestDataUtils.getDefaultCampaignsContainerInput()
                .withStatRequirements(statRequirementsInput);
    }


    @Test
    public void testStatPreset() {
        if (statRequirementsExpected != null) {
            testStatPresetCorrect();
        } else {
            testStatPresetException();
        }
    }

    private void testStatPresetCorrect() {
        campaignsGraphQlService.getCampaigns(TEST_CONTEXT, GD_CLIENT, input);

        verify(campaignsInfoService)
                .getFilteredCampaigns(eq(GD_CLIENT.getInfo()),
                        any(), any(), inputCaptor.capture(), eq(TEST_INSTANT), any());

        GdCampaignsContainer expectedInput = new GdCampaignsContainer()
                .withFilter(input.getFilter())
                .withOrderBy(input.getOrderBy())
                .withLimitOffset(input.getLimitOffset())
                .withStatRequirements(statRequirementsExpected);
        assertThat(inputCaptor.getValue())
                .is(matchedBy(beanDiffer(expectedInput)));
    }

    private void testStatPresetException() {
        assertThatThrownBy(() -> campaignsGraphQlService.getCampaigns(TEST_CONTEXT, GD_CLIENT, input))
                .isInstanceOf(exceptionClass);
    }
}
