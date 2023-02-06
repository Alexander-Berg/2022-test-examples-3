# -*- coding: utf-8 -*-
"""
Parse serps and compare results with expected outcome.
"""

import argparse
import json
import sys
import pandas as pd

from utils import read_json
from calc_offline_dashboard import get_date


def get_serp_by_id(df, serp_id):
    return df[df.serp_id.eq(serp_id)]


def check_tests(df, test_data):
    failed_tests = []
    for test_suit in test_data:
        serp_id, tests = test_suit['serp_id'], test_suit['tests']
        data = get_serp_by_id(df, serp_id)
        global_context = dict(data=data)

        for test_expr in tests:
            try:
                result = eval(test_expr, global_context)
                result = bool(result)
            except Exception as e:
                message = "serp_id={}\texpr={}\n{}\n\n".format(serp_id, test_expr, e.message)
                sys.stderr.write(message)
                result = False
            if not result:
                failed_tests.append([serp_id, test_expr])
    return failed_tests


def main(args):
    test_data = read_json(args.tests)
    df = pd.read_csv(args.input, sep='\t', encoding='utf8')
    failed_tests = check_tests(df, test_data)

    for failed in failed_tests:
        msg = "Serp_id={}:\nFailed expression {}\n\n".format(*failed)
        sys.stderr.write(msg)

    dashboard_result = {"failed_tests": len(failed_tests), "fielddate": get_date(args.timestamp)}
    with open(args.output, 'w') as f:
        json.dump(dashboard_result, f)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(add_help=True)
    parser.add_argument('-i', dest='input', required=True, help='parsed serps tsv')
    parser.add_argument('-l',  dest='line', help='input json is lined json', action='store_true')
    parser.add_argument('-o', dest='output', required=True, help='wizard data')
    parser.add_argument('-ts', dest='timestamp', required=True, help='timestamp for dashboard')
    parser.add_argument('-t', dest='tests', required=True, help='tests description. Json with "serp_id" and "tests" keys.')
    args = parser.parse_args()
    main(args)
