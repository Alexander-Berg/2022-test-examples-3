#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Promo, PromoType, Shop
from core.testcase import TestCase, main
from core.types.autogen import b64url_md5

from datetime import datetime

FESH_REGULAR = 774


def hyperid(model_n):
    return 400 + model_n


def offerid(model_n, offer_n):
    return str(200 + model_n * 10 + offer_n)


def model(n):
    return Model(hyperid=hyperid(n), title='philips model %d' % n)


def fesh(offer_n):
    return FESH_REGULAR + offer_n


def offer_title(model_n, offer_n):
    return 'offer_for_philips %d %s' % (model_n, offer_n)


def promo_code(model_n, offer_n):
    return Promo(
        promo_type=PromoType.PROMO_CODE,
        start_date=datetime(1980, 1, 1),
        end_date=datetime(2050, 1, 1),
        key='promo_code_%d_%s' % (model_n, offer_n),
        url='http://my.url',
        promo_code="my promo code",
        discount_value=30,
    )


def promo_flash(model_n, offer_n):
    return Promo(
        promo_type=PromoType.FLASH_DISCOUNT,
        start_date=datetime(1985, 6, 20),
        end_date=datetime(1985, 6, 26),
        key='promo_flash_%d_%s' % (model_n, offer_n),
        url='http://my.url',
    )


def promo(model_n, offer_n):
    t = (offer_n + model_n) % 3
    if t == 1:
        return promo_code(model_n, offer_n)
    if t == 2:
        return promo_flash(model_n, offer_n)
    return None


def offer(model_n, offer_n):
    title = offer_title(model_n, offer_n)
    return Offer(
        title=title,
        offerid=offerid(model_n, offer_n),
        fesh=fesh(offer_n),
        hyperid=hyperid(model_n),
        promo=promo(model_n, offer_n),
        waremd5=b64url_md5(title),
    )


def shop(n):
    return Shop(fesh=fesh(n), priority_region=213)


def response_model(model_n):
    return {
        "results": [
            {
                'entity': 'product',
                "id": hyperid(model_n),
            }
        ]
    }


def response_offer(model_n, offer_n):
    return {
        "results": [
            {
                'entity': 'offer',
                'model': {'id': hyperid(model_n)},
                'shop': {'feed': {'offerId': offerid(model_n, offer_n)}},
            }
        ]
    }


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.shops += [shop(n) for n in range(10)]

        cls.index.models += [
            model(1),
            model(2),
            model(3),
            model(4),
        ]
        cls.index.offers += [
            offer(1, 1),
            offer(1, 2),
            offer(2, 1),
            offer(2, 3),
            offer(3, 1),
            offer(3, 2),
            offer(3, 4),
        ]

    def test_default_collapsing(self):
        '''
        проверяем, что по умолчанию - модельная выдача, но можно явно попросить офферную
        '''
        response = self.report.request_json("place=shopmodels&fesh=%d" % fesh(1))  # implicit (models)
        self.assertFragmentIn(response, {"results": [{'entity': 'product'}]})
        self.assertFragmentNotIn(response, {"results": [{'entity': 'offer'}]})

        response = self.report.request_json("place=shopmodels&allow-collapsing=1&fesh=%d" % fesh(1))  # explicit models
        self.assertFragmentIn(response, {"results": [{'entity': 'product'}]})
        self.assertFragmentNotIn(response, {"results": [{'entity': 'offer'}]})

        response = self.report.request_json("place=shopmodels&allow-collapsing=0&fesh=%d" % fesh(1))  # explicit offers
        self.assertFragmentIn(response, {"results": [{'entity': 'offer'}]})
        self.assertFragmentNotIn(response, {"results": [{'entity': 'product'}]})

    def test_fesh_models(self):
        '''
        проверяем модельную выдачу для магазина
        '''
        response = self.report.request_json("place=shopmodels&allow-collapsing=1&fesh=%d" % fesh(2))
        # модели из нужного магазина
        self.assertFragmentIn(response, response_model(1))
        self.assertFragmentIn(response, response_model(3))
        # а в этом - нет
        self.assertFragmentNotIn(response, response_model(2))

    def test_fesh_offers(self):
        '''
        проверяем офферную выдачу для магазина
        '''
        response = self.report.request_json("place=shopmodels&allow-collapsing=0&fesh=%d" % fesh(3))
        # офферы из нужного магазина
        self.assertFragmentIn(response, response_offer(2, 3))
        # офферы остальные
        self.assertFragmentNotIn(response, response_offer(1, 1))
        self.assertFragmentNotIn(response, response_offer(2, 1))
        self.assertFragmentNotIn(response, response_offer(2, 2))
        self.assertFragmentNotIn(response, response_offer(3, 1))
        self.assertFragmentNotIn(response, response_offer(3, 2))
        self.assertFragmentNotIn(response, response_offer(3, 3))
        self.assertFragmentNotIn(response, response_offer(3, 4))

    def test_fesh_promo_offers(self):
        '''
        проверяем офферную выдачу для магазина и промо
        '''
        response = self.report.request_json(
            "place=shopmodels&allow-collapsing=0&fesh=%d&promo-type=%s" % (fesh(1), PromoType.PROMO_CODE)
        )
        self.assertFragmentIn(response, response_offer(3, 1))
        self.assertFragmentNotIn(response, response_offer(1, 1))
        self.assertFragmentNotIn(response, response_offer(2, 1))

        response = self.report.request_json(
            "place=shopmodels&allow-collapsing=0&fesh=%d&promo-type=%s" % (fesh(1), PromoType.FLASH_DISCOUNT)
        )
        self.assertFragmentIn(response, response_offer(1, 1))
        self.assertFragmentNotIn(response, response_offer(2, 1))
        self.assertFragmentNotIn(response, response_offer(3, 1))

    def test_fesh_promo_models(self):
        '''
        проверяем модельную выдачу для магазина и промо
        '''
        response = self.report.request_json(
            "place=shopmodels&allow-collapsing=1&fesh=%d&promo-type=%s" % (fesh(1), PromoType.PROMO_CODE)
        )
        self.assertFragmentIn(response, response_model(3))
        self.assertFragmentNotIn(response, response_model(1))
        self.assertFragmentNotIn(response, response_model(2))

        response = self.report.request_json(
            "place=shopmodels&allow-collapsing=1&fesh=%d&promo-type=%s" % (fesh(1), PromoType.FLASH_DISCOUNT)
        )
        self.assertFragmentIn(response, response_model(1))
        self.assertFragmentNotIn(response, response_model(2))
        self.assertFragmentNotIn(response, response_model(3))


if __name__ == '__main__':
    main()
