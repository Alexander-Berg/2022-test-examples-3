import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class TestcaldavIOP():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('caldavIOP.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    @pytest.mark.xfail
    def test_0_1_Event_Creation_0_1_1(self):
        self._test_case("0_1_Event_Creation", "0_1_1")

    @pytest.mark.xfail
    def test_0_1_Event_Creation_1_1_2(self):
        self._test_case("0_1_Event_Creation", "1_1_2")

    @pytest.mark.xfail
    def test_0_1_Event_Creation_2_1_3(self):
        self._test_case("0_1_Event_Creation", "2_1_3")

    @pytest.mark.xfail
    def test_0_1_Event_Creation_3_1_4(self):
        self._test_case("0_1_Event_Creation", "3_1_4")

    @pytest.mark.xfail
    def test_1_2_Event_Modification_0_2_1(self):
        self._test_case("1_2_Event_Modification", "0_2_1")

    @pytest.mark.xfail
    def test_1_2_Event_Modification_1_2_2(self):
        self._test_case("1_2_Event_Modification", "1_2_2")

    @pytest.mark.xfail
    def test_1_2_Event_Modification_2_2_3(self):
        self._test_case("1_2_Event_Modification", "2_2_3")

    @pytest.mark.xfail
    def test_1_2_Event_Modification_3_2_4(self):
        self._test_case("1_2_Event_Modification", "3_2_4")

    @pytest.mark.xfail
    def test_1_2_Event_Modification_4_2_5(self):
        self._test_case("1_2_Event_Modification", "4_2_5")

    @pytest.mark.xfail
    def test_1_2_Event_Modification_5_2_6(self):
        self._test_case("1_2_Event_Modification", "5_2_6")

    @pytest.mark.xfail
    def test_1_2_Event_Modification_6_2_7(self):
        self._test_case("1_2_Event_Modification", "6_2_7")

    @pytest.mark.xfail
    def test_1_2_Event_Modification_7_2_8(self):
        self._test_case("1_2_Event_Modification", "7_2_8")

    @pytest.mark.xfail
    def test_1_2_Event_Modification_8_2_9(self):
        self._test_case("1_2_Event_Modification", "8_2_9")

    @pytest.mark.xfail
    def test_2_4_Event_Deletion_0_4_1(self):
        self._test_case("2_4_Event_Deletion", "0_4_1")

    @pytest.mark.xfail
    def test_2_4_Event_Deletion_1_4_2(self):
        self._test_case("2_4_Event_Deletion", "1_4_2")

    @pytest.mark.xfail
    def test_2_4_Event_Deletion_2_4_3(self):
        self._test_case("2_4_Event_Deletion", "2_4_3")

    @pytest.mark.xfail
    def test_2_4_Event_Deletion_3_4_4(self):
        self._test_case("2_4_Event_Deletion", "3_4_4")

    @pytest.mark.xfail
    def test_2_4_Event_Deletion_4_4_5(self):
        self._test_case("2_4_Event_Deletion", "4_4_5")
