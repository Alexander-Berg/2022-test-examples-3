import argparse
import logging
import re
import sys

import pytest

import library.python.pytest.plugins.conftests as conftests

from . import filter_tests
from . import build_sandbox_json
from . import collection
from . import metasearch

DEFAULT_STAND_PATH = 'http://addrs-testing.search.yandex.net/search/stable/yandsearch?'


def get_feature_names():
    feature_names = []
    for name in sys.extra_modules:
        m = re.match(r'^__tests__.test_(?P<feature_name>[^\.]+)$', name)
        if m is not None:
            feature_names.append(m.group('feature_name'))
    feature_names.sort()
    return feature_names


def parse_metasearch(param):
    if '/yandsearch?' in param:
        return param

    m = re.match(r'^(http://)?(?P<sockaddr>.+?)(/yandsearch)?$', param)
    if m is not None:
        return 'http://{}/yandsearch?'.format(m.group('sockaddr'))

    return param


def parse_args():
    feature_names = get_feature_names()

    parser = argparse.ArgumentParser()
    parser.add_argument('features', nargs='*', metavar='FEATURE', help='BDD feature')
    parser.add_argument('-L', '--list-tests', help='List tests', action='store_true')
    parser.add_argument('--json', help='Save JSON report to a file')
    parser.add_argument('--metasearch', help='Custom upper metasearch instance to test', default=DEFAULT_STAND_PATH)
    parser.add_argument('-F', '--test-filter', help='Run only tests that match <test-filter>', action='append')
    parser.add_argument('--test-stderr', help='Show stderr of tests', action='store_true', default=False)
    args = parser.parse_args()

    for name in args.features:
        if name not in feature_names:
            parser.error(
                'Unknown BDD feature: {}\n\nPossible values:\n{}'.format(
                    name, ''.join('- {}\n'.format(n) for n in feature_names)
                )
            )
    return args


def main():
    logging.getLogger().setLevel(logging.INFO)
    args = parse_args()

    metasearch.ENDPOINT = parse_metasearch(args.metasearch)

    filter_tests.TEST_FILTER = args.test_filter
    filter_tests.FEATURES = args.features

    pytest_args = [
        '-p',
        'no:warnings',  # disable stupid RemovedInPytest4Warning
        '-p',
        'no:cacheprovider',  # prevents pytest from creating .cache directory
        '--tb=short',
    ]

    if args.list_tests:
        pytest_args.append('--collect-only')
    if args.test_stderr:
        pytest_args.append('--capture=no')

    plugins = [
        collection.CollectionPlugin(['test_{}'.format(name) for name in get_feature_names()]),
        conftests,
        filter_tests,
    ]

    if args.json is not None:
        plugins.append(build_sandbox_json.JsonReporter(args.json))

    rc = pytest.main(args=pytest_args, plugins=plugins)
    sys.exit(rc)


if __name__ == '__main__':
    main()
