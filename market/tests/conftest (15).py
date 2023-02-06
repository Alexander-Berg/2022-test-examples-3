import pytest

from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.resources.tokens import YtTokenStub
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper import ypath_join


@pytest.fixture(scope='module')
def input_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def output_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def offers_blog_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def yt_token():
    return YtTokenStub()


@pytest.fixture(scope='module')
def datacamp_msku_table_path():
    return ypath_join(get_yt_prefix(), 'msku/msku')


@pytest.fixture(scope='module')
def resale_business_ids_table_path():
    return ypath_join(get_yt_prefix(), 'goods/resale_business_ids')


@pytest.yield_fixture(scope='module')
def partner_info_table_path():
    return ypath_join(get_yt_prefix(), 'datacamp', 'partners')


@pytest.fixture(scope='module')
def partners_table(yt_server, partner_info_table_path):
    return DataCampPartnersTable(
        yt_stuff=yt_server,
        path=partner_info_table_path,
    )


@pytest.fixture(scope='module')
def univermags_table_path():
    return ypath_join(get_yt_prefix(), 'datacamp', 'univermags')


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic, partners_table):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'partners_table': partners_table
    }

    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner
