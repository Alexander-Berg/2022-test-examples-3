from pycommon import *
from subscriber import *
import time

def setUp(self):
    global hub
    hub = Client(Testing.host(), Testing.port())

def tearDown(self):
    hub.xtasks_clear()


class TestXTasksIntegrated:
    def setup(self):
        hub.xtasks_clear()

    def teardown(self):
        hub.xtasks_clear()

    def test_wakeup_works(self):
        counter = 0
        while counter == 0:
            hub.xtasks_create("1", "fake", 0)
            counter = hub.xtasks_start("A", 1)
        assert_equals(hub.xtasks_counters()['delayed'], 0)
        assert_equals(hub.xtasks_counters()['active'], 1)

        hub.xtasks_delay("A", '1##fake', 0)
        assert_equals(hub.xtasks_counters()['delayed'], 1)
        assert_equals(hub.xtasks_counters()['active'], 0)

        wait(lambda: hub.xtasks_counters()['delayed'] == 0, 5, 0.5)

        print hub.xtasks_summary()['workers']
        counters = hub.xtasks_counters()
        assert_equals(counters['delayed'], 0)
