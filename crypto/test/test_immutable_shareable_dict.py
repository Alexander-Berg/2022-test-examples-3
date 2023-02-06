import multiprocessing

from crypta.lib.python.shared_data_structures.immutable_shareable_dict import ImmutableShareableDict


def check(k, v, data):
    if k not in data:
        assert v is None
    else:
        assert v == data[k]


def test_shared_immutable_dict():
    base_data = {
        "a": 1,
        2: "b",
        b"c": b"d"
    }
    test_keys = ["a", b"c", 2, 3]

    shared_data = ImmutableShareableDict(base_data, "my_name")

    for k in test_keys:
        check(k, shared_data.get(k), base_data)

    result_msgs = multiprocessing.Queue()

    def run_test():
        shared_data = ImmutableShareableDict(name="my_name")
        for key in test_keys:
            result_msgs.put_nowait((key, shared_data.get(key)))
        shared_data.close()

    process = multiprocessing.Process(target=run_test)
    process.start()

    for _ in range(len(test_keys)):
        k, v = result_msgs.get()
        check(k, v, base_data)

    process.join()
    shared_data.close()
