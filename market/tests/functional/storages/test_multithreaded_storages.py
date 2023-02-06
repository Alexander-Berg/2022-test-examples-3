from edera.invokers import MultiThreadedInvoker


def test_storage_is_multithreaded(multithreaded_storage):

    def get_target(index):

        def target():
            multithreaded_storage.put(str(index), str(index**2))

        return target

    MultiThreadedInvoker({str(i): get_target(i) for i in range(5)}).invoke()
    assert {(key, value) for _, key, value in multithreaded_storage.gather()} == {
        ("0", "0"),
        ("1", "1"),
        ("2", "4"),
        ("3", "9"),
        ("4", "16"),
    }
