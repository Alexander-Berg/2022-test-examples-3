#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, NotEmpty
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types import DynamicBlueGenericBundlesPromos, Promo, PromoType, Shop
from core.types.autogen import b64url_md5
from core.types.dynamic_filters import DynamicBluePromosBlacklist
from core.types.sku import MarketSku, BlueOffer
from core.types.offer_promo import PromoBlueFlash, OffersMatchingRules, calc_discount_percent

from itertools import count


FEED = 777
nummer = count()


def __blue_offer(price=1000):
    num = next(nummer)
    return BlueOffer(
        waremd5=b64url_md5(num),
        price=price,
        fesh=FEED,
        feedid=FEED,
        offerid='ССКУ_{}'.format(num),
    )


blue_offer_1 = __blue_offer(price=9999)
blue_offer_2 = __blue_offer()
blue_offer_3 = __blue_offer()
blue_offer_4 = __blue_offer()
blue_offer_5 = __blue_offer()
blue_offer_6 = __blue_offer(price=1000)
blue_offer_7 = __blue_offer(price=1000)
blue_offer_7a = __blue_offer(price=800)
blue_offer_8 = __blue_offer(price=1000000)
blue_offer_9 = __blue_offer(price=1000)
blue_offer_10 = __blue_offer(price=1000)
# price в оффере игнорируется, цена берётся из промо-акции
blue_offer_A = __blue_offer(price=100500)


def __msku(offers):
    num = next(nummer)
    return MarketSku(sku=num, hyperid=num, blue_offers=offers if isinstance(offers, list) else [offers])


msku_1 = __msku(blue_offer_1)
msku_2 = __msku(blue_offer_2)
msku_3 = __msku(blue_offer_3)
msku_4 = __msku(blue_offer_4)
msku_5 = __msku(blue_offer_5)
msku_6 = __msku(blue_offer_6)
msku_7 = __msku([blue_offer_7, blue_offer_7a])
msku_8 = __msku(blue_offer_8)
msku_9 = __msku(blue_offer_9)
msku_10 = __msku(blue_offer_10)
msku_A = __msku(blue_offer_A)


# для флэш акций требуется высокая (до секунд) точность контроля времени начала и окончания акции
# чтобы задать время используется метод Promo.override_start_end


# действующая акция
promo1 = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': 777, 'offer_id': blue_offer_1.offerid, 'price': {'value': 8999, 'currency': 'RUR'}},
            {'feed_id': 777, 'offer_id': blue_offer_2.offerid, 'price': {'value': 660, 'currency': 'RUR'}},
            {'feed_id': 777, 'offer_id': blue_offer_10.offerid, 'price': {'value': 1005, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_1]),
        OffersMatchingRules(mskus=[msku_2, msku_10]),
        OffersMatchingRules(feed_offer_ids=[[777, blue_offer_10.offerid]]),
    ],
)
promo1.override_start_end(REQUEST_TIMESTAMP - 2, REQUEST_TIMESTAMP + 2)  # ±2 секунды

# уже закончилась
promo2 = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    url='http://localhost.ru/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': 777, 'offer_id': blue_offer_3.offerid, 'price': {'value': 55, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_3])],
)
promo2.override_start_end(REQUEST_TIMESTAMP - 100500, REQUEST_TIMESTAMP - 2)

# ещё не началась
promo3 = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    url='http://localhost.ru/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': 777, 'offer_id': blue_offer_4.offerid, 'price': {'value': 55, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_4])],
)
promo3.override_start_end(REQUEST_TIMESTAMP + 2, REQUEST_TIMESTAMP + 100500)

# акция не включена в белом списке лоялти
promo4 = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    url='http://localhost.ru/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': 777, 'offer_id': blue_offer_5.offerid, 'price': {'value': 55, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_5])],
)

# в акции указана такая цена, что размер скидки ниже порога 5% и ниже порога в 500 рублей
promo6 = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    url='http://localhost.ru/',
    blue_flash=PromoBlueFlash(
        items=[
            # если размер скидки слишком маленький (44 руб от 1000, <5% с округлением вверх этого процента и меньше 500 рублей) - промо не должно показываться
            {
                'feed_id': 777,
                'offer_id': blue_offer_6.offerid,
                'price': {'value': blue_offer_6.price - 44, 'currency': 'RUR'},
            },
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_6])],
)

# тест промо в buybox
promo7 = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': 777, 'offer_id': blue_offer_7.offerid, 'price': {'value': 700, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[OffersMatchingRules(feed_offer_ids=[[777, blue_offer_7.offerid]])],
)


# В акции указана такая цена, что размер скидки ниже порога 5%, но выше порога в 500 рублей. Это валидное промо.
promo8 = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    url='http://blue_flash_8.ru/',
    blue_flash=PromoBlueFlash(
        items=[
            {
                'feed_id': 777,
                'offer_id': blue_offer_8.offerid,
                'price': {'value': blue_offer_8.price - 501, 'currency': 'RUR'},
            },
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_8])],
)

# В акции указана такая цена, что размер скидки ниже порога в 500 рублей, но выше порога в 5%. Это валидное промо.
promo9 = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    url='http://blue_flash_9.ru/',
    blue_flash=PromoBlueFlash(
        items=[
            {
                'feed_id': 777,
                'offer_id': blue_offer_9.offerid,
                'price': {'value': blue_offer_9.price - 104, 'currency': 'RUR'},
            },
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_9])],
)

# действующая акция с old_price
promoA = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[
            {
                'feed_id': 777,
                'offer_id': blue_offer_A.offerid,
                'price': {'value': 8999, 'currency': 'RUR'},
                'old_price': {'value': 9999, 'currency': 'RUR'},
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_A])],
)
promoA.override_start_end(REQUEST_TIMESTAMP - 2, REQUEST_TIMESTAMP + 2)  # ±2 секунды


class T(TestCase):
    @classmethod
    def prepare(cls):
        blue_offer_1.promo = [promo1]
        blue_offer_2.promo = [promo1]
        blue_offer_3.promo = [promo2]
        blue_offer_4.promo = [promo3]
        blue_offer_5.promo = [promo4]
        blue_offer_6.promo = [promo6]
        blue_offer_7.promo = [promo7]
        blue_offer_8.promo = [promo8]
        blue_offer_9.promo = [promo9]
        blue_offer_10.promo = [promo1]
        blue_offer_A.promo = [promoA]
        cls.index.shops += [
            Shop(fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
        ]

        cls.index.mskus += [
            msku_1,
            msku_2,
            msku_3,
            msku_4,
            msku_5,
            msku_6,
            msku_7,
            msku_8,
            msku_9,
            msku_10,
            msku_A,
        ]

        cls.index.promos += [
            promo1,
            promo2,
            promo3,
            promo6,
            promo7,
            promo8,
            promo9,
            promoA,
        ]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo.key for promo in cls.index.promos])]

        cls.index.promos += [
            promo4,
        ]

    def __should(self, msku, blue_offer, promo, promo_price):
        promo_offer = next(item for item in promo.blue_flash.items if item['offer_id'] == blue_offer.offerid)
        # по новой логике синих флешей (MARKETOUT-42994), старая цена берётся не из оффера, а из поля old_price в промо-акции (если оно присутствует)
        if 'old_price' in promo_offer:
            promo_price = promo_offer['price']['value']
            old_price = promo_offer['old_price']['value']
        else:
            old_price = blue_offer.price
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('sku_offers', 'prime'):
                # выбор байбокса нужен детерминированный, для этого фиксируем yandexuid (MARKETOUT-16443)
                params = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1'
                response = self.report.request_json(params.format(place=place, msku=msku.sku, rgb=rgb))

                # проверяем что в выдаче есть оффер с корректным блоком "promo"
                percent = calc_discount_percent(promo_price, old_price)
                self.assertFragmentIn(
                    response,
                    [
                        {
                            'entity': 'offer',
                            'wareId': blue_offer.waremd5,
                            'prices': {
                                'value': str(promo_price),
                                'discount': {
                                    'percent': percent,
                                    'oldMin': str(old_price),
                                },
                            },
                            'promos': [
                                {
                                    'type': promo.type_name,
                                    'key': promo.key,
                                    'url': promo.url or Absent(),
                                    'startDate': NotEmpty() if promo.start_date else Absent(),
                                    'endDate': NotEmpty() if promo.start_date else Absent(),
                                    'itemsInfo': {
                                        'promoPrice': {
                                            'value': str(promo_price),
                                            'currency': 'RUR',
                                        },
                                        'discount': {
                                            'percent': percent,
                                            'oldMin': str(old_price),
                                        },
                                        'constraints': {
                                            'allow_berubonus': promo.blue_flash.allow_berubonus,
                                            'allow_promocode': promo.blue_flash.allow_promocode,
                                        },
                                    },
                                }
                            ],
                        }
                    ],
                    allow_different_len=False,
                )

    def __should_not(self, msku, offer, extra_param=''):
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('sku_offers', 'prime'):
                params = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1' + extra_param
                response = self.report.request_json(params.format(place=place, msku=msku, rgb=rgb))
                # блок промо должен отсутстовать
                self.assertFragmentIn(
                    response,
                    [
                        {
                            'prices': {
                                'value': str(offer.price),
                                'discount': Absent(),
                            },
                            'entity': 'offer',
                            'wareId': offer.waremd5,
                            'promos': Absent(),
                        }
                    ],
                )

    def test_blue_flash(self):
        self.__should(msku_1, blue_offer_1, promo1, promo1.blue_flash.items[0]['price']['value'])
        self.__should(msku_2, blue_offer_2, promo1, promo1.blue_flash.items[1]['price']['value'])
        self.__should(msku_A, blue_offer_A, promoA, None)

        self.dynamic.loyalty += [
            DynamicBluePromosBlacklist(
                blacklist=[
                    (888, blue_offer_1.offerid),
                    (777, 'just-a-fake-offer-id'),
                ]
            )
        ]
        self.__should(msku_1, blue_offer_1, promo1, promo1.blue_flash.items[0]['price']['value'])
        self.__should(msku_2, blue_offer_2, promo1, promo1.blue_flash.items[1]['price']['value'])
        self.__should(msku_A, blue_offer_A, promoA, None)
        self.dynamic.loyalty += [
            DynamicBluePromosBlacklist(
                blacklist=[
                    (777, blue_offer_1.offerid),
                    (777, blue_offer_A.offerid),
                ]
            )
        ]
        self.__should_not(msku_1.sku, blue_offer_1)
        self.__should_not(msku_A.sku, blue_offer_A)
        self.__should(msku_2, blue_offer_2, promo1, promo1.blue_flash.items[1]['price']['value'])
        self.__should(msku_8, blue_offer_8, promo8, promo8.blue_flash.items[0]['price']['value'])
        self.__should(msku_9, blue_offer_9, promo9, promo9.blue_flash.items[0]['price']['value'])
        # Акция блокируется, так как цена по акции выше, чем цена оффера без акции
        self.__should_not(msku_10.sku, blue_offer_10)

    def test_compact_offer_info(self):
        # Проверяем, что compact_offer_info flag ничего не ломает и все промо показываются как надо
        offers = [blue_offer_1, blue_offer_A]
        promos = [promo1, promoA]
        for i in range(0, 2):
            blue_offer = offers[i]
            promo = promos[i]
            promo_price = promo.blue_flash.items[0]['price']['value']
            if i == 0:
                # случай promo = promo1, когда старая цена берётся из оффера (old_price в акции не задана)
                old_price = blue_offer.price
            else:
                # случай promo = promoA, когда старая цена берётся из акции
                old_price = promo.blue_flash.items[0]['old_price']['value']
            for rgb in ('blue', 'green', 'green_with_blue'):
                for compact_offer_info in ('', 'price_and_promo'):
                    # выбор байбокса нужен детерминированный, для этого фиксируем yandexuid (MARKETOUT-16443)
                    params = 'place=offerinfo&rids=0&regset=1&pp=18&offerid={offerid}&rgb={rgb}&yandexuid=1&compact-offer-output='
                    params += compact_offer_info
                    response = self.report.request_json(params.format(offerid=blue_offer.waremd5, rgb=rgb))

                    # проверяем, что в компактной форме некоторые поля отсутствуют
                    if compact_offer_info == 'price_and_promo':
                        self.assertFragmentIn(
                            response,
                            [
                                {
                                    'pictures': Absent(),
                                    'marketSku': Absent(),
                                    'sku': Absent(),
                                    'ownMarketPlace': Absent(),
                                    'supplierSku': Absent(),
                                    'shopSku': Absent(),
                                    'shop_category_path': Absent(),
                                }
                            ],
                            allow_different_len=False,
                        )

                    # проверяем что в выдаче есть оффер с корректным блоком "promo"
                    self.assertFragmentIn(
                        response,
                        [
                            {
                                'wareId': blue_offer.waremd5,
                                'prices': {
                                    'value': str(promo_price),
                                    'discount': {
                                        'oldMin': str(old_price),
                                    },
                                },
                                'promos': [
                                    {
                                        'type': promo.type_name,
                                        'key': promo.key,
                                        'url': promo.url or Absent(),
                                        'startDate': NotEmpty(),
                                        'endDate': NotEmpty(),
                                        'itemsInfo': {
                                            'promoPrice': {
                                                'value': str(promo_price),
                                                'currency': 'RUR',
                                            },
                                            'discount': {
                                                'percent': ((old_price - promo_price) * 100 / old_price),
                                                'oldMin': str(old_price),
                                            },
                                            'constraints': {
                                                'allow_berubonus': promo.blue_flash.allow_berubonus,
                                                'allow_promocode': promo.blue_flash.allow_promocode,
                                            },
                                        },
                                    }
                                ],
                            }
                        ],
                        allow_different_len=False,
                    )

    def test_promo_inactive(self):
        # проверяем отключение акций по времени
        self.__should_not(msku_3.sku, blue_offer_3)
        self.__should_not(msku_4.sku, blue_offer_4)
        # проверяем акция не работает без белого списка
        self.__should_not(msku_5.sku, blue_offer_5)
        # Промо блокируется, если размер скидки меньше 5% и меньше 500 рублей
        self.__should_not(msku_6.sku, blue_offer_6)

    def test_promo_in_buybox(self):
        # в msku_7 2 оффера, с базовой ценой 1000 (blue_offer_7) и 800 (blue_offer_7a)
        # но промо-7 меняет цену оффера с 1000 на 700, потому он выиграет buybox
        self.__should(msku_7, blue_offer_7, promo7, promo7.blue_flash.items[0]['price']['value'])

        self.__should_not(msku_7.sku, blue_offer_7a, extra_param='&rearr-factors=promo_price_in_buybox=0')

        # если акцию для оффера отключить - то выиграет второй оффер, как более дешевый
        self.dynamic.loyalty += [
            DynamicBluePromosBlacklist(
                blacklist=[
                    (FEED, blue_offer_7.offerid),
                ]
            )
        ]
        self.__should_not(msku_7.sku, blue_offer_7a)


if __name__ == '__main__':
    main()
