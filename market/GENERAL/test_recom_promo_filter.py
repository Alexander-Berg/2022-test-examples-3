#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryOption,
    HyperCategory,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Promo,
    PromoType,
    RegionalDelivery,
    Shop,
)
from core.types.offer_promo import PromoBlueCashback
from core.testcase import TestCase, main
from core.dj import DjModel

model_ids = [model_id for model_id in range(10, 18)]
user_id = 1


class T(TestCase):
    """MARKETOUT-24939 Test that promo-type, promo-check-min-price and nid filtering works for recom places"""

    @classmethod
    def prepare(cls):
        """Prepare index"""

        cls.index.hypertree += [
            HyperCategory(
                hid=1,
            ),
            HyperCategory(
                hid=2,
            ),
        ]
        cls.index.navtree += [
            NavCategory(nid=1, hid=1),
            NavCategory(nid=2, hid=2),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[213], name='Котиковый магаз', cpa=Shop.CPA_REAL, is_dsbs=True)
        ]

        cls.index.models += [Model(hid=1, hyperid=model_id) for model_id in model_ids[:4]]
        cls.index.models += [Model(hid=2, hyperid=model_id) for model_id in model_ids[4:]]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=1)]),
                ],
            ),
        ]

        promos = {}
        for model_id in model_ids[:4]:
            promos[model_id] = Promo(
                promo_type=PromoType.BLUE_CASHBACK,
                key='promo%s' % model_id,
                shop_promo_id='cashback_%s' % model_id,
                blue_cashback=PromoBlueCashback(share=0, version=6, priority=2),
            )
        for model_id in model_ids[4:]:
            promos[model_id] = Promo(
                promo_type=PromoType.PROMO_CODE,
                discount_value=50,
                key='promo%s' % model_id,
                shop_promo_id='promocode_%s' % model_id,
            )

        # default offers with promo tags
        cls.index.offers += [
            Offer(
                hyperid=model_id,
                price=200000,
                ts=1,
                fesh=1,
                delivery_buckets=[1],
                cpa=Offer.CPA_REAL,
                promo=[promos[model_id]],
            )
            for model_id in model_ids
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.02)

        # non-default offers to create statistics for promo-check-min-price
        # even-numbered ones are more expensive, odd-numbered ones are less expensive
        cls.index.offers += [
            Offer(hyperid=model_id, price=300000, ts=2, cpa=Offer.CPA_REAL, promo=[promos[model_id]])
            for model_id in model_ids[::2]
        ]
        cls.index.offers += [
            Offer(hyperid=model_id, price=100000, ts=2, cpa=Offer.CPA_REAL, promo=[promos[model_id]])
            for model_id in model_ids[1::2]
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.01)

        # recommender should return all models from model_ids
        RECOMMENDER_RESPONSE = {
            'models': map(str, model_ids),
            'timestamps': map(str, list(range(len(model_ids), 0, -1))),
        }
        # 40 is magic number in products_by_history.cpp - number of models requested from recommender
        # we need 2*40 models requested when applying promo-filters
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:%s' % user_id, item_count=40, with_timestamps=True
        ).respond(RECOMMENDER_RESPONSE)
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:%s' % user_id, item_count=2 * 40, with_timestamps=True
        ).respond(RECOMMENDER_RESPONSE)
        dj_models = [DjModel(id=modelid) for modelid in model_ids]
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid=user_id).respond(dj_models)
        cls.bigb.on_request(yandexuid=str(user_id), client='merch-machine').respond(counters=[])

    def test_products_by_history(self):
        """Test that products_by_history filters by promo-type, promo-check-min-price and nid"""

        request_base = 'place=products_by_history&rids=213&perks=yandex_cashback&yandexuid=%s' % user_id

        self.__check_request(request_base, model_ids)

        self.__check_request(request_base + '&promo-type=' + PromoType.BLUE_CASHBACK, model_ids[:4])
        self.__check_request(request_base + '&promo-type=' + PromoType.PROMO_CODE, model_ids[4:])

        # with promo-check-min-price we should get only models which default offer is cheapest (even-numbered ones)
        # filter-promo-or-discount uses promo types AllWhiteAllowed
        # only promo-code is left allowe. Other white promo types are absolete @see https://a.yandex-team.ru/arc/trunk/arcadia/market/library/libpromo/common.h#L147
        self.__check_request(request_base + '&promo-check-min-price=1&filter-promo-or-discount=1', model_ids[4::2])

        # without promo-check-min-price we should get all models
        self.__check_request(request_base + '&promo-check-min-price=0&filter-promo-or-discount=1', model_ids[4:])
        self.__check_request(request_base + '&filter-promo-or-discount=1', model_ids[4:])

        self.__check_request(request_base + '&nid=1', model_ids[:4])
        self.__check_request(request_base + '&nid=2', model_ids[4:])

    def __check_request(self, request, model_ids):
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': len(model_ids),
                    'results': [{'entity': 'product', 'type': 'model', 'id': model_id} for model_id in model_ids],
                },
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
