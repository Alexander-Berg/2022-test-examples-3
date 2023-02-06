# coding: utf-8
import pytest
from hamcrest import assert_that, equal_to, has_key

from market.idx.datacamp.proto.promo.Promo_pb2 import (
    PromoType as DataCampPromoType,
    BusinessMigrationInfo,
)
from market.idx.promos.promo_details_collector.yatf.collector_env import PromoDetailsCollectorTestEnv
from market.idx.promos.yt_promo_indexer.yatf.resources import YtBluePromocodeTable
from market.idx.yatf.resources.yt_stuff_resource import(
    get_yt_prefix
)
from yt.wrapper import ypath_join

from market.proto.common.promo_pb2 import ESourceType

from market.pylibrary.const.payment_methods import (
    PaymentMethod,
)

from market.pylibrary.const.offer_promo import (
    PromoType,
    MechanicsPaymentType,
)

from market.idx.promos.yatf.utils import make_datacamp_promos, make_datacamp_offers, make_promo_details


DT_NOW = 1637040857  # 17/11/20021
DT_DELTA = 241921800  # timedelta(weeks=400)
MMAP_NAME = 'yt_promo_details_generic_bundle.mmap'
FEED_ID = 777
HID_1 = 888
HID_2 = 889
HID_3 = 890


@pytest.fixture(scope="module")
def offers():
    return [
        # соотв. параметрам функции make_datacamp_offers()
        # feed_id, offer_id, warehouse_id, shop_id, promo_id, supplier_type, promo_block
        (100, 'offer_id_1', 200, 300, 'promo_id_1', 1, 'anaplan_promos_active'),
        (101, 'offer_id_2', 201, 301, 'promo_id_1', 1, 'anaplan_promos_active'),
        (102, 'offer_id_3', 202, 302, 'promo_id_2', 1, 'anaplan_promos_active'),
        (103, 'offer_id_4', 203, 303, 'promo_id_3', 1, 'anaplan_promos_active'),
    ]


@pytest.fixture(scope="module")
def valid_datacamp_promos_data():
    return {
        '4C-x9myciHsUX2u01tH_yA': {
            'promo_id': 'promo_id_1',
            'type': DataCampPromoType.CHEAPEST_AS_GIFT,
            'source': ESourceType.PARTNER_SOURCE,
            'enabled': True,
        },
    }


@pytest.fixture(scope="module")
def invalid_datacamp_promos_data():
    return {
        # Акция фильтруется, так как у неё установлен флаг enabled=False
        'N7bkthhWnHbvy0LZrqG1qA': {
            'promo_id': 'promo_id_2',
            'type': DataCampPromoType.CHEAPEST_AS_GIFT,
            'source': ESourceType.PARTNER_SOURCE,
            'enabled': False,
        },
        # Акция фильтруется, так как у неё не поддерживаемый источник
        'N7bkthhWnHbvy0LZrqGGGG': {
            'promo_id': 'promo_id_3',
            'type': DataCampPromoType.CHEAPEST_AS_GIFT,
            'source': ESourceType.AFFILIATE,
            'enabled': True,
        },
        # Акция фильтруется, так как находится в состоянии миграции
        'N7bkthhWnHbvy0LZrqFFFF': {
            'promo_id': 'promo_id_4',
            'type': DataCampPromoType.CHEAPEST_AS_GIFT,
            'source': ESourceType.PARTNER_SOURCE,
            'enabled': True,
            'business_migration_info': {
                'status': BusinessMigrationInfo.Status.OLD_BUSINESS,
            }
        },
    }


@pytest.fixture(scope="module")
def datacamp_promos_data(valid_datacamp_promos_data, invalid_datacamp_promos_data):
    res = valid_datacamp_promos_data.copy()
    res.update(invalid_datacamp_promos_data)
    return res


@pytest.fixture(scope='module')
def invalid_loyalty_promocode_data(request):
    return [
        # Акция невалидна, так как у неё отсутствуют ограничения на включение.
        {
            'feed_id': FEED_ID,
            'type': PromoType.PROMO_CODE,
            'promo_code': 'promo_code_1_text',
            'discount': {
                'value': 300,
                'currency': 'RUR',
            },
            'url': 'http://promocode_1.com',
            'landing_url': 'http://promocode_1_landing.com',
            'shop_promo_id': 'promo_code_1',
            'start_date': (DT_NOW - DT_DELTA),
            'end_date': (DT_NOW + DT_DELTA),
            'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
            'mechanics_payment_type': MechanicsPaymentType.CPC,
            'source_type': ESourceType.LOYALTY,
            'source_reference': 'http://source_reference_1.ru',
            'offers_matching_rules': [
                {
                    'category_restriction': {
                        'excluded_categories': [
                            HID_3,
                        ],
                    },
                },
            ],
        },
        # Акция фильтруется, так как у неё не поддерживаемый источник
        {
            'feed_id': FEED_ID,
            'type': PromoType.PROMO_CODE,
            'promo_code': 'promo_code_3_text',
            'discount': {
                'value': 300,
                'currency': 'RUR',
            },
            'url': 'http://promocode_3.com',
            'landing_url': 'http://promocode_3_landing.com',
            'shop_promo_id': 'promo_code_3',
            'start_date': (DT_NOW - DT_DELTA),
            'end_date': (DT_NOW + DT_DELTA),
            'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
            'mechanics_payment_type': MechanicsPaymentType.CPC,
            'source_type': ESourceType.AFFILIATE,
            'source_reference': 'http://source_reference_1.ru',
            'offers_matching_rules': [
                {
                    'category_restriction': {
                        'categories': [
                            HID_1, HID_2,
                        ],
                    },
                },
            ],
        },
        # Акция фильтруется, так как у неё истек срок действия
        {
            'feed_id': FEED_ID,
            'type': PromoType.PROMO_CODE,
            'promo_code': 'promo_code_4_text',
            'discount': {
                'value': 301,
                'currency': 'RUR',
            },
            'url': 'http://promocode_4.com',
            'landing_url': 'http://promocode_4_landing.com',
            'shop_promo_id': 'promo_code_4',
            'start_date': (DT_NOW - DT_DELTA),
            'end_date': (DT_NOW - DT_DELTA),
            'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
            'mechanics_payment_type': MechanicsPaymentType.CPC,
            'source_type': ESourceType.LOYALTY,
            'source_reference': 'http://source_reference_4.ru',
            'offers_matching_rules': [
                {
                    'category_restriction': {
                        'categories': [
                            HID_1, HID_2,
                        ],
                    },
                },
            ],
        },
        # Акция фильтруется по disabled by promo key in config
        {
            'feed_id': FEED_ID,
            'type': PromoType.PROMO_CODE,
            'promo_code': 'promo_code_5_text',
            'discount': {
                'value': 301,
                'currency': 'RUR',
            },
            'url': 'http://promocode_5.com',
            'landing_url': 'http://promocode_5_landing.com',
            'shop_promo_id': 'promo_code_5',
            'start_date': (DT_NOW - DT_DELTA),
            'end_date': (DT_NOW + DT_DELTA),
            'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
            'mechanics_payment_type': MechanicsPaymentType.CPC,
            'source_type': ESourceType.LOYALTY,
            'source_reference': 'http://source_reference_5.ru',
            'offers_matching_rules': [
                {
                    'category_restriction': {
                        'categories': [
                            HID_1, HID_2,
                        ],
                    },
                },
            ],
        },
    ]


@pytest.fixture(scope='module')
def valid_loyalty_promocode_data(request):
    return [
        {
            'feed_id': FEED_ID,
            'type': PromoType.PROMO_CODE,
            'promo_code': 'promo_code_2_text',
            'discount': {
                'value': 301,
                'currency': 'RUR',
            },
            'url': 'http://promocode_2.com',
            'landing_url': 'http://promocode_2_landing.com',
            'shop_promo_id': 'promo_code_2',
            'start_date': (DT_NOW - DT_DELTA),
            'end_date': (DT_NOW + DT_DELTA),
            'generation_ts': (DT_NOW - DT_DELTA),
            'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
            'mechanics_payment_type': MechanicsPaymentType.CPC,
            'source_type': ESourceType.LOYALTY,
            'source_reference': 'http://source_reference_2.ru',
            'offers_matching_rules': [
                {
                    'category_restriction': {
                        'categories': [
                            HID_1, HID_2,
                        ],
                    },
                },
            ],
        },
    ]


@pytest.fixture(scope='module')
def loyalty_promocode_data(valid_loyalty_promocode_data, invalid_loyalty_promocode_data):
    return valid_loyalty_promocode_data + invalid_loyalty_promocode_data


@pytest.yield_fixture(scope="module")
def expected_promo_details_datacamp(valid_datacamp_promos_data):
    result = {}
    for key, data in valid_datacamp_promos_data.items():
        result.update(make_promo_details(key, data))
    return result


@pytest.yield_fixture(scope="module")
def expected_promo_details_all(valid_datacamp_promos_data, valid_loyalty_promocode_data):
    result = {}
    for key, data in valid_datacamp_promos_data.items():
        result.update(make_promo_details(key, data))
    for pd in valid_loyalty_promocode_data:
        result[(pd['shop_promo_id'], 0)] = pd
    return result


@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers, datacamp_promos_data, loyalty_promocode_data):
    datacamp_offers = make_datacamp_offers(offers)
    datacamp_promos = make_datacamp_promos(datacamp_promos_data)
    loyalty_promocode_table_name = ypath_join(get_yt_prefix(), 'promocode_coin', 'recent')
    resources = {
        'loyalty_promocode_table': YtBluePromocodeTable(
            yt_stuff=yt_server,
            path=loyalty_promocode_table_name,
            data=loyalty_promocode_data
        ),
    }
    with PromoDetailsCollectorTestEnv(yt_server, datacamp_offers, datacamp_promos, [],
                                        enable_filtered_promos_table=True,
                                        cashback_calculation_values=None,
                                        disabled_promo_keys="promo_code_5",
                                        **resources) as env:
        env.execute()
        env.verify()
        yield env


def test_filtered_promos_table(
        workflow,
        expected_promo_details_datacamp,
        expected_promo_details_all,
        invalid_datacamp_promos_data,
        datacamp_promos_data,
        valid_loyalty_promocode_data,
        invalid_loyalty_promocode_data):
    assert_that(len(datacamp_promos_data), equal_to(4))

    datacamp_promo_details_data = workflow.datacamp_promo_details_data
    assert_that(len(datacamp_promo_details_data), equal_to(len(expected_promo_details_datacamp)))

    for promo_key, _ in datacamp_promo_details_data.items():
        assert_that(expected_promo_details_datacamp, has_key(promo_key), 'Unexpected promo_key {} in datacamp_promo_details_data'.format(promo_key))

    for promo_key, _ in expected_promo_details_datacamp.items():
        assert_that(datacamp_promo_details_data, has_key(promo_key), 'Not found promo_key {} in datacamp_promo_details_data'.format(promo_key))

    collected_promo_details_data = workflow.promo_details_result_data
    assert_that(len(collected_promo_details_data), equal_to(len(expected_promo_details_all)))

    for (promo_key, part_id), _ in expected_promo_details_all.items():
        assert_that(collected_promo_details_data, has_key((promo_key, part_id)), "Not found (promo_key {}, part_id {}) in collected_promo_details_data".format(promo_key, part_id))

    filtered_promos_data = workflow.filtered_promos_data
    assert_that(len(filtered_promos_data), equal_to(len(invalid_datacamp_promos_data) + len(invalid_loyalty_promocode_data)))
    for _, promo in invalid_datacamp_promos_data.items():
        assert_that(filtered_promos_data, has_key(promo['promo_id']), 'Not found promo_id {} in filtered_promos_data'.format(promo['promo_id']))
