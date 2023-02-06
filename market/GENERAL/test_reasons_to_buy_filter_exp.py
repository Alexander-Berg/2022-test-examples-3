#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    MarketSku,
    Model,
    Offer,
    ReferenceShop,
    Shop,
    Tax,
    Vat,
    YamarecJsonPartition,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.matcher import NoKey

YANDEX_STATION_ID = 1971204201


def serialize_consumer_factors(factors):
    records = [
        """{{"id":"best_by_factor", "factor_name": "{factor_name}", "type":"consumerFactor", "factor_id":"{factor_id}", "value":{value}}}""".format(
            **f
        )
        for f in factors
    ]
    return '[{}]'.format(', '.join(records))


def make_consumer_factors(weights):
    return [
        {'factor_id': str(factor_id + 1), 'factor_name': 'Bla bla {}'.format(factor_id), 'value': value}
        for factor_id, value in enumerate(weights)
        if value is not None
    ]


def serialize_consumer_factor_matrix(matrix):
    """
    [(101, [0.91, 0.92, 0.93]),
     (102, [0.94, 0.95, 0.96]),
    ]
    converts to:
    {
        101: '[{"factor_id":"1", "value":0.91, "id":"best_by_factor", "factor_name":"Blabala 1", "type":"consumerFactor"}
              {"factor_id":"2", "value":0.92, "id":"best_by_factor", "factor_name":"Blabala 2", "type":"consumerFactor"}
              {"factor_id":"3", "value":0.93, "id":"best_by_factor", "factor_name":"Blabala 3", "type":"consumerFactor"}
             ]',
        101: '[{"factor_id":"1", "value":0.94, "id":"best_by_factor", "factor_name":"Blabala 1", "type":"consumerFactor"}
              {"factor_id":"2", "value":0.95, "id":"best_by_factor", "factor_name":"Blabala 2", "type":"consumerFactor"}
              {"factor_id":"3", "value":0.96, "id":"best_by_factor", "factor_name":"Blabala 3", "type":"consumerFactor"}
             ]',
    }
    """
    return {model_id: serialize_consumer_factors(make_consumer_factors(row)) for model_id, row in matrix}


class T(TestCase):
    """
    Проверка вставки данных о причинах купить товар в модельную выдачу
    """

    @classmethod
    def prepare(cls):
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()
        cls.settings.rgb_blue_is_cpa = True

        """
        Данные о причинах купить товар
        """
        cls.index.models += [Model(hyperid=hyperid, hid=10) for hyperid in range(100, 113)]

        cls.index.shops += [
            Shop(
                fesh=2088111,
                datafeed_id=2088110,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=hyperid, price=100, fesh=2088111, cpa=Offer.CPA_REAL, override_cpa_check=True)
            for hyperid in range(100, 113)
        ]

        cls.index.reference_shops += [
            ReferenceShop(hid=2088100, fesh=2088111),
        ]

        cls.index.mskus += [
            MarketSku(
                title="Good camera",
                hyperid=hyperid,
                sku=hyperid * 10 + 1,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=2088110),
                ],
            )
            for hyperid in range(100, 113)
        ]

        reasons_column_names = ["model_id", "reason_json"]
        _ = ['reason1', 'reason2', 'reason3']
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': '1'}, splits=[{}]),
                ],
            ),
        ]
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.REASONS_TO_BUY,
                kind=YamarecPlace.Type.JSON,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecJsonPartition(column_names=reasons_column_names, data={}, splits=[{'split': 'test1'}]),
                    YamarecJsonPartition(
                        column_names=reasons_column_names,
                        data=serialize_consumer_factor_matrix(
                            [
                                (101, [0.91, 0.92, 0.93]),
                            ]
                        ),
                        splits=[{'split': 'test2'}],
                    ),
                    YamarecJsonPartition(
                        column_names=reasons_column_names,
                        data=serialize_consumer_factor_matrix(
                            [
                                (101, [0.91, None, None]),
                                (102, [None, 0.92, None]),
                                (103, [0.92, None, None]),
                                (104, [None, None, 0.95]),
                                (105, [None, 0.99, None]),
                                (106, [None, None, 0.98]),
                            ]
                        ),
                        splits=[{'split': 'split1-test3'}],
                    ),
                    YamarecJsonPartition(
                        column_names=reasons_column_names,
                        data=serialize_consumer_factor_matrix(
                            [
                                (101, [0.91, None, None]),
                                (102, [None, 0.92, None]),
                                (103, [0.92, None, None]),
                                (104, [None, None, 0.95]),
                                (105, [None, None, 0.98]),
                            ]
                        ),
                        splits=[{'split': 'split1-test4'}],
                    ),
                    YamarecJsonPartition(
                        column_names=reasons_column_names,
                        data=serialize_consumer_factor_matrix(
                            [(hyperid, [0.91, None, None]) for hyperid in range(101, 107)]
                        ),
                        splits=[{'split': 'split1-test5'}],
                    ),
                    YamarecJsonPartition(
                        column_names=reasons_column_names,
                        data=serialize_consumer_factor_matrix(
                            [
                                (103, [None, 0.99, None]),
                                # ---page ends here ----
                                (107, [0.91, None, None]),
                                (108, [0.91, None, None]),
                                (109, [0.91, None, None]),
                                (110, [0.91, None, None]),
                                (111, [0.91, None, None]),
                                (112, [0.91, None, None]),
                            ]
                        ),
                        splits=[{'split': 'split1-test6'}],
                    ),
                    YamarecJsonPartition(
                        column_names=reasons_column_names,
                        data=serialize_consumer_factor_matrix(
                            [
                                (101, [None, None, None]),
                                (102, [0.91, 0.92, 0.93]),
                                (103, [0.95, None, 0.89]),
                                (104, [None, None, None]),
                                (105, [0.99, 0.99, 0.99]),
                                (106, [0.90, 0.87, 0.88]),
                            ]
                        ),
                        splits=[{'split': 'split2-test3'}],
                    ),
                    YamarecJsonPartition(
                        column_names=reasons_column_names,
                        data=serialize_consumer_factor_matrix(
                            [
                                (101, [None, 0.81, None]),
                                (102, [0.91, 0.92, 0.93]),
                                (103, [0.95, None, 0.89]),
                                (104, [0.82, None, None]),
                                (105, [0.99, 0.99, 0.99]),
                                (106, [0.90, 0.87, 0.88]),
                            ]
                        ),
                        splits=[{'split': 'split2-test4'}],
                    ),
                    YamarecJsonPartition(
                        column_names=reasons_column_names,
                        data=serialize_consumer_factor_matrix(
                            [
                                (101, [None, 0.81, None]),
                                (104, [0.91, 0.92, 0.93]),
                                (106, [0.95, None, 0.89]),
                                (109, [0.82, None, None]),
                                (110, [0.99, 0.99, 0.99]),
                                (112, [0.90, 0.87, 0.88]),
                            ]
                        ),
                        splits=[{'split': 'split2-test7'}],
                    ),
                    YamarecJsonPartition(
                        column_names=reasons_column_names,
                        data=serialize_consumer_factor_matrix(
                            [
                                (101, [0.99, None, None]),
                                (102, [None, 0.98, None]),
                                (103, [0.81, None, None]),
                                (104, [None, None, 0.81]),
                                (105, [None, 0.99, None]),
                                (106, [None, None, 0.99]),
                            ]
                        ),
                        splits=[{'split': 'split3-test2'}],
                    ),
                    YamarecJsonPartition(
                        column_names=reasons_column_names,
                        data=serialize_consumer_factor_matrix(
                            [
                                (101, [0.99, 0.90, 0.81]),
                                (102, [0.81, 0.99, 0.95]),
                                (103, [0.90, 0.95, 0.90]),
                                (104, [None, 0.81, None]),
                                (105, [None, None, 0.99]),
                            ]
                        ),
                        splits=[{'split': 'split3-test3'}],
                    ),
                    YamarecJsonPartition(
                        column_names=reasons_column_names,
                        data=serialize_consumer_factor_matrix(
                            [(hyperid, [0.99, None, None]) for hyperid in range(101, 107)]
                            + [(hyperid, [0.85, None, None]) for hyperid in range(107, 110)]
                            + [(hyperid, [None, 0.99, None]) for hyperid in range(110, 113)]
                        ),
                        splits=[{'split': 'split3-test4'}],
                    ),
                ],
            ),
        ]

        cls.recommender.on_request_accessory_models(model_id=100, item_count=1000, version='1').respond(
            {'models': map(str, list(range(101, 113)))}
        )

    def test_split1(self):
        """
        MARKETOUT-23571, split 1
        """
        # [test1] крайний случай: все модели без характеристик
        #   6 моделей без хар
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=1&numdoc=6&hyperid=100&rearr-factors=market_reasons_to_buy_filter=1;split=test1&yandexuid=split1-test1'
        )
        self.assertFragmentIn(
            response,
            {'results': [{'id': hyperid, 'reasonsToBuy': NoKey('reasonsToBuy')} for hyperid in range(101, 106)]},
        )
        # [test2] только одна модель с характеристиками, она должна остаться с топовой хар
        #   1 модель с неск хар (A+B+C) + 5 моделей без хар
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=1&numdoc=6&hyperid=100&rearr-factors=market_reasons_to_buy_filter=1;split=test2&yandexuid=split1-test2'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 101, 'reasonsToBuy': make_consumer_factors([None, None, 0.93])},
                ]
                + [{'id': hyperid, 'reasonsToBuy': NoKey('reasonsToBuy')} for hyperid in range(102, 106)]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {'id': 101, 'reasonsToBuy': make_consumer_factors([0.91, 0.92, 0.93])},
                ]
            },
        )
        # [test3] всех характеристик равное кол-во вычеркивается всегда самая худшая по value
        #   2 модели на каждую хар: ABACBC -> A_maxB_maxC_max
        #       max rate модель нужно поставить в самую дальнюю позицию относительно всех представителей хар-ки
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=1&numdoc=6&hyperid=100&rearr-factors=market_reasons_to_buy_filter=1;split=split1-test3&yandexuid=split1-test3'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 101, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 102, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 103, 'reasonsToBuy': make_consumer_factors([0.92, None, None])},
                    {'id': 104, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 105, 'reasonsToBuy': make_consumer_factors([None, 0.99, None])},
                    {'id': 106, 'reasonsToBuy': make_consumer_factors([None, None, 0.98])},
                ]
            },
        )
        # [test4]
        # ABACC -> A_maxB_maxC_max
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=1&numdoc=6&hyperid=100&rearr-factors=market_reasons_to_buy_filter=1;split=split1-test4&yandexuid=split1-test4'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 101, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 102, 'reasonsToBuy': make_consumer_factors([None, 0.92, None])},
                    {'id': 103, 'reasonsToBuy': make_consumer_factors([0.92, None, None])},
                    {'id': 104, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 105, 'reasonsToBuy': make_consumer_factors([None, None, 0.98])},
                    {'id': 106, 'reasonsToBuy': NoKey('reasonsToBuy')},
                ]
            },
        )
        # [test5] corner case: 6 models with the same chr of equal rate -> only the frist 3 are left
        #   6 * A_max -> A1A2A3
        # [test6] Проверяем позиционирование на странице: на первой и второй странице есть модели
        #   с характеристиками, проверям, что алгоритм применяется только к выбранной странице
        #   запрошенная страница - вторая. если бы считалсь первая, то повлияло бы
        #   --B---|6 * A_max -> A1A2A3
        #   если бы смотрели на обе страницы, то с писок победивших х-к пролезла бы B и одной из A не было бы
        # [test7] неполная страница, модели с характеристиками из test4

    def test_split2(self):
        """
        MARKETOUT-23571, split 2
        """
        # [test1] -> split1-test1
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=1&numdoc=6&hyperid=100&rearr-factors=market_reasons_to_buy_filter=2;split=test1&yandexuid=split2-test1'
        )
        self.assertFragmentIn(
            response,
            {'results': [{'id': hyperid, 'reasonsToBuy': NoKey('reasonsToBuy')} for hyperid in range(101, 106)]},
        )
        # [test2] -> split1-test2
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=1&numdoc=6&hyperid=100&rearr-factors=market_reasons_to_buy_filter=2;split=test2&yandexuid=split2-test2'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 101, 'reasonsToBuy': make_consumer_factors([None, None, 0.93])},
                ]
                + [{'id': hyperid, 'reasonsToBuy': NoKey('reasonsToBuy')} for hyperid in range(102, 107)]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {'id': 101, 'reasonsToBuy': make_consumer_factors([0.91, 0.92, 0.93])},
                ]
            },
        )
        # [test3]  4 модели с неск хар-ками, и еще 2 без харк
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=1&numdoc=6&hyperid=100&rearr-factors=market_reasons_to_buy_filter=2;split=split2-test3&yandexuid=split2-test3'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 101, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 102, 'reasonsToBuy': make_consumer_factors([None, 0.92, None])},
                    {'id': 103, 'reasonsToBuy': make_consumer_factors([0.95, None, None])},
                    {'id': 104, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 105, 'reasonsToBuy': make_consumer_factors([None, None, 0.99])},
                    {'id': 106, 'reasonsToBuy': NoKey('reasonsToBuy')},
                ]
            },
        )
        # [test4]  6 моделей с характеристиками
        # [test5]  страницы. запрошенная страница - вторая. если бы считалсь первая, то повлияло бы
        #   допустим у моделей только по одной хар-ке:
        #   --A_max---|6 * A_min -> 3 * A_min
        #   если бы смотрели на обе страницы, то с писок победивших х-к пролезла бы B и одной из A не было бы
        # [test6] неполная страница, модели с характеристиками из test3
        # [test7] запрошенная страница больше видимого блока, проверяем, что таки работает окно в 6
        # (101, [None, 0.81, None]),
        # (104, [0.91, 0.92, 0.93]),
        # (106, [0.95, None, 0.89]),
        # (109, [0.82, None, None]),
        # (110, [0.99, 0.99, 0.99]),
        # (112, [0.90, 0.87, 0.88]),
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=1&numdoc=12&hyperid=100&rearr-factors=market_reasons_to_buy_filter=2;split=split2-test7&yandexuid=split2-test7'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 101, 'reasonsToBuy': make_consumer_factors([None, 0.81, None])},
                    {'id': 102, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 103, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 104, 'reasonsToBuy': make_consumer_factors([None, None, 0.93])},
                    {'id': 105, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 106, 'reasonsToBuy': make_consumer_factors([0.95, None, None])},
                    # ---visual-block-break----
                    {'id': 107, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 108, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 109, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 110, 'reasonsToBuy': make_consumer_factors([None, None, 0.99])},
                    {'id': 111, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 112, 'reasonsToBuy': make_consumer_factors([0.90, None, None])},
                ]
            },
            allow_different_len=False,
        )
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=1&numdoc=8&hyperid=100&rearr-factors=market_reasons_to_buy_filter=2;split=split2-test7&yandexuid=split2-test7'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 101, 'reasonsToBuy': make_consumer_factors([None, 0.81, None])},
                    {'id': 102, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 103, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 104, 'reasonsToBuy': make_consumer_factors([None, None, 0.93])},
                    {'id': 105, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 106, 'reasonsToBuy': make_consumer_factors([0.95, None, None])},
                    # ---visual-block-break----
                    {'id': 107, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 108, 'reasonsToBuy': NoKey('reasonsToBuy')},
                ]
            },
            allow_different_len=False,
        )
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=2&numdoc=7&hyperid=100&rearr-factors=market_reasons_to_buy_filter=2;split=split2-test7&yandexuid=split2-test7'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 108, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 109, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 110, 'reasonsToBuy': make_consumer_factors([None, None, 0.99])},
                    {'id': 111, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 112, 'reasonsToBuy': make_consumer_factors([0.90, None, None])},
                ]
            },
            allow_different_len=False,
        )
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=2&numdoc=4&hyperid=100&rearr-factors=market_reasons_to_buy_filter=2;split=split2-test7&yandexuid=split2-test7'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 105, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 106, 'reasonsToBuy': make_consumer_factors([0.95, None, None])},
                    # ---visual-block-break----
                    {'id': 107, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 108, 'reasonsToBuy': NoKey('reasonsToBuy')},
                ]
            },
            allow_different_len=False,
        )

    def test_split3(self):
        """
        MARKETOUT-23571, split 3
        """
        # [test1] -> split1-test1
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=1&numdoc=6&hyperid=100&rearr-factors=market_reasons_to_buy_filter=3;split=test1&yandexuid=split3-test1'
        )
        self.assertFragmentIn(
            response,
            {'results': [{'id': hyperid, 'reasonsToBuy': NoKey('reasonsToBuy')} for hyperid in range(101, 106)]},
        )
        # [test2] крайний случай: все 6 из 6 моделей окажутся в итоге с хар
        #   допустим у моделей только по одной хар-ке:
        #   2 модели на каждую хар: ABACBC -> ABACBC
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=1&numdoc=6&hyperid=100&rearr-factors=market_reasons_to_buy_filter=3;split=split3-test2&yandexuid=split3-test2'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 101, 'reasonsToBuy': make_consumer_factors([0.99, None, None])},
                    {'id': 102, 'reasonsToBuy': make_consumer_factors([None, 0.98, None])},
                    {'id': 103, 'reasonsToBuy': make_consumer_factors([0.81, None, None])},
                    {'id': 104, 'reasonsToBuy': make_consumer_factors([None, None, 0.81])},
                    {'id': 105, 'reasonsToBuy': make_consumer_factors([None, 0.99, None])},
                    {'id': 106, 'reasonsToBuy': make_consumer_factors([None, None, 0.99])},
                ]
            },
        )

        # [test3] пересечения
        #   модели имеют по несколько характеристик, некоторые остаются с характ-ками, у некоторых не ост хар
        #   m1(A_max, B_mid, C_min), m2(A_min, B_max, C_mid2), m3(A_mid, B_mid2, С_mid), m4(B_min), m5(C_max) ->
        #   m1(A_max, B_mid),        m2(A_min, B_max, C_mid2), m3(A_mid, B_mid2, C_mid), m4(),      m5(C_max)
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=1&numdoc=6&hyperid=100&rearr-factors=market_reasons_to_buy_filter=3;split=split3-test3&yandexuid=split3-test3'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': 101, 'reasonsToBuy': make_consumer_factors([0.99, 0.90, None])},
                    {'id': 102, 'reasonsToBuy': make_consumer_factors([0.81, 0.99, 0.95])},
                    {'id': 103, 'reasonsToBuy': make_consumer_factors([0.90, 0.95, 0.90])},
                    {'id': 104, 'reasonsToBuy': NoKey('reasonsToBuy')},
                    {'id': 105, 'reasonsToBuy': make_consumer_factors([None, None, 0.99])},
                    {'id': 106, 'reasonsToBuy': NoKey('reasonsToBuy')},
                ]
            },
        )
        # [test4] страницы. запрошенная страница - вторая. если бы считалсь первая, то повлияло бы
        #   допустим у моделей только по одной хар-ке:
        #   6 * A_max | 3 * A_min, 3 * B_max -> A1A2A3B1B2B3
        #   если бы смотрели на обе страницы, то пролезла бы B
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&page=2&numdoc=6&hyperid=100&rearr-factors=market_reasons_to_buy_filter=3;split=split3-test4&yandexuid=split3-test4'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'id': hyperid, 'reasonsToBuy': make_consumer_factors([0.85, None, None])}
                    for hyperid in range(107, 110)
                ]
                + [
                    {'id': hyperid, 'reasonsToBuy': make_consumer_factors([None, 0.99, None])}
                    for hyperid in range(110, 113)
                ]
            },
        )


if __name__ == "__main__":
    main()
