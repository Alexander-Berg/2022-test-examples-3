import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testalarm_dismissal():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('alarm-dismissal.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    @pytest.mark.xfail
    def test_0_Bad_property_value_0_1(self):
        self._test_case("0_Bad_property_value", "0_1")

    @pytest.mark.xfail
    def test_1_Recurring_dismissal_0_1(self):
        self._test_case("1_Recurring_dismissal", "0_1")

    @pytest.mark.xfail
    def test_1_Recurring_dismissal_1_2(self):
        self._test_case("1_Recurring_dismissal", "1_2")

    @pytest.mark.xfail
    def test_1_Recurring_dismissal_2_3(self):
        self._test_case("1_Recurring_dismissal", "2_3")

    @pytest.mark.xfail
    def test_1_Recurring_dismissal_3_4(self):
        self._test_case("1_Recurring_dismissal", "3_4")

    @pytest.mark.xfail
    def test_2_Simple_dismissal_0_1(self):
        self._test_case("2_Simple_dismissal", "0_1")

    @pytest.mark.xfail
    def test_2_Simple_dismissal_1_2(self):
        self._test_case("2_Simple_dismissal", "1_2")

    @pytest.mark.xfail
    def test_2_Simple_dismissal_2_3(self):
        self._test_case("2_Simple_dismissal", "2_3")

    @pytest.mark.xfail
    def test_2_Simple_dismissal_3_4(self):
        self._test_case("2_Simple_dismissal", "3_4")
