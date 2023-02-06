#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import socket
import time


def backctld_command(command):
    TIMEOUT = 10
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(TIMEOUT)
    s.connect(('localhost', 9002))
    s.sendall(command + '\n')
    reply = s.recv(1024)
    s.close()
    return reply.rstrip()


def remote_unpack_index(index_gen, snippet_report, blue_report):
    TIMEOUT = 600
    report_type = 'marketsearch3'
    if blue_report:
        report_type = 'marketsearchblue'
    elif snippet_report:
        report_type = 'marketsearchsnippet'
    reply = backctld_command('{0} unpack_reload {1} {2}'.format(
        report_type, index_gen, TIMEOUT))
    if reply != 'ok':
        raise Exception('backctld returned error: {0}'.format(reply))
    deadline = time.time() + TIMEOUT
    while time.time() < deadline:
        time.sleep(1)
        check_result = backctld_command('{0} check'.format(report_type))
        if check_result == '! in progress':
            continue
        if check_result == 'ok':
            return
        raise Exception('backctld returned error: {0}'.format(check_result))
    raise Exception('Timed out waiting for index to unpack')


def main():
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('index_gen')
    arg_parser.add_argument('--snippet-report', action='store_true')
    arg_parser.add_argument(
        '--blue', action='store_true', help='Unpack blue index (will disable --snippet-report option)')
    args = arg_parser.parse_args()
    remote_unpack_index(args.index_gen, args.snippet_report, args.blue)


if __name__ == "__main__":
    main()
