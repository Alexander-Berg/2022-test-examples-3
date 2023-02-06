#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import fcntl
import httplib
import json
import multiprocessing
import os
import re
import signal
import struct
import sys
import termios

escape_code_re = re.compile('\033' + r'\[\d+;\d+m')


def get_terminal_width():
    return struct.unpack('HHHH', fcntl.ioctl(sys.stdout.fileno(), termios.TIOCGWINSZ, struct.pack('HHHH', 0, 0, 0, 0)))[1]


def red_text(text):
    RED = '\033[1;31m'
    RESET = '\033[0;0m'
    return RED + text + RESET


def strip_escape_codes(text):
    return escape_code_re.sub('', text)


def display_progress(ok_count, fail_count, total_count):
    PROGRESS_BAR_CHAR = '█'
    PROGRESS_BG_CHAR = '░'
    percent_done = 100 * (ok_count + fail_count) // total_count
    fail_count_text = '{:>4}'.format(fail_count)
    if fail_count:
        fail_count_text = red_text(fail_count_text)
    header = '{:>3}% Ok: {:>4} Err: {} '.format(percent_done, ok_count, fail_count_text)
    term_width = get_terminal_width()
    bar_len = max(term_width - len(strip_escape_codes(header)) - 5, 0)
    filled_bar_len = bar_len * percent_done // 100
    empty_bar_len = bar_len - filled_bar_len
    progress_str = header + PROGRESS_BAR_CHAR * filled_bar_len + PROGRESS_BG_CHAR * empty_bar_len + '\r'
    sys.stdout.write(progress_str)
    sys.stdout.flush()


def send_http_query(host, port, query):
    conn = httplib.HTTPConnection(host=host, port=port, strict=True, timeout=30)
    conn.request('GET', query)
    try:
        return (conn.getresponse().read(), None)
    except Exception:
        return (None, sys.exc_info()[0])
    finally:
        conn.close()


class SendQuery(object):

    def __init__(self, host, port):
        self.host = host
        self.port = port

    def __call__(self, query_and_index):
        query, index = query_and_index
        response, error_msg = send_http_query(self.host, self.port, query)
        return index, response, error_msg


class GetReproducibleResponse(object):

    def __init__(self, host, port):
        self.host = host
        self.port = port

    def __call__(self, query_and_index):
        query, index = query_and_index
        responses = list()
        while True:
            response, error_msg = send_http_query(self.host, self.port, query)
            if error_msg:
                break
            if response:
                response = prepare_response_for_comparison(response)
                if response in responses:
                    break
                elif len(responses) == 2:
                    return index, None, 'Response stays different after 3 tries: {}'.format(query)
                else:
                    responses.append(response)
        return index, response, error_msg


def process_json_obj(json_obj):
    if isinstance(json_obj, dict):
        if 'debug' in json_obj:
            del json_obj['debug']
        for key in ('cpc', 'showUid'):
            if key in json_obj:
                json_obj[key] = '0' * len(json_obj[key])
        if 'urls' in json_obj:
            for key in json_obj['urls']:
                if key != 'direct':
                    json_obj['urls'][key] = ''
        for key in json_obj:
            process_json_obj(json_obj[key])
    elif isinstance(json_obj, list):
        for item in json_obj:
            process_json_obj(item)


def prepare_response_for_comparison(response_str):
    try:
        json_obj = json.loads(response_str)
        process_json_obj(json_obj)
        return json.dumps(json_obj, indent=4, sort_keys=True, ensure_ascii=False).encode('utf8')
    except Exception:
        return response_str


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('file_path', help='Path to file with query list')
    parser.add_argument('--host', default='localhost', help='Target host')
    parser.add_argument('--port', '-p', type=int, default=17051, help='Target port')
    parser.add_argument('--threads', '-t', type=int, default=1, help='Number of threads to use')
    parser.add_argument('--repeat', '-r', type=int, default=1, help='Repetition count')
    parser.add_argument('--save-dir', help='Directory for saving responses')
    args = parser.parse_args()

    if args.save_dir and not os.path.isdir(args.save_dir):
        os.makedirs(args.save_dir)

    queries = []
    index = 0
    with open(args.file_path, "r") as f:
        for line in f:
            query = line.strip()
            queries.append((query, index))
            index += 1
    error_stats = dict()
    ok_count = 0
    fail_cout = 0

    def initializer():
        signal.signal(signal.SIGINT, signal.SIG_IGN)
    pool = multiprocessing.Pool(processes=args.threads, initializer=initializer)
    try:
        for _ in xrange(args.repeat):
            requester = GetReproducibleResponse if args.save_dir else SendQuery
            for index, response, error_msg in pool.imap_unordered(requester(args.host, args.port), queries):
                if error_msg is not None:
                    fail_cout += 1
                    error_stats[error_msg] = error_stats.get(error_msg, 0) + 1
                else:
                    ok_count += 1
                    if args.save_dir:
                        with open(os.path.join(args.save_dir, '{:05}'.format(index)), 'w') as f:
                            f.write(response)
                display_progress(ok_count, fail_cout, len(queries) * args.repeat)
    except:
        pool.terminate()
        raise
    else:
        pool.close()
    finally:
        pool.join()
        print

    if error_stats:
        for err_msg, err_count in error_stats.iteritems():
            print '{:5}: {}'.format(err_count, err_msg)


if __name__ == '__main__':
    main()
