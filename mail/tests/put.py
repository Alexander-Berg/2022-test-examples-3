import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testput():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('put.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    @pytest.mark.xfail
    def test_0_PUT_VEVENT_0_1(self):
        self._test_case("0_PUT_VEVENT", "0_1")

    @pytest.mark.xfail
    def test_0_PUT_VEVENT_2_3(self):
        self._test_case("0_PUT_VEVENT", "2_3")

    @pytest.mark.xfail
    def test_1_PUT_with_Content_Type_parameters_0_1(self):
        self._test_case("1_PUT_with_Content_Type_parameters", "0_1")

    @pytest.mark.xfail
    def test_1_PUT_with_Content_Type_parameters_1_2(self):
        self._test_case("1_PUT_with_Content_Type_parameters", "1_2")

    @pytest.mark.xfail
    def test_1_PUT_with_Content_Type_parameters_2_3(self):
        self._test_case("1_PUT_with_Content_Type_parameters", "2_3")

    @pytest.mark.xfail
    def test_3_PUT_with_X_using_VALUE_TEXT_0_1(self):
        self._test_case("3_PUT_with_X_using_VALUE_TEXT", "0_1")

    @pytest.mark.xfail
    def test_4_PUT_with_relaxed_parsing_0_1(self):
        self._test_case("4_PUT_with_relaxed_parsing", "0_1")

    @pytest.mark.xfail
    def test_4_PUT_with_relaxed_parsing_1_2(self):
        self._test_case("4_PUT_with_relaxed_parsing", "1_2")

    @pytest.mark.xfail
    def test_5_PUTs_with_parameter_encoding_0_1(self):
        self._test_case("5_PUTs_with_parameter_encoding", "0_1")

    @pytest.mark.xfail
    def test_7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values_0_1(self):
        self._test_case("7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values", "0_1")

    @pytest.mark.xfail
    def test_7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values_1_10(self):
        self._test_case("7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values", "1_10")

    @pytest.mark.xfail
    def test_7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values_2_11(self):
        self._test_case("7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values", "2_11")

    @pytest.mark.xfail
    def test_7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values_3_2(self):
        self._test_case("7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values", "3_2")

    @pytest.mark.xfail
    def test_7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values_4_3(self):
        self._test_case("7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values", "4_3")

    @pytest.mark.xfail
    def test_7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values_5_4(self):
        self._test_case("7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values", "5_4")

    @pytest.mark.xfail
    def test_7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values_6_5(self):
        self._test_case("7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values", "6_5")

    @pytest.mark.xfail
    def test_7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values_7_6(self):
        self._test_case("7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values", "7_6")

    @pytest.mark.xfail
    def test_7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values_8_7(self):
        self._test_case("7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values", "8_7")

    @pytest.mark.xfail
    def test_7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values_9_8(self):
        self._test_case("7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values", "9_8")

    @pytest.mark.xfail
    def test_7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values_10_9(self):
        self._test_case("7_Problem_VEVENTs_EXDATE_various_combinations_of_date_time_date_values", "10_9")

    @pytest.mark.xfail
    def test_8_Problem_VTODOs_various_combinations_of_date_time_date_values_0_1(self):
        self._test_case("8_Problem_VTODOs_various_combinations_of_date_time_date_values", "0_1")

    @pytest.mark.xfail
    def test_8_Problem_VTODOs_various_combinations_of_date_time_date_values_1_10(self):
        self._test_case("8_Problem_VTODOs_various_combinations_of_date_time_date_values", "1_10")

    @pytest.mark.xfail
    def test_8_Problem_VTODOs_various_combinations_of_date_time_date_values_2_11(self):
        self._test_case("8_Problem_VTODOs_various_combinations_of_date_time_date_values", "2_11")

    @pytest.mark.xfail
    def test_8_Problem_VTODOs_various_combinations_of_date_time_date_values_3_12(self):
        self._test_case("8_Problem_VTODOs_various_combinations_of_date_time_date_values", "3_12")

    @pytest.mark.xfail
    def test_8_Problem_VTODOs_various_combinations_of_date_time_date_values_4_2(self):
        self._test_case("8_Problem_VTODOs_various_combinations_of_date_time_date_values", "4_2")

    @pytest.mark.xfail
    def test_8_Problem_VTODOs_various_combinations_of_date_time_date_values_5_3(self):
        self._test_case("8_Problem_VTODOs_various_combinations_of_date_time_date_values", "5_3")

    @pytest.mark.xfail
    def test_8_Problem_VTODOs_various_combinations_of_date_time_date_values_6_4(self):
        self._test_case("8_Problem_VTODOs_various_combinations_of_date_time_date_values", "6_4")

    @pytest.mark.xfail
    def test_8_Problem_VTODOs_various_combinations_of_date_time_date_values_7_5(self):
        self._test_case("8_Problem_VTODOs_various_combinations_of_date_time_date_values", "7_5")

    @pytest.mark.xfail
    def test_8_Problem_VTODOs_various_combinations_of_date_time_date_values_8_6(self):
        self._test_case("8_Problem_VTODOs_various_combinations_of_date_time_date_values", "8_6")

    @pytest.mark.xfail
    def test_8_Problem_VTODOs_various_combinations_of_date_time_date_values_9_7(self):
        self._test_case("8_Problem_VTODOs_various_combinations_of_date_time_date_values", "9_7")

    @pytest.mark.xfail
    def test_8_Problem_VTODOs_various_combinations_of_date_time_date_values_10_8(self):
        self._test_case("8_Problem_VTODOs_various_combinations_of_date_time_date_values", "10_8")

    @pytest.mark.xfail
    def test_8_Problem_VTODOs_various_combinations_of_date_time_date_values_11_9(self):
        self._test_case("8_Problem_VTODOs_various_combinations_of_date_time_date_values", "11_9")

    @pytest.mark.xfail
    def test_9_Put_VTODO_0_1(self):
        self._test_case("9_Put_VTODO", "0_1")

    @pytest.mark.xfail
    def test_9_Put_VTODO_1_2(self):
        self._test_case("9_Put_VTODO", "1_2")

    @pytest.mark.xfail
    def test_9_Put_VTODO_2_3(self):
        self._test_case("9_Put_VTODO", "2_3")

    @pytest.mark.xfail
    def test_9_Put_VTODO_3_4(self):
        self._test_case("9_Put_VTODO", "3_4")

    @pytest.mark.xfail
    def test_9_Put_VTODO_4_5(self):
        self._test_case("9_Put_VTODO", "4_5")
