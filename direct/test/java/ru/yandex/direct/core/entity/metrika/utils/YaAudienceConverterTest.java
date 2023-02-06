package ru.yandex.direct.core.entity.metrika.utils;

import org.junit.Test;

import ru.yandex.direct.audience.client.model.AudienceSegment;
import ru.yandex.direct.audience.client.model.SegmentStatus;
import ru.yandex.direct.core.entity.adgroup.model.ExternalAudienceStatus;
import ru.yandex.direct.core.entity.metrika.model.Segment;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class YaAudienceConverterTest {
    @Test
    public void convertSegment() {
        long id = 1234L;
        Segment segment = YaAudienceConverter.convertSegment(new AudienceSegment()
                .withId(id)
                .withStatus(SegmentStatus.PROCESSED));
        assertThat(segment, beanDiffer(new Segment().withId(id).withStatus(ExternalAudienceStatus.PROCESSED)));
    }

    @Test
    public void convertSegment_WithUnsupportedInDbType() {
        long id = 1234L;
        Segment segment = YaAudienceConverter.convertSegment(new AudienceSegment()
                .withId(id)
                .withStatus(SegmentStatus.UPLOADED));
        assertThat(segment, beanDiffer(new Segment().withId(id).withStatus(ExternalAudienceStatus.IS_PROCESSED)));
    }
}
