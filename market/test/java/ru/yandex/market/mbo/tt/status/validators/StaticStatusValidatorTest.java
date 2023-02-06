package ru.yandex.market.mbo.tt.status.validators;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.tt.model.TaskList;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.status.Status;
import ru.yandex.market.mbo.tt.status.validators.statuschange.StatusTransition;

/**
 * @author Ilya Ryzhenkov, i-ryzhenkov@yandex-team.ru
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class StaticStatusValidatorTest {
    private HashMap<TaskType, List<StatusTransition>> allowedTransitions;
    private final Set<TaskType> deprecatedTypes = ImmutableSet.<TaskType>builder()
        .add(TaskType.MODEL_SOURCE_CHECK)
        .add(TaskType.BOOKS_FIX_ERROR)
        .add(TaskType.BOOKS_AUTHOR_CARD_CHECK)
        .add(TaskType.BOOKS_WORK_CARD_CHECK)
        .add(TaskType.BOOKS_EDITION_CARD_CHECK)
        .add(TaskType.BOOKS_AUTHOR_CARD_FILL)
        .add(TaskType.BOOKS_WORK_CARD_FILL)
        .add(TaskType.BOOKS_AUTHOR_DOUBLES)
        .add(TaskType.BOOKS_WORK_DOUBLES)
        .add(TaskType.BOOKS_CHECK_FILL)
        .add(TaskType.BOOKS_CHECK_ERROR_FIX)
        .add(TaskType.BOOKS_CHECK_ENTITY_CARD_CHECK).build();
    private static final Logger log = Logger.getLogger(StaticStatusValidatorTest.class);

    @Before
    public void setUp() {
        allowedTransitions = new HashMap<>();
        InputStream inputStream = StaticStatusValidatorTest.class.getResourceAsStream(
            "/mbo-core/ru/yandex/market/mbo/allowed_status_change.csv");
        String csv = "";
        try {
            csv = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            CSVParser parser = CSVParser.parse(csv, CSVFormat.DEFAULT);
            for (CSVRecord record : parser) {
                TaskType type = TaskType.getTaskType(Integer.parseInt(record.get(0)));
                Status oldStatus = Status.getStatus(Integer.parseInt(record.get(1)));
                Status newStatus = Status.getStatus(Integer.parseInt(record.get(2)));
                allowedTransitions.computeIfAbsent(type, k -> new ArrayList<>()).add(
                    new StatusTransition(oldStatus, newStatus));
            }
        } catch (Exception e) {
            log.warn(e);
        }
    }
    /**
     * Test was created to validate migration of StaticStatusValidator from Oracle DB to Java (MBO-37111).
     * Transition rules from oracle view site_catalog.v_tt_allowed_status_change
     * stored in allowed_status_change.csv file.
     */
    @Test
    public void testAllCases() {
        StaticStatusValidator validator = new StaticStatusValidator(null);
        for (TaskType taskListType : TaskType.values()) {
            if (deprecatedTypes.contains(taskListType)) {
                try {
                    TaskList taskList = new TaskList(0, 0, 0, 0, Status.INITIAL,
                        taskListType, 0, null);
                    validator.checkRules(taskList, Status.INITIAL, Status.TASK_OPENED, 0);
                    Assert.fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException e) {
                    Assert.assertNotEquals("", e.getMessage());
                }
                continue;
            }
            for (Status oldStatus : Status.values()) {
                for (Status newStatus : Status.values()) {
                    TaskList taskList = new TaskList(0, 0, 0, 0, Status.INITIAL,
                        taskListType, 0, null);
                    boolean validatorStatus = validator.checkRules(taskList, oldStatus, newStatus, 0);
                    boolean allowedTransitionStatus = allowedTransitions.get(taskListType).contains(
                        new StatusTransition(oldStatus, newStatus));
                    String errorMessage = String.format(
                        "TaskType: %s, old: %s, new: %s Expected:%b actual:%b",
                        taskList.getType(),
                        oldStatus,
                        newStatus,
                        allowedTransitionStatus,
                        validatorStatus
                    );
                    Assert.assertEquals(errorMessage, allowedTransitionStatus, validatorStatus);
                }
            }
        }
    }
}
