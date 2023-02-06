import contextlib
import datetime
import logging
import multiprocessing
import threading
import time

import kazoo.exceptions
import pytest
from six.moves import range
import six.moves.queue

import crypta.lib.python.bt.workflow.base as base
import crypta.lib.python.bt.workflow.exception as exception
import crypta.lib.python.bt.workflow.scha as scha
from crypta.lib.python.bt.workflow import (
    SinkTask,
    IndependentTask,
    Parameter,
)
from crypta.lib.python.zk import fake_zk_client

logger = logging.getLogger(__name__)


MANAGER = multiprocessing.Manager()

GLOBAL_STATE = {}


def registry():
    return GLOBAL_STATE['REGISTRY']


@pytest.fixture(scope="function")
def state(request):
    GLOBAL_STATE['REGISTRY'] = MANAGER.dict()


@pytest.yield_fixture(scope="function")
def zk(request, state):
    with fake_zk_client() as zk:
        yield zk


@pytest.yield_fixture(scope="function")
def slow_zk(request, state):
    import types

    with fake_zk_client() as zk:
        original_exists = zk.exists

        def slow_exists(self, path):
            time.sleep(0.01)
            return original_exists(path)

        zk.exists = types.MethodType(slow_exists, zk)
        yield zk


def query(identifier):
    result = identifier in GLOBAL_STATE['REGISTRY']
    logger.info('Checking if %s is complete: %s', identifier, result)
    return result


def complete(identifier):
    logger.info('Marking %s as complete', identifier)
    now = time.time()
    if identifier in registry():
        registry()[identifier] += [now]
    else:
        registry()[identifier] = [now]


class QueryTarget(base.Target):
    def __init__(self, name):
        self.name = name

    def satisfied(self):
        return query(self.name)


class QueryTask(IndependentTask):
    task_id = base.Parameter()

    def targets(self):
        yield QueryTarget(self.task_id)

    def run(self, **kwargs):
        complete(self.task_id)


class MultipleRuntimeDependenciesTask(IndependentTask):

    def targets(self):
        yield QueryTarget('runtime-one')
        yield QueryTarget('runtime-two')
        yield QueryTarget('runtime-three')

    def run(self, **kwargs):
        yield QueryTask(task_id='runtime-one')
        yield QueryTask(task_id='runtime-two')
        yield QueryTask(task_id='runtime-three')


class CountdownTask(base.Task):
    number = base.Parameter(parse=int)

    def requires(self):
        if self.number > 0:
            yield CountdownTask(number=self.number-1)

    def targets(self):
        yield QueryTarget(self.identifier())

    def identifier(self):
        return 'countdown-{}'.format(self.number)

    def run(self, **kwargs):
        complete(self.identifier())


class ImpossibleTask(IndependentTask):
    def complete(self):
        return False

    def run(self, **kwargs):
        raise Exception()


class InvalidTask(IndependentTask):
    def complte(self):
        return False

    @property
    def valid(self):
        return False

    def run(self, **kwargs):
        pass


class UnprocessableTask(IndependentTask):
    def complete(self):
        raise Exception("I AM UNPROCESSABLE!")

    def run(self, **kwargs):
        pass


class CompleteTask(IndependentTask):
    """Complete task"""

    task_id = base.Parameter()

    def complete(self):
        return True

    def run(self, **kwargs):
        complete(self.task_id)


class UncompletableTask(IndependentTask):
    def complete(self):
        return False

    def run(self, **kwargs):
        pass


class LongTask(IndependentTask):
    timeout = datetime.timedelta(seconds=1)

    def complete(self):
        return False

    def run(self, **kwargs):
        time.sleep(10)


class SpawningTask(IndependentTask):
    def targets(self):
        yield QueryTarget("spawning")

    def run(self, **kwargs):
        yield CountdownTask(number=2)
        complete("spawning")


class DynamicUroborosTask(IndependentTask):

    def targets(self):
        yield QueryTarget("uroboros")

    def run(self, **kwargs):
        yield DynamicUroborosTask()


class UroborosHeadTask(base.Task):
    def requires(self):
        yield UroborosTailTask()

    def targets(self):
        yield QueryTarget("uroboros-head")

    def run(self, **kwargs):
        complete("uroboros-head")


class UroborosTailTask(base.Task):
    def requires(self):
        yield UroborosHeadTask()

    def targets(self):
        yield QueryTarget("uroboros-tail")

    def run(self, **kwargs):
        complete("uroboros-tail")


class UncompletableTaskWithTransaction(IndependentTask):
    TX_START = "TX_START"
    TX_FINISH = "TX_FINISH"
    TX_ABORT = "TX_ABORT"

    def targets(self):
        yield QueryTarget("something-that-wont-be-there")

    @contextlib.contextmanager
    def run_context(self):
        try:
            complete(self.TX_START)
            yield
            complete(self.TX_FINISH)
        except:
            complete(self.TX_ABORT)

    def run(self, **kwargs):
        pass


class UngreasedTask(IndependentTask):
    TRY_1 = 'ungreased-try-1'
    TRY_2 = 'ungreased-try-2'

    def targets(self):
        yield QueryTarget(self.TRY_1)
        yield QueryTarget(self.TRY_2)

    def run(self, **kwargs):
        if not query(self.TRY_1):
            complete(self.TRY_1)
            raise ValueError('Not greased enough')
        else:
            if not query(self.TRY_2):
                complete(self.TRY_2)


class QuerySinkTask(SinkTask):

    task_id = base.Parameter()

    def requires(self):
        yield QueryTask(task_id=self.task_id + 'req1')
        yield QueryTask(task_id=self.task_id + 'req2')


class TaskWithNoMethods(IndependentTask):
    pass


class JunkProducingTask(IndependentTask):

    def targets(self):
        yield QueryTarget("junk-producer")

    def run(self, **kwargs):
        complete("junk-producer")
        return 3.14


class OptionalParameterTask(IndependentTask):

    optional = Parameter(default="default")

    def targets(self):
        yield QueryTarget(self.optional)

    def run(self, **kwargs):
        complete(self.optional)


class NoTargetsTask(IndependentTask):

    ID = "no-targets"

    def run(self, **kwargs):
        complete(self.ID)


class TaskThatRequresNoTargetTask(IndependentTask):

    ID = "task-that-requires-no-targets-task"

    def requires(self):
        yield NoTargetsTask()

    def run(self):
        complete(self.ID)


def executed_once(task_id):
    return len(registry()[task_id]) == 1


def test_lock(zk):
    worker = scha.Worker(zk)
    task = QueryTask(task_id='1')
    with worker.lock(task) as lock:
        assert lock


def test_missed_parameter():
    with pytest.raises(exception.ParameterMissed):
        task = QueryTask()
        assert task


def test_unexpected_parameter():
    with pytest.raises(exception.ParameterUnexpected):
        task = QueryTask(unexpected='unexpected')
        assert task


def test_executed_once(zk):
    task_id = 'only-once'
    for _ in range(11):
        task = base.task_instance(QueryTask.full_name(), task_id=task_id)
        scha.execute_sync(task, zk)
    assert executed_once(task_id)


def test_multiple_runtime_dependencies(slow_zk):
    for _ in range(10):
        task = MultipleRuntimeDependenciesTask()
        scha.execute_sync(task, slow_zk)
        assert task.complete()


def test_countdown(zk):
    number = 5
    task = base.task_instance(CountdownTask.full_name(),
                                       number=number)
    scha.execute_sync(task, zk)
    for i in range(number):
        task_id = CountdownTask(number=i).identifier()
        assert executed_once(task_id)


def test_start_and_stop_worker(zk):
    worker = scha.Worker(zk)

    def stop():
        worker.stopped = True

    def loop():
        worker.loop()

    stopper = threading.Thread(target=stop)
    looper = threading.Thread(target=loop)

    looper.start()
    stopper.start()
    stopper.join()
    looper.join()


def test_task_of_wrong_type(zk):
    import pickle
    import json
    task_id = 'wrong'
    path = scha.Paths.task_by_id(task_id)

    zk.ensure_path(path)
    data = {scha.TaskLayout.MARSHALLED: six.ensure_text(pickle.dumps(object(), protocol=0)),
            scha.TaskLayout.PRIORITY: '0'}
    zk.set(path, six.ensure_binary(json.dumps(data)))
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskInvalid):
        worker.load(task_id)


def test_corrupted_task(zk):
    task_id = 'corrupted'
    path = scha.Paths.task_by_id(task_id)
    zk.ensure_path(path)
    zk.set(path, six.ensure_binary("dsdasd"))
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskInvalid):
        worker.load(task_id)


def test_impossible_task_no_crash(zk):
    task = ImpossibleTask()
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskFailed):
        for _ in worker.execute_and_get_dependencies(task):
            pass


def test_delete_locked_task(zk):
    task = CompleteTask(task_id='delete-locked')
    task_id = scha.Paths.task_id(task)
    path = scha.Paths.task_by_id(task_id)
    zk.ensure_path(path)
    zk.set(path, base.Task.dumps(task))
    lock_path = scha.Paths.lock(task)
    lock = zk.Lock(lock_path)
    lock.acquire()
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskLocked):
        worker.delete(task, with_lock=True)


def test_locked_task_ignored(zk):
    impossible_task = ImpossibleTask()
    locked_task = CompleteTask(task_id='locked-ignored')
    scha.enqueue(zk, impossible_task)
    scha.enqueue(zk, locked_task)

    lock_path = scha.Paths.lock(locked_task)
    lock = zk.Lock(lock_path)
    lock.acquire()
    worker = scha.Worker(zk)
    for task in worker.tasks():
        task_id = scha.Paths.task_id(task)
        assert task_id != scha.Paths.task_id(locked_task)


def test_disappeared_task(zk):
    task = CompleteTask(task_id='disappeared')
    task_id = scha.Paths.task_id(task)
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskDisappeared):
        worker.load(task_id)


def test_disappeared_task_when_iterating(zk):
    task = CompleteTask(task_id='disappeared-when-iterating')
    scha.enqueue(zk, task)
    worker = scha.Worker(zk)

    delete_task = lambda _: zk.delete(scha.Paths.task(task),
                                      recursive=True)
    worker.callbacks.before_task_loading = delete_task

    for task_id in worker.tasks():
        worker.load(task_id)


def test_connection_loss_on_load(zk):
    task = CompleteTask(task_id='connection-loss')
    task_id = scha.Paths.task_id(task)
    worker = scha.Worker(zk)
    zk.stop()
    with pytest.raises(kazoo.exceptions.KazooException):
        worker.load(task_id)


def test_error_when_locking(zk):
    task = CompleteTask(task_id='error-on-lock')
    worker = scha.Worker(zk)
    worker.callbacks.before_acquiring_lock = lambda _: zk.stop()
    with pytest.raises(kazoo.exceptions.KazooException):
        with worker.lock(task) as l:
            assert not l


def test_disconnected_before_execution(zk):
    task = CompleteTask(task_id='disconneced-before-execution')
    worker = scha.Worker(zk)
    worker.callbacks.before_executing = lambda _: zk.stop()
    with pytest.raises(kazoo.exceptions.KazooException):
        for _ in worker.execute_and_get_dependencies(task):
            pass


def test_broken_pipe(zk):
    task = QueryTask(task_id='broken-pipe')
    worker = scha.Worker(zk)
    worker.callbacks.before_receiving_result = lambda channel: channel.close()
    with pytest.raises(Exception):
        for _ in worker.execute_and_get_dependencies(task):
            pass


def test_unprocessable_task(zk):
    task = UnprocessableTask()
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskInvalid):
        worker.process(task)


def test_broken_enqueue_transaction(zk):
    task = UnprocessableTask()
    path = scha.Paths.task_by_id('wrong/path')
    assert path
    with pytest.raises(exception.TaskNotEnqueued):
        scha._enqueue_within_transaction(zk, task)


def test_execute_async(zk):
    task = UnprocessableTask()
    errors = six.moves.queue.Queue()
    scha._run_worker_until_done(zk, errors)
    assert errors.empty()
    scha.execute_async(task, zk)
    with pytest.raises(scha.RecoverableError):
        scha._run_worker_until_done(zk, errors)


def test_wrong_version(zk):
    task = UnprocessableTask(version='different_version')
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskOutdated):
        worker.process(task)


def test_invalid_task(zk):
    task = InvalidTask()
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskInvalid):
        worker.process(task)


def test_run_worker(zk):
    zk_stopper = threading.Thread(target=zk.stop)

    def _run_worker_safe():
        try:
            scha.run_worker(zk)
        except:
            pass

    worker = threading.Thread(target=_run_worker_safe)
    worker.start()
    zk_stopper.start()
    zk_stopper.join()
    worker.join()


def test_status(zk):
    complete_task = CompleteTask(task_id='test-status')
    unprocessable_task = UnprocessableTask()
    locked_task = QueryTask(task_id='locked')
    wrong_task_id = 'wrong'

    scha.enqueue(zk, locked_task)
    lock = zk.Lock(scha.Paths.lock(locked_task))
    lock.acquire()

    scha.enqueue(zk, complete_task)

    scha.enqueue(zk, unprocessable_task)

    zk.ensure_path(scha.Paths.task_by_id(wrong_task_id))
    zk.set(scha.Paths.task_by_id(wrong_task_id), six.ensure_binary('fdsfds'))

    status = scha.status(zk)

    assert set([complete_task, locked_task, unprocessable_task]) == \
            status[base.Task.Status.ENQUEUED]
    assert set([unprocessable_task]) == \
            status[base.Task.Status.FAILING]
    assert set([complete_task]) == \
            status[base.Task.Status.COMPLETE]
    assert set([complete_task]) == \
            status[base.Task.Status.COMPLETE]
    assert set([locked_task]) == \
            status[base.Task.Status.LOCKED]
    assert set([wrong_task_id]) == \
            status[base.Task.Status.INVALID]


def test_deps_while_run(zk):
    task = SpawningTask()
    scha.execute_sync(task, zk)
    task_id = CountdownTask(number=0).identifier()
    assert executed_once(task_id)


def test_failed_attempt(zk):
    task = UngreasedTask()
    worker = scha.Worker(zk)
    worker.enqueue(task)
    assert not task.complete()
    assert len(list(scha.all_tasks(zk))) == 1
    try:
        worker.try_to_process_any_task(fail_immediately=True)
    except exception.TaskFailed:
        pass
    assert len(list(scha.all_tasks(zk))) == 0


def test_fails_on_cyclic_deps(zk):
    task = UroborosHeadTask()
    #      _oo
    #  ,-- _> \
    #  |      |
    #  |      |
    #  \      /
    #    ----
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskCyclic):
        worker.process(task)


def test_fails_on_dynamic_self_dep(zk):
    task = DynamicUroborosTask()
    #      _oo
    #  ,-- _> \
    #  |      |
    #  |      |
    #  \      /
    #    ----
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskCyclic):
        worker.process(task)


def test_fails_on_uncompletable_task(zk):
    task = UncompletableTask()
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskUncompletable):
        worker.process(task)


def test_purge(zk):
    scha.purge(zk)


def test_force(zk):
    task = CompleteTask(task_id='force-execution')
    scha.execute_sync_force(task, zk)
    assert len(registry()[task.task_id]) == 1


def test_transaction_aborted(zk):
    task = UncompletableTaskWithTransaction()
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskUncompletable):
        worker.process(task)
    assert query(UncompletableTaskWithTransaction.TX_START)
    assert not query(UncompletableTaskWithTransaction.TX_FINISH)
    assert query(UncompletableTaskWithTransaction.TX_ABORT)


def test_execute_locked(zk):
    task = CompleteTask(task_id='execute-locked')
    lock_path = scha.Paths.lock(task)
    lock = zk.Lock(lock_path)
    lock.acquire()
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskLocked):
        worker.process(task)


def test_non_existing_task():
    with pytest.raises(exception.TaskMissed):
        base.task_instance('something that does not exist')


def test_retrieve_all_tasks():
    tasks = base.available_tasks()
    assert tasks


def test_task_declaration():
    expected = '__tests__.test_workflow.CompleteTask(priority,tag,task_id)'
    assert CompleteTask.declaration() == expected


def test_task_description():
    assert CompleteTask.description() == CompleteTask.__doc__


def test_sink_task(zk):
    task = QuerySinkTask(task_id='query-sink-task')
    for r in task.processed_requires():
        assert not r.complete()
    scha.execute_sync(task, zk)
    for r in task.processed_requires():
        assert r.complete()


def test_sink_task_locked_requirements(zk):
    task = QuerySinkTask(task_id='query-sink-task-locked')
    for r in task.processed_requires():
        assert not r.complete()
        lock_path = scha.Paths.lock(r)
        lock = zk.Lock(lock_path)
        lock.acquire()

    worker = scha.Worker(zk)
    worker.enqueue(task)
    worker.try_to_process_any_task(fail_immediately=True)
    for r in task.processed_requires():
        assert not r.complete()


def test_abstract_task():
    with pytest.raises(exception.TaskAbstract):
        base.task_instance(TaskWithNoMethods.full_name())


def test_all_tasks(zk):
    for _ in scha.all_tasks(zk):
        pass
    task = CompleteTask(task_id='present-in-all-tasks')
    scha.enqueue(zk, task)
    for enqueued_task in scha.all_tasks(zk):
        assert scha.Paths.task_id(task) == enqueued_task


def test_all_workers(zk):
    for _ in scha.all_workers(zk):
        pass
    worker = scha.Worker(zk)
    worker.register()
    for available_workers in scha.all_workers(zk):
        assert available_workers == str(worker.worker_id)


def test_execute_sync_force(zk):
    task_id = 'only-twice-by-force'
    task = QueryTask(task_id=task_id)
    scha.execute_sync(task, zk)
    assert len(registry()[task_id]) == 1
    scha.execute_sync_force(task, zk)
    assert len(registry()[task_id]) == 2


def test_produced_junk_ignored(zk):
    task = JunkProducingTask()
    scha.execute_sync(task, zk)


def test_enqueuing_twice(zk):
    task = CompleteTask(task_id='enqueued-twice')
    worker = scha.Worker(zk)
    worker.enqueue(task)
    worker.enqueue(task)


def test_loading_locked_task(zk):
    task = CompleteTask(task_id='locked-on-loading')
    lock_path = scha.Paths.lock(task)
    lock = zk.Lock(lock_path)
    lock.acquire()
    worker = scha.Worker(zk)
    worker.enqueue(task)
    task_id = scha.Paths.task_id(task)
    with pytest.raises(exception.TaskLocked):
        task = worker.load(task_id)
        worker.process(task)


def test_cleanup(zk):
    fake_lock = scha.Paths.lock_by_task_id("fake_locked_task")
    zk.ensure_path(fake_lock)

    task = CompleteTask(task_id='cleaning-up')
    scha.enqueue(zk, task)
    real_lock = scha.Paths.lock(task)
    zk.ensure_path(real_lock)

    worker = scha.Worker(zk)

    assert zk.exists(fake_lock)
    assert zk.exists(real_lock)
    worker.cleanup()
    assert not zk.exists(fake_lock)
    assert zk.exists(real_lock)


def test_optional_parameter(zk):
    task_no_parameter = OptionalParameterTask()
    task_with_parameter = OptionalParameterTask(optional="other")
    scha.execute_sync(task_no_parameter, zk)
    assert task_no_parameter.complete()
    scha.execute_sync(task_with_parameter, zk)
    assert task_with_parameter.complete()


def test_requirements_of_complete_are_not_enqueued(zk):
    task = CountdownTask(number=10)
    for _ in task.processed_run():
        pass
    scha.execute_sync(task, zk)

    assert next(task.targets()).satisfied()
    previous_task = next(task.processed_requires())
    assert not next(previous_task.targets()).satisfied()


def test_no_targets_executed_at_least_once(zk):
    task = NoTargetsTask()
    assert task.ID not in registry()
    scha.execute_sync(task, zk)
    assert task.ID in registry()


def test_priorities(zk):
    highest_priority = QueryTask(task_id='high-priority', priority=1000)
    medium_priority = QueryTask(task_id='medium-priority', priority=500)
    low_priority = QueryTask(task_id='low-priority', priority=0)
    expected_order = [highest_priority, medium_priority, low_priority]

    worker = scha.Worker(zk)
    for task in (low_priority, highest_priority, medium_priority):
        worker.enqueue(task)

    actual_order = [task for task in worker.tasks()]
    assert expected_order == actual_order


def test_disappeared_before_sorting(zk):
    some_task = QueryTask(task_id='something')
    worker = scha.Worker(zk)
    worker.enqueue(some_task)

    worker.callbacks.before_sorting_by_priority = \
            lambda _: zk.delete(scha.Paths.task(some_task),
                                recursive=True)
    for _ in worker.tasks():
        pass


def test_worker_deletes_outdated_task(zk):
    some_task = QueryTask(task_id='something_of_wrong_version')
    worker = scha.Worker(zk)
    worker.enqueue(some_task)
    assert list(scha.all_tasks(zk))
    worker.try_to_process_any_task(fail_immediately=True)
    assert not list(scha.all_tasks(zk))


def test_worker_skips_outdated_locked_task(zk):
    some_task = QueryTask(task_id='something_of_wrong_version_but_locked')
    worker = scha.Worker(zk)
    worker.enqueue(some_task)
    assert list(scha.all_tasks(zk))
    lock = zk.Lock(scha.Paths.lock(some_task))
    lock.acquire()
    assert list(scha.all_tasks(zk))
    try:
        worker.try_to_process_any_task(fail_immediately=True)
    except exception.NoTasksLeft:
        pass
    assert list(scha.all_tasks(zk))


def test_workers_work_on_different_tags(zk):
    def _process_tasks_twice(worker):
        for _ in range(2):
            try:
                worker.try_to_process_any_task(fail_immediately=True)
            except exception.NoTasksLeft:
                return

    TAG_1 = 'tag_1'
    TAG_2 = 'tag_2'

    tag_1_task = QueryTask(task_id=TAG_1 + '_task', tag=TAG_1)
    tag_2_task = QueryTask(task_id=TAG_2 + '_task', tag=TAG_2)

    worker_on_tag_1 = scha.Worker(zk, tag=TAG_1)
    worker_on_tag_2 = scha.Worker(zk, tag=TAG_2)

    # cross enqueue to check they are ok to enqueue different tag
    worker_on_tag_1.enqueue(tag_2_task)
    worker_on_tag_2.enqueue(tag_1_task)

    assert len(list(scha.all_tasks(zk))) == 2

    assert not tag_1_task.complete()
    assert not tag_2_task.complete()
    _process_tasks_twice(worker_on_tag_1)
    assert tag_1_task.complete()
    assert not tag_2_task.complete()
    _process_tasks_twice(worker_on_tag_2)
    assert tag_2_task.complete()


def test_tag_inheritance(zk):
    TAG = 'some_tag'
    task = MultipleRuntimeDependenciesTask(tag=TAG)
    worker = scha.Worker(zk, tag=TAG)
    worker.enqueue(task)
    assert not task.complete()
    try:
        worker.try_to_process_any_task(fail_immediately=True)
    except exception.NoTasksLeft:
        pass
    tasks = list(worker.tasks())
    assert tasks
    for task in tasks:
        assert task.tag == TAG


def test_empty_targets_requirement(zk):
    task = TaskThatRequresNoTargetTask()
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskInvalid):
        worker.process(task)


def test_without_fork(zk):
    task_id = 'without-fork'
    task = QueryTask(task_id=task_id)
    scha.execute_sync(task, zk, do_fork=False)
    assert executed_once(task_id)


def test_with_timeout(zk):
    task = LongTask()
    worker = scha.Worker(zk)
    with pytest.raises(exception.TaskFailed, match="Task timed out"):
        worker.process(task)
