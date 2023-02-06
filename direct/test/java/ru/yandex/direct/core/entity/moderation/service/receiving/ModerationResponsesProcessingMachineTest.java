package ru.yandex.direct.core.entity.moderation.service.receiving;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.repository.bulk_update.BulkUpdateHolder;
import ru.yandex.direct.core.entity.moderation.service.receiving.processing_configurations.ResponseOperationsChain;
import ru.yandex.direct.core.entity.moderation.service.receiving.processor.ChooseResponseWithMaxVersion;
import ru.yandex.direct.core.entity.moderation.service.receiving.processor.ModerationResponseParser;
import ru.yandex.direct.core.entity.moderation.service.receiving.processor.ModerationResponseParserImpl;
import ru.yandex.direct.core.entity.moderation.service.receiving.processor.ModerationResponseProcessingResult;
import ru.yandex.direct.core.entity.moderation.service.receiving.processor.ModerationResponsesProcessingMachine;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.makeTextBannerResponse;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ModerationResponsesProcessingMachineTest {
    ResponseOperationsChain<BannerModerationResponse> responseResponseOperationsChain;
    PpcPropertiesSupport ppcPropertiesSupport;

    @Before
    public void before() {
        responseResponseOperationsChain = mock(ResponseOperationsChain.class);
    }

    @Test
    public void checkChoosingObject() {
        List<BannerModerationResponse> responses = List.of(
                makeTextBannerResponse(1L, 100L, Yes, List.of()),
                makeTextBannerResponse(1L, 1L, No, List.of(34L)),
                makeTextBannerResponse(1L, 3L, No, List.of(36L)),
                makeTextBannerResponse(2L, 10L, Yes, List.of()),
                makeTextBannerResponse(2L, 12L, Yes, List.of()),
                makeTextBannerResponse(3L, 14L, Yes, List.of()),
                makeTextBannerResponse(4L, 1L, Yes, List.of())
        );

        ModerationResponseParser<BannerModerationResponse, Long> responseParser =
                ModerationResponseParserImpl.<BannerModerationResponse>builder()
                        .withIdGetter(this::getId)
                        .withReasonsGetter(this::getReasons)
                        .withUnixtimeGetter(this::getUnixtime)
                        .withIsValid(e -> true)
                        .withVersionGetter(this::getVersion)
                        .withHasCriticalReason((a, b) -> false)
                        .build();

        ModerationResponsesProcessingMachine<BannerModerationResponse, Long> processor =
                ModerationResponsesProcessingMachine.<BannerModerationResponse, Long>builder()
                        .withResponseParser(responseParser)
                        .withModerateResponseChooser(ChooseResponseWithMaxVersion.INSTANCE)
                        .withObjectLocker(this::locker)
                        .withOperations(responseResponseOperationsChain)
                        .withResponsesConsumer((l, c) -> {
                            List<Long> ids =
                                    l.stream().map(e -> e.getMeta().getBannerId()).collect(Collectors.toList());
                            assertThat(ids).isNotEmpty();
                            assertThat(ids).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
                        })
                        .withBulkUpdateHolder(new BulkUpdateHolder())
                        .build();

        ModerationResponseProcessingResult<BannerModerationResponse> result = processor.processResponses(responses,
                mock(Configuration.class));

        assertThat(result.getUnknownResponses()).isEqualTo(0);

        BannerModerationResponse[] expected = new BannerModerationResponse[4];
        expected[0] = responses.get(0);
        expected[1] = responses.get(4);
        expected[2] = responses.get(5);
        expected[3] = responses.get(6);

        assertThat(result.getSuccessfulResponses()).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void checkLocking() {
        List<BannerModerationResponse> responses = List.of(
                makeTextBannerResponse(1L, 100L, Yes, List.of()),
                makeTextBannerResponse(2L, 10L, Yes, List.of()),
                makeTextBannerResponse(2L, 12L, Yes, List.of()),
                makeTextBannerResponse(3L, 14L, Yes, List.of()),
                makeTextBannerResponse(4L, 1L, Yes, List.of())
        );

        ModerationResponseParser<BannerModerationResponse, Long> responseParser =
                ModerationResponseParserImpl.<BannerModerationResponse>builder()
                        .withIdGetter(this::getId)
                        .withReasonsGetter(this::getReasons)
                        .withUnixtimeGetter(this::getUnixtime)
                        .withIsValid(e -> true)
                        .withVersionGetter(this::getVersion)
                        .withHasCriticalReason((a, b) -> false)
                        .build();

        ModerationResponsesProcessingMachine<BannerModerationResponse, Long> processor =
                ModerationResponsesProcessingMachine.<BannerModerationResponse, Long>builder()
                        .withResponseParser(responseParser)
                        .withModerateResponseChooser(ChooseResponseWithMaxVersion.INSTANCE)
                        .withObjectLocker((c, lst) -> lst.stream().map(BaseModerationReceivingService.ModeratedObjectKeyWithVersion::getKey)
                                .filter(el -> el == 4L)
                                .collect(Collectors.toList()))
                        .withOperations(responseResponseOperationsChain)
                        .withResponsesConsumer((l, c) -> {
                            List<Long> ids =
                                    l.stream().map(e -> e.getMeta().getBannerId()).collect(Collectors.toList());
                            assertThat(ids).isNotEmpty();
                            assertThat(ids).containsExactlyInAnyOrder(4L);
                        })
                        .withBulkUpdateHolder(new BulkUpdateHolder())
                        .build();

        ModerationResponseProcessingResult<BannerModerationResponse> result = processor.processResponses(responses,
                mock(Configuration.class));

        assertThat(result.getUnknownResponses()).isEqualTo(0);

        assertThat(result.getSuccessfulResponses()).containsExactlyInAnyOrder(responses.get(4));
    }

    @Test
    public void checkValidation() {
        List<BannerModerationResponse> responses = List.of(
                makeTextBannerResponse(1L, 100L, Yes, List.of()),
                makeTextBannerResponse(2L, 10L, Yes, List.of()),
                makeTextBannerResponse(2L, 12L, Yes, List.of()),
                makeTextBannerResponse(3L, 14L, Yes, List.of()),
                makeTextBannerResponse(4L, 1L, Yes, List.of())
        );

        ModerationResponseParser<BannerModerationResponse, Long> responseParser =
                ModerationResponseParserImpl.<BannerModerationResponse>builder()
                        .withIdGetter(this::getId)
                        .withReasonsGetter(this::getReasons)
                        .withUnixtimeGetter(this::getUnixtime)
                        .withIsValid(e -> (e.getMeta().getBannerId() & 1L) > 0)
                        .withVersionGetter(this::getVersion)
                        .withHasCriticalReason((a, b) -> false)
                        .build();

        ModerationResponsesProcessingMachine<BannerModerationResponse, Long> processor =
                ModerationResponsesProcessingMachine.<BannerModerationResponse, Long>builder()
                        .withResponseParser(responseParser)
                        .withModerateResponseChooser(ChooseResponseWithMaxVersion.INSTANCE)
                        .withObjectLocker(this::locker)
                        .withOperations(responseResponseOperationsChain)
                        .withResponsesConsumer((l, c) -> {
                            List<Long> ids =
                                    l.stream().map(e -> e.getMeta().getBannerId()).collect(Collectors.toList());
                            assertThat(ids).isNotEmpty();
                            assertThat(ids).containsExactlyInAnyOrder(1L, 3L);
                        })
                        .withBulkUpdateHolder(new BulkUpdateHolder())
                        .build();

        ModerationResponseProcessingResult<BannerModerationResponse> result = processor.processResponses(responses,
                mock(Configuration.class));

        assertThat(result.getUnknownResponses()).isEqualTo(3);

        BannerModerationResponse[] expected =
                responses.stream().filter(e -> (getId(e) & 1L) > 0).toArray(BannerModerationResponse[]::new);

        assertThat(result.getSuccessfulResponses()).containsExactlyInAnyOrder(expected);
    }

    private List<Long> locker(Configuration conf,
                              List<BaseModerationReceivingService.ModeratedObjectKeyWithVersion<Long>> responses) {
        return responses.stream().map(BaseModerationReceivingService.ModeratedObjectKeyWithVersion::getKey).collect(Collectors.toList());
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
