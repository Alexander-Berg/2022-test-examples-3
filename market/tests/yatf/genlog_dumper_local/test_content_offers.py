# coding: utf-8

import pytest

from market.proto.indexer.GenerationLog_pb2 import Record
from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import (
    InputRecordsProto
)
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import (
    RunOptions
)
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)


@pytest.fixture(scope="module")
def offers():
    return InputRecordsProto([
        Record(
            feed_id=1,
            offer_id='abc',
            ware_md5='dce'
        )
    ])


@pytest.yield_fixture(scope="module")
def genlog_dumper(offers):
    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'OFFER_CONTENT',
        ]),
        OFFERS_RESOURCE_NAME: offers
    }
    with GenlogDumperTestEnv(**gd_resources) as genlog_dumper:
        genlog_dumper.execute()
        genlog_dumper.verify()
        yield genlog_dumper


@pytest.yield_fixture(scope="module")
def content_offers(genlog_dumper):
    genlog_dumper.outputs['content_offers'].load()
    yield genlog_dumper.outputs['content_offers'].content


def test_content_offers_count(offers, content_offers):
    assert len(content_offers) == len(offers.records)


def test_content_offer_data(offers, content_offers):
    expected = [
        [r.ware_md5, str(r.feed_id), r.offer_id, '0']  # NUMBER_INDEX_PART = '0'
        for r in offers.records
    ]
    actual = content_offers
    assert actual == expected
