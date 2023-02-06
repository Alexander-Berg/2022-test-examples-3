import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testnonascii():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('nonascii.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    @pytest.mark.xfail
    def test_3_Non_ascii_calendar_data_0_1(self):
        self._test_case("3_Non_ascii_calendar_data", "0_1")

    @pytest.mark.xfail
    def test_3_Non_ascii_calendar_data_1_2(self):
        self._test_case("3_Non_ascii_calendar_data", "1_2")

    @pytest.mark.xfail
    def test_3_Non_ascii_calendar_data_2_3(self):
        self._test_case("3_Non_ascii_calendar_data", "2_3")

    @pytest.mark.xfail
    def test_3_Non_ascii_calendar_data_3_4(self):
        self._test_case("3_Non_ascii_calendar_data", "3_4")

    @pytest.mark.xfail
    def test_3_Non_ascii_calendar_data_4_5(self):
        self._test_case("3_Non_ascii_calendar_data", "4_5")

    @pytest.mark.xfail
    def test_5_Non_utf_8_calendar_data_0_1(self):
        self._test_case("5_Non_utf_8_calendar_data", "0_1")

    @pytest.mark.xfail
    def test_5_Non_utf_8_calendar_data_1_2(self):
        self._test_case("5_Non_utf_8_calendar_data", "1_2")

    @pytest.mark.xfail
    def test_5_Non_utf_8_calendar_data_2_3(self):
        self._test_case("5_Non_utf_8_calendar_data", "2_3")

    def test_6_POSTs_0_1(self):
        self._test_case("6_POSTs", "0_1")

    @pytest.mark.xfail
    def test_6_POSTs_1_2(self):
        self._test_case("6_POSTs", "1_2")

    @pytest.mark.xfail
    def test_7_PUT_with_CN_re_write_0_1(self):
        self._test_case("7_PUT_with_CN_re_write", "0_1")

    @pytest.mark.xfail
    def test_7_PUT_with_CN_re_write_1_2(self):
        self._test_case("7_PUT_with_CN_re_write", "1_2")
