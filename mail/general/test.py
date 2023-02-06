import yatest.common
from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


def test_propfind():
    TEST_PROFIND_XML = 'test_propfind.xml'

    tester = TestsRunner()
    tester.add_file(_get_script_path(TEST_PROFIND_XML), 'scripts/tests/CalDAV/' + TEST_PROFIND_XML)
    tester.run(xml_file=TEST_PROFIND_XML, throw_on_fail=True)


def test_synclimit():
    TEST_SYNCLIMIT_XML = 'test_synclimit.xml'

    tester = TestsRunner()
    tester.add_file(_get_script_path(TEST_SYNCLIMIT_XML), 'scripts/tests/CalDAV/' + TEST_SYNCLIMIT_XML)
    tester.add_file(_get_resource_path('report.xml'), 'Resource/CalDAV/report.xml')
    tester.run(xml_file=TEST_SYNCLIMIT_XML, throw_on_fail=True)


def _get_resource_path(file_name):
    return yatest.common.source_path('mail/calendar/caldav-ut/handwritten/resources/CalDAV/' + file_name)


def _get_script_path(file_name):
    return yatest.common.source_path('mail/calendar/caldav-ut/handwritten/scripts/tests/CalDAV/' + file_name)
