# coding: utf-8

import pytest

from hamcrest import assert_that

from market.idx.generation.yatf.matchers.genlog_dumper.env_matchers import HasBaseOfferPropsExtFbRecursive
from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)
from market.proto.common.common_pb2 import (
    TConversion,
    EConversionType
)


@pytest.fixture(
    params=[
        (
            make_gl_record(
                vendor_code='vcode'
            ),
            {
                'VendorCode': 'vcode'
            },
        ),
        (
            make_gl_record(
                vendor_code='v' * 31
            ),
            {
                'VendorCode': None
            }
        ),
        (
            make_gl_record(
                buybox_elasticity=[
                    {'price_variant': 100, 'demand_mean': 2.0}
                ]
            ),
            {
                'BuyboxElasticity': [
                    {'PriceVariant': 100, 'DemandMean': 2.0}
                ]
            }
        ),
        (
            make_gl_record(
                virtual_model_id=123,
            ),
            {
                'VirtualModelId': {
                    'Value': 123,
                },
            }
        ),
        (
            make_gl_record(
                market_sku=1234,
                is_fast_sku=False,
            ),
            {
                'VirtualModelId': None,
            }
        ),
        (
            make_gl_record(
                market_sku=1234,
                is_fast_sku=True,
            ),
            {
                'VirtualModelId': {
                    'Value': 1234,
                },
            }
        ),
        (
            make_gl_record(
                virtual_model_id=123,
                market_sku=1234,
                is_fast_sku=True,
            ),
            {
                'VirtualModelId': {
                    'Value': 1234,
                },
            }
        ),
        (
            make_gl_record(
                classifier_category_confidence_for_filtering_stupids=0.5,
            ),
            {
                'ClassifierCategoryConfidenceForFilteringStupids': 0.5
            }
        ),
        (
            make_gl_record(
                offer_conversion=[
                    TConversion(conversion_value=0.2, conversion_type=EConversionType.Value('SPB')),
                    TConversion(conversion_value=0.3, conversion_type=EConversionType.Value('MSK')),
                ]
            ),
            {
                'OfferConversion': {
                    'OfferConversion': [
                        {
                            'ConversionValue2': 0.2,
                            'ConversionType': 'SPB',
                        },
                        {
                            'ConversionValue2': 0.3,
                            'ConversionType': 'MSK',
                        },
                    ]
                }
            }
        ),
        (
            make_gl_record(
                supplier_ogrn=25235,
            ),
            {
                'MpSupplierOgrn': '25235'
            }
        ),
        (
            make_gl_record(
                supplier_name='oao apple'
            ),
            {
                'MpSupplierOgrn': 'oao apple'
            }
        ),
        (
            make_gl_record(
                feed_id=235,
            ),
            {
                'FeedId': 235
            }
        ),
        (
            make_gl_record(
                classifier_magic_id="d534b1cba22c994c415d9cee78b0ab94",
            ),
            {
                'ClassifierMagicId': {
                    'Lower': 5519491896032965845,
                    'Upper': 10712850172076645697,
                }
            }
        ),
        (
            make_gl_record(
                ware_md5="dynamicpricing1000000w",
                dynamic_pricing_type=2,
                dynamic_pricing_threshold_is_percent=True,
                dynamic_pricing_threshold_value=1234,
            ),
            {
                "DynamicPricingData": 0b0100001000000000000000000000000000000000000000000000010011010010,  # 2 bit True  7,8 bits - type 2, last - 1234
            }
        ),
        (
            make_gl_record(
                ware_md5="dynamicpricing2000000w",
                dynamic_pricing_type=2,
                dynamic_pricing_threshold_is_percent=False,
                dynamic_pricing_threshold_value=1234,
            ),
            {
                "DynamicPricingData": 0b0000001000000000000000000000000000000000000000000000010011010010,  # 2 bit False 7,8 bits - type 2, last - 1234
            }
        ),
        (
            make_gl_record(
                ware_md5="dynamicpricing3000000w",
                dynamic_pricing_type=1,
                dynamic_pricing_threshold_is_percent=True,
                dynamic_pricing_threshold_value=1234,
            ),
            {
                "DynamicPricingData": 0b0100000100000000000000000000000000000000000000000000010011010010,  # 2 bit True  7,8 bits - type 1, last - 1234
            }
        ),
        (
            make_gl_record(
                ware_md5="dynamicpricing4000000w",
                dynamic_pricing_type=0,
                dynamic_pricing_threshold_is_percent=False,
                dynamic_pricing_threshold_value=1234,
            ),
            {
                "DynamicPricingData": 0b0000000000000000000000000000000000000000000000000000000000000000,  # type 0
            }
        ),
        (
            make_gl_record(
                ware_md5="dynamicpricing5000000w",
                dynamic_pricing_type=2,
                dynamic_pricing_threshold_is_percent=False,
            ),
            {
                "DynamicPricingData": 0b0000001000000000000000000000000000000000000000000000000000000000,  # 2 bit False 7,8 bits - type 2, last - auto 0
            }
        ),
        (
            make_gl_record(
                ware_md5="dynamicpricing6000000w",
                dynamic_pricing_type=1,
                dynamic_pricing_threshold_is_percent=True,
            ),
            {
                "DynamicPricingData": 0b0100000100000000000000000000000000000000000000000000000000000000,  # 2 bit True  7,8 bits - type 1, last - auto 0
            }
        ),
        (
            make_gl_record(
                ware_md5="dynamicpricing7000000w",
                dynamic_pricing_type=0,
                dynamic_pricing_threshold_is_percent=False,
            ),
            {
                "DynamicPricingData": 0b0000000000000000000000000000000000000000000000000000000000000000,  # type 0
            }
        ),
        (
            make_gl_record(
                ware_md5="dynamicpricing8000000w",
                dynamic_pricing_type=2,
            ),
            {
                "DynamicPricingData": 0b0000001000000000000000000000000000000000000000000000000000000000,  # 2 bit auto False 7,8 bits - type 2, last - auto 0
            }
        ),
        (
            make_gl_record(
                ware_md5="dynamicpricing9000000w",
                dynamic_pricing_type=1,
            ),
            {
                "DynamicPricingData": 0b0000000100000000000000000000000000000000000000000000000000000000,  # 2 bit auto False 7,8 bits - type 1, last - auto 0
            }
        ),
        (
            make_gl_record(
                ware_md5="dynamicpricing0100000w",
                dynamic_pricing_type=0,
            ),
            {
                "DynamicPricingData": 0b0000000000000000000000000000000000000000000000000000000000000000,  # type 0
            }
        ),
        (
            make_gl_record(
                medical_flags=255,
            ),
            {
                'MedicalFlags': 255
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
            '--dumper', 'BASE_OFFER_PROPS_EXT',
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto([gl_record])
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


def test_base_offer_props_ext(genlog_dumper, offers):
    """
    Проверяем, что поля из yt-genlog правильно попадают в base-offer-props-ext.fb64
    """
    _, fb_record = offers
    expected = {
        0: fb_record
    }

    assert_that(
        genlog_dumper,
        HasBaseOfferPropsExtFbRecursive(expected),
        'base-offer-props-ext.fb64 contains expected values'
    )
