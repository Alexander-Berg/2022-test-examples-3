import threading

from edera.managers import MongoManager


def test_manager_does_not_leave_background_threads(mongo):
    initial_thread_count = threading.active_count()
    with MongoManager(mongo):
        assert threading.active_count() == initial_thread_count
        assert mongo.instance is not None
        assert threading.active_count() > initial_thread_count
    assert threading.active_count() == initial_thread_count
