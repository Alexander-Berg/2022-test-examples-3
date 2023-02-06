package ru.yandex.direct.jobs.redirects;

import java.util.List;

import one.util.streamex.IntStreamEx;
import one.util.streamex.LongStreamEx;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.redirectcheckqueue.model.CheckRedirectTask;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.jobs.redirects.RedirectCheckService.MAX_CHUNKS_TO_PROCESS;
import static ru.yandex.direct.jobs.redirects.RedirectCheckService.MAX_CHUNK_SIZE;
import static ru.yandex.direct.jobs.redirects.RedirectCheckService.MIN_CHUNK_SIZE;
import static ru.yandex.direct.jobs.redirects.RedirectCheckService.partitionTasksForAsyncRun;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


@JobsTest
@ExtendWith(SpringExtension.class)
class RedirectCheckServicePartitionTasksTest {

    private static final Long USER_ID1 = 12345L;
    private static final Long USER_ID2 = 12346L;

    @Test
    void partitionTasks_whenEmptyList() {
        List<List<CheckRedirectTask>> actualChunks = partitionTasksForAsyncRun(emptyList());
        assertThat(actualChunks).hasSize(0);
    }

    @Test
    void partitionTasks_whenOneTask() {
        CheckRedirectTask task = generateCheckRedirectTask(USER_ID1);
        List<List<CheckRedirectTask>> actualChunks = partitionTasksForAsyncRun(singletonList(task));
        assertThat(actualChunks).isEqualTo(singletonList(singletonList(task)));
    }

    @Test
    void partitionTasks_whenOneUser() {
        List<CheckRedirectTask> tasks = generateCheckRedirectTasks(USER_ID1, MIN_CHUNK_SIZE + 1);
        List<List<CheckRedirectTask>> actualChunks = partitionTasksForAsyncRun(tasks);
        assumeThat(actualChunks, hasSize(1));
        assertThat(actualChunks.get(0)).hasSize(MIN_CHUNK_SIZE + 1);
    }

    @Test
    void partitionTasks_whenTwoUsers() {
        List<CheckRedirectTask> firstUsersTasks = generateCheckRedirectTasks(USER_ID1, MIN_CHUNK_SIZE);
        List<CheckRedirectTask> secondUsersTasks = generateCheckRedirectTasks(USER_ID2, MIN_CHUNK_SIZE);
        List<CheckRedirectTask> tasks = StreamEx.of(firstUsersTasks, secondUsersTasks).toFlatList(identity());

        List<List<CheckRedirectTask>> actualChunks = partitionTasksForAsyncRun(tasks);
        assumeThat(actualChunks, hasSize(2));
        assertThat(actualChunks.get(0)).hasSize(MIN_CHUNK_SIZE);
        assertThat(actualChunks.get(1)).hasSize(MIN_CHUNK_SIZE);
    }

    @Test
    void partitionTasks_whenTwoUsers_andTasksCountLessThanChunkSize() {
        List<CheckRedirectTask> firstUsersTasks = generateCheckRedirectTasks(USER_ID1, MIN_CHUNK_SIZE - 1);
        List<CheckRedirectTask> secondUsersTasks = generateCheckRedirectTasks(USER_ID2, MIN_CHUNK_SIZE - 1);
        List<CheckRedirectTask> tasks = StreamEx.of(firstUsersTasks, secondUsersTasks).toFlatList(identity());

        List<List<CheckRedirectTask>> actualChunks = partitionTasksForAsyncRun(tasks);
        assumeThat(actualChunks, hasSize(1));
        assertThat(actualChunks.get(0)).hasSize(2 * MIN_CHUNK_SIZE - 2);
    }

    @Test
    void partitionTasks_whenSeveralUsers() {
        // генерируем MIN_CHUNK_SIZE + 1 задач, у каждой задачи свой userId
        List<CheckRedirectTask> tasks = LongStreamEx.range(MIN_CHUNK_SIZE + 1)
                .map(i -> i + 1000L) // userId
                .mapToObj(RedirectCheckServicePartitionTasksTest::generateCheckRedirectTask)
                .toList();

        List<List<CheckRedirectTask>> actualChunks = partitionTasksForAsyncRun(tasks);
        assumeThat(actualChunks, hasSize(2));
        assertThat(actualChunks.get(0)).hasSize(MIN_CHUNK_SIZE);
        assertThat(actualChunks.get(1)).hasSize(1);
    }

    @Test
    void partitionTasks_whenChunksCountMoreThanMax() {
        List<Long> userIds = LongStreamEx.range(MAX_CHUNKS_TO_PROCESS + 1)
                .mapToObj(i -> i + 1000L)
                .toList();
        List<CheckRedirectTask> tasks = StreamEx.of(userIds)
                .flatMap(userId -> generateCheckRedirectTasks(userId, MIN_CHUNK_SIZE).stream())
                .toList();

        List<List<CheckRedirectTask>> actualChunks = partitionTasksForAsyncRun(tasks);
        assertThat(actualChunks).hasSize(MAX_CHUNKS_TO_PROCESS);
    }

    @Test
    void partitionTasks_whenChunkSizeMoreThanMax() {
        List<CheckRedirectTask> tasks = generateCheckRedirectTasks(USER_ID1, MAX_CHUNK_SIZE + 1);
        List<List<CheckRedirectTask>> actualChunks = partitionTasksForAsyncRun(tasks);
        assumeThat(actualChunks, hasSize(1));
        assertThat(actualChunks.get(0)).hasSize(MAX_CHUNK_SIZE);
    }

    private static List<CheckRedirectTask> generateCheckRedirectTasks(Long userId, int tasksCount) {
        return IntStreamEx.range(tasksCount)
                .mapToObj(i -> generateCheckRedirectTask(userId))
                .toList();
    }

    private static CheckRedirectTask generateCheckRedirectTask(Long userId) {
        return new CheckRedirectTask()
                .withTaskId(RandomNumberUtils.nextPositiveLong())
                .withBannerId(RandomNumberUtils.nextPositiveLong())
                .withUserId(userId)
                .withHref("https://www.yandex.ru/test/href/" + RandomStringUtils.randomNumeric(5));
    }
}
