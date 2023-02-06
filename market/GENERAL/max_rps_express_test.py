#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import functools
import httplib
import json
import logging
import os
import re
import shutil
import subprocess
import tempfile


def setup_logging():
    class ColoredFormatter(logging.Formatter):
        RED = '\033[1;31m'
        YELLOW = '\033[1;33m'
        RESET = '\033[0;0m'

        def __init__(self, fmt):
            logging.Formatter.__init__(self, fmt)

        def format(self, record):
            if record.levelname == logging.getLevelName(logging.WARNING):
                color = self.YELLOW
            elif record.levelname == logging.getLevelName(logging.ERROR):
                color = self.RED
            else:
                color = None
            text = logging.Formatter.format(self, record)
            if color:
                text = color + text + self.RESET
            return text

    log_formatter = ColoredFormatter('%(asctime)s [%(levelname)-5.5s]  %(message)s')
    root_logger = logging.getLogger()
    root_logger.setLevel(logging.INFO)

    console_handler = logging.StreamHandler()
    console_handler.setFormatter(log_formatter)
    root_logger.addHandler(console_handler)


def get_nanny_token():
    auth_token = os.environ.get('NANNY_AUTH_TOKEN')
    if auth_token:
        return auth_token
    token_file_path = os.path.expanduser('~/.robot-market-st.tokens')
    if not os.path.isfile(token_file_path):
        return None
    with open(token_file_path, 'r') as token_file:
        tokens = json.loads(token_file.read())
        return tokens.get('nanny')


def send_nanny_api_request(query, auth_token=None):
    headers = dict()
    headers['Accept'] = 'application/json'
    if auth_token:
        headers['Authorization'] = 'OAuth {0}'.format(auth_token)
    conn = httplib.HTTPConnection('nanny.yandex-team.ru')
    conn.request('GET', query, headers=headers)
    try:
        response = conn.getresponse()
        if response.status != 200:
            if response.status == 404:
                return None
            raise Exception('Nanny API returned error: {} {}\n{}'.format(response.status, response.reason, query))
        json_str = response.read()
    finally:
        conn.close()
    return json.loads(json_str)


def get_nanny_group_list(auth_token):
    group_list = list()
    nanny_data = send_nanny_api_request('/v2/services/?exclude_runtime_attrs=1&category=/market/report', auth_token)
    for group_info in nanny_data['result']:
        group_name = group_info['_id']
        group_list.append(group_name)
    return group_list


def parse_nanny_group_name(group_name):
    parts = group_name.split('_')
    if len(parts) < 4 or parts[1] != 'report':
        return None
    env_type = parts[0]
    dc = parts[-1]
    is_snippet = parts[-2] == 'snippet'
    report_type = '_'.join(parts[2:-2 if is_snippet else -1])
    return (env_type, report_type, dc, is_snippet)


def get_host_list(group_name, cluster_index):
    logging.info('Querying Nanny group %s', group_name)
    nanny_data = send_nanny_api_request('/v2/services/{0}/current_state/instances/'.format(group_name))
    if nanny_data is None:
        return list()
    shard_matcher = re.compile(r'^a_shard_(\d+)$')
    host_list = list()
    for host_info in nanny_data['result']:
        shard = None
        for itag in host_info['itags']:
            shard_match = shard_matcher.match(itag)
            if shard_match:
                shard = int(shard_match.group(1))
        if shard is None or shard != cluster_index:
            continue
        host_name = host_info['hostname']
        container_hostname = host_info['container_hostname']
        instance_port = host_info['port']
        logging.info('host name: %s, port: %d', host_name, instance_port)
        host_list.append((host_name, instance_port, container_hostname))
    return host_list


def send_report_request(host, query):
    conn = httplib.HTTPConnection(host)
    conn.request('GET', query)
    try:
        response = conn.getresponse()
        if response.status != 200:
            raise Exception('Report returned error: {} {}\n{}'.format(response.status, response.reason, query))
        return response.read()
    finally:
        conn.close()


def check_for_lockdown(host_list):
    for host_info in host_list:
        host_port = '{}:{}'.format(host_info.container_hostname, host_info.instance_port + 1)
        report_status = send_report_request(host_port, '/yandsearch?place=report_status')
        if 'LOCKDOWN' not in report_status:
            raise Exception('Report is not in lockdown: {} says {}'.format(host_port, report_status))


class HostInfo(object):
    def __init__(self, host_name, instance_port, is_snippet, container_hostname):
        self.host_name = host_name
        self.instance_port = instance_port
        self.is_snippet = is_snippet
        self.container_hostname = container_hostname


class TempDir(object):

    def __init__(self):
        self.temp_dir = None

    def __enter__(self):
        self.temp_dir = tempfile.mkdtemp()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        shutil.rmtree(self.temp_dir)

    @property
    def name(self):
        return self.temp_dir


BAD_REQUEST_LIMIT = 0.01
RPS_STEP_MULTIPLIER = 2.0
MIN_RPS_DIFF_PERCENT = 0.01


def perform_test(rps, host_list, ammo, max_timing, request_count):
    logging.info('Testing rps: %f', rps)

    shiny_tank_bin = os.path.join(os.path.dirname(__file__), 'shiny_tank')
    if not os.path.isfile(shiny_tank_bin):
        raise Exception('Not found: {}'.format(shiny_tank_bin))

    with TempDir() as temp_dir:
        output_file_path = os.path.join(temp_dir.name, 'output')

        shiny_tank_command = [
            shiny_tank_bin,
            '--input', ammo,
            '--output', output_file_path,
            '--rps', str(rps),
            '--request-count', str(request_count),
            '--precache-connections',
            '--warmup',
        ]
        for host_info in host_list:
            shiny_tank_command.extend([
                '--target', '{}:{}'.format(host_info.container_hostname, host_info.instance_port + 1)
            ])

        subprocess.check_call(shiny_tank_command)

        with open(output_file_path, 'r') as output_file:
            output_data = json.load(output_file)
        bad_count = 0
        for result in output_data['results']:
            if result['is_skipped']:
                continue
            if result['http_code'] != 200:
                bad_count += 1
            elif result['system_code'] != 0:
                bad_count += 1
            elif result['time'] > max_timing * 1000.0:
                bad_count += 1
        bad_request_ratio = float(bad_count) / len(output_data['results'])
        logging.info('Bad request ratio: %f', bad_request_ratio)
        return bad_request_ratio <= BAD_REQUEST_LIMIT


def main():
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('--cluster-id', required=True, help='Pinger-like cluster ID e.g. prod@shadow@sas@00')
    arg_parser.add_argument('--ammo', required=True, help='Ammo file path')
    arg_parser.add_argument('--start-rps', required=True, type=int, help='Start RPS')
    arg_parser.add_argument('--duration', required=True, type=int, help='Test duration for starting RPS')
    arg_parser.add_argument('--max-timing', required=True, type=int, help='Timing limit (ms)')
    args = arg_parser.parse_args()

    setup_logging()

    try:
        auth_token = get_nanny_token()
        if not auth_token:
            raise Exception('Get .robot-market-st.tokens with \'nanny\' token in it or set NANNY_AUTH_TOKEN environment variable.')

        if not os.path.isfile(args.ammo):
            raise Exception('Ammo file not found: {}'.format(args.ammo))

        host_list = list()
        cluster_env_type, cluster_report_type, cluster_dc, cluster_index = args.cluster_id.replace('-', '_').split('@')
        cluster_index = int(cluster_index)
        for group_name in get_nanny_group_list(auth_token):
            if not parse_nanny_group_name(group_name):
                continue
            env_type, report_type, dc, is_snippet = parse_nanny_group_name(group_name)
            if env_type != cluster_env_type:
                continue
            if report_type != cluster_report_type:
                continue
            if dc != cluster_dc:
                continue
            for host_name, instance_port, container_hostname in get_host_list(group_name, cluster_index):
                if not is_snippet:
                    host_list.append(HostInfo(host_name, instance_port, is_snippet, container_hostname))
        if not host_list:
            raise Exception('Ho hosts found for cluster {}'.format(args.cluster_id))

        check_for_lockdown(host_list)

        max_rps_test = functools.partial(
            perform_test,
            host_list=host_list,
            ammo=args.ammo,
            max_timing=args.max_timing,
            request_count=int(args.duration * args.start_rps * len(host_list))
        )
        logging.info('Looking for max rps')
        min_rps = 0.0
        max_rps = float(args.start_rps)
        # Два теста т.к. на повышенном РПС нужен прогрев
        while max_rps_test(max_rps) or max_rps_test(max_rps):
            min_rps = max_rps
            max_rps = max_rps * RPS_STEP_MULTIPLIER

        if not min_rps:
            logging.info('Looking for min rps')
            min_rps = float(args.start_rps) / RPS_STEP_MULTIPLIER
            while not max_rps_test(min_rps):
                max_rps = min_rps
                min_rps = min_rps / RPS_STEP_MULTIPLIER
                if min_rps < float(args.start_rps) / 2:
                    logging.error('Could not find min rps')
                    min_rps = 0.0
                    break

        if min_rps:
            logging.info('Performing binary search')
            while max_rps - min_rps > MIN_RPS_DIFF_PERCENT * min_rps:
                logging.info('Current RPS range is [ %f .. %f ]', min_rps, max_rps)
                test_rps = (min_rps + max_rps) / 2.0
                if max_rps_test(test_rps):
                    min_rps = test_rps
                else:
                    max_rps = test_rps

            # Пытаемся увеличить РПС на 1% пока не упремся в предел
            logging.info('Trying to increase RPS')
            while True:
                test_rps = min_rps + min_rps * MIN_RPS_DIFF_PERCENT
                if not max_rps_test(test_rps):
                    break
                min_rps = test_rps

        logging.info('Max RPS = %f', min_rps)

    except Exception as e:
        logging.exception(e)


if __name__ == '__main__':
    main()
