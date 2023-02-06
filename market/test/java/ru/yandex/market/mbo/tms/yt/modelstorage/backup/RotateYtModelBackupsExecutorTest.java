package ru.yandex.market.mbo.tms.yt.modelstorage.backup;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.mbo.utils.DateFormatUtils;
import ru.yandex.market.mbo.yt.TestYt;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 * @since 12/28/2018
 */
public class RotateYtModelBackupsExecutorTest {

    private static final String YT_ROOT_PATH = "//home/market/test/mbo";
    private static final String YT_BACKUP_PATH = YT_ROOT_PATH + "/backup";

    private RotateYtModelBackupsExecutor rotateYtModelBackupsExecutor;

    private Yt yt;

    @Before
    public void setUp() {
        yt = new TestYt();
        rotateYtModelBackupsExecutor = new RotateYtModelBackupsExecutor(yt, YT_ROOT_PATH);
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testRotation() throws Exception {
        YPath backupPath = YPath.simple(YT_BACKUP_PATH);
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime last2WeeksStart = now.minusWeeks(2);
        List<String> last2WeeksBackups = Arrays.asList(
            createBackupName(now),
            createBackupName(now.minusHours(3)),
            createBackupName(now.minusHours(6)),
            createBackupName(last2WeeksStart.plusMinutes(1)),
            createBackupName(last2WeeksStart.plusHours(1)),
            createBackupName(last2WeeksStart.plusDays(2))
        );

        List<String> allBackups = new ArrayList<>(last2WeeksBackups);
        List<String> backupsToPreserve = new ArrayList<>(last2WeeksBackups);

        LocalDateTime last2MonthsStart = now.minusMonths(2)
            .withHour(0)
            .withMinute(0);
        allBackups.addAll(Arrays.asList(
            createBackupName(last2MonthsStart.plusMinutes(1)),
            createBackupName(last2MonthsStart.plusHours(1)),
            createBackupName(last2MonthsStart.plusHours(10)),
            createBackupName(last2MonthsStart.plusMinutes(1).plusDays(1)),
            createBackupName(last2MonthsStart.plusMinutes(1).plusDays(2)),
            createBackupName(last2MonthsStart.plusMinutes(1).plusDays(3)),
            createBackupName(last2MonthsStart.plusMinutes(1).plusDays(4))
        ));
        backupsToPreserve.addAll(Arrays.asList(
            createBackupName(last2MonthsStart.plusMinutes(1)),
            createBackupName(last2MonthsStart.plusMinutes(1).plusDays(3))
        ));

        LocalDateTime lastYearStart = now
            .minusYears(1)
            .with(DayOfWeek.MONDAY)
            .withHour(0)
            .withMinute(0);
        allBackups.addAll(Arrays.asList(
            createBackupName(lastYearStart.plusMinutes(1)),
            createBackupName(lastYearStart.plusHours(1)),
            createBackupName(lastYearStart.plusDays(1)),
            createBackupName(lastYearStart.plusMinutes(1).plusWeeks(1))
        ));
        backupsToPreserve.addAll(Arrays.asList(
            createBackupName(lastYearStart.plusMinutes(1)),
            createBackupName(lastYearStart.plusMinutes(1).plusWeeks(1))
        ));

        LocalDateTime last5YearsStart = now
            .minusYears(5)
            .withMonth(1)
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0);
        allBackups.addAll(Arrays.asList(
            createBackupName(last5YearsStart.plusMinutes(1)),
            createBackupName(last5YearsStart.plusDays(1)),
            createBackupName(last5YearsStart.plusWeeks(1)),
            createBackupName(last5YearsStart.plusMonths(2)),
            createBackupName(last5YearsStart.plusMinutes(1).plusMonths(3))
        ));
        backupsToPreserve.addAll(Arrays.asList(
            createBackupName(last5YearsStart.plusMinutes(1)),
            createBackupName(last5YearsStart.plusMinutes(1).plusMonths(3))
        ));

        allBackups.addAll(Arrays.asList(
            createBackupName(now.minusYears(6)),
            createBackupName(now.minusYears(10))
        ));
        allBackups.forEach(backup ->
            yt.cypress().create(backupPath.child(backup), CypressNodeType.TABLE, true));

        rotateYtModelBackupsExecutor.doRealJob(null);

        List<String> actuallyPreserved = yt.cypress().list(backupPath).stream()
            .map(YTreeStringNode::getValue)
            .collect(Collectors.toList());
        assertThat(actuallyPreserved).containsExactlyInAnyOrder(backupsToPreserve.toArray(new String[0]));
    }

    private String createBackupName(LocalDateTime dateTime) {
        return "mbo-models_" + DateFormatUtils.DEFAULT_SESSION_FORMAT.format(dateTime);
    }
}
