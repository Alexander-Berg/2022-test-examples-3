import multiprocessing

from crypta.lib.python.shared_data_structures.hot_swap_immutable_shareable_dict import HotSwapImmutableShareableDict


def check(k, v, data):
    if k not in data:
        assert v is None
    else:
        assert v == data[k]


def test_updateable_shared_immutable_dict():
    manager = multiprocessing.Manager()
    base_data = {
        "a": 1,
        2: "b",
        b"c": b"d"
    }
    test_keys = ["a", b"c", 2, 3]

    shared_data = HotSwapImmutableShareableDict("my_prefix", manager)
    shared_data.update(base_data)

    result_msgs = multiprocessing.Queue()

    barrier = manager.Barrier(3)

    def run_test():
        view = shared_data.create_view()

        for key in test_keys:
            result_msgs.put_nowait((key, view.get(key)))

        barrier.wait()
        for key in test_keys:
            result_msgs.put_nowait((key, view.get(key)))
        view.close()

    processes = [multiprocessing.Process(target=run_test) for _ in range(2)]
    for process in processes:
        process.start()

    for _ in range(len(test_keys) * len(processes)):
        k, v = result_msgs.get()
        check(k, v, base_data)

    new_data = {3: "фыва"}

    shared_data.update(new_data)

    barrier.wait()

    for _ in range(len(test_keys) * len(processes)):
        k, v = result_msgs.get()
        check(k, v, new_data)

    shared_data.cleanup()

    for process in processes:
        process.join()

    shared_data.close()
