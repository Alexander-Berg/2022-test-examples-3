#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    Model,
    Offer,
    Region,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.matcher import ElementCount, Absent, Contains
from core.testcase import main
from core.types.reserveprice_fee import ReservePriceFee
from test_madv_model_card import TMadvModelCardTest


class T(TMadvModelCardTest):
    """
    Тесты врезок от предыдущей ручки competitive_model_card, получаемых от новой madv_model_card
    """

    COMPETITIVE_MODEL_CARD_ON_DESKTOP = 162
    COMPETITIVE_MODEL_CARD_ON_TOUCH = 662
    COMPETITIVE_MODEL_CARD_ON_ANDROID = 1761
    COMPETITIVE_MODEL_CARD_ON_IOS = 1861
    COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_DESKTOP = 168
    COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_TOUCH = 668
    COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_ANDROID = 1768
    COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_IOS = 1868

    _rearr_factors = (
        "&rearr-factors="
        "market_competitive_model_card_do_not_bill=0"
        ";market_competitive_model_card_closeness_threshold=10"
        ";split=empty"
        ";market_competitive_model_card_disable_remove_empty_do=1"  # отключение функционала по запрету выдачи моделей без ДО
    )

    @classmethod
    def prepare(cls):
        """
        Модели, офферы и конфигурация для выдачи place=madv_incut для первого простого теста
        """

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

        model_ids = list(range(1, 6))
        random_ts = [5, 2, 6, 3, 1, 4]
        cls.index.models += [Model(hyperid=x[0], ts=x[1], hid=101, vbid=100) for x in zip(model_ids, random_ts)]
        cls.index.offers += [
            Offer(hyperid=hyperid, fesh=13, fee=10, price=1000, cpa=Offer.CPA_REAL) for hyperid in model_ids
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COMPETITIVE_MODEL,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    # no partition with split 'noconfig'
                    # partitions with data
                    YamarecSettingPartition(params={'version': 'SIBLINGS1_AUGMENTED'}, splits=[{'split': 'empty'}]),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(model_id=1, item_count=1000, version='SIBLINGS1_AUGMENTED').respond(
            {'models': ['4:0.1', '2:0.2', '3:0.3', '5:0.44']}
        )

    def test_models_scores(self):
        """
        Проверяем, что при равных ставках выигрывает модель с большей степенью похожести
        """

        request = 'place=madv_incut' + self._rearr_factors + '&hyperid=1&debug=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})

    def test_cgi_cpa_real(self):
        """
        Проверяем, что при &cpa=real все работает также, как и без
        """
        request = 'place=madv_incut' + self._rearr_factors + '&hyperid=1&debug=1&cpa=real'
        response = self.report.request_json(request + '&rearr-factors=market_competitive_model_card_fix_cpa_real=1')
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})
        # Проверяем, что флагом можно отключить фикс для &cpa=real
        response = self.report.request_json(request + '&rearr-factors=market_competitive_model_card_fix_cpa_real=0')
        self.assertFragmentIn(response, "No model has pass all filters")
        self.assertFragmentNotIn(response, {"modelId": 5})

    def test_models_scores_in_shows_log(self):
        """
        Проверяем, что
        1. При равных ставках выигрывает модель с большей степенью похожести
        2. В shows-log залогируются modelId и score для моделей-аналогов, отсортированные в порядке убывания скоров; market_analog_score_in_show_log_multiplier=100 задает точность в 2 знака
        """

        request = (
            'place=madv_incut'
            + self._rearr_factors
            + ';market_analog_score_in_show_log_multiplier=100&hyperid=1&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})
        self.show_log.expect(analogs_data="5,44;3,30;2,20;4,10", original_model_id=1)

    def test_models_scores_in_shows_log_limited(self):
        """
        Проверяем, что
        1. При равных ставках выигрывает модель с большей степенью похожести
        2. При market_max_analogs_data_in_show_log_in_competitive_model_card=3 в shows-log залогируются modelId и score для 3-х первых по скорам моделей-аналогов
        """

        request = (
            'place=madv_incut'
            + self._rearr_factors
            + ';market_max_analogs_data_in_show_log_in_competitive_model_card=3&hyperid=1&debug=1'
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})
        self.show_log.expect(analogs_data="5,440;3,300;2,200", original_model_id=1)

    def test_models_single_madv_incut(self):
        request = (
            'place=madv_incut'
            + self._rearr_factors
            + ';market_log_analogs_data_in_competitive_model_card=0&hyperid=1&debug=1'
            + '&rearr-factors=market_competitive_model_card_disable_multiple_cards=1'
            + '&show-urls=productVendorBid,cpa'
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'incutId': 'CompetitiveModelCardSingle',
                        "items": ElementCount(1),
                    }
                ]
            },
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'incutId': 'CompetitiveModelCardSingle',
                        "items": [
                            {
                                "urls": {
                                    "encrypted": Contains(
                                        "hyper_cat_id=101",
                                        "pp={}".format(self.COMPETITIVE_MODEL_CARD_ON_DESKTOP),
                                    ),
                                }
                            }
                        ],
                    }
                ]
            },
        )

    def test_models_scores_in_shows_log_absent(self):
        """
        Проверяем, что
        1. При равных ставках выигрывает модель с большей степенью похожести
        2. market_log_analogs_data_in_competitive_model_card=0 выключает логирование данных по моделям-аналогам в show-log
        """
        request = (
            'place=madv_incut'
            + self._rearr_factors
            + ';market_log_analogs_data_in_competitive_model_card=0&hyperid=1&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})
        self.show_log.expect(analogs_data=Absent(), original_model_id=Absent())

    @classmethod
    def prepare_more_than_one_competitive_model(cls):
        cls.index.models += [
            Model(hyperid=70, ts=70, hid=107, vendor_id=71, vbid=100),  # vendorFee = 6000 (60%)
            Model(hyperid=71, ts=71, hid=107, vendor_id=72, vbid=80),  # vendorFee = 4800 (48%)
            Model(hyperid=72, ts=72, hid=107, vendor_id=73, vbid=70),  # vendorFee = 4200 (42%)
            Model(hyperid=73, ts=73, hid=107, vendor_id=73, vbid=60),  # vendorFee = 3600 (36%)
            Model(hyperid=74, ts=74, hid=107, vendor_id=73, vbid=50),  # vendorFee = 3000 (30%)
            Model(hyperid=75, ts=75, hid=107, vendor_id=75, vbid=40),  # vendorFee = 2400 (24%)
            Model(hyperid=76, ts=76, hid=107, vendor_id=77, vbid=6),  # vendorFee = 360 (3.6%)
            Model(hyperid=77, ts=77, hid=107, vendor_id=77, vbid=5),  # vendorFee = 300 (3%)
            # Model(hyperid=79) - model with siblings
            # Model(hyperid=80) - model with siblings (one of sibling has zero fee)
            Model(hyperid=81, ts=81, hid=107, vendor_id=81, vbid=100),
            Model(hyperid=82, ts=82, hid=107, vendor_id=82, vbid=110),
            Model(hyperid=83, ts=82, hid=107, vendor_id=82, vbid=100),
            Model(hyperid=84, ts=82, hid=107, vendor_id=82, vbid=0),
            Model(hyperid=85, ts=85, hid=107, vendor_id=85, vbid=10),
            Model(hyperid=86, ts=86, hid=107, vendor_id=86, vbid=10),
            # Model(hyperid=87) - one of siblings is better with score, another is better with bid
        ]

        cls.index.offers += [
            Offer(hyperid=70, fesh=14, fee=1000, price=1000, waremd5='RcSMzi4xxxxqGvxRx8atJg'),
            Offer(hyperid=71, fesh=14, fee=500, price=1000),
            Offer(hyperid=72, fesh=14, fee=0, price=1000),
            Offer(hyperid=73, fesh=14, fee=0, price=1000),
            Offer(hyperid=74, fesh=14, fee=100, price=1000),
            Offer(hyperid=75, fesh=14, fee=0, price=1000),
            Offer(hyperid=76, fesh=15, fee=100, price=1000),
            Offer(hyperid=77, fesh=14, fee=500, price=1000),
            Offer(hyperid=81, fesh=14, fee=50, price=1000),
            Offer(hyperid=82, fesh=14, fee=60, price=1000),
            Offer(hyperid=83, fesh=14, fee=50, price=1000),
            Offer(hyperid=84, fesh=14, fee=0, price=1000),
            Offer(hyperid=85, fesh=14, fee=2000, price=1000),
            Offer(hyperid=86, fesh=14, fee=10, price=1000),
        ]

        cls.recommender.on_request_accessory_models(
            model_id=79, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['70:0.5', '71:0.5', '72:0.5', '73:0.5', '74:0.5', '75:0.5', '76:0.5', '77:0.5']})
        cls.recommender.on_request_accessory_models(
            model_id=80, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['81:0.5', '82:0.5', '83:0.5', '84:0.5']})
        cls.recommender.on_request_accessory_models(
            model_id=87, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['85:0.4', '86:0.6']})

        cls.index.reserveprice_fee += [ReservePriceFee(hyper_id=107, reserveprice_fee=0.1)]

    def test_missing_pp(self):
        request = (
            'place=madv_incut&hyperid=79&numdoc=8'
            + self._rearr_factors
            + '&show-urls=productVendorBid,cpa&debug=1'
            + '&rearr-factors=market_competitive_model_card_reserve_price_fee_coef=0.0'
        )
        response = self.report.request_json(request, add_defaults=False)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'incutId': 'CompetitiveModelCardCarousel',
                        "items": [
                            {
                                "urls": {
                                    "encrypted": Contains(
                                        "hyper_cat_id=107",
                                        "pp={}".format(self.COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_DESKTOP),
                                    ),
                                }
                            }
                        ],
                    }
                ]
            },
        )

    def test_more_than_one_competitive_model(self):
        request = (
            'place=madv_incut&hyperid=79&numdoc=8'
            + self._rearr_factors
            + '&show-urls=productVendorBid,cpa&debug=1'
            + '&rearr-factors=market_competitive_model_card_reserve_price_fee_coef=0.0'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'incutId': 'CompetitiveModelCardCarousel',
                        "items": ElementCount(8),
                    }
                ]
            },
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'incutId': 'CompetitiveModelCardCarousel',
                        "items": [
                            {
                                "urls": {
                                    "encrypted": Contains(
                                        "hyper_cat_id=107",
                                        "pp={}".format(self.COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_DESKTOP),
                                    ),
                                }
                            }
                        ],
                    }
                ]
            },
        )

    def test_correct_urls_for_carousel(self):
        """
        проверка правильных PP в зависимости от платформы
        @see https://st.yandex-team.ru/MEDIAADV-198
        """
        params = {
            'place': 'madv_incut',
            'hyperid': 79,
            'numdoc': 8,
            'show-urls': 'productVendorBid,cpa',
        }
        rearr_flags = {
            'market_competitive_model_card_do_not_bill': 0,
            'market_competitive_model_card_closeness_threshold': 10,
            'split': 'empty',
            'market_competitive_model_card_disable_remove_empty_do': 1,  # отключение функционала по запрету выдачи моделей без ДО
            'market_competitive_model_card_reserve_price_fee_coef': 0.0,
        }

        def __get_expected_response(pp):
            return {
                "results": [
                    {
                        'incutId': 'CompetitiveModelCardCarousel',
                        "items": [
                            {
                                "urls": {
                                    "encrypted": Contains(
                                        "pp={}".format(pp),
                                    ),
                                }
                            }
                        ],
                    }
                ]
            }

        # desktop
        params['platform'] = 'desktop'
        params['client'] = 'frontend'
        response = self.report.request_json(self.get_request(params, rearr_flags))
        self.assertFragmentIn(response, __get_expected_response(self.COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_DESKTOP))

        # touch
        params['touch'] = 1
        response = self.report.request_json(self.get_request(params, rearr_flags))
        self.assertFragmentIn(response, __get_expected_response(self.COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_TOUCH))

        # android
        params.pop('platform')
        params.pop('touch')
        params['client'] = 'ANDROID'
        response = self.report.request_json(self.get_request(params, rearr_flags))
        self.assertFragmentIn(response, __get_expected_response(self.COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_ANDROID))

        # ios
        params['client'] = 'IOS'
        response = self.report.request_json(self.get_request(params, rearr_flags))
        self.assertFragmentIn(response, __get_expected_response(self.COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_IOS))

    def test_correct_urls_for_single(self):
        """
        проверка правильных PP в зависимости от платформы
        @see https://st.yandex-team.ru/MEDIAADV-198
        """
        params = {
            'place': 'madv_incut',
            'hyperid': 1,
            'numdoc': 8,
            'show-urls': 'productVendorBid,cpa',
        }
        rearr_flags = {
            'market_competitive_model_card_do_not_bill': 0,
            'market_competitive_model_card_closeness_threshold': 10,
            'split': 'empty',
            'market_competitive_model_card_disable_remove_empty_do': 1,  # отключение функционала по запрету выдачи моделей без ДО
            'market_competitive_model_card_disable_multiple_cards': 1,
        }

        def __get_expected_response(pp):
            return {
                "results": [
                    {
                        'incutId': 'CompetitiveModelCardSingle',
                        "items": [
                            {
                                "urls": {
                                    "encrypted": Contains(
                                        "pp={}".format(pp),
                                    ),
                                }
                            }
                        ],
                    }
                ]
            }

        # desktop
        params['platform'] = 'desktop'
        params['client'] = 'frontend'
        response = self.report.request_json(self.get_request(params, rearr_flags))
        self.assertFragmentIn(response, __get_expected_response(self.COMPETITIVE_MODEL_CARD_ON_DESKTOP))

        # touch
        params['touch'] = 1
        response = self.report.request_json(self.get_request(params, rearr_flags))
        self.assertFragmentIn(response, __get_expected_response(self.COMPETITIVE_MODEL_CARD_ON_TOUCH))

        # android
        params.pop('platform')
        params.pop('touch')
        params['client'] = 'ANDROID'
        response = self.report.request_json(self.get_request(params, rearr_flags))
        self.assertFragmentIn(response, __get_expected_response(self.COMPETITIVE_MODEL_CARD_ON_ANDROID))

        # ios
        params['client'] = 'IOS'
        response = self.report.request_json(self.get_request(params, rearr_flags))
        self.assertFragmentIn(response, __get_expected_response(self.COMPETITIVE_MODEL_CARD_ON_IOS))


if __name__ == '__main__':
    main()
