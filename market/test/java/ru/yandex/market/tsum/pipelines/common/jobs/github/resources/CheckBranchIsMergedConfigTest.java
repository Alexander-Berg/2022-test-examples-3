package ru.yandex.market.tsum.pipelines.common.jobs.github.resources;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ru.yandex.market.tsum.pipelines.common.resources.BranchRef;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.CheckBranchIsMergedJob.Mode.TARGET_INTO_SOURCE;

@RunWith(JUnit4.class)
public class CheckBranchIsMergedConfigTest {

    private static final BranchRef BRANCH = new BranchRef("branch");

    @Test
    public void emptyConfigIsEmpty() throws Exception {
        CheckBranchIsMergedConfig config = CheckBranchIsMergedConfig.builder().build();
        assertNull(config.getTargetBranch());
        assertNull(config.getMode());
    }

    @Test
    public void targetBranchSpecifiedInBuilderIsAvailbleInConfig() throws Exception {
        CheckBranchIsMergedConfig config = CheckBranchIsMergedConfig.builder().withTargetBranch(BRANCH).build();
        assertSame(BRANCH, config.getTargetBranch());
        assertNull(config.getMode());
    }

    @Test
    public void modeSpecifiedInBuilderIsAvailbleInConfig() throws Exception {
        CheckBranchIsMergedConfig config = CheckBranchIsMergedConfig.builder().withMode(TARGET_INTO_SOURCE).build();
        assertNull(config.getTargetBranch());
        assertSame(TARGET_INTO_SOURCE, config.getMode());
    }

    @Test
    public void withTargetBranchFactoryCreatesNewBranchRef() throws Exception {
        CheckBranchIsMergedConfig config = CheckBranchIsMergedConfig.withTargetBranch("zzz");
        assertSame("zzz", config.getTargetBranch().getName());
        assertNull(config.getMode());
    }

    @Test
    public void emptyConstructorForDeserializer() throws Exception {
        new CheckBranchIsMergedConfig();
    }
}
