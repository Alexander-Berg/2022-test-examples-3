#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, MarketSku, Model, Offer, RegionalModel, Shop
from core.testcase import main
from core.matcher import ElementCount, Absent
from core.bigb import (
    ModelLastSeenEvent,
    SkuPurchaseEvent,
    MarketModelLastTimeCounter,
    MarketMetrikaExternalModelViewLastTime,
    MarketMetrikaInternalModelViewLastTime,
    BeruModelViewLastTimeCounter,
    BeruMetrikaInternalModelViewLastTimeCounter,
    BeruSkuOrderCountCounter,
)
from core.dj import DjModel
from simple_testcase import SimpleTestCase
import random
import time

NOW = int(time.time())
DAY = 86400

MARKET_COUNTER_MODELS = list(range(6200, 6204))
BERU_COUNTER_MODELS = list(range(6204, 6208))
BERU_INTERNAL_COUNTER_MODELS = list(range(6208, 6212))
METRIKA_EXTERNAL_COUNTER_MODELS = list(range(6212, 6216))
MARKET_INTERNAL_COUNTER_MODELS = list(range(6216, 6220))
MODELS = (
    MARKET_COUNTER_MODELS
    + BERU_COUNTER_MODELS
    + BERU_INTERNAL_COUNTER_MODELS
    + METRIKA_EXTERNAL_COUNTER_MODELS
    + MARKET_INTERNAL_COUNTER_MODELS
)


class T(SimpleTestCase):
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
    def prepare_total_renderable(cls):
        _reg_ichwill_request(cls, "yandexuid:17001", list(range(100, 120)))
        dj_models = [DjModel(id=str(modelid)) for modelid in range(100, 120)]
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='17002').respond(dj_models)

    def test_total_renderable(self):
        """Проверяется, что общее количество для показа = total"""
        params = ['yandexuid=17001&rearr-factors=market_disable_dj_for_recent_findings%3D1', 'yandexuid=17002']
        for param in params:
            request = 'place=products_by_history&{}'.format(param)
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {"total": 20})

            response = self.report.request_json(request + '&numdoc=10')
            self.assertFragmentIn(response, {"total": 20})

        self.access_log.expect(total_renderable='20').times(4)

        """
        Проверка поля url_hash в show log
        """
        self.show_log_tskv.expect(url_hash=ElementCount(32))

        """
        Проверка поля url_hash в show log
        """
        self.show_log_tskv.expect(url_hash=ElementCount(32))

    @classmethod
    def prepare_ichwill_uid_priority(cls):
        _reg_ichwill_request(cls, 'yandexuid:10001', list(range(100, 104)))
        _reg_ichwill_request(cls, 'uuid:10101', list(range(104, 108)))
        _reg_ichwill_request(cls, 'puid:10201', list(range(108, 112)))

    def test_ichwill_uid_priority(self):
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
    def prepare_ichwill_ok_answer(cls):
        _reg_ichwill_request(cls, 'yandexuid:11001', list(range(100, 140)))
        _reg_ichwill_request(cls, 'yandexuid:11101', [])
        dj_models = [DjModel(id=str(modelid)) for modelid in range(100, 140)]
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='11002').respond(dj_models)
        cls.dj.on_request(yandexuid='11102').respond([DjModel(25000)])

    def test_ichwill_ok_answer(self):
        test_data = _make_test_data(
            [
                'yandexuid=11001&numdoc=40&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                list(range(100, 140)),
                'yandexuid=11001&numdoc=10&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                list(range(100, 110)),
                'yandexuid=11101&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [],
                'yandexuid=11002&numdoc=40',
                list(range(100, 140)),
                'yandexuid=11002&numdoc=10',
                list(range(100, 110)),
                'yandexuid=11102',
                [],
            ]
        )
        self._check_report_answer(test_data)

    @classmethod
    def prepare_ichwill_no_analogs(cls):
        """
        Ответ Ихвиля на запрос для products_by_history без аналогов.
        В режиме без аналогов мы запрашиваем больше моделей.
        """
        _reg_ichwill_request(cls, 'yandexuid:12002', [121, 122, 123, 125], 40)
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='12003').respond(
            [DjModel(id='121'), DjModel(id='122'), DjModel(id='123'), DjModel(id='125')]
        )

    def test_ichwill_no_analogs(self):
        test_data = _make_test_data(
            [
                'yandexuid=12002&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [121, 122, 123, 125],
                'yandexuid=12003',
                [121, 122, 123, 125],
            ]
        )
        self._check_report_answer(test_data)

    @classmethod
    def prepare_ichwill_regional_offers(cls):
        _reg_ichwill_request(cls, 'yandexuid:13001', [i for i in range(140, 200) if i % 10 < 4])
        dj_models = [DjModel(id=str(i)) for i in range(140, 200) if i % 10 < 4]
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='13002').respond(dj_models)

    def test_ichwill_regional_offers(self):
        test_data = _make_test_data(
            [
                'yandexuid=13001&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [140, 141, 142, 143, 150, 151, 152, 153],
                'yandexuid=13001&rids=10&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [140, 141, 142, 143, 150, 151, 152, 153, 160, 161, 162, 163],
                'yandexuid=13001&rids=20&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [140, 141, 142, 143, 150, 151, 152, 153, 170, 171, 172, 173],
                'yandexuid=13002',
                [140, 141, 142, 143, 150, 151, 152, 153],
                'yandexuid=13002&rids=10',
                [140, 141, 142, 143, 150, 151, 152, 153, 160, 161, 162, 163],
                'yandexuid=13002&rids=20',
                [140, 141, 142, 143, 150, 151, 152, 153, 170, 171, 172, 173],
            ]
        )
        self._check_report_answer(test_data)

    @classmethod
    def prepare_ichwill_bad_response(cls):
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:14001', item_count=40, with_timestamps=True, version=4
        ).respond('{"models": ["100", "101", "zzz", "102", "103"], "timestamps": ["1", "2", "3", "4", "5"]}')
        cls.bigb.on_request(yandexuid="14001", client='merch-machine').respond(counters=[])

        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='14002').respond([DjModel(id="100"), DjModel(id="zzz")])

        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:14101', item_count=40, with_timestamps=True, version=4
        ).respond('{"models": "100", "timestamps": "100"}')
        cls.bigb.on_request(yandexuid="14101", client='merch-machine').respond(counters=[])

        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:14201', item_count=40, with_timestamps=True, version=4
        ).respond('zzz')
        cls.bigb.on_request(yandexuid="14201", client='merch-machine').respond(counters=[])

        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:14301', item_count=40, with_timestamps=True, version=4
        ).respond('{}')
        cls.bigb.on_request(yandexuid="14301", client='merch-machine').respond(counters=[])

        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:14401', item_count=40, with_timestamps=True, version=4
        ).respond('{"models": "100"}')
        cls.bigb.on_request(yandexuid="14401", client='merch-machine').respond(counters=[])

        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:14501', item_count=40, with_timestamps=True, version=4
        ).respond('{"models": ["100", "200"], "timestamps": ["100"]}')
        cls.bigb.on_request(yandexuid="14501", client='merch-machine').respond(counters=[])

        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:14601', item_count=40, with_timestamps=True, version=4
        )
        cls.bigb.on_request(yandexuid="14601", client='merch-machine').respond(counters=[])

    def test_ichwill_bad_response(self):
        test_data = _make_test_data(
            [
                'yandexuid=14001&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [],
                'yandexuid=14002',
                [],
            ]
        )
        self._check_report_answer(test_data)
        self.error_log.expect('Problem parsing integer value in').once()
        self.error_log.expect('Wrong item at position 1').once()

        test_data = _make_test_data(['yandexuid=14101&rearr-factors=market_disable_dj_for_recent_findings%3D1', []])
        self._check_report_answer(test_data)

        test_data = _make_test_data(['yandexuid=14201&rearr-factors=market_disable_dj_for_recent_findings%3D1', []])
        self._check_report_answer(test_data)
        self.error_log.expect('Cannot parse JSON string').once()

        test_data = _make_test_data(['yandexuid=14301&rearr-factors=market_disable_dj_for_recent_findings%3D1', []])
        self._check_report_answer(test_data)
        self.error_log.expect("Param 'models' not found in JSON object").once()

        test_data = _make_test_data(['yandexuid=14401&rearr-factors=market_disable_dj_for_recent_findings%3D1', []])
        self._check_report_answer(test_data)
        self.error_log.expect("Param 'timestamps' not found in JSON object").once()

        test_data = _make_test_data(['yandexuid=14501&rearr-factors=market_disable_dj_for_recent_findings%3D1', []])
        self._check_report_answer(test_data)
        self.error_log.expect("Length of params 'models' (").once()

        test_data = _make_test_data(['yandexuid=14601&rearr-factors=market_disable_dj_for_recent_findings%3D1', []])
        self._check_report_answer(test_data)
        self.error_log.expect("Request to RECOMMENDER_MODELS_OF_INTEREST failed").once()

    @classmethod
    def prepare_category_first_ordering(cls):
        _reg_ichwill_request(cls, 'yandexuid:10002', [501, 502, 511, 521])

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
        _reg_ichwill_request(cls, 'yandexuid:13002', [501, 502, 511, 521])
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='13003').respond(
            [DjModel(id='501'), DjModel(id='502'), DjModel(id='511'), DjModel(id='521')]
        )

    def test_no_category_first_ordering(self):
        """
        Проверка отсутствия чередования результатов выдачи по категориям
        """
        test_data = _make_test_data(
            [
                'yandexuid=13002&rearr-factors=market_disable_dj_for_recent_findings%3D1',
                [501, 502, 511, 521],
                'yandexuid=13003',
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

    def test_bigb_and_models_of_interest(self):
        """
        Проверка слияния данных из bigb и models_of_interest
        """
        test_data = _make_test_data(
            ['yandexuid=15001&rearr-factors=market_disable_dj_for_recent_findings%3D1', list(range(100, 113, 4))]
        )
        self._check_report_answer(test_data, preserve_order=True)

    @classmethod
    def prepare_sku_enrichment_from_bigb(cls):
        cls.index.mskus += [
            MarketSku(
                hid=500,
                hyperid=154,
                sku=170,
                title="MSKU-170",
                blue_offers=[BlueOffer(ts=10, price=170, fesh=1)],
            ),
            MarketSku(
                hid=500,
                hyperid=154,
                sku=171,
                title="MSKU-171",
                blue_offers=[BlueOffer(ts=10, price=171, fesh=1)],
            ),
        ]

        sku_purchases_counter = BeruSkuOrderCountCounter(
            [
                SkuPurchaseEvent(sku_id=70, count=1),
                SkuPurchaseEvent(sku_id=171, count=1),
            ]
        )
        cls.bigb.on_request(yandexuid='15002', client='merch-machine').respond(counters=[sku_purchases_counter])

        cls.recommender.on_request_models_of_interest(
            user_id="yandexuid:15002", item_count=40, with_timestamps=True, version=4
        ).respond({'models': ['150', '154', '156', '158'], 'timestamps': ['80', '84', '86', '88']})

    def test_sku_enrichment_from_bigb(self):
        """
        Проверка, что вывод обогащается данными из bigb о ранее купленных ску
        Модель 150 не имеет никаких ску вообще
        Модель 154 имеет два ску, но только 171й указан в счетчике
        """
        response = self.report.request_json(
            'place=products_by_history&yandexuid=15002&rearr-factors=market_disable_dj_for_recent_findings=1'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 4,
                    "results": [
                        {
                            "type": "model",
                            "id": 150,
                            "offers": {"items": [{"marketSku": Absent(), "sku": Absent(), "titles": {"raw": ""}}]},
                            "titles": {"raw": "HYPERID-150"},
                        },
                        {
                            "type": "model",
                            "id": 154,
                            "offers": {"items": [{"marketSku": "171", "sku": "171", "titles": {"raw": "MSKU-171"}}]},
                            "titles": {"raw": "HYPERID-154"},
                        },
                    ],
                }
            },
        )

    @classmethod
    def prepare_clean_history(cls):
        cls.index.models += [Model(hyperid=hyperid, hid=620) for hyperid in MODELS]
        cls.index.offers += [Offer(hyperid=hyperid) for hyperid in MODELS]
        cls.bigb.on_request(yandexuid=704, client='merch-machine').respond(
            counters=[
                MarketModelLastTimeCounter(
                    [ModelLastSeenEvent(model_id=hyperid, timestamp=NOW - 1 * DAY) for hyperid in MARKET_COUNTER_MODELS]
                ),
                BeruModelViewLastTimeCounter(
                    [ModelLastSeenEvent(model_id=hyperid, timestamp=NOW - 1 * DAY) for hyperid in BERU_COUNTER_MODELS]
                ),
                BeruMetrikaInternalModelViewLastTimeCounter(
                    [
                        ModelLastSeenEvent(model_id=hyperid, timestamp=NOW - 1 * DAY)
                        for hyperid in BERU_INTERNAL_COUNTER_MODELS
                    ]
                ),
                MarketMetrikaExternalModelViewLastTime(
                    [
                        ModelLastSeenEvent(model_id=hyperid, timestamp=NOW - 1 * DAY)
                        for hyperid in METRIKA_EXTERNAL_COUNTER_MODELS
                    ]
                ),
                MarketMetrikaInternalModelViewLastTime(
                    [
                        ModelLastSeenEvent(model_id=hyperid, timestamp=NOW - 1 * DAY)
                        for hyperid in MARKET_INTERNAL_COUNTER_MODELS
                    ]
                ),
            ]
        )
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:704', item_count=40, with_timestamps=True, version=4
        ).respond('{"models": [], "timestamps": []}')
        dj_models = [DjModel(id=str(hyperid)) for hyperid in MARKET_COUNTER_MODELS]
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='705').respond(dj_models)

    def test_clean_history(self):
        """
        При запросе чистой истории смотрим на правильные счетчики.
        """
        request = (
            'place=products_by_history&yandexuid=704&rearr-factors='
            + 'recom_history_from_bigb_only={bigb_only};'
            + 'recom_history_from_bigb_include_beru={include_beru};'
            + 'products_by_history_clean_history={clean_history};'
            + 'market_disable_dj_for_recent_findings=1'
        )

        response = self.report.request_json(request.format(bigb_only=0, include_beru=0, clean_history=0))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'results': ([{'entity': 'product', 'id': hyperid} for hyperid in MARKET_COUNTER_MODELS]),
                }
            },
        )
        response = self.report.request_json(request.format(bigb_only=0, include_beru=0, clean_history=1))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 8,
                    'results': (
                        [{'entity': 'product', 'id': hyperid} for hyperid in METRIKA_EXTERNAL_COUNTER_MODELS]
                        + [{'entity': 'product', 'id': hyperid} for hyperid in MARKET_INTERNAL_COUNTER_MODELS]
                    ),
                }
            },
        )
        response = self.report.request_json(request.format(bigb_only=1, include_beru=0, clean_history=0))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'results': ([{'entity': 'product', 'id': hyperid} for hyperid in MARKET_COUNTER_MODELS]),
                }
            },
        )
        response = self.report.request_json(request.format(bigb_only=1, include_beru=0, clean_history=1))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 8,
                    'results': (
                        [{'entity': 'product', 'id': hyperid} for hyperid in METRIKA_EXTERNAL_COUNTER_MODELS]
                        + [{'entity': 'product', 'id': hyperid} for hyperid in MARKET_INTERNAL_COUNTER_MODELS]
                    ),
                }
            },
        )
        response = self.report.request_json(request.format(bigb_only=1, include_beru=1, clean_history=0))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 8,
                    'results': (
                        [{'entity': 'product', 'id': hyperid} for hyperid in MARKET_COUNTER_MODELS]
                        + [{'entity': 'product', 'id': hyperid} for hyperid in BERU_COUNTER_MODELS]
                    ),
                }
            },
        )
        response = self.report.request_json(request.format(bigb_only=1, include_beru=1, clean_history=1))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 12,
                    'results': (
                        [{'entity': 'product', 'id': hyperid} for hyperid in BERU_INTERNAL_COUNTER_MODELS]
                        + [{'entity': 'product', 'id': hyperid} for hyperid in METRIKA_EXTERNAL_COUNTER_MODELS]
                        + [{'entity': 'product', 'id': hyperid} for hyperid in MARKET_INTERNAL_COUNTER_MODELS]
                    ),
                }
            },
        )
        request = (
            'place=products_by_history&yandexuid=705&rearr-factors='
            + 'recom_history_from_bigb_only={bigb_only};'
            + 'recom_history_from_bigb_include_beru={include_beru};'
            + 'products_by_history_clean_history={clean_history}'
        )
        response = self.report.request_json(request.format(bigb_only=0, include_beru=0, clean_history=0))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'results': ([{'entity': 'product', 'id': hyperid} for hyperid in MARKET_COUNTER_MODELS]),
                }
            },
        )

    @classmethod
    def prepare_products_by_history_long_history(cls):

        models = list(range(6300, 6330))
        random.shuffle(models)

        cls.index.models += [Model(hyperid=model, hid=model / 10) for model in models]
        cls.index.offers += [Offer(hyperid=model) for model in models]
        cls.recommender.on_request_models_of_interest(
            user_id="yandexuid:705", item_count=40, with_timestamps=True, version=4
        ).respond(
            {
                'models': [str(m) for m in models],
                'timestamps': [str(NOW - 1 * DAY - m) for m in models],
            }
        )
        cls.bigb.on_request(yandexuid=705, client='merch-machine').respond(counters=[])

    def test_products_by_history_long_history(self):
        """
        Смотрим как себя ведет плейс в случае длинного списка истории у пользователя.
        """

        # CRAP: number of results should be divisible by 4 if all models are available
        # CRAP: this place ignores `numdoc`. To test it, lower upper bound of expected models range by 8.
        models = list(range(6300, 6328))

        response = self.report.request_json(
            'place=products_by_history&yandexuid=705&numdoc={}'
            '&rearr-factors=market_disable_dj_for_recent_findings%3D1'.format(len(models))
        )
        self.assertFragmentIn(
            response,
            {'search': {'total': len(models), 'results': [{'entity': 'product', 'id': model} for model in models]}},
        )

    def _request_report(self, request, place='products_by_history'):
        return self.report.request_json('place={}&'.format(place) + request)

    def _check_report_answer(self, test_data, preserve_order=False):
        for request, models in test_data:
            response = self._request_report(request)
            fragment = [{"entity": "product", "id": model} for model in models]
            self.assertFragmentIn(response, fragment, preserve_order=preserve_order)


def _reg_ichwill_request(cls, user_id, models, item_count=40):
    cls.recommender.on_request_models_of_interest(
        user_id=user_id, item_count=item_count, with_timestamps=True, version=4
    ).respond({'models': list(map(str, models)), 'timestamps': list(map(str, list(range(len(models), 0, -1))))})
    cls.bigb.on_request(yandexuid=user_id.replace('yandexuid:', ''), client='merch-machine').respond(counters=[])


def _make_test_data(data):
    return [data[idx : idx + 2] for idx in range(0, len(data), 2)]


if __name__ == '__main__':
    main()
