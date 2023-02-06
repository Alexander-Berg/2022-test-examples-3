package ru.yandex.direct.jobs.moderation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.common.log.service.ModerationLogService;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.ModerationError;
import ru.yandex.direct.core.entity.moderation.model.ModerationErrorToken;
import ru.yandex.direct.jobs.moderation.errors.ModerationErrorCodeToJugglerStatus;
import ru.yandex.direct.jobs.moderation.processor.ModerationResponseProcessorFilter;
import ru.yandex.direct.juggler.JugglerStatus;
import ru.yandex.monlib.metrics.primitives.Counter;
import ru.yandex.monlib.metrics.registry.MetricRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationErrorResponseProcessorTest {

    private ModerationResponseProcessorFilter filter;
    private MetricRegistry metricRegistry;
    private ModerationLogService moderationLogService;
    private BiConsumer<JugglerStatus, String> jugglerDelegate;
    private ModerationErrorCodeToJugglerStatus moderationErrorCodeToJugglerStatus;
    private Counter counterMock;

    private ModerationErrorResponseProcessor moderationErrorResponseProcessor;

    @BeforeEach
    public void init() {
        filter = new ModerationResponseProcessorFilter(e -> true);
        metricRegistry = mock(MetricRegistry.class);
        moderationLogService = mock(ModerationLogService.class);
        jugglerDelegate = mock(BiConsumer.class);
        moderationErrorCodeToJugglerStatus = mock(ModerationErrorCodeToJugglerStatus.class);
        counterMock = mock(Counter.class);
        when(metricRegistry.counter(ModerationErrorResponseProcessor.ERROR_RESPONSE)).thenReturn(counterMock);

        moderationErrorResponseProcessor = new ModerationErrorResponseProcessor(filter, metricRegistry,
                moderationLogService, jugglerDelegate, moderationErrorCodeToJugglerStatus);
    }

    List<ModerationError> makeErrors(List<ModerationErrorToken> tokens) {
        BannerModerationMeta meta = new BannerModerationMeta();
        meta.setCampaignId(1L);
        meta.setAdGroupId(1L);
        meta.setBannerId(2L);

        return tokens.stream().map(t -> {
            ModerationError error = new ModerationError();
            error.setCode(t);
            error.setMeta(meta);
            return error;
        }).collect(Collectors.toList());
    }

    @Test
    public void processResponsesTest() {
        List<ModerationError> errors = makeErrors(List.of(ModerationErrorToken.NO_PARENT_OBJECT,
                ModerationErrorToken.UNKNOWN_ERROR));

        when(moderationErrorCodeToJugglerStatus.getJugglerStatus(ModerationErrorToken.NO_PARENT_OBJECT)).thenReturn(JugglerStatus.OK);
        when(moderationErrorCodeToJugglerStatus.getJugglerStatus(ModerationErrorToken.UNKNOWN_ERROR)).thenReturn(JugglerStatus.CRIT);

        moderationErrorResponseProcessor.processResponses(errors);

        verify(counterMock, times(1)).add(2);
        verify(jugglerDelegate, times(1)).accept(eq(JugglerStatus.CRIT), anyString());
    }

    @Test
    public void processResponses_checkPriorityTest() {
        List<ModerationError> errors = makeErrors(List.of(ModerationErrorToken.NO_PARENT_OBJECT,
                ModerationErrorToken.BAD_DATA,
                ModerationErrorToken.UNKNOWN_ERROR));

        when(moderationErrorCodeToJugglerStatus.getJugglerStatus(ModerationErrorToken.NO_PARENT_OBJECT)).thenReturn(JugglerStatus.WARN);
        when(moderationErrorCodeToJugglerStatus.getJugglerStatus(ModerationErrorToken.UNKNOWN_ERROR)).thenReturn(JugglerStatus.INFO);
        when(moderationErrorCodeToJugglerStatus.getJugglerStatus(ModerationErrorToken.BAD_DATA)).thenReturn(JugglerStatus.INFO);

        moderationErrorResponseProcessor.processResponses(errors);

        verify(counterMock, times(1)).add(3);
        verify(jugglerDelegate, times(1)).accept(eq(JugglerStatus.WARN), anyString());
    }

    @Test
    public void processResponses_allOkTest() {
        List<ModerationError> errors = makeErrors(List.of(ModerationErrorToken.NO_PARENT_OBJECT,
                ModerationErrorToken.NO_PARENT_OBJECT,
                ModerationErrorToken.NO_PARENT_OBJECT));

        when(moderationErrorCodeToJugglerStatus.getJugglerStatus(ModerationErrorToken.NO_PARENT_OBJECT)).thenReturn(JugglerStatus.OK);

        moderationErrorResponseProcessor.processResponses(errors);

        verify(counterMock, times(1)).add(3);
        verify(jugglerDelegate, never()).accept(any(), anyString());
    }

    @Test
    public void processResponses_NullTokenTest() {
        List<ModerationErrorToken> nullList = new ArrayList<>();
        nullList.add(null);
        List<ModerationError> errors = makeErrors(nullList);

        when(moderationErrorCodeToJugglerStatus.getJugglerStatus(null)).thenCallRealMethod();

        moderationErrorResponseProcessor.processResponses(errors);

        verify(counterMock, times(1)).add(1);
        verify(jugglerDelegate, times(1)).accept(eq(JugglerStatus.CRIT), anyString());
    }


}
