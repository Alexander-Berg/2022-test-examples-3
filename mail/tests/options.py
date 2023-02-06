import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testoptions():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('options.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    def test_0_OPTIONS_DAV_0_1(self):
        self._test_case("0_OPTIONS_DAV", "0_1")

    def test_0_OPTIONS_DAV_1_2(self):
        self._test_case("0_OPTIONS_DAV", "1_2")

    @pytest.mark.xfail
    def test_1_PROPFIND_no_DAV_0_1(self):
        self._test_case("1_PROPFIND_no_DAV", "0_1")

    @pytest.mark.xfail
    def test_1_PROPFIND_no_DAV_1_2(self):
        self._test_case("1_PROPFIND_no_DAV", "1_2")
