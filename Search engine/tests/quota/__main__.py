# coding: utf-8

'''
Rpslimiter tester entrypoint.
'''

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

import argparse
import atexit
import json
import logging
import os
import requests
import subprocess
import sys
import tempfile
import time
import yaml

from collections import OrderedDict
from google.protobuf import text_format
from random import Random
from search.martylib.core.date_utils import now, set_timezone
from search.resonance.pylib.loadgen import LoadgenClientLocal

from search.priemka.rpslimiter.tests.quota.rpslimiter_config import generate_rpslimiter_configs
from search.priemka.rpslimiter.tests.quota.serval_config import generate_serval_config
from search.priemka.rpslimiter.tests.quota.stat_builder import build_stat, print_stat_result


LOGGER = logging.getLogger('rpslimiter.tester')
LOCALHOST = '127.0.0.1'
RANDOM = Random()


def configure_loggers():
    formatter = logging.Formatter('[%(levelname)s %(name)s %(asctime)s] %(message)s')
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(formatter)
    logging.basicConfig(level=logging.INFO, handlers=(handler,))


def parse_args():
    parser = argparse.ArgumentParser('Resonance tester')
    parser.add_argument('-c', '--config', required=True, dest='config_path', help='Tests config path')
    parser.add_argument(
        '-t', '--time',
        required=True, type=float, dest='test_case_time', help='Test case work time (s)',
    )

    parser.add_argument('--serval', required=True, dest='serval_path', help='Serval binary path')
    parser.add_argument(
        '--s-config-path', required=False, default=tempfile.mktemp(),
        dest='serval_config_path', help='Save serval config to path',
    )
    parser.add_argument(
        '--s-threads', required=False, type=int, default=4, dest='serval_threads', help='Serval threads',
    )
    parser.add_argument(
        '--s-max-rps', required=False, type=int, default=100000, dest='serval_max_rps', help='Serval max rps',
    )
    parser.add_argument(
        '--s-weight', required=False, default='1,1', dest='serval_weight_range', help='Serval weight uniform range',
    )
    parser.add_argument(
        '--s-reload-time',
        required=False, type=float, default=1.0, dest='serval_reload_time', help='Serval reload interval (s)',
    )

    parser.add_argument('--loadgen', required=True, dest='loadgen_path', help='Loadgen binary path')

    parser.add_argument('--rpslimiter', required=True, dest='rpslimiter_path', help='RpsLimiter binary path')
    parser.add_argument(
        '--r-count', required=False, default=1, type=int, dest='rpslimiter_count', help='Backends count',
    )
    parser.add_argument(
        '--r-threads', required=False, default=3, type=int, dest='rpslimiter_threads', help='Backend threads count',
    )

    parser.add_argument(
        '--r-sync-interval', required=False, default=25, type=int,
        dest='rpslimiter_sync_interval', help='Gossip sync interval (ms)',
    )
    parser.add_argument(
        '--r-sync-parallel', required=False, default=3, type=int,
        dest='rpslimiter_sync_parallel', help='Gossip sync parallel requests',
    )

    parser.add_argument(
        '--r-records-path', required=False, default=tempfile.mktemp(),
        dest='rpslimiter_records_path', help='Save records config to path',
    )
    parser.add_argument(
        '--r-quotas-path', required=False, default=tempfile.mktemp(),
        dest='rpslimiter_quotas_path', help='Save quotas config to path',
    )
    parser.add_argument(
        '--r-config-dir', required=False, default=tempfile.mkdtemp(),
        dest='rpslimiter_config_dir', help='Save rpslimiter configs to directory',
    )
    parser.add_argument('-o', required=False, dest='output_file', help='Output yaml file')
    return parser.parse_args()


def prepare_records(config):
    return {
        'objects': [
            {
                'content': {
                    'predicate': {
                        'conditions': [
                            {
                                'type': 'CGI',
                                'value': 'quotaId={}'.format(i)
                            }
                        ]
                    }
                },
                'quotaKey': 'quota-{}'.format(i)
            }
            for i, quota in enumerate(config)
        ]
    }


def prepare_quotas(config):
    return {
        'objects': [
            {
                'key': 'quota-{}'.format(i),
                'content': {
                    'maxRps': int(quota['quota'])
                }
            }
            for i, quota in enumerate(config)
        ]
    }


def prepare_tasks(config, serval_port):
    tasks = []
    for i, quota in enumerate(config):
        quota_tasks = []
        for rate in quota['rates']:
            config = LoadgenClientLocal.Config()
            config.rps = int(rate * quota['quota'])
            config.connections = max(1, int(config.rps / 10))
            config.threads = max(1, int(config.rps / 10000))
            config.host = 'http://{}:{}'.format(LOCALHOST, serval_port)
            config.paths = ('/quota.acquire?quotaId={}'.format(i),)
            quota_tasks.append((i, rate, config))
        tasks.append(quota_tasks)
    return tasks


def generate_ports(count):
    result = ()
    while len(result) != count:
        result = set(RANDOM.randint(20000, 30000) for _ in range(count))
    return tuple(result)


def stop_all(serval_process, rpslimiter_processes, loadgens):
    serval_process.kill()
    for rpslimiter_process in rpslimiter_processes:
        rpslimiter_process.kill()
    for loadgen in loadgens:
        try:
            loadgen.stop()
        except:
            pass


def wait_consistency(serval_port, rpslimiter_ports, config):
    all_ports = (serval_port,) + tuple(rpslimiter_ports)
    for i in range(10):
        passed = True
        for port in all_ports:
            try:
                unistat = json.loads(requests.get('http://{}:{}/unistat'.format(LOCALHOST, port)).content)
                for key, value in unistat:
                    if key.endswith('RecordsSize_ahhh') and value != len(config):
                        passed = False
                        LOGGER.warning('invalid records size: %s != %s', len(config), value)
                        break
                    if key.endswith('AvailableBackends_ahhh') and value != len(rpslimiter_ports):
                        passed = False
                        LOGGER.warning('invalid gossip state: %s != %s', len(rpslimiter_ports), value)
                        break
            except:
                passed = False
            if not passed:
                break

        if passed:
            return True
        else:
            LOGGER.warning('consistency check not passed')
            time.sleep(i + 1)

    return False


def build_serval_config(args, serval_port, serval_admin_port, rpslimiter_ports):
    serval_weight_range = tuple(float(x) for x in args.serval_weight_range.split(','))
    serval_config = generate_serval_config(
        LOCALHOST,
        serval_port,
        serval_admin_port,
        rpslimiter_ports,
        args.serval_threads,
        serval_weight_range,
    )
    with open(args.serval_config_path, 'w') as f:
        yaml.dump(serval_config, f)


def rebuild_serval_config(args, serval_port, serval_admin_port, rpslimiter_ports):
    build_serval_config(args, serval_port, serval_admin_port, rpslimiter_ports)
    requests.get('http://{}:{}/reload'.format(LOCALHOST, serval_admin_port))


def build_flatten_config(config):
    result = []
    for quota in config:
        for rate in quota['rates']:
            result.append({
                'quota': quota['quota'],
                'rates': [rate]
            })
    result.sort(key=lambda x: x['quota'] * x['rates'][0])
    return result


def main():
    args = parse_args()
    with open(args.config_path) as f:
        config = build_flatten_config(yaml.load(f))
    records = prepare_records(config)
    quotas = prepare_quotas(config)

    ports = generate_ports(args.rpslimiter_count + 2)
    serval_admin_port = ports[0]
    serval_port = ports[1]
    rpslimiter_ports = ports[2:]

    rpslimiter_configs = generate_rpslimiter_configs(
        LOCALHOST,
        rpslimiter_ports,
        args.rpslimiter_records_path,
        args.rpslimiter_quotas_path,
        args.rpslimiter_threads,
        args.rpslimiter_sync_interval,
        args.rpslimiter_sync_parallel,
    )
    build_serval_config(args, serval_port, serval_admin_port, rpslimiter_ports)
    LOGGER.info('serval config saved to %s', args.serval_config_path)
    tasks = prepare_tasks(config, serval_port)

    with open(args.rpslimiter_records_path, 'w') as f:
        json.dump(records, f)
    with open(args.rpslimiter_quotas_path, 'w') as f:
        json.dump(quotas, f)

    for i, rpslimiter_config in enumerate(rpslimiter_configs):
        rpslimiter_config_path = os.path.join(args.rpslimiter_config_dir, '{}.config'.format(rpslimiter_ports[i]))
        with open(rpslimiter_config_path, 'w') as f:
            text_format.PrintMessage(rpslimiter_config, f)

    LOGGER.info('ok, ready to start')

    serval_process = subprocess.Popen((args.serval_path, '-c', args.serval_config_path))
    rpslimiter_processes = tuple(
        subprocess.Popen((
            args.rpslimiter_path,
            '-c', os.path.join(args.rpslimiter_config_dir, '{}.config'.format(port)),
            '--host-id', str(i),
        ))
        for i, port in enumerate(rpslimiter_ports)
    )
    all_loadgens = []
    atexit.register(lambda: stop_all(serval_process, rpslimiter_processes, all_loadgens))

    LOGGER.info('all processes started, wait consistency')
    if not wait_consistency(serval_port, rpslimiter_ports, config):
        LOGGER.error('rpslimiter inconsistent, exit')
        exit(1)

    LOGGER.info('serval port=%s serval admin=%s', serval_port, serval_admin_port)

    response = []
    while sum(map(len, tasks)) > 0:
        batch = []
        batch_rps = 0
        for quota_tasks in tasks:
            if not quota_tasks:
                continue
            task = quota_tasks[0]
            if not batch or batch_rps + task[-1].rps <= args.serval_max_rps:
                batch.append(task)
                batch_rps += task[-1].rps
                quota_tasks.pop(0)
        LOGGER.info('process %s items with summary rps %s', len(batch), batch_rps)
        loadgens = [
            LoadgenClientLocal(args.loadgen_path)
            for _ in batch
        ]
        all_loadgens += loadgens
        for i, (quota_id, rate, loadgen_config) in enumerate(batch):
            loadgens[i].start(loadgen_config)
            LOGGER.info('loadgen log path: %s', loadgens[i].log_path)

        start_time = now().timestamp()
        while now().timestamp() - start_time < args.test_case_time:
            reload_interval = min(args.serval_reload_time, args.test_case_time - now().timestamp() + start_time)
            if reload_interval < 0:
                break
            time.sleep(reload_interval)
            rebuild_serval_config(args, serval_port, serval_admin_port, rpslimiter_ports)

        for loadgen in loadgens:
            loadgen.stop()
        LOGGER.info('process batch complete, process results')

        for i, (quota_id, rate, loadgen_config) in enumerate(batch):
            task_result = build_stat(loadgens[i], config[quota_id]['quota'], loadgen_config.rps)
            response.append(task_result)
            print_stat_result(LOGGER, task_result)
    LOGGER.info('all complete, save results')
    if args.output_file:
        with open(args.output_file, 'w') as f:
            json.dump(response, f)
    for task_result in response:
        print_stat_result(LOGGER, task_result)


if __name__ == '__main__':
    set_timezone('Europe/Moscow')
    configure_loggers()
    yaml.add_representer(OrderedDict, lambda self, data: self.represent_mapping('tag:yaml.org,2002:map', data.items()))
    main()
