package ru.yandex.market.takeout.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.Assert;

import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.takeout.common.TimeoutContext;
import ru.yandex.market.takeout.service.models.State;
import ru.yandex.market.takeout.service.models.TakeoutData;
import ru.yandex.market.takeout.service.models.TakeoutError;
import ru.yandex.market.takeout.service.models.TakeoutStatus;

public class GetStatusServiceImplTest extends RequestProcessorTestCases {
    private final static DeletedTasksProvider NO_DELETED_TASKS =
            new DeletedTasksProvider() {
                @Override
                public CompletableFuture<Set<String>> getDeleteTypes(long uid, RequestContext requestContext) {
                    return CompletableFuture.completedFuture(Collections.emptySet());
                }

                @Override
                public CompletableFuture<?> enqueueTasks(long uid, long muid, Set<String> types,
                                                         RequestContext requestContext) {
                    return CompletableFuture.completedFuture(true);
                }

                @Override
                public List<DeleteTask> getOldestDeleteTasks() {
                    return Collections.emptyList();
                }

                @Override
                public void setTaskProcessed(DeleteTask deleteTask, boolean succeed, RequestContext context) {

                }

                @Override
                public List<DeleteTask> getTasksForDeleteHard() {
                    return Collections.emptyList();
                }

                @Override
                public void setTaskHardDeletedStatus(DeleteTask deleteTask, boolean succeed, RequestContext context) {}
            };

    private DeletedTasksProvider getDeletedTaskProvider(String type) {
        return new DeletedTasksProvider() {
            @Override
            public CompletableFuture<Set<String>> getDeleteTypes(long uid, RequestContext requestContext) {
                return CompletableFuture.completedFuture(Collections.singleton(type));
            }

            @Override
            public CompletableFuture<?> enqueueTasks(long uid, long muid, Set<String> types, RequestContext requestContext) {
                return CompletableFuture.completedFuture(true);
            }

            @Override
            public List<DeleteTask> getOldestDeleteTasks() {
                return Collections.emptyList();
            }

            @Override
            public void setTaskProcessed(DeleteTask deleteTask, boolean succeed, RequestContext context) {

            }

            @Override
            public List<DeleteTask> getTasksForDeleteHard() {
                return Collections.emptyList();
            }

            @Override
            public void setTaskHardDeletedStatus(DeleteTask deleteTask, boolean succeed, RequestContext context) {}
        };
    }

    public void testGetEmptyStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getEmptyProcessor("type"));
        assertStatus(type, State.Empty, "type");
    }

    public void testGetSuccessfulStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSuccessfulProcessor("type"));
        assertStatus(type, State.ReadyToDelete, "type");
    }

    public void testGetExceptionalStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getExceptionalProcessor("type"));
        assertExceptionalStatus(type, "type");
    }

    public void testSlowGetEmptyStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowEmptyProcessor("type", 500));
        assertStatus(type, State.Empty, "type");
    }

    public void testSlowGetSuccessfulStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowSuccessfulProcessor("type", 500));
        assertStatus(type, State.ReadyToDelete, "type");
    }

    public void testSlowGetExceptionalStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowExceptionalProcessor("type", 500));
        assertExceptionalStatus(type, "type");
    }

    public void testTwoSlowGetExceptionalStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowExceptionalProcessor("type", 200),
                getSlowExceptionalProcessor("type", 500));
        assertExceptionalStatus(type, "type");
    }

    public void testSlowEmptyAndExceptionalStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowEmptyProcessor("type", 200),
                getSlowExceptionalProcessor("type", 500));
        assertExceptionalStatus(type, "type");
    }

    public void testSlowSuccessfulAndExceptionalStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowSuccessfulProcessor("type", 200),
                getSlowExceptionalProcessor("type", 500));
        assertStatus(type, State.ReadyToDelete, "type");
    }

    public void testSlowExceptionalAndEmptyStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowEmptyProcessor("type", 500),
                getSlowExceptionalProcessor("type", 200));
        assertExceptionalStatus(type, "type");
    }

    public void testSlowExceptionalAndSuccessfulStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowSuccessfulProcessor("type", 500),
                getSlowExceptionalProcessor("type", 200));
        assertStatus(type, State.ReadyToDelete, "type");
    }

    public void testTwoSlowEmptyStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowEmptyProcessor("type", 200),
                getSlowEmptyProcessor("type", 500));
        assertStatus(type, State.Empty, "type");
    }

    public void testSlowSuccessfulAndEmptyStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowSuccessfulProcessor("type", 200),
                getSlowEmptyProcessor("type", 500));
        assertStatus(type, State.ReadyToDelete, "type");
    }

    public void testSlowEmptyAndSuccessfulStatus() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowEmptyProcessor("type", 200),
                getSlowSuccessfulProcessor("type", 500));
        assertStatus(type, State.ReadyToDelete, "type");
    }

    public void testSlowEmptyAndExceptionalStatusTypes() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowEmptyProcessor("type1", 200),
                getSlowExceptionalProcessor("type2", 500));
        assertStatus(type, State.Empty, "type1");
        assertExceptionalStatus(type, "type2");
    }

    public void testSlowExceptionalAndEmptyStatusTypes() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowExceptionalProcessor("type1", 200),
                getSlowEmptyProcessor("type2", 500));
        assertExceptionalStatus(type, "type1");
        assertStatus(type, State.Empty, "type2");
    }

    public void testSlowSuccessfulAndExceptionalStatusTypes() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowSuccessfulProcessor("type1", 200),
                getSlowExceptionalProcessor("type2", 500));
        assertStatus(type, State.ReadyToDelete, "type1");
        assertExceptionalStatus(type, "type2");
    }

    public void testSlowExceptionalAndSuccessfulStatusTypes() throws Exception {
        TakeoutStatus type = getStatus(NO_DELETED_TASKS, getSlowExceptionalProcessor("type1", 200),
                getSlowSuccessfulProcessor("type2", 500));
        assertExceptionalStatus(type, "type1");
        assertStatus(type, State.ReadyToDelete, "type2");
    }

    public void testGetEmptyStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getEmptyProcessor("type"));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testGetSuccessfulStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getSuccessfulProcessor("type"));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testGetExceptionalStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getExceptionalProcessor("type"));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testSlowGetEmptyStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getSlowEmptyProcessor("type", 500));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testSlowGetSuccessfulStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getSlowSuccessfulProcessor("type", 500));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testSlowGetExceptionalStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getSlowExceptionalProcessor("type", 500));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testTwoSlowGetExceptionalStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getSlowExceptionalProcessor("type", 200),
                getSlowExceptionalProcessor("type", 500));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testSlowEmptyAndExceptionalStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getSlowEmptyProcessor("type", 200),
                getSlowExceptionalProcessor("type", 500));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testSlowSuccessfulAndExceptionalStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getSlowSuccessfulProcessor("type", 200),
                getSlowExceptionalProcessor("type", 500));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testSlowExceptionalAndEmptyStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getSlowEmptyProcessor("type", 500),
                getSlowExceptionalProcessor("type", 200));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testSlowExceptionalAndSuccessfulStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getSlowSuccessfulProcessor("type", 500),
                getSlowExceptionalProcessor("type", 200));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testTwoSlowEmptyStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getSlowEmptyProcessor("type", 200),
                getSlowEmptyProcessor("type", 500));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testSlowSuccessfulAndEmptyStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getSlowSuccessfulProcessor("type", 200),
                getSlowEmptyProcessor("type", 500));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testSlowEmptyAndSuccessfulStatusWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type"), getSlowEmptyProcessor("type", 200),
                getSlowSuccessfulProcessor("type", 500));
        assertStatus(type, State.DeleteInProgress, "type");
    }

    public void testSlowEmptyAndExceptionalStatusTypesWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type1"), getSlowEmptyProcessor("type1", 200),
                getSlowExceptionalProcessor("type2", 500));
        assertStatus(type, State.DeleteInProgress, "type1");
        assertExceptionalStatus(type, "type2");
    }

    public void testSlowExceptionalAndEmptyStatusTypesWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type2"), getSlowExceptionalProcessor("type1", 200),
                getSlowEmptyProcessor("type2", 500));
        assertExceptionalStatus(type, "type1");
        assertStatus(type, State.DeleteInProgress, "type2");
    }

    public void testSlowSuccessfulAndExceptionalStatusTypesWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type1"), getSlowSuccessfulProcessor("type1", 200),
                getSlowExceptionalProcessor("type2", 500));
        assertStatus(type, State.DeleteInProgress, "type1");
        assertExceptionalStatus(type, "type2");
    }

    public void testSlowExceptionalAndSuccessfulStatusTypesWithDeletedTask() throws Exception {
        TakeoutStatus type = getStatus(getDeletedTaskProvider("type2"), getSlowExceptionalProcessor("type1", 200),
                getSlowSuccessfulProcessor("type2", 500));
        assertExceptionalStatus(type, "type1");
        assertStatus(type, State.DeleteInProgress, "type2");
    }

    private void assertStatus(TakeoutStatus status, State empty, String type) {
        Collection<TakeoutData> data = status.getData();
        Assert.assertEquals(1, data.size());
        TakeoutData next = data.iterator().next();
        Assert.assertEquals(type, next.getId());
        Assert.assertEquals(type, next.getSlug());
        Assert.assertEquals(empty, next.getState());
    }

    private void assertExceptionalStatus(TakeoutStatus status, String type) {
        Collection<TakeoutError> takeoutErrors = status.getErrors();
        Assert.assertFalse(takeoutErrors.isEmpty());
        Assert.assertEquals(1, takeoutErrors.size());
        TakeoutError next = takeoutErrors.iterator().next();
        Assert.assertEquals(type, next.getCode());
    }

    private TakeoutStatus getStatus(DeletedTasksProvider deletedTasksProvider, RequestProcessor... requestProcessors) throws Exception {
        GetStatusServiceImpl getStatusService = new GetStatusServiceImpl(deletedTasksProvider, requestProcessors);
        return getStatusService.getStatus(0L, new RequestContext(""), Collections.emptyMap()
                , new TimeoutContext() {
                    @Override
                    public <T> CompletableFuture<T> withTimeout(CompletableFuture<T> task) {
                        return task;
                    }
                }).get();
    }
}
