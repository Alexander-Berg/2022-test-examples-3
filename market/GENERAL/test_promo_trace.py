#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent
from core.report import REQUEST_TIMESTAMP
from core.testcase import (
    TestCase,
    main,
)
from core.types import DynamicBlueGenericBundlesPromos, Promo, PromoMSKU, PromoType, Region, Shop, DynamicQPromos
from core.types.offer_promo import (
    PromoBlueFlash,
    PromoDirectDiscount,
    PromoBlueCashback,
    source_type_name,
    PromoRestrictions,
    PromoSpreadDiscountCount,
    OffersMatchingRules,
)
from core.types.sku import (
    MarketSku,
    BlueOffer,
)
from market.proto.common.promo_pb2 import ESourceType
from datetime import datetime, timedelta
import time


DEFAULT_HID = 123456
HID_1 = 123000
HID_2 = 123001
CURRENT_TIME = datetime.utcfromtimestamp(REQUEST_TIMESTAMP)
TIME_SHIFT = timedelta(days=1)
ONE_DAY_SEC = 24 * 60 * 60


class _Shops(object):
    third_party = Shop(
        fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL
    )
    third_party1 = Shop(
        fesh=778,
        datafeed_id=778,
        priority_region=213,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
    )
    controversial_third_party = Shop(
        fesh=779,
        datafeed_id=779,
        priority_region=213,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
    )


blue_cashback = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key='Promo_blue_cashback',
    url='http://blue_cashback.com/',
    blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
    promo_internal_priority=112,
    promo_bucket_name='blabla',
)

direct_discount = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=_Shops.third_party.datafeed_id,
    key='Promo_direct_discount',
    url='http://direct_discount.com/',
    direct_discount=PromoDirectDiscount(
        discounts_by_category=[
            {
                'category_restriction': {
                    'categories': [
                        HID_1,
                    ],
                },
                'discount_percent': 12.3,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    source_reference='http://source_reference.com/direct_discount',
    anaplan_id='direct_discount_anaplan_id',
    shop_promo_id='direct_discount_shop_promo_id',
    source_type=ESourceType.ROBOT,
    parent_promo_id='#BF20',
)


def make_offer_for_promo(id, shop, promo=None):
    return BlueOffer(
        price=500,
        price_old=5000,
        offerid='blue.offer.{}'.format(id),
        feedid=shop.datafeed_id,
        fesh=shop.fesh,
        waremd5='Sku{}____-vm1Goleg'.format(id),
        promo=promo,
    )


class _Offers(object):
    offer_1 = make_offer_for_promo(
        "100000",
        _Shops.third_party,
        promo=[
            blue_cashback,
            direct_discount,
        ],
    )
    offer_2 = make_offer_for_promo(
        "100001",
        _Shops.third_party1,
        promo=[],
    )
    offer_3 = make_offer_for_promo(
        "100002",
        _Shops.third_party1,
        promo=[],
    )
    offer_4 = make_offer_for_promo(
        "100004",
        _Shops.third_party1,
        promo=[],
    )
    offer_5 = make_offer_for_promo(
        "100005",
        _Shops.third_party1,
        promo=[],
    )
    offer_6 = make_offer_for_promo("100006", _Shops.controversial_third_party, promo=[])
    offer_7 = make_offer_for_promo("100007", _Shops.controversial_third_party, promo=[])


def make_msku(id, offers, hyperid, hid=DEFAULT_HID):
    return MarketSku(sku=id, hyperid=hyperid, hid=hid, blue_offers=offers)


class _Mskus(object):
    msku_1 = make_msku(110000, [_Offers.offer_1], hyperid=2, hid=HID_1)
    msku_2 = make_msku(110001, [_Offers.offer_2], hyperid=2, hid=HID_1)
    msku_3 = make_msku(110002, [_Offers.offer_3], hyperid=3, hid=HID_2)
    msku_4 = make_msku(110004, [_Offers.offer_4], hyperid=4, hid=HID_2)
    msku_5 = make_msku(110005, [_Offers.offer_5], hyperid=5, hid=HID_2)
    msku_6 = make_msku(110006, [_Offers.offer_6], hyperid=6, hid=HID_2)
    msku_7 = make_msku(110007, [_Offers.offer_7], hyperid=7, hid=HID_2)


class _Promos(object):
    blue_3p_flash_discount = Promo(
        promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
        key='FLvklxUgdnawSJPG4UhZGA',
        shop_promo_id=1,
        feed_id=_Shops.third_party.datafeed_id,
        mskus=[
            PromoMSKU(msku=str(_Mskus.msku_1.sku), market_promo_price=500, market_old_price=1000),
        ],
        source_type=ESourceType.ANAPLAN,
    )

    blue_flash = Promo(
        promo_type=PromoType.BLUE_FLASH,
        key='BLUE_FLASH_PROMO',
        blue_flash=PromoBlueFlash(
            items=[
                {
                    'feed_id': 777,
                    'offer_id': _Offers.offer_1.offerid,
                    'price': {'value': _Offers.offer_1.price - 50, 'currency': 'RUR'},
                },
            ],
            allow_berubonus=True,
            allow_promocode=True,
        ),
        source_reference='http://source_reference.com/blue_flash',
        anaplan_id='blue_flash_anaplan_id',
        shop_promo_id='blue_flash_shop_promo_id',
        source_type=ESourceType.PARTNER_SOURCE,
    )

    disabled_promo = Promo(
        promo_type=PromoType.BLUE_FLASH,
        key='BLUE_FLASH_PROMO1',
        blue_flash=PromoBlueFlash(
            items=[
                {
                    'feed_id': 778,
                    'offer_id': _Offers.offer_2.offerid,
                    'price': {'value': _Offers.offer_2.price - 50, 'currency': 'RUR'},
                },
            ],
            allow_berubonus=True,
            allow_promocode=True,
        ),
        source_reference='http://source_reference.com/blue_flash',
        anaplan_id='blue_flash_anaplan_id',
        shop_promo_id='blue_flash_shop_promo_id1',
        source_type=ESourceType.PARTNER_SOURCE,
        force_disabled=True,
    )

    declined_by_time_not_started = Promo(
        promo_type=PromoType.BLUE_CASHBACK,
        key='Promo_declined_by_time_not_started',
        url='http://blue_cashback.com/',
        blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
        start_date=CURRENT_TIME + TIME_SHIFT,
        restrictions=PromoRestrictions(
            predicates=[
                {
                    'perks': ['yandex_employee_extra_cashback'],
                }
            ]
        ),
        promo_internal_priority=-1,
    )

    declined_by_time_its_over = Promo(
        promo_type=PromoType.BLUE_CASHBACK,
        key='Promo_declined_by_time_its_over',
        url='http://blue_cashback.com/',
        blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
        end_date=CURRENT_TIME - TIME_SHIFT,
        restrictions=PromoRestrictions(
            predicates=[
                {
                    'perks': ['yandex_employee_extra_cashback'],
                }
            ]
        ),
        promo_internal_priority=-1,
    )

    # промки, ограниченые по регионам
    whitelist_region_promo = Promo(
        promo_type=PromoType.BLUE_CASHBACK,
        key='whitelist_region_promo',
        url='http://blue_cashback.com/',
        blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
        restrictions=PromoRestrictions(
            predicates=[
                {
                    'perks': ['yandex_employee_extra_cashback'],
                }
            ],
            regions=[213, 2, 134],
            excluded_regions=[10590],
        ),
        promo_internal_priority=-1,
    )
    blacklist_region_promo = Promo(
        promo_type=PromoType.BLUE_CASHBACK,
        key='blacklist_region_promo',
        url='http://blue_cashback.com/',
        blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
        restrictions=PromoRestrictions(
            predicates=[
                {
                    'perks': ['yandex_employee_extra_cashback'],
                }
            ],
            regions=[],
            excluded_regions=[134, 157],
        ),
        promo_internal_priority=-1,
    )

    # A -> B C
    # B -> D E -- не пересекается с A
    # C -> F A -- пересекается с A

    restricted_promo_A = Promo(
        promo_type=PromoType.BLUE_CASHBACK,
        key='BLUE_CASHBACK_A',
        url='http://blue_cashback.com/',
        blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
        shop_promo_id='BLUE_CASHBACK_a',
        promo_internal_priority=112,
        promo_bucket_name='What you see is what you get. No one' 's been disappointed yet.',
        restrictions=PromoRestrictions(
            restricted_promo_types=[PromoType.SPREAD_DISCOUNT_COUNT, PromoType.DIRECT_DISCOUNT]
        ),
    )

    restricted_promo_B = Promo(
        promo_type=PromoType.SPREAD_DISCOUNT_COUNT,
        description='spread discount normal',
        feed_id=_Shops.controversial_third_party.datafeed_id,
        key='SPREAD_DISCOUNT_COUNT_B',
        url='http://spdc_1.com/',
        landing_url='http://spdc_1_landing.com/',
        shop_promo_id='spread_discount_count_single',
        spread_discount_count=PromoSpreadDiscountCount(
            items={
                _Mskus.msku_6.sku: [{'count': 3, 'percent_discount': 7}, {'count': 5, 'percent_discount': 13}],
            }
        ),
        restrictions=PromoRestrictions(restricted_promo_types=[PromoType.CHEAPEST_AS_GIFT, PromoType.BLUE_FLASH]),
    )

    restricted_promo_C = Promo(
        promo_type=PromoType.DIRECT_DISCOUNT,
        feed_id=_Shops.third_party.datafeed_id,
        key='DIRECT_DISCOUNT_PROMO_C',
        url='http://direct_discount.com/',
        direct_discount=PromoDirectDiscount(
            items=[
                {
                    'feed_id': _Shops.controversial_third_party.datafeed_id,
                    'offer_id': _Offers.offer_6.offerid,
                    'discount_price': {
                        'value': _Offers.offer_6.price - 10,
                        'currency': 'RUR',
                    },
                    'old_price': {
                        'value': 12345,
                        'currency': 'RUR',
                    },
                },
            ],
            allow_berubonus=True,
            allow_promocode=True,
        ),
        shop_promo_id='DIRECT_DISCOUNT_c',
        restrictions=PromoRestrictions(
            restricted_promo_types=[PromoType.SPREAD_DISCOUNT_RECEIPT, PromoType.BLUE_CASHBACK]
        ),
    )

    index_promo = Promo(
        promo_type=PromoType.BLUE_FLASH,
        key='BLUE_FLASH_PROMO_2',
        blue_flash=PromoBlueFlash(
            items=[
                {
                    'feed_id': 779,
                    'offer_id': _Offers.offer_7.offerid,
                    'price': {'value': _Offers.offer_7.price - 50, 'currency': 'RUR'},
                },
            ],
            allow_berubonus=False,
            allow_promocode=False,
        ),
        anaplan_id='blue_flash_anaplan_id_slow',
        shop_promo_id='blue_flash_shop_promo_id2',
        feed_id=779,
        url='http://яндекс.рф/',
    )

    fast_promo = Promo(
        promo_type=PromoType.BLUE_FLASH,
        key='BLUE_FLASH_PROMO_2',
        blue_flash=PromoBlueFlash(
            items=[
                {
                    'feed_id': 779,
                    'offer_id': _Offers.offer_7.offerid,
                    'price': {'value': _Offers.offer_7.price - 100, 'currency': 'RUR'},
                },
            ],
            allow_berubonus=False,
            allow_promocode=False,
        ),
        anaplan_id='blue_flash_anaplan_id_fast',
        shop_promo_id='blue_flash_shop_promo_id2',
        feed_id=779,
        url='http://яндекс.рф/',
        offers_matching_rules=[
            OffersMatchingRules(mskus=[_Mskus.msku_7]),
        ],
        generation_ts=100,  # признак быстрого промо
    )

    fake_promo = Promo(  # используется только в проверке отладочного вывода
        anaplan_id="Отладочная информация по быстрым промо",
        key="привязаные промо-ключи [BLUE_FLASH_PROMO_2]",
        promo_type=PromoType.NO_PROMO,
        shop_promo_id="время индекса Thu, 01 Jan 1970 03:00:00 MSK",
        generation_ts=100,
    )


class PromoData(object):
    def __init__(
        self,
        promo,
        status,
        debug_str=None,
    ):
        self.promo = promo
        self.status = status
        self.debug_str = debug_str


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']

        _Offers.offer_1.promo += [
            _Promos.blue_3p_flash_discount,
            _Promos.blue_flash,
        ]
        _Offers.offer_2.promo += [
            _Promos.disabled_promo,
        ]
        _Offers.offer_3.promo += [
            _Promos.declined_by_time_not_started,
            _Promos.declined_by_time_its_over,
        ]
        _Offers.offer_4.promo += [_Promos.whitelist_region_promo]
        _Offers.offer_5.promo += [_Promos.blacklist_region_promo]
        _Offers.offer_6.promo += [_Promos.restricted_promo_A, _Promos.restricted_promo_B, _Promos.restricted_promo_C]
        _Offers.offer_7.promo += [_Promos.index_promo]

        cls.index.shops += [_Shops.third_party, _Shops.third_party1, _Shops.controversial_third_party]

        cls.index.mskus += [
            _Mskus.msku_1,
            _Mskus.msku_2,
            _Mskus.msku_3,
            _Mskus.msku_4,
            _Mskus.msku_5,
            _Mskus.msku_6,
            _Mskus.msku_7,
        ]

        cls.index.promos += [
            _Promos.blue_3p_flash_discount,
            _Promos.blue_flash,
            _Promos.disabled_promo,
            _Promos.declined_by_time_not_started,
            _Promos.declined_by_time_its_over,
            _Promos.whitelist_region_promo,
            _Promos.blacklist_region_promo,
            _Promos.restricted_promo_A,
            _Promos.restricted_promo_B,
            _Promos.restricted_promo_C,
            _Promos.index_promo,
        ]
        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    _Promos.blue_flash.key,
                    _Promos.disabled_promo.key,
                    _Promos.declined_by_time_not_started.key,
                    _Promos.declined_by_time_its_over.key,
                    _Promos.whitelist_region_promo.key,
                    _Promos.blacklist_region_promo.key,
                    _Promos.restricted_promo_A.key,
                    _Promos.restricted_promo_B.key,
                    _Promos.restricted_promo_C.key,
                    _Promos.index_promo.key,
                    _Promos.fast_promo.key,
                ]
            )
        ]

        cls.index.regiontree += [
            Region(rid=213, name='Москва', genitive='Москвы', preposition='в ', accusative='Москву', tz_offset=10800),
            Region(rid=2, name='Санкт-Петербург', tz_offset=10800),
            Region(rid=10758, name='Химки', tz_offset=10800),
            Region(rid=157, name='Минск', tz_offset=10800),
            Region(
                rid=134,
                name='Татуин',
                tz_offset=28800,
                region_type=Region.COUNTRY,
                genitive="Татуине",
                preposition="на",
                accusative="Татуин",
                children=[
                    Region(
                        rid=10591,
                        name='Дюнное море',
                        genitive="Дюнное море",
                        preposition="в",
                        accusative="Дюнном море",
                        tz_offset=28800,
                        children=[
                            Region(
                                rid=10590,
                                name='Мос Эйсли',
                                genitive="Мос Эйсли",
                                preposition="в",
                                accusative="Мос Эйсли",
                                tz_offset=28800,
                            ),
                        ],
                    )
                ],
            ),
        ]

    def __check_prime_response_with_promo_trace(self, waremd5, promo_data, rids=0, custom_query_params=None):
        def __get_additional_query_params(waremd5):
            additional_query_params = '&rearr-factors=market_documents_search_trace={}'.format(waremd5)
            additional_query_params += ';market_promo_blue_cashback=1'
            additional_query_params += ';market_metadoc_search=no'
            additional_query_params += '&debug=1'
            return additional_query_params

        def __make_promo_fragment(promo, promo_state, debug_str=None):

            fragment = {
                'promoKey': promo.key,
                'promoType': promo.type_name,
                'promoState': promo_state,
                'sourceType': source_type_name(promo.source_type),
                'anaplanId': promo.anaplan_id or '',
                'isFastPromo': 'true' if promo.generation_ts > 0 else 'false',
                'priority': promo.promo_internal_priority if promo.promo_internal_priority else 0,
            }
            if promo.promo_type is not PromoType.NO_PROMO:
                fragment['sourceReference'] = promo.source_reference or ''
                fragment['debugStr'] = debug_str or Absent()
                fragment['promoBucketName'] = str(promo.promo_bucket_name)
                fragment['shopPromoId'] = str(
                    promo.shop_promo_id
                )  # чтоб не заморачиваться с переводом времени в разных часовых поясах
            if promo.parent_promo_id:
                fragment['parentPromoId'] = promo.parent_promo_id

            return fragment

        def __make_promo_fragments(promo_data):
            fragments = []
            for sample in promo_data:
                fragments.append(
                    __make_promo_fragment(
                        sample.promo,
                        sample.status,
                        sample.debug_str,
                    )
                )
            return fragments

        request = (
            'place=prime'
            '&rgb=blue'
            '&offerid={waremd5}'
            '&rids={rids}'
            '&regset=1'
            '&pp=18'
            '{additional_query_params}'
        )
        if custom_query_params is not None:
            request += "{}".format(custom_query_params)

        response = self.report.request_json(
            request.format(waremd5=waremd5, rids=rids, additional_query_params=__get_additional_query_params(waremd5))
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'docs_search_trace': {
                        'traces': [{'promos': __make_promo_fragments(promo_data)}],
                    },
                },
            },
            allow_different_len=False,
        )

    def test_promo_trace(self):
        '''
        Проверяем что в отладочной выдаче есть информация о промоакциях, в которых участвует оффер
        '''

        promo_data = [
            PromoData(
                _Promos.blue_flash,
                'Active',
                'promo_price 450, offer_base_price 500, discount_percent 10%, discount_value 50, percent_threshold 5%, absolute_threshold 500',
            ),
            PromoData(_Promos.blue_3p_flash_discount, 'DeclinedByIncompatibleMultiPromo'),
            PromoData(direct_discount, 'DeclinedByDirectDiscountForCategoryThreshold'),
            PromoData(blue_cashback, 'DeclinedByPerk'),
        ]

        self.__check_prime_response_with_promo_trace(waremd5=_Offers.offer_1.waremd5, promo_data=promo_data)

    def test_force_disabled(self):
        '''
        Проверяем что в отладочной выдаче есть информация о промоакциях, которые отключены
        '''

        promo_data = [
            PromoData(
                _Promos.disabled_promo,
                'ForceDisabled',
            ),
        ]

        self.__check_prime_response_with_promo_trace(waremd5=_Offers.offer_2.waremd5, promo_data=promo_data)

    def test_declined_by_time(self):
        '''
        Проверяем отключение промо по дате: промо либо еще не началось, либо уже кончилось
        '''

        promo_data = [
            PromoData(
                _Promos.declined_by_time_not_started,
                'DeclinedByTimeNotStarted',
                'Акция еще не началась: вреня начала акции {start_time}, а время запроса {now_time}'.format(
                    start_time=time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(REQUEST_TIMESTAMP + ONE_DAY_SEC)),
                    now_time=time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(REQUEST_TIMESTAMP)),
                ),
            ),
            PromoData(
                _Promos.declined_by_time_its_over,
                'DeclinedByTimeItsOver',
                'Акция уже закончилась: время окончания акции {end_time}, а время запроса {now_time}'.format(
                    end_time=time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(REQUEST_TIMESTAMP - ONE_DAY_SEC)),
                    now_time=time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(REQUEST_TIMESTAMP)),
                ),
            ),
        ]

        custom_query_params = '&rearr-factors=market_promo_blue_cashback=1;market_promo_blue_cashback_on_white=1;market_use_promo_predicate_enhancement=1&perks=yandex_cashback,yandex_employee_extra_cashback'  # noqa

        self.__check_prime_response_with_promo_trace(
            waremd5=_Offers.offer_3.waremd5, promo_data=promo_data, custom_query_params=custom_query_params
        )

    def test_declined_by_region(self):
        '''
        Проверяем что в отладочной выдаче есть информация о промоакциях, не подошедших по региону,
        есть два варианта: регион в белом списке промо, региона в черном списке промо
        '''

        promo_data = [
            PromoData(
                _Promos.whitelist_region_promo,
                'DeclinedByRegion',
                'Региона пользователя Химки (10758) нет в списке разрешённых регионов',
            ),
        ]

        self.__check_prime_response_with_promo_trace(
            waremd5=_Offers.offer_4.waremd5,
            promo_data=promo_data,
            rids=10758,
            custom_query_params='&rearr-factors=market_promo_blue_cashback=1;market_promo_blue_cashback_on_white=1;market_use_promo_predicate_enhancement=1;cpa_enabled_countries=134'
            '&perks=yandex_cashback,yandex_employee_extra_cashback',
        )

        promo_data = [
            PromoData(
                _Promos.blacklist_region_promo,
                'DeclinedByRegion',
                'Регион пользователя Минск (157) который есть в списке запрещенных регионов',
            ),
        ]

        self.__check_prime_response_with_promo_trace(
            waremd5=_Offers.offer_5.waremd5,
            promo_data=promo_data,
            rids=157,
            custom_query_params='&rearr-factors=market_promo_blue_cashback=1;market_promo_blue_cashback_on_white=1;market_use_promo_predicate_enhancement=1;cpa_enabled_countries=134'
            '&perks=yandex_cashback,yandex_employee_extra_cashback',
        )

        promo_data = [
            PromoData(
                _Promos.blacklist_region_promo,
                'DeclinedByRegion',
                'Регион пользователя Мос Эйсли (10590) находится в Дюнное море (10591) который находится в Татуин (134), который есть в списке запрещенных регионов',
            ),
        ]

        self.__check_prime_response_with_promo_trace(
            waremd5=_Offers.offer_5.waremd5,
            promo_data=promo_data,
            rids=10590,
            custom_query_params='&rearr-factors=market_promo_blue_cashback=1;market_promo_blue_cashback_on_white=1;market_use_promo_predicate_enhancement=1;cpa_enabled_countries=134'
            '&perks=yandex_cashback,yandex_employee_extra_cashback',
        )

    def test_mutually_restricted_promos(self):
        '''
        Проверяем, что работает взаимное исключение промо по несовместитым типам промо,
         и что работает старая версия исключения промо, в одностороннем порядке
        '''

        promo_data = [
            PromoData(
                _Promos.restricted_promo_A,
                'Active',
                '',
            ),
            PromoData(
                _Promos.restricted_promo_B,
                'Active',
                '',
            ),
            PromoData(
                _Promos.restricted_promo_C,
                'DeclinedByRestrictedPromoType',
                'Промо взаимно несовместимо с промо c ключем {key} и shopPromoId {shopPromoId} типа {type} которое имеет больший приоритет'.format(
                    key=_Promos.restricted_promo_A.key,
                    shopPromoId=_Promos.restricted_promo_A.shop_promo_id,
                    type=_Promos.restricted_promo_A.promo_type,
                ),
            ),
        ]

        # сначала проверяем, что работает новая функциональность
        self.__check_prime_response_with_promo_trace(
            waremd5=_Offers.offer_6.waremd5,
            promo_data=promo_data,
            custom_query_params='&rearr-factors=market_promo_blue_cashback=1;market_promo_blue_cashback_on_white=1;market_use_promo_predicate_enhancement=1;cpa_enabled_countries=134;'
            '&perks=yandex_cashback,yandex_employee_extra_cashback',
        )

        promo_data = [
            PromoData(
                _Promos.restricted_promo_A,
                'Active',
                '',
            ),
            PromoData(
                _Promos.restricted_promo_B,
                'Active',
                '',
            ),
            PromoData(
                _Promos.restricted_promo_C,
                'Active',
                'discount_price 490, old_price 12345, discount_percent 96, discount_value 11855, percent_threshold: 5%, absolute_threshold: 500 rubles',
            ),
        ]

        # зетем проверяем, что с выключенным флагом работает по старому
        self.__check_prime_response_with_promo_trace(
            waremd5=_Offers.offer_6.waremd5,
            promo_data=promo_data,
            custom_query_params='&rearr-factors=market_promo_blue_cashback=1;market_promo_blue_cashback_on_white=1;market_use_promo_predicate_enhancement=1;cpa_enabled_countries=134;'
            'market_promo_mutually_restrictions_enable=0;'
            '&perks=yandex_cashback,yandex_employee_extra_cashback',
        )

    def test_fast_promo_marker(self):
        '''
        Проверяем, что в трассировке появился новый признак: является ли промо бытсрым
        '''
        promo_data = [
            PromoData(
                _Promos.index_promo,
                'Active',
                'promo_price 450, offer_base_price 500, discount_percent 10%, discount_value 50, percent_threshold 5%, absolute_threshold 500',
            ),
        ]

        self.__check_prime_response_with_promo_trace(
            waremd5=_Offers.offer_7.waremd5,
            promo_data=promo_data,
            custom_query_params='&rearr-factors=market_promo_blue_cashback=1;market_promo_blue_cashback_on_white=1;market_use_promo_predicate_enhancement=1;cpa_enabled_countries=134'
            '&perks=yandex_cashback,yandex_employee_extra_cashback',
        )

        # довозим быстрое промо
        self.dynamic.qpromos += [DynamicQPromos([_Promos.fast_promo])]

        # проверяем, что промо стало быстрым
        promo_data = [
            PromoData(
                _Promos.index_promo,
                'DeclinedByFastPromoOverride',
                'Promo BLUE_FLASH_PROMO_2 has been overridden by fastpromo (shop_promo_id=blue_flash_shop_promo_id2, promoKey=BLUE_FLASH_PROMO_2)',
            ),
            PromoData(
                _Promos.fake_promo,
                'Allowed',
                '',
            ),
            PromoData(
                _Promos.fast_promo,
                'Active',
                'promo_price 400, offer_base_price 500, discount_percent 20%, discount_value 100, percent_threshold 5%, absolute_threshold 500',
            ),
        ]

        self.__check_prime_response_with_promo_trace(
            waremd5=_Offers.offer_7.waremd5,
            promo_data=promo_data,
            custom_query_params='&rearr-factors=market_promo_blue_cashback=1;market_promo_blue_cashback_on_white=1;market_use_promo_predicate_enhancement=1;cpa_enabled_countries=134'
            ';enable_fast_promo_matcher=1;enable_fast_promo_matcher_test=0'
            '&perks=yandex_cashback,yandex_employee_extra_cashback',
        )


if __name__ == '__main__':
    main()
