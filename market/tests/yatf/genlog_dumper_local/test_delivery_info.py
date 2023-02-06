# coding: utf-8

import pytest

from hamcrest import assert_that

from market.proto.common.common_pb2 import (
    TPickupOption,
)

from market.idx.generation.yatf.matchers.genlog_dumper.env_matchers import HasOffersDeliveryInfoFbRecursive
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
                pickup_options=[
                    TPickupOption(
                        Cost=300,
                        DaysMin=1,
                        DaysMax=2,
                        OrderBeforeHour=11
                    )
                ]
            ),
            {
                'pickup_options': {
                    'Options': [
                        {
                            'PriceValue': 300 * (10 ** 7),
                            'DaysMin': 1,
                            'DaysMax': 2,
                            'OrderBeforeHour': 11
                        }
                    ]
                }
            }
        ),
        (
            make_gl_record(
                pickup_options=[
                    TPickupOption(
                        Cost=100,
                        DaysMin=3,
                        DaysMax=4,
                        OrderBeforeHour=13
                    ),
                    TPickupOption(
                        Cost=200,
                        DaysMin=5,
                        DaysMax=6,
                        OrderBeforeHour=14
                    ),
                ]
            ),
            {
                'pickup_options': {
                    'Options': [
                        {
                            'PriceValue': 100 * (10 ** 7),
                            'DaysMin': 3,
                            'DaysMax': 4,
                            'OrderBeforeHour': 13
                        },
                        {
                            'PriceValue': 200 * (10 ** 7),
                            'DaysMin': 5,
                            'DaysMax': 6,
                            'OrderBeforeHour': 14
                        }
                    ]
                }
            }
        ),
        (
            make_gl_record(),
            {}
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
            '--dumper', 'OFFERS_DELIVERY_INFO',
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto([gl_record])
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


def test_delivery_info(genlog_dumper, offers):
    """
    Проверяем, что поля из yt-genlog правильно попадают в offers-delivery-info.fb
    """
    _, fb_record = offers
    expected = {
        0: fb_record
    }

    assert_that(
        genlog_dumper,
        HasOffersDeliveryInfoFbRecursive(expected),
        'offers-delivery-info.fb contains expected values'
    )
