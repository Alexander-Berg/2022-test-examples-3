import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testconditional():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('conditional.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    @pytest.mark.xfail
    def test_0_Last_modified_handling_0_1(self):
        self._test_case("0_Last_modified_handling", "0_1")

    def test_0_Last_modified_handling_1_2(self):
        self._test_case("0_Last_modified_handling", "1_2")

    @pytest.mark.xfail
    def test_0_Last_modified_handling_2_3(self):
        self._test_case("0_Last_modified_handling", "2_3")
