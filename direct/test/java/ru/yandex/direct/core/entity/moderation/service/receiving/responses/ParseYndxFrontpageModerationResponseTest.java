package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerFlags.AGE;
import static ru.yandex.direct.utils.JsonUtils.fromJson;

@ParametersAreNonnullByDefault
public class ParseYndxFrontpageModerationResponseTest {
    private static final String MODERATION_RESPONSE_EXAMPLE_FILENAME =
            "yndx_frontpage_moderation_response_example.json";
    private static final String MODERATION_RESPONSE_EXAMPLE_REJECTED_FILENAME =
            "yndx_frontpage_moderation_response_example.rejected.json";

    @Test
    public void testModerationResponseJsonParser() throws IOException {
        String json = IOUtils.toString(this.getClass().getResourceAsStream(
                MODERATION_RESPONSE_EXAMPLE_FILENAME), UTF_8);

        BannerModerationResponse response = fromJson(json, BannerModerationResponse.class);

        Verdict verdict = response.getResult();
        assertThat(verdict.getVerdict(), is(ModerationDecision.Yes));
        assertThat(verdict.getReasons(), empty());
        assertThat(verdict.getMinusRegions(), hasItems(1L));
        assertThat(verdict.getFlags().get(AGE.getKey()), is("age18"));
    }

    @Test
    public void testModerationResponseRejectedJsonParser() throws IOException {
        String json = IOUtils.toString(this.getClass().getResourceAsStream(
                MODERATION_RESPONSE_EXAMPLE_REJECTED_FILENAME), UTF_8);

        BannerModerationResponse response = fromJson(json, BannerModerationResponse.class);

        Verdict verdict = response.getResult();
        assertThat(verdict.getVerdict(), is(ModerationDecision.No));
        assertThat(verdict.getReasons(), is(singletonList(1017L)));
    }
}
