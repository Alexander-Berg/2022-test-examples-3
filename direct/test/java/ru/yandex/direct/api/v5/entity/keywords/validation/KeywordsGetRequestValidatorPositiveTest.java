package ru.yandex.direct.api.v5.entity.keywords.validation;

import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.general.LimitOffset;
import com.yandex.direct.api.v5.keywords.GetRequest;
import com.yandex.direct.api.v5.keywords.KeywordsSelectionCriteria;
import one.util.streamex.LongStreamEx;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_IDS_COUNT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_OFFSET;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_IDS_COUNT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_OFFSET;
import static ru.yandex.direct.api.v5.entity.keywords.validation.KeywordsGetRequestValidator.MAX_ADGROUP_IDS_COUNT;
import static ru.yandex.direct.api.v5.entity.keywords.validation.KeywordsGetRequestValidator.MAX_CAMPAIGN_IDS_COUNT;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class KeywordsGetRequestValidatorPositiveTest {
    private static KeywordsGetRequestValidator requestValidator;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public GetRequest request;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"указан только SelectionCriteria.Ids и кол-во элементов в нем равно минимально допустимому",
                        new GetRequest().withSelectionCriteria(
                                new KeywordsSelectionCriteria()
                                        .withIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList()))
                },
                {"указан только SelectionCriteria.Ids и кол-во элементов в нем равно максимально допустимому",
                        new GetRequest().withSelectionCriteria(
                                new KeywordsSelectionCriteria()
                                        .withIds(LongStreamEx.range(DEFAULT_MAX_IDS_COUNT).boxed().toList()))
                },
                {"указан только SelectionCriteria.AdGroupIds и кол-во элементов в нем равно минимально допустимому",
                        new GetRequest().withSelectionCriteria(
                                new KeywordsSelectionCriteria()
                                        .withAdGroupIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList()))
                },
                {"указан только SelectionCriteria.AdGroupIds и кол-во элементов в нем равно максимально допустимому",
                        new GetRequest().withSelectionCriteria(
                                new KeywordsSelectionCriteria()
                                        .withAdGroupIds(LongStreamEx.range(MAX_ADGROUP_IDS_COUNT).boxed().toList()))
                },
                {"указан только SelectionCriteria.CampaignIds и кол-во элементов в нем равно минимально допустимому",
                        new GetRequest().withSelectionCriteria(
                                new KeywordsSelectionCriteria()
                                        .withCampaignIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList()))
                },
                {"указан только SelectionCriteria.CampaignIds и кол-во элементов в нем равно максимально допустимому",
                        new GetRequest().withSelectionCriteria(
                                new KeywordsSelectionCriteria()
                                        .withCampaignIds(LongStreamEx.range(MAX_CAMPAIGN_IDS_COUNT).boxed().toList()))
                },
                {"указаны SelectonCriteria.Ids, SelectionCriteria.AdGroupIds и SelectionCriteria.CampaignIds",
                        new GetRequest().withSelectionCriteria(
                                new KeywordsSelectionCriteria().withIds(singletonList(1L))
                                        .withAdGroupIds(singletonList(1L))
                                        .withCampaignIds(singletonList(1L)))
                },
                {"указаны SelectionCriteria.Ids и Page.Limit, значение Page.Limit равно минимально допустимому",
                        new GetRequest()
                                .withSelectionCriteria(new KeywordsSelectionCriteria().withIds(singletonList(1L)))
                                .withPage(new LimitOffset().withLimit(DEFAULT_MIN_LIMIT))
                },
                {"указаны SelectionCriteria.CampaignIds и Page.Limit, значение Page.Limit равно максимально допустимому",
                        new GetRequest()
                                .withSelectionCriteria(
                                        new KeywordsSelectionCriteria().withCampaignIds(singletonList(1L)))
                                .withPage(new LimitOffset().withLimit(DEFAULT_MAX_LIMIT))
                },
                {"указаны SelectionCriteria.Ids и Page.Offset, значение Page.Offset равно минимально допустимому",
                        new GetRequest()
                                .withSelectionCriteria(new KeywordsSelectionCriteria().withIds(singletonList(1L)))
                                .withPage(new LimitOffset().withOffset(DEFAULT_MIN_OFFSET))
                },
                {"указаны SelectionCriteria.AdGroupIds и Page.Offset, значение Page.Offset равно максимально допустимому",
                        new GetRequest()
                                .withSelectionCriteria(
                                        new KeywordsSelectionCriteria().withCampaignIds(singletonList(1L)))
                                .withPage(new LimitOffset().withOffset(DEFAULT_MAX_OFFSET))
                },
                {"указаны SelectionCriteria.Ids, SelectionCriteria.AdGroupIds и SelectionCriteria.CampaignIds, а так же Page.Limit и Page.Offset",
                        new GetRequest()
                                .withSelectionCriteria(
                                        new KeywordsSelectionCriteria().withIds(asList(1L, 2L, 3L))
                                                .withAdGroupIds(asList(1L, 2L, 3L)).withCampaignIds(asList(1L, 2L, 3L)))
                                .withPage(new LimitOffset().withLimit(10L).withOffset(10L))
                }
        });
    }

    @BeforeClass
    public static void setUp() {
        requestValidator = new KeywordsGetRequestValidator();
    }

    @Test
    public void test() {
        ValidationResult<GetRequest, DefectType> validationResult = requestValidator.validate(request);
        assertThat(validationResult.hasAnyErrors()).isEqualTo(false);
    }

}

