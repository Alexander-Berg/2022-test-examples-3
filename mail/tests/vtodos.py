import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testvtodos():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('vtodos.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    @pytest.mark.xfail
    def test_0_PUT_0_1(self):
        self._test_case("0_PUT", "0_1")
