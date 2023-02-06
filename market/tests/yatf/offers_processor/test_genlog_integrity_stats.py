from collections import namedtuple
import json
import pytest
import tempfile
from hamcrest import (
    all_of,
    assert_that,
    close_to,
    has_entries,
    has_items,
    has_properties,
)

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogIntegrityStatsRecord
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorGenlogIntegrityTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog

from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


@pytest.fixture(scope="module")
def genlog_rows():
    offers = [
        default_genlog(),
        default_genlog(),
        default_genlog(
            offer_id='20',
            ware_md5='000000000000000000020w',
            is_blue_offer=True,
            supplier_type=yt.yson.YsonUint64(1),
            warehouse_id=yt.yson.YsonUint64(222),
        ),
        default_genlog(
            offer_id='',
            ware_md5='000000000000000000020e',
        ),
    ]
    return offers


@pytest.yield_fixture(scope="module")
def integrity_log():
    with tempfile.NamedTemporaryFile('w+') as f:
        yield f.name


@pytest.yield_fixture(scope="module")
def integrity_stats_file():
    with tempfile.NamedTemporaryFile('w+') as f:
        yield f.name


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow_with_reducer(yt_server, genlog_table, integrity_log, integrity_stats_file):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorGenlogIntegrityTestEnv(
        yt_server,
        integrity_log_path=integrity_log,
        integrity_stats_path=integrity_stats_file,
        use_genlog_scheme=True,
        input_table_paths=input_table_paths,
        genlog_integrity_detect_empty_strings=True
    ) as env:
        env.execute()
        yield env


def test_genlog_integrity_stats(workflow_with_reducer):
    """ Check integrity stats table """
    assert_that(workflow_with_reducer, all_of(
        HasGenlogIntegrityStatsRecord(
            'supplier_type',
            null_count=3, non_null_count=1,
            null_ratio=3.0/4, non_null_ratio=1.0/4),
        HasGenlogIntegrityStatsRecord(
            'offer_id',
            null_count=1, non_null_count=3,
            null_ratio=1.0/4, non_null_ratio=3.0/4),
    ))


def test_genlog_integrity_log_file(workflow_with_reducer, integrity_log):
    """ Check integrity stats log """
    with open(integrity_log) as f:
        lines = f.readlines()

    entries = [json.loads(line) for line in lines]

    supplier_type = next(e for e in entries if e['field'] == 'supplier_type')
    assert_that(supplier_type, has_items('generation', 'host', 'timestamp', 'color'))
    assert_that(supplier_type, has_entries({
        'null_count': 3, 'non_null_count': 1, 'null_ratio': close_to(3.0/4, 0.01), 'non_null_ratio': close_to(1.0/4, 0.01)
    }))

    offer_id = next(e for e in entries if e['field'] == 'offer_id')
    assert_that(offer_id, has_items('generation', 'host', 'timestamp', 'color'))
    assert_that(offer_id, has_entries({
        'null_count': 1, 'non_null_count': 3, 'null_ratio': close_to(1.0/4, 0.01), 'non_null_ratio': close_to(3.0/4, 0.01)
    }))


def test_genlog_integrity_stats_file(workflow_with_reducer, integrity_stats_file):
    """ Check integrity stats log """
    with open(integrity_stats_file) as f:
        lines = f.readlines()

    Record = namedtuple('Record', ['field', 'null_count', 'non_null_count', 'null_ratio', 'non_null_ratio'])

    def parse_line(line):
        field, null_count, non_null_count, null_ratio, non_null_ratio = line.split('\t')
        return Record(field, int(null_count), int(non_null_count), float(null_ratio), float(non_null_ratio))

    entries = [parse_line(line) for line in lines]

    supplier_type = next(e for e in entries if e.field == 'supplier_type')
    assert_that(supplier_type, has_properties({
        'null_count': 3, 'non_null_count': 1, 'null_ratio': close_to(3.0/4, 0.01), 'non_null_ratio': close_to(1.0/4, 0.01)
    }))

    offer_id = next(e for e in entries if e.field == 'offer_id')
    assert_that(offer_id, has_properties({
        'null_count': 1, 'non_null_count': 3, 'null_ratio': close_to(1.0/4, 0.01), 'non_null_ratio': close_to(3.0/4, 0.01)
    }))
