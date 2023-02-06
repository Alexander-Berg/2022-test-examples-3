import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testproppatch():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('proppatch.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    @pytest.mark.xfail
    def test_0_prop_patch_property_attributes_0_1(self):
        self._test_case("0_prop_patch_property_attributes", "0_1")

    @pytest.mark.xfail
    def test_0_prop_patch_property_attributes_1_2(self):
        self._test_case("0_prop_patch_property_attributes", "1_2")

    @pytest.mark.xfail
    def test_1_prop_patches_0_1(self):
        self._test_case("1_prop_patches", "0_1")

    @pytest.mark.xfail
    def test_1_prop_patches_1_2(self):
        self._test_case("1_prop_patches", "1_2")

    @pytest.mark.xfail
    def test_1_prop_patches_2_3(self):
        self._test_case("1_prop_patches", "2_3")

    @pytest.mark.xfail
    def test_1_prop_patches_3_4(self):
        self._test_case("1_prop_patches", "3_4")

    @pytest.mark.xfail
    def test_1_prop_patches_4_5(self):
        self._test_case("1_prop_patches", "4_5")

    def test_1_prop_patches_5_6(self):
        self._test_case("1_prop_patches", "5_6")
