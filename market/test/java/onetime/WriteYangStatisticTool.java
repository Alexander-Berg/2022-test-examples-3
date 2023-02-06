package onetime;

import com.google.common.io.Resources;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.markup2.entries.yang.YangAssignmentInfo;
import ru.yandex.market.markup2.entries.yang.YangPoolInfo;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.TaskIdMapper;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.toloka.model.ResultItem;
import ru.yandex.market.toloka.model.Task;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author galaev
 * @since 2019-09-18
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:tool-stable.xml"})
public class WriteYangStatisticTool extends ToolBase {

    private Map<Integer, Integer> categoryIds = new HashMap<>();
    private Map<Integer, User> userIds = new HashMap<>();
    private Map<Integer, User> inspectionUserIds = new HashMap<>();

    private Map<Integer, Integer> inspectionToMainTaskIds;

    @Test
    @Ignore("Don't need to run tools with unit tests")
    public void writeYangStatistic() {
        List<Integer> taskIds = readTaskIds();
        Map<Integer, YangAssignmentInfo> completedAssignments = filterCompletedAssignments(taskIds);
        log.info("Extracted {} completed assignments", completedAssignments.size());

        readCategoryIdsAndUids(completedAssignments.keySet());

        completedAssignments.forEach(this::doSaveStatistics);

        writeOfferIdsByOperator(completedAssignments.keySet());
    }

    @Test
    @Ignore("Don't need to run tools with unit tests")
    public void writeYangStatisticInspection() {
        inspectionToMainTaskIds = readInspectionTaskIds();
        Map<Integer, YangAssignmentInfo> completedAssignments = filterCompletedAssignments(
            new ArrayList<>(inspectionToMainTaskIds.keySet()));
        log.info("Extracted {} completed inspection assignments", completedAssignments.size());

        readCategoryIdsAndUids(completedAssignments.keySet());

        completedAssignments.forEach(this::doSaveStatistics);

        writeOfferIdsByOperator(completedAssignments.keySet());
    }

    private Map<Integer, YangAssignmentInfo> filterCompletedAssignments(List<Integer> taskIds) {
        Map<Integer, YangAssignmentInfo> completedAssignments = new HashMap<>();
        for (Integer taskId : taskIds) {
            List<YangPoolInfo> pools = yangPoolPersister.loadYangPoolInfosByTaskId(taskId);
            if (pools.size() != 1) {
                log.info("Wrong number of pools for task {}", taskId);
                continue;
            }
            YangPoolInfo pool = pools.get(0);
            YangAssignmentInfo completedAssignment = getCompletedAssignment(pool.getPoolId());
            if (completedAssignment != null) {
                Integer mappingTaskId = inspectionToMainTaskIds != null ? inspectionToMainTaskIds.get(taskId) : taskId;
                completedAssignments.put(mappingTaskId, completedAssignment);
            }
        }
        return completedAssignments;
    }

    private void readCategoryIdsAndUids(Set<Integer> taskIds) {
        try {
            Resources.readLines(Resources.getResource("onetime/yang_statistics/tasks.info"), Charset.defaultCharset())
                .stream()
                .forEach(line -> {
                    String[] fields = line.split("\t");
                    Integer taskId = Integer.parseInt(fields[0]);
                    if (!taskIds.contains(taskId)) {
                        return;
                    }
                    Integer categoryId = Integer.parseInt(fields[1]);
                    if (categoryId != 0) {
                        categoryIds.put(taskId, categoryId);
                    }

                    Integer userId = Integer.parseInt(fields[2]);
                    Integer lastModifiedTs = Integer.parseInt(fields[3]);
                    User user = userIds.get(taskId);
                    if (user != null) {
                        if (user.lastModifiedTs > lastModifiedTs) {
                            inspectionUserIds.put(taskId, user);
                            userIds.put(taskId, new User(userId, lastModifiedTs));
                        } else {
                            inspectionUserIds.put(taskId, new User(userId, lastModifiedTs));
                        }
                    } else {
                        userIds.put(taskId, new User(userId, lastModifiedTs));
                    }
                });
            log.info("Read {} category info", categoryIds.size());
            log.info("Read {} user info", userIds.size());
            log.info("Read {} inspection user info", inspectionUserIds.size());
        } catch (IOException e) {
            log.info("Failed to read category ids and uids", e);
        }
    }

    private List<Integer> readTaskIds() {
        try {
            List<Integer> tasks = Resources.readLines(Resources.getResource("onetime/yang_statistics/main.tasks"),
                Charset.defaultCharset())
                .stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());
            log.info("Read {} tasks from file", tasks.size());
            return tasks;
        } catch (IOException e) {
            log.info("Failed to read task ids", e);
            return Collections.emptyList();
        }
    }

    private Map<Integer, Integer> readInspectionTaskIds() {
        try {
            Map<Integer, Integer> myInspectionToMainTaskIds = new HashMap<>();
            Resources.readLines(
                Resources.getResource("onetime/yang_statistics/main.and.inspection.tasks"),
                Charset.defaultCharset())
                .stream()
                .forEach(line -> {
                    String[] fields = line.split(",");
                    myInspectionToMainTaskIds.put(Integer.parseInt(fields[1]), Integer.parseInt(fields[0]));
                });
            log.info("Read {} inspection tasks from file", myInspectionToMainTaskIds.size());
            return myInspectionToMainTaskIds;
        } catch (IOException e) {
            log.info("Failed to read task ids", e);
            return Collections.emptyMap();
        }
    }

    private YangAssignmentInfo getCompletedAssignment(int poolId) {
        List<ResultItem> completedAssignments = tolokaApi.getResult(poolId);
        return completedAssignments.size() > 0 ? convertAssignmentInfo(completedAssignments.get(0)) : null;
    }

    private YangAssignmentInfo convertAssignmentInfo(ResultItem assignmentResult) {
        YangAssignmentInfo assignmentInfo = new YangAssignmentInfo()
            .setAssignmentId(assignmentResult.getId())
            .setTaskSuiteId(assignmentResult.getTaskSuiteId())
            .setPoolId(assignmentResult.getPoolId())
            .setWorkerId(assignmentResult.getUserId());
        assignmentResult.getTasks().stream()
            .map(Task::getId)
            .findFirst()
            .ifPresent(assignmentInfo::setTaskId);
        return assignmentInfo;
    }

    private YangLogStorage.YangLogStoreResponse doSaveStatistics(int mappingTaskId, YangAssignmentInfo assignmentInfo) {

        YangLogStorage.YangLogStoreRequest.Builder requestBuilder = YangLogStorage.YangLogStoreRequest.newBuilder();
        Integer categoryId = categoryIds.get(mappingTaskId);
        if (categoryId == null || categoryId == 0) {
            log.info("No category id for task {}", mappingTaskId);
            return null;
        }
        requestBuilder.setCategoryId(categoryId);
        requestBuilder.setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS);
        requestBuilder.setId(TaskIdMapper.mapTaskId(mappingTaskId));
        requestBuilder.setHitmanId(mappingTaskId);

        User operator = userIds.get(mappingTaskId);
        YangLogStorage.OperatorInfo contractorInfo = getOperatorInfo(operator.uid, assignmentInfo);
        requestBuilder.setContractorInfo(contractorInfo);

        User inspector = inspectionUserIds.get(mappingTaskId);
        if (inspector != null) {
            YangLogStorage.OperatorInfo inspectorInfo = getOperatorInfo(inspector.uid, assignmentInfo);
            requestBuilder.setInspectorInfo(inspectorInfo);
        }

        log.info("Request: " + requestBuilder.build().toString());
        return yangLogStorageService.yangLogStore(requestBuilder.build());
    }

    private YangLogStorage.OperatorInfo getOperatorInfo(long uid, YangAssignmentInfo assignmentInfo) {
        String poolId = String.valueOf(assignmentInfo.getPoolId());
        return YangLogStorage.OperatorInfo.newBuilder()
            .setUid(uid)
            .setPoolId(poolId)
            .setTaskId(assignmentInfo.getTaskId())
            .setAssignmentId(assignmentInfo.getAssignmentId())
            .build();
    }

    private void writeOfferIdsByOperator(Collection<Integer> taskIds) {
        Map<Long, AtomicInteger> offersByOperator = new HashMap<>();
        for (Integer taskId : taskIds) {
            int count = countTaskDataItems(taskId);
            User operator = userIds.get(taskId);
            if (operator != null) {
                AtomicInteger counter = offersByOperator.computeIfAbsent(operator.uid, (k) -> new AtomicInteger());
                counter.addAndGet(count);
            }
            User inspector = inspectionUserIds.get(taskId);
            if (inspector != null) {
                AtomicInteger counter = offersByOperator.computeIfAbsent(inspector.uid, (k) -> new AtomicInteger());
                counter.addAndGet(count);
            }
        }
        log.info("Offers count by operator uid: " + offersByOperator);
    }

    static class User {

        long uid;
        long lastModifiedTs;

        User(long uid, long lastModifiedTs) {
            this.uid = uid;
            this.lastModifiedTs = lastModifiedTs;
        }
    }
}
