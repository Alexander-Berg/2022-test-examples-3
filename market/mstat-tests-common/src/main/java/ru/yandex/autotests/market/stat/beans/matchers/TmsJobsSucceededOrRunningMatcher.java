package ru.yandex.autotests.market.stat.beans.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;
import ru.yandex.autotests.market.stat.beans.meta.TmsStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jkt on 15.05.14.
 */
public class TmsJobsSucceededOrRunningMatcher extends TypeSafeMatcher<Iterable<TmsRunState>> {

    private List<TmsRunState> failedRuns = new ArrayList<>();

    @Factory
    public static TmsJobsSucceededOrRunningMatcher allSucceededOrRunning() {
        return new TmsJobsSucceededOrRunningMatcher();
    }

    public TmsJobsSucceededOrRunningMatcher() {
    }

    @Override
    protected boolean matchesSafely(Iterable<TmsRunState> tmsRunStates) {
        for (TmsRunState runState : tmsRunStates) {
            String jobStatus = runState.getJobStatus();
            if (!TmsStatus.RUNNING.matches(jobStatus) && !TmsStatus.OK.matches(jobStatus)) {
                failedRuns.add(runState);
            }
        }
        return failedRuns.isEmpty();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("All job running or succeeded. See attachments.");
    }

    @Override
    protected void describeMismatchSafely(Iterable<TmsRunState> item, Description mismatchDescription) {
        mismatchDescription.appendText("Were job failures:\n");
        mismatchDescription.appendText(Attacher.attach("Failed", failedRuns));
    }
}
