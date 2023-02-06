package ru.yandex.autotests.direct.cmd.rules;

import org.junit.rules.ExternalResource;

import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.model.User;

import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class VideoAdditionCreativeRule extends ExternalResource {
    private String ulogin;
    private Long videoAdditionCreativeId;
    private DirectJooqDbSteps dbSteps;

    public VideoAdditionCreativeRule(String ulogin) {
        this.ulogin = ulogin;
        dbSteps = TestEnvironment.newDbSteps(ulogin);
    }

    @Override
    protected void before() {
        videoAdditionCreativeId = dbSteps.perfCreativesSteps()
                .saveDefaultVideoCreative(Long.parseLong(User.get(ulogin).getClientID()), 1L);
    }

    @Override
    protected void after() {
        dbSteps.bannersPerformanceSteps().deleteBannersPerformanceRecord(videoAdditionCreativeId);
        dbSteps.perfCreativesSteps().deletePerfCreatives(videoAdditionCreativeId);
    }

    public Long getCreativeId() {
        assumeThat("креатив видеодополнения был создан", videoAdditionCreativeId, notNullValue());
        return videoAdditionCreativeId;
    }
}
