import sys
from src.manager import manager
from mock import patch
import os
from re import sub
import tarfile
from library.python import resource
import shutil

default_args = ['prog', '--ssl', '--observer', 'jsondump', '-x', 'scripts/tests', '--subdir', 'CalDAV',
                '--exclude', 'CalDAV/directory.xml',
                '--exclude', 'CalDAV/proxyauthz.xml',
                '--exclude', 'CalDAV/quota.xml',
                '--exclude', 'CalDAV/timezonestdservice.xml']


class TestsRunner():
    def __init__(self):
        _extract_resources()
        _fill_password_in_serverinfo()

    def run(self, xml_file=None, throw_on_fail=False):
        args = self._prepare_args(xml_file)

        result = self._run_caldav_testsing_library(args)

        if throw_on_fail and self._is_failed(result):
            raise Exception("Failed")

        if xml_file is None:
            return result
        return self._convert_file_results(result[0])

    def add_file(self, in_file, out_file):
        shutil.copyfile(in_file, out_file)

    def _is_failed(self, result):
        for test_suite in result[0]['tests']:
            for test in test_suite['tests']:
                if test['result'] != 0:
                    return True
        return False

    def _prepare_args(self, xml_file):
        return default_args + ["--all" if xml_file is None else 'CalDAV/' + xml_file]

    def _run_caldav_testsing_library(self, args):
        with patch.object(sys, 'argv', args):
            mgr = manager()
            mgr.readCommandLine()
            mgr.runAll()

            return self._get_results(mgr.observers)

    def _get_results(self, observers):
        for observer in observers:
            if 'jsondump' in str(type(observer)):
                return observer.manager.results

    def _convert_file_results(self, json_result):
        test_suites = get_tests_sorted_by_name(json_result)
        suite_to_results = {}
        for i, test_suite in enumerate(test_suites):
            name = construct_unique_name(i, test_suite['name'])
            tests = get_tests_sorted_by_name(test_suite)
            test_to_result = {}
            for j, test in enumerate(tests):
                test_name = construct_unique_name(j, test['name'])
                test_to_result[test_name] = (test['result'], test['details'])
            suite_to_results[name] = test_to_result
        return suite_to_results


def get_tests_sorted_by_name(d):
    tests = d['tests']
    tests.sort(key=lambda x: x['name'])
    return tests


def escape_unsupported_letters(st):
    return sub('[^a-zA-Z0-9_]+', '_', st)


def construct_unique_name(number, line):
    return escape_unsupported_letters(str(number) + ' ' + line)


def _extract_resources():
    _extract_tar('scripts.tar.gz')
    _extract_tar('Resource.tar.gz')


def _extract_tar(tar_name):
    with open(tar_name, 'w') as f:
        f.write(resource.find('resfs/file/' + tar_name))
    with tarfile.open(tar_name) as tar_f:
        tar_f.extractall('.')


def _get_user_password():
    pswd_path = os.environ.get('CALDAV_USER_TOKEN_PATH')

    if pswd_path is None:
        raise Exception('Failed to get sandbox token CALDAV_USER_TOKEN_PATH. Please provide it as environment variable for local tests')

    with open(pswd_path, 'rt') as f:
        return f.readline().strip()


def _fill_password_in_serverinfo():
    pswd = _get_user_password()
    template = resource.find('resfs/file/serverinfo_template.xml')
    with open('scripts/server/serverinfo.xml', 'w') as fout:
        fout.write(template.replace('CALDAV_USER_TOKEN', pswd))
