import pytest

from crypta.lib.python.worker_utils import inflight_tracker


def test_execute_batch():
    tracker = inflight_tracker.InflightTracker()
    tasks = list(range(5))
    result = []

    registered_tasks = tracker.register(tasks, lambda: result.append("finished"))

    for registered_task in registered_tasks[:-1]:
        tracker.complete(registered_task)
        assert [] == result

    tracker.complete(registered_tasks[-1])
    assert ["finished"] == result


def test_bad_task():
    tracker = inflight_tracker.InflightTracker()
    bad_task = inflight_tracker.RegisteredTask("key", "task")
    with pytest.raises(Exception):
        tracker.complete(bad_task)


def test_empty_deps():
    tracker = inflight_tracker.InflightTracker()
    result = []

    registered_tasks = tracker.register([], lambda: result.append("finished"))
    assert [] == registered_tasks
    assert ["finished"] == result
