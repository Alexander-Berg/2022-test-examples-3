#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import ExperimentalModelGroup, HybridAuctionParam, MnPlace, Model, Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.3)
        # do not set default values
        # @see market/report/lite/core/report.py set_default_search_experiment_flags
        cls.settings.ignore_search_experiment_flags += [
            'market_ha_ctr_pow',
        ]
        cls.index.hybrid_auction_settings += [
            HybridAuctionParam(
                category=90401,
                cpc_ctr_for_cpc=0.1,
                cpc_ctr_for_cpc_msk=0.1,
            ),
        ]
        cls.index.models += [
            Model(hyperid=1),
            Model(hyperid=2),
        ]
        cls.index.experimental_model_groups += [
            ExperimentalModelGroup(model_id=1, group_id=42),
            ExperimentalModelGroup(model_id=2, group_id=3),
        ]
        cls.index.offers += [
            Offer(hyperid=2),  # experimental with power=0.5
            Offer(hyperid=3),  # non-experimental
        ]

    def test_trace_me(self):
        response = self.report.request_json('place=productoffers&hyperid=1&debug=1')
        self.assertFragmentIn(response, 'Model 1 is part of experimental group 42')

    # @skip('Enable with enabling model-based CTR experiment: https://st.yandex-team.ru/MARKETOUT-18635')
    # make ctr power experiment great again! @see https://st.yandex-team.ru/MARKETOUT-30469
    def test_ctr_power_productoffers(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=3&debug=1&rearr-factors=market_enable_ctr_power_by_model_group_experiment=0,1,2,3'
        )
        self.assertFragmentIn(
            response,
            {
                "HYBRID_AUCTION_CTR_POWER": "0.200000003",
                "HYBRID_AUCTION_CTR_CPC": "0.1245730966",
            },
        )

        response = self.report.request_json(
            'place=productoffers&hyperid=2&debug=1&rearr-factors=market_enable_ctr_power_by_model_group_experiment=0,3'
        )
        self.assertFragmentIn(
            response,
            {
                "HYBRID_AUCTION_CTR_POWER": "0.5",
                "HYBRID_AUCTION_CTR_CPC": "0.1732050925",  # 0.1 ^ 0.5 * 0.1 ^ (1 - 0.5) = 0.17320508075688772935274463415059 ~ 0.1732050925 (due to float errors)
            },
        )

    @classmethod
    def prepare_ctr_power_experiment(cls):
        cls.index.models += [
            Model(hyperid=101),
            Model(hyperid=102),
            Model(hyperid=103),
            Model(hyperid=104),
            Model(hyperid=105),  # model with no group
        ]
        cls.index.experimental_model_groups += [
            ExperimentalModelGroup(model_id=101, group_id=0),
            ExperimentalModelGroup(model_id=102, group_id=1),
            ExperimentalModelGroup(model_id=103, group_id=2),
            ExperimentalModelGroup(model_id=104, group_id=3),
        ]
        cls.index.offers += [
            Offer(hyperid=101),  # experimental with power=0.2 - control
            Offer(hyperid=102),  # experimental with power=0.3
            Offer(hyperid=103),  # experimental with power=0.4
            Offer(hyperid=104),  # experimental with power=0.5
            Offer(hyperid=105),  # non-experimental
        ]

    def test_ctr_power_differece_first_disabled(self):
        """
        Для группы 1 - проверяем, что без реарр флага вернется дефолтное значение степени
        """
        response = self.report.request_json('place=productoffers&hyperid=102&debug=1')
        self.assertFragmentIn(
            response,
            {
                "HYBRID_AUCTION_CTR_POWER": "0.200000003",
            },
        )

    # Проверяем, что для каждой группы возвращается ожидаемый CTR
    def test_ctr_power_differece_control(self):
        """
        Для группы 0
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=101&debug=1&rearr-factors=market_enable_ctr_power_by_model_group_experiment=0'
        )
        self.assertFragmentIn(
            response,
            {
                "HYBRID_AUCTION_CTR_POWER": "0.200000003",
            },
        )

    def test_ctr_power_differece_first(self):
        """
        Для группы 1 - когда она выключена
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=102&debug=1&rearr-factors=market_enable_ctr_power_by_model_group_experiment=0,1'
        )
        self.assertFragmentIn(
            response,
            {
                "HYBRID_AUCTION_CTR_POWER": "0.3000000119",
            },
        )

    def test_ctr_power_market_ha_ctr_pow_has_highest_priority(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=102&debug=1&rearr-factors=market_enable_ctr_power_by_model_group_experiment=0,1;market_ha_ctr_pow=3;'
        )
        self.assertFragmentIn(
            response,
            {
                "HYBRID_AUCTION_CTR_POWER": "3",
            },
        )

    def test_ctr_power_differece_first_disabled_2(self):
        """
        Для группы 1
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=102&debug=1&rearr-factors=market_enable_ctr_power_by_model_group_experiment=0,2,3'
        )
        self.assertFragmentIn(
            response,
            {
                "HYBRID_AUCTION_CTR_POWER": "0.200000003",
            },
        )

    def test_ctr_power_differece_second(self):
        """
        Для группы 2
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=103&debug=1&rearr-factors=market_enable_ctr_power_by_model_group_experiment=0,2'
        )
        self.assertFragmentIn(
            response,
            {
                "HYBRID_AUCTION_CTR_POWER": "0.400000006",
            },
        )

    def test_ctr_power_differece_second_disabled(self):
        """
        Для группы 2 - когда она выключена
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=103&debug=1&rearr-factors=market_enable_ctr_power_by_model_group_experiment=0,1,3'
        )
        self.assertFragmentIn(
            response,
            {
                "HYBRID_AUCTION_CTR_POWER": "0.200000003",
            },
        )

    def test_ctr_power_differece_third(self):
        """
        Для группы 3
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=104&debug=1&rearr-factors=market_enable_ctr_power_by_model_group_experiment=0,3'
        )
        self.assertFragmentIn(
            response,
            {
                "HYBRID_AUCTION_CTR_POWER": "0.5",
            },
        )

    def test_ctr_power_differece_third_disabled(self):
        """
        Для группы 3 - когда она выключена
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=104&debug=1&rearr-factors=market_enable_ctr_power_by_model_group_experiment=0,1,2'
        )
        self.assertFragmentIn(
            response,
            {
                "HYBRID_AUCTION_CTR_POWER": "0.200000003",
            },
        )

    def test_ctr_power_differece_no_group(self):
        """
        Без группы
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=105&debug=1&rearr-factors=market_enable_ctr_power_by_model_group_experiment=0,1,2,4'
        )
        self.assertFragmentIn(
            response,
            {
                "HYBRID_AUCTION_CTR_POWER": "0.200000003",
            },
        )

    def test_ctr_power_differece_disbled(self):
        """
        Без группы - эксперимент выключен
        """
        response = self.report.request_json('place=productoffers&hyperid=105&debug=1')
        self.assertFragmentIn(
            response,
            {
                "HYBRID_AUCTION_CTR_POWER": "0.200000003",
            },
        )


if __name__ == '__main__':
    main()
