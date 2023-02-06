import datetime
import threading
import time

import pytest

from edera.exceptions import MasterSlaveInvocationError
from edera.invokers import MultiThreadedInvoker


def test_invoker_runs_targets_in_parallel():

    def append_index(collection, index):
        time.sleep(0.1 * index)
        collection.append(index)

    collection = []
    targets = {
        "A": lambda: append_index(collection, 2),
        "B": lambda: append_index(collection, 1),
        "C": lambda: append_index(collection, 0),
    }
    MultiThreadedInvoker(targets).invoke()
    assert collection == [0, 1, 2]


def test_invoker_notifies_about_failures():

    def append_index(collection, index):
        time.sleep(0.1 * index)
        if index % 2 == 0:
            raise RuntimeError("index must be odd")
        collection.append(index)

    collection = []
    targets = {
        "A": lambda: append_index(collection, 3),
        "B": lambda: append_index(collection, 2),
        "C": lambda: append_index(collection, 1),
        "D": lambda: append_index(collection, 0),
    }
    with pytest.raises(MasterSlaveInvocationError) as info:
        MultiThreadedInvoker(targets).invoke()
    assert len(list(info.value.failed_slaves)) == 2
    assert collection == [1, 3]


def test_invoker_can_replicate_single_target():

    def increment():
        with mutex:
            counter[0] += 1
            if counter[0] == 15:
                raise RuntimeError("reached the end")

    mutex = threading.Lock()
    counter = [0]
    with pytest.raises(MasterSlaveInvocationError) as info:
        MultiThreadedInvoker.replicate(increment, count=15, prefix="T").invoke()
    assert len(list(info.value.failed_slaves)) == 1
    assert counter == [15]


def test_invoker_terminates_after_being_interrupted():

    def hang():
        while True:
            time.sleep(5)

    def interrupt():
        time.sleep(1)
        raise RuntimeError

    targets = {
        "A": hang,
        "B": hang,
    }
    interrupter = threading.Thread(target=interrupt)
    interrupter.daemon = True
    interrupter.start()
    with pytest.raises(RuntimeError):
        MultiThreadedInvoker(
            targets,
            interruption_timeout=datetime.timedelta(seconds=2)).invoke[interrupt]()
    interrupter.join()
