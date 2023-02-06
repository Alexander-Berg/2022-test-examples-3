from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Testschedulenomore():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('schedulenomore.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details

    def test_0_SCHEDULE_Fails_0_1(self):
        self._test_case("0_SCHEDULE_Fails", "0_1")
