package ru.yandex.market.tsum.pipelines.mbo.jobs.manual_testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.SupportType;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResourceList;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;

/**
 * Джоба будет падать, если в ченджлоге встречается определенная команда.
 * Команда задается через {@link OnManualDemandFailConfig#getFailCommand()}.
 *
 * @author s-ermakov
 */
public class OnManualDemandFailChangelogJob implements JobExecutor {
    private static final Logger log = LoggerFactory.getLogger(OnManualDemandFailChangelogJob.class);

    @WiredResource
    private OnManualDemandFailConfig config;

    @WiredResourceList(ChangelogInfo.class)
    private List<ChangelogInfo> changelogInfos = new ArrayList<>();

    @Override
    public void execute(JobContext context) throws Exception {
        String failCommand = config.getFailCommand();
        String failMessage = config.getFailMessage();
        if (StringUtils.isEmpty(failCommand)) {
            log.info("Fail command is empty. Skip check.");
            return;
        }

        String logsWithCommands = changelogInfos.stream()
            .flatMap(c -> c.getFilteredChangelogEntriesOrFullChangelogIfEmpty().stream())
            .map(ChangelogEntry::getChange)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty() && s.toLowerCase().contains(failCommand))
            .distinct()
            .collect(Collectors.joining("\n"));

        if (!logsWithCommands.isEmpty()) {
            context.actions().failJob(
                failMessage == null ? "" : failMessage +
                    String.format("\nFail command: '%s'\nCommits with command:\n\"%s\"", failCommand, logsWithCommands),
                SupportType.NONE
            );
        }
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("e6487a1f-42d6-4fb6-8362-d9dd0cb1bb51");
    }
}
