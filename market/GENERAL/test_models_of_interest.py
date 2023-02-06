#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Currency, Model, Shop
from core.testcase import TestCase, main
from core.matcher import ElementCount
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.dj import DjModel

import random


class _Offers(object):
    waremd5s = [
        'Sku1Price5-IiLVm1Goleg',
        'Sku2Price50-iLVm1Goleg',
        'Sku3Price45-iLVm1Goleg',
        'Sku4Price36-iLVm1Goleg',
        'Sku5Price15-iLVm1Goleg',
        'Sku5Price16-iLVm1Goleg',
    ]
    feed_ids = [1] * len(waremd5s)
    prices = [5, 50, 45, 36, 15, 16]
    shop_skus = ['Feed_{feedid}_sku{i}'.format(feedid=feedid, i=i + 1) for i, feedid in enumerate(feed_ids)]
    sku_offers = [
        BlueOffer(price=price, vat=Vat.VAT_10, offerid=shop_sku, feedid=feedid, waremd5=waremd5)
        for feedid, waremd5, price, shop_sku in zip(feed_ids, waremd5s, prices, shop_skus)
    ]
    model_ids = list(range(1, len(waremd5s) + 1))


class T(TestCase):
    """
    Набор тестов для плейса Рекомендации на основе истории просмотров" для Синего Маркета
    products_by_history&cpa=real
    """

    @classmethod
    def prepare(cls):
        """
        Модели, офферы и конфигурация для выдачи products_by_history&cpa=real
        """

        # shops
        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
            ),
        ]

        # models with randomly selected ts
        random.seed(0)
        random_ts = list(range(1, len(_Offers.model_ids) + 1))
        random.shuffle(random_ts)

        # (model_id,category_id):
        # (1,1), (2,2), (3,0), (4,1), (5,2), (6,0)
        cls.index.models += [
            Model(hyperid=model_id, hid=(model_id % 3) + 1, ts=ts) for model_id, ts in zip(_Offers.model_ids, random_ts)
        ]

        # market skus
        cls.index.mskus += [
            MarketSku(
                title='Blue offer {sku}'.format(sku=shop_sku),
                hyperid=hyperid,
                sku='{i}'.format(i=hyperid),
                waremd5='Sku{i}-wdDXWsIiLVm1goleg'.format(i=hyperid),
                blue_offers=[sku_offer],
            )
            for hyperid, shop_sku, sku_offer in zip(_Offers.model_ids, _Offers.shop_skus, _Offers.sku_offers)
        ]

        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='14000').respond([DjModel(id='50000')])
        cls.dj.on_request(yandexuid='14001').respond(
            [DjModel(id='2'), DjModel(id='4'), DjModel(id='3'), DjModel(id='5'), DjModel(id='6')]
        )

    def test_user_with_no_history(self):
        """
        Проверка корректности работы при возврате ихвилем пустого вектора моделей
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            nouser_request = 'place=products_by_history&{}&yandexuid=14000'.format(suffix)
            response = self.report.request_json(nouser_request)
            self.assertFragmentIn(response, {'total': 0})

    def test_total_renderable(self):
        """
        Проверяется, что общее количество для показа = total
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            request = 'place=products_by_history&{}&yandexuid=14001'.format(suffix)
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {'total': 5})

            response = self.report.request_json(request + '&numdoc=2')
            self.assertFragmentIn(response, {'total': 5})
            # Pay Attention: after removing of rgb=blue block, reduce number of tskv_log expected number to 2
            self.access_log.expect(total_renderable='5').times(4)

            """
            Проверка поля url_hash в show log
            """
            self.show_log_tskv.expect(url_hash=ElementCount(32))

    def test_order(self):
        """
        Порядок выдачи должен соответствовать порядку рекомендаций от ichwill
        c учетом категорийного разнообразия.
        В индексе модели размещены в другом порядке (см. поле ts)
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            response = self.report.request_json('place=products_by_history&{}&yandexuid=14001'.format(suffix))
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 5,
                        'results': [
                            {'entity': 'product', 'id': 2},
                            {'entity': 'product', 'id': 4},
                            {'entity': 'product', 'id': 3},
                            {'entity': 'product', 'id': 5},
                            {'entity': 'product', 'id': 6},
                        ],
                    }
                },
                preserve_order=True,
            )


if __name__ == '__main__':
    main()
