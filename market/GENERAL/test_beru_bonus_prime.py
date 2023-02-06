#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.loyalty import BonusRestrictions
from core.types import BlueOffer, Currency, HyperCategory, MarketSku, MnPlace, Shop, Tax, Vendor, Offer
from core.logs import ErrorCodes
from core.matcher import EmptyList
from core.types.autogen import b64url_md5


TOP_HID = 100
FIRST_LEFT_HID = 1001
FIRST_RIGHT_HID = 1002
SECOND_LEFT_HID = 10011
SECOND_CENTER_HID = 10012
SECOND_RIGHT_HID = 10013
HEAVY_HID = 90533

DEFAULT_VENDOR_ID = 1
INTEL_VENDOR_ID = 2
AMD_VENDOR_ID = 3
ARM_VENDOR_ID = 4

FIRST_SUPPLIER_SHOP_ID = 1
FIRST_SUPPLIER_FEED_ID = 11

SECOND_SUPPLIER_SHOP_ID = 2
SECOND_SUPPLIER_FEED_ID = 22

DSBS_SUPPLIER_SHOP_ID = 3
DSBS_SUPPLIER_FEED_ID = 33

WHITE_SUPPLIER_SHOP_ID = 4
WHITE_SUPPLIER_FEED_ID = 44

VIRTUAL_SHOP_ID = 5
VIRTUAL_SHOP_FEED_ID = 5

MSKU_ID_WITH_SEVERAL_SUPPLIERS = 25
MSKU_ID_WHITE = 26


# Синие офферы и Market SKU
def __create_blue_offer(title, hid, sku, price, feedid, vendor=DEFAULT_VENDOR_ID, model=None, offer_ts=None):
    hyperid = hid * 1000 + sku if model is None else model
    blue_offer = BlueOffer(
        price=price,
        offerid='Shop1_sku{msku}'.format(msku=sku),
        waremd5='Sku{msku}Price{price}k-vm1Goleg'.format(msku=sku, price=str(price)[:2]),
        feedid=feedid,
        ts=offer_ts,
    )
    msku = MarketSku(title=title, hid=hid, hyperid=hyperid, sku=str(sku), blue_offers=[blue_offer], vendor_id=vendor)
    return msku, blue_offer


def __create_white_offer(msku, supplier_id, cpa=Offer.CPA_NO, price=1000):
    return Offer(
        waremd5=b64url_md5(supplier_id + price),
        price=price,
        fesh=supplier_id,
        sku=msku.sku,
        title=msku.title,
        cpa=cpa,
    )


def __create_blue_offer_with_given_msku(sku, price, feedid, offer_ts=None):
    return BlueOffer(
        price=price,
        offerid='Shop1_sku{msku}'.format(msku=sku),
        waremd5='Sku{msku}Price{price}k-vm1Goleg'.format(msku=sku, price=str(price)[:2]),
        feedid=feedid,
        ts=offer_ts,
    )


INTEL_MSKU, INTEL_OFFER = __create_blue_offer(
    title="Тестовый синий оффер от Intel",
    hid=SECOND_LEFT_HID,
    sku=10,
    price=3000,
    vendor=INTEL_VENDOR_ID,
    feedid=FIRST_SUPPLIER_FEED_ID,
)
AMD_MSKU, AMD_OFFER = __create_blue_offer(
    title="Тестовый синий оффер от AMD",
    hid=SECOND_LEFT_HID,
    sku=11,
    price=4000,
    vendor=AMD_VENDOR_ID,
    feedid=FIRST_SUPPLIER_FEED_ID,
)
ARM_MSKU, ARM_OFFER = __create_blue_offer(
    title="Тестовый синий оффер от ARM",
    hid=SECOND_LEFT_HID,
    sku=12,
    price=3500,
    vendor=ARM_VENDOR_ID,
    feedid=FIRST_SUPPLIER_FEED_ID,
)
CHEAP_MSKU, CHEAP_OFFER = __create_blue_offer(
    title="Тестовый дешевый синий оффер", hid=FIRST_RIGHT_HID, sku=13, price=1000, feedid=FIRST_SUPPLIER_FEED_ID
)
ORDINARY_MSKU, ORDINARY_OFFER = __create_blue_offer(
    title="Тестовый обычный синий оффер", hid=FIRST_RIGHT_HID, sku=14, price=5000, feedid=FIRST_SUPPLIER_FEED_ID
)
EXPENSIVE_MSKU, EXPENSIVE_OFFER = __create_blue_offer(
    title="Тестовый дорогой синий оффер", hid=FIRST_RIGHT_HID, sku=15, price=6000, feedid=FIRST_SUPPLIER_FEED_ID
)
ANOTHER_ORDINARY_MSKU, ANOTHER_ORDINARY_OFFER = __create_blue_offer(
    title="Еще один тестовый синий оффер",
    hid=SECOND_RIGHT_HID,
    sku=16,
    price=3000,
    feedid=FIRST_SUPPLIER_FEED_ID,
    model=None,  # generate hyperid
    offer_ts=501,
)
YET_ANOTHER_ORDINARY_MSKU, YET_ANOTHER_ORDINARY_OFFER = __create_blue_offer(
    title="И еще один тестовый синий оффер",
    hid=SECOND_RIGHT_HID,
    sku=17,
    price=5000,
    feedid=FIRST_SUPPLIER_FEED_ID,
    model=ANOTHER_ORDINARY_MSKU.hyperid,
    offer_ts=502,
)
LENS_MSKU, LENS_OFFER = __create_blue_offer(
    title="Лупа Френеля", hid=HEAVY_HID, sku=18, price=2500, feedid=FIRST_SUPPLIER_FEED_ID
)

INTEL_I20, INTEL_OFFER_SECOND_SUPPLIER = __create_blue_offer(
    title="Тестовый синий оффер от Intel",
    hid=SECOND_LEFT_HID,
    sku=20,
    price=3000,
    vendor=INTEL_VENDOR_ID,
    feedid=SECOND_SUPPLIER_FEED_ID,
)
AMD_MSKU_M880G, AMD_OFFER_SECOND_SUPPLIER = __create_blue_offer(
    title="Тестовый синий оффер от AMD",
    hid=SECOND_LEFT_HID,
    sku=21,
    price=4000,
    vendor=AMD_VENDOR_ID,
    feedid=SECOND_SUPPLIER_FEED_ID,
)
ARM_MSKU_APPLE, ARM_OFFER_SECOND_SUPPLIER = __create_blue_offer(
    title="Тестовый синий оффер от ARM",
    hid=SECOND_LEFT_HID,
    sku=22,
    price=3500,
    vendor=ARM_VENDOR_ID,
    feedid=SECOND_SUPPLIER_FEED_ID,
)

OFFER_FIRST_SUPPLIER = __create_blue_offer_with_given_msku(
    sku=MSKU_ID_WITH_SEVERAL_SUPPLIERS, price=3500, feedid=FIRST_SUPPLIER_FEED_ID
)

OFFER_SECOND_SUPPLIER = __create_blue_offer_with_given_msku(
    sku=MSKU_ID_WITH_SEVERAL_SUPPLIERS, price=3699, feedid=SECOND_SUPPLIER_FEED_ID
)

# просто магазин для DSBS
DSBS_SHOP = Shop(
    fesh=DSBS_SUPPLIER_SHOP_ID,
    datafeed_id=DSBS_SUPPLIER_FEED_ID,
    cpa=Shop.CPA_REAL,
)

# просто магазин для белого оффера
WHITE_SHOP = Shop(
    fesh=WHITE_SUPPLIER_SHOP_ID,
    datafeed_id=WHITE_SUPPLIER_FEED_ID,
    cpa=Shop.CPA_NO,
    cpc=Shop.CPC_REAL,
)

MSKU_WITH_SEVERAL_SUPPLIERS = MarketSku(
    title="msku с несколькими поставщиками",
    hid=FIRST_RIGHT_HID,
    hyperid=900020,
    sku=str(MSKU_ID_WITH_SEVERAL_SUPPLIERS),
    blue_offers=[OFFER_FIRST_SUPPLIER, OFFER_SECOND_SUPPLIER],
    vendor_id=ARM_VENDOR_ID,
)

MSKU_WHITE = MarketSku(
    title='msku c dbsb и не dsbs офферами',
    hid=FIRST_RIGHT_HID,
    hyperid=900021,
    sku=str(MSKU_ID_WHITE),
)

OFFER_WHITE_DSBS = __create_white_offer(MSKU_WHITE, supplier_id=DSBS_SUPPLIER_SHOP_ID, cpa=Offer.CPA_REAL, price=14000)
OFFER_WHITE_NOT_DSBS = __create_white_offer(
    MSKU_WHITE, supplier_id=WHITE_SUPPLIER_SHOP_ID, cpa=Offer.CPA_NO, price=14050
)

NONEXISTENT_BONUS_ID = 'nonexistent_bonus_id'
EMPTY_REQUEST_BONUS_ID = 'empty_request_bonus_id'

# Набор тестовых Беру бонусов, построенных на основе таблицы:
# https://wiki.yandex-team.ru/users/fonar101/Statistika-po-BB-dlja-proekta-Perexod-s-BB-na-spisok-tovarov/
BONUS_RESTRICTIONS = [
    # msku_list
    BonusRestrictions(bonus_id='test_bonus_id_001', mskus=[INTEL_MSKU.sku, ORDINARY_MSKU.sku]),
    # category_list
    BonusRestrictions(bonus_id='test_bonus_id_002', hids=[FIRST_LEFT_HID]),
    # vendor_list
    BonusRestrictions(bonus_id='test_bonus_id_003', vendors=[INTEL_VENDOR_ID, AMD_VENDOR_ID]),
    # msku_list AND category_list
    BonusRestrictions(bonus_id='test_bonus_id_004', mskus=[EXPENSIVE_MSKU.sku], hids=[FIRST_RIGHT_HID]),
    # vendor_list AND NOT category_list
    # ANOTHER_ORDINARY_OFFER и YET_ANOTHER_ORDINARY_OFFER удовлетворяет условиям этого бонуса
    BonusRestrictions(bonus_id='test_bonus_id_005', vendors=[DEFAULT_VENDOR_ID], not_hids=[FIRST_RIGHT_HID]),
    # category_list AND not vendor_list
    BonusRestrictions(bonus_id='test_bonus_id_006', hids=[SECOND_LEFT_HID], not_vendors=[INTEL_VENDOR_ID]),
    # msku_list AND category_list
    # EXPENSIVE_MSKU не из категории SECOND_LEFT_HID - ожидается пустая выдача
    BonusRestrictions(bonus_id=EMPTY_REQUEST_BONUS_ID, mskus=[EXPENSIVE_MSKU.sku], hids=[SECOND_LEFT_HID]),
    # msku_list
    # Два разных Market SKU от одной и той же модели (проверям работу с параметром '&allow-collapsing')
    BonusRestrictions(bonus_id='test_bonus_id_007', mskus=[ANOTHER_ORDINARY_MSKU.sku, YET_ANOTHER_ORDINARY_MSKU.sku]),
    # lens restriction
    BonusRestrictions(bonus_id='bonus_lens', hids=[HEAVY_HID, SECOND_LEFT_HID]),
    # supplier_list
    BonusRestrictions(bonus_id='test_bonus_id_009', suppliers=[SECOND_SUPPLIER_SHOP_ID]),
    # hids_list and NOT supplier_list
    BonusRestrictions(bonus_id='test_bonus_id_010', hids=[SECOND_LEFT_HID], not_suppliers=[SECOND_SUPPLIER_SHOP_ID]),
    # vendors_list + suppliers_list
    BonusRestrictions(
        bonus_id='test_bonus_id_011', vendors=[INTEL_VENDOR_ID, AMD_VENDOR_ID], suppliers=[SECOND_SUPPLIER_SHOP_ID]
    ),
    # hids_list + supplier_list + msku_list + vendors_list
    BonusRestrictions(
        bonus_id='test_bonus_id_012',
        hids=[SECOND_LEFT_HID],
        suppliers=[SECOND_SUPPLIER_SHOP_ID],
        mskus=[INTEL_I20.sku],
        vendors=[INTEL_VENDOR_ID],
    ),
    # msku + supplier для проверки фильтрации по поставщику для одного msku
    BonusRestrictions(
        bonus_id='test_bonus_id_013', suppliers=[SECOND_SUPPLIER_SHOP_ID], mskus=[MSKU_WITH_SEVERAL_SUPPLIERS.sku]
    ),
    # фильтр по DSBS
    BonusRestrictions(bonus_id='test_bonus_id_014', mskus=[MSKU_WHITE.sku], not_for_dbs=True),
    # фильтр по не DSBS
    BonusRestrictions(bonus_id='test_bonus_id_015', mskus=[MSKU_WHITE.sku], not_for_dbs=False),
    # msku + shop_id для проверки фильтрации белого офера
    BonusRestrictions(bonus_id='test_bonus_id_016', mskus=[MSKU_WHITE.sku], suppliers=[DSBS_SUPPLIER_SHOP_ID]),
]

PRIME_REQUEST = (
    'place=prime'
    '&bonus_id={bonus}'
    '&allow-collapsing={collapse}'
    '&rearr-factors=market_extreq_loyalty_enabled={exp_flag}'
    '&use-default-offers={default_offers}'
)


class T(TestCase):
    """
    Набор тестов на построение фильтрованной выдачи по правилам применения Беру бонусов
    """

    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'market'
        cls.settings.lms_autogenerate = True

    @classmethod
    def prepare_categories(cls):
        cls.index.hypertree = [
            HyperCategory(
                hid=TOP_HID,
                children=[
                    HyperCategory(
                        hid=FIRST_LEFT_HID,
                        children=[
                            HyperCategory(hid=SECOND_LEFT_HID),
                            HyperCategory(hid=SECOND_CENTER_HID),
                            HyperCategory(hid=SECOND_RIGHT_HID),
                        ],
                    ),
                    HyperCategory(hid=FIRST_RIGHT_HID),
                ],
            )
        ]

    @classmethod
    def prepare_vendors(cls):
        cls.index.vendors += [
            Vendor(vendor_id=INTEL_VENDOR_ID, name="Intel"),
            Vendor(vendor_id=AMD_VENDOR_ID, name="AMD"),
            Vendor(vendor_id=ARM_VENDOR_ID, name="ARM"),
        ]

    @classmethod
    def prepare_delivery(cls):
        cls.index.shops += [
            Shop(
                fesh=FIRST_SUPPLIER_SHOP_ID,
                datafeed_id=FIRST_SUPPLIER_FEED_ID,
                name="Тестовый поставщик",
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=SECOND_SUPPLIER_SHOP_ID,
                datafeed_id=SECOND_SUPPLIER_FEED_ID,
                name="Тестовый поставщик",
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=VIRTUAL_SHOP_ID,
                datafeed_id=VIRTUAL_SHOP_FEED_ID,
                name="Тестовый виртуальный магазин",
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            DSBS_SHOP,
            WHITE_SHOP,
        ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [
            INTEL_MSKU,
            AMD_MSKU,
            ARM_MSKU,
            CHEAP_MSKU,
            ORDINARY_MSKU,
            EXPENSIVE_MSKU,
            ANOTHER_ORDINARY_MSKU,
            YET_ANOTHER_ORDINARY_MSKU,
            LENS_MSKU,
            INTEL_I20,
            AMD_MSKU_M880G,
            ARM_MSKU_APPLE,
            MSKU_WITH_SEVERAL_SUPPLIERS,
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 501).respond(0.8)  # ANOTHER_ORDINARY_OFFER
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 502).respond(0.9)  # YET_ANOTHER_ORDINARY_OFFER

    @classmethod
    def prepare_white_offers(cls):
        cls.index.mskus += [MSKU_WHITE]
        cls.index.offers += [
            OFFER_WHITE_DSBS,
            OFFER_WHITE_NOT_DSBS,
        ]

    @classmethod
    def prepare_loyalty(cls):
        cls.loyalty.set_bonus_restrictions(BONUS_RESTRICTIONS)

    def test_place_prime(self):
        """
        Проверяем, что построение фильтрованной выдачи по правилам применения
        Беру бонусов происходит корректно

        В данном сценарии каждое Market SKU принадлженит отдельной модели, поэтому
        параметр '&allow-collapsing' на общее число офферов на выдаче
        """
        test_list = [
            # (Правила Беру бонуса; список офферов, на которые он распространяется)
            (BONUS_RESTRICTIONS[0], [INTEL_OFFER, ORDINARY_OFFER]),
            (
                BONUS_RESTRICTIONS[1],
                [
                    INTEL_OFFER,
                    AMD_OFFER,
                    ARM_OFFER,
                    ANOTHER_ORDINARY_OFFER,
                    YET_ANOTHER_ORDINARY_OFFER,
                    INTEL_OFFER_SECOND_SUPPLIER,
                    AMD_OFFER_SECOND_SUPPLIER,
                    ARM_OFFER_SECOND_SUPPLIER,
                ],
            ),
            (BONUS_RESTRICTIONS[2], [INTEL_OFFER, AMD_OFFER, INTEL_OFFER_SECOND_SUPPLIER, AMD_OFFER_SECOND_SUPPLIER]),
            (BONUS_RESTRICTIONS[3], [EXPENSIVE_OFFER]),
            (BONUS_RESTRICTIONS[4], [ANOTHER_ORDINARY_OFFER, YET_ANOTHER_ORDINARY_OFFER, LENS_OFFER]),
            (BONUS_RESTRICTIONS[5], [AMD_OFFER, ARM_OFFER, AMD_OFFER_SECOND_SUPPLIER, ARM_OFFER_SECOND_SUPPLIER]),
            (
                BONUS_RESTRICTIONS[9],
                [
                    INTEL_OFFER_SECOND_SUPPLIER,
                    AMD_OFFER_SECOND_SUPPLIER,
                    ARM_OFFER_SECOND_SUPPLIER,
                    OFFER_SECOND_SUPPLIER,
                ],
            ),
            (BONUS_RESTRICTIONS[10], [INTEL_OFFER, AMD_OFFER, ARM_OFFER]),
            (BONUS_RESTRICTIONS[11], [INTEL_OFFER_SECOND_SUPPLIER, AMD_OFFER_SECOND_SUPPLIER]),
            (BONUS_RESTRICTIONS[12], [INTEL_OFFER_SECOND_SUPPLIER]),
            (BONUS_RESTRICTIONS[14], [OFFER_WHITE_NOT_DSBS]),
            (BONUS_RESTRICTIONS[15], [OFFER_WHITE_DSBS, OFFER_WHITE_NOT_DSBS]),
            (BONUS_RESTRICTIONS[16], [OFFER_WHITE_DSBS]),
        ]
        for bonus, offer_list in test_list:
            response = self.report.request_json(
                PRIME_REQUEST.format(bonus=bonus.bonus_id, collapse=0, exp_flag=1, default_offers=0)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': offer.waremd5,
                            'prices': {'currency': Currency.RUR, 'value': str(offer.price)},
                        }
                        for offer in offer_list
                    ]
                },
                allow_different_len=False,
            )

        # Беру бонус с заведомо пустой выдачей
        response = self.report.request_json(
            PRIME_REQUEST.format(bonus=EMPTY_REQUEST_BONUS_ID, collapse=0, exp_flag=1, default_offers=0)
        )
        self.assertFragmentIn(response, {'search': {'total': 0, 'results': EmptyList()}})

        # Проверяем rgb=blue, пока не оторвали
        response = self.report.request_json(
            PRIME_REQUEST.format(bonus='test_bonus_id_001', collapse=0, exp_flag=1, default_offers=0) + '&rgb=blue'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': offer.waremd5,
                            'prices': {'currency': Currency.RUR, 'value': str(offer.price)},
                        }
                        for offer in [INTEL_OFFER, ORDINARY_OFFER]
                    ]
                }
            },
        )

    def test_experimental_flag(self):
        """
        Проверяем, что экспериментальный флаг 'market_extreq_loyalty_enabled' работает корректно
        """
        response = self.report.request_json(
            tail=PRIME_REQUEST.format(bonus=BONUS_RESTRICTIONS[0].bonus_id, collapse=0, exp_flag=0, default_offers=0),
            strict=False,
        )
        self.assertFragmentIn(response, {"error": {'code': 'EMPTY_REQUEST', 'message': "Request is empty"}})

    def test_error_log(self):
        """
        Проверяем, что запись в error.log Report'а происходит корректно,
        если в запросе передан неизвестный идентификатор бонуса
        """
        response = self.report.request_json(
            tail=PRIME_REQUEST.format(bonus=NONEXISTENT_BONUS_ID, collapse=0, exp_flag=1, default_offers=0),
            strict=False,
        )
        self.assertFragmentIn(
            response,
            {
                "error": {
                    "code": "INVALID_USER_CGI",
                    "message": "Unable to retrieve bonus restrictions for given '&bonus_id'",
                }
            },
        )
        self.error_log.expect(code=ErrorCodes.EXTREQUEST_LOYALTY_FAILED)

    def test_beru_bonus_allow_collapsing(self):
        """
        Отдельно тестируем, что для Market SKU из одной и той же модели выдача с параметром
        '&allow-collapsing=0' содержит оба оффера, а при '&allow-collapsing=1' только
        наиболее релевантный
        """
        # Беру-бонус, распространяющийся на 2 Market SKU от одной и той же модели
        bonus = BONUS_RESTRICTIONS[7]

        # офферный формат выдачи
        response = self.report.request_json(
            PRIME_REQUEST.format(bonus=bonus.bonus_id, collapse=0, exp_flag=1, default_offers=0)
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': offer.waremd5,
                        'prices': {'currency': Currency.RUR, 'value': str(offer.price)},
                    }
                    for offer in [ANOTHER_ORDINARY_OFFER, YET_ANOTHER_ORDINARY_OFFER]
                ]
            },
            allow_different_len=False,
        )

        # модельный формат выдачи
        # YET_ANOTHER_ORDINARY_MSKU дефолт оффер, так как он наиболее релевантный
        response = self.report.request_json(
            PRIME_REQUEST.format(bonus=bonus.bonus_id, collapse=1, exp_flag=1, default_offers=0) + '&rgb=blue'
        )  # логика для blue ещё поменяется.
        # Со временем нужно будет убрать rgb=blue, и, возможно, добавить cpa=real
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'offers': {
                            'count': 2,
                            'items': [
                                {
                                    'entity': 'offer',
                                    'wareId': YET_ANOTHER_ORDINARY_OFFER.waremd5,
                                    'prices': {
                                        'currency': Currency.RUR,
                                        'value': str(YET_ANOTHER_ORDINARY_OFFER.price),
                                    },
                                }
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_beru_bonus_default_offer(self):
        """
        Проверяем, что при поиске моделей по bonus_id не происходит дозапрос за офферами по модели
        и для модели не выбирается дефолтным оффер, не удовлетворяющий ограничениям бонуса
        """
        # Беру-бонус, распространяющийся только на один оффер (один msku + один поставщик)
        bonus = BONUS_RESTRICTIONS[13]

        # модельный формат выдачи с use-default-offers=1 и allow-collapsing=1
        response = self.report.request_json(
            PRIME_REQUEST.format(bonus=bonus.bonus_id, collapse=1, exp_flag=1, default_offers=1)
            + '&rgb=green_with_blue'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'offers': {
                            'count': 2,
                            'items': [
                                {
                                    'entity': 'offer',
                                    'wareId': OFFER_SECOND_SUPPLIER.waremd5,
                                    'prices': {
                                        'currency': Currency.RUR,
                                        'value': str(OFFER_SECOND_SUPPLIER.price),
                                    },
                                    'supplier': {
                                        'entity': 'shop',
                                        'id': SECOND_SUPPLIER_SHOP_ID,
                                    },
                                }
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_loyalty_caching(cls):
        cls.settings.memcache_enabled = True

    def test_loyalty_caching(self):
        '''
        Тест глобального кэширования
        '''

        # Беру-бонус, распространяющийся на 2 Market SKU от одной и той же модели
        bonus = BONUS_RESTRICTIONS[7]

        expected_fragment = {
            'results': [
                {
                    'entity': 'offer',
                    'wareId': offer.waremd5,
                    'prices': {'currency': Currency.RUR, 'value': str(offer.price)},
                }
                for offer in [ANOTHER_ORDINARY_OFFER, YET_ANOTHER_ORDINARY_OFFER]
            ]
        }

        # Без кэширования
        response = self.report.request_json(
            PRIME_REQUEST.format(bonus=bonus.bonus_id, collapse=0, exp_flag=1, default_offers=0)
        )
        self.assertFragmentIn(response, expected_fragment, allow_different_len=False)

        # С кэшированием
        response = self.report.request_json(
            PRIME_REQUEST.format(bonus=bonus.bonus_id, collapse=0, exp_flag=1, default_offers=0)
            + "&rearr-factors=loyalty_client_memcached_ttl_min=1"
        )
        self.assertFragmentIn(response, expected_fragment, allow_different_len=False)

        response = self.report.request_json(
            PRIME_REQUEST.format(bonus=bonus.bonus_id, collapse=0, exp_flag=1, default_offers=0)
            + "&rearr-factors=loyalty_client_memcached_ttl_min=1"
        )
        self.assertFragmentIn(response, expected_fragment, allow_different_len=False)

        self.external_services_log.expect(service='memcached_loyalty', http_code=204).times(1)
        self.external_services_log.expect(service='memcached_set_loyalty').times(1)
        self.external_services_log.expect(service='memcached_loyalty', http_code=200).times(1)


if __name__ == '__main__':
    main()
