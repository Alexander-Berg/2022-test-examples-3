#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, MarketSku, Model, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import Absent, EmptyList, NotEmpty
from core.bigb import SkuPurchaseEvent, BeruSkuOrderCountCounter


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.fixed_index_generation = '20200101_0300'

        models = list(range(1000, 1100))
        cls.index.models += [Model(hyperid=m, hid=m / 10) for m in models]

        cls.index.shops += [
            Shop(fesh=101, priority_region=213, regions=[225]),
            Shop(fesh=102, priority_region=213, regions=[225]),
            Shop(fesh=103, priority_region=2, regions=[225]),
        ]

        cls.index.offers += [
            # Московские оффера
            Offer(hyperid=1000, fesh=101, price=10000),
            Offer(hyperid=1000, fesh=102, price=12345),
            Offer(hyperid=1002, fesh=101, price=54321),
            # Питерские оффера
            Offer(hyperid=1002, fesh=103, price=50000),
            Offer(hyperid=1003, fesh=103, price=40000),
        ]

    def test_main(self):
        # В запросе 5 моделей. Модели 100500 не существует вообще, а модели 1001 нет в наличии нигде
        # Хаотичный порядок моделей в запросе, что бы убедиться, что он сохраняется
        # Информация о ценах аггрегируется из всех регионов
        models = ('', '1002', '1001', '1000', '100500', '1003')
        for region in (2, 213):
            for param in ('&hyperid=', '&modelid='):
                response = self.report.request_json('place=recom_models&{}&rids={}'.format(param.join(models), region))
                self.assertFragmentIn(
                    response,
                    {
                        'search': {
                            'total': 3,
                            'results': [
                                {
                                    'entity': 'product',
                                    'id': 1002,
                                    'prices': {
                                        'min': "50000",
                                        'max': "54321",
                                    },
                                },
                                {
                                    'entity': 'product',
                                    'id': 1000,
                                    'prices': {
                                        'min': "10000",
                                        'max': "12345",
                                    },
                                },
                                {
                                    'entity': 'product',
                                    'id': 1003,
                                    'prices': {
                                        'min': "40000",
                                        'max': "40000",
                                    },
                                },
                            ],
                        },
                        'filters': Absent(),
                        'sorts': EmptyList(),
                    },
                    preserve_order=True,
                    allow_different_len=False,
                )

    def test_urls(self):
        """Проверяем, что в оффере приходит урл до модели в маркете"""
        response = self.report.request_json('place=recom_models&hyperid=1002')
        self.assertFragmentIn(response, {"search": {"results": [{"urls": {"marketModelOffer": NotEmpty()}}]}})

    @classmethod
    def prepare_sku_enrichment_from_bigb(cls):
        cls.index.mskus += [
            MarketSku(
                hid=100,
                hyperid=1002,
                sku=170,
                title="MSKU-170",
                blue_offers=[BlueOffer(price=170, fesh=101)],
            ),
            MarketSku(
                hid=100,
                hyperid=1002,
                sku=171,
                title="MSKU-171",
                blue_offers=[BlueOffer(price=171, fesh=101)],
            ),
        ]

        sku_purchases_counter = BeruSkuOrderCountCounter(
            [
                SkuPurchaseEvent(sku_id=70, count=1),
                SkuPurchaseEvent(sku_id=170, count=1),
            ]
        )
        cls.bigb.on_request(yandexuid='007', client='merch-machine').respond(counters=[sku_purchases_counter])

    def test_sku_enrichment_from_bigb(self):
        """
        Если пользователь покупал какую-то конкретную ску данной модели, то в выводе плейса будет предложена именно
        эта ску. Данные берутся из бигб
        Для этого делаем запрос с yandexuid=007, чтобы было на что счетчик возвращать
        """

        response = self.report.request_json('place=recom_models&hyperid=1002&yandexuid=007')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "id": 1002,
                            "offers": {"items": [{"marketSku": "170", "sku": "170", "titles": {"raw": "MSKU-170"}}]},
                            "titles": {"raw": "HYPERID-1002"},
                        },
                    ]
                }
            },
        )

    def test_show_log_generation(self):
        self.report.request_json('place=recom_models&hyperid=1002')
        self.show_log.expect(record_type=1, hyper_id=1002, index_generation=self.index.fixed_index_generation)


if __name__ == '__main__':
    main()
