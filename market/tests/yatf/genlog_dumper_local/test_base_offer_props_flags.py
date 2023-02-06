# coding: utf-8

import pytest

from hamcrest import assert_that

from market.idx.pylibrary.offer_flags.flags import DisabledFlags, OfferFlags

from market.idx.generation.yatf.matchers.genlog_dumper.env_matchers import HasBaseOfferPropsFbRecursive
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
            make_gl_record(),
            {
                'DisabledFlags': 0
            }
        ),
        (
            make_gl_record(
                disabled_flags=DisabledFlags.MARKET_STOCK
            ),
            {
                'DisabledFlags': DisabledFlags.MARKET_STOCK
            }
        ),
        (
            make_gl_record(
                flags=OfferFlags.IS_PREORDER
            ),
            {
                'Flags64': OfferFlags.IS_PREORDER
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
            '--dumper', 'BASE_OFFER_PROPS',
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto([gl_record])
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


def test_base_offer_props(genlog_dumper, offers):
    """
    Проверяем, что флаги из генлога правильно попадают в base-offer-props.fb
    """
    _, fb_record = offers
    expected = {
        0: fb_record
    }

    assert_that(
        genlog_dumper,
        HasBaseOfferPropsFbRecursive(expected),
        'base-offer-props.fb contains expected disabled_flags value'
    )
