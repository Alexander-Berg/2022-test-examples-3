# coding: utf-8

import pytest
import uuid
import yt.wrapper as yt
from market.idx.datacamp.picrobot.py_lib.state import encode_state
from market.pylibrary.proto_utils import message_from_data
from market.idx.yatf.resources.resource import FileGeneratorResource
from market.idx.yatf.resources.yt_tables.picrobot_state import PicrobotStateTable
from market.idx.datacamp.picrobot.processor.proto.state_pb2 import TPicrobotState
from market.idx.datacamp.picrobot.http.yatf.fake_avatars import AvatarsHttpServer
from market.idx.datacamp.picrobot.http.yatf.picrobot_http import make_picrobot_http
from yatest.common.network import PortManager


class PicrobotHttpConfigMock(FileGeneratorResource):
    def __init__(self, **kwargs):
        super(PicrobotHttpConfigMock, self).__init__('picrobot_http_config.cfg', **kwargs)


@pytest.fixture(scope='module')
def config():
    cfg = PicrobotHttpConfigMock()
    cfg.tvm_client_id = 0
    cfg.yt_home = yt.ypath_join('//home/test_datacamp', str(uuid.uuid4()))
    cfg.state_path = yt.ypath_join(cfg.yt_home, 'state')
    return cfg


ALL_NAMESPACES = ['marketpic', 'mrkt_idx_direct', 'yabs_performance']
PICROBOT_STATE_DATA = [
    encode_state('https://rt.tr/beshbarmak.jpg', message_from_data({
        'MdsInfo': [
            {
                'MdsId': {
                    'Namespace': ns,
                    'GroupId': 1,
                    'ImageName': 'beshbarmak'
                }
            } for ns in ALL_NAMESPACES
        ]
    }, TPicrobotState()), 'zstd_6'),
    encode_state('https://rt.tr/kazylyk.jpg', message_from_data({
        'MdsInfo': [
            {
                'MdsId': {
                    'Namespace': ns,
                    'GroupId': 1,
                    'ImageName': 'kazylyk'
                }
            } for ns in ALL_NAMESPACES
        ]
    }, TPicrobotState()), 'zstd_6'),
    encode_state('https://rt.tr/hamburger.jpg', message_from_data({
        'MdsInfo': [
            {
                'MdsId': {
                    'Namespace': ns,
                    'GroupId': 1,
                    'ImageName': 'hamburger'
                }
            } for ns in ALL_NAMESPACES
        ]
    }, TPicrobotState()), 'zstd_6'),
    encode_state('datacamp.market.yandex.net/duplicate_empty_mdsinfo', message_from_data({
        'MdsInfo': []
    }, TPicrobotState()), 'zstd_6'),
    encode_state('datacamp.market.yandex.net/duplicate_other_namespace', message_from_data({
        'MdsInfo': [
            {
                'MdsId': {
                    'Namespace': 'other_namespace',
                    'GroupId': 1,
                    'ImageName': 'hamburger'
                }
            },
        ]
    }, TPicrobotState()), 'zstd_6')
]


@pytest.fixture(scope='module')
def avatars_server():
    with PortManager() as pm:
        server = AvatarsHttpServer(port=pm.get_port())
        server.start_server()
        return server


@pytest.fixture(scope='module')
def picrobot_state_table(yt_server, config):
    return PicrobotStateTable(yt_server, config.state_path, PICROBOT_STATE_DATA)


@pytest.yield_fixture(scope='module')
def picrobot_server(
    config,
    yt_server,
    picrobot_state_table,
    avatars_server
):
    with make_picrobot_http(
        config,
        yt_server,
        picrobot_state_table,
        avatars_server
    ) as picrobot_http:
        yield picrobot_http
