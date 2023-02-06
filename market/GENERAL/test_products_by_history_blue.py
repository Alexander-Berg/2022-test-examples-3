#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, HyperCategory, MarketSku, Model, YamarecCategoryPartition, YamarecPlace
from core.testcase import main
from core.bigb import (
    ModelLastSeenEvent,
    CategoryLastSeenEvent,
    CategoryLastOrderEvent,
    MarketModelLastTimeCounter,
    BeruModelViewLastTimeCounter,
    BeruAccessLogModelViewLastTimeCounter,
    BeruMobileAppModelViewLastTimeCounter,
    BeruMetrikaInternalModelViewLastTimeCounter,
    BeruPersHistoryModelViewLastTimeCounter,
    BeruPersHistoryCategoryViewLastTimeCounter,
    BeruModelOrderLastTimeCounter,
    BeruCategoryOrderLastTimeCounter,
)
from core.dj import DjModel
from simple_testcase import SimpleTestCase
import random
import time

NOW = int(time.time())
DAY = 86400


class T(SimpleTestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rgb_blue_is_cpa = True

        cls.index.hypertree += [
            HyperCategory(
                hid=612,
                children=[
                    HyperCategory(hid=611),
                ],
            ),
        ]
        cls.index.models += [
            Model(hyperid=6000, hid=600),
            Model(hyperid=6001, hid=600),
            Model(hyperid=6010, hid=601),
            Model(hyperid=6020, hid=602),
            Model(hyperid=6030, hid=603),
            Model(hyperid=6100, hid=610),
            Model(hyperid=6110, hid=611),
        ]
        cls.index.mskus += [
            MarketSku(hyperid=6000, sku=60000, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=6001, sku=60010, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=6010, sku=60100, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=6020, sku=60200, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=6030, sku=60300, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=6100, sku=61000, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=6110, sku=61100, blue_offers=[BlueOffer()]),
        ]

    @classmethod
    def prepare_my_recent_findings(cls):
        cls.bigb.on_request(yandexuid=700, client='merch-machine').respond(
            counters=[
                BeruPersHistoryModelViewLastTimeCounter(
                    [
                        ModelLastSeenEvent(model_id=6000, timestamp=NOW - 2 * DAY),
                        ModelLastSeenEvent(model_id=6001, timestamp=NOW - 2 * DAY),
                        ModelLastSeenEvent(model_id=6010, timestamp=NOW - 2 * DAY),
                    ]
                ),
                BeruPersHistoryCategoryViewLastTimeCounter(
                    [
                        CategoryLastSeenEvent(category_id=600, timestamp=NOW - 2 * DAY),
                        CategoryLastSeenEvent(category_id=601, timestamp=NOW - 2 * DAY),
                    ]
                ),
                BeruCategoryOrderLastTimeCounter(
                    [
                        CategoryLastOrderEvent(category_id=600, timestamp=NOW - 1 * DAY),
                    ]
                ),
            ]
        )
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid=700).respond(
            models=[
                DjModel(id=6010, title='model'),
            ]
        )

    def test_my_recent_findings(self):
        """
        Проверяем, что в "Моих недавних находках" отфильтровываются товары из берут только синюю историю просмотров из "чистого" счетчика.
        """
        for dj_flag in ['', '&rearr-factors=market_disable_dj_for_recent_findings%3D1']:
            for suffix in ['cpa=real', 'rgb=blue']:
                response = self.report.request_json(
                    'place=products_by_history&{}&history=blue&yandexuid=700{}'.format(suffix, dj_flag)
                )
                self.assertFragmentIn(
                    response,
                    {
                        'search': {
                            'total': 1,
                            'results': [
                                {'entity': 'product', 'id': 6010},
                            ],
                        }
                    },
                )

    @classmethod
    def prepare_my_recent_findings_source(cls):
        cls.bigb.on_request(yandexuid=701, client='merch-machine').respond(
            counters=[
                MarketModelLastTimeCounter(
                    [
                        ModelLastSeenEvent(model_id=6010, timestamp=NOW - 1),
                    ]
                ),
                BeruModelViewLastTimeCounter(
                    [
                        ModelLastSeenEvent(model_id=6020, timestamp=NOW - 2),
                    ]
                ),
                BeruPersHistoryModelViewLastTimeCounter(
                    [
                        ModelLastSeenEvent(model_id=6030, timestamp=NOW - 3),
                    ]
                ),
            ]
        )
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid=701).respond(
            models=[
                DjModel(id=6030, title='model'),
            ]
        )

    def test_my_recent_findings_source(self):
        """
        Проверяем, что "Мои недавние находки" берут только синюю историю просмотров из "чистого" счетчика.
        """
        for dj_flag in ['', '&rearr-factors=market_disable_dj_for_recent_findings%3D1']:
            for param in ['rgb=blue', 'cpa=real']:
                response = self.report.request_json(
                    'place=products_by_history&{}&history=blue&yandexuid=701{}'.format(param, dj_flag)
                )
                self.assertFragmentIn(
                    response,
                    {
                        'search': {
                            'total': 1,
                            'results': [
                                {'entity': 'product', 'id': 6030},
                            ],
                        }
                    },
                )

    @classmethod
    def prepare_my_recent_findings_time_slices(cls):
        cls.bigb.on_request(yandexuid=702, client='merch-machine').respond(
            counters=[
                BeruPersHistoryModelViewLastTimeCounter(
                    [
                        ModelLastSeenEvent(model_id=6000, timestamp=NOW - 10 * DAY),
                        ModelLastSeenEvent(model_id=6010, timestamp=NOW - 7 * DAY),
                        ModelLastSeenEvent(model_id=6020, timestamp=NOW - 4 * DAY),
                        ModelLastSeenEvent(model_id=6030, timestamp=NOW - 1 * DAY),
                    ]
                ),
                BeruPersHistoryCategoryViewLastTimeCounter(
                    [
                        CategoryLastSeenEvent(category_id=600, timestamp=NOW - 10 * DAY),
                        CategoryLastSeenEvent(category_id=601, timestamp=NOW - 7 * DAY),
                        CategoryLastSeenEvent(category_id=602, timestamp=NOW - 4 * DAY),
                        CategoryLastSeenEvent(category_id=603, timestamp=NOW - 1 * DAY),
                    ]
                ),
                BeruCategoryOrderLastTimeCounter(
                    [
                        CategoryLastOrderEvent(category_id=601, timestamp=NOW - 5 * DAY),
                        CategoryLastOrderEvent(category_id=602, timestamp=NOW - 5 * DAY),
                        CategoryLastOrderEvent(category_id=603, timestamp=NOW - 5 * DAY),
                    ]
                ),
            ]
        )
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid=702).respond(
            models=[
                DjModel(id=6030, title='model'),
            ]
        )

    def test_my_recent_findings_time_slices(self):
        """
        Проверяем фильтрацию по времени просмотра и покупки в "Моих недавних находках".
        """
        for dj_flag in ['', '&rearr-factors=market_disable_dj_for_recent_findings%3D1']:
            for param in ['rgb=blue', 'cpa=real']:
                request = 'place=products_by_history&{}&history=blue&yandexuid=702{}'.format(param, dj_flag)
                request += '&rearr-factors=recom_blue_history_view_history_duration={}'.format(9 * DAY)
                request += '&rearr-factors=recom_blue_history_order_history_duration={}'.format(6 * DAY)
                request += '&rearr-factors=recom_blue_history_order_lag_duration={}'.format(3 * DAY)
                response = self.report.request_json(request)
                self.assertFragmentIn(
                    response,
                    {
                        'search': {
                            'total': 1,
                            'results': [
                                {'entity': 'product', 'id': 6030},
                            ],
                        }
                    },
                )

    @classmethod
    def prepare_my_recent_findings_filter_commonly_purchased(cls):
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COMMONLY_PURCHASED_UNIVERSAL_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[YamarecCategoryPartition([610])],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.COMMONLY_PURCHASED_PERSONAL_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[YamarecCategoryPartition([612])],
            ),
        ]
        cls.bigb.on_request(yandexuid=703, client='merch-machine').respond(
            counters=[
                BeruPersHistoryModelViewLastTimeCounter(
                    [
                        ModelLastSeenEvent(model_id=6000, timestamp=NOW - 2 * DAY),
                        ModelLastSeenEvent(model_id=6010, timestamp=NOW - 2 * DAY),
                        ModelLastSeenEvent(model_id=6100, timestamp=NOW - 2 * DAY),
                        ModelLastSeenEvent(model_id=6110, timestamp=NOW - 2 * DAY),
                    ]
                ),
                BeruCategoryOrderLastTimeCounter(
                    [
                        CategoryLastOrderEvent(category_id=600, timestamp=NOW - 1 * DAY),
                        CategoryLastOrderEvent(category_id=610, timestamp=NOW - 1 * DAY),
                        CategoryLastOrderEvent(category_id=611, timestamp=NOW - 1 * DAY),
                    ]
                ),
            ]
        )
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid=703).respond(
            models=[DjModel(id=6010, title='model'), DjModel(id=6100, title='model'), DjModel(id=6110, title='model')]
        )

    def test_my_recent_findings_filter_commonly_purchased(self):
        """
        Проверяем, что товары из частотных категорий остаются в истории даже после покупки.
        """
        for dj_flag in ['', '&rearr-factors=market_disable_dj_for_recent_findings%3D1']:
            for param in ['rgb=blue', 'cpa=real']:
                request = 'place=products_by_history&{}&history=blue&yandexuid=703{}'.format(param, dj_flag)
                response = self.report.request_json(request)
                self.assertFragmentIn(
                    response,
                    {
                        'search': {
                            'total': 3,
                            'results': [
                                {'entity': 'product', 'id': 6010},
                                {'entity': 'product', 'id': 6100},
                                {'entity': 'product', 'id': 6110},
                            ],
                        }
                    },
                )

        """
        Проверяем работу rearr-флага `recom_blue_history_keep_commonly_purchased=0`, который
        отключает специальную обработку частотных категорий.
        """
        request += '&rearr-factors=recom_blue_history_keep_commonly_purchased=0'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {'entity': 'product', 'id': 6010},
                    ],
                }
            },
        )

    @classmethod
    def prepare_my_recent_findings_filter_whole_categories(cls):
        cls.bigb.on_request(yandexuid=704, client='merch-machine').respond(
            counters=[
                BeruPersHistoryModelViewLastTimeCounter(
                    [
                        ModelLastSeenEvent(model_id=6000, timestamp=NOW - 2 * DAY),
                        ModelLastSeenEvent(model_id=6001, timestamp=NOW - 2 * DAY),
                    ]
                ),
                BeruModelOrderLastTimeCounter(
                    [
                        ModelLastSeenEvent(model_id=6000, timestamp=NOW - 1 * DAY),
                    ]
                ),
                BeruCategoryOrderLastTimeCounter(
                    [
                        CategoryLastOrderEvent(category_id=600, timestamp=NOW - 1 * DAY),
                    ]
                ),
            ]
        )
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid=704).respond(
            models=[
                DjModel(id=6001, title='model'),
            ]
        )

    def test_my_recent_findings_filter_whole_categories(self):
        """
        Проверяем, отключение фильтрации категории целиком, если из нее был
        куплен хотя бы один товар.
        """
        for dj_flag in ['', '&rearr-factors=market_disable_dj_for_recent_findings%3D1']:
            for param in ['rgb=blue', 'cpa=real']:
                request = 'place=products_by_history&{}&history=blue&yandexuid=704{}'.format(param, dj_flag)
                request += '&rearr-factors=recom_blue_history_filter_whole_categories=0'
                response = self.report.request_json(request)
                self.assertFragmentIn(
                    response,
                    {
                        'search': {
                            'total': 1,
                            'results': [
                                {'entity': 'product', 'id': 6001},
                            ],
                        }
                    },
                )

    @classmethod
    def prepare_my_recent_findings_empty_history(cls):
        cls.bigb.on_request(yandexuid=705, client='merch-machine').respond()
        cls.bigb.on_request(yandexuid=706, client='merch-machine').respond(
            counters=[
                BeruPersHistoryModelViewLastTimeCounter(
                    [
                        ModelLastSeenEvent(model_id=6000, timestamp=NOW - 2 * DAY),
                    ]
                ),
                BeruCategoryOrderLastTimeCounter(
                    [
                        CategoryLastOrderEvent(category_id=600, timestamp=NOW - 1 * DAY),
                    ]
                ),
            ]
        )
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid=705).respond(models=[])
        cls.dj.on_request(yandexuid=706).respond(
            models=[
                DjModel(id=88888, title='model'),
            ]
        )

    def test_my_recent_findings_empty_history(self):
        """
        Пустая история, это не повод возвращать ошибку.
        """

        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json('place=products_by_history&{}&history=blue&yandexuid=705'.format(param))
            self.assertFragmentIn(response, {'search': {'total': 0}})

            response = self.report.request_json('place=products_by_history&{}&history=blue&yandexuid=706'.format(param))
            self.assertFragmentIn(response, {'search': {'total': 0}})

    @classmethod
    def prepare_my_recent_findings_sources(cls):
        cls.bigb.on_request(yandexuid=707, client='merch-machine').respond(
            counters=[
                BeruPersHistoryModelViewLastTimeCounter([ModelLastSeenEvent(model_id=6000, timestamp=NOW - 3 * DAY)]),
                BeruAccessLogModelViewLastTimeCounter([ModelLastSeenEvent(model_id=6010, timestamp=NOW - 3 * DAY)]),
                BeruMobileAppModelViewLastTimeCounter([ModelLastSeenEvent(model_id=6020, timestamp=NOW - 3 * DAY)]),
                BeruMetrikaInternalModelViewLastTimeCounter(
                    [ModelLastSeenEvent(model_id=6030, timestamp=NOW - 3 * DAY)]
                ),
            ]
        )

    def test_my_recent_findings_sources(self):
        """
        Перебираем все возможные комбинации флагов включающих
        или выключающих источники истории для плейса.
        """

        SOURCE_FLAGS = [
            'recom_blue_history_source_pers_history',
            'recom_blue_history_source_access_log',
            'recom_blue_history_source_app_metrika',
            'recom_blue_history_source_metrika_internal',
        ]

        for combination in range(1, 2 ** len(SOURCE_FLAGS)):
            enabled = [bool(combination & 2**i) for i in range(len(SOURCE_FLAGS))]
            for param in ['rgb=blue', 'cpa=real']:
                response = self.report.request_json(
                    'place=products_by_history&{}&history=blue&yandexuid=707&rearr-factors='.format(param)
                    + ';'.join(map('{}={}'.format, SOURCE_FLAGS, enabled))
                    + ';market_disable_dj_for_recent_findings=1'
                )
                self.assertFragmentIn(
                    response,
                    {
                        'search': {
                            'results': [
                                {'entity': 'product', 'id': 6000 + 10 * i}
                                for i in range(len(SOURCE_FLAGS))
                                if enabled[i]
                            ]
                        }
                    },
                    allow_different_len=False,
                )

    @classmethod
    def prepare_my_recent_findings_long_history(cls):

        models = list(range(6200, 6230))
        random.shuffle(models)

        cls.index.models += [Model(hyperid=model, hid=model / 10) for model in models]
        cls.index.mskus += [MarketSku(hyperid=model, sku=model * 10, blue_offers=[BlueOffer()]) for model in models]
        cls.bigb.on_request(yandexuid=708, client='merch-machine').respond(
            counters=[
                BeruPersHistoryModelViewLastTimeCounter(
                    [ModelLastSeenEvent(model_id=model, timestamp=NOW - 1 * DAY - model) for model in models]
                )
            ]
        )
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid=708).respond(
            models=[DjModel(id=model, title='model') for model in range(6200, 6230)]
        )

    def test_my_recent_findings_long_history(self):
        """
        Смотрим как себя ведет плейс в случае длинного списка истории у пользователя.
        """

        models = list(range(6200, 6220))

        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=products_by_history&{}&history=blue&yandexuid=708&numdoc={}'.format(param, len(models))
            )
            self.assertFragmentIn(
                response,
                {'search': {'total': 30, 'results': [{'entity': 'product', 'id': model} for model in models]}},
                preserve_order=True,
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
