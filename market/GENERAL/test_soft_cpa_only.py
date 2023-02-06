#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import MarketSku, Model, Offer, Region, RegionalModel, Shop
from core.testcase import TestCase, main
from core.matcher import Absent


class _C:

    rid_spb = 2
    rid_tier3 = 3

    model_cpa = 1  # cpa
    model_cpc = 2  # cpc
    model_any = 3  # cpa & cpc

    model_has_good_cpa = 4
    model_not_has_good_cpa = 5

    hid_a = 11

    fesh_tier3_cpa = 1
    fesh_tier3_cpc = 2
    fesh_spb_1_cpa = 101
    fesh_spb_2_cpc = 102
    fesh_spb_3_cpc = 103
    fesh_spb_4_cpa = 104
    fesh_spb_5_cpa = 105
    fesh_msk_1_cpa = 111
    fesh_msk_2_cpc = 112


class T(TestCase):
    """
    Набор тестов для режима "Мягкий CPA-only".
    См. https://st.yandex-team.ru/MARKETOUT-40733
    """

    @classmethod
    def prepare(cls):
        cls.disable_check_empty_output()

        cls.index.regiontree = [
            Region(rid=_C.rid_tier3, name='Tier3'),
            Region(rid=_C.rid_spb, name='СПб'),
        ]

        cls.index.models += [
            Model(hyperid=_C.model_cpa, hid=_C.hid_a),
            Model(hyperid=_C.model_cpc, hid=_C.hid_a),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=_C.model_any, rids=[_C.rid_spb], has_good_cpa=True, has_cpa=True),
        ]

        cls.index.shops += [
            Shop(
                fesh=_C.fesh_tier3_cpa,
                priority_region=_C.rid_tier3,
                regions=[_C.rid_tier3],
                cpa=Shop.CPA_REAL,
                name='CPA Магазин',
            ),
            Shop(
                fesh=_C.fesh_tier3_cpc,
                priority_region=_C.rid_tier3,
                regions=[_C.rid_tier3],
                cpa=Shop.CPA_NO,
                name='CPC Магазин',
            ),
            Shop(
                fesh=_C.fesh_spb_1_cpa,
                priority_region=_C.rid_spb,
                regions=[_C.rid_spb],
                cpa=Shop.CPA_REAL,
                name='CPA Cпб 1 Магазин',
            ),
            Shop(
                fesh=_C.fesh_spb_2_cpc,
                priority_region=_C.rid_spb,
                regions=[_C.rid_spb],
                cpa=Shop.CPA_NO,
                name='CPC Cпб 2 Магазин',
            ),
            Shop(
                fesh=_C.fesh_spb_3_cpc,
                priority_region=_C.rid_spb,
                regions=[_C.rid_spb],
                cpa=Shop.CPA_NO,
                name='CPC Cпб 3 Магазин',
            ),
            Shop(
                fesh=_C.fesh_spb_4_cpa,
                priority_region=_C.rid_spb,
                regions=[_C.rid_spb],
                cpa=Shop.CPA_REAL,
                name='CPA Cпб 4 Магазин',
            ),
            Shop(
                fesh=_C.fesh_spb_5_cpa,
                priority_region=_C.rid_spb,
                regions=[_C.rid_spb],
                cpa=Shop.CPA_REAL,
                name='CPA Cпб 5 Магазин',
            ),
        ]

        cls.index.mskus += [
            MarketSku(hyperid=_C.model_cpa, hid=_C.hid_a, sku=1),
            MarketSku(hyperid=_C.model_cpc, hid=_C.hid_a, sku=2),
            MarketSku(hyperid=_C.model_any, hid=_C.hid_a, sku=3),
        ]

        cls.index.offers += [
            Offer(
                fesh=_C.fesh_tier3_cpa, title="CPA оффер [1/1]", cpa=Offer.CPA_REAL, hyperid=_C.model_cpa, hid=_C.hid_a
            ),
            Offer(
                fesh=_C.fesh_tier3_cpc, title="CPC оффер [1/1]", cpa=Offer.CPA_NO, hyperid=_C.model_cpc, hid=_C.hid_a
            ),
            Offer(
                fesh=_C.fesh_tier3_cpa, title="CPA оффер [1/2]", cpa=Offer.CPA_REAL, hyperid=_C.model_any, hid=_C.hid_a
            ),
            Offer(
                fesh=_C.fesh_tier3_cpc, title="CPC оффер [2/2]", cpa=Offer.CPA_NO, hyperid=_C.model_any, hid=_C.hid_a
            ),
            Offer(
                fesh=_C.fesh_spb_1_cpa,
                title="CPA Спб оффер [1/5]",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_any,
                hid=_C.hid_a,
            ),
            Offer(
                fesh=_C.fesh_spb_2_cpc,
                title="CPC Спб оффер [2/5]",
                cpa=Offer.CPA_NO,
                hyperid=_C.model_any,
                hid=_C.hid_a,
            ),
            Offer(
                fesh=_C.fesh_spb_3_cpc,
                title="CPC Спб оффер [3/5]",
                cpa=Offer.CPA_NO,
                hyperid=_C.model_any,
                hid=_C.hid_a,
            ),
            Offer(
                fesh=_C.fesh_spb_4_cpa,
                title="CPA Спб оффер [4/5]",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_any,
                hid=_C.hid_a,
            ),
            Offer(
                fesh=_C.fesh_spb_5_cpa,
                title="CPA Спб оффер [5/5]",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_any,
                hid=_C.hid_a,
            ),
            Offer(
                fesh=_C.fesh_spb_1_cpa,
                title="CPA-only Спб оффер [1/3]",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_cpa,
                hid=_C.hid_a,
            ),
            Offer(
                fesh=_C.fesh_spb_4_cpa,
                title="CPA-only Спб оффер [2/3]",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_cpa,
                hid=_C.hid_a,
            ),
            Offer(
                fesh=_C.fesh_spb_5_cpa,
                title="CPA-only Спб оффер [3/3]",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_cpa,
                hid=_C.hid_a,
            ),
            Offer(
                fesh=_C.fesh_spb_2_cpc,
                title="CPC-only Спб оффер [1/2]",
                cpa=Offer.CPA_NO,
                hyperid=_C.model_cpc,
                hid=_C.hid_a,
            ),
            Offer(
                fesh=_C.fesh_spb_3_cpc,
                title="CPC-only Спб оффер [2/2]",
                cpa=Offer.CPA_NO,
                hyperid=_C.model_cpc,
                hid=_C.hid_a,
            ),
        ]

    def test_cpa(self):
        # Запрос для cpa - в ответе только cpa, фильтр не отображается
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_tier3, _C.model_cpa, _C.hid_a
            )
        )
        self.assertFragmentIn(
            response,
            {'results': [{'titles': {'raw': 'CPA оффер [1/1]'}}]},
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            {'filters': [{'id': 'cpa'}]},
        )

    def test_cpc(self):
        # Запрос для cpc - в ответе только cpc, фильтр не отображается
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_tier3, _C.model_cpc, _C.hid_a
            )
        )
        self.assertFragmentIn(
            response,
            {'results': [{'titles': {'raw': 'CPC оффер [1/1]'}}]},
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            {'filters': [{'id': 'cpa'}]},
        )

    def test_soft_mode_rid_tier3(self):
        # Запрос в регионе tier3 - в ответе отображаем фильтр cpa без галочки
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_tier3, _C.model_any, _C.hid_a
            )
        )
        self.assertFragmentIn(
            response,
            {'results': [{'titles': {'raw': 'CPA оффер [1/2]'}}, {'titles': {'raw': 'CPC оффер [2/2]'}}]},
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {'filters': [{'id': 'cpa', 'name': 'Покупка на Маркете', 'values': [{'value': '1', 'checked': Absent()}]}]},
        )

    def verify_soft_mode_content(self, response, result_cpa_only=False, soft_mode=False):
        if result_cpa_only:
            totalBefore = 5 if soft_mode else 3
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {'titles': {'raw': 'CPA Спб оффер [1/5]'}},
                        {'titles': {'raw': 'CPA Спб оффер [4/5]'}},
                        {'titles': {'raw': 'CPA Спб оффер [5/5]'}},
                    ],
                    'totalOffersBeforeFilters': totalBefore,
                    'totalShopsBeforeFilters': totalBefore,
                },
                allow_different_len=False,
            )
        else:
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {'titles': {'raw': 'CPA Спб оффер [1/5]'}},
                        {'titles': {'raw': 'CPC Спб оффер [2/5]'}},
                        {'titles': {'raw': 'CPC Спб оффер [3/5]'}},
                        {'titles': {'raw': 'CPA Спб оффер [4/5]'}},
                        {'titles': {'raw': 'CPA Спб оффер [5/5]'}},
                    ],
                    'totalOffersBeforeFilters': 5,
                    'totalShopsBeforeFilters': 5,
                },
                allow_different_len=False,
            )

    def test_soft_mode_rid_tier12_cpa_disabled(self):
        # Запрос с отключенным cpa - в ответе отображаем фильтр cpa без галочки
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&rearr-factors=market_soft_cpa_only_enabled=1;market_cpa_only_enabled=0'.format(
                _C.rid_spb, _C.model_any, _C.hid_a
            )
        )
        self.verify_soft_mode_content(response)
        self.assertFragmentIn(
            response,
            {'filters': [{'id': 'cpa', 'name': 'Покупка на Маркете', 'values': [{'value': '1', 'checked': Absent()}]}]},
        )

    def test_soft_mode_rid_tier12_soft_cpa_only_disabled(self):
        # Запрос с отключенным soft cpa-only и без cpa в запросе - в ответе отображаем все офферы и фильтр cpa без галочки
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&rearr-factors=market_soft_cpa_only_enabled=0'.format(
                _C.rid_spb, _C.model_any, _C.hid_a
            )
        )
        self.verify_soft_mode_content(response)
        self.assertFragmentIn(
            response,
            {'filters': [{'id': 'cpa', 'name': 'Покупка на Маркете', 'values': [{'value': '1', 'checked': Absent()}]}]},
        )

    def test_soft_mode_rid_tier12_soft_cpa_only_enabled_by_default(self):
        # Запрос cpa=any - в ответе отображаем все офферы и фильтр cpa без галочки
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&cpa=any'.format(_C.rid_spb, _C.model_any, _C.hid_a)
        )
        self.verify_soft_mode_content(response)
        self.assertFragmentIn(
            response,
            {'filters': [{'id': 'cpa', 'name': 'Покупка на Маркете', 'values': [{'value': '1', 'checked': Absent()}]}]},
        )

        # Запрос cpa=real - в ответе отображаем только cpa-офферы и фильтр cpa с выставленной галочкой
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&cpa=real&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_any, _C.hid_a
            )
        )
        self.verify_soft_mode_content(response, result_cpa_only=True)
        self.assertFragmentIn(
            response,
            {'filters': [{'id': 'cpa', 'name': 'Покупка на Маркете', 'values': [{'value': '1', 'checked': True}]}]},
        )

        # Запрос без cpa - в ответе отображаем только cpa-офферы и фильтр cpa с выставленной галочкой
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_any, _C.hid_a
            )
        )
        self.verify_soft_mode_content(response, result_cpa_only=True, soft_mode=True)
        self.assertFragmentIn(
            response,
            {'filters': [{'id': 'cpa', 'name': 'Покупка на Маркете', 'values': [{'value': '1', 'checked': True}]}]},
        )

    def verify_soft_mode_content_cpa_only(self, response):
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'CPA-only Спб оффер [1/3]'}},
                    {'titles': {'raw': 'CPA-only Спб оффер [2/3]'}},
                    {'titles': {'raw': 'CPA-only Спб оффер [3/3]'}},
                ]
            },
            allow_different_len=False,
        )

    def test_soft_mode_rid_tier12_cpa_only(self):
        # Запрос cpa=any - фильтра cpa нет -- в выдаче только cpa-офферы
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&cpa=any&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_cpa, _C.hid_a
            )
        )
        self.verify_soft_mode_content_cpa_only(response)
        self.assertFragmentNotIn(
            response,
            {'filters': [{'id': 'cpa'}]},
        )

        # Запрос cpa=real - фильтр cpa отображаем (явный запрос)
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&cpa=real&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_cpa, _C.hid_a
            )
        )
        self.verify_soft_mode_content_cpa_only(response)
        self.assertFragmentIn(response, {'filters': [{'id': 'cpa'}]})

        # Запрос без cpa - фильтра cpa нет -- в выдаче только cpa-офферы
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_cpa, _C.hid_a
            )
        )
        self.verify_soft_mode_content_cpa_only(response)
        self.assertFragmentNotIn(
            response,
            {'filters': [{'id': 'cpa'}]},
        )

    def verify_soft_mode_content_cpc_only(self, response):
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'CPC-only Спб оффер [1/2]'}},
                    {'titles': {'raw': 'CPC-only Спб оффер [2/2]'}},
                ]
            },
            allow_different_len=False,
        )

    def test_soft_mode_rid_tier12_cpc_only(self):
        # Запрос cpa=any - фильтра cpa нет -- в выдаче только cpc-офферы
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&cpa=any&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_cpc, _C.hid_a
            )
        )
        self.verify_soft_mode_content_cpc_only(response)
        self.assertFragmentNotIn(response, {'filters': [{'id': 'cpa'}]})

        # Запрос cpa=real - в ответе пусто -- только cpc-офферы, фильтр cpa отображаем (явный запрос)
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&cpa=real&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_cpc, _C.hid_a
            )
        )
        self.assertFragmentIn(
            response,
            {"search": {"total": 0, "cpaCount": 0}},
        )
        self.assertFragmentIn(response, {'filters': [{'id': 'cpa'}]})

        # Запрос без cpa - фильтра cpa нет -- в выдаче только cpc-офферы
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_cpc, _C.hid_a
            )
        )
        self.verify_soft_mode_content_cpc_only(response)
        self.assertFragmentNotIn(response, {'filters': [{'id': 'cpa'}]})

    @classmethod
    def prepare_good_cpa(cls):
        cls.index.regional_models += [
            RegionalModel(hyperid=_C.model_has_good_cpa, rids=[_C.rid_spb], has_good_cpa=True, has_cpa=True),
            RegionalModel(hyperid=_C.model_not_has_good_cpa, rids=[_C.rid_spb], has_good_cpa=False, has_cpa=True),
        ]

        cls.index.models += [
            Model(hyperid=_C.model_has_good_cpa, hid=_C.hid_a),
            Model(hyperid=_C.model_not_has_good_cpa, hid=_C.hid_a),
        ]

        cls.index.mskus += [
            MarketSku(hyperid=_C.model_has_good_cpa, hid=_C.hid_a, sku=11),
            MarketSku(hyperid=_C.model_not_has_good_cpa, hid=_C.hid_a, sku=12),
        ]

        cls.index.offers += [
            Offer(
                fesh=_C.fesh_spb_1_cpa,
                title="CPA-hgc оффер [1/2]",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_has_good_cpa,
                hid=_C.hid_a,
            ),
            Offer(
                fesh=_C.fesh_spb_2_cpc,
                title="CPC-hgc оффер [2/2]",
                cpa=Offer.CPA_NO,
                hyperid=_C.model_has_good_cpa,
                hid=_C.hid_a,
            ),
            Offer(
                fesh=_C.fesh_spb_1_cpa,
                title="CPA-not-hgc оффер [1/2]",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_not_has_good_cpa,
                hid=_C.hid_a,
            ),
            Offer(
                fesh=_C.fesh_spb_2_cpc,
                title="CPC-not-hgc оффер [2/2]",
                cpa=Offer.CPA_NO,
                hyperid=_C.model_not_has_good_cpa,
                hid=_C.hid_a,
            ),
        ]

    def test_soft_mode_has_good_cpa(self):
        # Запрос cpa=any - фильтр cpa есть (неактивен) -- в выдаче все офферы
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&cpa=any&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_has_good_cpa, _C.hid_a
            )
        )
        self.assertFragmentIn(
            response,
            {'results': [{'titles': {'raw': 'CPA-hgc оффер [1/2]'}}, {'titles': {'raw': 'CPC-hgc оффер [2/2]'}}]},
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {'filters': [{'id': 'cpa', 'values': [{'value': '1', 'checked': Absent()}]}]})

        # Запрос cpa=real - фильтр cpa есть (активен) -- в выдаче только cpa-офферы
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&cpa=real&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_has_good_cpa, _C.hid_a
            )
        )
        self.assertFragmentIn(
            response,
            {'results': [{'titles': {'raw': 'CPA-hgc оффер [1/2]'}}]},
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {'filters': [{'id': 'cpa', 'values': [{'value': '1', 'checked': True}]}]})

        # Запрос без cpa - фильтр cpa есть (активен) -- в выдаче только cpa-офферы
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_has_good_cpa, _C.hid_a
            )
        )
        self.assertFragmentIn(
            response,
            {'results': [{'titles': {'raw': 'CPA-hgc оффер [1/2]'}}]},
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {'filters': [{'id': 'cpa', 'values': [{'value': '1', 'checked': True}]}]})

    def test_soft_mode_not_has_good_cpa(self):
        # Запрос cpa=any - фильтр cpa есть (неактивен) -- в выдаче все офферы
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&cpa=any&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_not_has_good_cpa, _C.hid_a
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'CPA-not-hgc оффер [1/2]'}},
                    {'titles': {'raw': 'CPC-not-hgc оффер [2/2]'}},
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {'filters': [{'id': 'cpa', 'values': [{'value': '1', 'checked': Absent()}]}]})

        # Запрос cpa=real - фильтр cpa есть (активен) -- в выдаче только cpa-офферы
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&cpa=real&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_not_has_good_cpa, _C.hid_a
            )
        )
        self.assertFragmentIn(
            response,
            {'results': [{'titles': {'raw': 'CPA-not-hgc оффер [1/2]'}}]},
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {'filters': [{'id': 'cpa', 'values': [{'value': '1', 'checked': True}]}]})

        # Запрос без cpa - фильтр cpa есть (неактивен) -- в выдаче все офферы
        response = self.report.request_json(
            'place=productoffers&rids={}&hyperid={}&hid={}&rearr-factors=market_soft_cpa_only_enabled=1'.format(
                _C.rid_spb, _C.model_not_has_good_cpa, _C.hid_a
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'CPA-not-hgc оффер [1/2]'}},
                    {'titles': {'raw': 'CPC-not-hgc оффер [2/2]'}},
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {'filters': [{'id': 'cpa', 'values': [{'value': '1', 'checked': Absent()}]}]})

    @classmethod
    def prepare_soft_cpa_only_for_models(cls):
        cls.index.models += [
            Model(hyperid=101, hid=54, vendor_id=404, vbid=10, title="coffee 101"),
            Model(hyperid=102, hid=54, vendor_id=404, vbid=10, title="coffee 102"),
            Model(hyperid=103, hid=54, vendor_id=404, vbid=10, title="coffee 103"),
            Model(hyperid=104, hid=54, vendor_id=404, vbid=10, title="coffee 104"),
        ]

        cls.index.shops += [
            Shop(fesh=201, priority_region=213, regions=[213], cpa=Shop.CPA_REAL, name='CPA Магазин 1'),
            Shop(fesh=202, priority_region=213, regions=[213], cpa=Shop.CPA_REAL, name='CPA Магазин 2'),
            Shop(
                fesh=203, priority_region=213, regions=[213], cpa=Shop.CPA_NO, cpc=Shop.CPC_REAL, name='CPC Магазин 3'
            ),
            Shop(
                fesh=204,
                priority_region=213,
                regions=[213],
                cpa=Shop.CPA_REAL,
                cpc=Shop.CPC_REAL,
                name='CPA CPC Магазин 4',
            ),
        ]

        cls.index.offers += [
            Offer(fesh=201, cpa=Offer.CPA_REAL, hyperid=101, hid=54, vendor_id=404),
            Offer(fesh=203, cpa=Offer.CPA_NO, is_cpc=True, hyperid=103, hid=54, vendor_id=404),
            Offer(fesh=204, cpa=Offer.CPA_REAL, hyperid=104, hid=54, vendor_id=404),
            Offer(fesh=204, cpa=Offer.CPA_NO, is_cpc=True, hyperid=104, hid=54, vendor_id=404),
        ]

    # для случая, когда market_cpa_only_by_index отключен, а мета не передает has_cpa,
    # проверим, что выдаются модели, для которых есть CPA офферы
    def test_soft_cpa_only_for_models(self):
        response = self.report.request_json(
            'place=vendor_incut&rids={}&hid={}&debug=1'
            '&rearr-factors=market_cpa_only_enabled=1;market_cpa_only_by_index=0;market_soft_cpa_only_enabled=1'
            ';market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0'
            ';market_vendor_incut_min_size=1;market_vendor_incut_hide_undeliverable_models=0'.format(213, 54)
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {'titles': {'raw': 'coffee 101'}},
                    {'titles': {'raw': 'coffee 104'}},
                ],
            },
        )


if __name__ == '__main__':
    main()
