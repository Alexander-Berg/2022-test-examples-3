from os import mkdir

from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner, \
    escape_unsupported_letters, get_tests_sorted_by_name, construct_unique_name

OUT_DIR = 'tests'

HEADER = """from mail.calendar.python.caldav_tests_runner.caldavtester_runner import TestsRunner


class Test{feature}():
    @classmethod
    def setup_class(cls):
        runner = TestsRunner()
        cls.results = runner.run('{xml_name}.xml')

    def _test_case(self, suite_name, test_name):
        code, details = self.results[suite_name][test_name]
        assert code == 0, details
"""

XFAILED = '    @pytest.mark.xfail\n'

TEST = """    def {entrypoint_name}(self):
        self._test_case("{suite_name}", "{test_name}")
"""


def main():
    mkdir(OUT_DIR)
    results = TestsRunner().run()
    with open(OUT_DIR + '/ya.make.inc', 'w') as ya_f:
        ya_f.write('TEST_SRCS(\n')
        for file in results:
            Generator().gen_file(file, ya_f)
        ya_f.write(')\n')


class Generator():
    def __init__(self):
        self.has_xfail = False
        self.tests_to_file = []

    def _gen_test(self, j, test, suite_name):
        test_name = construct_unique_name(j, test['name'])
        result = test['result']
        if result == 3:  # IGNORED
            return

        entrypoint_name = "test_" + suite_name + "_" + test_name
        entrypoint = TEST.replace('{entrypoint_name}', entrypoint_name)\
                        .replace('{suite_name}', suite_name)\
                        .replace('{test_name}', test_name)

        if result != 0:  # NOT OK
            self.has_xfail = True
            entrypoint = XFAILED + entrypoint

        self.tests_to_file.append('\n' + entrypoint)

    def _gen_suite(self, i, test_suite):
        suite_name = construct_unique_name(i, test_suite['name'])
        tests = get_tests_sorted_by_name(test_suite)
        for j, test in enumerate(tests):
            self._gen_test(j, test, suite_name)

    def _extract_feature_from_filename(self, file_name):
        without_path = file_name.split('/')[-1]
        without_ext = without_path.replace('.xml', '')
        return without_ext

    def gen_file(self, file, ya_f):
        xml_name = self._extract_feature_from_filename(file['name'])
        feature = escape_unsupported_letters(xml_name)

        test_suites = get_tests_sorted_by_name(file)
        for i, test_suite in enumerate(test_suites):
            self._gen_suite(i, test_suite)
        if len(self.tests_to_file) == 0:
            return

        ya_f.write('\t' + OUT_DIR + '/' + feature+'.py\n')

        with open(OUT_DIR + '/' + feature + '.py', 'w') as f:
            if self.has_xfail:
                f.write('import pytest\n')
            f.write(HEADER.replace('{feature}', feature).replace('{xml_name}', xml_name))
            for test_to_file in self.tests_to_file:
                f.write(test_to_file)


if __name__ == "__main__":
    main()
