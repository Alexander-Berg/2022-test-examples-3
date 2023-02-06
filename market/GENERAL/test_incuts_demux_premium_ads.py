#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Model,
    Offer,
    Opinion,
    Shop,
    MnPlace,
)
from core.testcase import TestCase, main
from core.matcher import ElementCount, NotEmpty, Contains, Capture
from core.blender_bundles import get_supported_incuts_cgi, create_blender_bundles


# Демультиплексор (demux, demultiplexer) - алгоритм набора врезок на базе ответа cpa_shop_incut
# https://st.yandex-team.ru/MARKETMPE-1036


def get_rearrs_dict():
    """
    Возвращает стандартный для этого набора тестов словарь rearr-флагов
    """
    return {
        "market_blender_cpa_shop_incut_enabled": 1,
        "market_blender_use_bundles_config": 1,
        "market_cpa_shop_incut_model_rating_to_analogs_fields": 1,  # Логируем рейтинги моделей во врезках
        # По умолчанию разрешаем рейтинговую врезку
        "market_premium_ads_gallery_never_create_incut_with_high_model_rating": 0,
        "market_blender_media_adv_incut_enabled": 0,  # Выключаем МПФ
        "market_cpa_shop_incut_premium_ads_use_demux": 1,  # Включаем логику демультиплексора
        "market_cpa_shop_incut_premium_ads_demux_random_seed": 123,  # Задаём seed рандома для воспроизводимости
        # Убираем доп. ограничения на дубли, чтобы было проще тестировать
        "market_cpa_shop_incut_premium_ads_demux_each_offer_duplicates_max_count": 0,
        "market_cpa_shop_incut_premium_ads_demux_banned_positions_for_duplicates": 0,
        "market_cpa_shop_incut_premium_ads_demux_prior_check_mult_coef": 1,
        # Включаем второй проход
        "market_cpa_shop_incut_premium_ads_demux_no_second_pass": 0,
        # Включаем по умолчанию порядок врезок - премиальная, потом рейтинговая
        "market_cpa_shop_incut_premium_ads_incuts_order": "0:1",
    }


def get_cgi_params_dict():
    """
    Возвращает стандартный для этого набора тестов словарь cgi-параметров
    """
    return {
        "place": "blender",
        "text": "торт",
        "client": "frontend",
        "platform": "desktop",
        'supported-incuts': get_supported_incuts_cgi(),
        'pp': 7,
        "debug": "da",
    }


class BlenderConstCpaShopIncut:
    BUNDLE_DEFAULT_THEN_HIGH_RATING = '''
{
    "incut_places": ["Search"],
    "incut_positions": [1, 4, 7],
    "incut_viewtypes": ["Gallery"],
    "incut_ids": ["default", "cpa_shop_incut_filter_by_model_rating"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.65
        },
        {
            "incut_place": "Search",
            "row_position": 4,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.75
        },
        {
            "incut_place": "Search",
            "row_position": 4,
            "incut_viewtype": "Gallery",
            "incut_id": "cpa_shop_incut_filter_by_model_rating",
            "score": 0.74
        },
        {
            "incut_place": "Search",
            "row_position": 7,
            "incut_viewtype": "Gallery",
            "incut_id": "cpa_shop_incut_filter_by_model_rating",
            "score": 0.75
        }
    ],
    "calculator_type": "ConstPosition"
}
'''
    BUNDLE_HIGH_RATING_THEN_DEFAULT = '''
{
    "incut_places": ["Search"],
    "incut_positions": [1, 4, 7],
    "incut_viewtypes": ["Gallery"],
    "incut_ids": ["default", "cpa_shop_incut_filter_by_model_rating"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "cpa_shop_incut_filter_by_model_rating",
            "score": 0.65
        },
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.64
        },
        {
            "incut_place": "Search",
            "row_position": 4,
            "incut_viewtype": "Gallery",
            "incut_id": "cpa_shop_incut_filter_by_model_rating",
            "score": 0.75
        },
        {
            "incut_place": "Search",
            "row_position": 4,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.74
        },
        {
            "incut_place": "Search",
            "row_position": 7,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.75
        }
    ],
    "calculator_type": "ConstPosition"
}
'''


class BlenderBundlesConfig:
    BUNDLES_CONFIG = """
{
    "INCLID_PREMIUM_ADS" : {
        "search_type == text && client == frontend && platform == desktop" : {
            "bundle_name": "const_premium_ads_default_then_high_rating_incut.json"
        },
        "client == frontend && platform == touch" : {
            "bundle_name": "const_premium_ads_default_then_high_rating_incut.json"
        },
        "search_type == textless && client == frontend && platform == desktop" : {
            "bundle_name": "const_premium_ads_default_then_high_rating_incut.json"
        }
    }
}
"""


class T(TestCase):
    @staticmethod
    def get_request(params, rearr):
        def dict_to_str(data, separator):
            return str(separator).join("{}={}".format(str(k), str(v)) for (k, v) in data.iteritems())

        return "{}&rearr-factors={}".format(dict_to_str(params, '&'), dict_to_str(rearr, ';'))

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    @classmethod
    def prepare_blender_bundles_config(cls):
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            BlenderBundlesConfig.BUNDLES_CONFIG,
            {
                "const_premium_ads_default_then_high_rating_incut.json": BlenderConstCpaShopIncut.BUNDLE_DEFAULT_THEN_HIGH_RATING,
                "const_premium_ads_high_rating_then_default_incut.json": BlenderConstCpaShopIncut.BUNDLE_HIGH_RATING_THEN_DEFAULT,
            },
        )

    @classmethod
    def prepare_incut_high_model_rating(cls):
        """
        Создаём офферы, которые будут набираться во врезки по текстовому запросу "торт"
        Тут много тестов завязано на кол-во офферов и их порядок, поэтому кол-во и порядок офферов для запроса "торт" лучше не менять
        """
        cls.index.models += [  # Модели с низким рейтингом (ниже 4)
            Model(
                hid=266,
                hyperid=266 + i,
                title="Торт с низким рейтингом {}".format(i),
                ts=200020 + i,
                opinion=Opinion(
                    total_count=100, positive_count=95, rating=3.5, precise_rating=3.58, rating_count=200, reviews=5
                ),
                vbid=10,
            )
            for i in range(1, 10)
        ]
        cls.index.models += [  # Модели с хорошим рейтингом (4.2)
            Model(
                hid=266,
                hyperid=266 + i,
                title="Торт с хорошим рейтингом {}".format(i),
                ts=200020 + i,
                opinion=Opinion(
                    total_count=100, positive_count=95, rating=4.2, precise_rating=4.23, rating_count=200, reviews=5
                ),
                vbid=10,
            )
            for i in range(10, 13)
        ]
        cls.index.models += [  # Модели с отличным рейтингом (4.8)
            Model(
                hid=266,
                hyperid=266 + i,
                title="Торт с отличным рейтингом {}".format(i),
                ts=200020 + i,
                opinion=Opinion(
                    total_count=100, positive_count=95, rating=4.8, precise_rating=4.81, rating_count=200, reviews=5
                ),
                vbid=10,
            )
            for i in range(13, 19)
        ]

        cls.index.shops += [
            Shop(
                fesh=266,
                priority_region=213,
                cpa=Shop.CPA_REAL,
                name='CPA Shop 266',
            ),
            Shop(
                fesh=267,
                priority_region=213,
                cpa=Shop.CPA_REAL,
                name='CPA Shop 267',
            ),
        ]

        cls.index.offers += [  # офферы с высокими ставками для моделей низкого рейтинга
            Offer(
                fesh=266,
                hyperid=266 + i,
                hid=266,
                fee=500 + i,
                ts=200120 + i * 10,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт {}".format(i),
                waremd5='off-lowrate-0{}-TT-111Q'.format(i),
            )
            for i in range(1, 4)
        ]
        cls.index.offers += [
            # Офферы с хорошим рейтингом
            Offer(
                fesh=266,
                hyperid=276,
                hid=266,
                fee=200,
                ts=200232,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт 10",
                waremd5="off-finerate-10-Hbid1Q",
            ),
            Offer(
                fesh=267,
                hyperid=276,
                hid=266,
                fee=180,
                ts=200232,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт 10",
                waremd5="off-finerate-10-Lbid1Q",
            ),
            Offer(
                # Оффер-победитель этой модели
                fesh=266,
                hyperid=277,
                hid=266,
                fee=160,
                ts=200233,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт 11",
                waremd5="off-finerate-11-Hbid1Q",
            ),
            Offer(
                fesh=267,
                hyperid=277,
                hid=266,
                fee=100,
                ts=200234,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт 11",
                waremd5="off-finerate-11-Lbid1Q",
            ),
            Offer(
                fesh=266,
                hyperid=278,
                hid=266,
                fee=150,
                ts=200235,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт 12",
                waremd5="off-finerate-12-Hbid1Q",
            ),
        ]
        cls.index.offers += [
            Offer(
                fesh=266,
                hyperid=266 + i,
                hid=266,
                fee=150 - (i - 10) * 2,
                ts=200220 + i + 13,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт {}".format(i),
                waremd5='off-finerate-{}-Hbid1Q'.format(i),
            )
            for i in range(13, 19)
        ]

        for i in range(1, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200020 + i).respond(0.06)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200030).respond(0.055)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200031).respond(0.052)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200032).respond(0.05)
        for i in range(13, 19):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200033 + i).respond(0.04)

    def test_order_positive(self):
        """
        Проверяем порядок врезок (логика флага market_cpa_shop_incut_premium_ads_incuts_order)
        Значение флага - строка из номеров врезок в перестановке (разделитель - двоеточие), которая соответствует их желаемому порядку
        Номера:
            0 - премиальная
            1 - рейтинговая
        Пример:
            0:1 - сначала премиальная, потом рейтинговая
            1:0 - сначала рейтинговая, потом премиальная
            1 - только рейтинговая (премиальную вообще не хотим)
        В этом тесте проверяем позитивные кейсы
        """

        def check_incut_in_result(response, incut_id, position):
            # Проверяем, что врезка incut_id стоит на позиции position
            self.assertFragmentIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': incut_id,
                                'position': position,
                            },
                        ],
                    },
                },
            )

        def check_incut_not_in_result(response, incut_id):
            self.assertFragmentNotIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': incut_id,
                            },
                        ],
                    },
                },
            )

        params = get_cgi_params_dict()
        rearr_flags = get_rearrs_dict()
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0"  # Хотим только премиальную врезку

        response = self.report.request_json(T.get_request(params, rearr_flags))
        # Проверяем, что есть только премиальная на первой позиции (хотя в бандле запросили ещё рейтинговую после премиальной)
        check_incut_in_result(response, incut_id='default', position=1)
        check_incut_not_in_result(response, incut_id='cpa_shop_incut_filter_by_model_rating')

        # Меняем порядок врезок, чтобы была только рейтинговая (хотя в бандле запросили ещё премиальную после рейтинговой)
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "1"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_high_rating_then_default_incut.json"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_incut_not_in_result(response, incut_id='default')
        check_incut_in_result(response, incut_id='cpa_shop_incut_filter_by_model_rating', position=1)

        # Меняем порядок врезок, чтобы были обе, премиальная первая
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0:1"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_incut_in_result(response, incut_id='default', position=1)
        check_incut_in_result(response, incut_id='cpa_shop_incut_filter_by_model_rating', position=5)

        # Меняем порядок врезок, чтобы были обе, рейтинговая первая
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "1:0"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_high_rating_then_default_incut.json"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_incut_in_result(response, incut_id='default', position=5)
        check_incut_in_result(response, incut_id='cpa_shop_incut_filter_by_model_rating', position=1)

        # Запрещаем собирать рейтинговую врезку безусловно флагом market_premium_ads_gallery_never_create_incut_with_high_model_rating
        rearr_flags["market_premium_ads_gallery_never_create_incut_with_high_model_rating"] = 1
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_incut_in_result(response, incut_id='default', position=1)
        check_incut_not_in_result(response, incut_id='cpa_shop_incut_filter_by_model_rating')

    def test_order_wrong_flag_values(self):
        """
        Проверяем порядок врезок (логика флага market_cpa_shop_incut_premium_ads_incuts_order)
        Проверяем негативные кейсы (флаг задан неверно)
        """
        params = get_cgi_params_dict()
        rearr_flags = get_rearrs_dict()
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"
        # Проверяем, что репорт кидает ошибку, когда запрашиваем 2 врезки одного типа в выдаче
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "1:1"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.error_log.expect(code=3636)  # CGI_CANNOT_PARSE_EXPERIMENTAL_FLAG

        # Проверяем, что репорт кидает ошибку, когда запрашиваем несуществующую врезку (нет врезки с таким номером)
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "2"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.error_log.expect(code=3636)  # CGI_CANNOT_PARSE_EXPERIMENTAL_FLAG

        # Проверяем, что репорт кидает ошибку, когда запрашиваем несуществующую врезку (нет врезки с таким номером)
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "10"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.error_log.expect(code=3636)  # CGI_CANNOT_PARSE_EXPERIMENTAL_FLAG

        # Проверяем, что репорт кидает ошибку, когда разделитель неправильный
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0-1"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.error_log.expect(code=3636)  # CGI_CANNOT_PARSE_EXPERIMENTAL_FLAG

        # Проверяем, что репорт не ломается (не крэшится), когда флаг не задан
        del rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"]
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(response, "results")

    def test_demux_common_single_incut(self):
        """
        Проверяем, что в целом алгоритм демукса работает, как ожидаем
        Проверяем для одиночных врезок
        """

        params = get_cgi_params_dict()
        rearr_flags = get_rearrs_dict()
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"

        # Хотим только премиальную врезку, без рейтинговой
        # Пока что не задаём рандомизацию - в "первую" врезку оффера проходят без рандомизации, тест не должен флакать
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        # В ответ попадает топ из выдачи
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 1,
                            'items': [
                                {
                                    "wareId": "off-lowrate-03-TT-111Q",
                                },
                                {
                                    "wareId": "off-lowrate-02-TT-111Q",
                                },
                                {
                                    "wareId": "off-lowrate-01-TT-111Q",
                                },
                            ]
                            + [{"wareId": "off-finerate-{}-Hbid1Q".format(offer_num)} for offer_num in range(10, 17)],
                        },
                    ],
                },
            },
            preserve_order=True,  # Порядок важен
        )

        # Хотим только рейтинговую врезку, без премиальной
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "1"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_high_rating_then_default_incut.json"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        # В ответ попадает топ из выдачи
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 1,
                            'items': [
                                {"wareId": "off-finerate-{}-Hbid1Q".format(offer_num)} for offer_num in range(10, 19)
                            ],
                        },
                    ],
                },
            },
            preserve_order=True,  # Порядок важен
        )

    def test_demux_common_two_incuts(self):
        """
        Проверяем, что в целом алгоритм демультиплексора работает, как ожидаем
        Проверяем для двух врезок
        """

        params = get_cgi_params_dict()
        rearr_flags = get_rearrs_dict()
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"

        # Хотим сначала премиальную врезку, потом рейтинговую
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0:1"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        # В ответ премиальной попадает топ из выдачи
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 1,
                            'items': [
                                {
                                    "wareId": "off-lowrate-03-TT-111Q",
                                },
                                {
                                    "wareId": "off-lowrate-02-TT-111Q",
                                },
                                {
                                    "wareId": "off-lowrate-01-TT-111Q",
                                },
                            ]
                            + [{"wareId": "off-finerate-{}-Hbid1Q".format(offer_num)} for offer_num in range(10, 17)],
                        },
                    ],
                },
            },
            preserve_order=True,  # Порядок важен
        )
        # В ответ рейтинговой попадают только офферы с высоким рейтингом
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 5,
                            'items': [
                                {
                                    "wareId": "off-finerate-{}-Hbid1Q".format(offer_num)
                                    # Тут для текущей ситуации 3 дубля - 10, 11, 16
                                    # 11 и 16 попали с "первого" прохода, "выиграли" в тесте на вероятность
                                    # 10 попал со "второго" прохода, безусловно, после того,
                                    # как врезка не набралась за "первый" проход
                                }
                                for offer_num in [10, 11, 16, 17, 18]
                            ],
                        },
                    ],
                },
            },
            preserve_order=True,  # Порядок важен
        )

        # Задаём другой random_seed - смотрим, что набор рейтинговой поменялся (поменялись дубли)
        rearr_flags["market_cpa_shop_incut_premium_ads_demux_random_seed"] = 100
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 5,
                            'items': [
                                {
                                    "wareId": "off-finerate-{}-Hbid1Q".format(offer_num)
                                    # В этот раз набор офферов другой, потому что рандом сработал по-другому
                                }
                                for offer_num in [10, 15, 16, 17, 18]
                            ],
                        },
                    ],
                },
            },
            preserve_order=True,  # Порядок важен
        )

        # Задаём другой порядок - сначала рейтинговая, потом премиальная
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "1:0"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_high_rating_then_default_incut.json"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        # Сначала показывается рейтинговая врезка
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 1,
                            'items': [
                                {"wareId": "off-finerate-{}-Hbid1Q".format(offer_num)} for offer_num in range(10, 19)
                            ],
                        },
                    ],
                },
            },
            preserve_order=True,  # Порядок важен
        )
        # Следом показывается премиальная. При этом офферы с низким рейтингом и большой релевантностью
        # безусловно проходят в её начало, а "хвост" премиальной набирается из дублей офферов с высоким рейтингом
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 5,
                            'items': [
                                {
                                    "wareId": "off-lowrate-03-TT-111Q",
                                },
                                {
                                    "wareId": "off-lowrate-02-TT-111Q",
                                },
                                {
                                    "wareId": "off-lowrate-01-TT-111Q",
                                },
                            ]
                            + [{"wareId": "off-finerate-{}-Hbid1Q".format(offer_num)} for offer_num in [14, 15, 17]],
                        },
                    ],
                },
            },
            preserve_order=True,  # Порядок важен
        )

    def test_two_incuts_log_with_autobroker(self):
        """
        Проверяем shows log и автоброкер
        Проверяем для двух врезок, когда есть дубли офферов во врезках
        """

        params = get_cgi_params_dict()
        rearr_flags = get_rearrs_dict()
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0:1"

        response = self.report.request_json(T.get_request(params, rearr_flags))
        # Сначала проверяем автоброкер премиальной врезки (она первая в выдаче)
        # Автоброкер проходит следующим образом:
        #   берётся релевантность текущего оффера и оффера, который его "подпирает"
        #   релевантность оффера-подпорки делится на релевантность текущего оффера - это множитель ставки
        #   ставка текущего оффера умножается на этот множитель - получается амнистированная ставка
        # Релевантности офферов и их подпорок можно найти в дебаг-трейсе, записи начинаются так: [Premium_ads] [replay_autobroker]
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 1,
                            'items': [
                                # У первых 3 офферов (lowrate) ставки отличаются на 1, они друг друга подпирают,
                                # амнистии равны 1
                                {
                                    "wareId": "off-lowrate-03-TT-111Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 503,
                                            "brokeredFee": 502,
                                        },
                                    },
                                },
                                {
                                    "wareId": "off-lowrate-02-TT-111Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 502,
                                            "brokeredFee": 501,
                                        },
                                    },
                                },
                                {
                                    # этот оффер подпирается finerate-оффером с гораздо более низкой ставкой
                                    "wareId": "off-lowrate-01-TT-111Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 501,
                                            "brokeredFee": 200,
                                        },
                                    },
                                },
                                {
                                    # У следующих офферов распределение ставок другое
                                    "wareId": "off-finerate-10-Hbid1Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 200,
                                            "brokeredFee": 180,
                                        },
                                    },
                                },
                                {
                                    "wareId": "off-finerate-11-Hbid1Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 160,
                                            "brokeredFee": 150,
                                        },
                                    },
                                },
                                {
                                    # Проверяем, что последний оффер амнистируется не в minbid'ы,
                                    # так как в премиальную врезку могло бы пройти значительно больше офферов
                                    "wareId": "off-finerate-16-Hbid1Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 138,
                                            "brokeredFee": 136,
                                        },
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
            preserve_order=True,  # Порядок важен
        )
        # Проверяем автоброкер врезки "товары с рейтингом 4 и выше"
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 5,
                            'items': [
                                # В выдаче - офферы с номерами 10, 11, 16, 17, 18
                                # Оффер 10 - дубль, собран на "втором" проходе (из-за нехватки офферов)
                                # Офферы 11, 16 - дубли, собраны на "первом" проходе (прошли случайный отбор)
                                # Офферы 17, 18 не дубли
                                # Тем не менее, амнистироваться должны 10 в 11, 11 в 16 и т.д.
                                {
                                    "wareId": "off-finerate-10-Hbid1Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 200,
                                            "brokeredFee": 180,
                                        },
                                    },
                                },
                                {
                                    "wareId": "off-finerate-11-Hbid1Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 160,
                                            # В этой врезке этот оффер уже амнистируется в 16-й оффер,
                                            # поэтому списанная ставка меньше
                                            "brokeredFee": 138,
                                        },
                                    },
                                },
                                {
                                    "wareId": "off-finerate-16-Hbid1Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 138,
                                            "brokeredFee": 136,
                                        },
                                    },
                                },
                                {
                                    # Проверяем, что последний оффер амнистируется в minbid'ы,
                                    # так как врезка не набралась за "первый" проход
                                    "wareId": "off-finerate-18-Hbid1Q",
                                    "debug": {
                                        "sale": {
                                            "shopFee": 134,
                                            "brokeredFee": 1,
                                        },
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
            preserve_order=True,  # Порядок важен
        )
        # Теперь проверяем логи:
        #   - проверяем, что "крайние" офферы попали в ответ (и что их позиции верные)
        #   - проверяем, что у дублей в логах разные записи
        # Проверяем первую врезку (премиальную)
        self.show_log_tskv.expect(
            ware_md5="off-lowrate-03-TT-111Q", url_type=6, pp=230, shop_fee=503, shop_fee_ab=502, position=1
        ).times(1)
        self.show_log_tskv.expect(
            ware_md5="off-lowrate-02-TT-111Q", url_type=6, pp=230, shop_fee=502, shop_fee_ab=501, position=2
        ).times(1)
        self.show_log_tskv.expect(
            ware_md5="off-lowrate-01-TT-111Q", url_type=6, pp=230, shop_fee=501, shop_fee_ab=200, position=3
        ).times(1)
        self.show_log_tskv.expect(
            ware_md5="off-finerate-10-Hbid1Q", url_type=6, pp=230, shop_fee=200, shop_fee_ab=180, position=4
        ).times(1)
        self.show_log_tskv.expect(
            ware_md5="off-finerate-11-Hbid1Q", url_type=6, pp=230, shop_fee=160, shop_fee_ab=150, position=5
        ).times(1)
        self.show_log_tskv.expect(
            ware_md5="off-finerate-16-Hbid1Q", url_type=6, pp=230, shop_fee=138, shop_fee_ab=136, position=10
        ).times(1)
        # Проверяем вторую врезку (рейтинговую)
        self.show_log_tskv.expect(
            ware_md5="off-finerate-10-Hbid1Q", url_type=6, pp=54, shop_fee=200, shop_fee_ab=180, position=1
        ).times(1)
        self.show_log_tskv.expect(
            ware_md5="off-finerate-11-Hbid1Q", url_type=6, pp=54, shop_fee=160, shop_fee_ab=138, position=2
        ).times(1)
        self.show_log_tskv.expect(
            ware_md5="off-finerate-16-Hbid1Q", url_type=6, pp=54, shop_fee=138, shop_fee_ab=136, position=3
        ).times(1)
        self.show_log_tskv.expect(
            ware_md5="off-finerate-18-Hbid1Q", url_type=6, pp=54, shop_fee=134, shop_fee_ab=1, position=5
        ).times(1)
        # Проверяем, что врезки можно отличить между собой по записи
        self.show_log_tskv.expect(incut_id="default", organic_row=1).times(1)
        # TODO: почему organic_row=4 вместо 5??
        self.show_log_tskv.expect(incut_id="cpa_shop_incut_filter_by_model_rating", organic_row=4).times(1)

    def test_prob_coeffs(self):
        """
        Проверяем коэффициенты обновления вероятностей
        Когда оффер встречается в текущей врезке, вероятность взять его в следующую умножается на (1 - pow_base_coef ^ (pos / pow_div_coef))
            market_cpa_shop_incut_premium_ads_demux_rand_pow_base_coef (0.75)
            market_cpa_shop_incut_premium_ads_demux_rand_pow_div_coef (3)
        """

        params = get_cgi_params_dict()
        rearr_flags = get_rearrs_dict()
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0:1"
        # Задаём такие коэффициенты, чтобы было больше дублей
        rearr_flags["market_cpa_shop_incut_premium_ads_demux_rand_pow_base_coef"] = 0.3
        rearr_flags["market_cpa_shop_incut_premium_ads_demux_rand_pow_div_coef"] = 0.7

        # Проверяем вторую из врезок (рейтинговую)
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 5,
                            # Стало проходить больше офферов (до этого проходило слишком мало), так как
                            # с выбранными коэффициентами "лояльнее" относимся к дублям
                            'items': ElementCount(9),
                        },
                    ],
                },
            },
        )

    def test_turn_off_duplicates_strict(self):
        """
        Проверяем режим отключения дублей
        Дубли полностью запрещаются включением флага market_cpa_shop_incut_premium_ads_demux_forbid_duplicates
        """

        params = get_cgi_params_dict()
        rearr_flags = get_rearrs_dict()
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0:1"
        # Включаем флаг, запрещающий дубли
        rearr_flags["market_cpa_shop_incut_premium_ads_demux_forbid_duplicates"] = 1

        response = self.report.request_json(T.get_request(params, rearr_flags))
        # Проверяем вторую из врезок (рейтинговую) - она не набирается, так как без дублей офферов на неё не хватает
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                        },
                    ],
                },
            },
        )

        # Меняем минимальное кол-во офферов во врезках, чтобы собрались обе врезки. Проверим, что соберутся без дублей
        rearr_flags["market_cpa_shop_incut_blender_min_numdoc_desktop"] = 2
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 1,
                            'items': [
                                {
                                    "wareId": "off-lowrate-03-TT-111Q",
                                },
                                {
                                    "wareId": "off-lowrate-02-TT-111Q",
                                },
                                {
                                    "wareId": "off-lowrate-01-TT-111Q",
                                },
                            ]
                            + [
                                {"wareId": "off-finerate-{}-Hbid1Q".format(offer_num)}
                                for offer_num in range(10, 17)
                                # Сюда наберутся офферы с высоким рейтингом с номерами от 10 до 16 включительно
                            ],
                        },
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 5,
                            'items': [
                                # Оффера с номерами от 10 до 16 включительно попали во врезку выше,
                                # сюда они точно не должны попасть (дубли выключены)
                                {
                                    "wareId": "off-finerate-17-Hbid1Q",
                                },
                                {
                                    "wareId": "off-finerate-18-Hbid1Q",
                                },
                            ],
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_rating_incut_switching_off(self):
        """
        Проверяем запрет рейтинговой врезки
        Набор рейтинговой врезки полностью запрещается флагом
            market_premium_ads_gallery_never_create_incut_with_high_model_rating
        """

        params = get_cgi_params_dict()
        rearr_flags = get_rearrs_dict()
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0:1"

        # Подсловарь из json-ответа, который соответствует набранной врезке "товары с рейтингом 4 и выше"
        rating_incut_dict_expectation = {
            'incuts': {
                'results': [
                    {
                        'entity': 'searchIncut',
                        'incutId': 'cpa_shop_incut_filter_by_model_rating',
                    },
                ],
            },
        }

        # Проверяем сначала, что рейтинговая врезка набирается, так как в тесте выставлен флаг
        # market_premium_ads_gallery_never_create_incut_with_high_model_rating=0
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(response, rating_incut_dict_expectation)

        # Проверяем рейтинговую врезку - она не набирается, так как запрещена флагом
        rearr_flags["market_premium_ads_gallery_never_create_incut_with_high_model_rating"] = 1
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentNotIn(response, rating_incut_dict_expectation)

    def test_randomization(self):
        """
        Проверяем рандомизацию - если seed не задан, то он будет генерироваться как хэш от hid, text запроса и
        поколения индекса
        """

        params = get_cgi_params_dict()
        params["hid"] = 266
        rearr_flags = get_rearrs_dict()
        # Убираем seed из флага, чтобы seed генерировался из параметров запроса
        del rearr_flags["market_cpa_shop_incut_premium_ads_demux_random_seed"]
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0:1"

        # Будем смотреть на рейтинговую врезку, так как рандомизация влияет именно на её набор
        def check_offers_in_rating_incut(response, offer_numbers):
            self.assertFragmentIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': 'cpa_shop_incut_filter_by_model_rating',
                                'position': 5,
                                'items': [
                                    {
                                        "wareId": "off-finerate-{}-Hbid1Q".format(offer_num),
                                    }
                                    for offer_num in offer_numbers
                                ],
                            },
                        ],
                    },
                },
            )

        # Проверяем номера офферов в выдаче рейтинговой врезки для "обычного" запроса
        response = self.report.request_json(T.get_request(params, rearr_flags))
        offers_in_rating_incut_default = [10, 11, 14, 15, 16, 17, 18]
        check_offers_in_rating_incut(response, offers_in_rating_incut_default)

        params["text"] = "Торты"  # поменяли текст, раньше был "торт"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_offers_in_rating_incut(response, [10, 13, 14, 15, 17, 18])  # Выдача изменилась

        # Проверяем, что если вернуть текст запроса обратно, то рандом вернётся обратно (есть воспроизводимость)
        params["text"] = "торт"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_offers_in_rating_incut(response, offers_in_rating_incut_default)  # Выдача вернулась обратно

    def test_forbid_second_pass(self):
        """
        Проверяем флаг, который жёстко отрубает "второй" проход. Второй проход пытается добрать врезку до
        min-num-doc'а, не используя вероятностный механизм, если она не набралась
        """

        params = get_cgi_params_dict()
        rearr_flags = get_rearrs_dict()
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0:1"

        # Проверяем, что рейтинговая врезка набирается в обычной ситуации, так как
        # включён "второй" проход
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 5,
                            'items': NotEmpty(),
                        },
                    ],
                },
            },
        )

        # Отрубаем "второй" проход - рейтинговая врезка не наберётся, офферов не хватит
        rearr_flags["market_cpa_shop_incut_premium_ads_demux_no_second_pass"] = 1
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 5,
                            'items': NotEmpty(),
                        },
                    ],
                },
            },
        )

        # Снизим требования (уменьшим min-num-doc), чтобы рейтинговая врезка набралась без
        # "второго" прохода - тогда она наберётся и покажется
        rearr_flags["market_cpa_shop_incut_blender_min_numdoc_desktop"] = 3
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 5,
                            'items': NotEmpty(),
                        },
                    ],
                },
            },
        )

    def test_duplicates_limits(self):
        """
        Проверяем флаги, которые жёстко ограничивают кол-во дублей:
            - market_cpa_shop_incut_premium_ads_demux_each_offer_duplicates_max_count - макс. кол-во вхождений оффера во врезки
            - market_cpa_shop_incut_premium_ads_demux_banned_positions_for_duplicates - жёсткий запрет дублировать офферы с первых позиций
        """

        params = get_cgi_params_dict()
        rearr_flags = get_rearrs_dict()
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0:1"
        # Флаги уже задаются такими в get_rearrs_dict, но ещё раз их укажем, чтобы сделать акцент на них
        # market_cpa_shop_incut_premium_ads_demux_each_offer_duplicates_max_count=0 означает, что дублей допускается сколько угодно
        rearr_flags["market_cpa_shop_incut_premium_ads_demux_each_offer_duplicates_max_count"] = 0
        # market_cpa_shop_incut_premium_ads_demux_banned_positions_for_duplicates=0 отключает запрет дублирования с первых позиций
        rearr_flags["market_cpa_shop_incut_premium_ads_demux_banned_positions_for_duplicates"] = 0

        def check_offers_in_rating_incut(response, offer_numbers):
            # Проверяем, что офферы с номерами offer_numbers есть в рейтинговой врезке
            self.assertFragmentIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': 'cpa_shop_incut_filter_by_model_rating',
                                'position': 5,
                                'items': [
                                    {
                                        "wareId": "off-finerate-{}-Hbid1Q".format(offer_num),
                                    }
                                    for offer_num in offer_numbers
                                ],
                            },
                        ],
                    },
                },
            )
            # Проверяем, что нет никаких других офферов в этой врезке
            self.assertFragmentIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': 'cpa_shop_incut_filter_by_model_rating',
                                'position': 5,
                                'items': ElementCount(len(offer_numbers)),
                            },
                        ],
                    },
                },
            )

        # Проверяем выдачу, где оба запрета нет. Смотрим только на рейтинговую врезку, так как в ней
        # дубли из премиальной
        response = self.report.request_json(T.get_request(params, rearr_flags))
        # Задублировались офферы 10, 11 и 16
        # При этом 10 и 11 стоят на 4 и 5 позициях в премиальной
        # 16 стоит на последней позиции в премиальной
        offer_numbers_default = [10, 11, 16, 17, 18]
        check_offers_in_rating_incut(response, offer_numbers_default)

        # Снижаем min-num-doc, чтобы после фильтрации врезки набирались всё равно
        rearr_flags["market_cpa_shop_incut_blender_min_numdoc_desktop"] = 2
        # Явно указываем, что "второй" проход есть (это докажет безусловность других жёстких фильтров)
        rearr_flags["market_cpa_shop_incut_premium_ads_demux_no_second_pass"] = 0
        # Запрещаем дубли с первых четырёх позиций - оффер 10 будет запрещён к дублированию, 11 и 16 разрешены
        rearr_flags["market_cpa_shop_incut_premium_ads_demux_banned_positions_for_duplicates"] = 4
        # Оффер 10 будет запрещён к дублированию - он стоит на позиции 4 (в премиальной, которая первая)
        # Рандом для него не будет разыгрываться, поэтому исход рандомов будет другой - во врезку пройдёт теперь 12-й оффер
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_offers_in_rating_incut(response, [12, 17, 18])

        # Увеличиваем флаг, чтобы был запрещён ещё и 12-й оффер. Исходы рандомов снова поменяются, но в ответ попадут только офферы
        # с номерами не меньше 13
        rearr_flags["market_cpa_shop_incut_premium_ads_demux_banned_positions_for_duplicates"] = 6
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_offers_in_rating_incut(response, [14, 15, 17, 18])

        # Запрещаем дубли вообще - тогда вплоть до 16-го оффера (самый последний из премиальной) никакие офферы из премиальной
        # не попадут в рейтинговую (пройти смогут только 17 и 18, которых не было в премиальной)
        # Флагом запрещаем показывать больше одного вхождения оффера во врезки
        rearr_flags["market_cpa_shop_incut_premium_ads_demux_each_offer_duplicates_max_count"] = 1
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_offers_in_rating_incut(response, [17, 18])

    def test_prior_minnumdoc_check(self):
        """
        Проверяем априорную корректировку алгоритма в зависимости от размера выдачи и примерного допустимого кол-ва дублей.
        Проверяем флаг market_cpa_shop_incut_premium_ads_demux_prior_check_mult_coef (mult_coef)
        Перед основной логикой демукса из желаемого кол-ва врезок оставляем лишь n врезок - столько, сколько удовлетворяет:
            min_num_doc * n < mult_coef * offersCount
            offersCount - доступный размер топа результата cpa_shop_incut
        """

        params = get_cgi_params_dict()
        rearr_flags = get_rearrs_dict()
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0:1"

        # Задаём флаг market_cpa_shop_incut_premium_ads_demux_prior_check_mult_coef, чтобы набралась лишь 1 врезка,
        # вторую демукс даже не попытается набрать:
        #   min_num_doc = 5; offersCount = 12
        #   mult_coef = 0.58 => 5 * n < 0.42 * 12 => n = 1 (наберётся только премиальная)
        rearr_flags["market_cpa_shop_incut_premium_ads_demux_prior_check_mult_coef"] = 0.42
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': ElementCount(1),
                },
            },
        )

        # mult_coef = 0.84 => 5 * n < 0.84 * 12 => n = 2 (будут набираться обе)
        rearr_flags["market_cpa_shop_incut_premium_ads_demux_prior_check_mult_coef"] = 0.84
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': ElementCount(2),
                },
            },
        )

    def test_logging_pp(self):
        """
        Проверяем, что проставляются правильные pp в случае, когда используется demux
        """

        params = get_cgi_params_dict()
        rearr_flags = get_rearrs_dict()
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0:1"

        def check_pp(response, expected_pp_default, expected_pp_rating, dont_check_logs=False):
            # На разных платформах разные num-doc'и - тут проверяем выборочно офферы (но не все);
            # Из-за разных num-doc'ов и min-num-doc'ов выдачи отличаются, поэтому парсим выборочно по 4 оффера
            ware_ids_default = [Capture() for _ in range(4)]
            ware_ids_rating = [Capture() for _ in range(4)]
            # Премиальная врезка
            self.assertFragmentIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': 'default',
                                'position': 1,
                                'items': [
                                    {
                                        "wareId": NotEmpty(capture=capture),
                                        "urls": {
                                            "encrypted": Contains('/pp={}/'.format(expected_pp_default)),
                                        },
                                        'feeShowPlain': Contains("pp: {}".format(expected_pp_default)),
                                    }
                                    for capture in ware_ids_default
                                ],
                            },
                        ],
                    },
                },
            )
            # Рейтинговая
            self.assertFragmentIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': 'cpa_shop_incut_filter_by_model_rating',
                                'position': 5,
                                'items': [
                                    {
                                        "wareId": NotEmpty(capture=capture),
                                        "urls": {
                                            "encrypted": Contains('/pp={}/'.format(expected_pp_rating)),
                                        },
                                        "feeShowPlain": Contains("pp: {}".format(expected_pp_rating)),
                                    }
                                    for capture in ware_ids_rating
                                ],
                            },
                        ],
                    },
                },
            )
            # Проверяем логи (премиальная врезка)
            for capture in ware_ids_default:
                self.show_log_tskv.expect(ware_md5=capture.value, url_type=6, pp=expected_pp_default).once()
            # рейтинговая врезка
            for capture in ware_ids_rating:
                self.show_log_tskv.expect(ware_md5=capture.value, url_type=6, pp=expected_pp_rating).once()

        # Проверяем desktop
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, expected_pp_default=230, expected_pp_rating=54)

        # Проверяем touch
        params["platform"] = "touch"
        params["touch"] = "1"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, expected_pp_default=620, expected_pp_rating=654)

        # Проверяем ANDROID
        del params["platform"]
        del params["touch"]
        params["client"] = "ANDROID"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, expected_pp_default=1709, expected_pp_rating=1754)

        # Проверяем IOS
        params["client"] = "IOS"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, expected_pp_default=1809, expected_pp_rating=1854)

    def test_logging_same_pp(self):
        """
        DEPRECATED
        Проверяем, что проставляются одни и те же pp в случае, когда используется demux и выставлен флаг
        market_cpa_shop_incut_premium_ads_use_different_pp=0

        TODO: надо убрать этот тест, когда будем убирать флаг market_cpa_shop_incut_premium_ads_use_different_pp

        Проверяем отдельным тестом, потому что у офферов одни и те же pp, проверка once не прокатывает
        """

        params = get_cgi_params_dict()
        rearr_flags = get_rearrs_dict()
        # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_default_then_high_rating_incut.json"
        rearr_flags["market_cpa_shop_incut_premium_ads_incuts_order"] = "0:1"
        # Возвращаем старую deprecated-логику, по которой pp одинаковые у всех врезок на базе cpa_shop_incut
        rearr_flags["market_cpa_shop_incut_premium_ads_use_different_pp"] = 0

        response = self.report.request_json(T.get_request(params, rearr_flags))

        # Парсим выдачу - надо захватить все офферы в каждой врезке
        # Захватить нужно прям всю выдачу, чтобы точно посчитать кол-во дублей
        # Точное кол-во дублей нужно, чтобы правильно проверить кол-во кортежей в shows-log
        offers_default_incut = Capture()
        offers_rating_incut = Capture()
        # Парсим выдачу
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            # Премиальная
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 1,
                            'items': NotEmpty(capture=offers_default_incut),
                        },
                        {
                            # Рейтинговая
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 5,
                            'items': NotEmpty(capture=offers_rating_incut),
                        },
                    ],
                },
            },
        )

        # Парсим сами офферы и проверяем, что в выдаче в урлах правильные pp
        ware_ids_from_default_incut = [Capture() for _ in range(len(offers_default_incut.value))]
        ware_ids_from_rating_incut = [Capture() for _ in range(len(offers_rating_incut.value))]

        expected_pp = 230  # pp у всех элементов врезок одни и те же (230 - десктопный pp для премиальной)
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            # Премиальная
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 1,
                            'items': [
                                {
                                    "wareId": NotEmpty(capture=ware_id),
                                    "urls": {
                                        "encrypted": Contains('/pp={}/'.format(expected_pp)),
                                    },
                                    "feeShowPlain": Contains("pp: {}".format(expected_pp)),
                                }
                                for ware_id in ware_ids_from_default_incut
                            ],
                        },
                        {
                            # Рейтинговая
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 5,
                            'items': [
                                {
                                    "wareId": NotEmpty(capture=ware_id),
                                    "urls": {
                                        "encrypted": Contains('/pp={}/'.format(expected_pp)),
                                    },
                                    "feeShowPlain": Contains("pp: {}".format(expected_pp)),
                                }
                                for ware_id in ware_ids_from_rating_incut
                            ],
                        },
                    ],
                },
            },
        )

        # Демукс допускает дублирование, ищем такие "дубли", чтобы правильно проверить логи
        # (полных дублей в логах быть не должно, но по-честному отличить записи слишком сложно) + этот тест скоро выпилят
        ware_id_counters = {}
        for ware_id in ware_ids_from_default_incut + ware_ids_from_rating_incut:
            # Обновляем счётчик дублей
            if ware_id.value in ware_id_counters:
                ware_id_counters[ware_id.value] += 1
            else:
                ware_id_counters[ware_id.value] = 1

        # Проверяем логи с учётом дублей
        for ware_id in ware_id_counters:
            self.show_log_tskv.expect(ware_md5=ware_id, url_type=6, pp=expected_pp).times(ware_id_counters[ware_id])


if __name__ == '__main__':
    main()
