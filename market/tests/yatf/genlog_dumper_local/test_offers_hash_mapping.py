# coding: utf-8

import pytest

from hamcrest import assert_that

from market.idx.generation.yatf.matchers.genlog_dumper.env_matchers import HasOffersHashMappingFbRecursive
from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)


@pytest.fixture(
    params=[
        (
            make_gl_record(
                feed_id=101967,
                offer_id='offer',
            ),
            {
                'offer_to_offset_map': {
                    'offer_hash': 5913504389159329514,
                    'offer_offset': 0,
                }
            }
        ),
    ],
    scope="module"
)
def offers(request):
    return request.param


@pytest.yield_fixture(scope="module")
def genlog_dumper(offers):
    gl_record, _ = offers
    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'OFFERS_HASH_MAPPING',
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto([gl_record])
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


def test_offers_hash_mapping(genlog_dumper, offers):
    """
    Проверяем, что поля из yt-genlog правильно попадают в offers-hash-mapping.fb
    """
    _, fb_record = offers
    expected = {
        0: fb_record
    }

    assert_that(
        genlog_dumper,
        HasOffersHashMappingFbRecursive(expected),
        'offers-hash-mapping.fb contains expected values'
    )
