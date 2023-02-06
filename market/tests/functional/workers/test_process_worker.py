import datetime
import multiprocessing
import time

from edera.exceptions import ExcusableError
from edera.workers import ProcessWorker


def test_worker_works_correctly():

    def work(stop_flag):
        stop_flag.wait()

    stop_flag = multiprocessing.Event()
    worker = ProcessWorker("worker", lambda: work(stop_flag))
    assert not worker.alive
    assert not worker.failed
    assert not worker.stopped
    worker.start()
    assert worker.alive
    assert not worker.failed
    assert not worker.stopped
    stop_flag.set()
    worker.join(datetime.timedelta(seconds=1))
    assert not worker.alive
    assert not worker.failed
    assert not worker.stopped


def test_worker_fails_gracefully():

    def fail():
        raise RuntimeError("oh shoot")

    worker = ProcessWorker("worker", fail)
    worker.start()
    worker.join(datetime.timedelta(seconds=1))
    assert not worker.alive
    assert worker.failed
    assert not worker.stopped


def test_worker_stops_gracefully():

    def stop():
        raise ExcusableError("oh shoot")

    worker = ProcessWorker("worker", stop)
    worker.start()
    worker.join(datetime.timedelta(seconds=1))
    assert not worker.alive
    assert not worker.failed
    assert worker.stopped


def test_worker_can_be_killed():

    def hang():
        while True:
            time.sleep(5)

    worker = ProcessWorker("worker", hang)
    worker.start()
    worker.join(datetime.timedelta(seconds=1))
    worker.kill()
    assert not worker.alive
    assert not worker.failed
    assert not worker.stopped
