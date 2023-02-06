package ru.yandex.direct.oneshot.core.entity.oneshot.repository;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.oneshot.core.configuration.OneshotCoreTest;
import ru.yandex.direct.oneshot.core.entity.oneshot.repository.testing.TestOneshots;
import ru.yandex.direct.oneshot.core.model.OneshotLaunch;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.dbschema.ppcdict.tables.Oneshots.ONESHOTS;

@OneshotCoreTest
@RunWith(SpringRunner.class)
public class OneshotLaunchRepositoryTest {

    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private OneshotRepository oneshotRepository;
    @Autowired
    private OneshotLaunchRepository oneshotLaunchRepository;

    @Before
    public void before() {
        dslContextProvider.ppcdict().deleteFrom(ONESHOTS).execute();
    }

    @Test
    public void getLaunchesByOneshotIds() {
        Long oneshotId = oneshotRepository.add(TestOneshots.defaultOneshot());

        OneshotLaunch oneshotLaunch = TestOneshots.defaultLaunch(oneshotId);
        Long launchId = oneshotLaunchRepository.add(oneshotLaunch);
        oneshotLaunch.withId(launchId);

        List<OneshotLaunch> launches = oneshotLaunchRepository.getByOneshotIds(Set.of(oneshotId));
        assertThat(launches, contains(beanDiffer(oneshotLaunch)));
    }
}
