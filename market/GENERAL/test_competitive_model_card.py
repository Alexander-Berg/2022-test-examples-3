#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    ClickType,
    Model,
    Offer,
    Region,
    YamarecPlace,
    YamarecSettingPartition,
    Shop,
    MarketSku,
    BlueOffer,
    UrlType,
    GLParam,
    HyperCategory,
    HyperCategoryType,
)
from core.types.reserveprice_fee import ReservePriceFee
from core.types.recommended_fee import RecommendedFee
from core.testcase import TestCase, main
from core.matcher import Absent, Contains, ElementCount, NotEmptyList, Capture, NotEmpty
from core.cpc import Cpc


def dict_to_rearr(rearr_flags_dict):
    return ';'.join([rearr_name + '=' + str(rearr_flags_dict[rearr_name]) for rearr_name in rearr_flags_dict])


class Pp:
    COMPETITIVE_MODEL_CARD_ON_DESKTOP = 162
    COMPETITIVE_MODEL_CARD_ON_TOUCH = 662
    COMPETITIVE_MODEL_CARD_ON_ANDROID = 1761
    COMPETITIVE_MODEL_CARD_ON_IOS = 1861
    COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_DESKTOP = 168
    COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_TOUCH = 668
    COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_ANDROID = 1768
    COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_IOS = 1868


class T(TestCase):

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
        Модели, офферы и конфигурация для выдачи place=competitive_model_card для первого простого теста
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

        request = 'place=competitive_model_card' + self._rearr_factors + '&hyperid=1&debug=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})

    def test_cgi_cpa_real(self):
        """
        Проверяем, что при &cpa=real все работает также, как и без
        """

        request = 'place=competitive_model_card' + self._rearr_factors + '&hyperid=1&debug=1&cpa=real'
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
            'place=competitive_model_card'
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
            'place=competitive_model_card'
            + self._rearr_factors
            + ';market_max_analogs_data_in_show_log_in_competitive_model_card=3&hyperid=1&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})
        self.show_log.expect(analogs_data="5,440;3,300;2,200", original_model_id=1)

    def test_models_scores_in_shows_log_absent(self):
        """
        Проверяем, что
        1. При равных ставках выигрывает модель с большей степенью похожести
        2. market_log_analogs_data_in_competitive_model_card=0 выключает логирование данных по моделям-аналогам в show-log
        """

        request = (
            'place=competitive_model_card'
            + self._rearr_factors
            + ';market_log_analogs_data_in_competitive_model_card=0&hyperid=1&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})
        self.show_log.expect(analogs_data=Absent(), original_model_id=Absent())

    @classmethod
    def prepare_vendor_bids(cls):
        model_ids = list(range(7, 12))
        random_ts = [7, 8, 9, 10, 11, 12]
        cls.index.models += [
            Model(hyperid=x[0], ts=x[1], hid=101, vbid=50 if x[0] % 2 == 1 else 100) for x in zip(model_ids, random_ts)
        ]

        cls.index.offers += [Offer(hyperid=hyperid, fesh=13, fee=0, price=1000) for hyperid in model_ids]

        cls.recommender.on_request_accessory_models(model_id=7, item_count=1000, version='SIBLINGS1_AUGMENTED').respond(
            {'models': ['8:0.1', '9:0.2', '10:0.15', '11:0.1', '12:0.1']}
        )

    def test_vendor_bids(self):
        """
        Провеяем, что если ранк меньше, но ставка позволяет выиграть, модель выигрывает, а автоброкер работает ожидаемо
        """

        request = 'place=competitive_model_card' + self._rearr_factors + '&hyperid=7&debug=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 10, "sale": {"vBid": 100, "vendorClickPrice": 67}})

    @classmethod
    def prepare_no_similar_models(cls):
        _ = list(range(7, 12))
        _ = [7, 8, 9, 10, 11, 12]
        cls.index.models += [
            Model(hyperid=13, ts=13, hid=101),
            Model(hyperid=14, ts=14, hid=101),
        ]
        cls.index.offers += [
            Offer(hyperid=13, fesh=13, fee=0, price=1000),
            Offer(hyperid=14, fesh=13, fee=0, price=1000),
        ]

        cls.recommender.on_request_accessory_models(
            model_id=13, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': []})

    def test_no_similar_models(self):
        """
        Провеяем, что если для модели нет похожих, то репорт не упадет случайно, два варианта:
         * в рекомендаторе есть модель, но у неё нет похожих,
         * модели нет в рекомендаторе -сейчас не проверяем, так как рекомендатор в таком случае выдает ошибку
        """

        request = 'place=competitive_model_card' + self._rearr_factors + '&hyperid=13&debug=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(0)})

        # request = 'place=competitive_model_card&rearr-factors=split=empty&hyperid=14&debug=1'
        # response = self.report.request_json(request)
        # self.assertFragmentIn(response, {"results": ElementCount(0)})

    @classmethod
    def prepare_filter_out_no_bid(cls):
        cls.index.models += [
            Model(hyperid=15, ts=15, hid=101, vbid=50),
            Model(hyperid=16, ts=16, hid=101, vbid=50),
            Model(hyperid=17, ts=17, hid=101, vbid=50),
            Model(hyperid=18, ts=18, hid=101),
            Model(hyperid=19, ts=19, hid=101, vbid=50),
        ]

        model_ids = list(range(15, 19))

        cls.index.offers += [Offer(hyperid=hyperid, fesh=13, fee=0, price=1000) for hyperid in model_ids]

        cls.recommender.on_request_accessory_models(
            model_id=15, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['16:0.1', '17:0.2', '18:0.95', '19:0.1']})

    def test_filter_out_no_bid(self):
        """
        Провеяем, что модель без ставки не попарает в ответ
        """

        request = 'place=competitive_model_card' + self._rearr_factors + '&hyperid=15&debug=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 17, "sale": {"vBid": 50, "vendorClickPrice": 25}})

    def test_market_competitive_model_request_items_count_zero(self):
        """
        Проверяем, что market_competitive_model_original_items_count=1 попадет модель с большим скором похожести, а поскольку ее ставка = 0 - то ничего
        """

        request = (
            'place=competitive_model_card'
            + self._rearr_factors
            + ';market_competitive_model_original_items_count=1&hyperid=15&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(0)})

    def test_market_competitive_model_request_items_count_two(self):
        """
        Проверяем, что market_competitive_model_original_items_count=2 попадет модель со ставкой и без ставкии; поскольку следующей модели нет, vendorClickPrice=1
        """

        request = (
            'place=competitive_model_card'
            + self._rearr_factors
            + ';market_competitive_model_original_items_count=2&hyperid=15&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 17, "sale": {"vBid": 50, "vendorClickPrice": 1}})

    def test_market_competitive_model_request_items_count_three(self):
        """
        Проверяем, что market_competitive_model_original_items_count=3 попадет модель со ставкой и без ставкии; следущая модель есть, vendorClickPrice=25
        """

        request = (
            'place=competitive_model_card'
            + self._rearr_factors
            + ';market_competitive_model_original_items_count=3&hyperid=15&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 17, "sale": {"vBid": 50, "vendorClickPrice": 25}})

    @classmethod
    def prepare_only_one_bid(cls):
        cls.index.models += [
            Model(hyperid=20, ts=20, hid=101),
            Model(hyperid=21, ts=21, hid=101, vbid=50),
            Model(hyperid=22, ts=22, hid=101),
        ]

        model_ids = list(range(20, 22))

        cls.index.offers += [Offer(hyperid=hyperid, fesh=13, fee=0, price=1000) for hyperid in model_ids]

        cls.recommender.on_request_accessory_models(
            model_id=20, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['21:0.95', '22:0.99']})

    def test_only_one_bid(self):
        """
        Провеяем, что если у модели только одна похожая с вендорской ставкой, то она обиллится по мин ставке
        """

        request = 'place=competitive_model_card' + self._rearr_factors + '&hyperid=20&debug=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 21, "sale": {"vBid": 50, "vendorClickPrice": 1}})

    @classmethod
    def prepare_vendor_no_bids(cls):
        model_ids = list(range(23, 25))
        random_ts = list(range(23, 25))
        cls.index.models += [Model(hyperid=x[0], ts=x[1], hid=101) for x in zip(model_ids, random_ts)]

        cls.index.offers += [Offer(hyperid=hyperid, fesh=13, fee=0, price=1000) for hyperid in model_ids]

        cls.recommender.on_request_accessory_models(
            model_id=23, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['24:0.1', '25:0.2']})

    def test_vendor_no_bids(self):
        """
        Провеяем, что если нет ставок на модель, то нет и моделей в ответе: нет ручек, нет варенья
        """

        request = 'place=competitive_model_card' + self._rearr_factors + '&hyperid=23&debug=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(0)})

    def test_zero_click_price(self):
        '''
        Проверяем работу флага market_competitive_model_card_do_not_bill
        по дефолту за конкурентную карточку модели вендора не биллятся
        '''

        _ = self.report.request_json(
            'place=competitive_model_card&hyperid=15'
            '&rearr-factors=market_write_click_price_to_fuid=1'
            ';market_competitive_model_card_closeness_threshold=10'
            ';split=empty'
            ';market_competitive_model_card_disable_remove_empty_do=1'
            '&show-urls=productVendorBid'
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='modelcard',
            hyper_id=17,
            vc_bid=50,
            vendor_price=0,
            fuid=Contains('vcp=25'),
        )

    def test_bill_click(self):
        '''
        Проверяем работу флага market_competitive_model_card_do_not_bill
        но если явно запросить, то биллятся
        '''
        _ = self.report.request_json(
            'place=competitive_model_card&hyperid=7&show-urls=productVendorBid' + self._rearr_factors
        )

        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=10, vc_bid=100, vendor_price=67)

    def test_disabled(self):
        """
        Провеяем, что работает флаг аварийного выкулючения плейса
        """

        request = (
            'place=competitive_model_card'
            '&rearr-factors=split=empty'
            ';market_competitive_model_card_disable=1'
            ';market_competitive_model_card_closeness_threshold=10'
            ';market_competitive_model_card_disable_remove_empty_do=1'
            '&hyperid=20&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(0)})

    @classmethod
    def prepare_autobroker_use_another_vendor(cls):
        cls.index.models += [
            Model(hyperid=26, ts=26, hid=102, vendor_id=1),
            Model(hyperid=27, ts=27, hid=102, vendor_id=2, vbid=100),
            Model(hyperid=28, ts=28, hid=102, vendor_id=3, vbid=120),
            Model(hyperid=29, ts=29, hid=102, vendor_id=3, vbid=120),
        ]

        model_ids = list(range(26, 29))

        cls.index.offers += [Offer(hyperid=hyperid, fesh=13, fee=0, price=1000) for hyperid in model_ids]

        cls.recommender.on_request_accessory_models(
            model_id=26, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['27:0.2', '28:0.4', '29:0.3']})

    def test_autobroker_use_another_vendor(self):
        '''
        Проверяем, что автоброкер выберет для амнистии лучшую модель другого вендора, и вендор не будет соревноавться сам с собой
        '''

        response = self.report.request_json(
            'place=competitive_model_card&hyperid=26' '' + self._rearr_factors + '&show-urls=productVendorBid&debug=1'
        )

        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains("CalculateAutobrokerMultiplier(): Competitive model id is 28 auction value: 2880")
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains("CalculateAutobrokerMultiplier(): Second competitive model id is 27 auction value: 1200")
                ]
            },
        )

        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=28, vc_bid=120, vendor_price=50)

    @classmethod
    def prepare_closeness_threshold(cls):
        cls.index.models += [
            Model(hyperid=30, ts=30, hid=103, vendor_id=1),
            Model(hyperid=31, ts=31, hid=103, vendor_id=2, vbid=1500),
            Model(hyperid=32, ts=32, hid=103, vendor_id=3, vbid=1200),
            Model(hyperid=33, ts=33, hid=103, vendor_id=4, vbid=20),
            Model(hyperid=330, ts=330, hid=103, vendor_id=5, vbid=5),
        ]

        model_ids = list(range(30, 33))

        cls.index.offers += [Offer(hyperid=hyperid, fesh=13, fee=0, price=1000) for hyperid in model_ids]

        cls.recommender.on_request_accessory_models(
            model_id=30, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['31:0.2', '32:0.29', '33:0.4', '330:0.5']})

    def test_closeness_threshold(self):
        '''
        Проверяем, дефолтное значение порога - 0.21 или 2100 - в реарр    флаге
        '''

        response = self.report.request_json(
            'place=competitive_model_card&hyperid=30'
            '&rearr-factors='
            'split=empty'
            ';market_competitive_model_card_disable_remove_empty_do=1'
            '&show-urls=productVendorBid&debug=1'
        )

        self.assertFragmentIn(response, {"filters": {"MODEL_IS_NOT_SIMILAR_ENOUGH": 1}})
        self.assertFragmentIn(response, "score == 0 for model: 33")

    def test_closeness_threshold_2(self):
        '''
        Проверяем, что работает порог по степени похожести модели
        '''

        response = self.report.request_json(
            'place=competitive_model_card&hyperid=30'
            '&rearr-factors='
            'market_competitive_model_card_closeness_threshold=4400'
            ';split=empty'
            ';market_competitive_model_card_disable_remove_empty_do=1'
            '&show-urls=productVendorBid&debug=1'
        )

        self.assertFragmentIn(response, {"filters": {"MODEL_IS_NOT_SIMILAR_ENOUGH": 3}})
        self.assertFragmentIn(response, "score == 0 for model: 330")

    @classmethod
    def prepare_bid_threshold(cls):
        cls.index.models += [
            Model(hyperid=34, ts=34, hid=104, vendor_id=1),
            Model(hyperid=35, ts=35, hid=104, vendor_id=2, vbid=10),
            Model(hyperid=36, ts=36, hid=104, vendor_id=3, vbid=11),
            Model(hyperid=37, ts=37, hid=104, vendor_id=4, vbid=15),
        ]

        model_ids = list(range(34, 37))

        cls.index.offers += [Offer(hyperid=hyperid, fesh=13, fee=0, price=1000) for hyperid in model_ids]

        cls.recommender.on_request_accessory_models(
            model_id=34, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['35:0.2', '36:0.3', '37:0.004']})

    def test_bid_threshold(self):
        '''
        Проверяем, что работает порог по вендорской ставке
        '''

        response = self.report.request_json(
            'place=competitive_model_card&hyperid=34'
            '&rearr-factors='
            'market_competitive_model_card_closeness_threshold=10'
            ';market_competitive_model_card_bid_threshold=12'
            ';market_competitive_model_only_bid_models=1'
            ';split=empty'
            ';market_competitive_model_card_disable_remove_empty_do=1'
            '&show-urls=productVendorBid&debug=1'
        )

        self.assertFragmentIn(response, {"filters": {"VENDOR_BID": 2}})
        self.assertFragmentIn(response, "score == 0 for model: 37")

    @classmethod
    def prepare_fee(cls):
        cls.index.models += [
            Model(hyperid=41, ts=41, hid=105, vendor_id=1),
            Model(hyperid=42, ts=42, hid=105, vendor_id=2, vbid=20),
            Model(hyperid=43, ts=43, hid=105, vendor_id=3, vbid=10),
            Model(hyperid=44, ts=44, hid=105, vendor_id=4, vbid=10),
        ]

        model_ids = [43, 44]

        cls.index.offers += [Offer(hyperid=hyperid, fesh=13, fee=10 * hyperid, price=1000) for hyperid in model_ids]

        cls.recommender.on_request_accessory_models(
            model_id=41, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['42:0.2', '43:0.2', '44:0.2']})

    def test_fee(self):
        '''
        Проверяем, что fee учитывается
        У 42 модели самая высокая ставка, но нет оффера, поэтому она выпадает из розыгрыша
        У остальных моделей одинаковая вендорская ставка. Выигрывает модель с большей ставкой на оффере
        '''

        response = self.report.request_json(
            'place=competitive_model_card&hyperid=41&debug=1&show-urls=productVendorBid' + self._rearr_factors
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 44,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 440, "brokeredFee": 435}}}]},
                        "debug": {"sale": {"vBid": 10, "vendorClickPrice": 10}},
                    }
                ]
            },
        )
        self.assertFragmentIn(
            response, "CalculateAutobrokerMultiplier(): Competitive model id is 44 auction value: 208"
        )

        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=44, vc_bid=10, vendor_price=10)

    @classmethod
    def prepare_fee_with_rp(cls):
        cls.index.models += [
            Model(hyperid=91, ts=91, hid=107, vendor_id=0),
            Model(hyperid=92, ts=92, hid=107, vendor_id=0),
        ]

        cls.index.offers += [
            Offer(hyperid=91, fesh=13, fee=1001, price=1000, hid=107),
            Offer(hyperid=92, fesh=13, fee=1001, price=1000, hid=107),
        ]

        cls.recommender.on_request_accessory_models(
            model_id=91, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['92:0.5']})

        cls.index.reserveprice_fee += [ReservePriceFee(hyper_id=107, reserveprice_fee=0.001)]

    def test_fee_with_rp_sum_fee_less_than_rp(self):
        """
        Тестируем, что при  market_competitive_model_card_reserve_price_fee_coef=1 в ККМ перестают попадать модели,
        у которых суммарная ставка меньше.
        """
        response = self.report.request_json(
            'place=competitive_model_card&hyperid=61&debug=1&show-urls=productVendorBid'
            + self._rearr_factors
            + ';market_competitive_model_card_reserve_price_fee_coef=1;'
        )

        self.assertFragmentIn(
            response,
            {"results": []},
        )

        response = self.report.request_json(
            'place=competitive_model_card&hyperid=61&debug=1&show-urls=productVendorBid' + self._rearr_factors
        )

        self.assertFragmentIn(
            response,
            {"results": NotEmptyList()},
        )

    def test_fee_only_last_offer_amnesty(self):
        """
        Тестируем, что у последнего оффера в выдаче списываемая ставка vbid=0 (когда исходные ставки vbid отсутствуют)
        """
        response = self.report.request_json(
            'place=competitive_model_card&hyperid=91&debug=1&show-urls=productVendorBid&numdoc=16'
            + self._rearr_factors
            + ';market_competitive_model_card_reserve_price_fee_coef=0;'
            + ';market_buybox_auction_cpa_competitive_model_card=0;market_use_minimal_amnesty_in_competitive_model_card=0'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 92,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 1001, "brokeredFee": 1000}}}]},
                        "debug": {"sale": {"vBid": 0, "vendorClickPrice": 0}},
                    }
                ]
            },
        )

    def test_fees_and_bids_null(self):
        '''
        Проверяем, что с флагом market_set_fees_and_bids_null все ставки и fee зануляются
        '''

        response = self.report.request_json(
            'place=competitive_model_card&hyperid=41&show-urls=productVendorBid&debug=1'
            + self._rearr_factors
            + ';market_set_fees_and_bids_null=1;'
        )
        # Так как ставки занулены, в ответ не попадает ничего
        self.assertFragmentIn(response, {"results": Absent()})

    def test_fee_disabled(self):
        '''
        Проверяем работу флага market_competitive_model_card_enable_do
        По умолчанию он включен. Пробуем его выключить. В этом случае выигрывает 42 модель, потому что ДО не запрашивается.
        Вендорская ставка амнистируется до 10, потому что у ближайшей похожей модели ставка 10 и соотношение итоговых очков получается как 2 к 1
        '''

        response = self.report.request_json(
            'place=competitive_model_card&hyperid=41&debug=1&show-urls=productVendorBid'
            '&rearr-factors='
            'market_competitive_model_card_closeness_threshold=10'
            ';market_competitive_model_card_enable_do=0'
            ';market_competitive_model_card_do_not_bill=0'
            ';split=empty'
            ';market_competitive_model_card_disable_remove_empty_do=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 42,
                        "offers": {'count': 0},
                        "debug": {"sale": {"vBid": 20, "vendorClickPrice": 10}},
                    }
                ]
            },
        )
        self.assertFragmentIn(response, "CalculateAutobrokerMultiplier(): Competitive model id is 42 auction value: 4")

        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=42, vc_bid=20, vendor_price=10)

    @classmethod
    def prepare_fee_without_vbid(cls):
        cls.index.models += [
            Model(hyperid=46, ts=46, hid=455, vendor_id=1, vbid=10),
            Model(hyperid=47, ts=47, hid=455, vendor_id=2, vbid=0),
            Model(hyperid=48, ts=48, hid=455, vendor_id=3, vbid=0),
            Model(hyperid=49, ts=49, hid=455, vendor_id=3, vbid=0),
        ]

        cls.index.reserveprice_fee += [ReservePriceFee(hyper_id=455, reserveprice_fee=0.01)]

        model_ids = [46, 47, 48]

        cls.index.offers += [
            Offer(hyperid=hyperid, fesh=13, fee=hyperid * 10, price=1000, cpa=Offer.CPA_REAL) for hyperid in model_ids
        ]
        cls.index.offers += [
            Offer(hyperid=49, fesh=13, fee=10, price=1000, cpa=Offer.CPA_REAL)
        ]  # fee < reserveprice_fee, model 49 with vbid=1 must be filtered

        cls.recommender.on_request_accessory_models(
            model_id=46, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['47:0.2', '48:0.2', '49:0.2']})

        cls.recommender.on_request_accessory_models(
            model_id=48, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['46:0.2', '47:0.2', '49:0.2']})

    def test_fee_without_vbid(self):
        '''
        Проверяем, что fee учитывается и модель не отфильтровывается даже если вендорская ставка отсутствует (но есть офферная)
        Выигрывает модель с большей ставкой на оффере
        '''

        response = self.report.request_json(
            'place=competitive_model_card&hyperid=46&debug=1&show-urls=productVendorBid'
            + self._rearr_factors
            + ';market_competitive_model_card_reserve_price_fee_coef=0.0'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 48,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 480, "brokeredFee": 470}}}]},
                        "debug": {"sale": {"vBid": 0, "vendorClickPrice": 0}},
                    }
                ]
            },
        )
        self.assertFragmentIn(response, "CalculateAutobrokerMultiplier(): Competitive model id is 48 auction value: 96")
        self.assertFragmentIn(
            response, "CalculateAutobrokerMultiplier(): Second competitive model id is 47 auction value: 94"
        )
        self.assertFragmentIn(response, "score == 0 for model: 49")

    def test_vbid_mix_fee_only_competition(self):
        '''
        Проверяем, что fee учитывается и модель не отфильтровывается даже если вендорская ставка отсутствует (но есть офферная)
        Проверяем, что выигрывает модель, где наибольшая взвешенная сумма vbid и fee
        '''

        response = self.report.request_json(
            'place=competitive_model_card&hyperid=48&debug=1&show-urls=productVendorBid'
            + self._rearr_factors
            + ';market_competitive_model_card_reserve_price_fee_coef=0.0'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 46,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 460, "brokeredFee": 203}}}]},
                        "debug": {"sale": {"vBid": 10, "vendorClickPrice": 5}},
                    }
                ]
            },
        )
        self.assertFragmentIn(
            response, "CalculateAutobrokerMultiplier(): Competitive model id is 46 auction value: 212"
        )
        self.assertFragmentIn(
            response, "CalculateAutobrokerMultiplier(): Second competitive model id is 47 auction value: 94"
        )
        self.assertFragmentIn(response, "score == 0 for model: 49")

        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=46, vc_bid=10, vendor_price=5)

    @classmethod
    def prepare_fee_amnesty(cls):
        cls.index.models += [
            Model(hyperid=51, ts=51, hid=105, vendor_id=1),
            Model(hyperid=52, ts=52, hid=105, vendor_id=2, vbid=10),
            Model(hyperid=53, ts=53, hid=105, vendor_id=3, vbid=10),
            Model(hyperid=54, ts=54, hid=105, vendor_id=4, vbid=12),
        ]

        cls.index.offers += [
            Offer(hyperid=52, fesh=13, fee=1200, price=1000),
            Offer(hyperid=53, fesh=13, fee=600, price=1000),
            Offer(hyperid=54, fesh=13, fee=0, price=1000),
        ]

        cls.recommender.on_request_accessory_models(
            model_id=51, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['52:0.2', '53:0.2', '54:0.2']})

    def test_fee_amnesty(self):
        '''
        Проверяем, амнистию вендорской ставки с учетом fee
        Итоговые очки 52 модели получаются так:
            600 - вендорская ставка сконвертированная в fee
            1200 - fee на оффере
            0.2 - коэффициент похожести
            (600+1200)*0.2 = 360
        Итоговые очки 53 модели получаются так:
            600 - вендорская ставка сконвертированная в fee
            600 - fee на оффере
            0.2 - коэффициент похожести
            (600+600)*0.2 = 240
        Итоговое соотношение очков получается 3 к 2
        Поэтому вендорская ставка и brokeredFee умножаются на 2/3
        '''

        response = self.report.request_json(
            'place=competitive_model_card&hyperid=51&debug=1&show-urls=productVendorBid' + self._rearr_factors
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 52,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 1200, "brokeredFee": 800}}}]},
                        "debug": {"sale": {"vBid": 10, "vendorClickPrice": 7}},
                    }
                ]
            },
        )
        self.assertFragmentIn(
            response, "CalculateAutobrokerMultiplier(): Competitive model id is 52 auction value: 360"
        )
        self.assertFragmentIn(
            response, "CalculateAutobrokerMultiplier(): Second competitive model id is 53 auction value: 240"
        )

        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=52, vc_bid=10, vendor_price=7)

    def test_fee_amnesty_disabled(self):
        '''
        Проверяем работу флага market_competitive_model_card_enable_do
        Побеждает 54 модель из-за более высокой ставки
        '''
        response = self.report.request_json(
            'place=competitive_model_card&hyperid=51&debug=1&show-urls=productVendorBid'
            '&rearr-factors='
            'market_competitive_model_card_closeness_threshold=10'
            ';market_competitive_model_card_enable_do=0'
            ';market_competitive_model_card_do_not_bill=0;split=empty;'
            ';market_competitive_model_card_disable_remove_empty_do=1'
        )
        self.assertFragmentIn(
            response,
            {"results": [{"entity": "product", "id": 54, "debug": {"sale": {"vBid": 12, "vendorClickPrice": 10}}}]},
        )
        self.assertFragmentIn(
            response, "CalculateAutobrokerMultiplier(): Competitive model id is 54 auction value: 2.4"
        )

        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=54, vc_bid=12, vendor_price=10)

    @classmethod
    def prepare_fee_no_amnesty(cls):
        cls.index.models += [
            Model(hyperid=61, ts=61, hid=105, vendor_id=1),
            Model(hyperid=62, ts=62, hid=105, vendor_id=2, vbid=10),
            Model(hyperid=63, ts=63, hid=105, vendor_id=3, vbid=10),
            Model(hyperid=64, ts=64, hid=105, vendor_id=4, vbid=10),
        ]

        cls.index.offers += [
            Offer(hyperid=62, fesh=13, fee=1200, price=1000),
            Offer(hyperid=63, fesh=13, fee=600, price=1000),
            Offer(hyperid=64, fesh=13, fee=0, price=1000),
        ]

        cls.recommender.on_request_accessory_models(
            model_id=61, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['62:0.2', '63:0.3', '64:0.2']})

    def test_fee_no_amnesty(self):
        '''
        Повторяем test_fee_amnesty, но в этот раз поднимаем степень похожести 63 модели.
        Итоговые очки сравниваются. Амнистии нет
        Тест, возможно, может плавать, потому что итоговые очки получаются одинаковые и может победить 62 модель
        '''

        response = self.report.request_json(
            'place=competitive_model_card&hyperid=61&debug=1&show-urls=productVendorBid' + self._rearr_factors
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 63,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 600, "brokeredFee": 600}}}]},
                        "debug": {"sale": {"vBid": 10, "vendorClickPrice": 10}},
                    }
                ]
            },
        )
        self.assertFragmentIn(
            response, "CalculateAutobrokerMultiplier(): Competitive model id is 63 auction value: 360"
        )
        self.assertFragmentIn(
            response, "CalculateAutobrokerMultiplier(): Second competitive model id is 62 auction value: 360"
        )

        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=63, vc_bid=10, vendor_price=10)

    def test_android_app(self):
        """
        Проверяем работу параметра client=android: по умолчанию, в приложеньках (client == ANDROID || client == IOS) врезку показываем
        """
        request = 'place=competitive_model_card' + self._rearr_factors + '&hyperid=1&debug=1&client=ANDROID'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})

        """
        Не показываем врезку в приложении под включенным флагом market_competitive_model_card_disable_in_application
        """
        request = (
            'place=competitive_model_card'
            '&rearr-factors='
            'market_competitive_model_card_closeness_threshold=10'
            ';market_competitive_model_card_do_not_bill=0'
            ';market_competitive_model_card_disable_in_application=1'
            ';split=empty'
            ';market_competitive_model_card_disable_remove_empty_do=1'
            '&hyperid=1&debug=1&client=ANDROID'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(0)})
        self.assertFragmentIn(
            response, {"logicTrace": [Contains("Competitive model card is disabled for mobile applications")]}
        )

        """
        Не показываем врезку на андройде под включенным флагом market_competitive_model_card_disable_in_android
        """
        request = (
            'place=competitive_model_card'
            '&rearr-factors='
            'market_competitive_model_card_closeness_threshold=10'
            ';market_competitive_model_card_do_not_bill=0'
            ';market_competitive_model_card_disable_in_android=1'
            ';split=empty'
            ';market_competitive_model_card_disable_remove_empty_do=1'
            '&hyperid=1&debug=1&client=ANDROID'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(0)})
        self.assertFragmentIn(
            response, {"logicTrace": [Contains("Competitive model card is disabled for Android application")]}
        )

        """
        но не аффектим IOS и все остальное
        """
        request = (
            'place=competitive_model_card'
            '&rearr-factors='
            'market_competitive_model_card_closeness_threshold=10'
            ';market_competitive_model_card_do_not_bill=0'
            ';market_competitive_model_card_disable_in_android=1'
            ';split=empty'
            ';market_competitive_model_card_disable_remove_empty_do=1'
            '&hyperid=1&debug=1&client=IOS'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})

        request = 'place=competitive_model_card&rearr-factors=market_competitive_model_card_closeness_threshold=10;market_competitive_model_card_do_not_bill=0;market_competitive_model_card_disable_in_android=1;split=empty&hyperid=1&debug=1'  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})

    def test_ios_app(self):
        """
        Проверяем работу параметра client=ios: по умолчанию, в приложеньках (client == ANDROID || client == IOS) врезку показываем
        """
        request = 'place=competitive_model_card' + self._rearr_factors + '&hyperid=1&debug=1&client=IOS'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})

        """
        Не показываем врезку в приложении под включенным флагом market_competitive_model_card_disable_in_application
        """
        request = (
            'place=competitive_model_card'
            '&rearr-factors='
            'market_competitive_model_card_closeness_threshold=10'
            ';market_competitive_model_card_do_not_bill=0'
            ';market_competitive_model_card_disable_in_application=1'
            ';split=empty'
            ';market_competitive_model_card_disable_remove_empty_do=1'
            '&hyperid=1&debug=1&client=IOS'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(0)})
        self.assertFragmentIn(
            response, {"logicTrace": [Contains("Competitive model card is disabled for mobile applications")]}
        )

        """
        Не показываем врезку на IOS  под включенным флагом market_competitive_model_card_disable_in_ios
        """
        request = (
            'place=competitive_model_card'
            '&rearr-factors='
            'market_competitive_model_card_closeness_threshold=10'
            ';market_competitive_model_card_do_not_bill=0'
            ';market_competitive_model_card_disable_in_ios=1'
            ';split=empty'
            ';market_competitive_model_card_disable_remove_empty_do=1'
            '&hyperid=1&debug=1&client=IOS'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(0)})
        self.assertFragmentIn(
            response, {"logicTrace": [Contains("Competitive model card is disabled for IOS application")]}
        )

        """
        но не аффектим IOS и все остальное
        """
        request = (
            'place=competitive_model_card'
            '&rearr-factors='
            'market_competitive_model_card_closeness_threshold=10'
            ';market_competitive_model_card_do_not_bill=0'
            ';market_competitive_model_card_disable_in_ios=1'
            ';split=empty'
            ';market_competitive_model_card_disable_remove_empty_do=1'
            '&hyperid=1&debug=1&client=ANDROID'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})

        request = (
            'place=competitive_model_card'
            '&rearr-factors='
            'market_competitive_model_card_closeness_threshold=10'
            ';market_competitive_model_card_do_not_bill=0'
            ';market_competitive_model_card_disable_in_ios=1'
            ';split=empty'
            ';market_competitive_model_card_disable_remove_empty_do=1'
            '&hyperid=1&debug=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"modelId": 5, "sale": {"vBid": 100, "vendorClickPrice": 69}})

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

    def test_more_than_one_competitive_model(self):
        '''
        Проверяем, что работает параметр numdoc, хотим 4 модели, 1-ый обиллится о второй, 2-ой о третий
        3-ий и 4-ый о 6-го (vendor_id == 76) так как они от одного вендора
        7-ой обогнал 8-го (из-за ДО), и оббилился об него (из-за разных мерчей)
        Магазинные ставки учитываются
        '''

        request = (
            'place=competitive_model_card&hyperid=79&numdoc=8'
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
                        "entity": "product",
                        "id": 70,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 1000, "brokeredFee": 757}}}]},
                        "debug": {"sale": {"vBid": 100, "vendorClickPrice": 76}},
                    },
                    {
                        "entity": "product",
                        "id": 71,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 500, "brokeredFee": 396}}}]},
                        "debug": {"sale": {"vBid": 80, "vendorClickPrice": 64}},
                    },
                    {
                        "entity": "product",
                        "id": 72,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 0, "brokeredFee": 0}}}]},
                        "debug": {"sale": {"vBid": 70, "vendorClickPrice": 40}},
                    },
                    {
                        "entity": "product",
                        "id": 73,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 0, "brokeredFee": 0}}}]},
                        "debug": {"sale": {"vBid": 60, "vendorClickPrice": 40}},
                    },
                    {
                        "entity": "product",
                        "id": 74,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 100, "brokeredFee": 77}}}]},
                        "debug": {"sale": {"vBid": 50, "vendorClickPrice": 39}},
                    },
                    {
                        "entity": "product",
                        "id": 75,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 0, "brokeredFee": 0}}}]},
                        "debug": {"sale": {"vBid": 40, "vendorClickPrice": 14}},
                    },
                    {
                        "entity": "product",
                        "id": 77,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 500, "brokeredFee": 287}}}]},
                        "debug": {"sale": {"vBid": 5, "vendorClickPrice": 3}},
                    },
                    {
                        "entity": "product",
                        "id": 76,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 100, "brokeredFee": 0}}}]},
                        "debug": {"sale": {"vBid": 6, "vendorClickPrice": 1}},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=70, vc_bid=100, vendor_price=76)
        # Сейчас живём без CPA ссылок
        # self.click_log.expect(clicktype=ClickType.CPA, hyper_id=70, shop_fee=100, shop_fee_ab=76)

    def test_more_than_one_competitive_model_with_rp(self):
        '''
        Проверяем, что работает параметр numdoc, хотим 4 модели, 1-ый обиллится о второй, 2-ой о третий
        3-ий и 4-ый о 6-го (vendor_id == 76) так как они от одного вендора
        7-ой обогнал 8-го (из-за ДО), и оббилился об него (из-за разных мерчей)
        Магазинные ставки учитываются
        '''

        request = (
            'place=competitive_model_card&hyperid=79&numdoc=8'
            + self._rearr_factors
            + ";market_competitive_model_card_reserve_price_fee_coef=0.5;"
            + '&show-urls=productVendorBid,cpa&debug=1'
        )

        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 70,  # value = 0.5 * (1000 - 500 + 6000) * 1000 # 0.735
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 1000, "brokeredFee": 869}}}]},
                        "debug": {"sale": {"vBid": 100, "vendorClickPrice": 74}},
                    },
                    {
                        "entity": "product",
                        "id": 71,  # value = 0.5 * (500 - 500 + 4800) * 1000 #multiplier = 0.875
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 500, "brokeredFee": 500}}}]},
                        "debug": {"sale": {"vBid": 80, "vendorClickPrice": 70}},
                    },
                    {
                        "entity": "product",
                        "id": 72,  # value = 0.5 * (4200) * 1000 # amnesty with 75 multiplier = 0.57
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 0, "brokeredFee": 0}}}]},
                        "debug": {"sale": {"vBid": 70, "vendorClickPrice": 40}},
                    },
                    {
                        "entity": "product",
                        "id": 73,  # value = 0.5 * 3600 * 1000 # amnesty with 75 multiplier = 0.66
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 0, "brokeredFee": 0}}}]},
                        "debug": {"sale": {"vBid": 60, "vendorClickPrice": 40}},
                    },
                    {
                        "entity": "product",
                        "id": 75,  # value = 0.5 * 2400 * 1000 # amnesty with 6
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 0, "brokeredFee": 0}}}]},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=True,
        )

        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=70, vc_bid=100, vendor_price=74)
        # Сейчас живём без CPA ссылок
        # self.click_log.expect(clicktype=ClickType.CPA, hyper_id=70, shop_fee=100, shop_fee_ab=76)

    def test_more_than_one_competitive_model_with_rp_large_rp(self):
        '''
        Проверяем амнистию в случае, когда рп очень большое. Мерчеые ставки не должны амнистироваться.
        1-ый обиллится о второй, 2-ой о третий.
        '''

        request = (
            'place=competitive_model_card&hyperid=79&numdoc=8'
            + self._rearr_factors
            + ";market_competitive_model_card_reserve_price_fee_coef=1;"
            + '&show-urls=productVendorBid,cpa&debug=1'
        )

        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 70,  # value = 0.5 * (6000) * 1000
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 1000, "brokeredFee": 1000}}}]},
                    },
                    {
                        "entity": "product",
                        "id": 71,  # value = 0.5 * (4800) * 1000
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 500, "brokeredFee": 500}}}]},
                    },
                    {
                        "entity": "product",
                        "id": 72,  # value = 0.5 * (4200) * 1000
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 0, "brokeredFee": 0}}}]},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=True,
        )

    def test_replace_pp_desktop(self):
        '''
        Проверяем правильность подмены Pp для карусельной ККМ на десктопе
        '''
        request = (
            'place=competitive_model_card&hyperid=79&min-num-doc=5&numdoc=16'
            + self._rearr_factors
            + '&show-urls=productVendorBid&debug=1&pp='
            + str(Pp.COMPETITIVE_MODEL_CARD_ON_DESKTOP)
            + '&rearr-factors=market_competitive_model_card_reserve_price_fee_coef=0.0'
        )
        response = self.report.request_json(request)
        cpc = Cpc.create_for_offer(
            click_price=2,
            offer_id='RcSMzi4xxxxqGvxRx8atJg',
            bid=2,
            minimal_bid=2,
            shop_id=14,
            shop_fee=1000,
            fee=757,
            minimal_fee=0,
            bid_type='minbid',
            hid=107,
            click_price_before_bid_correction=2,
            pp=Pp.COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_DESKTOP,
        )
        self.assertFragmentIn(response, {'results': [{'offers': {'items': [{'cpc': str(cpc)}]}}]})

        self.click_log.expect(pp=Pp.COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_DESKTOP)

        request = (
            'place=competitive_model_card&hyperid=79&'
            + self._rearr_factors
            + '&show-urls=productVendorBid&debug=1&pp='
            + str(Pp.COMPETITIVE_MODEL_CARD_ON_DESKTOP)
        )
        self.report.request_json(request)

        self.click_log.expect(pp=Pp.COMPETITIVE_MODEL_CARD_ON_DESKTOP)

    def test_replace_pp_touch(self):
        '''
        Проверяем правильность подмены Pp для карусельной ККМ на таче
        '''
        request = (
            'place=competitive_model_card&hyperid=79&min-num-doc=5&numdoc=16'
            + self._rearr_factors
            + '&show-urls=productVendorBid&debug=1&pp='
            + str(Pp.COMPETITIVE_MODEL_CARD_ON_TOUCH)
        )
        self.report.request_json(request)

        self.click_log.expect(pp=Pp.COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_TOUCH)

        request = (
            'place=competitive_model_card&hyperid=79&'
            + self._rearr_factors
            + '&show-urls=productVendorBid&debug=1&pp='
            + str(Pp.COMPETITIVE_MODEL_CARD_ON_TOUCH)
        )
        self.report.request_json(request)

        self.click_log.expect(pp=Pp.COMPETITIVE_MODEL_CARD_ON_TOUCH)

    def test_replace_pp_android(self):
        '''
        Проверяем правильность подмены Pp для карусельной ККМ на андройде
        '''
        request = (
            'place=competitive_model_card&hyperid=79&min-num-doc=5&numdoc=16'
            + self._rearr_factors
            + '&show-urls=productVendorBid&debug=1&pp='
            + str(Pp.COMPETITIVE_MODEL_CARD_ON_ANDROID)
        )
        self.report.request_json(request)

        self.click_log.expect(pp=Pp.COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_ANDROID)

        request = (
            'place=competitive_model_card&hyperid=79&'
            + self._rearr_factors
            + '&show-urls=productVendorBid&debug=1&pp='
            + str(Pp.COMPETITIVE_MODEL_CARD_ON_ANDROID)
        )
        self.report.request_json(request)

        self.click_log.expect(pp=Pp.COMPETITIVE_MODEL_CARD_ON_ANDROID)

    def test_replace_pp_ios(self):
        '''
        Проверяем правильность подмены Pp для карусельной ККМ на IOS
        '''
        request = (
            'place=competitive_model_card&hyperid=79&min-num-doc=5&numdoc=16'
            + self._rearr_factors
            + '&show-urls=productVendorBid&debug=1&pp='
            + str(Pp.COMPETITIVE_MODEL_CARD_ON_IOS)
        )
        self.report.request_json(request)

        self.click_log.expect(pp=Pp.COMPETITIVE_MODEL_CARD_MULTIPLE_CARDS_ON_IOS)

        request = (
            'place=competitive_model_card&hyperid=79&'
            + self._rearr_factors
            + '&show-urls=productVendorBid&debug=1&pp='
            + str(Pp.COMPETITIVE_MODEL_CARD_ON_IOS)
        )
        self.report.request_json(request)

        self.click_log.expect(pp=Pp.COMPETITIVE_MODEL_CARD_ON_IOS)

    def test_replace_pp_disable(self):
        '''
        Проверяем отключение подмены Pp для карусельной ККМ
        '''
        request = (
            'place=competitive_model_card&hyperid=79&min-num-doc=5&numdoc=16'
            + self._rearr_factors
            + ';market_competitive_model_card_disable_pp_replacement_for_multiple_cards=1&show-urls=productVendorBid&debug=1&pp='
            + str(Pp.COMPETITIVE_MODEL_CARD_ON_IOS)
        )
        self.report.request_json(request)

        self.click_log.expect(pp=Pp.COMPETITIVE_MODEL_CARD_ON_IOS)

    def test_min_num_doc_in_more_than_one_competitive_model(self):
        '''
        Проверяем, что работает параметр min-num-doc
        Ожидаем первый докуент соло, магазинная ставка учитывается :)

        '''

        request = (
            'place=competitive_model_card&hyperid=79&min-num-doc=16'
            + self._rearr_factors
            + '&show-urls=productVendorBid&debug=1'
            + '&rearr-factors=market_competitive_model_card_reserve_price_fee_coef=0.0'
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(response, {"results": ElementCount(1)})

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 70,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 1000, "brokeredFee": 757}}}]},
                        "debug": {"sale": {"vBid": 100, "vendorClickPrice": 76}},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=70, vc_bid=100, vendor_price=76)

    def test_min_num_doc_in_more_than_one_competitive_model_and_zero_score(self):
        '''
        Проверяем, что работает параметр min-num-doc
        Ожидаем один документ - всего конкурентов столько, сколько в min-num-doc, но у одного скор нулевой, и он не пройдёт в ответ,
        значит, валидных документов (min-num-doc - 1), чего не достаточно для карусели => в таком случае возвращаем 1 документ.
        '''

        request = (
            'place=competitive_model_card&hyperid=80&min-num-doc=4&numdoc=4'
            + self._rearr_factors
            + '&show-urls=productVendorBid&debug=1'
            + '&rearr-factors=market_competitive_model_card_reserve_price_fee_coef=0.0'
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(response, {"results": ElementCount(1)})

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 82,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 60, "brokeredFee": 54}}}]},
                        "debug": {"sale": {"vBid": 110, "vendorClickPrice": 100}},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=82, vc_bid=110, vendor_price=100
        )

    def test_min_num_doc_and_numdoc_in_more_than_one_competitive_model(self):
        '''
        Проверяем, что если моделек не хватило до numdoc, но больше min-num-doc, то вернутся все модельки
        У последней цена клика - min vendor bid
        '''

        request = (
            'place=competitive_model_card&hyperid=79&min-num-doc=5&numdoc=16'
            + self._rearr_factors
            + '&show-urls=productVendorBid&debug=1'
            + '&rearr-factors=market_competitive_model_card_reserve_price_fee_coef=0.0'
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 70,
                    },
                    {
                        "entity": "product",
                        "id": 71,
                    },
                    {
                        "entity": "product",
                        "id": 72,
                    },
                    {
                        "entity": "product",
                        "id": 73,
                    },
                    {
                        "entity": "product",
                        "id": 74,
                    },
                    {
                        "entity": "product",
                        "id": 75,
                    },
                    {
                        "entity": "product",
                        "id": 77,
                    },
                    {
                        "entity": "product",
                        "id": 76,
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=76, vc_bid=6, vendor_price=1
        )  # min vendor price

    def test_disable_multiple_cards(self):
        '''
        Проверяем, что флаг market_competitive_model_card_disable_multiple_cards выключает карусель-мод для ККМ
        для этого повторим запрос из предыдущего теста с данным флагом
        '''

        request = (
            'place=competitive_model_card&hyperid=79&min-num-doc=5&numdoc=16'
            + self._rearr_factors
            + ';market_competitive_model_card_disable_multiple_cards=1&show-urls=productVendorBid&debug=1'
            + '&rearr-factors=market_competitive_model_card_reserve_price_fee_coef=0.0'
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(response, {"results": ElementCount(1)})

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 70,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 1000, "brokeredFee": 757}}}]},
                        "debug": {"sale": {"vBid": 100, "vendorClickPrice": 76}},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=70, vc_bid=100, vendor_price=76)

    @classmethod
    def prepare_remove_empty_default_offer(cls):

        model_id = 7700
        cls.index.models += [Model(hyperid=model_id + n, ts=model_id + n, hid=101, vbid=50) for n in range(0, 4)]

        cls.index.offers += [
            Offer(hyperid=model_id + n, fesh=13, fee=10, price=1000)
            for n in range(0, 3)  # only first 3 (4th without offer)
        ]

        cls.recommender.on_request_accessory_models(
            model_id=7700, item_count=1000, version='SIBLINGS1_AUGMENTED'  # for first model
        ).respond(
            {'models': ['7701:0.3', '7702:0.8']}  # с офферами  # с офферами (должен быть этот)
        )
        cls.recommender.on_request_accessory_models(
            model_id=7701, item_count=1000, version='SIBLINGS1_AUGMENTED'  # for second model
        ).respond(
            {'models': ['7702:0.1', '7703:0.8']}  # с офферами, но м меньшей похожестью (должен быть этот)  # без
        )
        cls.recommender.on_request_accessory_models(
            model_id=7702, item_count=1000, version='SIBLINGS1_AUGMENTED'  # for third model
        ).respond(
            {'models': ['7703:0.8']}  # без оффера
        )

    def test_remove_empty_default_offer(self):
        """
        проверяем, что модели без ДО (дефолтного оффера) удаляются из выдачи
        """
        request = str(
            'place=competitive_model_card'
            '&rearr-factors='
            'market_competitive_model_card_do_not_bill=0'
            ';market_competitive_model_card_closeness_threshold=10'
            ';split=empty'
            # ';market_competitive_model_card_disable_remove_empty_do=1'
            '&debug=1'
        )

        # для 7700 модели подходит с наибольшей похожестью (у всех есть офферы)
        response = self.report.request_json(request + '&hyperid=7700')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "offers": {
                            "items": [
                                {
                                    "entity": "offer",
                                    'model': {
                                        'id': 7702,
                                    },
                                }
                            ]
                        }
                    },
                ]
            },
            preserve_order=True,
        )

        # для 7701 модели. подходят 7702 и 7703(больше похожесть, но нет офферов)
        response = self.report.request_json(request + '&hyperid=7701')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "offers": {
                            "items": [
                                {
                                    "entity": "offer",
                                    'model': {
                                        'id': 7702,
                                    },
                                }
                            ]
                        }
                    },
                ]
            },
            preserve_order=True,
        )

        # для 7702 подходит только 7703, но она без оферов - должна быть пустая выдача
        response = self.report.request_json(request + '&hyperid=7702')
        self.assertListEqual(response['results'], list())  # не должен ничего выдавать

    def test_models_to_request_flag(self):
        '''
        Проверяем, что флагом market_competitive_model_card_additional_model_to_request можно менять кол-во дополнительно
        запрашиваемых моделей
        '''

        # Сначала запросим с флагом=1 и numdoc=1: будет запрошено флаг + numdoc = 2 модели
        # Для обеих моделей будет запрошен ДО, с учётом ставки ДО получим, что выигрывает модель hyperid=85 (ставка перевешивает скор)
        request = (
            'place=competitive_model_card&hyperid=87&numdoc=1'
            + self._rearr_factors
            + '&show-urls=productVendorBid&debug=1&rearr-factors=market_competitive_model_card_additional_model_to_request='
        )
        response = self.report.request_json(request + str(1))

        self.assertFragmentIn(response, {"results": ElementCount(1)})
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 85,
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Теперь запросим с флагом=0 и numdoc=1: будет запрошена 1 модель
        # Так как запрашивается лучшая по скору модель, то выиграет hyperid=86, потому что конкурентов по ставкам нет (модели для них не запросили)
        response = self.report.request_json(request + str(0))

        self.assertFragmentIn(response, {"results": ElementCount(1)})
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 86,
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_competitive_model_card_buybox_auction(cls):
        cls.index.shops += [
            Shop(
                fesh=801,
                priority_region=213,
                cpa=Shop.CPA_REAL,
                cis=Shop.CIS_REAL,
                cpc=Shop.CPC_NO,
                name='DSBS магазин 1',
            ),
            Shop(
                fesh=802,
                priority_region=213,
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name='3P поставщик 1',
            ),
            Shop(
                fesh=803,
                priority_region=213,
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name='3P поставщик 2',
            ),
        ]
        cls.index.models += [
            Model(
                hid=3001,
                hyperid=3001,
                ts=501,
                title='model_1',
                vbid=11,
                glparams=[GLParam(param_id=1, value=1)],
            ),
            Model(
                hid=3002,
                hyperid=3002,
                ts=502,
                title='model_2',
                vbid=11,
                glparams=[GLParam(param_id=1, value=2)],
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                title="msku_1",
                hid=3001,
                hyperid=3001,
                sku=100001,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=802,
                        fee=200,
                        waremd5="BLUE-100001-FEED-0001Q",
                        title="3P buybox offer 1",
                        ts=7001,
                    ),
                    BlueOffer(
                        price=1630,
                        feedid=803,
                        fee=100,
                        waremd5="BLUE-100001-FEED-0002Q",
                        title="3P buybox offer 2",
                        ts=7002,
                    ),
                ],
            ),
            MarketSku(
                title="msku_2",
                hid=3002,
                hyperid=3002,
                sku=100002,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=802,
                        fee=200,
                        waremd5="BLUE-100002-FEED-0001Q",
                        title="3P buybox offer 1",
                        ts=7003,
                    ),
                    BlueOffer(
                        price=1630,
                        feedid=803,
                        fee=100,
                        waremd5="BLUE-100002-FEED-0002Q",
                        title="3P buybox offer 2",
                        ts=7004,
                    ),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(
            model_id=3001, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['3002:0.2']})

    def test_competitive_model_card_buybox_auction(self):
        """
        Проверяем, что в ККМ работает аукцион в байбоксе под флагом market_buybox_auction_cpa_competitive_model_card
        """
        request = 'place=competitive_model_card&pp=18&hyperid=3001&debug=da'
        rearr_flags_dict = {
            'market_competitive_model_card_do_not_bill': 0,
            'market_buybox_auction_cpa_competitive_model_card': 1,
            'market_competitive_model_card_closeness_threshold': 10,
            'market_buybox_auction_coef_b': 0.8,  # повышаем значимость ставки
            'split': 'empty',
        }
        response = self.report.request_json(request + '&rearr-factors=' + dict_to_rearr(rearr_flags_dict))
        # Проверим, что выиграл оффер именно по аукциону
        ware_md5_winner, ware_md5_auctioned = Capture(), Capture()
        self.assertFragmentIn(
            response,
            {
                "wareId": NotEmpty(capture=ware_md5_winner),
                "buyboxDebug": {
                    "Offers": [
                        {
                            "IsWinnerByRandom": False,
                            "ShopFee": 200,
                            "AuctionedShopFee": 102,
                            "WareMd5": NotEmpty(capture=ware_md5_auctioned),
                        },
                    ],
                    "WonMethod": "WON_BY_AUCTION",
                },
            },
        )
        self.assertTrue(ware_md5_winner.value == ware_md5_auctioned.value)

        # Проверяем, что флаг market_buybox_auction_cpa_competitive_model_card отключает аукцион в ККМ
        rearr_flags_dict['market_buybox_auction_cpa_competitive_model_card'] = 0
        response = self.report.request_json(request + '&rearr-factors=' + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentNotIn(response, "WON_BY_AUCTION")
        self.assertFragmentNotIn(response, "AuctionedShopFee")

    def test_filters_exist(self):
        """
        Проверяем, что с флагом market_competitive_model_card_show_filters у модели есть фильтры
        """
        request = 'place=competitive_model_card&pp=18&hyperid=3001'
        rearr_flags_dict = {
            'market_competitive_model_card_closeness_threshold': 10,
            'split': 'empty',
            'market_competitive_model_card_show_filters': 1,
        }
        response = self.report.request_json(request + '&rearr-factors=' + dict_to_rearr(rearr_flags_dict))

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "filters": [
                            {
                                "id": "1",
                                "values": [
                                    {
                                        "id": "2",
                                    },
                                ],
                            }
                        ],
                    }
                ]
            },
        )

    def test_filters_not_exist(self):
        """
        Проверяем, что по умолчанию на плейсе competitive_model_card моедль есть а фильтры не выводятся
        """
        request = 'place=competitive_model_card&pp=18&hyperid=3001'
        rearr_flags_dict = {
            'market_competitive_model_card_closeness_threshold': 10,
            'split': 'empty',
        }
        response = self.report.request_json(request + '&rearr-factors=' + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "filters": Absent(),
                    }
                ]
            },
        )

    def test_competitive_model_card_buybox_auction_price_threshold(self):
        """
        Проверяем, что для включенного аукциона в ККМ работает флаг market_buybox_competitive_model_card_price_rel_max_threshold
        """
        request = 'place=competitive_model_card&pp=18&hyperid=3001&debug=da'
        rearr_flags_dict = {
            'market_competitive_model_card_do_not_bill': 0,
            'market_buybox_auction_cpa_competitive_model_card': 1,
            'market_competitive_model_card_closeness_threshold': 10,
            'market_buybox_competitive_model_card_price_rel_max_threshold': 1.005,  # Оффер за 1640 дороже, чем оффер за 1630, поэтому он не пройдёт (1640 / 1630 > 1.006)
            'market_buybox_auction_coef_b': 0.8,  # повышаем значимость ставки
            'market_buybox_enable_advert_buybox': 0,
            'split': 'empty',
        }
        response = self.report.request_json(request + '&rearr-factors=' + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "buyboxDebug": {
                    "Offers": ElementCount(1),  # второй оффер не прошёл в байбокс из-за разницы в ценах на 10 рублей
                }
            },
        )

    def test_competitive_model_card_buybox_auction_price_threshold_adv(self):
        """
        Проверяем, что для включенного аукциона в ККМ работает флаг market_buybox_competitive_model_card_price_rel_max_threshold
        """
        request = 'place=competitive_model_card&pp=18&hyperid=3001&debug=da'
        rearr_flags_dict = {
            'market_competitive_model_card_do_not_bill': 0,
            'market_buybox_auction_cpa_competitive_model_card': 1,
            'market_competitive_model_card_closeness_threshold': 10,
            'market_buybox_adv_buybox_price_rel_max_threshold': 1.005,  # Оффер за 1640 дороже, чем оффер за 1630, поэтому он не пройдёт (1640 / 1630 > 1.006)
            'market_blue_buybox_max_price_rel_add_diff_adv': 0,
            'market_buybox_auction_coef_b': 0.8,  # повышаем значимость ставки
            'market_buybox_enable_advert_buybox': 1,
            'split': 'empty',
        }
        response = self.report.request_json(request + '&rearr-factors=' + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "buyboxDebug": {
                    "Offers": ElementCount(1),  # второй оффер не прошёл в байбокс из-за разницы в ценах на 10 рублей
                }
            },
        )

    def test_competitive_model_card_buybox_auction_coeffs(self):
        """
        Проверяем, специальные флаги аукциона в байбоксе для ККМ
        Поскольку проверяем аукционы, нужно перерасчитать AuctionValue по ответу репорта
        """

        def get_auction_value(gmv, shop_fee, vendor_fee, rp_fee, ue_add, price, A=0, B=0, C=0, D=0, E=0, F=0, G=0, W=0):
            # В полной формуле есть ещё функция f, но она сейчас в проде просто возвращает аргумент
            return (
                gmv
                * (
                    A
                    + B * (max(0, shop_fee + vendor_fee - W * rp_fee) + G * ue_add + E + float(F) / price)
                    + float(C) / price
                )
                + D
            )

        def parse_auction_in_response(response, buybox_auction_coeffs):
            # Парсим ответ репорта и возвращаем пересчитанное значение аукциона и значение аукциона, полученное репортом
            shop_fee, offer_vendor_fee, gmv_randomized, price, auction_value_parsed = (
                Capture(),
                Capture(),
                Capture(),
                Capture(),
                Capture(),
            )
            ware_md5_winner = 'BLUE-100002-FEED-0001Q'
            self.assertFragmentIn(
                response,
                {
                    "wareId": ware_md5_winner,
                    "buyboxDebug": {
                        "Offers": [
                            {
                                "IsWinnerByRandom": False,
                                "ShopFee": NotEmpty(capture=shop_fee),
                                "OfferVendorFee": NotEmpty(capture=offer_vendor_fee),
                                "AuctionedShopFee": NotEmpty(),
                                "GmvRandomized": NotEmpty(capture=gmv_randomized),
                                "WareMd5": ware_md5_winner,
                                # Тут нет rp_fee
                                # Юнит-экономика фактически не попадает в формулу аукциона, поэтому мы её тут не берём
                                "PriceAfterCashback": NotEmpty(capture=price),
                                "AuctionValueRandomized": NotEmpty(capture=auction_value_parsed),
                            },
                        ],
                        "WonMethod": "WON_BY_AUCTION",
                    },
                },
            )
            auction_value_recomputed = get_auction_value(
                gmv=gmv_randomized.value,
                shop_fee=shop_fee.value,
                vendor_fee=offer_vendor_fee.value,
                rp_fee=0,
                ue_add=0,
                price=price.value,
                **buybox_auction_coeffs
            )
            return auction_value_recomputed, auction_value_parsed.value

        request_base_kkm = 'place=competitive_model_card&pp=18&hyperid=3001&debug=da'
        rearrs_base = (
            '&rearr-factors='
            + dict_to_rearr(
                {
                    'market_competitive_model_card_do_not_bill': 0,
                    'market_buybox_auction_cpa_competitive_model_card': 1,
                    'market_competitive_model_card_closeness_threshold': 10,
                    'split': 'empty',
                }
            )
            + ';'
        )

        def check_recomputed_equals_parsed(rearr_flags_dict, buybox_auction_coeffs, request_base=request_base_kkm):
            response = self.report.request_json(request_base + rearrs_base + dict_to_rearr(rearr_flags_dict))
            auction_value_recomputed, auction_value_parsed = parse_auction_in_response(response, buybox_auction_coeffs)
            self.assertAlmostEqual(auction_value_recomputed, auction_value_parsed, delta=0.1)

        # Проверяем, что на аукцион влияют специальные флаги для ККМ
        check_recomputed_equals_parsed(
            {
                'market_buybox_auction_coef_a_additive_bid_coef_kkm': 1,
                'market_buybox_auction_coef_b_multiplicative_bid_coef_kkm': 0.01,
                'market_buybox_auction_coef_e_additive_coef_inside_bid_kkm': 0.2,
                'market_buybox_auction_coef_f_div_price_coef_in_bid_kkm': 20,
                'market_buybox_auction_coef_w_rp_fee_coef_kkm': 1,
            },
            {
                'A': 1,
                'B': 0.01,
                'E': 0.2,
                'F': 20,
                'W': 1,
            },
        )

    @classmethod
    def prepare_buybox_auction_amnesty_in_kkm(cls):
        cls.index.models += [
            Model(hid=3003, hyperid=3003, ts=503, title='model_3', vbid=11),
            Model(hid=3004, hyperid=3004, ts=504, title='model_4', vbid=12),
            Model(hid=3005, hyperid=3005, ts=505, title='model_5', vbid=13),
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_3_msku_1",
                hid=3003,
                hyperid=3003,
                sku=100003,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=802,
                        fee=200,
                        waremd5="BLUE-100003-FEED-0001Q",
                        title="model_3 3P buybox offer 1",
                        ts=7005,
                    ),
                ],
            ),
            MarketSku(
                title="model_4_msku_1",
                hid=3004,
                hyperid=3004,
                sku=100004,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=803,
                        fee=210,
                        waremd5="BLUE-100004-FEED-0001Q",
                        title="model_4 3P buybox offer 1",
                        ts=7006,
                    ),
                ],
            ),
            MarketSku(
                title="model_5_msku_1",
                hid=3005,
                hyperid=3005,
                sku=100005,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1630,
                        feedid=803,
                        fee=220,
                        waremd5="BLUE-100005-FEED-0001Q",
                        title="model_5 3P buybox offer 1",
                        ts=7007,
                    ),
                    BlueOffer(
                        price=1632,
                        feedid=802,
                        fee=230,
                        waremd5="BLUE-100005-FEED-0002Q",
                        title="model_5 3P buybox offer 2",
                        ts=7008,
                    ),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(
            model_id=3002, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['3003:0.2', '3004:0.25', '3005:0.28']})

    def test_buybox_auction_amnesty_in_kkm(self):
        """
        Проверяем, что для аукциона в ККМ берётся минимальная амнистия из аукциона ККМ и аукциона в байбоксе.
        Работает под флагом market_use_minimal_amnesty_in_competitive_model_card
        """

        def get_expected_response(with_minimal_amnesty):
            return {
                "results": [
                    {
                        "slug": "model-5",
                        "offers": {
                            "items": [
                                {
                                    "slug": "model-5-3p-buybox-offer-2",
                                    "wareId": "BLUE-100005-FEED-0002Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 230,
                                            # Для этого ДО разыгрывался аукцион в байбоксе, итоговая амнистия зависит от флага market_use_minimal_amnesty_in_competitive_model_card
                                            "brokeredFee": 224 if with_minimal_amnesty else 188,
                                        },
                                        "buyboxDebug": {
                                            "Offers": [
                                                {
                                                    "WareMd5": "BLUE-100005-FEED-0002Q",
                                                    "AuctionedShopFee": 224,  # Если выбираем минимальную амнистию, то эта ставка должна попасть в brokeredFee
                                                },
                                            ],
                                        },
                                    },
                                },
                            ],
                        },
                        "debug": {
                            "sale": {
                                "vBid": 13,
                                "vendorClickPrice": 11,
                            },
                        },
                    },
                    {
                        "slug": "model-4",
                        "offers": {
                            "items": [
                                {
                                    "slug": "model-4-3p-buybox-offer-1",
                                    "wareId": "BLUE-100004-FEED-0001Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 210,
                                            # Тут аукциона в байбоксе не было (так как оффер 1, нет конкурентов)
                                            # Поэтому списание будет производиться за аукцион ККМ
                                            "brokeredFee": 155,
                                        },
                                    },
                                },
                            ],
                        },
                        "debug": {
                            "sale": {
                                "vBid": 12,
                                "vendorClickPrice": 9,
                            }
                        },
                    },
                    {
                        "slug": "model-3",
                        "offers": {
                            "items": [
                                {
                                    "slug": "model-3-3p-buybox-offer-1",
                                    "wareId": "BLUE-100003-FEED-0001Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 200,
                                            # Тут аукциона в байбоксе не было (так как оффер 1, нет конкурентов)
                                            # Это последний оффер, списания shopFee за ДО не будет, потому что не было rp_fee и потому что аукциона в байбоксе не было
                                            "brokeredFee": 0,
                                        },
                                    },
                                },
                            ],
                        },
                        "debug": {
                            "sale": {
                                "vBid": 11,
                                "vendorClickPrice": 2,
                            }
                        },
                    },
                ],
            }

        request_base_kkm = 'place=competitive_model_card&pp=18&hyperid=3002&debug=da&min-num-doc=3&numdoc=10'
        rearr_flags_dict = {
            'market_competitive_model_card_do_not_bill': 0,
            'market_buybox_auction_cpa_competitive_model_card': 1,
            'market_competitive_model_card_closeness_threshold': 10,
            'market_use_minimal_amnesty_in_competitive_model_card': 1,  # Проверяем списания, когда включена минимальная амнистия
            'market_use_additional_brokered_prices_info_in_competitive_model_card': 1,  # По логам хотим понимать, из какого аукциона взята амнистия
            'market_buybox_auction_coef_b_multiplicative_bid_coef_kkm': 0.001,
            'split': 'empty',
        }
        rearrs_str = '&rearr-factors=' + dict_to_rearr(rearr_flags_dict) + ';'
        response = self.report.request_json(request_base_kkm + rearrs_str)
        self.assertFragmentIn(
            response,
            get_expected_response(with_minimal_amnesty=True),
        )
        # Проверим, что по логу можем понять, какая амнистия была взята (shop_fee_ab должна быть равна shop_fee_ab_bb или shop_fee_ab_search, в зависимости от выбранной амнистии)
        self.show_log.expect(
            url_type=6, shop_fee_ab=224, shop_fee_ab_bb=224, shop_fee_ab_search=188, ware_md5="BLUE-100005-FEED-0002Q"
        )
        self.show_log.expect(
            url_type=6, shop_fee_ab=155, shop_fee_ab_bb=0, shop_fee_ab_search=155, ware_md5="BLUE-100004-FEED-0001Q"
        )  # Тут аукциона в байбоксе не было
        self.show_log.expect(
            url_type=6, shop_fee_ab=0, shop_fee_ab_bb=0, shop_fee_ab_search=0, ware_md5="BLUE-100003-FEED-0001Q"
        )
        # Выключаем флаг, чтобы брать не минимальную амнистию, а амнистию аукциона ККМ (игнорируя амнистию аукциона в байбоксе)
        rearr_flags_dict['market_use_minimal_amnesty_in_competitive_model_card'] = 0
        rearrs_str = '&rearr-factors=' + dict_to_rearr(rearr_flags_dict) + ';'
        response = self.report.request_json(request_base_kkm + rearrs_str)
        self.assertFragmentIn(
            response,
            get_expected_response(with_minimal_amnesty=False),
        )
        # Если shop_fee_ab_bb или shop_fee_ab_search пропущены, то использована обычная амнистия (поисковая)
        self.show_log.expect(
            url_type=6,
            shop_fee_ab=188,
            shop_fee_ab_bb=Absent(),
            shop_fee_ab_search=Absent(),
            ware_md5="BLUE-100005-FEED-0002Q",
        )
        self.show_log.expect(
            url_type=6,
            shop_fee_ab=155,
            shop_fee_ab_bb=Absent(),
            shop_fee_ab_search=Absent(),
            ware_md5="BLUE-100004-FEED-0001Q",
        )
        self.show_log.expect(
            url_type=6,
            shop_fee_ab=0,
            shop_fee_ab_bb=Absent(),
            shop_fee_ab_search=Absent(),
            ware_md5="BLUE-100003-FEED-0001Q",
        )

    @classmethod
    def prepare_buybox_auction_amnesty_single_supplier_and_vendor(cls):
        cls.index.models += [
            Model(hid=3006, hyperid=3006, ts=506, title='model_6', vendor_id=6, vbid=13),
            Model(hid=3007, hyperid=3007, ts=507, title='model_7', vendor_id=6, vbid=14),
            Model(hid=3008, hyperid=3008, ts=508, title='model_8', vendor_id=6, vbid=15),
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_6_msku_1",
                hid=3006,
                hyperid=3006,
                sku=100006,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=802,
                        fee=220,
                        waremd5="BLUE-100006-FEED-0001Q",
                        title="model_6 3P buybox offer 1",
                        ts=7009,
                    ),
                    BlueOffer(
                        price=1638,
                        feedid=803,
                        fee=210,
                        waremd5="BLUE-100006-FEED-0002Q",
                        title="model_6 3P buybox offer 2",
                        ts=7010,
                    ),
                ],
            ),
            MarketSku(
                title="model_7_msku_1",
                hid=3007,
                hyperid=3007,
                sku=100007,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=802,
                        fee=220,
                        waremd5="BLUE-100007-FEED-0001Q",
                        title="model_7 3P buybox offer 1",
                        ts=7011,
                    ),
                    BlueOffer(
                        price=1638,
                        feedid=803,
                        fee=210,
                        waremd5="BLUE-100007-FEED-0002Q",
                        title="model_7 3P buybox offer 2",
                        ts=7012,
                    ),
                ],
            ),
            MarketSku(
                title="model_8_msku_1",
                hid=3008,
                hyperid=3008,
                sku=100008,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=802,
                        fee=220,
                        waremd5="BLUE-100008-FEED-0001Q",
                        title="model_8 3P buybox offer 1",
                        ts=7013,
                    ),
                    BlueOffer(
                        price=1638,
                        feedid=803,
                        fee=210,
                        waremd5="BLUE-100008-FEED-0002Q",
                        title="model_8 3P buybox offer 2",
                        ts=7014,
                    ),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(
            model_id=3003, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['3006:0.5', '3007:0.48', '3008:0.46']})

    def test_buybox_auction_amnesty_single_supplier_and_vendor(self):
        """
        Проверяем, что берётся наименьшая амнистия в том случае, когда в ККМ рядом разные MSKU одного поставщика.
        Аукцион в ККМ подразумевает, что амнистию проводим не всегда в соседний оффер, а в оффер, у которого другой поставщик или вендор.
        Но для одинакового поставщика и вендора добиваемся списаний в соседний оффер за счёт аукциона в байбоксе.
        """

        def get_expected_response(with_minimal_amnesty):
            return {
                "results": [
                    {
                        "slug": "model-8",
                        "offers": {
                            "items": [
                                {
                                    "slug": "model-8-3p-buybox-offer-1",
                                    "wareId": "BLUE-100008-FEED-0001Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 220,
                                            "brokeredFee": 215 if with_minimal_amnesty else 0,
                                        },
                                        "buyboxDebug": {
                                            "Offers": [
                                                {
                                                    "WareMd5": "BLUE-100008-FEED-0001Q",
                                                    "AuctionedShopFee": 215,
                                                },
                                            ],
                                        },
                                    },
                                },
                            ],
                        },
                        "debug": {
                            "sale": {
                                "vBid": 15,
                                "vendorClickPrice": 2,  # У всех моделей в выдаче один вендор, поэтому списываем минбид с вендора
                            },
                        },
                    },
                    {
                        "slug": "model-7",
                        "offers": {
                            "items": [
                                {
                                    "slug": "model-7-3p-buybox-offer-1",
                                    "wareId": "BLUE-100007-FEED-0001Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 220,
                                            "brokeredFee": 213 if with_minimal_amnesty else 0,
                                        },
                                        "buyboxDebug": {
                                            "Offers": [
                                                {
                                                    "WareMd5": "BLUE-100007-FEED-0001Q",
                                                    "AuctionedShopFee": 213,
                                                },
                                            ],
                                        },
                                    },
                                },
                            ],
                        },
                        "debug": {
                            "sale": {
                                "vBid": 14,
                                "vendorClickPrice": 2,
                            },
                        },
                    },
                    {
                        "slug": "model-6",
                        "offers": {
                            "items": [
                                {
                                    "slug": "model-6-3p-buybox-offer-1",
                                    "wareId": "BLUE-100006-FEED-0001Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 220,
                                            "brokeredFee": 215 if with_minimal_amnesty else 0,
                                        },
                                        "buyboxDebug": {
                                            "Offers": [
                                                {
                                                    "WareMd5": "BLUE-100006-FEED-0001Q",
                                                    # Хоть это и последний оффер и последняя рекомендуемая модель, был разыгран аукцион в байбоксе,
                                                    # поэтому списываем амнистированную shopFee из аукциона в байбоксе
                                                    "AuctionedShopFee": 215,
                                                },
                                            ],
                                        },
                                    },
                                },
                            ],
                        },
                        "debug": {
                            "sale": {
                                "vBid": 13,
                                "vendorClickPrice": 2,
                            },
                        },
                    },
                ],
            }

        request_base_kkm = 'place=competitive_model_card&pp=18&hyperid=3003&debug=da&min-num-doc=3&numdoc=10'
        rearr_flags_dict = {
            'market_competitive_model_card_do_not_bill': 0,
            'market_buybox_auction_cpa_competitive_model_card': 1,
            'market_buybox_auction_coef_b_multiplicative_bid_coef_kkm': 0.001,
            'market_competitive_model_card_closeness_threshold': 10,
            'market_use_minimal_amnesty_in_competitive_model_card': 1,  # Включаем выбор минимальной амнистии из аукциона ККМ и аукциона в байбоксе
            'market_use_additional_brokered_prices_info_in_competitive_model_card': 1,  # По логам хотим понимать, из какого аукциона взята амнистия
            'split': 'empty',
        }
        rearr_flags_str = '&rearr-factors=' + dict_to_rearr(rearr_flags_dict) + ';'

        response = self.report.request_json(request_base_kkm + rearr_flags_str)
        self.assertFragmentIn(response, get_expected_response(with_minimal_amnesty=True))
        # Проверим, что по логу можем понять, какая амнистия была взята (shop_fee_ab должна быть равна shop_fee_ab_bb или shop_fee_ab_search, в зависимости от выбранной амнистии)
        self.show_log.expect(
            url_type=6, shop_fee_ab=215, shop_fee_ab_bb=215, shop_fee_ab_search=0, ware_md5="BLUE-100008-FEED-0001Q"
        )
        self.show_log.expect(
            url_type=6, shop_fee_ab=213, shop_fee_ab_bb=213, shop_fee_ab_search=0, ware_md5="BLUE-100007-FEED-0001Q"
        )  # Тут аукциона в байбоксе не было
        self.show_log.expect(
            url_type=6, shop_fee_ab=215, shop_fee_ab_bb=215, shop_fee_ab_search=0, ware_md5="BLUE-100006-FEED-0001Q"
        )

        rearr_flags_dict[
            'market_use_minimal_amnesty_in_competitive_model_card'
        ] = 0  # Используется только амнистия из аукциона ККМ
        rearr_flags_str = '&rearr-factors=' + dict_to_rearr(rearr_flags_dict) + ';'
        response = self.report.request_json(request_base_kkm + rearr_flags_str)
        self.assertFragmentIn(response, get_expected_response(with_minimal_amnesty=False))
        # Проверим, что по логу можем понять, какая амнистия была взята (shop_fee_ab должна быть равна shop_fee_ab_bb или shop_fee_ab_search, в зависимости от выбранной амнистии)
        self.show_log.expect(
            url_type=6,
            shop_fee_ab=0,
            shop_fee_ab_bb=Absent(),
            shop_fee_ab_search=Absent(),
            ware_md5="BLUE-100008-FEED-0001Q",
        )
        self.show_log.expect(
            url_type=6,
            shop_fee_ab=0,
            shop_fee_ab_bb=Absent(),
            shop_fee_ab_search=Absent(),
            ware_md5="BLUE-100007-FEED-0001Q",
        )  # Тут аукциона в байбоксе не было
        self.show_log.expect(
            url_type=6,
            shop_fee_ab=0,
            shop_fee_ab_bb=Absent(),
            shop_fee_ab_search=Absent(),
            ware_md5="BLUE-100006-FEED-0001Q",
        )

        # Проверим флаг market_use_additional_brokered_prices_info_in_competitive_model_card, если его отключить, то в логи не попадают shop_fee_ab_bb и shop_fee_ab_search
        rearr_flags_dict['market_use_additional_brokered_prices_info_in_competitive_model_card'] = 0
        rearr_flags_dict[
            'market_use_minimal_amnesty_in_competitive_model_card'
        ] = 1  # Обратно включаем выбор минимальной амнистии
        rearr_flags_str = '&rearr-factors=' + dict_to_rearr(rearr_flags_dict) + ';'
        response = self.report.request_json(request_base_kkm + rearr_flags_str)
        self.assertFragmentIn(response, get_expected_response(with_minimal_amnesty=True))
        self.show_log.expect(
            url_type=6,
            shop_fee_ab=215,
            shop_fee_ab_bb=Absent(),
            shop_fee_ab_search=Absent(),
            ware_md5="BLUE-100008-FEED-0001Q",
        )
        self.show_log.expect(
            url_type=6,
            shop_fee_ab=213,
            shop_fee_ab_bb=Absent(),
            shop_fee_ab_search=Absent(),
            ware_md5="BLUE-100007-FEED-0001Q",
        )  # Тут аукциона в байбоксе не было
        self.show_log.expect(
            url_type=6,
            shop_fee_ab=215,
            shop_fee_ab_bb=Absent(),
            shop_fee_ab_search=Absent(),
            ware_md5="BLUE-100006-FEED-0001Q",
        )

    @classmethod
    def prepare_buybox_auction_1p_pessimization(cls):
        cls.index.shops += [
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                regions=[213],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
        ]
        cls.index.models += [
            Model(hid=3009, hyperid=3009, ts=509, title='model_9', vbid=11),
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_9_msku_1",
                hid=3009,
                hyperid=3009,
                sku=100009,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=2,
                        fee=220,  # Это fee игнорируется, потому что 1p. Будет взята рекомендованная ставка
                        waremd5="BLUE-100009-FEED-0001Q",
                        title="model_9 1P buybox offer 1",
                        ts=7015,
                    ),
                    BlueOffer(
                        price=1640,
                        feedid=803,
                        fee=200,
                        waremd5="BLUE-100009-FEED-0002Q",
                        title="model_9 3P buybox offer 2",
                        ts=7016,
                    ),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(
            model_id=3008, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['3009:0.5']})
        cls.index.recommended_fee += [
            RecommendedFee(hyper_id=3009, recommended_bid=0.0220),
        ]
        cls.index.reserveprice_fee += [
            ReservePriceFee(hyper_id=3009, reserveprice_fee=0.01),
        ]

    def test_buybox_auction_1p_pessimization(self):
        """
        Проверяем, что в аукционе в байбоксе обычно побеждает 1p оффер, так как у него ставка выше, но за счёт коэффициента пессимизации ставки 1p
        market_buybox_auction_coef_1p_pessimization_kkm может победить 3p оффер
        """

        def get_auction_value(
            gmv,
            shop_fee,
            vendor_fee,
            price,
            shop_fee_pessimization_coef,
            rp_fee=0,
            ue_add=0,
            A=1,
            B=0.001,
            C=0,
            D=0,
            E=0,
            F=0,
            G=0,
            W=1,
        ):
            return (
                gmv
                * (
                    A
                    + B
                    * (
                        max(0, shop_fee * shop_fee_pessimization_coef + vendor_fee - W * rp_fee)
                        + G * ue_add
                        + E
                        + float(F) / price
                    )
                    + float(C) / price
                )
                + D
            )

        def get_expected_response(pessimize_1p):
            return {
                "results": [
                    {
                        "slug": "model-9",
                        "offers": {
                            "items": [
                                {
                                    "slug": "model-9-3p-buybox-offer-2"
                                    if pessimize_1p
                                    else "model-9-1p-buybox-offer-1",
                                    "wareId": "BLUE-100009-FEED-0002Q" if pessimize_1p else "BLUE-100009-FEED-0001Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 200 if pessimize_1p else 220,
                                            "brokeredFee": 163 if pessimize_1p else 191,
                                        },
                                        "buyboxDebug": {
                                            "Offers": [
                                                {
                                                    "WareMd5": "BLUE-100009-FEED-0002Q"
                                                    if pessimize_1p
                                                    else "BLUE-100009-FEED-0001Q",
                                                    "AuctionedShopFee": 163 if pessimize_1p else 191,
                                                },
                                            ],
                                            "WonMethod": "WON_BY_AUCTION",
                                        },
                                    },
                                },
                            ],
                        },
                    },
                ],
            }

        def parse_1p_auction(response):
            captures = {
                key: Capture()
                for key in ['auction_value_parsed', 'shop_fee', 'offer_vendor_fee', 'gmv_randomized', 'price']
            }
            self.assertFragmentIn(
                response,
                {
                    "WareMd5": "BLUE-100009-FEED-0001Q",  # 1p offer
                    "AuctionValueRandomized": NotEmpty(capture=captures['auction_value_parsed']),
                    "ShopFee": NotEmpty(capture=captures['shop_fee']),
                    "OfferVendorFee": NotEmpty(capture=captures['offer_vendor_fee']),
                    "GmvRandomized": NotEmpty(capture=captures['gmv_randomized']),
                    "PriceAfterCashback": NotEmpty(capture=captures['price']),
                },
            )
            return captures

        def check_for_rearrs(rearr_flags_dict, pessimization_coef):
            request_base_kkm = 'place=competitive_model_card&pp=18&hyperid=3008&debug=da&min-num-doc=1&numdoc=5'
            rearr_flags_str = '&rearr-factors=' + dict_to_rearr(rearr_flags_dict) + ';'
            response = self.report.request_json(request_base_kkm + rearr_flags_str)
            # Проверяем, что победил нужный оффер
            self.assertFragmentIn(response, get_expected_response(pessimize_1p=(pessimization_coef < 1)))
            # Парсим данные об 1p, переразыгрываем аукцион, чтобы убедиться, что коэффициент правильно учитывается
            captures = parse_1p_auction(response)
            auction_value_recomputed = get_auction_value(
                gmv=captures['gmv_randomized'].value,
                shop_fee=captures['shop_fee'].value,
                vendor_fee=captures['offer_vendor_fee'].value,
                price=captures['price'].value,
                shop_fee_pessimization_coef=pessimization_coef,
                rp_fee=100,
            )
            self.assertAlmostEqual(auction_value_recomputed, captures['auction_value_parsed'].value, delta=0.01)

        rearr_flags_dict = {
            'market_competitive_model_card_do_not_bill': 0,
            'market_buybox_auction_cpa_competitive_model_card': 1,
            'market_competitive_model_card_closeness_threshold': 10,
            'market_use_minimal_amnesty_in_competitive_model_card': 1,
            'market_buybox_auction_coef_b_multiplicative_bid_coef_kkm': 0.001,
            'split': 'empty',
        }

        # Сначала проверяем поведение когда пессимизации нет
        rearr_flags_dict['market_buybox_auction_coef_1p_pessimization_kkm'] = 1.0
        check_for_rearrs(rearr_flags_dict, pessimization_coef=1)

        # Включаем пессимизацию для ККМ
        rearr_flags_dict['market_buybox_auction_coef_1p_pessimization_kkm'] = 0.7
        check_for_rearrs(rearr_flags_dict, pessimization_coef=0.7)

    def test_vendor_click_urls_for_null_bids(self):
        """
        Проверяем, что под флагом market_money_return_vendor_urls_even_for_zero_v_bid возвращаются урлы вендорских кликов всегда, даже если нет ставки
        https://st.yandex-team.ru/MARKETOUT-45277
        https://st.yandex-team.ru/MARKETOUT-44164#61b9e4dc665e882d7089a688
        """

        request = (
            'place=competitive_model_card&hyperid=91&debug=1&show-urls=productVendorBid&numdoc=16'
            + self._rearr_factors
            + ';market_competitive_model_card_reserve_price_fee_coef=0;market_money_return_vendor_urls_even_for_zero_v_bid={}'
            + ';market_buybox_auction_cpa_competitive_model_card=0;market_use_minimal_amnesty_in_competitive_model_card=0'
        )

        # Проверяем старое поведение - когда флаг выключен, урлов нет, потому что вендорская ставка нулевая
        response = self.report.request_json(request.format(0))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 92,
                        "urls": ElementCount(0),
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 1001, "brokeredFee": 1000}}}]},
                        "debug": {"sale": {"vBid": 0, "vendorClickPrice": 0}},
                    }
                ]
            },
        )
        # Когда флаг включён, появляется клик-урл, но ставка в нём всё равно нулевая
        response = self.report.request_json(request.format(1))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 92,
                        "urls": {
                            "encrypted": Contains(
                                "hyper_id=92",
                                "vendor_ds_id=0",
                                "vendor_price=0",
                                "vc_bid=0",
                                "position=1",
                                "url_type=16",
                            ),
                            "direct": Contains("//market.yandex.ru/product/92"),
                        },
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 1001, "brokeredFee": 1000}}}]},
                        "debug": {"sale": {"vBid": 0, "vendorClickPrice": 0}},
                    }
                ]
            },
        )

    @classmethod
    def prepare_merchant_data_in_model_click_urls(cls):
        # Оригинал (ДО на КМ) - 5000
        # Аналоги, которые попадают в ККМ - 5001-5004
        cls.index.models += [
            Model(hid=5000, ts=5000, hyperid=5000, title='model_sausage_original', vbid=11),
            Model(hid=5000, ts=5001, hyperid=5001, title='model_sausage_5001', vbid=11),
            Model(hid=5000, ts=5002, hyperid=5002, title='model_sausage_5002', vbid=11),
            Model(hid=5000, ts=5003, hyperid=5003, title='model_sausage_5003', vbid=11),
            Model(hid=5000, ts=5004, hyperid=5004, title='model_sausage_5004', vbid=11),
        ]

        # Рекомендованная ставка - она будет поставлена на 1p офферы
        cls.index.recommended_fee += [
            RecommendedFee(hyper_id=5001, recommended_bid=0.0220),
        ]

        # MSKU с синими офферами
        cls.index.mskus += [
            # MSKU для модели-аналога 5001
            MarketSku(
                title="msku_sausage_5001_1",
                hyperid=5001,
                sku=500001,
                hid=5000,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        title='blue_sausage_5001_1_1',
                        price=1000,
                        feedid=2,
                        waremd5="BLUE-500001-FEED-0001Q",
                        ts=50011,
                        # Это 1p оффер, поэтому ставка игнорируется
                        # Вместо неё будет поставлена рекомендованная ставка
                    ),
                ],
            ),
            # MSKU для модели-аналога 5002
            #   Аукцион в байбоксе выигрывает первый синий оффер по ставке
            MarketSku(
                title="msku_sausage_5002_1",
                hyperid=5002,
                sku=500002,
                hid=5000,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        title='blue_sausage_5002_1_1',
                        price=1000,
                        feedid=803,
                        waremd5="BLUE-500002-FEED-0001Q",
                        ts=50021,
                        fee=170,
                    ),
                    BlueOffer(
                        title='blue_sausage_5002_1_2',
                        price=1000,
                        feedid=803,
                        waremd5="BLUE-500002-FEED-0002Q",
                        ts=50022,
                        fee=150,
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            # DSBS офферы модели 5003
            # Цена одинаковая, ставки разные
            Offer(
                title="DSBS_sausage_5003_1",
                hid=5000,
                hyperid=5003,
                price=800,
                fee=140,
                fesh=801,
                cpa=Offer.CPA_REAL,
                waremd5='sausage5003_1_4TT-lbqQ',
                delivery_buckets=[1234],
            ),
            Offer(
                title="DSBS_sausage_5003_2",
                hid=5000,
                hyperid=5003,
                price=800,
                fee=120,
                fesh=801,
                cpa=Offer.CPA_REAL,
                waremd5='sausage5003_2_4TT-lbqQ',
                delivery_buckets=[1234],
            ),
            # Белые 3p офферы модели 5004
            # Цена одинаковая, ставки разные
            Offer(
                title="White_sausage_5004_1",
                hid=5000,
                hyperid=5004,
                price=900,
                fee=110,
                fesh=803,
                cpa=Offer.CPA_REAL,
                waremd5='sausage5004_1_4TT-lbqQ',
                delivery_buckets=[1234],
            ),
            Offer(
                title="White_sausage_5004_2",
                hid=5000,
                hyperid=5004,
                price=900,
                fee=90,
                fesh=803,
                cpa=Offer.CPA_REAL,
                waremd5='sausage5004_2_4TT-lbqQ',
                delivery_buckets=[1234],
            ),
        ]

        # Настраиваем скоры-релевантности ККМ
        cls.recommender.on_request_accessory_models(
            model_id=5000, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['5001:0.7', '5002:0.6', '5003:0.5', '5004:0.4']})

    def test_merchant_data_in_model_click_urls(self):
        """
        Проверяем, что в vendor-clicks-log в модельной клик-ссылке отправляется мерчовая информация
        Нужно для модели атрибуции в рекламе https://st.yandex-team.ru/MADV-1233
        """

        def check_for_hyperid_request(original_hyperid, rearr_flags_dict={}):
            """
            Для конкретного hyperid сформировать запрос, распарсить выдачу и
            сверить выдачу со сгенерированными модельными ссылками
            """

            request_raw = (
                'place=competitive_model_card&hyperid={}&debug=da&show-urls=productVendorBid&numdoc=16&'.format(
                    original_hyperid
                )
                + self._rearr_factors
                + dict_to_rearr(rearr_flags_dict)
            )
            response = self.report.request_json(request_raw)
            # Будем парсить информацию обо всех офферах в выдаче, сначала узнаем размер выдачи
            results_list_capture = Capture()
            self.assertFragmentIn(
                response,
                {
                    "results": NotEmpty(capture=results_list_capture),
                },
            )
            # Узнаём размер выдачи
            offers_in_response = len(results_list_capture.value)
            # Парсим характеристики офферов
            capture_list = [
                {
                    # У моделей shop_id их ДО пишется в original_shop_id
                    # В просто shop_id пишется невалидный id
                    "original_shop_id": Capture(),
                    "supplier_id": Capture(),
                    "feed_id": Capture(),
                    "offer_id": Capture(),
                    "shop_fee": Capture(),
                    "shop_fee_ab": Capture(),
                    "ware_md5": Capture(),
                }
                for _ in range(offers_in_response)
            ]
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": NotEmpty(capture=capture_dict["ware_md5"]),
                                        "supplier": {
                                            "id": NotEmpty(capture=capture_dict["supplier_id"]),
                                        },
                                        "shop": {
                                            "id": NotEmpty(capture=capture_dict["original_shop_id"]),
                                        },
                                        "debug": {
                                            "feed": {
                                                "id": NotEmpty(capture=capture_dict["feed_id"]),
                                                "offerId": NotEmpty(capture=capture_dict["offer_id"]),
                                            },
                                            "sale": {
                                                "shopFee": NotEmpty(capture=capture_dict["shop_fee"]),
                                                "brokeredFee": NotEmpty(capture=capture_dict["shop_fee_ab"]),
                                            },
                                        },
                                    },
                                ],
                            },
                        }
                        for capture_dict in capture_list
                    ],
                },
            )

            # Смотрим, что индекс для теста правильно сконфигурирован -
            #   есть мерчовые списанные ставки
            has_brokered_fees = False
            for capture_dict in capture_list:
                if capture_dict["shop_fee_ab"].value:
                    has_brokered_fees = True
                    break
            self.assertTrue(has_brokered_fees)  # Тест корректный, если есть списанные ставки

            # Проверяем, что требуемые характеристики офферов попали в модельную ссылку
            for capture_dict in capture_list:
                # Ссылки типа MODEL (url_type = 16) пишутся в shows-log
                # После клика эти ссылки попадают в vendor-clicks-log
                # При этом clickdaemon в LITE-тестах не будет отправлять эти клики в clicks-log
                self.show_log.expect(
                    url_type=UrlType.MODEL,
                    # Вытаскиваем value из capture-ов и передаем как kwargs
                    **{prop: capture_dict[prop].value for prop in capture_dict}
                ).once()

        check_for_hyperid_request(5000)

    @classmethod
    def prepare_disable_fashion_models(cls):

        cls.index.hypertree += [
            HyperCategory(
                hid=7877999,
                output_type=HyperCategoryType.GURU,
                name='Одежда, обувь и аксессуары',
                children=[
                    HyperCategory(
                        hid=7811873,
                        output_type=HyperCategoryType.GURU,
                        name='Женская одежда',
                        children=[
                            HyperCategory(hid=7811945, output_type=HyperCategoryType.GURU, name='Женские платья'),
                        ],
                    ),
                ],
            ),
        ]

        model_ids = list(range(6001, 6006))
        cls.index.models += [Model(hyperid=hyperid, ts=hyperid, hid=7877999, vbid=100) for hyperid in model_ids]
        cls.index.offers += [
            Offer(hyperid=hyperid, fesh=13, fee=10, price=1000, cpa=Offer.CPA_REAL) for hyperid in model_ids
        ]

        cls.recommender.on_request_accessory_models(
            model_id=6001, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['6004:0.1', '6002:0.2', '6003:0.3', '6005:0.44']})

        model_ids = list(range(6011, 6016))
        cls.index.models += [Model(hyperid=hyperid, ts=hyperid, hid=7811945, vbid=100) for hyperid in model_ids]
        cls.index.offers += [
            Offer(hyperid=hyperid, fesh=13, fee=10, price=1000, cpa=Offer.CPA_REAL) for hyperid in model_ids
        ]

        cls.recommender.on_request_accessory_models(
            model_id=6011, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['6014:0.1', '6012:0.2', '6013:0.3', '6015:0.44']})

    def test_disable_fashion_models(self):
        """
        Проверяем что для категории fashion ККМ выключена
        Если есть флаг market_competitive_model_card_disable_in_fashion=1
        """

        # модель из fashion с включенным флагом
        request = (
            'place=competitive_model_card&hyperid=6001&debug=1'
            + self._rearr_factors
            + ';market_competitive_model_card_disable_in_fashion=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(0)})

        # модель из fashion c выключенным флагом
        request = (
            'place=competitive_model_card&hyperid=6001&debug=1'
            + self._rearr_factors
            + ';market_competitive_model_card_disable_in_fashion=0'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(1)})

        # модель из дочерней категории fashion с включенным флагом
        request = (
            'place=competitive_model_card&hyperid=6011&debug=1'
            + self._rearr_factors
            + ';market_competitive_model_card_disable_in_fashion=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(0)})

        # модель из дочерней категории fashion c выключенным флагом
        request = (
            'place=competitive_model_card&hyperid=6011&debug=1'
            + self._rearr_factors
            + ';market_competitive_model_card_disable_in_fashion=0'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(1)})

        # не fashion модель с включенным флагом
        request = (
            'place=competitive_model_card&hyperid=1&debug=1'
            + self._rearr_factors
            + ';market_competitive_model_card_disable_in_fashion=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"results": ElementCount(1)})

    @classmethod
    def prepare_amnesty_with_price(cls):
        cls.index.models += [
            Model(hyperid=7001, ts=7001, hid=7000, vendor_id=1),
            Model(hyperid=7002, ts=7002, hid=7000, vendor_id=2, vbid=10),
            Model(hyperid=7003, ts=7003, hid=7000, vendor_id=3, vbid=10),
            Model(hyperid=7004, ts=7004, hid=7000, vendor_id=4, vbid=12),
        ]

        cls.index.offers += [
            Offer(hyperid=7002, fesh=13, fee=1200, price=2000),
            Offer(hyperid=7003, fesh=13, fee=900, price=1000),
            Offer(hyperid=7004, fesh=13, fee=0, price=1000),
        ]

        cls.recommender.on_request_accessory_models(
            model_id=7001, item_count=1000, version='SIBLINGS1_AUGMENTED'
        ).respond({'models': ['7004:0.2', '7002:0.2', '7003:0.2', '7005:0.2']})

    def test_amnesty_with_price(self):
        '''
        Проверяем, амнистию вендорской ставки c учетом цены оффера при расчете score (score = price*total_fee*analog_score)
        Итоговые очки 6002 модели получаются так:
            300 - вендорская ставка сконвертированная в fee
            1200 - fee на оффере
            0.2 - коэффициент похожести
            (300+1200)*0.2*2000 = 600000
        Итоговые очки 53 модели получаются так:
            600 - вендорская ставка сконвертированная в fee
            600 - fee на оффере
            0.2 - коэффициент похожести
            (900+600)*0.2*1000 = 300000
        Итоговое соотношение очков получается 3 к 1
        Поэтому вендорская ставка и brokeredFee умножаются на 1/2
        '''

        response = self.report.request_json(
            'place=competitive_model_card&hyperid=7001&debug=1&show-urls=productVendorBid'
            + self._rearr_factors
            + ";market_competitive_model_card_price_in_score=1;"
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 7002,
                        "offers": {'items': [{"debug": {"sale": {"shopFee": 1200, "brokeredFee": 600}}}]},
                        "debug": {"sale": {"vBid": 10, "vendorClickPrice": 5}},
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
