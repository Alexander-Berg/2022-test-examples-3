import pytest

from market.idx.export.yatf.test_envs.genlog_export import GenlogExportTestEnv
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
"""
from market.idx.yatf.resources.yt_stuff_resource import yt_server
"""


@pytest.fixture(scope="module")
def genlog():
    return []


@pytest.yield_fixture(scope="module")
def genlog_export(yt_server, genlog):
    resources = {
        "genlog_table": GenlogOffersTable(yt_server, get_yt_prefix()+ '/genlog', genlog)
    }
    with GenlogExportTestEnv(**resources) as env:
        env.execute(yt_server)
        env.verify()
        yield env


def test_genlog_export(genlog_export):
    pass
