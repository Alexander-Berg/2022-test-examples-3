import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testrecurrenceput():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('recurrenceput.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    @pytest.mark.xfail
    def test_0_VEVENTs_0_1(self):
        self._test_case("0_VEVENTs", "0_1")

    @pytest.mark.xfail
    def test_0_VEVENTs_1_10(self):
        self._test_case("0_VEVENTs", "1_10")

    @pytest.mark.xfail
    def test_0_VEVENTs_2_11(self):
        self._test_case("0_VEVENTs", "2_11")

    @pytest.mark.xfail
    def test_0_VEVENTs_3_12(self):
        self._test_case("0_VEVENTs", "3_12")

    @pytest.mark.xfail
    def test_0_VEVENTs_4_2(self):
        self._test_case("0_VEVENTs", "4_2")

    @pytest.mark.xfail
    def test_0_VEVENTs_5_3(self):
        self._test_case("0_VEVENTs", "5_3")

    @pytest.mark.xfail
    def test_0_VEVENTs_6_4(self):
        self._test_case("0_VEVENTs", "6_4")

    @pytest.mark.xfail
    def test_0_VEVENTs_7_5(self):
        self._test_case("0_VEVENTs", "7_5")

    @pytest.mark.xfail
    def test_0_VEVENTs_8_6(self):
        self._test_case("0_VEVENTs", "8_6")

    @pytest.mark.xfail
    def test_0_VEVENTs_9_7(self):
        self._test_case("0_VEVENTs", "9_7")

    @pytest.mark.xfail
    def test_0_VEVENTs_10_8(self):
        self._test_case("0_VEVENTs", "10_8")

    @pytest.mark.xfail
    def test_0_VEVENTs_11_9(self):
        self._test_case("0_VEVENTs", "11_9")

    @pytest.mark.xfail
    def test_1_VTODOs_0_1(self):
        self._test_case("1_VTODOs", "0_1")

    @pytest.mark.xfail
    def test_1_VTODOs_1_2(self):
        self._test_case("1_VTODOs", "1_2")

    @pytest.mark.xfail
    def test_1_VTODOs_2_3(self):
        self._test_case("1_VTODOs", "2_3")

    @pytest.mark.xfail
    def test_1_VTODOs_3_4(self):
        self._test_case("1_VTODOs", "3_4")

    @pytest.mark.xfail
    def test_1_VTODOs_4_5(self):
        self._test_case("1_VTODOs", "4_5")

    @pytest.mark.xfail
    def test_1_VTODOs_5_6(self):
        self._test_case("1_VTODOs", "5_6")

    @pytest.mark.xfail
    def test_1_VTODOs_6_7(self):
        self._test_case("1_VTODOs", "6_7")
