package ru.yandex.direct.dialogs.client;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.dialogs.client.model.Skill;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.inside.passport.tvm2.TvmHeaders;

public class DialogsClientGetSkillsTest extends DialogsClientTestBase {
    private final String skillId = "edae52db-e03c-482f-8dc2-58d5c1212ed1";

    @Test
    public void getSkillsTest() {
        List<Skill> skills = dialogsClient.getSkills(Collections.singletonList(skillId));
        Skill expected = createTestSkill(skillId);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(skills).hasSize(1);
            Skill skill = skills.get(0);
            soft.assertThat(skill.getSkillId()).isEqualTo(expected.getSkillId());
            soft.assertThat(skill.getBotGuid()).isEqualTo(expected.getBotGuid());
            soft.assertThat(skill.getName()).isEqualTo(expected.getName());
            soft.assertThat(skill.getOnAir()).isEqualTo(expected.getOnAir());
            soft.assertThat(skill.getError()).isNull();
        });
    }

    @Override
    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(request.getHeader(TvmHeaders.SERVICE_TICKET)).isEqualTo(TICKET_BODY);
                    soft.assertThat(request.getHeader("Content-type")).isEqualTo("application/json");
                    soft.assertThat(request.getPath()).isEqualTo("/skills/bulk/get");
                    soft.assertThat(request.getBody().readString(Charset.defaultCharset())).contains(skillId);
                });
                return new MockResponse()
                        .setBody(JsonUtils.toJson(Collections.singletonList(createTestSkill(skillId))));
            }
        };
    }
}
