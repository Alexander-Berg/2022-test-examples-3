import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testical_client():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('ical-client.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    def test_0_Account_setup_client_1_0_1(self):
        self._test_case("0_Account_setup_client_1", "0_1")

    @pytest.mark.xfail
    def test_0_Account_setup_client_1_1_2(self):
        self._test_case("0_Account_setup_client_1", "1_2")

    def test_1_Account_setup_client_2_0_1(self):
        self._test_case("1_Account_setup_client_2", "0_1")

    @pytest.mark.xfail
    def test_1_Account_setup_client_2_1_2(self):
        self._test_case("1_Account_setup_client_2", "1_2")

    def test_2_Polling_client_1_0_1(self):
        self._test_case("2_Polling_client_1", "0_1")

    def test_3_Polling_client_2_0_1(self):
        self._test_case("3_Polling_client_2", "0_1")
