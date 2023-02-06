package ru.yandex.direct.jobs.moderation.reader;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.moderation.model.AbstractModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.moderation.ModerationReadMonitoring;
import ru.yandex.direct.jobs.moderation.reader.support.ModerationResponseConverter;
import ru.yandex.direct.jobs.moderation.reader.support.ModerationResponseSupportFacade;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.AUDIO_CREATIVE;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.CANVAS;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.CONTENT_PROMOTION_COLLECTION;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.CONTENT_PROMOTION_VIDEO;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.FIXCPM_YNDX_FRONTPAGE_CREATIVE;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.GEO_PIN_CREATIVE;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.HTML5;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.YNDX_FRONTPAGE_CREATIVE;
import static ru.yandex.direct.jobs.moderation.BannerModerationResponseTestUtils.getAppropriateBanner;
import static ru.yandex.direct.jobs.moderation.BannerModerationResponseTestUtils.getBannerModerationResponse;
import static ru.yandex.direct.utils.JsonUtils.fromJson;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@JobsTest
@ExtendWith(SpringExtension.class)
public class BannerModerationResponseConverterFacadeTest {

    @Autowired
    protected Steps steps;

    @Autowired
    protected List<ModerationResponseConverter> moderationSupports;

    private ModerationResponseSupportFacade supportFacade;

    private static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {AUDIO_CREATIVE},
                {YNDX_FRONTPAGE_CREATIVE},
                {FIXCPM_YNDX_FRONTPAGE_CREATIVE},
                {CONTENT_PROMOTION_COLLECTION},
                {CONTENT_PROMOTION_VIDEO},
                {GEO_PIN_CREATIVE},
                {HTML5},
                {CANVAS},
        });
    }

    @BeforeEach
    void before() {
        ModerationReadMonitoring readMonitoring = mock(ModerationReadMonitoring.class);
        supportFacade = new ModerationResponseSupportFacade(readMonitoring, moderationSupports);
    }

    @ParameterizedTest(name = "check converted: {0}")
    @MethodSource("parameters")
    void checkProcessModerationResponse_OneBanner_YesVerdict(ModerationObjectType moderationType) {
        AbstractBannerInfo bannerInfo = getAppropriateBanner(moderationType, steps);
        AbstractModerationResponse expectedResult = getBannerModerationResponse(moderationType, Yes, bannerInfo);
        AbstractModerationResponse actualResult = supportFacade.parseResponse(toJsonResponse(expectedResult));

        assertThat(actualResult, beanDiffer(expectedResult));
    }

    @Test
    void checkProcessModerationResponse_OneBanner_ParseError() {
        AbstractModerationResponse actualResult = supportFacade.parseResponse(fromJson("{\"test\" : \"test\"}"));

        assertThat(actualResult, nullValue());
    }

    private JsonNode toJsonResponse(AbstractModerationResponse response) {
        return fromJson(toJson(response));
    }
}
