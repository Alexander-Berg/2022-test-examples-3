package ru.yandex.direct.internaltools.tools.oneshot;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.oneshot.launch.OneshotLaunchesTool;
import ru.yandex.direct.internaltools.tools.oneshot.launch.model.OneshotLaunchAction;
import ru.yandex.direct.internaltools.tools.oneshot.launch.model.OneshotLaunchInput;
import ru.yandex.direct.oneshot.core.entity.oneshot.OneshotAppVersionProvider;
import ru.yandex.direct.oneshot.core.entity.oneshot.repository.OneshotLaunchDataRepository;
import ru.yandex.direct.oneshot.core.entity.oneshot.repository.OneshotLaunchRepository;
import ru.yandex.direct.oneshot.core.entity.oneshot.repository.OneshotRepository;
import ru.yandex.direct.oneshot.core.entity.oneshot.repository.testing.TestOneshots;
import ru.yandex.direct.oneshot.core.model.LaunchStatus;
import ru.yandex.direct.oneshot.core.model.Oneshot;
import ru.yandex.direct.oneshot.core.model.OneshotLaunch;
import ru.yandex.direct.oneshot.core.model.OneshotLaunchData;
import ru.yandex.direct.oneshot.core.model.OneshotLaunchValidationStatus;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.utils.CommonUtils.nvl;

@InternalToolsTest
@RunWith(SpringRunner.class)
public class OneshotLaunchesToolTest {
    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.onlyExpectedFields();

    @Autowired
    private OneshotLaunchesTool oneshotLaunchesTool;
    @Autowired
    private OneshotRepository oneshotRepository;
    @Autowired
    private OneshotLaunchRepository oneshotLaunchRepository;
    @Autowired
    private OneshotLaunchDataRepository oneshotLaunchDataRepository;
    @Autowired
    private OneshotAppVersionProvider oneshotAppVersionProvider;
    @Autowired
    private ShardHelper shardHelper;

    private Oneshot oneshot;
    private OneshotLaunch oneshotLaunch;

    private OneshotLaunchInput defaultInput;

    @Before
    public void before() {
        oneshot = TestOneshots.defaultOneshot();
        oneshot.setId(oneshotRepository.add(oneshot));

        defaultInput = new OneshotLaunchInput();
        defaultInput.setOperator(new User().withDomainLogin(oneshot.getApprovers().iterator().next()));
    }

    @Test
    public void testApprove() {
        oneshotLaunch = TestOneshots.defaultLaunch(oneshot.getId())
                .withValidationStatus(OneshotLaunchValidationStatus.VALID)
                .withApprover(null)
                .withApprovedRevision(null);
        initLaunch(oneshotLaunch);
        defaultInput.setLaunchAction(OneshotLaunchAction.APPROVE);

        oneshotLaunchesTool.process(defaultInput);

        OneshotLaunch actualLaunch = oneshotLaunchRepository.get(oneshotLaunch.getId());
        OneshotLaunch expected = new OneshotLaunch()
                .withApprover(defaultInput.getOperator().getDomainLogin())
                .withApprovedRevision(nvl(oneshotAppVersionProvider.getCurrentRevision(), "").toString());
        assertThat(actualLaunch, beanDiffer(expected).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void testLaunch() {
        oneshotLaunch = TestOneshots.defaultLaunch(oneshot.getId())
                .withValidationStatus(OneshotLaunchValidationStatus.VALID)
                .withApprover(oneshot.getApprovers().iterator().next())
                .withApprovedRevision("123");
        initLaunch(oneshotLaunch);
        defaultInput.setLaunchAction(OneshotLaunchAction.LAUNCH);
        defaultInput.setOperator(new User().withDomainLogin(oneshotLaunch.getLaunchCreator()));

        oneshotLaunchesTool.process(defaultInput);

        OneshotLaunch launch = oneshotLaunchRepository.get(oneshotLaunch.getId());
        assertThat(launch,
                beanDiffer(new OneshotLaunch()).useCompareStrategy(DefaultCompareStrategies.onlyFields(
                        newPath(OneshotLaunch.LAUNCH_REQUEST_TIME.name()))
                        .forFields(newPath(OneshotLaunch.LAUNCH_REQUEST_TIME.name())).useMatcher(approximatelyNow())));

        Map<Long, List<OneshotLaunchData>> launchDataList =
                oneshotLaunchDataRepository.getLaunchDataByLaunchId(Set.of(oneshotLaunch.getId()));
        assertThat(launchDataList.get(launch.getId()), hasSize(shardHelper.dbShards().size()));
    }

    @Test
    public void testLaunch_safeOneshot() {
        oneshot = TestOneshots.defaultOneshot()
                .withSafeOneshot(true);
        oneshot.setId(oneshotRepository.add(oneshot));

        // launch without approve;
        oneshotLaunch = TestOneshots.defaultLaunch(oneshot.getId())
                .withValidationStatus(OneshotLaunchValidationStatus.VALID)
                .withApprover(null)
                .withApprovedRevision(null);
        initLaunch(oneshotLaunch);
        defaultInput.setLaunchAction(OneshotLaunchAction.LAUNCH);
        defaultInput.setOperator(new User().withDomainLogin(oneshotLaunch.getLaunchCreator()));

        oneshotLaunchesTool.process(defaultInput);

        OneshotLaunch launch = oneshotLaunchRepository.get(oneshotLaunch.getId());
        assertThat(launch,
                beanDiffer(new OneshotLaunch()).useCompareStrategy(DefaultCompareStrategies.onlyFields(
                        newPath(OneshotLaunch.LAUNCH_REQUEST_TIME.name()))
                        .forFields(newPath(OneshotLaunch.LAUNCH_REQUEST_TIME.name())).useMatcher(approximatelyNow())));

        Map<Long, List<OneshotLaunchData>> launchDataList =
                oneshotLaunchDataRepository.getLaunchDataByLaunchId(Set.of(oneshotLaunch.getId()));
        assertThat(launchDataList.get(launch.getId()), hasSize(shardHelper.dbShards().size()));
    }

    @Test
    public void testDelete() {
        oneshotLaunch = TestOneshots.defaultLaunch(oneshot.getId())
                .withValidationStatus(OneshotLaunchValidationStatus.VALID)
                .withApprover(oneshot.getApprovers().iterator().next())
                .withApprovedRevision("123");
        initLaunch(oneshotLaunch);
        defaultInput.setLaunchAction(OneshotLaunchAction.DELETE);

        oneshotLaunchesTool.process(defaultInput);

        OneshotLaunch launch = oneshotLaunchRepository.get(oneshotLaunch.getId());
        assertThat(launch, nullValue());
    }

    @Test
    public void testPause() {
        oneshotLaunch = TestOneshots.defaultLaunch(oneshot.getId())
                .withValidationStatus(OneshotLaunchValidationStatus.VALID)
                .withApprover(oneshot.getApprovers().iterator().next())
                .withLaunchRequestTime(LocalDateTime.now());
        initLaunch(oneshotLaunch);
        defaultInput.setLaunchAction(OneshotLaunchAction.PAUSE);

        Long launchId = oneshotLaunch.getId();
        OneshotLaunchData oneshotLaunchData = TestOneshots.defaultLaunchData(launchId);
        oneshotLaunchDataRepository.add(oneshotLaunchData);

        oneshotLaunchesTool.process(defaultInput);

        Map<Long, List<OneshotLaunchData>> launchDataByLaunchId =
                oneshotLaunchDataRepository.getLaunchDataByLaunchId(Collections.singleton(launchId));
        assertThat("должны быть данные по одному запуску", launchDataByLaunchId.keySet(), hasSize(1));
        assertThat("должны быть данные об одном потоке", launchDataByLaunchId.get(launchId), hasSize(1));
        LaunchStatus actualStatus = launchDataByLaunchId.get(launchId).get(0).getLaunchStatus();
        assertThat(actualStatus, is(LaunchStatus.PAUSE_REQUESTED));
    }

    @Test
    public void testCancel() {
        oneshotLaunch = TestOneshots.defaultLaunch(oneshot.getId())
                .withValidationStatus(OneshotLaunchValidationStatus.VALID)
                .withApprover(oneshot.getApprovers().iterator().next())
                .withLaunchRequestTime(LocalDateTime.now());
        initLaunch(oneshotLaunch);
        defaultInput.setLaunchAction(OneshotLaunchAction.CANCEL);

        Long launchId = oneshotLaunch.getId();
        OneshotLaunchData oneshotLaunchData = TestOneshots.defaultLaunchData(launchId);
        oneshotLaunchDataRepository.add(oneshotLaunchData);

        oneshotLaunchesTool.process(defaultInput);

        Map<Long, List<OneshotLaunchData>> launchDataByLaunchId =
                oneshotLaunchDataRepository.getLaunchDataByLaunchId(Collections.singleton(launchId));
        assertThat("должны быть данные по одному запуску", launchDataByLaunchId.keySet(), hasSize(1));
        assertThat("должны быть данные об одном потоке", launchDataByLaunchId.get(launchId), hasSize(1));
        LaunchStatus actualStatus = launchDataByLaunchId.get(launchId).get(0).getLaunchStatus();
        assertThat(actualStatus, is(LaunchStatus.CANCELED));
    }

    private void initLaunch(OneshotLaunch oneshotLaunch) {
        oneshotLaunch.setId(oneshotLaunchRepository.add(oneshotLaunch));
        defaultInput.setLaunchName(oneshotLaunch.getId() + ":" + oneshot.getClassName());
    }

}
