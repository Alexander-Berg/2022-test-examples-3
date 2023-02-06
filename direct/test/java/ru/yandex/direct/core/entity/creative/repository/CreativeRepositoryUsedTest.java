package ru.yandex.direct.core.entity.creative.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.SqlUtils;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.multitype.entity.LimitOffset.limited;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CreativeRepositoryUsedTest {

    private Long nextCreativeId;

    @Autowired
    private Steps steps;

    @Autowired
    private CreativeRepository repoUnderTest;

    private ClientInfo clientInfo;
    private CreativeInfo canvas1;
    private CreativeInfo canvas2;

    private int shard;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        Long creativeId1 = steps.creativeSteps().getNextCreativeId();
        canvas1 = steps.creativeSteps().addDefaultCanvasCreative(clientInfo, creativeId1);
        Long creativeId2 = steps.creativeSteps().getNextCreativeId();
        canvas2 = steps.creativeSteps().addDefaultCanvasCreative(clientInfo, creativeId2);

        addCreativeToNewCamp(canvas1);
        addCreativeToNewCamp(canvas2);

        nextCreativeId = steps.creativeSteps().getNextCreativeId();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getClientUsedCreativeIds() {
        List<Long> usedCreativeIds = repoUnderTest
                .getClientUsedCreativeIds(shard, clientInfo.getClientId(), singleton(CreativeType.CANVAS), 0, null,
                        null);
        assertThat("полученные ид креативов соответствует ожидаемым", usedCreativeIds,
                contains(equalTo(canvas1.getCreativeId()), equalTo(canvas2.getCreativeId())));
    }

    @Test
    public void getClientUsedCreativeIds_videoAddition() {
        CreativeInfo videoAddition = steps.creativeSteps().addDefaultVideoAdditionCreative(clientInfo, nextCreativeId);
        addCreativeToNewCamp(videoAddition);
        List<Long> usedCreativeIds = repoUnderTest.getClientUsedCreativeIds(shard, clientInfo.getClientId(),
                singleton(CreativeType.VIDEO_ADDITION_CREATIVE), 0, null, null);
        assertThat("полученные ид креативов соответствует ожидаемым", usedCreativeIds,
                contains(equalTo(videoAddition.getCreativeId())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getClientUsedCreativeIds_sortAscWithLastId() {
        addCreativeToNewCamp(canvas1);

        List<Long> usedCreativeIds = repoUnderTest
                .getClientUsedCreativeIds(shard, clientInfo.getClientId(), singleton(CreativeType.CANVAS), 0, null,
                        null);
        assertThat("полученные ид креативов соответствует ожидаемым", usedCreativeIds,
                contains(equalTo(canvas1.getCreativeId()), equalTo(canvas2.getCreativeId())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getClientUsedCreativeIds_sortDesc() {
        List<Long> usedCreativeIds = repoUnderTest
                .getClientUsedCreativeIds(shard, clientInfo.getClientId(), singleton(CreativeType.CANVAS), 0,
                        SqlUtils.SortOrder.DESC, null);
        assertThat("полученные ид креативов соответствует ожидаемым", usedCreativeIds,
                contains(equalTo(canvas2.getCreativeId()), equalTo(canvas1.getCreativeId())));
    }

    @Test
    public void getClientUsedCreativeIds_sortAscWithLimit() {

        List<Long> usedCreativeIds =
                repoUnderTest.getClientUsedCreativeIds(shard, clientInfo.getClientId(), singleton(CreativeType.CANVAS),
                        (long) 0, SqlUtils.SortOrder.ASC, limited(1));
        assertThat("полученные ид креативов соответствует ожидаемым", usedCreativeIds,
                contains(equalTo(canvas1.getCreativeId())));
    }

    @Test
    public void getClientUsedCreativeIds_sortDescWithLimit() {

        List<Long> usedCreativeIds =
                repoUnderTest.getClientUsedCreativeIds(shard, clientInfo.getClientId(), singleton(CreativeType.CANVAS),
                        (long) 0, SqlUtils.SortOrder.DESC, limited(1));
        assertThat("полученные ид креативов соответствует ожидаемым", usedCreativeIds,
                contains(equalTo(canvas2.getCreativeId())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getClientUsedCreativeIds_sortAscWithLastId2() {
        CreativeInfo canvas3 = steps.creativeSteps().addDefaultCanvasCreative(clientInfo, nextCreativeId);
        addCreativeToNewCamp(canvas3);
        Long lastCreativeId = canvas1.getCreativeId();
        List<Long> usedCreativeIds = repoUnderTest
                .getClientUsedCreativeIds(shard, clientInfo.getClientId(), singleton(CreativeType.CANVAS),
                        lastCreativeId,
                        SqlUtils.SortOrder.ASC, null);
        assertThat("полученные ид креативов соответствует ожидаемым", usedCreativeIds,
                contains(equalTo(canvas2.getCreativeId()), equalTo(canvas3.getCreativeId())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getClientUsedCreativeIds_sortDescWithLastId() {
        CreativeInfo canvas3 = steps.creativeSteps().addDefaultCanvasCreative(clientInfo, nextCreativeId);
        addCreativeToNewCamp(canvas3);
        Long lastCreativeId = canvas3.getCreativeId();
        List<Long> usedCreativeIds = repoUnderTest
                .getClientUsedCreativeIds(shard, clientInfo.getClientId(), singleton(CreativeType.CANVAS),
                        lastCreativeId,
                        SqlUtils.SortOrder.DESC, null);
        assertThat("полученные ид креативов соответствует ожидаемым", usedCreativeIds,
                contains(equalTo(canvas2.getCreativeId()), equalTo(canvas1.getCreativeId())));
    }

    private void addCreativeToNewCamp(CreativeInfo creativeInfo) {
        steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                .withBanner(fullTextBanner()
                        .withCreativeId(creativeInfo.getCreativeId())
                        .withCreativeStatusModerate(BannerCreativeStatusModerate.NEW))
                .withClientInfo(clientInfo));
    }
}
