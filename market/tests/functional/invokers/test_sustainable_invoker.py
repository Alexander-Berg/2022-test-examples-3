import datetime
import threading
import time

import pytest

from edera.invokers import SustainableInvoker


def test_invoker_runs_target_with_given_delay():

    def append_current_timestamp():
        counter[0] += 1
        timestamps.append(datetime.datetime.now())

    def poller():
        if counter[0] >= limit:
            raise RuntimeError

    timestamps = []
    counter = [0]
    limit = 3
    delay = datetime.timedelta(seconds=0.3)
    with pytest.raises(RuntimeError):
        SustainableInvoker(append_current_timestamp, delay=delay).invoke[poller]()
    assert len(timestamps) == limit
    assert 2 * abs((timestamps[2] - timestamps[0]) - (limit - 1) * delay) < delay


def test_invoker_runs_target_forever():

    def append_current_timestamp():
        timestamps.append(datetime.datetime.now())
        raise RuntimeError("to be swallowed")

    def poller():
        if interruption_flag:
            raise RuntimeError

    timestamps = []
    delay = datetime.timedelta(seconds=0.3)
    invoker = SustainableInvoker(append_current_timestamp, delay=delay)
    invoker_thread = threading.Thread(target=invoker.invoke[poller])
    invoker_thread.daemon = True
    interruption_flag = False
    invoker_thread.start()
    time.sleep(0.2)
    assert invoker_thread.is_alive()
    assert len(timestamps) >= 1
    time.sleep(0.8)
    assert invoker_thread.is_alive()
    assert len(timestamps) >= 3
    interruption_flag = True
    time.sleep(0.5)
    assert not invoker_thread.is_alive()
    invoker_thread.join()
