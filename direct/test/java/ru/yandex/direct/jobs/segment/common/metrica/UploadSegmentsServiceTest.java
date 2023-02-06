package ru.yandex.direct.jobs.segment.common.metrica;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.audience.client.exception.YaAudienceClientException;
import ru.yandex.direct.audience.client.model.SegmentContentType;
import ru.yandex.direct.core.entity.metrika.model.Segment;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled // только для ручного тестирования. Заливаем данные в удаленный севис, или читаем реальные данные
@ExtendWith(SpringExtension.class)
@JobsTest
class UploadSegmentsServiceTest {

    private static final List<BigInteger> IDS = Arrays.asList(
            new BigInteger("917429861374909575"),
            new BigInteger("671638081489504958"),
            new BigInteger("8687265461301931125"),
            new BigInteger("4891949901494848194"),
            new BigInteger("5257567511493053123"),
            new BigInteger("7367780911464770678"),
            new BigInteger("6128380461489425096"),
            new BigInteger("2402976061411723979"),
            new BigInteger("8761101431456828108")
    );
    @Autowired
    private UploadSegmentsService uploadSegmentsService;

    private static final String OWNER_LOGIN = "yndx.asavanovich.super";

    @Test
    void uploadUids() {
        Segment response = uploadSegmentsService.uploadUids(IDS, OWNER_LOGIN);
        long segmentId = response.getId();
        Segment confirmResp = uploadSegmentsService.confirm(
                segmentId, OWNER_LOGIN, "BigDecimal", SegmentContentType.YUID);
        long confirmedSegmentId = confirmResp.getId();
        assertThat("id сегмента должны совпадать", confirmedSegmentId, is(segmentId));
    }

    @Test
    void modify() {
        long segmentId = 2351739;
        Segment modifyResponse = uploadSegmentsService.modifySegment(segmentId, OWNER_LOGIN, IDS);
    }

    @Test
    void upload_EmptyAndModify_NotWorkUnfortunately() {
        Segment segmentResponse = uploadSegmentsService.uploadUids(Collections.emptyList(), OWNER_LOGIN);

        assertThatThrownBy(() -> uploadSegmentsService.modifySegment(segmentResponse.getId(), OWNER_LOGIN, IDS))
                .isInstanceOf(YaAudienceClientException.class);
    }

    @Test
    void getSegmentsSelf() {
        List<Segment> segments = uploadSegmentsService.getSegments("yndx-robot-aud-video-goal");
        assertNotNull(segments);
    }

    @Test
    void getSegmentsStranger() {
        List<Segment> segments = uploadSegmentsService.getSegments("asavan23");
        assertNotNull(segments);
    }

    @Test
    void confirmTest() {
        Segment response = uploadSegmentsService.confirm(
                2351703L, OWNER_LOGIN, "newSegment2", SegmentContentType.YUID);

    }

    @Test
    void confirmTest2() {
        Segment response = uploadSegmentsService.confirm(
                2351739L, OWNER_LOGIN, "newSegment2", SegmentContentType.YUID);
    }
}
