import pytest
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testattachments():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('attachments.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    @pytest.mark.xfail
    def test_0_External_0_1(self):
        self._test_case("0_External", "0_1")

    @pytest.mark.xfail
    def test_0_External_1_2(self):
        self._test_case("0_External", "1_2")

    @pytest.mark.xfail
    def test_1_Inline_0_1(self):
        self._test_case("1_Inline", "0_1")

    @pytest.mark.xfail
    def test_1_Inline_1_2(self):
        self._test_case("1_Inline", "1_2")

    @pytest.mark.xfail
    def test_2_Too_many_0_1(self):
        self._test_case("2_Too_many", "0_1")

    @pytest.mark.xfail
    def test_2_Too_many_1_2(self):
        self._test_case("2_Too_many", "1_2")

    @pytest.mark.xfail
    def test_2_Too_many_2_3(self):
        self._test_case("2_Too_many", "2_3")

    @pytest.mark.xfail
    def test_2_Too_many_3_4(self):
        self._test_case("2_Too_many", "3_4")
