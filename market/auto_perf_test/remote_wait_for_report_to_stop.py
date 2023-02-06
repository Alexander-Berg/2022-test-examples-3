#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import subprocess
import time

from constants import *


def remote_wait_for_report_to_stop(timeout):
    deadline = time.time() + timeout
    while time.time() < deadline:
        time.sleep(1)
        try:
            subprocess.check_output(GET_REPORT_PID_COMMAND, shell=True)
        except subprocess.CalledProcessError as e:
            if e.returncode == 1:
                return
            else:
                raise
    raise Exception('Timed out waiting for Report to stop')


def main():
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('timeout', type=int)
    args = arg_parser.parse_args()
    remote_wait_for_report_to_stop(args.timeout)


if __name__ == "__main__":
    main()
