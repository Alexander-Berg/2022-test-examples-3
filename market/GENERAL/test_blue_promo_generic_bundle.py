#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, NotEmpty, EqualToOneOf
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types import (
    CategoryStatsRecord,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicBlueGenericBundlesPromos,
    DynamicDeliveryServiceInfo,
    DynamicSkuOffer,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    GradeDispersionItem,
    Model,
    OfferDimensions,
    PhotoDataItem,
    Promo,
    PromoType,
    Region,
    RegionalDelivery,
    ReviewDataItem,
    Shop,
    make_generic_bundle_promo,
)
from core.types.dynamic_filters import DynamicBluePromosBlacklist
from core.types.sku import MarketSku, BlueOffer
from core.types.offer_promo import make_generic_bundle_content, OffersMatchingRules


from datetime import datetime, timedelta
import copy


RID_RUSSIA = 213
WH_1 = 11
CARRIER_1 = 1
BUCKET_1 = 71

BLUE = 'blue'
GREEN = 'green'
GREEN_WITH_BLUE = 'green_with_blue'


def get_warehouse_and_delivery_service(warehouse_id, service_id, enabled=True):
    date_switch_hours = [
        DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=RID_RUSSIA),
    ]
    return DynamicWarehouseAndDeliveryServiceInfo(
        warehouse_id=warehouse_id,
        delivery_service_id=service_id,
        operation_time=0,
        date_switch_time_infos=date_switch_hours,
        shipment_holidays_days_set_key=6,
        is_active=enabled,
    )


# ведущие офферы акций
blue_offer_primary1 = BlueOffer(
    waremd5='BlueOffer1-Primary---w',
    price=1000,
    price_old=1100,  # необходимо для того чтобы оффер попал в выдачу в place deals
    feedid=777,
    offerid='shop_sku_1.яЯя',
    weight=1,
    dimensions=OfferDimensions(length=10, width=10, height=10),
)

blue_offer_primary1_1 = BlueOffer(
    waremd5='BlueOffer1-Primary-1-w',
    price=1000,
    feedid=777,
    offerid='shop_sku_1_1.яЯя',
)

blue_offer_primary2 = BlueOffer(
    waremd5='BlueOffer2-Primary---w',
    price=1000,
    feedid=777,
    offerid='shop_sku_2',
)

blue_offer_primary3 = BlueOffer(
    waremd5='BlueOffer3-Primary---w',
    price=1000,
    feedid=777,
    offerid='shop_sku_3',
)

blue_offer_primary4 = BlueOffer(
    waremd5='BlueOffer4-Primary---w',
    price=1000,
    feedid=777,
    offerid='shop_sku_4',
)

blue_offer_primary5 = BlueOffer(
    waremd5='BlueOffer5-Primary---w',
    price=2000,
    feedid=777,
    offerid='shop_sku_5',
)

blue_offer_primary6 = BlueOffer(
    waremd5='BlueOffer6-Primary---w',
    price=2000,
    feedid=777,
    offerid='shop_sku_6',
)

blue_offer_primary7 = BlueOffer(
    price=3500,
    feedid=777,
    offerid='birdie',
    is_fulfillment=False,
    waremd5='DeliveredByShopToo___g',
)

blue_offer_primary8 = BlueOffer(
    waremd5='BlueOffer8-Primary---w',
    price=10000,
    feedid=777,
    offerid='shop_sku_8',
)

blue_offer_primary9 = BlueOffer(
    waremd5='BlueOffer9-Primary---w',
    price=10333,
    feedid=777,
    offerid='shop_sku_9',
)

blue_offer_primary10 = BlueOffer(
    waremd5='BlueOffer10Primary---w',
    price=10,
    feedid=777,
    offerid='shop_sku_10',
)

blue_offer_primary11 = BlueOffer(
    waremd5='BlueOffer11Primary---w',
    price=11,
    feedid=777,
    offerid='shop_sku_11',
)

blue_offer_primary12 = BlueOffer(
    waremd5='BlueOffer12Primary---w',
    price=5000,
    feedid=777,
    offerid='shop_sku_12',
)

blue_offer_primary13 = BlueOffer(
    waremd5='BlueOffer13Primary---w',
    price=5000,
    feedid=777,
    offerid='shop_sku_13',
)

blue_offer_primary14 = BlueOffer(
    waremd5='BlueOffer14Primary---w',
    price=11,
    feedid=777,
    offerid='shop_sku_14',
)

blue_offer_primary15 = BlueOffer(
    waremd5='BlueOffer15Primary---w',
    price=11,
    feedid=777,
    offerid='shop_sku_15',
)

blue_offer_primary16 = BlueOffer(
    waremd5='BlueOffer16Primary---w',
    price=11,
    feedid=777,
    offerid='shop_sku_16',
)

blue_offer_primary19 = BlueOffer(
    waremd5='BlueOffer19Primary---w',
    price=1000,
    feedid=777,
    offerid='shop_sku_19',
)

blue_offer_primary20 = BlueOffer(
    waremd5='BlueOffer20Primary---w',
    price=1000,
    feedid=777,
    offerid='shop_sku_20',
)

blue_offer_primary21 = BlueOffer(
    waremd5='BlueOffer21Primary---w',
    price=1000,
    feedid=777,
    offerid='shop_sku_21',
)

blue_offer_primary22 = BlueOffer(
    waremd5='BlueOffer22Primary---w',
    price=1000,
    feedid=778,
    offerid='shop_sku_22',
)


# подарок
blue_offer_secondary1 = BlueOffer(
    waremd5='BlueOffer1-Secondary-w',
    price=11,
    feedid=777,
    offerid='shop_sku_gift?!ыыЫЫ',
    weight=1,
    dimensions=OfferDimensions(length=10, width=10, height=10),
)

blue_offer_secondary1_1 = BlueOffer(
    waremd5='BlueOffer1-Secondary1w',
    price=11,
    feedid=777,
    offerid='shop_sku_gift_11?!ыыЫЫ',
)

blue_offer_secondary2 = BlueOffer(
    waremd5='BlueOffer2-Secondary-w',
    price=22,
    feedid=777,
    offerid='фы-.^2^.-ва',
)

blue_offer_secondary8 = BlueOffer(
    waremd5='BlueOffer8-Secondary-w',
    price=2000,
    feedid=777,
    offerid='фы-.^8^.-ва',
)

blue_offer_secondary9 = BlueOffer(
    waremd5='BlueOffer9-Secondary-w',
    price=1333,
    feedid=777,
    offerid='gift9',
)

blue_offer_secondary10 = BlueOffer(
    waremd5='BlueOffer10Secondary-w',
    price=1333,
    feedid=777,
    offerid='gift10',
)

blue_offer_secondary11 = BlueOffer(
    waremd5='BlueOffer11Secondary-w',
    price=1333,
    feedid=777,
    offerid='gift11',
)

blue_offer_secondary12 = BlueOffer(
    waremd5='BlueOffer12Secondary-w',
    price=5000,
    feedid=777,
    offerid='gift12',
)

blue_offer_secondary13 = BlueOffer(
    waremd5='BlueOffer13Secondary-w',
    price=1333,
    feedid=777,
    offerid='gift13',
)

blue_offer_secondary15 = BlueOffer(
    waremd5='BlueOffer15Secondary-w',
    price=33,
    feedid=777,
    offerid='gift15',
)

blue_offer_secondary16 = BlueOffer(
    waremd5='BlueOffer16Secondary-w',
    price=33,
    feedid=777,
    offerid='gift16',
)


msku_primary1 = MarketSku(hyperid=1, sku=110011, blue_offers=[blue_offer_primary1], title='msku1')

msku_primary1_1 = MarketSku(hyperid=4, sku=1100111, blue_offers=[blue_offer_primary1_1])

msku_primary2 = MarketSku(hyperid=2, sku=110012, blue_offers=[blue_offer_primary2])

msku_primary3 = MarketSku(hyperid=2, sku=110013, waremd5='MarketSku3-IiLVm1goleg', blue_offers=[blue_offer_primary3])

msku_primary4 = MarketSku(title='blue market sku4', hyperid=2, sku=110014, blue_offers=[blue_offer_primary4])

msku_primary5 = MarketSku(hyperid=2, sku=110015, blue_offers=[blue_offer_primary5])

msku_primary6 = MarketSku(hyperid=2, sku=110016, blue_offers=[blue_offer_primary6])

msku_primary7 = MarketSku(
    title='blue market sku7',
    hyperid=2,
    sku=110017,
    blue_offers=[blue_offer_primary7],
    delivery_buckets=[BUCKET_1],
)

msku_primary8 = MarketSku(
    title='blue market sku8',
    hyperid=2,
    sku=110018,
    blue_offers=[blue_offer_primary8],
)

msku_primary9 = MarketSku(
    title='blue market sku9',
    hyperid=2,
    sku=110019,
    blue_offers=[blue_offer_primary9],
)

msku_primary10 = MarketSku(
    title='blue market sku10',
    hyperid=2,
    sku=110020,
    blue_offers=[blue_offer_primary10],
)

msku_primary11 = MarketSku(
    title='blue market sku11',
    hyperid=2,
    sku=110021,
    blue_offers=[blue_offer_primary11],
)

msku_primary12 = MarketSku(
    title='blue market sku12',
    hyperid=2,
    sku=110022,
    blue_offers=[blue_offer_primary12],
)

msku_primary13 = MarketSku(
    title='blue market sku13',
    hyperid=2,
    sku=110023,
    blue_offers=[blue_offer_primary13],
)

msku_primary14 = MarketSku(
    title='blue market sku14',
    hyperid=2,
    sku=110024,
    blue_offers=[blue_offer_primary14],
)

msku_primary15 = MarketSku(
    title='blue market sku15',
    hyperid=2,
    sku=110025,
    blue_offers=[blue_offer_primary15],
)

msku_primary16 = MarketSku(
    title='blue market sku16',
    hyperid=2,
    sku=110026,
    blue_offers=[blue_offer_primary16],
)

msku_primary18 = MarketSku(
    title='blue market sku18',
    hyperid=2,
    sku=110028,
    blue_offers=[blue_offer_primary19, blue_offer_primary20],
)

msku_primary19 = MarketSku(
    title='blue market sku19',
    hyperid=2,
    sku=110029,
    blue_offers=[blue_offer_primary21, blue_offer_primary22],
)


msku_secondary1 = MarketSku(title='blue market sku sec1', hyperid=2, sku=220011, blue_offers=[blue_offer_secondary1])

msku_secondary1_1 = MarketSku(
    title='blue market sku sec1_1', hyperid=2, sku=2200111, blue_offers=[blue_offer_secondary1_1]
)

msku_secondary2 = MarketSku(title='blue market sku sec2', hyperid=2, sku=220022, blue_offers=[blue_offer_secondary2])

msku_secondary8 = MarketSku(title='blue market sku sec8', hyperid=2, sku=220088, blue_offers=[blue_offer_secondary8])

msku_secondary9 = MarketSku(title='blue market sku sec9', hyperid=2, sku=220099, blue_offers=[blue_offer_secondary9])

msku_secondary10 = MarketSku(title='blue market sku sec10', hyperid=2, sku=220100, blue_offers=[blue_offer_secondary10])

msku_secondary11 = MarketSku(title='blue market sku sec11', hyperid=2, sku=220111, blue_offers=[blue_offer_secondary11])

msku_secondary12 = MarketSku(
    title='blue market sku sec12', hyperid=2, sku=22000012, blue_offers=[blue_offer_secondary12]
)

msku_secondary13 = MarketSku(title='blue market sku sec13', hyperid=2, sku=220122, blue_offers=[blue_offer_secondary13])

msku_secondary15 = MarketSku(title='blue market sku sec15', hyperid=2, sku=220125, blue_offers=[blue_offer_secondary15])

msku_secondary16 = MarketSku(title='blue market sku sec16', hyperid=2, sku=220126, blue_offers=[blue_offer_secondary16])


now = datetime.fromtimestamp(REQUEST_TIMESTAMP)  # нет рандома - нет нормального времени
delta_big = timedelta(days=1)
delta_small = timedelta(hours=5)  # похоже что лайт-тесты криво работают с временной зоной…
gifts = [{"item": {"offer_id": blue_offer_secondary1.offerid, "count": 1}, "value": 1, "currency": "RUB"}]

gifts2 = [{"offer_id": blue_offer_secondary2.offerid, "count": 1, "value": 5, "currency": "RUB"}]

gifts3 = [{"offer_id": "NON-EXISTENT", "count": 1, "value": 1, "currency": "RUB"}]

gifts8 = [{"offer_id": blue_offer_secondary8.offerid, "count": 1, "value": 0, "currency": "RUB"}]

gifts9 = [{"offer_id": blue_offer_secondary9.offerid, "count": 1, "value": 0, "currency": "RUB"}]

# gifts12 = [{"offer_id": blue_offer_secondary12.offerid, "count": 1, "value": 0, "currency": "RUB"}]

gifts15 = [{"offer_id": blue_offer_secondary15.offerid, "count": 1, "value": 0, "currency": "RUB"}]


# действующая акция
promo1 = Promo(
    promo_type=PromoType.GENERIC_BUNDLE,
    feed_id=777,
    key='JVvklxUgdnawSJPG4UhZ-1',
    url='http://localhost.ru/',
    generic_bundles_content=[
        make_generic_bundle_content(blue_offer_primary1.offerid, blue_offer_secondary1.offerid, 1),
        make_generic_bundle_content(blue_offer_primary1_1.offerid, blue_offer_secondary1_1.offerid),
    ],
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary1.offerid],
                [777, blue_offer_primary1_1.offerid],
            ]
        ),
    ],
)

# уже закончилась
promo2 = Promo(
    promo_type=PromoType.GENERIC_BUNDLE,
    feed_id=777,
    key='JVvklxUgdnawSJPG4UhZ-2',
    url='http://localhost.ru/',
    end_date=now - delta_small,
    generic_bundles_content=[
        make_generic_bundle_content(blue_offer_primary2.offerid, blue_offer_secondary1.offerid),
    ],
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary2.offerid],
            ]
        ),
    ],
)

# ещё не началась
promo3 = Promo(
    promo_type=PromoType.GENERIC_BUNDLE,
    feed_id=777,
    key='JVvklxUgdnawSJPG4UhZ-3',
    url='http://localhost.ru/',
    start_date=now + delta_small,
    generic_bundles_content=[
        make_generic_bundle_content(blue_offer_primary3.offerid, blue_offer_secondary1.offerid),
    ],
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary3.offerid],
            ]
        ),
    ],
)

# нет в белом списке от loyalty
promo4 = Promo(
    promo_type=PromoType.GENERIC_BUNDLE,
    feed_id=777,
    key='JVvklxUgdnawSJPG4UhZ-4',
    url='http://localhost.ru/',
    generic_bundles_content=[
        make_generic_bundle_content(blue_offer_primary4.offerid, blue_offer_secondary1.offerid),
    ],
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary4.offerid],
            ]
        ),
    ],
)

# одинаковый подарок для двух разных ведущих товаров
promo5 = Promo(
    promo_type=PromoType.GENERIC_BUNDLE,
    feed_id=777,
    key='JVvklxUgdnawSJPG4UhZ-5',
    url='http://localhost.ru/',
    generic_bundles_content=[
        make_generic_bundle_content(blue_offer_primary5.offerid, blue_offer_secondary2.offerid),
        make_generic_bundle_content(blue_offer_primary6.offerid, blue_offer_secondary2.offerid),
    ],
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary5.offerid],
                [777, blue_offer_primary6.offerid],
            ]
        ),
    ],
)

# не существующий в индексе подарок
promo6 = Promo(
    promo_type=PromoType.GENERIC_BUNDLE,
    feed_id=777,
    key='JVvklxUgdnawSJPG4UhZ-6',
    url='http://localhost.ru/',
    generic_bundles_content=[
        make_generic_bundle_content(blue_offer_primary1.offerid, "NON-EXISTENT"),
    ],
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary1.offerid],
            ]
        ),
    ],
)

# промо для другой схемы feed_offer_id
promo7 = Promo(
    promo_type=PromoType.GENERIC_BUNDLE,
    feed_id=777,
    key='JVvklxUgdnawSJPG4UhZ-7',
    url='http://localhost.ru/7',
    generic_bundles_content=[
        make_generic_bundle_content(blue_offer_primary7.offerid, blue_offer_secondary1.offerid),
    ],
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_primary7,
            ]
        ),
    ],
)

# 2 промо для проверки флага ограничения возвратов
promo8 = Promo(
    promo_type=PromoType.GENERIC_BUNDLE,
    feed_id=777,
    key='JVvklxUgdnawSJPG4UhZ-8',
    url='http://localhost.ru/8',
    generic_bundles_content=[
        make_generic_bundle_content(blue_offer_primary8.offerid, blue_offer_secondary8.offerid, spread_discount=90),
    ],
    restrict_refund=True,
    spread_discount=-1,  # тест на то что новое значение имеет приоритет
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary8.offerid],
            ]
        ),
    ],
)

promo9 = Promo(
    promo_type=PromoType.GENERIC_BUNDLE,
    feed_id=777,
    key='JVvklxUgdnawSJPG4UhZ-9',
    url='http://localhost.ru/9',
    generic_bundles_content=[
        make_generic_bundle_content(blue_offer_primary9.offerid, blue_offer_secondary9.offerid),
    ],
    spread_discount=90.55,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary9.offerid],
            ]
        ),
    ],
)

# 2 промо для проверки флагов взаимодействия с другими промо
promo10 = Promo(
    promo_type=PromoType.GENERIC_BUNDLE,
    feed_id=777,
    key='VvklxUgdnawSJPG4UhZ-10',
    url='http://localhost.ru/',
    generic_bundles_content=[
        make_generic_bundle_content(blue_offer_primary10.offerid, blue_offer_secondary1.offerid),
    ],
    allow_promocode=False,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary10.offerid],
            ]
        ),
    ],
)

promo11 = Promo(
    promo_type=PromoType.GENERIC_BUNDLE,
    feed_id=777,
    key='VvklxUgdnawSJPG4UhZ-11',
    url='http://localhost.ru/',
    generic_bundles_content=[
        make_generic_bundle_content(blue_offer_primary11.offerid, blue_offer_secondary8.offerid),
    ],
    allow_berubonus=False,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary11.offerid],
            ]
        ),
    ],
)

# 2 промо для проверки граничных значений процента распределения скидки
promo12 = Promo(
    promo_type=PromoType.GENERIC_BUNDLE,
    feed_id=777,
    key='VvklxUgdnawSJPG4UhZ-12',
    url='http://localhost.ru/',
    generic_bundles_content=[
        make_generic_bundle_content(blue_offer_primary12.offerid, blue_offer_secondary12.offerid, 0),
    ],
    spread_discount=0.0,
    allow_promocode=False,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary12.offerid],
            ]
        ),
    ],
)

promo13 = Promo(
    promo_type=PromoType.GENERIC_BUNDLE,
    feed_id=777,
    key='VvklxUgdnawSJPG4UhZ-13',
    url='http://localhost.ru/',
    generic_bundles_content=[
        make_generic_bundle_content(blue_offer_primary13.offerid, blue_offer_secondary12.offerid, 0),
    ],
    spread_discount=100.0,
    allow_berubonus=False,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary13.offerid],
            ]
        ),
    ],
)

promo14 = make_generic_bundle_promo(
    primary_offer_ids=[blue_offer_primary14.offerid, blue_offer_primary15.offerid, blue_offer_primary16.offerid],
    secondary_offer_ids=[
        blue_offer_secondary10.offerid,
        blue_offer_secondary11.offerid,
        blue_offer_secondary13.offerid,
    ],
    feed_id=777,
    key='VvklxUgdnawSJPG4UhZ-14',
    now_timestamp=REQUEST_TIMESTAMP,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary14.offerid],
                [777, blue_offer_primary15.offerid],
                [777, blue_offer_primary16.offerid],
            ]
        ),
    ],
)

# промо для тестирования 2 ССКУ в одном МСКУ с одинаковыми подарками (+мульти-оффер)
promo16 = make_generic_bundle_promo(
    primary_offer_ids=[blue_offer_primary19.offerid, blue_offer_primary20.offerid],
    secondary_offer_ids=[blue_offer_secondary15.offerid, blue_offer_secondary15.offerid],
    feed_id=777,
    key='VvklxUgdnawSJPG4UhZ-16',
    now_timestamp=REQUEST_TIMESTAMP,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary19.offerid],
                [777, blue_offer_primary20.offerid],
            ]
        ),
    ],
)

# промо для тестирования дефолтного оффера в productoffers
promo17 = make_generic_bundle_promo(
    primary_offer_ids=[blue_offer_primary21.offerid],
    secondary_offer_ids=[blue_offer_secondary16.offerid],
    feed_id=777,
    key='VvklxUgdnawSJPG4UhZ-17',
    now_timestamp=REQUEST_TIMESTAMP,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [777, blue_offer_primary21.offerid],
            ]
        ),
    ],
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=1;enable_fast_promo_matcher_test=1']
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']
        cls.settings.rgb_blue_is_cpa = True

        def __make_virtual_secondary_promo(promo):
            import copy

            vp = copy.deepcopy(promo)
            vp.key += 'secondary'  # произвольный суффикс чтобы контроль уникальности не ругался
            vp.promo_type = PromoType.GENERIC_BUNDLE_SECONDARY
            return vp

        # виртуальные промо для вторички (не пишутся в mmap!)
        sp1 = __make_virtual_secondary_promo(promo1)
        sp2 = __make_virtual_secondary_promo(promo2)
        sp3 = __make_virtual_secondary_promo(promo3)
        sp4 = __make_virtual_secondary_promo(promo4)
        sp7 = __make_virtual_secondary_promo(promo7)
        sp8 = __make_virtual_secondary_promo(promo8)
        sp9 = __make_virtual_secondary_promo(promo9)
        sp10 = __make_virtual_secondary_promo(promo10)
        sp11 = __make_virtual_secondary_promo(promo11)
        sp12 = __make_virtual_secondary_promo(promo12)
        sp13 = __make_virtual_secondary_promo(promo13)
        sp14 = __make_virtual_secondary_promo(promo14)

        blue_offer_primary1.promo = [promo1, promo6]
        blue_offer_secondary1.promo = [sp1, sp3, sp4, sp7, sp8]
        blue_offer_primary1_1.promo = [promo1]
        blue_offer_secondary1_1.promo = [sp1]
        blue_offer_primary2.promo = [promo2]
        blue_offer_secondary2.promo = [sp2]
        blue_offer_primary3.promo = [promo3]
        blue_offer_primary4.promo = [promo4]
        blue_offer_primary5.promo = [promo5]
        blue_offer_primary6.promo = [promo5]
        blue_offer_primary7.promo = [promo7]
        blue_offer_primary8.promo = [promo8]
        blue_offer_secondary8.promo = [sp8, sp11]
        blue_offer_primary9.promo = [promo9]
        blue_offer_secondary9.promo = [sp9]
        blue_offer_primary10.promo = [promo10]
        blue_offer_secondary10.promo = [sp10, sp14]
        blue_offer_primary11.promo = [promo11]
        blue_offer_secondary11.promo = [sp11]
        blue_offer_primary12.promo = [promo12]
        blue_offer_secondary12.promo = [sp12, sp13]
        blue_offer_primary13.promo = [promo13]
        blue_offer_secondary13.promo = [sp13]
        blue_offer_primary14.promo = [promo14]
        blue_offer_primary15.promo = [promo14]
        blue_offer_secondary15.promo = [sp14]
        blue_offer_primary16.promo = [promo14]
        blue_offer_primary19.promo = [promo16]
        blue_offer_primary20.promo = [promo16]
        blue_offer_primary21.promo = [promo17]

        cls.settings.loyalty_enabled = True

        cls.index.shops += [
            Shop(
                fesh=777,
                datafeed_id=777,
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=WH_1,
            ),
            Shop(
                fesh=778,
                datafeed_id=778,
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=WH_1,
            ),
        ]
        cls.index.regiontree += [
            Region(rid=RID_RUSSIA, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
        ]
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=WH_1, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehousesPriorityInRegion(
                region=RID_RUSSIA,
                warehouses=[
                    WH_1,
                ],
            ),
            get_warehouse_and_delivery_service(WH_1, CARRIER_1),
            DynamicDeliveryServiceInfo(CARRIER_1, "B_" + str(CARRIER_1)),
        ]
        cls.index.lms = copy.deepcopy(cls.dynamic.lms)

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=BUCKET_1,
                dc_bucket_id=BUCKET_1,
                carriers=[CARRIER_1],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=RID_RUSSIA, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                ],
            )
        ]
        cls.delivery_calc.on_request_offer_buckets(weight=2, height=22, length=11, width=11, warehouse_id=WH_1).respond(
            [BUCKET_1], [], []
        )

        cls.index.mskus += [
            msku_primary1,
            msku_primary1_1,
            msku_primary2,
            msku_primary3,
            msku_primary4,
            msku_primary5,
            msku_primary6,
            msku_primary7,
            msku_primary8,
            msku_primary9,
            msku_primary10,
            msku_primary11,
            msku_primary12,
            msku_primary13,
            msku_primary14,
            msku_primary15,
            msku_primary16,
            msku_primary18,
            msku_primary19,
            msku_secondary1,
            msku_secondary1_1,
            msku_secondary2,
            msku_secondary8,
            msku_secondary9,
            msku_secondary10,
            msku_secondary11,
            msku_secondary12,
            msku_secondary13,
            msku_secondary15,
            msku_secondary16,
        ]

        cls.index.models += [
            Model(hyperid=msku_primary1.hyperid, hid=msku_primary1.hyperid, title='blue model 1'),
            Model(hyperid=msku_primary2.hyperid, hid=msku_primary2.hyperid, title='blue model 2'),
        ]

        cls.index.promos += [
            promo1,
            promo2,
            promo3,
            promo4,
            promo5,
            promo6,
            promo7,
            promo8,
            promo9,
            promo10,
            promo11,
            promo12,
            promo13,
            promo14,
            promo16,
            promo17,
        ]

        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    promo1.key,
                    promo2.key,
                    promo3.key,
                    promo5.key,
                    promo6.key,
                    promo8.key,
                    promo9.key,
                    promo10.key,
                    promo11.key,
                    promo12.key,
                    promo13.key,
                    promo14.key,
                    promo16.key,
                    promo17.key,
                ]
            )
        ]

        # personal categories ordering
        ichwill_answer = {
            'models': [str(msku_primary1.hyperid)],
            'timestamps': [1],
        }
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1').respond(ichwill_answer)
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1', item_count=40, with_timestamps=True
        ).respond(ichwill_answer)
        cls.index.blue_category_region_stat += [
            CategoryStatsRecord(
                msku_primary1.hyperid, 213, n_offers=3, n_discounts=3
            ),  # необходимо для того чтобы оффер попал в выдачу в place deals
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(
                review_id=14808001,
                model_id=1,
                author_id=12345,
                region_id=213,
                cpa=False,
                anonymous=0,
                usage_time=1,
                pro='Хорошие ботинки',
                contra='Но пахнут гуталином',
                cr_time='1970-01-01T03:00:00',
                short_text='Ходят и ладно',
                agree=5,
                reject=1,
                total_votes=6,
                grade_value=4,
                photos=[PhotoDataItem(group_id=0, image_name="12345"), PhotoDataItem(group_id=1, image_name="67890")],
                most_useful=1,
            )
        ]

        cls.index.model_grade_dispersion_data += [GradeDispersionItem(model_id=1)]

    def primary_offer_in_generic_bundle(self):
        return {
            'entity': 'offer',
            'wareId': blue_offer_primary1.waremd5,
            'promos': [
                {
                    'type': promo1.type_name,
                    'key': promo1.key,
                    'url': 'http://localhost.ru/',
                    'startDate': NotEmpty() if promo1.start_date else Absent(),
                    'endDate': NotEmpty() if promo1.end_date else Absent(),
                    'itemsInfo': {
                        'additionalOffers': [
                            {
                                'totalOldPrice': {
                                    'value': str(blue_offer_primary1.price + blue_offer_secondary1.price),
                                    'currency': 'RUR',
                                },
                                'totalPrice': {'value': str(blue_offer_primary1.price), 'currency': 'RUR'},
                                'offer': {
                                    'price': {'value': str(gifts[0].get('value')), 'currency': 'RUR'},
                                    'offerId': blue_offer_secondary1.waremd5,
                                    "showUid": NotEmpty(),
                                    "feeShow": NotEmpty(),
                                    "entity": "showPlace",
                                    "urls": NotEmpty(),
                                },
                            }
                        ],
                        'constraints': NotEmpty(),
                    },
                }
            ],
        }

    def secondary_offer_in_generic_bundle(self):
        return {
            'entity': 'offer',
            'wareId': blue_offer_secondary1.waremd5,
        }

    def test_promo_generic_bundle(self):
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('sku_offers', 'offerinfo', 'commonly_purchased', 'deals'):
                # выбор байбокса нужен детерминированный, для этого фиксируем yandexuid (MARKETOUT-16443)
                params = 'place={place}&rids=213&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1&numdoc=10&rearr-factors=turn_on_commonly_purchased=1;'
                if rgb != BLUE:
                    if place == 'deals':
                        continue  # deals работает только на синем
                    params += '&hyperid={}'.format(msku_primary1.hyperid)
                response = self.report.request_json(params.format(place=place, msku=msku_primary1.sku, rgb=rgb))

                # проверяем что в выдаче есть список вторичных офферов
                self.assertFragmentIn(
                    response,
                    {
                        'offers': [
                            self.secondary_offer_in_generic_bundle(),
                        ]
                    },
                    allow_different_len=False,
                )

                # проверяем что в выдаче есть оффер с корректным блоком "promo"
                self.assertFragmentIn(
                    response,
                    [
                        self.primary_offer_in_generic_bundle(),
                    ],
                    allow_different_len=False,
                )

    def test_promo_generic_bundle_in_prime(self):
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('prime',):
                # выбор байбокса нужен детерминированный, для этого фиксируем yandexuid (MARKETOUT-16443)
                params = 'place={place}&rids=213&regset=1&pp=18&rgb={rgb}&yandexuid=1&numdoc=10&rearr-factors=turn_on_commonly_purchased=1'

                if rgb != BLUE:
                    params += '&hyperid={}'.format(msku_primary1.hyperid)
                else:
                    params += '&market-sku={}'.format(msku_primary1.sku)

                response = self.report.request_json(params.format(place=place, rgb=rgb))

                # проверяем что в выдаче есть список вторичных офферов
                self.assertFragmentIn(
                    response,
                    {
                        'offers': [
                            self.secondary_offer_in_generic_bundle(),
                        ]
                    },
                    allow_different_len=False,
                )

                # Формат выдачи place=prime для rgb=green и rgb=blue отличается
                expected = self.primary_offer_in_generic_bundle()
                if rgb == BLUE:
                    expected = [expected]
                # проверяем что в выдаче есть оффер с корректным блоком "promo"
                self.assertFragmentIn(response, expected, allow_different_len=False)

    def test_promo_generic_bundle_in_productoffers(self):
        for rgb in [GREEN, GREEN_WITH_BLUE]:
            for enable_rearr_flag in [None, 0, 1]:
                params = 'place=productoffers&rids=213&regset=1&pp=18&rgb={rgb}&numdoc=10&hyperid={hyperid}&offers-set=defaultList,list'
                if enable_rearr_flag is not None:
                    params += '&rearr-factors=market_promo_enable_generic_bundle_in_product_offers={}'.format(
                        enable_rearr_flag
                    )

                response = self.report.request_json(params.format(rgb=rgb, hyperid=msku_primary1.hyperid))

                if enable_rearr_flag != 0:
                    # проверяем что в выдаче есть список вторичных офферов
                    self.assertFragmentIn(
                        response,
                        {
                            'offers': [
                                self.secondary_offer_in_generic_bundle(),
                            ]
                        },
                        allow_different_len=False,
                    )
                else:
                    self.assertFragmentNotIn(
                        response,
                        {
                            'entity': 'offer',
                            'wareId': blue_offer_secondary1.waremd5,
                        },
                    )

                if enable_rearr_flag != 0:
                    expected = self.primary_offer_in_generic_bundle()
                    # проверяем что в выдаче есть дефолтный оффер с корректным блоком "promo"
                    expected['benefit'] = {'isPrimary': True}
                    self.assertFragmentIn(response, expected, allow_different_len=False)
                else:
                    expected = {
                        'entity': 'offer',
                        'wareId': blue_offer_primary1.waremd5,
                        'promos': Absent(),
                    }
                    self.assertFragmentIn(response, expected)
                    expected['benefit'] = {'isPrimary': True}
                    self.assertFragmentIn(response, expected)

    def test_promo_generic_bundle_in_combine(self):
        offers = (
            (blue_offer_primary1.waremd5, msku_primary1.sku),
            (blue_offer_secondary1.waremd5, msku_secondary1.sku),
        )

        for rgb in ['blue', 'green']:
            params = 'place=combine&rids=213&regset=1&pp=18&rgb={}&offers-list={}&rearr-factors=enable_cart_split_on_combinator=0'
            response = self.report.request_json(
                params.format(
                    rgb,
                    ','.join('{}:1;msku:{}'.format(offer_id, msku_id) for offer_id, msku_id in offers),
                )
            )

            # проверяем что в выдаче есть список офферов
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'offers': {
                            'items': [
                                self.primary_offer_in_generic_bundle(),
                                self.secondary_offer_in_generic_bundle(),
                            ],
                        },
                    },
                },
                allow_different_len=False,
            )

    def test_different_showuids(self):
        # для генерации разных showuid нужен включенный рандом, включаем его только для одного запроса (no-random=0)
        params = 'no-random=0&place={place}&rids=213&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1&numdoc=10'
        for place, rgb in [
            ('offerinfo', BLUE),
            ('offerinfo', GREEN),
            ('offerinfo', GREEN_WITH_BLUE),
        ]:
            response = self.report.request_json(params.format(place=place, msku=msku_primary1.sku, rgb=rgb))

            # проверяем что в выдаче есть список вторичных офферов
            self.assertFragmentIn(
                response,
                {
                    'offers': [
                        {
                            'entity': 'offer',
                            'wareId': blue_offer_secondary1.waremd5,
                        }
                    ]
                },
                allow_different_len=False,
            )

            # проверяем что cpaUrl, feeShow и showUid различны у осн. товара и подарка
            offerGift = response.root['offers'][0]
            resultPrimary = response.root['search']['results'][0]
            resultEntity = resultPrimary['entity']
            if resultEntity == 'offer':
                offerPrimary = resultPrimary
            if resultEntity == 'sku':
                offerPrimary = resultPrimary['offers']['items'][0]

            self.assertIn('showUid', offerPrimary)
            self.assertIn('showUid', offerGift)
            self.assertNotEqual(offerPrimary.get('showUid'), offerGift.get('showUid'))

            self.assertIn('feeShow', offerPrimary)
            self.assertIn('feeShow', offerGift)
            self.assertNotEqual(offerPrimary.get('feeShow'), offerGift.get('feeShow'))

            self.assertIn('cpa', offerPrimary['urls'])
            self.assertIn('cpa', offerGift['urls'])
            self.assertNotEqual(offerPrimary['urls'].get('cpa'), offerGift['urls'].get('cpa'))

    def test_secondary_offers_dedup(self):
        # проверяем "схлопывание" вторичных офферов в списке
        for place in ['sku_offers', 'prime']:
            params = 'place=sku_offers&rids=213&regset=1&pp=18&market-sku={},{}&rgb=blue&yandexuid=1'
            response = self.report.request_json(params.format(msku_primary5.sku, msku_primary6.sku))

            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': blue_offer_primary5.waremd5,
                    'promos': [
                        {
                            'key': promo5.key,
                            'itemsInfo': {
                                'additionalOffers': [
                                    {
                                        'totalOldPrice': {
                                            'value': str(blue_offer_primary5.price + blue_offer_secondary2.price),
                                            'currency': 'RUR',
                                        },
                                        'totalPrice': {'value': str(blue_offer_primary5.price), 'currency': 'RUR'},
                                        'offer': {
                                            'price': {'value': str(gifts2[0].get('value')), 'currency': 'RUR'},
                                            'offerId': blue_offer_secondary2.waremd5,
                                        },
                                    }
                                ]
                            },
                        }
                    ],
                },
                allow_different_len=False,
            )

            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': blue_offer_primary6.waremd5,
                    'promos': [
                        {
                            'key': promo5.key,
                            'itemsInfo': {
                                'additionalOffers': [
                                    {
                                        'totalOldPrice': {
                                            'value': str(blue_offer_primary6.price + blue_offer_secondary2.price),
                                            'currency': 'RUR',
                                        },
                                        'totalPrice': {'value': str(blue_offer_primary6.price), 'currency': 'RUR'},
                                        'offer': {
                                            'price': {'value': str(gifts2[0].get('value')), 'currency': 'RUR'},
                                            'offerId': blue_offer_secondary2.waremd5,
                                        },
                                    }
                                ]
                            },
                        }
                    ],
                },
                allow_different_len=False,
            )

            # должен быть только 1 вторичный оффер, т.к. он является подарком для двух разных ведущих офферов
            self.assertFragmentIn(
                response,
                {
                    'offers': [
                        {
                            'entity': 'offer',
                            'wareId': blue_offer_secondary2.waremd5,
                        }
                    ]
                },
                allow_different_len=False,
            )

    def __should(self, promo, msku, offer):
        for place in ['sku_offers', 'prime']:
            params = 'place=sku_offers&rids=213&regset=1&pp=18&market-sku={}&rgb=blue&yandexuid=1'
            response = self.report.request_json(params.format(msku.sku))

            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': offer.waremd5,
                    'promos': [
                        {
                            'key': promo.key,
                            'itemsInfo': NotEmpty(),
                        }
                    ],
                },
                allow_different_len=False,
            )

    def __should_not(self, promo, msku, waremd5):
        for place, rgb in [
            ('sku_offers', BLUE),
            ('prime', BLUE),
            ('offerinfo', BLUE),
            ('offerinfo', GREEN),
        ]:
            params = 'place={place}&rids=213&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1'
            response = self.report.request_json(params.format(place=place, msku=msku, rgb=rgb))
            # блок промо должен отсутстовать
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': waremd5,
                        'promos': Absent(),
                    }
                ],
            )

    def test_promo_generic_bundle_inactive(self):
        # проверяем отключение акций по времени и белому списку
        self.__should_not(promo2, msku_primary2.sku, blue_offer_primary2.waremd5)
        self.__should_not(promo3, msku_primary3.sku, blue_offer_primary3.waremd5)
        self.__should_not(promo4, msku_primary4.sku, blue_offer_primary4.waremd5)
        self.__should_not(promo7, msku_primary7.sku, blue_offer_primary7.waremd5)

        # проверяем отключение акций по стокам
        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=777, sku=blue_offer_secondary1.offerid, warehouse_id=WH_1),
        ]
        self.__should_not(promo1, msku_primary1.sku, blue_offer_primary1.waremd5)

        # вовращаем стоки, но отключаем через чёрный список лоялти
        self.dynamic.disabled_sku_offers.clear()
        self.__should(promo1, msku_primary1, blue_offer_primary1)
        self.__should(promo1, msku_primary1_1, blue_offer_primary1_1)
        self.dynamic.loyalty += [
            DynamicBluePromosBlacklist(
                blacklist=[
                    (777, blue_offer_secondary1.offerid),
                ]
            )
        ]
        self.__should_not(promo1, msku_primary1.sku, blue_offer_primary1.waremd5)
        self.__should(promo1, msku_primary1_1, blue_offer_primary1_1)

        # не существующий подарок
        self.__should_not(promo6, msku_primary1.sku, blue_offer_primary1.waremd5)

        # процент распределения скидки таков (0% и 100%), что либо стоимость основного товара, либо стоимость подарка оказываются меньше 1 рубля, что нельзя
        # стоимости осн. и подарочного товара равны, если 0% - то цена подарка == 0, если 100% - то цена осн.товара становится равна 0
        self.__should_not(promo12, msku_primary12.sku, blue_offer_primary12.waremd5)
        self.__should_not(promo13, msku_primary13.sku, blue_offer_primary13.waremd5)

    def test_refund_and_spread(self):
        for place, rgb in [
            ('sku_offers', BLUE),
            ('prime', BLUE),
            ('offerinfo', BLUE),
            ('offerinfo', GREEN),
        ]:
            for promo, msku, primary, secondary, price_p, price_s in [
                (promo8, msku_primary8, blue_offer_primary8, blue_offer_secondary8, 8200, 1800),
                (promo9, msku_primary9, blue_offer_primary9, blue_offer_secondary9, 9126, 1207),
            ]:
                params = 'place={place}&rids=213&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1'
                response = self.report.request_json(params.format(place=place, msku=msku.sku, rgb=rgb))

                # проверяем что в выдаче есть список вторичных офферов
                self.assertFragmentIn(
                    response,
                    {
                        'offers': [
                            {
                                'entity': 'offer',
                                'wareId': secondary.waremd5,
                            }
                        ]
                    },
                    allow_different_len=False,
                )

                spread_discount = promo.generic_bundles_content[0]['spread_discount']
                if spread_discount is None:
                    spread_discount = promo.spread_discount
                spread_discount /= 100.0

                self.assertEqual(primary.price - int(secondary.price * spread_discount), price_p)
                self.assertEqual(int(secondary.price * spread_discount), price_s)

                # проверяем что в выдаче есть оффер с корректным блоком "promo"
                self.assertFragmentIn(
                    response,
                    [
                        {
                            'entity': 'offer',
                            'wareId': primary.waremd5,
                            'promos': [
                                {
                                    'type': promo.type_name,
                                    'key': promo.key,
                                    'url': promo.url,
                                    'startDate': NotEmpty() if promo.start_date else Absent(),
                                    'endDate': NotEmpty() if promo.end_date else Absent(),
                                    'itemsInfo': {
                                        'additionalOffers': [
                                            {
                                                'totalOldPrice': {
                                                    'value': str(primary.price + secondary.price),
                                                    'currency': 'RUR',
                                                },
                                                'totalPrice': {'value': str(primary.price), 'currency': 'RUR'},
                                                'primaryPrice': {'value': str(price_p), 'currency': 'RUR'},
                                                'offer': {
                                                    'price': {'value': str(price_s), 'currency': 'RUR'},
                                                    'offerId': secondary.waremd5,
                                                    "entity": "showPlace",
                                                },
                                            }
                                        ]
                                    },
                                }
                            ],
                        }
                    ],
                    allow_different_len=False,
                )

    def test_allow_flags(self):
        params = 'place={place}&rids=213&regset=1&pp=18&market-sku={msku}&rgb=blue&yandexuid=1'

        response = self.report.request_json(params.format(place='sku_offers', msku=msku_primary10.sku))
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': blue_offer_primary10.waremd5,
                'promos': [
                    {
                        'type': promo10.type_name,
                        'key': promo10.key,
                        'itemsInfo': {
                            'additionalOffers': NotEmpty(),
                            'constraints': {
                                'allow_berubonus': promo10.allow_berubonus,
                                'allow_promocode': promo10.allow_promocode,
                            },
                        },
                    }
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json(params.format(place='sku_offers', msku=msku_primary11.sku))
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': blue_offer_primary11.waremd5,
                'promos': [
                    {
                        'type': promo11.type_name,
                        'key': promo11.key,
                        'itemsInfo': {
                            'additionalOffers': NotEmpty(),
                            'constraints': {
                                'allow_berubonus': promo11.allow_berubonus,
                                'allow_promocode': promo11.allow_promocode,
                            },
                        },
                    }
                ],
            },
            allow_different_len=False,
        )

    def test_promo_generic_bundle_several_offers_in_promo(self):
        """
        Проверяет корректность выдачу промоакций при указании несколько наборов (основной оффер-подарок) в одной акции
        """
        primary_offers = [blue_offer_primary14, blue_offer_primary15, blue_offer_primary16]
        secondary_offers = [blue_offer_secondary10, blue_offer_secondary11, blue_offer_secondary13]
        primary_mskus = [msku_primary14.sku, msku_primary15.sku, msku_primary16.sku]
        request = 'place={}&rids=213&regset=1&pp=18&market-sku={}&rgb={rgb}&yandexuid=1'

        for place, rgb in [
            ('sku_offers', BLUE),
            ('prime', BLUE),
            ('offerinfo', BLUE),
            ('offerinfo', GREEN),
        ]:
            for bundle_content, primary_offer, secondary_offer, msku_primary in zip(
                promo14.generic_bundles_content, primary_offers, secondary_offers, primary_mskus
            ):
                response = self.report.request_json(request.format(place, msku_primary, rgb=rgb))

                # проверяем что в выдаче есть оффер с корректным блоком "promo"
                self.assertFragmentIn(
                    response,
                    {
                        'entity': 'offer',
                        'wareId': primary_offer.waremd5,
                        'promos': [
                            {
                                'type': promo14.type_name,
                                'key': promo14.key,
                                'url': promo14.url,
                                'startDate': NotEmpty() if promo14.start_date else Absent(),
                                'endDate': NotEmpty() if promo14.end_date else Absent(),
                                'itemsInfo': {
                                    'additionalOffers': [
                                        {
                                            'totalOldPrice': {
                                                'value': str(primary_offer.price + secondary_offer.price),
                                                'currency': 'RUR',
                                            },
                                            'totalPrice': {'value': str(primary_offer.price), 'currency': 'RUR'},
                                            'offer': {
                                                'price': {
                                                    'value': str(
                                                        bundle_content['secondary_item']['discount_price']['value']
                                                        / 100
                                                    ),
                                                    'currency': 'RUR',
                                                },
                                                'offerId': secondary_offer.waremd5,
                                                "showUid": NotEmpty(),
                                                "feeShow": NotEmpty(),
                                                "entity": "showPlace",
                                                "urls": NotEmpty(),
                                            },
                                        }
                                    ],
                                    'constraints': NotEmpty(),
                                },
                            }
                        ],
                    },
                    allow_different_len=False,
                )

    def test_two_ssku_same_promo(self):
        # проблема невыдачи офферов-подарков проявлялась только при включенном режиме мульти-оффера (enable-multioffer=1)
        for place, rgb in [
            ('sku_offers', BLUE),
            ('sku_offers', GREEN),
            ('sku_offers', GREEN_WITH_BLUE),
            ('prime', BLUE),
            ('offerinfo', BLUE),
            ('offerinfo', GREEN),
            ('offerinfo', GREEN_WITH_BLUE),
        ]:
            # выбор байбокса нужен детерминированный, для этого фиксируем yandexuid (MARKETOUT-16443)
            params = 'place={place}&rids=213&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1&numdoc=10&enable-multioffer=1'
            response = self.report.request_json(params.format(place=place, msku=msku_primary18.sku, rgb=rgb))

            # проверяем что в выдаче есть список вторичных офферов - с единственным заданным подарком
            self.assertFragmentIn(
                response,
                {
                    'offers': [
                        {
                            'entity': 'offer',
                            'wareId': blue_offer_secondary15.waremd5,
                        }
                    ]
                },
                allow_different_len=False,
            )

            # проверяем что в выдаче есть оффер(ы) с корректным блоком "promo" (может быть 1 или 2 оффера)
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': EqualToOneOf(blue_offer_primary19.waremd5, blue_offer_primary20.waremd5),  # buybox
                        'promos': [
                            {
                                'type': promo16.type_name,
                                'key': promo16.key,
                            }
                        ],
                    }
                ],
                allow_different_len=True,
            )

    def test_reviews(self):
        response = self.report.request_json(
            "place=prime&rids=213&regset=1&pp=18&yandexuid=1&numdoc=10&hyperid=1&text=msku1&allow-collapsing=1&debug=1&use-default-offers=1&show-reviews=1"
        )

        # проверяем что в выдаче есть ДО, а репорт не упал
        self.assertFragmentIn(
            response,
            {
                'offers': {
                    'count': 1,
                }
            },
            allow_different_len=False,
        )

    def test_generic_bundle_in_default_offer(self):
        """
        В тесте у одного MSKU два оффера. Запрашиваем дефолтный оффер с акцией.
        В ответе должно быть 2 оффера с одним ware_md5 (один из результатов msku, второй как дефолтный).
        Оба оффера должны иметь акцию
        """
        response = self.report.request_json(
            'place=productoffers&market-sku={msku}&pp=6&rids=213&offers-set=defaultList,listCpa&do-waremd5={ware_md5}'.format(
                msku=msku_primary19.sku, ware_md5=blue_offer_primary21.waremd5
            )
        )
        for is_default in [True, False]:
            self.assertFragmentIn(
                response,
                {
                    'wareId': blue_offer_primary21.waremd5,
                    'entity': 'offer',
                    'isDefaultOffer': is_default,
                    'promos': [
                        {
                            'key': promo17.key,
                        }
                    ],
                },
            )


if __name__ == '__main__':
    main()
