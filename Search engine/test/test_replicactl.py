import os
import time
import jinja2
import pytest
import yatest
import logging
import requests
import yp.client
import yt.wrapper as yt
from pathlib import Path

import scenario

import search.plutonium.deploy.proto.sources_pb2 as sources_pb2


log = logging.getLogger('test_replicactl')

REPLICACTL_PATH = 'search/plutonium/deploy/replicactl/bin/replicactl'


class ReplicaCtlDaemon:
    def __init__(self):
        self._exe = yatest.common.binary_path(REPLICACTL_PATH)

    def gen_config(self, ctx):
        self._port_manager = yatest.common.network.PortManager()
        self._port = self._port_manager.get_port(9681)
        ctx['replicactl']['monitoring_port'] = self._port

        self._workdir = Path(yatest.common.output_path('replicactl'))
        os.makedirs(self._workdir, exist_ok=True)

        self._log_config = self._workdir / 'replicactl.log.conf'
        template = yatest.common.test_source_path('data/log.conf.j2')
        with open(self._log_config, 'w') as f:
            f.write(jinja2.Template(open(template).read()).render({
                'dir': self._workdir / 'logs',
            }))

        self._config = self._workdir / 'replicactl.conf'
        template = yatest.common.test_source_path('data/config.j2')
        with open(self._config, 'w') as f:
            ctx['replicactl']['log_config'] = self._log_config
            f.write(jinja2.Template(open(template).read()).render(ctx))

    def start(self):
        self._process = yatest.common.execute([self._exe, '-c', self._config], wait=False)

    def stop(self):
        if self._process.running:
            try:
                self.get('shutdown?timeout=10s')
            except Exception as e:
                assert not self._process.running, f"Failed to shutdown process: {e}"
        self._process.wait(timeout=5)

    def get(self, path, timeout=None):
        res = requests.get(f'http://localhost:{self._port}/{path}', timeout=timeout)
        res.raise_for_status()
        return res

    def metrics(self):
        raw = self.get('unistat').json()
        return {metric[0] : metric[1] for metric in raw}


@scenario.action('replicactl')
def prepare(ctx, payload):
    ctx['daemon'] = ReplicaCtlDaemon()
    ctx.daemon.gen_config(ctx)


@scenario.action('replicactl')
def start(ctx, payload):
    ctx.daemon.start()


@scenario.action('replicactl')
def stop(ctx, payload):
    ctx.daemon.stop()


@scenario.action('replicactl')
def barrier(ctx, payload):
    timeout, iterations, any = payload['timeout'], payload['iterations'], payload.get('any_status', False)

    def num_iterations():
        if any:
            try:
                metrics = ctx.daemon.metrics()
            except:
                metrics = {}
            return metrics.get('module_core_name_succeded_iterations_dmmm', 0) + metrics.get('module_core_name_failed_iterations_dmmm', 0)
        else:
            return ctx.daemon.metrics()['module_core_name_succeded_iterations_dmmm']

    start = num_iterations()
    for _ in range(timeout):
        current = num_iterations()
        if current - start >= iterations:
            return
        time.sleep(1)
    raise Exception(f'Replicactl failed to finish {iterations} iterations in {timeout} seconds')


@scenario.action('replicactl')
def add_targets(ctx, payload):
    targets = payload['targets']
    for target in targets:
        if 'PodId' not in target:
            target['PodId'] = ''
        if 'ResourceSpec' not in target:
            target['ResourceSpec'] = sources_pb2.TSource(
                Static=sources_pb2.TStaticSource(Content=b'Test source')
            ).SerializeToString()
    ctx.yt_client.insert_rows(f'{ctx.yt_root}/replicactl_target', targets)


@scenario.action('replicactl')
def check_status(ctx, payload):
    def parse_status(status):
        return {(row.get('PodId', ''), row['Namespace'], row['LocalPath']) : row for row in status}

    status = parse_status(ctx.yt_client.select_rows(f'* from [{ctx.yt_root}/replicactl_status]'))
    log.info('Found status: %s', status)
    expected = parse_status(payload['expected'])

    for key, lvalue in expected.items():
        assert key in status
        rvalue = status[key]
        if 'Annotation' in lvalue:
            assert lvalue['Annotation'] == rvalue['Annotation']
    for key, value in status.items():
        assert key in expected, f'Unexpected status row {value}'


@scenario.action('yp')
def create_yp_object(ctx, payload):
    ctx.yp_client.create_object(payload['type'], payload['attrs'])


@scenario.action('yp')
def create_pod_set(ctx, payload):
    payload['type'] = 'pod_set'
    create_yp_object(ctx, payload)


@scenario.action('yp')
def create_pod(ctx, payload):
    payload['type'] = 'pod'
    create_yp_object(ctx, payload)


@scenario.action('yp')
def create_pods(ctx, payload):
    for node in payload['nodes']:
        ctx.yp_client.create_object('node', node)
        for resource in ('cpu', 'memory', 'slot'):
            ctx.yp_client.create_object('resource', {
                'meta': {'node_id': node['meta']['id']},
                'spec': {
                    resource: {
                        'total_capacity': 42,
                    }
                }
            })
    for pod in payload['pods']:
        ctx.yp_client.create_object('pod', pod)
    for pod in payload['pods']:
        if 'pod_agent_payload' in pod.get('status', {}).get('agent', {}):
            ctx.yp_client.update_object('pod', pod['meta']['id'], set_updates=[{
                'path': '/status/agent/pod_agent_payload',
                'value': pod['status']['agent']['pod_agent_payload']
            }])


@scenario.action('yp')
def remove_pods(ctx, payload):
    ctx.yp_client.remove_objects((('pod', pod) for pod in payload['pods']))


@scenario.action('yt')
def create_map_node(ctx, payload):
    ctx.yt_client.create(
        'map_node',
        payload['path'],
        payload.get('recursive', True),
        payload.get('ignore_existing', True)
    )


@pytest.fixture
def yp_client():
    address = os.environ['YP_MASTER_GRPC_INSECURE_ADDR']
    os.environ['YP_ADDRESS'] = address
    client = yp.client.YpClient(address, config={'enable_ssl': False})
    return client


def run_scenario(yp_client, name):
    scn = scenario.Scenario(name=name)
    scn.bind('yt_client', yt)
    scn.bind('yp_client', yp_client)
    scn.bind('yp_address', os.environ['YP_ADDRESS'])
    scn.run()


def test_simple(yp_client):
    run_scenario(yp_client, 'simple')


def test_remove_pods(yp_client):
    run_scenario(yp_client, 'remove_pods')
