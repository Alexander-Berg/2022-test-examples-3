package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.CONTENT_PROMOTION_VIDEO;
import static ru.yandex.direct.utils.JsonUtils.fromJson;

public class ParseContentPromotionVideoModerationResponseTest {

    private static final String MODERATION_RESPONSE_EXAMPLE_FILENAME =
            "content_promotion_video_moderation_response_example.json";
    private static final String MODERATION_RESPONSE_EXAMPLE_REJECTED_FILENAME =
            "content_promotion_video_moderation_response_example.rejected.json";

    @Test
    public void testModerationResponseJsonParser() throws IOException {
        String json = IOUtils.toString(this.getClass().getResourceAsStream(
                MODERATION_RESPONSE_EXAMPLE_FILENAME), UTF_8);

        BannerModerationResponse actual = fromJson(json, BannerModerationResponse.class);
        BannerModerationResponse expected = new BannerModerationResponse();

        Verdict verdict = new Verdict();
        verdict.setVerdict(ModerationDecision.Yes);
        verdict.setCategories(Arrays.asList(200000614L, 200000612L));
        verdict.setFlags(ImmutableMap.of("age", "age18", "finance", "1"));
        verdict.setReasons(Collections.emptyList());
        verdict.setLang("ru");

        BannerModerationMeta meta = new BannerModerationMeta();
        meta.setVersionId(6);
        meta.setUid(1);
        meta.setClientId(2);
        meta.setBannerId(5);
        meta.setCampaignId(3);
        meta.setAdGroupId(4);

        expected.setResult(verdict);
        expected.setService(ModerationServiceNames.DIRECT_SERVICE);
        expected.setType(CONTENT_PROMOTION_VIDEO);
        expected.setUnixtime(1549291298);
        expected.setMeta(meta);

        assertThat(actual, beanDiffer(expected));
    }

    @Test
    public void testModerationResponseRejectedJsonParser() throws IOException {
        String json = IOUtils.toString(this.getClass().getResourceAsStream(
                MODERATION_RESPONSE_EXAMPLE_REJECTED_FILENAME), UTF_8);

        BannerModerationResponse actual = fromJson(json, BannerModerationResponse.class);
        BannerModerationResponse expected = new BannerModerationResponse();

        Verdict verdict = new Verdict();
        verdict.setVerdict(ModerationDecision.No);
        verdict.setCategories(Arrays.asList(200000614L, 200000612L));
        verdict.setFlags(ImmutableMap.of("age", "age18", "finance", "1"));
        verdict.setReasons(Arrays.asList(1L, 2L, 3L));
        verdict.setLang("ru");

        BannerModerationMeta meta = new BannerModerationMeta();
        meta.setVersionId(1);
        meta.setUid(6);
        meta.setClientId(5);
        meta.setBannerId(2);
        meta.setCampaignId(4);
        meta.setAdGroupId(3);

        expected.setResult(verdict);
        expected.setService(ModerationServiceNames.DIRECT_SERVICE);
        expected.setType(null);
        expected.setUnixtime(1549291298);
        expected.setMeta(meta);

        assertThat(actual, beanDiffer(expected));
    }
}
