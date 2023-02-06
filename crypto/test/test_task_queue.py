import multiprocessing

from crypta.lib.python.worker_utils.task_queue import TaskQueue


def test_task_queue():
    cookie_queue = multiprocessing.Queue()
    context = "context"
    worker_count = 2

    result_msgs = multiprocessing.Queue()
    msgs = list(range(5))

    class DummyWorker:
        def __init__(self, worker_config):
            self.worker_config = worker_config

        def run(self):
            for _ in range(len(msgs)):
                task = self.worker_config.task_queue.get()
                result_msgs.put_nowait(task.task ** 2)
                self.worker_config.done_queue.put_nowait(task)

    task_queue = TaskQueue(cookie_queue, context, worker_count, DummyWorker, [{"sensor": "sensor"}])
    cookie = 100

    result = []
    with task_queue:
        for _ in range(worker_count):
            task_queue.schedule(msgs, cookie)

        for _ in range(worker_count):
            assert cookie == cookie_queue.get()

            for _ in msgs:
                result.append(result_msgs.get())

    return sorted(result)
