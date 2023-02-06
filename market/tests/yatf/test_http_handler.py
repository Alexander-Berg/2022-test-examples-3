import pytest
import requests
from hamcrest import assert_that, equal_to

from yt.wrapper import ypath_join

from market.idx.input.mdm_dumper.yatf.test_envs.test_env import LbDumperTestEnv
from market.idx.input.mdm_dumper.yatf.resources.config import LbDumperConfig
from market.idx.yatf.resources.yt_tables.lbdumper_tables import LbDumperMdmTable
from market.idx.input.mdm_dumper.yatf.resources.tokens import YtTokenStub
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix


@pytest.fixture(scope='module')
def yt_token():
    return YtTokenStub()


@pytest.yield_fixture(scope="module")
def yt_mdm_info_table_path():
    return ypath_join(get_yt_prefix(), 'lbdumper', 'mdm_info')


@pytest.yield_fixture(scope="module")
def yt_mdm_info_table(yt_server, yt_mdm_info_table_path):
    return LbDumperMdmTable(
        yt_stuff=yt_server,
        path=yt_mdm_info_table_path,
        data=None
    )


@pytest.fixture(scope='module')
def lbdumper_config(
    yt_server,
    yt_token,
    yt_mdm_info_table_path,
):
    cfg = LbDumperConfig()

    http_handler = cfg.create_monitorings_http_handler_processor(
        'mdm_table_modification_time',
        yt_server,
        yt_token.path,
        yt_mdm_info_table_path
    )
    cfg.create_link('HttpInput', http_handler)

    return cfg


@pytest.fixture(scope="module")
def workflow(yt_server, lbdumper_config, yt_mdm_info_table):
    resources = {
        'lbdumper_config': lbdumper_config,
        'yt_mdm_info_table': yt_mdm_info_table,
    }

    with LbDumperTestEnv(yt_server, **resources) as env:
        yield env


def test_monitoring_unknwon_command(workflow):
    url = 'http://{host}:{port}/monitoring/unknown'.format(
        host=workflow.host,
        port=workflow.http_port
    )
    post_status = requests.post(url).status_code
    assert_that(post_status, equal_to(400))

    get_status = requests.get(url).status_code
    assert_that(get_status, equal_to(400))


def test_monitoring_mdm_table_modification_time(workflow):
    url = 'http://{host}:{port}/monitoring/mdm_table_modification_time'.format(
        host=workflow.host,
        port=workflow.http_port
    )

    post_req = requests.post(url)
    assert_that(post_req.status_code, equal_to(200))
    assert_that(post_req.text, equal_to('0;OK'))

    get_req = requests.get(url)
    assert_that(get_req.status_code, equal_to(200))
    assert_that(get_req.text, equal_to('0;OK'))
