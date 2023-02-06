import json
import multiprocessing
import time

from frozendict import frozendict
from library.python.monlib import encoder

from crypta.lib.python.worker_utils import inflight_tracker
from crypta.lib.python.worker_utils.multiprocessing_metrics_registry import MultiprocessingMetricRegistry
from crypta.lib.python.worker_utils.task_metrics import TaskMetrics
from crypta.lib.python.worker_utils.worker import Worker
from crypta.lib.python.worker_utils.worker_config import WorkerConfig


class TestWorker(Worker):
    def execute(self, task, labels):
        labels["sensor"] = "sensor"


def test_worker():
    done_queue = multiprocessing.Queue()
    task_queue = multiprocessing.Queue()
    mp_metric_registry = MultiprocessingMetricRegistry()
    mp_metric_registry.start()
    running = multiprocessing.Value('i', 1)

    result_msgs = multiprocessing.Queue()
    labels = {"sensor": "sensor"}
    metrics = {frozendict(labels): TaskMetrics(mp_metric_registry, labels)}

    worker_config = WorkerConfig(
        done_queue,
        task_queue,
        mp_metric_registry,
        running,
        result_msgs,
        metrics,
    )
    task = inflight_tracker.RegisteredTask(1, 1)

    worker = TestWorker(worker_config)
    process = multiprocessing.Process(target=worker.run)
    task_queue.put_nowait(task)
    process.start()
    time.sleep(5)

    running.value = 0
    process.join()

    assert task == done_queue.get_nowait()

    return json.loads(encoder.dumps(mp_metric_registry, format='json'))
