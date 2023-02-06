package ru.yandex.autotests.direct.cmd.steps.feeds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.model.User;

import static java.lang.String.format;
import static ru.yandex.autotests.direct.cmd.util.FileUtils.getFilePath;

public class FeedHelper {
    private static final String CHANGED_FEED_FILE_NAME = "changed_feed_%d.xml";
    private static final String WIN_1251 = "Windows-1251";

    public static Path generateUniqueFeed(Path templateFile) {
        return generateUniqueFeed(templateFile, WIN_1251);
    }

    public static String generateUniqueFeedStringPath(String templateFile) {
        return generateUniqueFeed(Paths.get(getFilePath(templateFile))).toString();
    }

    /**
     * Create xmp file with unique content. template should be a valid YML file with at least one %d specifier
     *
     * @param templateFile xml template path
     * @param encoding template and result file encoding
     * @return result path
     */
    private static Path generateUniqueFeed(Path templateFile, String encoding) {
        long time = System.nanoTime();
        Path resultPath;
        try {
            String fileContent = new String(Files.readAllBytes(templateFile), encoding);
            Preconditions.checkArgument(fileContent.contains("%d"),
                    "xml файл должен содержать как минимум один спецификатор %d");
            fileContent = format(fileContent, time);
            resultPath = Files.write(Paths.get(format(CHANGED_FEED_FILE_NAME, time)), fileContent.getBytes(encoding));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return resultPath;
    }

    public static void deleteFeed(DirectCmdRule cmdRule, String client, Long feedId) {
        TestEnvironment.newDbSteps().useShardForLogin(client).feedsSteps()
                .updateFeedsStatus(FeedsUpdateStatus.Done, User.get(client).getClientID(), feedId);
        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.SUPER));
        cmdRule.cmdSteps().ajaxDeleteFeedsSteps().deleteFeed(client, feedId);
    }

    public static void deleteAllFeeds(DirectCmdRule cmdRule, String client) {
        List<Long> existingIds = cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(client).getFeedsResult()
                .getFeeds().stream().map(t -> Long.valueOf(t.getFeedId())).collect(Collectors.toList());
        TestEnvironment.newDbSteps().useShardForLogin(client).feedsSteps()
                .updateFeedsStatus(FeedsUpdateStatus.Done, User.get(client).getClientID(), existingIds);
        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.SUPER));
        cmdRule.cmdSteps().ajaxDeleteFeedsSteps().deleteFeeds(client, existingIds);
    }
}
