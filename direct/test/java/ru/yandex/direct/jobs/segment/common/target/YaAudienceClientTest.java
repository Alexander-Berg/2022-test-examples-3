package ru.yandex.direct.jobs.segment.common.target;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.audience.client.YaAudienceClient;
import ru.yandex.direct.audience.client.exception.SegmentNotModifiedException;
import ru.yandex.direct.core.entity.adgroup.model.ExternalAudienceStatus;
import ru.yandex.direct.core.entity.metrika.model.Segment;
import ru.yandex.direct.core.entity.metrika.utils.YaAudienceConverter;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.metrika.utils.YaAudienceConverter.convertSegment;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@Disabled // только для ручного тестирования. Заливаем данные в удаленный севис, или читаем реальные данные
@ExtendWith(SpringExtension.class)
@JobsTest
class YaAudienceClientTest {
    private static final String FAKE_DATA_FILE_PATH = "videogoals/fake_data.tsv";
    private static final String OWNER_LOGIN = "yndx.asavanovich.super";

    @Autowired
    private YaAudienceClient yaAudienceClient;

    @Test
    void getSegments() {
        var audienceSegments = yaAudienceClient.getSegments(OWNER_LOGIN);
        List<Segment> segments = mapList(audienceSegments, YaAudienceConverter::convertSegment);
        List<Long> ids = mapList(segments, Segment::getId);
        System.out.println(ids);
        List<ExternalAudienceStatus> status = mapList(segments, Segment::getStatus);
        System.out.println(status);
    }

    @Test
    void upload() throws IOException {
        Segment segment = uploadSegment();
        assertThat("сегменту должен быть присвоен id", segment.getId(), notNullValue());
        assertThat("статус в метрике ожидается uploaded, но у нас этот статус не поддержан - поэтому null",
                segment.getStatus(), nullValue());
    }

    @Test
    void confirm() throws IOException {
        Segment segment = uploadSegment();
        assumeThat("upload должен пройти успешно и вернуть id", segment.getId(), notNullValue());
        var audienceSegment = yaAudienceClient.confirmYuidSegment(OWNER_LOGIN, segment.getId(),
                "Test segment (do not delete)");
        Segment confirmedSegment = convertSegment(audienceSegment);
        Segment expected = new Segment()
                .withId(segment.getId())
                .withStatus(ExternalAudienceStatus.IS_PROCESSED);
        assertThat("сегмент соответствует ожидаемому", confirmedSegment, beanDiffer(expected));
    }

    @Test
    void modify_SegmentNotChanged() {
        assertThatThrownBy(() -> yaAudienceClient.modifySegment(OWNER_LOGIN, 2357039L, getSegmentContent()))
                .isInstanceOf(SegmentNotModifiedException.class);
    }

    private Segment uploadSegment() throws IOException {
        byte[] content = getSegmentContent();
        var audienceSegment = yaAudienceClient.uploadSegment(OWNER_LOGIN, content);
        return convertSegment(audienceSegment);
    }

    private byte[] getSegmentContent() throws IOException {
        return getClass().getClassLoader().getResourceAsStream(FAKE_DATA_FILE_PATH)
                .readAllBytes();
    }
}
