import threading

from edera import coroutine


def test_coroutine_clones_context():

    @coroutine
    def routine(cc):
        enter_flag = threading.Event()
        exit_flag = threading.Event()
        thread = threading.Thread(target=nested_routine[cc], args=(enter_flag, exit_flag))
        thread.daemon = True
        thread.start()
        enter_flag.wait(1.0)
        assert enter_flag.is_set()
        yield
        exit_flag.set()
        thread.join(1.0)

    @coroutine
    def nested_routine(cc, enter_flag, exit_flag):
        with cc.extend(append_mark):
            enter_flag.set()
            yield
            exit_flag.wait()

    def append_mark():
        marks.append(1)

    marks = []
    routine()
    assert len(marks) == 1
