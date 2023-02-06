import multiprocessing

from yamarec1.kit.helpers import LazyProxy


def test_lazy_proxy_delegates_method_calls_to_subject():
    proxy = LazyProxy[list]()
    proxy.extend(range(5))
    assert len(proxy) == 5
    assert proxy == [0, 1, 2, 3, 4]


def test_lazy_proxy_delays_instantiation():

    class InstanceCounter(object):

        count = 0

        def __init__(self):
            InstanceCounter.count += 1

    proxy = LazyProxy[InstanceCounter]()
    assert InstanceCounter.count == 0
    repr(proxy)
    assert InstanceCounter.count == 1


def test_lazy_proxy_is_forkaware():

    def append_world():
        proxy.append("world")
        assert proxy == ["world"]  # and not ["hello", "world"]

    proxy = LazyProxy[list]()
    proxy.append("hello")
    process = multiprocessing.Process(target=append_world)
    process.start()
    process.join()
    assert proxy == ["hello"]  # still
