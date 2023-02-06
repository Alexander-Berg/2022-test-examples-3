#!/usr/bin/env python
#
# $Id$
# $HeadURL$
#
# related to https://st.yandex-team.ru/SERP-61678

import argparse
import json
import sys
import xml.etree.ElementTree as ET


TERMCOLORS = {
    'FAIL': '\033[91m', # red
    'PASS': '\033[92m', # green
    'SKIP': '\033[93m', # yellow
    'LINE': '\033[35m', # purple
    'ENDC': '\033[0m',
}

TERMCOLORS['failure']   = TERMCOLORS['FAIL']
TERMCOLORS['passed']    = TERMCOLORS['PASS']
TERMCOLORS['skipped']   = TERMCOLORS['SKIP']


def colored(string, color):
    return '{}{}{}'.format(TERMCOLORS[color], string, TERMCOLORS['ENDC'])

def filter_tests(tests, status):
    if status:
        filtered = []
        for test_case in tests:
            if test_case['status'] in status:
                filtered.append(test_case)
    else:
        filtered = tests

    return filtered

def parse_args():
    parser = argparse.ArgumentParser(description='results.xml viewer/converter',
        formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('filename', nargs=1, help='path to results.xml')
    parser.add_argument('--ofmt',   help='output format <JSON|TERM>', dest='ofmt',
        action='store', default='TERM', metavar='FORMAT')
    parser.add_argument('--stats',  help='print tests statistics', action="store_true")
    parser.add_argument('--status', help='status to show <failure|passed|skipped>, all if omitted',
        dest='status', action='store', nargs='*', default=[])

    return parser.parse_args()

def parse_xml(string):
    root = ET.fromstring(string)
    stats = root.attrib
    tests = []

    for test_case in root:
        details = test_case.attrib

        for status in test_case:
            details[status.tag] = status.attrib
            details['status'] = status.tag

        if not 'status' in details:
            details['status'] = 'passed'

        tests.append(details)

    return (stats, tests)


if __name__ == '__main__':
    opts = parse_args()

    data = open(sys.argv[1]).read()
    data = parse_xml(data)

    if opts.stats:
        print(json.dumps(data[0], indent=3, sort_keys=True))
    else:
        results = filter_tests(data[1], opts.status)

        if opts.ofmt == 'TERM':
            for test_case in results:
                print('{}\t{} {}'.format(
                    colored(test_case['status'].upper(), test_case['status']),
                    test_case['name'],
                    colored('{}@{}:{}'.format(
                        test_case['classname'],
                        test_case['file'],
                        test_case['line']
                    ), 'LINE')
                ))

                if test_case['status'] in test_case and 'message' in test_case[test_case['status']]:
                    print('\t{}'.format(test_case[test_case['status']]['message']))

        elif opts.ofmt == 'JSON':
            print(json.dumps(results, indent=3, sort_keys=True))
        else:
            sys.exit('Unsupported output format specified: {}'.format(opts.ofmt))

