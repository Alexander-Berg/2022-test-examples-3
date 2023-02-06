import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testmkcalendar():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('mkcalendar.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    @pytest.mark.xfail
    def test_0_MKCALENDAR_read_free_busy_privilege_0_1(self):
        self._test_case("0_MKCALENDAR_read_free_busy_privilege", "0_1")

    @pytest.mark.xfail
    def test_0_MKCALENDAR_read_free_busy_privilege_1_2(self):
        self._test_case("0_MKCALENDAR_read_free_busy_privilege", "1_2")

    @pytest.mark.xfail
    def test_1_MKCALENDAR_supported_component_set_0_1(self):
        self._test_case("1_MKCALENDAR_supported_component_set", "0_1")

    @pytest.mark.xfail
    def test_1_MKCALENDAR_supported_component_set_1_2(self):
        self._test_case("1_MKCALENDAR_supported_component_set", "1_2")

    @pytest.mark.xfail
    def test_1_MKCALENDAR_supported_component_set_2_3(self):
        self._test_case("1_MKCALENDAR_supported_component_set", "2_3")

    @pytest.mark.xfail
    def test_1_MKCALENDAR_supported_component_set_3_4(self):
        self._test_case("1_MKCALENDAR_supported_component_set", "3_4")

    @pytest.mark.xfail
    def test_1_MKCALENDAR_supported_component_set_4_5(self):
        self._test_case("1_MKCALENDAR_supported_component_set", "4_5")

    @pytest.mark.xfail
    def test_1_MKCALENDAR_supported_component_set_5_6(self):
        self._test_case("1_MKCALENDAR_supported_component_set", "5_6")

    @pytest.mark.xfail
    def test_1_MKCALENDAR_supported_component_set_6_7(self):
        self._test_case("1_MKCALENDAR_supported_component_set", "6_7")

    @pytest.mark.xfail
    def test_1_MKCALENDAR_supported_component_set_7_8(self):
        self._test_case("1_MKCALENDAR_supported_component_set", "7_8")

    @pytest.mark.xfail
    def test_1_MKCALENDAR_supported_component_set_8_9(self):
        self._test_case("1_MKCALENDAR_supported_component_set", "8_9")

    @pytest.mark.xfail
    def test_2_MKCALENDAR_with_body_0_1(self):
        self._test_case("2_MKCALENDAR_with_body", "0_1")

    @pytest.mark.xfail
    def test_2_MKCALENDAR_with_body_1_2(self):
        self._test_case("2_MKCALENDAR_with_body", "1_2")

    @pytest.mark.xfail
    def test_2_MKCALENDAR_with_body_2_3(self):
        self._test_case("2_MKCALENDAR_with_body", "2_3")

    @pytest.mark.xfail
    def test_2_MKCALENDAR_with_body_3_4(self):
        self._test_case("2_MKCALENDAR_with_body", "3_4")

    @pytest.mark.xfail
    def test_3_MKCALENDAR_without_body_0_1(self):
        self._test_case("3_MKCALENDAR_without_body", "0_1")

    @pytest.mark.xfail
    def test_3_MKCALENDAR_without_body_1_2(self):
        self._test_case("3_MKCALENDAR_without_body", "1_2")

    @pytest.mark.xfail
    def test_3_MKCALENDAR_without_body_2_3(self):
        self._test_case("3_MKCALENDAR_without_body", "2_3")

    @pytest.mark.xfail
    def test_3_MKCALENDAR_without_body_3_4(self):
        self._test_case("3_MKCALENDAR_without_body", "3_4")
