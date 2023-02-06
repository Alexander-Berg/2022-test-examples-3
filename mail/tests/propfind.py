import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testpropfind():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('propfind.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    @pytest.mark.xfail
    def test_0_Depth_infinity_disabled_1_2(self):
        self._test_case("0_Depth_infinity_disabled", "1_2")

    @pytest.mark.xfail
    def test_0_Depth_infinity_disabled_2_3(self):
        self._test_case("0_Depth_infinity_disabled", "2_3")

    @pytest.mark.xfail
    def test_0_Depth_infinity_disabled_3_4(self):
        self._test_case("0_Depth_infinity_disabled", "3_4")

    @pytest.mark.xfail
    def test_0_Depth_infinity_disabled_4_5(self):
        self._test_case("0_Depth_infinity_disabled", "4_5")

    @pytest.mark.xfail
    def test_0_Depth_infinity_disabled_5_6(self):
        self._test_case("0_Depth_infinity_disabled", "5_6")

    @pytest.mark.xfail
    def test_0_Depth_infinity_disabled_6_7(self):
        self._test_case("0_Depth_infinity_disabled", "6_7")

    def test_1_prop_all_0_1(self):
        self._test_case("1_prop_all", "0_1")

    def test_1_prop_all_1_2(self):
        self._test_case("1_prop_all", "1_2")

    @pytest.mark.xfail
    def test_1_prop_all_2_3(self):
        self._test_case("1_prop_all", "2_3")

    def test_2_prop_errors_0_1(self):
        self._test_case("2_prop_errors", "0_1")

    @pytest.mark.xfail
    def test_2_prop_errors_1_2(self):
        self._test_case("2_prop_errors", "1_2")

    def test_2_prop_errors_2_3(self):
        self._test_case("2_prop_errors", "2_3")

    def test_3_prop_names_0_1(self):
        self._test_case("3_prop_names", "0_1")

    def test_3_prop_names_1_2(self):
        self._test_case("3_prop_names", "1_2")

    @pytest.mark.xfail
    def test_3_prop_names_2_3(self):
        self._test_case("3_prop_names", "2_3")

    def test_4_regular_calendar_prop_finds_0_1(self):
        self._test_case("4_regular_calendar_prop_finds", "0_1")

    def test_4_regular_calendar_prop_finds_1_2(self):
        self._test_case("4_regular_calendar_prop_finds", "1_2")

    @pytest.mark.xfail
    def test_4_regular_calendar_prop_finds_2_3(self):
        self._test_case("4_regular_calendar_prop_finds", "2_3")

    @pytest.mark.xfail
    def test_4_regular_calendar_prop_finds_3_4(self):
        self._test_case("4_regular_calendar_prop_finds", "3_4")

    def test_4_regular_calendar_prop_finds_4_5(self):
        self._test_case("4_regular_calendar_prop_finds", "4_5")

    def test_4_regular_calendar_prop_finds_5_6(self):
        self._test_case("4_regular_calendar_prop_finds", "5_6")

    def test_4_regular_calendar_prop_finds_6_7(self):
        self._test_case("4_regular_calendar_prop_finds", "6_7")

    def test_5_regular_home_prop_finds_0_1(self):
        self._test_case("5_regular_home_prop_finds", "0_1")

    def test_5_regular_home_prop_finds_1_2(self):
        self._test_case("5_regular_home_prop_finds", "1_2")

    @pytest.mark.xfail
    def test_5_regular_home_prop_finds_2_3(self):
        self._test_case("5_regular_home_prop_finds", "2_3")

    @pytest.mark.xfail
    def test_5_regular_home_prop_finds_3_4(self):
        self._test_case("5_regular_home_prop_finds", "3_4")
