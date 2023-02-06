import multiprocessing

from crypta.lib.python.shared_data_structures.immutable_shareable_list import ImmutableShareableList


def test_immutable_shareable_list():
    base_data = [1, 2.0, b"abc", "строка", False, None]
    data = ImmutableShareableList(base_data, "list")

    def check_item(a, b):
        if a is not None:
            assert a == b
        else:
            assert b is None

    for i, value in enumerate(base_data):
        check_item(value, data[i])

    result_msgs = multiprocessing.Queue()

    def run_test():
        shared_data = ImmutableShareableList(name="list")
        for i in range(len(base_data)):
            result_msgs.put_nowait((i, shared_data[i]))
        shared_data.close()

    process = multiprocessing.Process(target=run_test)
    process.start()

    for _ in range(len(base_data)):
        i, item = result_msgs.get()
        check_item(base_data[i], item)

    process.join()
    data.close(True)
