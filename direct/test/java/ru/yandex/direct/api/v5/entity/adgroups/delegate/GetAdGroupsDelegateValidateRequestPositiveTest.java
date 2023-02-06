package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.AdGroupsSelectionCriteria;
import com.yandex.direct.api.v5.adgroups.GetRequest;
import com.yandex.direct.api.v5.general.LimitOffset;
import one.util.streamex.LongStreamEx;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.ValidationResult;

import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_IDS_COUNT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_OFFSET;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_IDS_COUNT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_OFFSET;
import static ru.yandex.direct.api.v5.entity.adgroups.Constants.MAX_CAMPAIGN_IDS_COUNT;
import static ru.yandex.direct.api.v5.entity.adgroups.Constants.MIN_CAMPAIGN_IDS_COUNT;

@Api5Test
@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class GetAdGroupsDelegateValidateRequestPositiveTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private GetAdGroupsDelegate delegate;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public GetRequest request;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"указан только SelectionCriteria.Ids и кол-во элементов в нем равно минимально допустимому",
                        new GetRequest().withSelectionCriteria(
                                new AdGroupsSelectionCriteria()
                                        .withIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList()))
                },
                {"указан только SelectionCriteria.Ids и кол-во элементов в нем равно максимально допустимому",
                        new GetRequest().withSelectionCriteria(
                                new AdGroupsSelectionCriteria()
                                        .withIds(LongStreamEx.range(DEFAULT_MAX_IDS_COUNT).boxed().toList()))
                },
                {"указан только SelectionCriteria.CampaignIds и кол-во элементов в нем равно минимально допустимому",
                        new GetRequest().withSelectionCriteria(
                                new AdGroupsSelectionCriteria()
                                        .withCampaignIds(LongStreamEx.range(MIN_CAMPAIGN_IDS_COUNT).boxed().toList()))
                },
                {"указан только SelectionCriteria.CampaignIds и кол-во элементов в нем равно максимально допустимому",
                        new GetRequest().withSelectionCriteria(
                                new AdGroupsSelectionCriteria()
                                        .withCampaignIds(LongStreamEx.range(MAX_CAMPAIGN_IDS_COUNT).boxed().toList()))
                },
                {"указаны и SelectonCriteria.Ids и SelectionCriteria.CampaignIds",
                        new GetRequest().withSelectionCriteria(
                                new AdGroupsSelectionCriteria().withIds(Collections.singletonList(1L))
                                        .withCampaignIds(Collections.singletonList(1L)))
                },
                {"указаны SelectionCriteria.Ids и Page.Limit, значение Page.Limit равно минимально допустимому",
                        new GetRequest()
                                .withSelectionCriteria(
                                        new AdGroupsSelectionCriteria().withIds(Collections.singletonList(1L)))
                                .withPage(new LimitOffset().withLimit(DEFAULT_MIN_LIMIT))
                },
                {"указаны SelectionCriteria.CampaignIds и Page.Limit, значение Page.Limit равно максимально " +
                        "допустимому",
                        new GetRequest()
                                .withSelectionCriteria(
                                        new AdGroupsSelectionCriteria().withCampaignIds(Collections.singletonList(1L)))
                                .withPage(new LimitOffset().withLimit(DEFAULT_MAX_LIMIT))
                },
                {"указаны SelectionCriteria.Ids и Page.Offset, значение Page.Offset равно минимально допустимому",
                        new GetRequest()
                                .withSelectionCriteria(
                                        new AdGroupsSelectionCriteria().withIds(Collections.singletonList(1L)))
                                .withPage(new LimitOffset().withOffset(DEFAULT_MIN_OFFSET))
                },
                {"указаны SelectionCriteria.CampaignIds и Page.Offset, значение Page.Offset равно максимально " +
                        "допустимому",
                        new GetRequest()
                                .withSelectionCriteria(
                                        new AdGroupsSelectionCriteria().withCampaignIds(Collections.singletonList(1L)))
                                .withPage(new LimitOffset().withOffset(DEFAULT_MAX_OFFSET))
                },
                {"указаны SelectionCriteria.Ids и SelectionCriteria.CampaignIds, а так же Page.Limit и Page.Offset",
                        new GetRequest()
                                .withSelectionCriteria(
                                        new AdGroupsSelectionCriteria().withIds(Arrays.asList(1L, 2L, 3L))
                                                .withCampaignIds(Arrays.asList(1L, 2L, 3L)))
                                .withPage(new LimitOffset().withLimit(10L).withOffset(10L))
                }
        });
    }

    @Test
    public void test() {
        ValidationResult<GetRequest, DefectType> validationResult = delegate.validateRequest(request);
        Assertions.assertThat(validationResult.hasAnyErrors()).isEqualTo(false);
    }
}
