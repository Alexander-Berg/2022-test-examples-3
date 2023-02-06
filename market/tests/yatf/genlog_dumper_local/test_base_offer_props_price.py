# coding: utf-8

import pytest

from hamcrest import assert_that

from market.idx.generation.yatf.matchers.genlog_dumper.env_matchers import HasBaseOfferPropsFbRecursive
from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.proto.common.common_pb2 import (
    PriceExpression
)
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)


RUR = "RUR"
BLUE_PRICE = 7770000000
WHIRE_PRICE = 222


@pytest.fixture(
    params=[
        #  Offers for testing oldPrice. We take binary_war_old_price for blue offers and binary_old_price for
        (
            make_gl_record(
                binary_raw_oldprice=PriceExpression(price=BLUE_PRICE,
                                                    id=RUR),
                binary_oldprice=PriceExpression(price=777,
                                                id=RUR),
                is_blue_offer=True
            ),
            {
                'OldPrice': {
                    "Value": BLUE_PRICE
                }
            }
        ),
        (
            make_gl_record(
                binary_raw_oldprice=PriceExpression(price=77,
                                                    id=RUR),
                binary_oldprice=PriceExpression(price=WHIRE_PRICE,
                                                id=RUR),
                is_blue_offer=False
            ),
            {
                'OldPrice': {
                    "Value": WHIRE_PRICE
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
    Проверяем, что поля с ценами генлога правильно попадают в base-offer-props.fb
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
