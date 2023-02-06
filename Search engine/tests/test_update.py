import copy
import os
import re
import time
import requests

import yatest.common as ytest

from yatest.common import network
from pathlib import Path
from contextlib import contextmanager, ExitStack

from search.rpslimiter.storage.proto.structures.root_pb2 import CompiledConfig
from search.rpslimiter.storage.proto.structures.agent_pb2 import (
    ServalConfiguration,
    BalancerConfiguration,
)

from search.rpslimiter.tools.rpslimiter_config_compiler.lib.config import read_directory
from search.rpslimiter.rpslimiter_agent.lib.update import (
    ServalUpdate,
    BalancerUpdate,
    PeerSets,
    PeerSet,
    Peer
)

_TEST_DATA = 'search/rpslimiter/test_data'
_BALANCER = 'balancer/daemons/balancer/balancer'
_SERVAL = 'search/rpslimiter/rpslimiter/rpslimiter'

_CURRENT_PEER = Peer(fqdn='self', ip6_address='::1')
_REMOTE_PEER_A = Peer(fqdn='peer-a', ip6_address='::2')
_REMOTE_PEER_B = Peer(fqdn='peer-b', ip6_address='::3')
_PEER_SET_A = PeerSet(
    cluster_name='TST-A',
    endpoint_set_id='test-a',
    peers=[_CURRENT_PEER, _REMOTE_PEER_A]
)
_PEER_SET_B = PeerSet(
    cluster_name='TST-B',
    endpoint_set_id='test-b',
    peers=[_REMOTE_PEER_B]
)
_PEER_SETS = PeerSets(
    peer_sets=[
        _PEER_SET_A,
        _PEER_SET_B
    ],
    current=_CURRENT_PEER
)
_MUTATED_PEER_SETS = PeerSets(
    peer_sets=[
        PeerSet(
            cluster_name=_PEER_SET_A.cluster_name,
            endpoint_set_id=_PEER_SET_A.endpoint_set_id,
            peers=reversed(_PEER_SET_A.peers)
        ),
        _PEER_SET_B
    ],
    current=_CURRENT_PEER
)


def _patch_port(url, port):
    return re.sub('localhost:\\d+/', f'localhost:{port}/', url)


def _patch_host_port(url, port):
    return re.sub('localhost:\\d+/', f'[::1]:{port}/', url)


@contextmanager
def _patch_hostname(hst):
    old_hostname = os.environ.get('HOSTNAME')
    os.environ['HOSTNAME'] = hst
    yield
    if old_hostname is None:
        del os.environ['HOSTNAME']
    else:
        os.environ['HOSTNAME'] = old_hostname


def _check_rpslimiter_works(rpslimiter, listening_port: int, quota: str):
    assert rpslimiter.running
    requests.get(f'http://[::1]:{listening_port}/ping').raise_for_status()
    requests.post(
        f'http://[::1]:{listening_port}/quota.acquire',
        data=f'GET / HTTP/1.1\r\nx-rpslimiter-balancer: {quota}\r\n\r\n'
    ).raise_for_status()


def _check_balancer_update_eq(
        update: BalancerUpdate,
        compiled_cfg: CompiledConfig,
        balancer_cfg: BalancerConfiguration
):
    assert update == BalancerUpdate(
        root=CompiledConfig(
            router=compiled_cfg.router + ' ',
            quotas=compiled_cfg.quotas + ' ',
            lua_config=compiled_cfg.lua_config
        ),
        peer_sets=copy.deepcopy(_PEER_SETS),
        config=balancer_cfg)

    assert update != BalancerUpdate(
        root=CompiledConfig(
            router=compiled_cfg.router,
            quotas=compiled_cfg.quotas,
            lua_config=compiled_cfg.lua_config + ' '
        ),
        peer_sets=_PEER_SETS,
        config=balancer_cfg)

    assert update != BalancerUpdate(
        root=compiled_cfg,
        peer_sets=_MUTATED_PEER_SETS,
        config=balancer_cfg)


def _check_serval_update_eq(
        update: ServalUpdate,
        compiled_cfg: CompiledConfig,
        serval_cfg: ServalConfiguration
):
    assert update == ServalUpdate(
        root=CompiledConfig(
            router=compiled_cfg.router,
            quotas=compiled_cfg.quotas,
            lua_config=compiled_cfg.lua_config + ' '
        ),
        peer_sets=copy.deepcopy(_PEER_SETS),
        config=serval_cfg)

    assert update != ServalUpdate(
        root=CompiledConfig(
            router=compiled_cfg.router + ' ',
            quotas=compiled_cfg.quotas,
            lua_config=compiled_cfg.lua_config
        ),
        peer_sets=_PEER_SETS,
        config=serval_cfg)

    assert update != ServalUpdate(
        root=CompiledConfig(
            router=compiled_cfg.router,
            quotas=compiled_cfg.quotas + ' ',
            lua_config=compiled_cfg.lua_config
        ),
        peer_sets=_PEER_SETS,
        config=serval_cfg)

    assert update != ServalUpdate(
        root=compiled_cfg,
        peer_sets=_MUTATED_PEER_SETS,
        config=serval_cfg
    )


def test_balancer_update():
    os.chdir(ytest.test_output_path('.'))
    os.symlink(src=ytest.binary_path(_BALANCER), dst='./rpslimiter')
    compiled_cfg = read_directory(
        ytest.source_path(f'{_TEST_DATA}/main/balancer'),
        mode='balancer'
    ).compile()

    with network.PortManager() as pm:
        listening_port = pm.get_tcp_port(8090)
        admin_port = pm.get_tcp_port(9100)
        unistat_port = pm.get_tcp_port(9102)

        balancer = ytest.execute([
            ytest.binary_path(_BALANCER),
            '-V', f'listening_port={listening_port}',
            '-V', f'admin_port={admin_port}',
            '-V', f'unistat_port={unistat_port}',
            '-V', 'workers=1',
            '-V', 'worker_maxconn=500',
            '-V', 'error_log=./error_log',
            '-V', 'access_log=./access_log',
            '-V', 'child_log=./child_log',
            ytest.test_source_path('./data/dummy.lua')
        ], stdout='./balancer_stdout', stderr='./balancer_stderr', wait=False)

        time.sleep(1)
        _check_rpslimiter_works(balancer, listening_port=listening_port, quota='dummy')

        balancer_cfg = BalancerConfiguration()
        balancer_cfg.test_port = pm.get_tcp_port(balancer_cfg.test_port)
        balancer_cfg.test_admin_port = pm.get_tcp_port(balancer_cfg.test_admin_port)
        balancer_cfg.test_unistat_port = pm.get_tcp_port(balancer_cfg.test_unistat_port)
        balancer_cfg.reload_url = _patch_port(balancer_cfg.reload_url, admin_port)
        balancer_cfg.ping_url = _patch_host_port(balancer_cfg.ping_url, balancer_cfg.test_port)
        balancer_cfg.shutdown_url = _patch_port(balancer_cfg.shutdown_url, balancer_cfg.test_admin_port)

        update = BalancerUpdate(root=compiled_cfg, peer_sets=_PEER_SETS, config=balancer_cfg)
        update.apply()

        _check_balancer_update_eq(update, compiled_cfg, balancer_cfg)

        balancer_lua = Path('./balancer.lua').read_text()
        assert _REMOTE_PEER_A.ip6_address in balancer_lua
        assert _REMOTE_PEER_B.ip6_address in balancer_lua

        time.sleep(1)
        _check_rpslimiter_works(balancer, listening_port=listening_port, quota='ping')

        balancer.kill()
        balancer.wait(check_exit_code=False)


def test_serval_update():
    os.chdir(ytest.test_output_path('.'))
    os.symlink(src=ytest.binary_path(_SERVAL), dst='./rpslimiter')
    os.symlink(src=ytest.test_source_path('./data/rpslimiter.yaml'), dst='./rpslimiter.yaml')
    os.symlink(src=ytest.test_source_path('./data/quotas.yaml'), dst='./quotas.yaml')
    os.symlink(src=ytest.test_source_path('./data/router.yaml'), dst='./router.yaml')
    os.symlink(src=ytest.test_source_path('./data/remote.yaml'), dst='./remote.yaml')

    compiled_cfg = read_directory(
        ytest.source_path(f'{_TEST_DATA}/main/serval'),
        mode='serval'
    ).compile()

    with ExitStack() as st:
        pm = st.enter_context(network.PortManager())
        st.enter_context(_patch_hostname('self'))

        listening_port = pm.get_tcp_port(8090)
        admin_port = pm.get_tcp_port(9100)

        env = os.environ.copy()
        env.update(**{
            'PORT': str(listening_port),
            'ADMIN_PORT': str(admin_port),
        })
        serval = ytest.execute([
            ytest.binary_path(_SERVAL),
            '-c', './rpslimiter.yaml',
            '-l', './rpslimiter_log'
        ], env=env, stdout='./serval_stdout', stderr='./serval_stderr', wait=False)

        time.sleep(1)
        _check_rpslimiter_works(serval, listening_port=listening_port, quota='dummy')

        serval_cfg = ServalConfiguration()
        serval_cfg.test_port = pm.get_tcp_port(serval_cfg.test_port)
        serval_cfg.test_admin_port = pm.get_tcp_port(serval_cfg.test_admin_port)
        serval_cfg.reload_url = _patch_host_port(serval_cfg.reload_url, admin_port)
        serval_cfg.ping_url = _patch_host_port(serval_cfg.ping_url, serval_cfg.test_port)

        update = ServalUpdate(root=compiled_cfg, peer_sets=_PEER_SETS, config=serval_cfg)
        update.apply()

        _check_serval_update_eq(update, compiled_cfg, serval_cfg)

        assert Path('./quotas.yaml').read_text() == compiled_cfg.quotas
        assert Path('./router.yaml').read_text() == compiled_cfg.router
        remotes = Path('./remote.yaml').read_text()
        assert _REMOTE_PEER_A.fqdn in remotes
        assert _REMOTE_PEER_B.fqdn in remotes

        time.sleep(1)
        _check_rpslimiter_works(serval, listening_port=listening_port, quota='ping')

        serval.kill()
        serval.wait(check_exit_code=False)
