package ru.yandex.direct.core.entity.moderation.service.receiving.processor;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.makeBannerResponse;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.makeTextBannerResponse;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ChooseResponseWithMaxVersionTest {

    private static final long CRITICAL_REASON = 12L;

    private final ModerationResponseParser<BannerModerationResponse, Long> responseParser =
            ModerationResponseParserImpl.<BannerModerationResponse>builder()
                    .withIdGetter(this::getId)
                    .withReasonsGetter(this::getReasons)
                    .withUnixtimeGetter(this::getUnixtime)
                    .withIsValid(e -> true)
                    .withVersionGetter(this::getVersion)
                    .withHasCriticalReason((a, b) -> b.stream().anyMatch(e -> e == CRITICAL_REASON))
                    .build();

    @Test
    public void chosenMaxVersionWhenNoCriticalReasons() {
        List<BannerModerationResponse> responses = List.of(
                makeTextBannerResponse(1891651, 10L, Yes, List.of()),
                makeTextBannerResponse(1891651, 1L, Yes, List.of()),
                makeTextBannerResponse(1891651, 33L, Yes, List.of()),
                makeTextBannerResponse(1891651, 78L, Yes, List.of()),
                makeTextBannerResponse(1891651, Long.MAX_VALUE, Yes, List.of()),
                makeTextBannerResponse(1891651, 1100L, Yes, List.of()),
                makeTextBannerResponse(1891651, 200L, Yes, List.of()),
                makeTextBannerResponse(1891651, 10L, No, List.of())
        );

        BannerModerationResponse bannerModerationResponse =
                ChooseResponseWithMaxVersion.INSTANCE.apply(responses, responseParser);

        assertThat(bannerModerationResponse.getMeta().getVersionId()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void chosenMaxVersionOfCriticalReasonWhenNotCriticalReasonsPresent() {
        List<BannerModerationResponse> responses = List.of(
                makeTextBannerResponse(1891651, 10L, Yes, List.of()),
                makeTextBannerResponse(1891651, 1L, Yes, List.of(CRITICAL_REASON)),
                makeTextBannerResponse(1891651, 33L, Yes, List.of(CRITICAL_REASON)),
                makeTextBannerResponse(1891651, 78L, Yes, List.of()),
                makeTextBannerResponse(1891651, Long.MAX_VALUE, Yes, List.of()),
                makeTextBannerResponse(1891651, 1100L, Yes, List.of()),
                makeTextBannerResponse(1891651, 200L, Yes, List.of(CRITICAL_REASON)),
                makeTextBannerResponse(1891651, 10L, No, List.of(CRITICAL_REASON))
        );

        BannerModerationResponse bannerModerationResponse =
                ChooseResponseWithMaxVersion.INSTANCE.apply(responses, responseParser);

        assertThat(bannerModerationResponse.getMeta().getVersionId()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void chosenMaxVersionOfCriticalReasonWhenOnlyCriticalReasonsPresent() {
        List<BannerModerationResponse> responses = List.of(
                makeTextBannerResponse(1891651, 1L, Yes, List.of(CRITICAL_REASON)),
                makeTextBannerResponse(1891651, 33L, Yes, List.of(CRITICAL_REASON)),
                makeTextBannerResponse(1891651, 200L, Yes, List.of(CRITICAL_REASON)),
                makeTextBannerResponse(1891651, 10L, No, List.of(CRITICAL_REASON))
        );

        BannerModerationResponse bannerModerationResponse =
                ChooseResponseWithMaxVersion.INSTANCE.apply(responses, responseParser);

        assertThat(bannerModerationResponse.getMeta().getVersionId()).isEqualTo(200L);
    }

    @Test
    public void chosenMaxUnixtimeOfSameVersion() {
        List<BannerModerationResponse> responses = List.of(
                makeBannerResponse(1891651, 10L, No, 3L, ModerationObjectType.TEXT_AD),
                makeBannerResponse(1891651, 10L, Yes, 5L, ModerationObjectType.TEXT_AD),
                makeBannerResponse(1891651, 10L, No, 1L, ModerationObjectType.TEXT_AD),
                makeBannerResponse(1891651, 5L, No, 10L, ModerationObjectType.TEXT_AD)
        );

        BannerModerationResponse bannerModerationResponse =
                ChooseResponseWithMaxVersion.INSTANCE.apply(responses, responseParser);

        assertThat(bannerModerationResponse.getUnixtime()).isEqualTo(5);
        assertThat(bannerModerationResponse.getResult().getVerdict()).isEqualTo(Yes);
    }

    private long getId(BannerModerationResponse response) {
        return response.getMeta().getBannerId();
    }

    private long getVersion(BannerModerationResponse response) {
        return response.getMeta().getVersionId();
    }

    private List<Long> getReasons(BannerModerationResponse response) {
        return response.getResult().getReasons();
    }

    private long getUnixtime(BannerModerationResponse response) {
        return response.getUnixtime();
    }
}
