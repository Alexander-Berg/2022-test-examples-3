#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, HyperCategoryType, Model, Offer, RegionalModel, Shop
from core.testcase import main
from core.matcher import ElementCount
from core.bigb import ModelLastSeenEvent, MarketMetrikaInternalModelViewLastTime
from simple_testcase import SimpleTestCase
from core.dj import DjModel


class T(SimpleTestCase):
    """
    Набор тестов для перехода с icwhill на recommender и отказа от extractFeatures
    """

    @classmethod
    def prepare(cls):

        hyper_ids = list(range(100, 200))

        cls.index.models += [Model(hyperid=hyp_id, hid=500, ts=10**4 - hyp_id) for hyp_id in hyper_ids]

        # For category ordering checks
        cls.index.models += [
            Model(hyperid=501, hid=5001, ts=12),
            Model(hyperid=502, hid=5001, ts=11),
            Model(hyperid=503, hid=5001, ts=10),
            Model(hyperid=504, hid=5001, ts=9),
            Model(hyperid=511, hid=5101, ts=8),
            Model(hyperid=512, hid=5101, ts=7),
            Model(hyperid=513, hid=5101, ts=6),
            Model(hyperid=514, hid=5101, ts=5),
            Model(hyperid=521, hid=5201, ts=4),
            Model(hyperid=522, hid=5201, ts=3),
            Model(hyperid=523, hid=5201, ts=2),
            Model(hyperid=524, hid=5201, ts=1),
        ]

        cls.index.shops += [
            Shop(fesh=1, regions=[10, 20]),
        ]

        # Offers for model ids 100..180
        cls.index.offers += [Offer(hyperid=hyperid, fesh=1) for hyperid in hyper_ids[:80]]

        # Regional offers for models 150..160
        cls.index.regional_models += [
            RegionalModel(hyperid=hyperid, offers=1, rids=[0, 10, 20]) for hyperid in hyper_ids[50:60]
        ]
        cls.index.regional_models += [
            RegionalModel(hyperid=hyperid, offers=1, rids=[10]) for hyperid in hyper_ids[60:70]
        ]
        cls.index.regional_models += [
            RegionalModel(hyperid=hyperid, offers=1, rids=[20]) for hyperid in hyper_ids[70:80]
        ]

        # Models 180..190 don't have regional offers
        cls.index.regional_models += [RegionalModel(hyperid=hyperid, offers=0) for hyperid in hyper_ids[80:90]]

        # Models 190..200 don't have any offers

        # Models 500..525 are used for category ordering checks and do have offers
        cls.index.offers += [
            Offer(hyperid=hyperid) for hyperid in list(range(501, 505)) + list(range(511, 515)) + list(range(521, 525))
        ]

    @classmethod
    def prepare_total_renderable_recommender(cls):
        _reg_recommender_request(cls, "yandexuid:17001", list(range(100, 120)))

    def test_total_renderable_recommender(self):
        """Проверяется, что общее количество для показа = total"""

        request = 'place=products_by_history&yandexuid=17001&debug=1'
        '&rearr-factors=market_use_recommender=1;market_disable_dj_for_recent_findings=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"total": 20})

        response = self.report.request_json(request + '&numdoc=10')
        self.assertFragmentIn(response, {"total": 20})
        self.access_log.expect(total_renderable='20').times(2)

        """
        Проверка поля url_hash в show log
        """
        self.show_log_tskv.expect(url_hash=ElementCount(32))

    @classmethod
    def prepare_total_renderable_dj(cls):
        _reg_dj_request(cls, "17001", list(range(100, 120)))

    def test_total_renderable_dj(self):
        """Проверяется, что общее количество для показа = total"""

        request = 'place=products_by_history&yandexuid=17001&rearr-factors=market_use_recommender=1&debug=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"total": 20})

        response = self.report.request_json(request + '&numdoc=10')
        self.assertFragmentIn(response, {"total": 20})
        self.access_log.expect(total_renderable='20').times(2)

        """
        Проверка поля url_hash в show log
        """
        self.show_log_tskv.expect(url_hash=ElementCount(32))

    @classmethod
    def prepare_recommender_uid_priority(cls):
        _reg_recommender_request(cls, 'yandexuid:10001', list(range(100, 104)))
        _reg_recommender_request(cls, 'uuid:10101', list(range(104, 108)))
        _reg_recommender_request(cls, 'puid:10201', list(range(108, 112)))

    def test_recommender_uid_priority(self):
        # We check only one model from answer. Neither the number of models
        # nor their order we are not interested in.
        test_data = _make_test_data(
            [
                'yandexuid=10001&uuid=10101&puid=10201&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [108],
                'yandexuid=10001&uuid=10101&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [104],
                'yandexuid=10001&puid=&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [100],
            ]
        )
        self._check_report_answer(test_data)

    @classmethod
    def prepare_recommender_ok_answer(cls):
        _reg_recommender_request(cls, 'yandexuid:11001', list(range(100, 140)))
        _reg_recommender_request(cls, 'yandexuid:11101', [])

    def test_recommender_ok_answer(self):
        test_data = _make_test_data(
            [
                'yandexuid=11001&numdoc=40&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                list(range(100, 140)),
                'yandexuid=11001&numdoc=10&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                list(range(100, 110)),
                'yandexuid=11101&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [],
            ]
        )
        self._check_report_answer(test_data)

    @classmethod
    def prepare_dj_ok_answer(cls):

        _reg_dj_request(cls, '11001', list(range(100, 140)))
        _reg_dj_request(cls, '11101', [])

    def test_dj_ok_answer(self):
        test_data = _make_test_data(
            [
                'yandexuid=11001&numdoc=40',
                list(range(100, 140)),
                'yandexuid=11001&numdoc=10',
                list(range(100, 110)),
                'yandexuid=11101',
                [],
            ]
        )
        self._check_report_answer(test_data)

    @classmethod
    def prepare_recommender_no_analogs(cls):
        """
        Ответ Ихвиля на запрос для products_by_history без аналогов.
        В режиме без аналогов мы запрашиваем больше моделей.
        """
        _reg_recommender_request(cls, 'yandexuid:12002', [121, 122, 123, 125], 40)

    def test_recommender_no_analogs(self):
        test_data = _make_test_data(
            ['yandexuid=12002&rearr-factors=market_disable_dj_for_recent_findings%3D1', [121, 122, 123, 125]]
        )
        self._check_report_answer(test_data)

    @classmethod
    def prepare_dj_no_analogs(cls):
        _reg_dj_request(cls, '12002', [121, 122, 123, 125])

    def test_dj_no_analogs(self):
        test_data = _make_test_data(['yandexuid=12002', [121, 122, 123, 125]])
        self._check_report_answer(test_data)

    @classmethod
    def prepare_recommender_regional_offers(cls):
        _reg_recommender_request(cls, 'yandexuid:13001', [i for i in range(140, 200) if i % 10 < 4])

    def test_recommender_regional_offers(self):
        test_data = _make_test_data(
            [
                'yandexuid=13001&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [140, 141, 142, 143, 150, 151, 152, 153],
                'yandexuid=13001&rids=10&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [140, 141, 142, 143, 150, 151, 152, 153, 160, 161, 162, 163],
                'yandexuid=13001&rids=20&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [140, 141, 142, 143, 150, 151, 152, 153, 170, 171, 172, 173],
            ]
        )
        self._check_report_answer(test_data)

    @classmethod
    def prepare_dj_regional_offers(cls):
        _reg_dj_request(cls, '13001', [i for i in range(140, 200) if i % 10 < 4])

    def test_dj_regional_offers(self):
        test_data = _make_test_data(
            [
                'yandexuid=13001',
                [140, 141, 142, 143, 150, 151, 152, 153],
                'yandexuid=13001&rids=10',
                [140, 141, 142, 143, 150, 151, 152, 153, 160, 161, 162, 163],
                'yandexuid=13001&rids=20',
                [140, 141, 142, 143, 150, 151, 152, 153, 170, 171, 172, 173],
            ]
        )
        self._check_report_answer(test_data)

    def _request_report(self, request, place='products_by_history'):
        return self.report.request_json('place={}&rearr-factors=market_use_recommender=1&'.format(place) + request)

    def _check_report_answer(self, test_data, preserve_order=False):
        for request, models in test_data:
            response = self._request_report(request)
            fragment = [{"entity": "product", "id": model} for model in models]
            self.assertFragmentIn(response, fragment, preserve_order=preserve_order)

    @classmethod
    def prepare_category_first_ordering(cls):
        _reg_recommender_request(cls, 'yandexuid:10002', [501, 502, 511, 521])

    def test_category_first_ordering(self):
        """
        Проверка чередования результатов выдачи по категориям
        """
        test_data = _make_test_data(
            [
                'yandexuid=10002&rearr-factors=products_by_history_without_category_rotation%3D0'
                '%3Bmarket_disable_dj_for_recent_findings%3D1',
                [501, 511, 521, 502],
            ]
        )
        self._check_report_answer(test_data, preserve_order=True)

    @classmethod
    def prepare_no_category_first_ordering(cls):
        _reg_recommender_request(cls, 'yandexuid:13002', [501, 502, 511, 521])

    def test_no_category_first_ordering(self):
        """
        Проверка отсутствия чередования результатов выдачи по категориям
        """
        test_data = _make_test_data(
            [
                'yandexuid=13002&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [501, 502, 511, 521],
            ]
        )
        self._check_report_answer(test_data, preserve_order=True)

    @classmethod
    def prepare_no_category_first_ordering_dj(cls):
        _reg_dj_request(cls, '13002', [501, 502, 511, 521])

    def test_no_category_first_ordering_dj(self):
        """
        Проверка отсутствия чередования результатов выдачи по категориям
        """
        test_data = _make_test_data(
            [
                'yandexuid=13002',
                [501, 502, 511, 521],
            ]
        )
        self._check_report_answer(test_data, preserve_order=True)

    @classmethod
    def prepare_bigb_and_models_of_interest(cls):
        models = [str(m) for m in range(100, 110, 4)]
        timestamps = [str(200 - t) for t in range(100, 110, 4)]
        cls.recommender.on_request_models_of_interest(
            user_id="yandexuid:15001", item_count=40, with_timestamps=True, version=4
        ).respond({'models': models, 'timestamps': timestamps})
        model_views = [ModelLastSeenEvent(model_id=i, timestamp=200 - i) for i in range(108, 121, 4)]
        model_last_seen_counter = MarketMetrikaInternalModelViewLastTime(model_views)
        cls.bigb.on_request(yandexuid='15001', client='merch-machine').respond(counters=[model_last_seen_counter])
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='15001').respond([DjModel(id=modelid) for modelid in range(100, 113, 4)])

    def test_bigb_and_models_of_interest(self):
        """
        Проверка слияния данных из bigb и models_of_interest
        """
        test_data = _make_test_data(
            [
                'yandexuid=15001&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                list(range(100, 113, 4)),
                'yandexuid=15001',
                list(range(100, 113, 4)),
            ]
        )
        self._check_report_answer(test_data, preserve_order=True)

    @classmethod
    def prepare_personal_offers(cls):
        hids = list(range(6101, 6106))
        models = [hid * 10 + 1 for hid in hids]
        cls.index.hypertree += [
            HyperCategory(
                hid=6100,
                output_type=HyperCategoryType.GURU,
                children=[HyperCategory(hid=hid, output_type=HyperCategoryType.GURU) for hid in hids],
            ),
        ]
        cls.index.models += [Model(hyperid=hyperid, hid=hid) for hyperid, hid in zip(models, hids)]
        cls.index.offers += [Offer(hyperid=hyperid, fesh=1, discount=12) for hyperid in models]
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:9001').respond(
            {'models': map(str, models), 'timestamps': map(str, list(range(len(models), 0, -1)))}
        )
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='9001').respond([DjModel(id=str(modelid)) for modelid in models])

    def test_personal_offers_recommender(self):
        """Personal offers old mode and new mode with recommender"""

        """using old personal categories"""
        response = self.report.request_json(
            'place=personal_offers&hid=6100&yandexuid=9001&debug=1'
            '&rearr-factors=market_disable_dj_for_recent_findings=1'
        )
        self.assertFragmentIn(
            response,
            [{'entity': 'offer', 'model': {'id': hid * 10 + 1}} for hid in range(6101, 6106)],
            allow_different_len=False,
            preserve_order=False,
        )
        response = self.report.request_json(
            'place=personal_offers&hid=6100,-6101&yandexuid=9001&debug=1'
            '&rearr-factors=market_disable_dj_for_recent_findings=1'
        )
        self.assertFragmentIn(
            response,
            [{'entity': 'offer', 'model': {'id': hid * 10 + 1}} for hid in range(6102, 6106)],
            allow_different_len=False,
            preserve_order=False,
        )
        self.error_log.ignore('Personal category config is not available for user 9001')

        """using new personal categories"""
        response = self.report.request_json(
            'place=personal_offers&hid=6100&yandexuid=9001'
            '&rearr-factors=market_use_recommender=1;market_disable_dj_for_recent_findings=1&debug=1'
        )
        self.assertFragmentIn(
            response,
            [{'entity': 'offer', 'model': {'id': hid * 10 + 1}} for hid in range(6101, 6106)],
            allow_different_len=False,
            preserve_order=False,
        )
        self.error_log.ignore('Personal category config is not available for user 9001')
        response = self.report.request_json(
            'place=personal_offers&hid=6100,-6101&yandexuid=9001'
            '&rearr-factors=market_use_recommender=1;market_disable_dj_for_recent_findings=1&debug=1'
        )
        self.assertFragmentIn(
            response,
            [{'entity': 'offer', 'model': {'id': hid * 10 + 1}} for hid in range(6102, 6106)],
            allow_different_len=False,
            preserve_order=False,
        )
        self.error_log.ignore('Personal category config is not available for user 9001')

    def test_personal_offers_dj(self):
        """Personal offers old mode and new mode with recommender"""

        """using old personal categories"""
        response = self.report.request_json('place=personal_offers&hid=6100&yandexuid=9001&debug=1')
        self.assertFragmentIn(
            response,
            [{'entity': 'offer', 'model': {'id': hid * 10 + 1}} for hid in range(6101, 6106)],
            allow_different_len=False,
            preserve_order=False,
        )
        response = self.report.request_json('place=personal_offers&hid=6100,-6101&yandexuid=9001&debug=1')
        self.assertFragmentIn(
            response,
            [{'entity': 'offer', 'model': {'id': hid * 10 + 1}} for hid in range(6102, 6106)],
            allow_different_len=False,
            preserve_order=False,
        )
        self.error_log.ignore('Personal category config is not available for user 9001')

        """using new personal categories"""
        response = self.report.request_json(
            'place=personal_offers&hid=6100&yandexuid=9001' '&rearr-factors=market_use_recommender=1&debug=1'
        )
        self.assertFragmentIn(
            response,
            [{'entity': 'offer', 'model': {'id': hid * 10 + 1}} for hid in range(6101, 6106)],
            allow_different_len=False,
            preserve_order=False,
        )
        self.error_log.ignore('Personal category config is not available for user 9001')
        response = self.report.request_json(
            'place=personal_offers&hid=6100,-6101&yandexuid=9001' '&rearr-factors=market_use_recommender=1&debug=1'
        )
        self.assertFragmentIn(
            response,
            [{'entity': 'offer', 'model': {'id': hid * 10 + 1}} for hid in range(6102, 6106)],
            allow_different_len=False,
            preserve_order=False,
        )
        self.error_log.ignore('Personal category config is not available for user 9001')


def _reg_recommender_request(cls, user_id, models, item_count=40):
    cls.recommender.on_request_models_of_interest(
        user_id=user_id, item_count=item_count, with_timestamps=True, version=4
    ).respond({'models': map(str, models), 'timestamps': map(str, list(range(len(models), 0, -1)))})
    cls.bigb.on_request(yandexuid=user_id.replace('yandexuid:', ''), client='merch-machine').respond(counters=[])


def _reg_dj_request(cls, yandexuid, models):
    cls.settings.set_default_reqid = False
    cls.dj.on_request(yandexuid=yandexuid).respond([DjModel(id=model_id) for model_id in models])


def _make_test_data(data):
    return [data[idx : idx + 2] for idx in range(0, len(data), 2)]


if __name__ == '__main__':
    main()
