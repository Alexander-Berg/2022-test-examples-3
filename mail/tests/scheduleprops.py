import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testscheduleprops():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('scheduleprops.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    @pytest.mark.xfail
    def test_0_Inbox_Outbox_0_1(self):
        self._test_case("0_Inbox_Outbox", "0_1")

    @pytest.mark.xfail
    def test_1_free_busy_set_0_1(self):
        self._test_case("1_free_busy_set", "0_1")

    def test_1_free_busy_set_1_10(self):
        self._test_case("1_free_busy_set", "1_10")

    @pytest.mark.xfail
    def test_1_free_busy_set_2_11(self):
        self._test_case("1_free_busy_set", "2_11")

    @pytest.mark.xfail
    def test_1_free_busy_set_3_2(self):
        self._test_case("1_free_busy_set", "3_2")

    def test_1_free_busy_set_4_3(self):
        self._test_case("1_free_busy_set", "4_3")

    def test_1_free_busy_set_5_4(self):
        self._test_case("1_free_busy_set", "5_4")

    @pytest.mark.xfail
    def test_1_free_busy_set_6_5(self):
        self._test_case("1_free_busy_set", "6_5")

    @pytest.mark.xfail
    def test_1_free_busy_set_7_6(self):
        self._test_case("1_free_busy_set", "7_6")

    @pytest.mark.xfail
    def test_1_free_busy_set_8_7(self):
        self._test_case("1_free_busy_set", "8_7")

    @pytest.mark.xfail
    def test_1_free_busy_set_9_8(self):
        self._test_case("1_free_busy_set", "9_8")

    def test_1_free_busy_set_10_9(self):
        self._test_case("1_free_busy_set", "10_9")
