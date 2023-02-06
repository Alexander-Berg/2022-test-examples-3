#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import logging
import os
import subprocess
import time
import urllib2

from constants import *


logging.basicConfig(filename=os.path.abspath(__file__) + '.log', level=logging.INFO, filemode='w')


def on_error(msg):
    logging.error(msg)
    raise Exception(msg)


def on_report_not_running():
    on_error('Report is not running')


def on_report_start_timeout():
    on_error('Timed out waiting for Report to start')


def remote_wait_for_report_to_start(timeout, report_port):
    deadline = time.time() + timeout
    url = 'http://localhost:{0}/yandsearch?place=consistency_check'.format(report_port)
    report_started = False
    while time.time() < deadline:
        time.sleep(1)
        try:
            subprocess.check_output(GET_REPORT_PID_COMMAND, shell=True)
            if not report_started:
                logging.info('Report process started. Waiting for readiness')
                report_started = True
        except:
            on_report_not_running()
        response = None
        try:
            response = urllib2.urlopen(url, timeout=1)
        except:
            pass
        if response is not None:
            output = response.read()
            if output.strip() == '0;OK':
                logging.info('Report is ready to work')
                return
    on_report_start_timeout()


def main():
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('timeout', type=int)
    arg_parser.add_argument('port', type=int)
    args = arg_parser.parse_args()
    logging.info('Trying to run report on port {}. Waiting for {} seconds'.format(args.port, args.timeout))
    remote_wait_for_report_to_start(args.timeout, args.port)


if __name__ == "__main__":
    main()
