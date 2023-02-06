package ru.yandex.direct.dialogs.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.dialogs.client.model.Skill;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.tvm.TvmIntegrationImpl;

import static com.google.common.primitives.Longs.asList;
import static ru.yandex.direct.config.EssentialConfiguration.CONFIG_SCHEDULER_BEAN_NAME;
import static ru.yandex.direct.tvm.TvmService.DIALOGS_TEST;
import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_TEST;

@Ignore("Ходим в тестовую ручку диалогов")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
public class DialogsClientManualTest {
    // Skill_id диалога, созданного в тестинге (https://st.yandex-team.ru/DIRECT-97164#5cdea9b12045af0021c875bf)
    private static final String SKILL_ID_CORRECT = "4067c784-8068-4c4f-8097-6066eb2b3e89";
    private static final String SKILL_ID_NOT_EXISTS = "edae52db-e03c-482f-8dc2-58d5c1212ed2";
    private static final String SKILL_ID_NOT_VALID = "not-uuid";
    private static final String SKILL_ID_EMPTY = "";
    private static final String SKILL_ID_OVER_COUNT = "not-uuid-over-count";
    // В тестинге для этого id был специально создан диалог:
    // https://st.yandex-team.ru/DIRECT-97164#5cf79850458c89001f6c95bf
    private static final Long USER_ID_CORRECT = 780533952L;
    private static final Long USER_ID_WITHOUT_DIALOG = 780533952L;

    @Autowired
    public AsyncHttpClient asyncHttpClient;
    @Autowired()
    @Qualifier(CONFIG_SCHEDULER_BEAN_NAME)
    public TaskScheduler liveConfigChangeTaskScheduler;

    private DialogsClient dialogsClient;

    @Before
    public void setUp() throws Exception {
        DirectConfig directConfig = getDirectConfig();
        TvmIntegrationImpl tvmIntegration = TvmIntegrationImpl.create(directConfig, liveConfigChangeTaskScheduler);
        ParallelFetcherFactory fetcherFactory = new ParallelFetcherFactory(asyncHttpClient, new FetcherSettings());
        dialogsClient = new DialogsClient(
                "https://paskills.test.voicetech.yandex.net/api/external/v2",
                DIALOGS_TEST,
                fetcherFactory,
                tvmIntegration
        );
    }

    private DirectConfig getDirectConfig() {
        Map<String, Object> conf = new HashMap<>();
        conf.put("tvm.enabled", true);
        conf.put("tvm.app_id", DIRECT_SCRIPTS_TEST.getId());
        conf.put("tvm.api.url", "https://tvm-api.yandex.net");
        conf.put("tvm.api.error_delay", "5s");
        conf.put("tvm.secret", "file:////etc/direct-tokens/tvm2_direct-scripts-test");
        return DirectConfigFactory.getConfig(EnvironmentType.DB_TESTING, conf);
    }

    @Test
    public void getSkillsTest() {
        List<String> skillIds = Arrays.asList(
                SKILL_ID_CORRECT, SKILL_ID_NOT_EXISTS, SKILL_ID_NOT_VALID, SKILL_ID_EMPTY,
                SKILL_ID_OVER_COUNT, SKILL_ID_OVER_COUNT, SKILL_ID_OVER_COUNT, SKILL_ID_OVER_COUNT,
                SKILL_ID_OVER_COUNT, SKILL_ID_OVER_COUNT, SKILL_ID_OVER_COUNT, SKILL_ID_OVER_COUNT
        );
        List<Skill> result = dialogsClient.getSkills(skillIds);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result).hasSize(12);

            soft.assertThat(result.get(0).getSkillId()).isEqualTo("4067c784-8068-4c4f-8097-6066eb2b3e89");
            soft.assertThat(result.get(0).getName()).isEqualTo("Привет привет");
            soft.assertThat(result.get(0).getBotGuid()).isEqualTo("0280b4f3-f42a-40ea-bdcc-5e331a035ddb");
            soft.assertThat(result.get(0).getOnAir()).isEqualTo(true);
            soft.assertThat(result.get(0).getError()).isNull();

            soft.assertThat(result.get(1).getError().getSkillId()).isEqualTo("edae52db-e03c-482f-8dc2-58d5c1212ed2");
            soft.assertThat(result.get(1).getError().getCode()).isEqualTo(404);
            soft.assertThat(result.get(1).getError().getMessage()).isEqualTo("skill not found");

            soft.assertThat(result.get(2).getError().getSkillId()).isEqualTo("not-uuid");
            soft.assertThat(result.get(2).getError().getCode()).isEqualTo(400);
            soft.assertThat(result.get(2).getError().getMessage()).isEqualTo("skillId must be a valid UUID");

            soft.assertThat(result.get(3).getError().getSkillId()).isEqualTo("");
            soft.assertThat(result.get(3).getError().getCode()).isEqualTo(400);
            soft.assertThat(result.get(3).getError().getMessage()).isEqualTo("skillId must be a valid UUID");

            soft.assertThat(result.get(4).getError().getSkillId()).isEqualTo("not-uuid-over-count");
            soft.assertThat(result.get(4).getError().getCode()).isEqualTo(400);
            soft.assertThat(result.get(4).getError().getMessage()).isEqualTo("skillId must be a valid UUID");
        });
    }

    @Test
    public void getSkillsByUserIdTest() {
        Map<Long, List<Skill>> result = dialogsClient
                .getSkillsByUserId(asList(USER_ID_CORRECT, USER_ID_WITHOUT_DIALOG));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result).hasSize(1);
            List<Skill> skill = result.get(USER_ID_CORRECT);

            soft.assertThat(skill.get(0).getSkillId()).isEqualTo("d61d0b66-ffaa-408a-9557-f481eaf3751b");
            soft.assertThat(skill.get(0).getName()).isEqualTo("Тестовый чат с организацией");
            soft.assertThat(skill.get(0).getBotGuid()).isNull();
            soft.assertThat(skill.get(0).getOnAir()).isEqualTo(true);
            soft.assertThat(skill.get(0).getError()).isNull();
        });
    }
}
