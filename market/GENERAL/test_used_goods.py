#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import Model, UngroupedModel, MarketSku, Offer, Shop, HyperCategory, GLType, GLParam, BlueOffer
from core.matcher import Absent, ElementCount, EqualToOneOf
from core.testcase import TestCase, main


mskus_for_jump_table_testing = [1, 2]


class T(TestCase):
    @classmethod
    def prepare(cls):
        for fesh in range(1, 5):
            cls.index.shops += [Shop(fesh=fesh, priority_region=213)]

        cls.index.hypertree += [HyperCategory(hid=1)]

        cls.index.models += [
            Model(
                hyperid=100,
                hid=1,
                title='ball',
                ungrouped_blue=[
                    UngroupedModel(group_id=1, key='100_1'),
                    UngroupedModel(group_id=2, key='100_2'),
                    UngroupedModel(group_id=3, key='100_3'),
                    UngroupedModel(group_id=3, key='100_4'),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title='new ball',
                hyperid=100,
                sku=1001,
                ungrouped_model_blue=1,
            ),
            MarketSku(
                title='good ball',
                hyperid=100,
                sku=1002,
                ungrouped_model_blue=2,
                visual_condition='good',
            ),
            MarketSku(
                title='excellent ball',
                hyperid=100,
                sku=1003,
                ungrouped_model_blue=3,
                visual_condition='excellent',
            ),
            MarketSku(
                title='as-new ball',
                hyperid=100,
                sku=1004,
                ungrouped_model_blue=4,
                visual_condition='as-new',
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=100,
                sku=1004,
                ungrouped_model_blue=4,
                fesh=1,
                title='used ball as new',
                visual_condition='as-new',
            ),
            Offer(
                hyperid=100,
                sku=1003,
                ungrouped_model_blue=3,
                fesh=2,
                title='excellent used ball',
                visual_condition='excellent',
            ),
            Offer(
                hyperid=100,
                sku=1002,
                ungrouped_model_blue=2,
                fesh=3,
                title='good used ball',
                visual_condition='good',
            ),
            Offer(
                hyperid=100,
                sku=1001,
                ungrouped_model_blue=1,
                fesh=4,
                title='new ball',
            ),
        ]

    def test_productoffers(self):
        def gen_req(market_sku, show_used_goods, rearr):
            req = 'place=productoffers&market-sku={}&rids=213&debug=1'.format(market_sku)
            if show_used_goods is not None:
                req += '&show-used-goods={}'.format(show_used_goods)
            if rearr is not None:
                req += '&rearr-factors=market_enable_used_goods={}'.format(rearr)
            return req

        msku_to_result = dict()
        msku_to_result[1001] = {'slug': 'new-ball', 'usedGoods': Absent()}
        msku_to_result[1002] = {'slug': 'good-used-ball', 'usedGoods': {'type': 'good', 'desc': 'хорошее'}}
        msku_to_result[1003] = {'slug': 'excellent-used-ball', 'usedGoods': {'type': 'excellent', 'desc': 'отличное'}}
        msku_to_result[1004] = {'slug': 'used-ball-as-new', 'usedGoods': {'type': 'as-new', 'desc': 'как новый'}}
        for show_used_goods in (None, '1', '0'):
            for rearr in (None, '1', '0'):
                for market_sku in (1001, 1002, 1003, 1004):
                    response = self.report.request_json(gen_req(market_sku, show_used_goods, rearr))
                    # с выключенными флагами оффер должен находится только у новой скю 1001
                    if (show_used_goods != '1' or rearr == '0') and market_sku != 1001:
                        self.assertFragmentIn(response, {'search': {'results': ElementCount(0)}})
                        self.assertFragmentNotIn(response, 'usedGoods')
                        self.assertFragmentIn(response, {'filters': {'USED_GOODS': 1}})
                    else:
                        self.assertFragmentIn(
                            response, {'results': [msku_to_result[market_sku]]}, allow_different_len=False
                        )
                        self.assertFragmentNotIn(response, {'USED_GOODS': 1})

    @staticmethod
    def _gen_prime_req(show_used_goods, rearr, condition=None):
        req = 'place=prime&hid=1&allow-collapsing=1&allow-ungrouping=1&rids=213&use-default-offers=1&debug=1&rearr-factors=market_hids_with_used_goods=1'
        if show_used_goods is not None:
            req += '&show-used-goods={}'.format(show_used_goods)
        if rearr is not None:
            req += '&rearr-factors=market_enable_used_goods={}'.format(rearr)
        if condition is not None:
            req += '&condition={}'.format(condition)
        return req

    def test_prime(self):
        sku_1001 = {
            'entity': 'product',
            'id': 100,
            'offers': {
                'items': [
                    {
                        'sku': '1001',
                    },
                ],
            },
        }
        for show_used_goods in (None, '1', '0'):
            for rearr in (None, '1', '0'):
                response = self.report.request_json(self._gen_prime_req(show_used_goods, rearr))
                # с выключенными флагами покажется только скю 1001
                if show_used_goods != '1' or rearr == '0':
                    self.assertFragmentIn(response, {'results': [sku_1001]}, allow_different_len=False)
                    self.assertFragmentNotIn(response, 'usedGoods')
                    self.assertFragmentIn(response, {'filters': {'USED_GOODS': 3}})
                    continue

                self.assertFragmentIn(
                    response,
                    [
                        sku_1001,
                        {
                            'entity': 'product',
                            'id': 100,
                            'offers': {
                                'items': [
                                    {
                                        'sku': '1002',
                                        'slug': 'good-used-ball',
                                        'usedGoods': {'type': 'good'},
                                    },
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'id': 100,
                            'offers': {
                                'items': [
                                    {
                                        'sku': '1003',
                                        'slug': 'excellent-used-ball',
                                        'usedGoods': {'type': 'excellent'},
                                    },
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'id': 100,
                            'offers': {
                                'items': [
                                    {
                                        'sku': '1004',
                                        'slug': 'used-ball-as-new',
                                        'usedGoods': {'type': 'as-new'},
                                    },
                                ],
                            },
                        },
                    ],
                    allow_different_len=False,
                )
                self.assertFragmentNotIn(response, {'USED_GOODS': 3})

    def test_filters(self):
        '''Проверка фильтра condition=new/cutprice. С ним можно показывать все товары, или только новые, или только б/у.
        Если фильтр не задан, то показываются все товары.
        Работает только с show-used-goods=1 и не выключенном реарром market_enable_used_goods.
        '''

        def gen_condition_filter(found_new, checked_new, found_cutprice, checked_cutprice):
            '''Странные результаты у счетчиков initialFound. В идеальном мире должно быть 1 vs 3 (1 новая скю и 3 б/у).
            Сейчас же в большинстве случаев ApplyAggregatesBeforeUserFilters вызывается по два раза для каждого документа,
            и поэтому initialFound задвоен. Но при включенном фильтре condition=new не находится документ тут
            https://a.yandex-team.ru/arc/trunk/arcadia/market/report/library/relevance/relevance/main_search_rel_calc.cpp?rev=r9214419#L4804
            и поэтому не доходим до вызова ApplyAggregatesBeforeUserFilters в TMainSearchRelCalc::Calculate
            '''
            return {
                'id': 'condition',
                'name': 'Состояние',
                'type': 'enum',
                'values': [
                    {
                        'id': 'new',
                        'value': 'Новый',
                        'found': found_new,
                        'initialFound': 2,
                        'checked': checked_new or Absent(),
                    },
                    {
                        'id': 'cutprice',
                        'value': 'Б/у или уценённый',
                        'found': found_cutprice,
                        'initialFound': EqualToOneOf(3, 6),
                        'checked': checked_cutprice or Absent(),
                    },
                ],
            }

        for show_used_goods in (None, '1', '0'):
            for rearr in (None, '1', '0'):
                for condition in (None, 'new,cutprice', 'cutprice,cutprice,abc,new', 'cutprice', 'new'):
                    response = self.report.request_json(self._gen_prime_req(show_used_goods, rearr, condition))
                    # С выключенными флагами б/у игнорим, а фильтр скрываем
                    if show_used_goods != '1' or rearr == '0':
                        self.assertFragmentNotIn(response, {'id': 'condition'})
                        self.assertFragmentIn(
                            response,
                            {
                                'filters': {
                                    'USED_GOODS': 3,
                                    'NOT_USED_GOODS': Absent(),
                                }
                            },
                        )
                        continue

                    # без выбранного фильтра или при выборе обоих значений
                    if condition != 'cutprice' and condition != 'new':
                        checked = condition is not None
                        self.assertFragmentIn(
                            response,
                            gen_condition_filter(
                                found_new=1, checked_new=checked, found_cutprice=3, checked_cutprice=checked
                            ),
                        )
                        self.assertFragmentIn(
                            response,
                            {
                                'filters': {
                                    'USED_GOODS': Absent(),
                                    'NOT_USED_GOODS': Absent(),
                                }
                            },
                        )
                    elif condition == 'cutpice':
                        self.assertFragmentIn(
                            response,
                            gen_condition_filter(
                                found_new=0, checked_new=False, found_cutprice=3, checked_cutprice=True
                            ),
                        )
                        self.assertFragmentIn(
                            response,
                            {
                                'filters': {
                                    'USED_GOODS': Absent(),
                                    'NOT_USED_GOODS': 1,
                                }
                            },
                        )
                    elif condition == 'new':
                        self.assertFragmentIn(
                            response,
                            gen_condition_filter(
                                found_new=1, checked_new=True, found_cutprice=0, checked_cutprice=False
                            ),
                        )
                        self.assertFragmentIn(
                            response,
                            {
                                'filters': {
                                    'USED_GOODS': 3,
                                    'NOT_USED_GOODS': Absent(),
                                }
                            },
                        )

    def test_modelinfo(self):
        response = self.report.request_json('place=modelinfo&market-sku=1001&rids=213')
        self.assertFragmentIn(
            response,
            {
                'entity': 'sku',
                'id': '1001',
                'usedGoods': Absent(),
            },
        )

        response = self.report.request_json('place=modelinfo&market-sku=1002&rids=213')
        self.assertFragmentIn(
            response,
            {
                'entity': 'sku',
                'id': '1002',
                'usedGoods': {'type': 'good'},
            },
        )

        response = self.report.request_json('place=modelinfo&market-sku=1003&rids=213')
        self.assertFragmentIn(
            response,
            {
                'entity': 'sku',
                'id': '1003',
                'usedGoods': {'type': 'excellent'},
            },
        )

        response = self.report.request_json('place=modelinfo&market-sku=1004&rids=213')
        self.assertFragmentIn(
            response,
            {
                'entity': 'sku',
                'id': '1004',
                'usedGoods': {'type': 'as-new'},
            },
        )

    def test_sku_search(self):
        def gen_req(show_used_goods, rearr):
            req = 'place=sku_search&text2=ball&debug=1'
            if show_used_goods is not None:
                req += '&show-used-goods={}'.format(show_used_goods)
            if rearr is not None:
                req += '&rearr-factors=market_enable_used_goods={}'.format(rearr)
            return req

        for show_used_goods in (None, '1', '0'):
            for rearr in (None, '1', '0'):
                request = gen_req(show_used_goods, rearr)
                response = self.report.request_json(request)
                self.assertFragmentIn(
                    response,
                    {
                        "total": 1,
                        "results": [
                            {
                                "entity": "sku",
                                "slug": "new-ball",
                            },
                        ],
                    },
                )
                self.assertFragmentIn(
                    response,
                    {
                        "brief": {
                            "filters": {
                                "USED_GOODS": 3,
                            }
                        }
                    },
                )

    @classmethod
    def prepare_mskus_for_one_jump_table_record_of_visual_condition(cls):
        cls.index.gltypes += [
            GLType(
                hid=2,
                param_id=29641890,
                name="Condition",
                xslname="condition",
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[1, 2, 3],
                model_filter_index=1,
                position=10,
            ),
        ]

        cls.index.models += [
            Model(
                hid=2,
                title="Model",
                hyperid=1,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=2,
                hyperid=2,
                sku=mskus_for_jump_table_testing[0],
                title="MSKU 1",
                blue_offers=[
                    BlueOffer(ts=1, price=200),
                ],
                glparams=[
                    GLParam(param_id=29641890, value=1),
                ],
            ),
            MarketSku(
                hid=2,
                hyperid=2,
                sku=mskus_for_jump_table_testing[1],
                title="MSKU 2",
                blue_offers=[
                    BlueOffer(ts=2, price=200),
                ],
                glparams=[
                    GLParam(param_id=29641890, value=1),
                ],
            ),
        ]

    def test_one_jump_table_record_of_visual_condition(self):
        mskus = mskus_for_jump_table_testing
        request_no_msku = 'place=productoffers&hyperid=2&hid=2&debug=da&onstock=0'

        for msku in mskus:
            request = request_no_msku + '&market-sku=' + str(msku)
            response = self.report.request_json(request)

            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "29641890",
                            "valuesCount": 1,
                            "values": [
                                {
                                    "checked": True,
                                    "marketSku": str(msku),
                                }
                            ],
                        }
                    ]
                },
            )


if __name__ == '__main__':
    main()
