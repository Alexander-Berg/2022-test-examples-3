# coding: utf-8

import pytest
import yt.wrapper as yt
from hamcrest import assert_that, equal_to, has_key, has_entries

from google.protobuf.json_format import MessageToDict
from market.idx.datacamp.proto.promo.Promo_pb2 import PromoType as DataCampPromoType, PromoMechanics
from market.idx.datacamp.proto.offer.OfferPromos_pb2 import (
    Promo,
    Promos,
    OfferPromos,
    MarketPromos,
)
from market.idx.promos.promo_details_collector.yatf.collector_env import PromoDetailsCollectorTestEnv
from market.proto.common.promo_pb2 import ESourceType
from market.proto.common.common_pb2 import PriceExpression
from market.proto.feedparser.Promo_pb2 import PromoDetails
from market.pylibrary.const.offer_promo import PromoType

from market.idx.promos.yatf.utils import make_datacamp_promos, make_datacamp_offers, make_promo_details


# проверка поиска
AxOP = OfferPromos(
    anaplan_promos=MarketPromos(
        active_promos=Promos(
            promos=[
                Promo(
                    id='promo_id_A', active=True, direct_discount=Promo.DirectDiscount(price=PriceExpression(price=33))
                )
            ]
        ),
        all_promos=Promos(
            promos=[
                Promo(
                    id='promo_id_A',
                    active=True,
                    direct_discount=Promo.DirectDiscount(base_price=PriceExpression(price=44)),
                )
            ]
        ),
    )
)

AxBlueSetMain1 = Promo(
    id='promo_id_B',
    active=True,
    partner_set=Promo.PartnerSet(
        primary_details=Promo.PartnerSet.PrimaryDetails(
            additional_offers_1=['offer_id_1100_2'],
            linked=True,
            discount_percent=12.3,
        )
    ),
)
AxBlueSetSecondary1 = Promo(
    id='promo_id_B',
    active=True,
    partner_set=Promo.PartnerSet(
        primary_offer_id='offer_id_1100_1',
    ),
)

AxBlueSetMain2 = Promo(
    id='promo_id_C',
    active=True,
    partner_set=Promo.PartnerSet(
        primary_details=Promo.PartnerSet.PrimaryDetails(
            additional_offers_1=['offer_id_1200_2'],
            additional_offers_2=['offer_id_1200_3', 'offer_id_1200_4'],
            linked=False,
            discount_percent=23.4,
        )
    ),
)
AxBlueSetSecondary2 = Promo(
    id='promo_id_C',
    active=True,
    partner_set=Promo.PartnerSet(
        primary_offer_id='offer_id_1200_1',
    ),
)

# офферы в хранилище
# соотв. параметрам функции make_datacamp_offers()
# feed_id, offer_id, warehouse_id, shop_id, promo_id, supplier_type, promo_block/AxPromo
PROMO1_01 = (100, 'offer_id_100_1', 100, 100, 'promo_id_1', 3, 'anaplan_promos_active')
PROMO2_01 = (200, 'offer_id_200_1', 200, 200, 'promo_id_2', 1, 'anaplan_promos_active')
PROMO3_01 = (300, 'offer_id_300_1', 300, 300, 'promo_id_3', 1, 'anaplan_promos_active')
PROMO3_02 = (300, 'offer_id_300_2', 300, 300, 'promo_id_3', 1, 'anaplan_promos_active')
PROMO4_01 = (400, 'offer_id_400_1', 400, 400, 'promo_id_4', 1, 'partner_cashback_promos')
PROMO7_01 = (700, 'offer_id_700_1', 700, 700, 'promo_id_7', 1, 'anaplan_promos_active')
PROMO7_02 = (700, 'offer_id_700_2', 700, 700, 'promo_id_7', 1, 'anaplan_promos_active')
PROMO7_03 = (700, 'offer_id_700_3', 700, 700, 'promo_id_7', 1, 'anaplan_promos_active')
PROMO8_01 = (800, 'offer_id_800_1', 800, 800, 'promo_id_8', 1, 'partner_promos')
PROMO9_01 = (900, 'offer_id_900_1', 900, 900, 'promo_id_9', 1, 'category_interface_promos')
PROMOA_01 = (101, 'offer_id_101_1', 101, 101, 'promo_id_A', 3, (AxOP, 33, 44))
# Partner set offers
PROMOB_01 = (1100, 'offer_id_1100_1', 1100, 1100, AxBlueSetMain1, 1, 'partner_promos')
PROMOB_02 = (1100, 'offer_id_1100_2', 1100, 1100, AxBlueSetSecondary1, 1, 'partner_promos')
PROMOC_01 = (1200, 'offer_id_1200_1', 1200, 1200, AxBlueSetMain2, 1, 'partner_promos')
PROMOC_02 = (1200, 'offer_id_1200_2', 1200, 1200, AxBlueSetSecondary2, 1, 'partner_promos')
PROMOC_03 = (1200, 'offer_id_1200_3', 1200, 1200, AxBlueSetSecondary2, 1, 'partner_promos')
PROMOC_04 = (1200, 'offer_id_1200_4', 1200, 1200, AxBlueSetSecondary2, 1, 'partner_promos')


@pytest.fixture(scope="module")
def offers():
    return [
        PROMO1_01,
        PROMO2_01,
        PROMO3_01,
        PROMO3_02,
        PROMO4_01,
        PROMO7_01,
        PROMO7_02,
        PROMO7_03,
        PROMO8_01,
        PROMO9_01,
        PROMOA_01,
        PROMOB_01,
        PROMOB_02,
        PROMOC_01,
        PROMOC_02,
        PROMOC_03,
        PROMOC_04,
    ]


@pytest.fixture(scope="module")
def valid_promos():
    return {
        '4C-x9myciHsUX2u01tH_yA': {
            'promo_id': 'promo_id_1',
            'type': DataCampPromoType.CHEAPEST_AS_GIFT,
            'source': ESourceType.PARTNER_SOURCE,
            'offers': [PROMO1_01],
        },
        'czq6xG-IYiRmsvog39ktiw': {
            'promo_id': 'promo_id_2',
            'type': DataCampPromoType.DIRECT_DISCOUNT,
            'source': ESourceType.PARTNER_SOURCE,
            'offers': [PROMO2_01],
        },
        '7kjjRoY9x6wq4ml8B0nZ9g': {  # MARKETINCIDENTS-8609, промокод 1
            'promo_id': 'promo_id_3',
            'type': DataCampPromoType.MARKET_PROMOCODE,
            'source': ESourceType.PARTNER_SOURCE,
            'offers': [PROMO3_01, PROMO3_02],
        },
        'OO07DVPySnAOUu46RNVYhw': {  # MARKETINCIDENTS-8609, промокод 2
            'promo_id': 'promo_id_4',
            'type': DataCampPromoType.MARKET_PROMOCODE,
            'source': ESourceType.PARTNER_SOURCE,
            'offers': [PROMO4_01],
        },
        '': {
            'promo_id': 'promo_id_5',
            'type': DataCampPromoType.PARTNER_STANDART_CASHBACK,
            'source': ESourceType.PARTNER_SOURCE,
            'mechanics_data': {
                'tariff_version_id': 0,
                'groups': [
                    {
                        'code_name': 'diy',
                        'value': 5,
                        'key': 'oe5AKO16VgfzpV1ff3_jhQ',
                        'priority': -198,
                        'is_extra_cashback': False,
                    },
                    {
                        'code_name': 'default',
                        'value': 12,
                        'key': 'Ltn701seg3XG_h5IC9-YTA',
                        'priority': -250,
                        'is_extra_cashback': True,
                    },
                    {
                        'code_name': 'cehac',
                        'value': 3,
                        'key': 'oA4BMu5fkR91zYVeUgPnnA',
                        'priority': -340,
                        'is_extra_cashback': True,
                    },
                ],
            },
            'suppliers': [123, 345],
            'offers_matching_rules': [
                PromoDetails.OffersMatchingRule(
                    suppliers=PromoDetails.OffersMatchingRule.IdsList(ids=[123, 345]),
                    category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(categories=[42]),
                ),
            ],
        },
        'Ek-gDPub7gGPBIQp4l_Esg': {
            'promo_id': 'promo_id_6',
            'type': DataCampPromoType.PARTNER_CUSTOM_CASHBACK,
            'source': ESourceType.PARTNER_SOURCE,
            'mechanics_data': {
                'tariff_version_id': 0,
                'value': 10,
                'priority': 200,
            },
            'suppliers': [123, 345],
            'offers_matching_rules': [
                PromoDetails.OffersMatchingRule(
                    suppliers=PromoDetails.OffersMatchingRule.IdsList(ids=[123, 345]),
                ),
            ],
        },
        'J2kWsQsAzQ5HW1RobKXnoQ': {
            'promo_id': 'promo_id_7',
            'type': DataCampPromoType.CHEAPEST_AS_GIFT,
            'source': ESourceType.PARTNER_SOURCE,
            'offers': [PROMO7_01, PROMO7_02, PROMO7_03],
        },
        'Nj6btZeRTj9dNUzfzRBqpw': {
            'promo_id': 'promo_id_8',
            'type': DataCampPromoType.MARKET_PROMOCODE,
            'source': ESourceType.PARTNER_SOURCE,
            'offers': [PROMO8_01],
            'mechanics_data': {
                'promo_code': 'SALE12',
                'bucket_min_price': 100,
                'order_max_price': 99999,
            },
            'additional_info': {'priority': 42},
        },
        'ZbckSgByFwq4Q6MTpfKfIQ': {
            'promo_id': 'promo_id_9',
            'type': DataCampPromoType.MARKET_PROMOCODE,
            'source': ESourceType.ANAPLAN,
            'offers': [PROMO9_01],
            'mechanics_data': {
                'promo_code': 'ANAPLAN-00',
                'bucket_min_price': 100,
                'order_max_price': 99999,
                'applying_type': PromoMechanics.MarketPromocode.ApplyingType.ONE_TIME,
            },
        },
        '-hWwSWkb6QVXSr7wjT6YIw': {
            'promo_id': 'promo_id_A',
            'type': DataCampPromoType.DIRECT_DISCOUNT,
            'source': ESourceType.PARTNER_SOURCE,
            'offers': [PROMOA_01],
        },
        'hCunXZk8_b1kaSeuKcWpuA': {
            'promo_id': 'promo_id_B',
            'type': DataCampPromoType.PARTNER_SET,
            'source': ESourceType.PARTNER_SOURCE,
            'offers': [PROMOB_01, PROMOB_02],
            'mechanics_data': {
                'items': {
                    PROMOB_01[1]: {
                        'offers1': [PROMOB_02[1]],
                        'discount_percent': 12.3,
                        'linked': True,
                    },
                },
            },
        },
        'TG_aj9WoqwJV95GsQDhXPg': {
            'promo_id': 'promo_id_C',
            'type': DataCampPromoType.PARTNER_SET,
            'source': ESourceType.PARTNER_SOURCE,
            'offers': [PROMOC_01],
            'mechanics_data': {
                'items': {
                    PROMOC_01[1]: {
                        'offers1': [PROMOC_02[1]],
                        'offers2': [PROMOC_03[1], PROMOC_04[1]],
                        'discount_percent': 23.4,
                        'linked': False,
                    },
                },
            },
        },
    }


@pytest.fixture(scope="module")
def rejected_promos():
    return {
        '1': {
            'promo_id': 'promo_id_-1',
            'type': DataCampPromoType.PARTNER_STANDART_CASHBACK,
            'source': ESourceType.PARTNER_SOURCE,
            'offers': [],
            'offers_matching_rules': [
                PromoDetails.OffersMatchingRule(
                    excluded_suppliers=PromoDetails.OffersMatchingRule.IdsList(ids=[1]),
                ),  # MARKETINCIDENTS-8587, пустые правила матчинга
            ],
            'mechanics_data': {
                'tariff_version_id': 0,
                'groups': [
                    {
                        'code_name': 'diy',
                        'value': 5,
                        'key': 'qb0Rpc-5qpfgEkSl0s7LNw',
                        'priority': -198,
                        'is_extra_cashback': False,
                    },
                ],
            },
        },
        '2': {
            'promo_id': 'promo_id_-2',
            'type': DataCampPromoType.DIRECT_DISCOUNT,
            'source': ESourceType.PARTNER_SOURCE,
            'offers': [],  # нет офферов
            'offers_matching_rules': [],  # MARKETINCIDENTS-8587, пустые правила матчинга
        },
    }


@pytest.fixture(scope="module")
def reject_reasons():
    return {'promo_id_-2': (10, 'Empty assortment'), 'promo_id_-1': (7, 'Missing inclusion matching rules')}


@pytest.fixture(scope="module")
def cashback_calculation_values():
    return {
        'categories': [
            {
                'market_tariff_version_id': 0,
                'code_name': 'diy',
                'hid': 42,
            },
            {
                'market_tariff_version_id': 0,
                'code_name': 'cehac',
                'hid': 42,
            },
            {
                'market_tariff_version_id': 0,
                'code_name': 'default',
                'hid': 42,
            },
        ],
        'priorities': [
            {
                'market_tariff_version_id': 0,
                'code_name': 'diy',
                'priority': -198,
            },
            {
                'market_tariff_version_id': 0,
                'code_name': 'cehac',
                'priority': -340,
            },
            {
                'market_tariff_version_id': 0,
                'code_name': 'default',
                'priority': -250,
            },
        ],
        'bucket_names': [
            {
                'market_tariff_version_id': 0,
                'custom_cashback_promo_priority_high': 1000,
                'custom_cashback_promo_priority_low': 100,
                'custom_cashback_promo_bucket_name': 'extra',
                'standard_cashback_promo_bucket_name': 'default',
            },
        ],
    }


@pytest.yield_fixture(scope="module")
def expected_promo_details(valid_promos, cashback_calculation_values):
    result = {}
    for key, data in valid_promos.items():
        result.update(make_promo_details(key, data, cashback_calculation_values, split_large_promos_part_size=2))
    return result


@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers, valid_promos, rejected_promos, cashback_calculation_values):
    datacamp_offers = make_datacamp_offers(offers)
    datacamp_promos = make_datacamp_promos(valid_promos, rejected_promos)
    with PromoDetailsCollectorTestEnv(
        yt_server,
        datacamp_offers,
        datacamp_promos,
        [],
        enable_filtered_promos_table=True,
        split_large_promos_part_size=2,
        cashback_calculation_values=cashback_calculation_values,
    ) as env:
        env.execute(keep_temporaries=True)
        env.verify()
        yield env


def test_promo_details_assembly_from_datacamp(
    workflow, valid_promos, rejected_promos, offers, expected_promo_details, reject_reasons
):

    # проверяем запись исходных данных (шапок промо) в таблицу АХ
    assert_that(len(workflow.datacamp_promos_data), equal_to(len(valid_promos) + len(rejected_promos)))

    # проверяем что кол-во офферов в промежуточной таблице из ОХ соответствует кол-ву офферов в тесте
    temporary_blue_offers_data = workflow.temporary_blue_offers_data
    assert_that(len(temporary_blue_offers_data), equal_to(len(offers) - 4))  # -4 вторичных оффера из комплектов
    # проверяем что старая и промо цена корректно подтянулись из данных АХ/ОХ
    for ax_offer in temporary_blue_offers_data:
        if ax_offer['promo_id'] == 'promo_id_A':
            assert_that(PriceExpression.FromString(yt.yson.get_bytes(ax_offer['discount_price'])).price, equal_to(33))
            assert_that(PriceExpression.FromString(yt.yson.get_bytes(ax_offer['old_price'])).price, equal_to(44))

    # проверяем отброшенные промо
    filtered_promos_data = workflow.filtered_promos_data
    assert_that(filtered_promos_data, equal_to(reject_reasons))

    # проверяем промежуточную таблицу (собраные промо-детали из АХ+ОХ)
    datacamp_promo_details_data = workflow.datacamp_promo_details_data
    for (promo_key, part_id), promo_details in datacamp_promo_details_data.items():
        if (promo_key, part_id) in expected_promo_details:
            assert_that(
                promo_details,
                equal_to(expected_promo_details[(promo_key, part_id)]),
                'promo details content check, key {}'.format(promo_key),
            )
        else:
            assert_that(promo_details, equal_to(dict()), f'unexpected promo_key {promo_key} part_id {part_id}')
    assert_that(len(datacamp_promo_details_data), equal_to(len(expected_promo_details)))

    for (promo_key, part_id), promo_details in expected_promo_details.items():
        assert_that(
            datacamp_promo_details_data,
            has_key((promo_key, part_id)),
            "Not found (promo_key {}, part_id {}) in datacamp_promo_details_data".format(promo_key, part_id),
        )

    # проверяем финальную таблицу с промо-деталями
    collected_promo_details_data = workflow.promo_details_result_data
    assert_that(len(collected_promo_details_data), equal_to(len(expected_promo_details)))

    for (promo_key, part_id), _ in collected_promo_details_data.items():
        assert_that(
            expected_promo_details,
            has_key((promo_key, part_id)),
            "Unexpected promo_key {} in collected_promo_details_data".format(promo_key),
        )

    for (promo_key, part_id), _ in expected_promo_details.items():
        assert_that(
            collected_promo_details_data,
            has_key((promo_key, part_id)),
            "Not found promo_key {} in collected_promo_details_data".format(promo_key),
        )


def test_datacamp_promocode_details(workflow, expected_promo_details):
    """
    Проверяем корректность заполнения данных при конвертации промокода из АХ
    """
    collected_promo_details_data = workflow.promo_details_result_data

    promocode_key = ('Nj6btZeRTj9dNUzfzRBqpw', 0)

    collected_promocode = MessageToDict(
        collected_promo_details_data[promocode_key], preserving_proto_field_name=True, use_integers_for_enums=True
    )
    expected_promocode = expected_promo_details[promocode_key]
    assert_that(
        collected_promocode,
        has_entries(
            {
                'shop_promo_id': expected_promocode.shop_promo_id,
                'type': str(PromoType.PROMO_CODE),
                'source_type': ESourceType.PARTNER_SOURCE,
                'promo_code': expected_promocode.promo_code,
                'conditions': expected_promocode.conditions,
                'restrictions': {
                    'order_min_price': {
                        'value': str(expected_promocode.restrictions.order_min_price.value),
                        'currency': 'RUR',
                    },
                    'order_max_price': {
                        'value': str(expected_promocode.restrictions.order_max_price.value),
                        'currency': 'RUR',
                    },
                },
                'same_type_priority': expected_promocode.same_type_priority,
            }
        ),
    )


def test_datacamp_partner_set_details_one_offer(workflow, expected_promo_details):
    collected_promo_details_data = workflow.promo_details_result_data
    blueset_key = ('hCunXZk8_b1kaSeuKcWpuA', 0)

    collected_blueset = MessageToDict(
        collected_promo_details_data[blueset_key], preserving_proto_field_name=True, use_integers_for_enums=True
    )
    expected_blueset = expected_promo_details[blueset_key]
    assert_that(
        collected_blueset,
        has_entries(
            {
                'shop_promo_id': expected_blueset.shop_promo_id,
                'type': str(PromoType.BLUE_SET),
                'source_type': ESourceType.PARTNER_SOURCE,
                'blue_set': {
                    'sets_content': [
                        {
                            'items': [
                                {
                                    'offer_id': 'offer_id_1100_1',
                                    'count': 1,
                                    'discount': 12.3,
                                },
                                {
                                    'offer_id': 'offer_id_1100_2',
                                    'count': 1,
                                    'discount': 12.3,
                                },
                            ],
                            'linked': True,
                        }
                    ]
                },
            }
        ),
    )


def test_datacamp_partner_set_details_multi_offers(workflow, expected_promo_details):
    collected_promo_details_data = workflow.promo_details_result_data
    blueset_key = ('TG_aj9WoqwJV95GsQDhXPg', 0)

    collected_blueset = MessageToDict(
        collected_promo_details_data[blueset_key], preserving_proto_field_name=True, use_integers_for_enums=True
    )
    expected_blueset = expected_promo_details[blueset_key]
    assert_that(
        collected_blueset,
        has_entries(
            {
                'shop_promo_id': expected_blueset.shop_promo_id,
                'type': str(PromoType.BLUE_SET),
                'source_type': ESourceType.PARTNER_SOURCE,
                'blue_set': {
                    'sets_content': [
                        {
                            'items': [
                                {
                                    'offer_id': 'offer_id_1200_1',
                                    'count': 1,
                                    'discount': 23.4,
                                },
                                {
                                    'offer_id': 'offer_id_1200_2',
                                    'count': 1,
                                    'discount': 23.4,
                                },
                                {
                                    'offer_id': 'offer_id_1200_3',
                                    'count': 1,
                                    'discount': 23.4,
                                },
                            ],
                            'linked': False,
                        },
                        {
                            'items': [
                                {
                                    'offer_id': 'offer_id_1200_1',
                                    'count': 1,
                                    'discount': 23.4,
                                },
                                {
                                    'offer_id': 'offer_id_1200_2',
                                    'count': 1,
                                    'discount': 23.4,
                                },
                                {
                                    'offer_id': 'offer_id_1200_4',
                                    'count': 1,
                                    'discount': 23.4,
                                },
                            ],
                            'linked': False,
                        },
                    ]
                },
            }
        ),
    )
