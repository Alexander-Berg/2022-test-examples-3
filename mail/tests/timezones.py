import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testtimezones():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('timezones.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    @pytest.mark.xfail
    def test_0_Timezone_cache_0_1(self):
        self._test_case("0_Timezone_cache", "0_1")

    @pytest.mark.xfail
    def test_0_Timezone_cache_1_2(self):
        self._test_case("0_Timezone_cache", "1_2")

    @pytest.mark.xfail
    def test_0_Timezone_cache_2_3(self):
        self._test_case("0_Timezone_cache", "2_3")

    @pytest.mark.xfail
    def test_0_Timezone_cache_3_4(self):
        self._test_case("0_Timezone_cache", "3_4")

    @pytest.mark.xfail
    def test_1_Timezone_cache_aliases_0_1(self):
        self._test_case("1_Timezone_cache_aliases", "0_1")

    @pytest.mark.xfail
    def test_1_Timezone_cache_aliases_1_2(self):
        self._test_case("1_Timezone_cache_aliases", "1_2")

    @pytest.mark.xfail
    def test_1_Timezone_cache_aliases_2_3(self):
        self._test_case("1_Timezone_cache_aliases", "2_3")

    @pytest.mark.xfail
    def test_1_Timezone_cache_aliases_3_4(self):
        self._test_case("1_Timezone_cache_aliases", "3_4")

    @pytest.mark.xfail
    def test_2_Timezone_properties_0_1(self):
        self._test_case("2_Timezone_properties", "0_1")

    def test_2_Timezone_properties_1_1(self):
        self._test_case("2_Timezone_properties", "1_1")

    @pytest.mark.xfail
    def test_2_Timezone_properties_3_3(self):
        self._test_case("2_Timezone_properties", "3_3")

    def test_2_Timezone_properties_4_4(self):
        self._test_case("2_Timezone_properties", "4_4")

    @pytest.mark.xfail
    def test_2_Timezone_properties_6_6(self):
        self._test_case("2_Timezone_properties", "6_6")

    def test_2_Timezone_properties_7_7(self):
        self._test_case("2_Timezone_properties", "7_7")
