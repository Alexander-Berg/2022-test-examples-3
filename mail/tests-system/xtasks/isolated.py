from pycommon import *
from subscriber import *
import json

def setUp(self):
    global hub, hub2
    hub = Client(Testing.host(), Testing.port())
    hub2 = Client(Testing.host(), Testing.port2())
    hub.disable('worker')
    hub2.disable('worker')
    # Sleep for a whole alive interval to let alive_coro complete.
    time.sleep(2.2)

def tearDown(self):
    hub.xtasks_clear()
    hub.enable('worker')
    hub2.enable('worker')


class TestXTasksIsolatedSingle:
    def setup(self):
        hub.xtasks_clear()

    def teardown(self):
        hub.xtasks_clear()

    def add(self):
        hub.xtasks_create("1", "fake", 0)
        time.sleep(0.05)

    def start(self, count = 1):
        return len(hub.xtasks_start("A", count))

    def fin(self):
        hub.xtasks_fin("A", "1##fake")

    def delay(self):
        hub.xtasks_delay("A", "1##fake", 0)

    def cleanup(self):
        hub.xtasks_cleanup_active(0)

    def wakeup(self):
        hub.xtasks_wakeup_delayed()

    def test_create(self):
        self.add()

    def test_start_zero_returns_nothing(self):
        self.add()
        assert_equals(self.start(0), 0)

    def test_start_one_returns_one(self):
        self.add()
        assert_equals(self.start(1), 1)

    def test_start_many_from_one_returns_one(self):
        self.add()
        assert_equals(self.start(5), 1)

    def test_start_one_if_active_returns_zero(self):
        self.add()
        self.start(1)
        assert_equals(self.start(1), 0)

    def test_no_actions_give_empty_summary(self):
        summary = hub.xtasks_summary()
        assert_equals(len(summary['pending']), 0)
        assert_equals(len(summary['delayed']), 0)
        assert_equals(len(summary['active']), 0)

    def test_created_task_in_summary_is_pending(self):
        self.add()
        summary = hub.xtasks_summary()
        assert_equals(len(summary['pending']), 1)
        assert_equals(len(summary['delayed']), 0)
        assert_equals(len(summary['active']), 0)

    def test_started_task_in_summary_is_active(self):
        self.add()
        self.start(1)
        summary = hub.xtasks_summary()
        assert_equals(len(summary['pending']), 0)
        assert_equals(len(summary['delayed']), 0)
        assert_equals(len(summary['active']), 1)

    def test_duplicate_in_summary_is_active_and_pending(self):
        self.add()
        self.start(1)
        self.add()
        summary = hub.xtasks_summary()
        assert_equals(len(summary['pending']), 1)
        assert_equals(len(summary['delayed']), 0)
        assert_equals(len(summary['active']), 1)

    def test_duplicate_after_fin_in_summary_is_pending(self):
        self.add()
        self.start(1)
        self.add()
        self.fin()
        summary = hub.xtasks_summary()
        assert_equals(len(summary['pending']), 1)
        assert_equals(len(summary['delayed']), 0)
        assert_equals(len(summary['active']), 0)

    def test_nothing_to_start_after_fin(self):
        self.add()
        self.start(1)
        self.fin()
        assert_equals(self.start(1), 0)

    def test_cant_start_while_active(self):
        self.add()
        self.start(1)
        self.add()
        assert_equals(self.start(1), 0)

    def test_start_pending_duplicate_after_fin(self):
        self.add()
        self.start(1)
        self.add()
        assert_equals(self.start(1), 0)
        self.fin()
        assert_equals(self.start(1), 1)

    def test_complex(self):
        self.add()
        self.start()

        self.add()
        self.fin()

        self.start()
        self.cleanup()
        self.add()

        self.start()
        self.delay()
        self.add()

        self.start()
        self.delay()
        self.wakeup()
        self.add()
        self.start()
        self.add()
        self.fin()

        assert_equals(hub.xtasks_counters()['pending'],1)
