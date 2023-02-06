#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DeliveryBucket,
    MarketSku,
    Model,
    Neighbour,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import Contains


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.enable_knn = True

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[213]),
            Shop(fesh=2, priority_region=2, regions=[2]),
            Shop(fesh=3, priority_region=192, regions=[225]),
        ]

        cls.index.outlets += [
            Outlet(point_id=3, fesh=3, region=192),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=1003,
                fesh=3,
                options=[PickupOption(outlet_id=3, price=0)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(ts=100101, title='Moscow offer', fesh=1, hid=101, waremd5='NpMM-ycrD02JYiBIu6fTfQ', randx=1),
            Offer(ts=100102, title='Another moscow offer', fesh=1, hid=101, waremd5='t8OGz2APRknMuvITa_N6lw', randx=2),
            Offer(ts=100201, title='Piter offer', fesh=2, hid=100, waremd5='nRjn2L51_EHjrhSsmeFQZA', randx=3),
            Offer(ts=100202, title='Another Piter offer', fesh=2, hid=100, waremd5='Kb0UAv-nFmohRdQpznK3Xg', randx=4),
            Offer(
                ts=100301,
                title='In KNN index 1',
                fesh=3,
                hid=100,
                pickup_buckets=[1003],
                waremd5='DIzlUWMFXE2lCoHgonNGjA',
                randx=5,
            ),
            Offer(
                ts=100302,
                title='In KNN index 2',
                fesh=3,
                hid=101,
                pickup_buckets=[1003],
                waremd5='XL39Dfoio_ynJmN2iH9Umw',
                randx=6,
            ),
        ]

        cls.index.mskus += [
            MarketSku(sku=1001, title='In KNN index 3', randx=7, blue_offers=[BlueOffer(ts=100303, randx=7)])
        ]

        cls.index.models += [
            Model(ts=111111, hid=100, title='Product in knn index', randx=10),
            Model(ts=222222, hid=101, title='Other product in knn index', randx=11),
        ]

        cls.index.dssm.query_embedding.on('offer from Moscow shop').random()
        cls.index.dssm.query_embedding.on('то не знаю что').random()
        cls.index.dssm.query_embedding.on('url:"http://some.url/as/literal"').random()

        cls.index.dssm.reformulation_query_embedding.on('offer from Moscow shop').random()
        cls.index.dssm.reformulation_query_embedding.on('то не знаю что').random()
        cls.index.dssm.reformulation_query_embedding.on('url:"http://some.url/as/literal"').random()

        # реквизард превращает запрос в литерал url
        cls.reqwizard.on_request('url:"http://some.url/as/literal"').respond(
            remove_query=True,
            qtree='cHic46rh4uFgEGCQ4FRg0GAyYBBiLi3KkWJQYtASUSrOz03VA3L1E4v1czJLUosSc4wYrHg4mIHKGYDKGQwYHBg'
            '8GAIYIhgSGDLmTLu3lmkCI8MsRpARixix6t_EyLCXkQEITjAyXGBMMWCwYHQSA9kPslKD0YARahhjFUMDIwPQNAD6jyMM',
        )
        cls.reqwizard.on_default_request().respond()

        cls.index.knn.on_query('offer from Moscow shop').respond(
            [
                Neighbour(ts=111111, dist=18000),
                Neighbour(ts=222222, dist=15900),
                Neighbour(ts=100301, dist=6700),
                Neighbour(ts=100302, dist=5900),
                Neighbour(ts=100303, dist=4870),
            ]
        )

        cls.index.knn.on_query('url:"http://some.url/as/literal"').respond(
            [Neighbour(ts=100301, dist=8000), Neighbour(ts=100202, dist=8000), Neighbour(ts=100101, dist=7000)]
        )

        cls.index.knn.on_query('то не знаю что').respond(
            [
                Neighbour(ts=111111, dist=18000),
                Neighbour(ts=222222, dist=15900),
                Neighbour(ts=100301, dist=6700),
                Neighbour(ts=100302, dist=5900),
                Neighbour(ts=100303, dist=4870),
            ]
        )

    def test_no_knn_index(self):
        '''Фиксируем как работает обычный пантерный индекс
        по запросу [offer from Moscow shop]
        находятся только офферы содержащие слова из запроса (не находится оффер ts=100301 и модель ts=111111)
        находятся только офферы доставляемые в Мск (Питерские офферы отсеялись по доставке)
        (как с кворумом так и без кворума)

        по умолчанию knn выключен на синем маркете, в контентном апи (на виджетах и советнике &api=content&client=widget|sovetnik)
        '''

        for query in [
            'place=prime&text=offer from Moscow shop&rids=213&rearr-factors=market_use_knn=0',
            # knn выключен по умолчанию на апи и советнике
            'place=prime&text=offer from Moscow shop&rids=213&api=content&client=widget',
            'place=prime&text=offer from Moscow shop&rids=213&api=content&client=sovetnik',
        ]:

            response = self.report.request_json(query + '&debug=da')
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 2,
                        'results': [{'titles': {'raw': 'Moscow offer'}}, {'titles': {'raw': 'Another moscow offer'}}],
                    }
                },
                allow_different_len=False,
            )

            self.assertFragmentIn(response, {"how": [{"args": Contains("use_knn: false")}]})

    def test_knn_index(self):
        '''Как работает knn-индекс:
        проверяем что он действительно "набирает" документы
        также при отключенном кворуме в выдаче будут документы, которые не находятся пантерой
        находятся 6 документа: 2 оффера находимых пантерой и 2 оффера и 2 модели заложенные в knn-индекс

        по умолчанию knn-индекс включен везде кроме виджетов и советника на контентном апи
        '''

        for query in [
            'place=prime&text=offer from Moscow shop&rids=213&rearr-factors=market_use_knn=1',
            # knn включен по умолчанию на белом маркете
            'place=prime&text=offer from Moscow shop&rids=213',
            # knn может быть включен явно на api и советнике
            'place=prime&text=offer from Moscow shop&rids=213&api=content&сlient=widget&rearr-factors=market_use_knn=1',
            'place=prime&text=offer from Moscow shop&rids=213&api=content&client=sovetnik&rearr-factors=market_use_knn=1',
        ]:

            response = self.report.request_json(query + '&debug=da')

            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 6,
                        'results': [
                            {'titles': {'raw': 'Product in knn index'}},
                            {'titles': {'raw': 'Other product in knn index'}},
                            {'titles': {'raw': 'Moscow offer'}},
                            {'titles': {'raw': 'Another moscow offer'}},
                            {'entity': 'regionalDelimiter'},
                            {'titles': {'raw': 'In KNN index 1'}},
                            {'titles': {'raw': 'In KNN index 2'}},
                        ],
                    }
                },
                allow_different_len=False,
            )

            self.assertFragmentIn(response, {"how": [{"args": Contains("use_knn: true")}]})
            self.assertFragmentIn(response, {"how": [{"args": Contains("knn_offers_top: 100")}]})
            self.assertFragmentIn(response, {"how": [{"args": Contains("knn_models_top: 20")}]})

        # knn включен на синем маркете
        response = self.report.request_json('place=prime&text=offer from Moscow shop&rids=213&debug=da&rgb=blue')
        self.assertFragmentIn(response, {"how": [{"args": Contains("use_knn: true")}]})
        self.assertFragmentIn(response, {"how": [{"args": Contains("knn_offers_top: 100")}]})

    def test_flags_for_knn_top(self):
        # флагами panther_offer_tpsz panther_blue_offer_tpsz panther_model_tpsz
        # можно задать топ кнн на офферном шарде, синем офферном шарде и в модельной коллекции
        response = self.report.request_json(
            'place=prime&text=то не знаю что&rids=0&debug=da'
            '&rearr-factors=market_knn_offers_top=120;market_knn_models_top=180'
            '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(response, {"how": [{"args": Contains("use_knn: true")}]})
        self.assertFragmentIn(response, {"how": [{"args": Contains("knn_offers_top: 120")}]})
        self.assertFragmentIn(response, {"how": [{"args": Contains("knn_models_top: 180")}]})

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'In KNN index 1'}},
                    {'titles': {'raw': 'In KNN index 2'}},
                    {'titles': {'raw': 'In KNN index 3'}},
                    {'titles': {'raw': 'Product in knn index'}},
                    {'titles': {'raw': 'Other product in knn index'}},
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        response = self.report.request_json(
            'place=prime&text=то не знаю что&rids=0&debug=da'
            '&rearr-factors=market_knn_offers_top=2;market_knn_models_top=1'
            ';market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'In KNN index 2'}},
                    {'titles': {'raw': 'In KNN index 3'}},
                    {'titles': {'raw': 'Other product in knn index'}},  # и одна модель
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

    def test_knn_index_for_mobile_apps(self):
        # knn включен для приложений маркета

        response = self.report.request_json(
            'place=prime&text=offer from Moscow shop&rids=213&debug=da&api=content&content-api-client=18932'
        )
        self.assertFragmentIn(response, {"how": [{"args": Contains("use_knn: true")}]})

        response = self.report.request_json(
            'place=prime&text=offer from Moscow shop&rids=213&debug=da&api=content&content-api-client=101'
        )
        self.assertFragmentIn(response, {"how": [{"args": Contains("use_knn: true")}]})

        response = self.report.request_json(
            'place=prime&text=offer from Moscow shop&rids=213&debug=da&api=content&content-api-client=6713'
        )
        self.assertFragmentIn(response, {"how": [{"args": Contains("use_knn: true")}]})

    def test_knn_index_for_geo(self):
        # knn включается для geo автоматически при включении пантеры

        response = self.report.request_json(
            'place=geo&text=offer from Moscow shop&rids=192&rearr-factors=geo_allow_panther=1&debug=da'
        )

        self.assertFragmentIn(response, {"how": [{"args": Contains("use_knn: true")}]})
        # находятся 2 оффера доступные в 192 регионе по самовывозу (но группировка по магазину)
        self.assertFragmentIn(
            response,
            {'search': {'total': 1, 'totalOffers': 2, 'results': [{'titles': {'raw': 'In KNN index 2'}}]}},
            allow_different_len=False,
        )

        # при этом они не находятся при выключении knn
        response = self.report.request_json(
            'place=geo&text=offer from Moscow shop&rids=192&rearr-factors=geo_allow_panther=1;market_use_knn=0&debug=da'
        )
        self.assertFragmentIn(response, {"how": [{"args": Contains("use_knn: false")}]})
        self.assertFragmentIn(response, {'search': {'total': 0, 'totalOffers': 0}})

    def test_knn_index_with_panther_tpsz_restriction(self):
        '''Ограничение panther_offer_tpsz и panther_model_tpsz влияет также и на офферы из knn-индекса
        Общее количество офферов набранных пантерой и knn индексом не будет превышать заданных порогов
        '''

        # при выкрученной в 0 пантере (tpsz=0) и включенном knn-индексе - находятся только офферы из knn-индекса
        response = self.report.request_json(
            'place=prime&text=offer from Moscow shop&local-offers-first=0'
            '&rearr-factors=panther_offer_tpsz=0;panther_model_tpsz=0;market_use_knn=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 5,
                    'results': [
                        {'titles': {'raw': 'Product in knn index'}},
                        {'titles': {'raw': 'Other product in knn index'}},
                        {'titles': {'raw': 'In KNN index 1'}},
                        {'titles': {'raw': 'In KNN index 2'}},
                        {'titles': {'raw': 'In KNN index 3'}},
                    ],
                },
            },
            allow_different_len=False,
        )

        # TODO kzhagorina Что-то здесь не очень корректно работает мок кнн надо разобраться (должны быть доки 3, 2, other или не должны)
        # количество найденных офферов не превышает panther_offer_tpsz и panther_model_tpsz
        # в первую очередь попадают документы из knn
        response = self.report.request_json(
            'place=prime&text=offer from Moscow shop&local-offers-first=0'
            '&rearr-factors=panther_offer_tpsz=2;panther_model_tpsz=1;market_use_knn=1;'
            'market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'results': [
                        {'titles': {'raw': 'Product in knn index'}},
                        {'titles': {'raw': 'In KNN index 1'}},
                        {'titles': {'raw': 'In KNN index 2'}},
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_knn_index_without_panter(self):
        '''knn индекс может работать при выключенной пантере
        находятся 2 оффера и 2 модели заложенные в knn-индекс'''

        response = self.report.request_json(
            'place=prime&text=offer from Moscow shop&rids=0&local-offers-first=0'
            '&rearr-factors=market_use_knn=1;allow_panther=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 5,
                    'results': [
                        {'titles': {'raw': 'Product in knn index'}},
                        {'titles': {'raw': 'Other product in knn index'}},
                        {'titles': {'raw': 'In KNN index 1'}},
                        {'titles': {'raw': 'In KNN index 2'}},
                        {'titles': {'raw': 'In KNN index 3'}},
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_knn_index_with_hid(self):
        '''Проверяем что офферы из knn-индекса фильтруются по литералам в запросе (по категории)
        Оффер "In KNN index 2" и модель "Other product in knn index" из 101 категории отфильтровались
        '''

        response = self.report.request_json(
            'place=prime&text=offer from Moscow shop&rids=213&hid=100&local-offers-first=0'
            '&rearr-factors=market_use_knn=1'
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [{'titles': {'raw': 'Product in knn index'}}, {'titles': {'raw': 'In KNN index 1'}}],
                }
            },
            allow_different_len=False,
        )

        # оффер из категории 101 не вошел в ответ
        self.assertFragmentNotIn(
            response,
            {
                'titles': {'raw': 'In KNN index 2'},
            },
        )

    def test_knn_index_with_full_query_as_literal(self):
        '''Knn-индекс не работает когда весь запрос превращается в литерал'''

        response = self.report.request_json(
            'place=prime&text=url:"http://some.url/as/literal"&rids=213&debug=da&rearr-factors=market_use_knn=1'
        )
        self.assertFragmentIn(response, {'search': {'total': 0}})

    @classmethod
    def prepare_preserve_order_of_knn(cls):
        cls.index.offers += [
            Offer(ts=98765, title='not-found-by-text 1'),
            Offer(ts=43210, title='not-found-by-text 2'),
        ]
        cls.index.knn.on_query('order').respond([Neighbour(ts=98765, dist=10000), Neighbour(ts=43210, dist=9000)])

    def test_preserve_order_of_knn(self):
        """
        Проверяем, что, если все кнн не влезают в топ, набираются те, у которых расстояние "лучше"
        """
        response = self.report.request_json(
            'place=prime&text=order&rearr-factors=panther_offer_tpsz=1;panther_model_tpsz=0;market_use_knn=1'
        )

        self.assertFragmentIn(
            response,
            {'search': {'total': 1, 'results': [{'titles': {'raw': 'not-found-by-text 1'}}]}},
            allow_different_len=False,
        )

    @classmethod
    def prepare_bad_tpsz(cls):
        # поймали редкий кейс когда > 20 доков и panther_offer_tpsz=0
        # https://paste.yandex-team.ru/3928560
        cls.index.dssm.reformulation_query_embedding.on('badtpsz').random()
        cls.index.offers += [Offer(ts=4500 + i, title="badtpsz") for i in range(1, 99)]
        cls.index.knn.on_query('badtpsz').respond([Neighbour(ts=4500 + i, dist=i * 1000) for i in range(1, 99)])

    def test_bad_tpsz(self):
        _ = self.report.request_json('place=prime&text=badtpsz&rearr-factors=panther_offer_tpsz=0')


if __name__ == '__main__':
    main()
