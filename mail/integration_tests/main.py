# -*- coding: utf-8 -*-

from mail.swat.integration_tests.request_classes import tests
from mail.swat.integration_tests.lib import const

from threading import Thread
from os import getenv
from argparse import ArgumentParser
from time import sleep


def _run_tests(args):
    params = vars(args)
    tests.run_tests(params)


def _get_args():
    parser = ArgumentParser(description='Integration Testing Tool launcher')
    parser.add_argument('--env', choices=[const.ENV_TESTING, const.ENV_PRODUCTION],
                        default=getenv(const.ENV_APP_ENV, ''),
                        type=str, help='Environment to choose')
    parser.add_argument('--self_tvm', help='Client TVM ID'),
    parser.add_argument('--oauth', type=str, default=getenv(const.ENV_OAUTH, ''),
                        help='OAuth token for authorisation')
    parser.add_argument('--cookie', type=str, default=getenv(const.ENV_COOKIE, ''),
                        help='Request cookie for authorization')
    parser.add_argument('--tvm-secret', type=str, default=getenv(const.ENV_TVM_SECRET, ''),
                        help='TVM secret for application')
    parser.add_argument('--token', type=str,
                        help='User Token from personal account')
    parser.add_argument('--uid', type=int,
                        help='Account UID')
    parser.add_argument('--debug', action='store_true', help='Print additional data')
    return parser.parse_args()



def main():
    args = _get_args()
    _run_tests(args)

if __name__ == '__main__':
    main()
