package ru.yandex.market.delivery.tracker.service.tracking;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.tracker.dao.repository.DeliveryTrackDao;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.entity.NotificationResult;
import ru.yandex.market.delivery.tracker.domain.entity.NotificationResultStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PushingResultMessageParserTest {

    private static final String INVALID_MESSAGE = "Some of notifications were ignored for track: 1. Duplicate ids: " +
        "blaa, blaa, bla";
    private static final String INVALID_MESSAGE2 = "Some of notifications were ignored for track: 1. Duplicate ids:";
    private DeliveryTrackDao repo = mock(DeliveryTrackDao.class);
    private final PushingResultMessageParser pushingResultMessageParser = new PushingResultMessageParser(repo);
    private static final String VALID_MESSAGE = "Some of notifications were ignored for track: 1. Duplicate ids: 2, 3";

    @Test
    void parseResultWithOkStatus() {
        when(repo.getDeliveryTrackCheckpoints(anyLong()))
            .thenReturn(createTracks(1L, 2L, 3L));
        String comment = pushingResultMessageParser.parseComment(getResult("", NotificationResultStatus.OK));
        assertEquals("Push successful for track " + 1 + ". Pushed ids: " + Arrays.asList(1L, 2L, 3L), comment);
    }

    @Test
    void parseResultWithOkStatusAndEmptyCheckpointList() {
        when(repo.getDeliveryTrackCheckpoints(anyLong()))
            .thenReturn(Collections.emptyList());
        String comment = pushingResultMessageParser.parseComment(getResult("", NotificationResultStatus.OK));
        assertEquals("No new checkpoints for track " + 1, comment);
    }

    @Test
    void parseResultWithIgnoredStatusAndEmptyMessage() {
        when(repo.getDeliveryTrackCheckpoints(anyLong()))
            .thenReturn(createTracks(1L, 2L, 3L));
        String comment = pushingResultMessageParser.parseComment(
            getResult("", NotificationResultStatus.IGNORED)
        );
        assertEquals("Couldn't parse checkpoint info. ", comment);
    }

    @Test
    void parseResultWithIgnoredStatusAndEmptyMessageAndEmptyCheckpointList() {
        when(repo.getDeliveryTrackCheckpoints(anyLong()))
            .thenReturn(Collections.emptyList());
        String comment = pushingResultMessageParser.parseComment(
            getResult("", NotificationResultStatus.IGNORED)
        );
        assertEquals("Couldn't parse checkpoint info. ", comment);
    }

    @Test
    void successfulParseResultWithIgnoreStatus() {
        when(repo.getDeliveryTrackCheckpoints(anyLong()))
            .thenReturn(createTracks(1L, 2L, 3L));
        String comment = pushingResultMessageParser.parseComment(
            getResult(VALID_MESSAGE, NotificationResultStatus.IGNORED)
        );
        assertEquals("Push successful for track 1. Pushed ids: [1]", comment);
    }

    @Test
    void parseResultWithInvalidMessage() {
        when(repo.getDeliveryTrackCheckpoints(anyLong()))
            .thenReturn(createTracks(1L, 2L, 3L));
        String comment = pushingResultMessageParser.parseComment(
            getResult(INVALID_MESSAGE, NotificationResultStatus.IGNORED)
        );
        assertEquals("Couldn't parse checkpoint info. " + INVALID_MESSAGE, comment);
    }

    @Test
    void parseResultWithInvalidMessage2() {
        when(repo.getDeliveryTrackCheckpoints(anyLong()))
            .thenReturn(createTracks(1L, 2L, 3L));
        String comment = pushingResultMessageParser.parseComment(
            getResult(INVALID_MESSAGE2, NotificationResultStatus.IGNORED)
        );
        assertEquals("Couldn't parse checkpoint info. " + INVALID_MESSAGE2, comment);
    }

    private NotificationResult getResult(String message, NotificationResultStatus status) {
        NotificationResult notificationResult = new NotificationResult();
        notificationResult.setMessage(message);
        notificationResult.setTrackerId(1);
        notificationResult.setStatus(status);
        return notificationResult;
    }

    private List<DeliveryTrackCheckpoint> createTracks(long... ids) {
        return Arrays.stream(ids).mapToObj(this::createCheckpoint).collect(Collectors.toList());
    }

    private DeliveryTrackCheckpoint createCheckpoint(long id) {
        DeliveryTrackCheckpoint checkpoint = new DeliveryTrackCheckpoint();
        checkpoint.setId(id);
        return checkpoint;
    }
}
