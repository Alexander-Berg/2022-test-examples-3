#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    GLParam,
    GLType,
    GLValue,
    MarketSku,
    Model,
    ModelDescriptionTemplates,
    MnPlace,
    Offer,
    Shop,
)
from core.blender_bundles import get_supported_incuts_cgi
from core.testcase import TestCase, main
from core.types.reserveprice_fee import ReservePriceFee
from core.matcher import ElementCount, NotEmpty, Capture


def dict_to_rearr(rearr_flags_dict):
    return ';'.join([rearr_name + '=' + str(rearr_flags_dict[rearr_name]) for rearr_name in rearr_flags_dict])


def get_default_rearrs_dict():
    """
    Создаём дефолтные реарр-флаги для запусков тестов на метадоковый поиск в cpa_shop_incut
    """
    return {
        'market_cpa_shop_inuct_enable_metadoc_search': 1,
        'market_blender_cpa_shop_incut_enabled': 1,
    }


def get_cgi_params_dict_cpa_shop_incut(text=None, hid=None):
    """
    Возвращает стандартный для этого набора тестов словарь cgi-параметров
    """
    cgi_params = {
        "place": "cpa_shop_incut",
        "pp": 230,
        "debug": "da",
        "min-num-doc": 1,
        "numdoc": 10,
    }
    if text is not None:
        cgi_params["text"] = text
    if hid is not None:
        cgi_params["hid"] = hid
    return cgi_params


def get_cgi_params_dict_blender(text=None, hid=None):
    """
    Возвращает стандартный для этого набора тестов словарь cgi-параметров
    """
    cgi_params = {
        "place": "prime",
        "blender": 1,
        "client": "frontend",
        "platform": "desktop",
        "supported-incuts": get_supported_incuts_cgi(),
        "pp": 7,
        "debug": "da",
        "use-default-offers": 1,
        "allow-collapsing": 1,
    }
    if text is not None:
        cgi_params["text"] = text
    if hid is not None:
        cgi_params["hid"] = hid
    return cgi_params


class T(TestCase):
    @staticmethod
    def get_request(params, rearr):
        def dict_to_str(data, separator):
            return str(separator).join("{}={}".format(str(k), str(v)) for (k, v) in data.iteritems())

        return "{}&rearr-factors={}".format(dict_to_str(params, '&'), dict_to_str(rearr, ';'))

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name='3P поставщик 2',
                cpa=Shop.CPA_REAL,
                regions=[213],
            ),
            Shop(
                fesh=2,
                priority_region=213,
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name='3P поставщик 2',
                cpa=Shop.CPA_REAL,
                regions=[213],
            ),
        ]

        # Наполнение для выдачи по запросу "провод"
        cls.index.models += [
            Model(hid=2000, hyperid=2000, ts=2000, title='model_2000', vbid=11),
            Model(hid=2000, hyperid=2001, ts=2001, title='model_2001', vbid=11),
            Model(hid=2000, hyperid=2002, ts=2002, title='model_2002', vbid=11),
        ]
        cls.index.mskus += [
            MarketSku(
                title="2000 msku_1 провод",
                hid=2000,
                hyperid=2000,
                sku=200001,
                delivery_buckets=[1234],
                ts=200001,
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=1,
                        fee=200,
                        waremd5="BLUE-200001-FEED-0001Q",
                        title="200001 3P offer 1 провод",
                        ts=2000011,
                    ),
                    BlueOffer(
                        price=1680,
                        feedid=2,
                        fee=100,
                        waremd5="BLUE-200001-FEED-0002Q",
                        title="200001 3P offer 2 провод",
                        ts=2000012,
                    ),
                ],
            ),
            MarketSku(
                title="2000 msku_2 провод",
                hid=2000,
                hyperid=2000,
                sku=200002,
                delivery_buckets=[1234],
                ts=200002,
                blue_offers=[
                    BlueOffer(
                        price=1680,
                        feedid=2,
                        fee=100,
                        waremd5="BLUE-200002-FEED-0002Q",
                        title="200002 3P offer 1 провод",
                        ts=2000021,
                    ),
                ],
            ),
            MarketSku(
                title="2001 msku_2 провод",
                hid=2000,
                hyperid=2001,
                sku=200101,
                delivery_buckets=[1234],
                ts=200101,
            ),
        ]
        cls.index.offers += [
            Offer(  # Белый оффер для ску 200101
                hid=2000,
                hyperid=2001,
                fesh=1,
                price=1800,
                fee=160,
                sku=200101,
                cpa=Offer.CPA_REAL,
                title="200101 3P white offer 1 провод",
                waremd5='200101-white-0001-111Q',
                ts=2001011,
            ),
            Offer(  # Кадавр модели 2002
                # (Оффер без ску)
                hid=2000,
                hyperid=2002,
                fesh=2,
                price=1800,
                fee=120,
                cpa=Offer.CPA_REAL,
                title="kadavr 2002 3P white offer провод",
                waremd5='kadavr-white-0001-111Q',
                ts=9999011,
            ),
        ]
        for timestamp in [200001, 200002, 200101]:
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, timestamp).respond(0.01)

    def test_metadoc_search(self):
        """
        Проверяем, что работает поиск по метадокам.
        Проверяем, что работает как для синих, так и для белых офферов.
        Проверяем, что нет группировки по моделям.

        Проверяем как для текстового поиска, так и для категорийного.
        """
        rearrs_dict = get_default_rearrs_dict()

        def _check_response(cgi_params):
            response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": ware_md5,
                        }
                        for ware_md5 in [
                            "BLUE-200001-FEED-0001Q",
                            "200101-white-0001-111Q",
                            "BLUE-200002-FEED-0002Q",
                        ]
                    ],
                },
                preserve_order=True,
                allow_different_len=True,
            )

        # Проверяем для текстового поиска
        _check_response(get_cgi_params_dict_cpa_shop_incut(text="провод"))

        # Проверяем для категорийного поиска
        _check_response(get_cgi_params_dict_cpa_shop_incut(hid=2000))

    def test_no_kadavers_in_output(self):
        """
        Проверяем, что в выдачу не попадают кадавры (офферы с моделью, но без ску)
        """
        rearrs_dict = get_default_rearrs_dict()

        def _check_response(cgi_params):
            response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
            # В выдаче не должно быть офферов модели 2002, так как у этой модели есть только офферы без ску
            # Когда включаем поиск по метадокам в cpa_shop_incut, не хотим допускать такие офферы в выдаче
            self.assertFragmentNotIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "model": {
                                "id": 2002,
                            },
                        },
                    ],
                },
            )

        # Проверяем для текстового поиска
        _check_response(get_cgi_params_dict_cpa_shop_incut(text="провод"))

        # Проверяем для категорийного поиска
        _check_response(get_cgi_params_dict_cpa_shop_incut(hid=2000))

    @classmethod
    def prepare_auction(cls):
        """
        Часть индекса для тестирования аукционов
        Проверяем аукцион в байбоксе и аукцион в cpa_shop_incut на мете
        """
        # Наполнение для выдачи по запросу "бутылка"
        cls.index.models += [
            Model(hid=2003, hyperid=2003, ts=2003, title='model_2003', vbid=11),
            Model(hid=2003, hyperid=2004, ts=2004, title='model_2004', vbid=11),
        ]

        cls.index.mskus += [
            MarketSku(
                title="2003 msku_1 бутылка",
                hid=2003,
                hyperid=2003,
                sku=200301,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        # Победитель байбокса этой msku
                        price=1600,
                        feedid=1,
                        fee=300,
                        waremd5="BLUE-200301-FEED-0001Q",
                        title="200301 3P offer 1 бутылка",
                        ts=2003011,
                    ),
                    BlueOffer(
                        price=1600,
                        feedid=2,
                        fee=250,
                        waremd5="BLUE-200301-FEED-0002Q",
                        title="200001 3P offer 2 бутылка",
                        ts=2003012,
                    ),
                ],
                ts=200301,
            ),
            MarketSku(
                title="2003 msku_2 бутылка",
                hid=2003,
                hyperid=2003,
                sku=200302,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        # Этот оффер проигрывает байбокс белому офферу этой msku
                        price=1600,
                        feedid=1,
                        fee=260,
                        waremd5="BLUE-200302-FEED-0001Q",
                        title="200302 3P offer 1 бутылка",
                        ts=2003021,
                    ),
                ],
                ts=200302,
            ),
            MarketSku(
                title="2004 msku_1 бутылка",
                hid=2003,
                hyperid=2004,
                sku=200401,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1600,
                        feedid=1,
                        fee=200,
                        waremd5="BLUE-200401-FEED-0001Q",
                        title="200401 3P offer 1 бутылка",
                        ts=2004011,
                    ),
                ],
                ts=200401,
            ),
            MarketSku(
                title="2004 msku_2 бутылка",
                hid=2003,
                hyperid=2004,
                sku=200402,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1600,
                        feedid=2,
                        fee=180,
                        waremd5="BLUE-200402-FEED-0001Q",
                        title="200402 3P offer 1 бутылка",
                        ts=2004021,
                    ),
                ],
                ts=200402,
            ),
        ]
        cls.index.offers += [
            Offer(  # Белый оффер для ску 200302 (победитель байбокса)
                hid=2003,
                hyperid=2003,
                fesh=2,
                price=1600,
                fee=280,
                sku=200302,
                cpa=Offer.CPA_REAL,
                title="200302 3P white offer 1 бутылка",
                waremd5='200302-white-0001-111Q',
                ts=2003022,
                delivery_buckets=[1234],
            ),
        ]

        for timestamp in [
            200301,
            200302,
            200401,
            200402,
            2003011,
            2003012,
            2003021,
            2003022,
            2004011,
            2004021,
            2003023,
        ]:
            # Всем ставим одинаковую релевантность, чтобы было ранжирование по ставке
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, timestamp).respond(0.2)

    def test_auction_in_metadoc_search(self):
        """
        Проверяем, что работает поисковый аукцион и аукцион в байбоксе
        """
        rearrs_dict = get_default_rearrs_dict()
        # Отключаем рандом в аукционе в байбоксе, чтобы в тестах не менялась амнистия
        rearrs_dict["market_buybox_auction_rand_low"] = 1.0
        rearrs_dict["market_buybox_auction_rand_delta"] = 0.0

        def _check_response(cgi_params):
            response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
            self.assertFragmentIn(
                response,
                {
                    # Все релевантности одинаковые, поэтому brokeredFee оффера = shopFee его подпорки
                    "results": [
                        {
                            # Оффер с самой большой ставкой
                            # Он подпирается оффером от следующей msku
                            "wareId": "BLUE-200301-FEED-0001Q",
                            "marketSku": "200301",
                            "debug": {
                                "sale": {
                                    "shopFee": 300,
                                    "brokeredFee": 280,
                                },
                            },
                        },
                        {
                            # У оффера 2 подпорки - оффер следующей msku со ставкой 200
                            # и оффер из аукциона в байбоксе со ставкой 260
                            # Выбираем максимальную подпорку для наименьшей амнистии
                            "wareId": "200302-white-0001-111Q",
                            "marketSku": "200302",
                            "debug": {
                                "sale": {
                                    "shopFee": 280,
                                    "brokeredFee": 261,
                                },
                            },
                        },
                        {
                            # Оффер подпирается оффером следующей msku
                            "wareId": "BLUE-200401-FEED-0001Q",
                            "marketSku": "200401",
                            "debug": {
                                "sale": {
                                    "shopFee": 200,
                                    "brokeredFee": 180,
                                },
                            },
                        },
                        {
                            # Оффер без подпорки (последний пришёл с базовых)
                            "wareId": "BLUE-200402-FEED-0001Q",
                            "marketSku": "200402",
                            "debug": {
                                "sale": {
                                    "shopFee": 180,
                                    "brokeredFee": 1,
                                },
                            },
                        },
                    ],
                },
                preserve_order=True,
                allow_different_len=True,
            )

        # Проверяем для текстового поиска
        _check_response(get_cgi_params_dict_cpa_shop_incut(text="бутылка"))

        # Проверяем для категорийного поиска
        _check_response(get_cgi_params_dict_cpa_shop_incut(hid=2003))

    def test_urls_and_logs(self):
        """
        Проверяем, что основные данные в урлах и логах не теряются (и нет дублей)
        """
        captures = {  # Словарь, чтобы парсить выдачу
            ware_md5: {
                "shopFee": Capture(),
                "brokeredFee": Capture(),
            }
            for ware_md5 in [
                "BLUE-200301-FEED-0001Q",
                "200302-white-0001-111Q",
                "BLUE-200401-FEED-0001Q",
                "BLUE-200402-FEED-0001Q",
            ]
        }
        cgi_params = get_cgi_params_dict_cpa_shop_incut(text="бутылка")
        rearrs_dict = get_default_rearrs_dict()
        response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))

        # Парсим выдачу
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "wareId": ware_md5,
                        "debug": {
                            "sale": {
                                "shopFee": NotEmpty(capture=captures[ware_md5]["shopFee"]),
                                "brokeredFee": NotEmpty(capture=captures[ware_md5]["brokeredFee"]),
                            },
                        },
                    }
                    for ware_md5 in captures
                ],
            },
            allow_different_len=True,
        )

        # Проверяем консистентность выдачи и логов
        for ware_md5 in captures:
            shop_fee = captures[ware_md5]["shopFee"].value
            brokered_fee = captures[ware_md5]["brokeredFee"].value
            self.show_log_tskv.expect(
                ware_md5=ware_md5, url_type=6, pp=230, shop_fee=shop_fee, shop_fee_ab=brokered_fee
            ).once()
        # Проверяем, что документ-модель не залогировался (таких документов и не было)
        self.show_log_tskv.expect(url_type=16).times(0)

    @classmethod
    def prepare_metadoc_search_from_blender(cls):
        """
        Часть индекса для тестирования запросов через блендер
        Офферов должно быть достаточно много в органике (хотя бы 8),
        чтобы врезке разрешено было собираться
        """
        # Наполнение для выдачи по запросу "скрепка"
        # Создаём 8 моделей - id от 2005 до 2012 включительно
        hid = 2005
        model_id_start = 2005
        models_count = 8
        for model_idx in range(models_count):
            model_id = model_id_start + model_idx
            cls.index.models.append(
                Model(
                    hid=hid,
                    hyperid=model_id,
                    ts=model_id,
                    title='model_{} скрепка'.format(model_id),
                    vbid=11,
                )
            )
            # На каждую модель - по 1 msku с одним оффером каждая
            msku_id = model_id * 100 + 1
            timestamp_offer = msku_id * 10 + 1
            # Ставки монотонно убывают
            shop_fee = 400 - model_idx * 30
            # Чередуем feedid
            feed_id = 1 if model_id % 2 == 1 else 2
            cls.index.mskus.append(
                MarketSku(
                    title="{} msku_1 скрепка".format(model_id),
                    hid=hid,
                    hyperid=model_id,
                    sku=msku_id,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            price=1600,
                            feedid=feed_id,
                            fee=shop_fee,
                            waremd5="BLUE-{}-FEED-0001Q".format(msku_id),
                            title="{} 3P offer 1 скрепка".format(msku_id),
                            ts=timestamp_offer,
                        ),
                    ],
                    ts=msku_id,
                )
            )
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, msku_id).respond(0.2)

    def test_metadoc_search_from_blender(self):
        """
        Проверяем, что работает поиск по метадокам при запуске из блендера
        """
        cgi_params = get_cgi_params_dict_blender(text="скрепка")
        rearrs_dict = get_default_rearrs_dict()
        response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            # Премиальная врезка
                            "position": 1,
                            "incutId": "default",
                            # В выдаче все 8 офферов
                            "items": ElementCount(8),
                        },
                    ],
                },
            },
        )

    @classmethod
    def prepare_has_adv_bid_literal(cls):
        """
        Часть индекса для проверки фильтрации метадоков по литералу has_adv_bid
        """
        # Наполнение для выдачи по запросу "воздух"
        cls.index.models += [
            Model(hid=2013, hyperid=2013, ts=2013, title='model_2013', vbid=11),
        ]
        cls.index.mskus += [
            MarketSku(
                title="2013 msku_1 воздух",
                hid=2013,
                hyperid=2013,
                sku=201301,
                delivery_buckets=[1234],
                # Выставляем на метадок (msku) литерал has_adv_bid
                set_has_adv_bid_literal=True,
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=1,
                        fee=200,
                        waremd5="BLUE-201301-FEED-0001Q",
                        title="201301 3P offer 1 воздух",
                        ts=2013011,
                    ),
                    BlueOffer(
                        price=1680,
                        feedid=2,
                        fee=180,
                        waremd5="BLUE-201301-FEED-0002Q",
                        title="201301 3P offer 2 воздух",
                        ts=2013012,
                    ),
                ],
                ts=201301,
            ),
            MarketSku(
                title="2013 msku_2 воздух",
                hid=2013,
                hyperid=2013,
                sku=201302,
                delivery_buckets=[1234],
                # НЕ выставляем литерал has_adv_bid
                # В индексаторе литерал должен выставляться на все метадоки,
                # где есть хотя бы 1 оффер со ставкой.
                # Но тут будем тестировать фильтрацию по этому литералу,
                # поэтому даже при условии наличия ставок не будем проставлять литерал
                set_has_adv_bid_literal=False,
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=1,
                        fee=160,
                        waremd5="BLUE-201302-FEED-0001Q",
                        title="201302 3P offer 1 воздух",
                        ts=2013021,
                    ),
                    BlueOffer(
                        price=1680,
                        feedid=2,
                        fee=120,
                        waremd5="BLUE-201302-FEED-0002Q",
                        title="201302 3P offer 2 воздух",
                        ts=2013022,
                    ),
                ],
                ts=201301,
            ),
        ]
        for timestamp in [201301, 201301]:
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, timestamp).respond(0.01)

    def test_has_adv_bid_literal(self):
        """
        Проверяем, что нагребаются только метадоки, где проставлен литерал has_adv_bid
        Литерал только метадоковый, поэтому не важно, какие оффера приматчены к ску - они
        попадают в байбокс (чтобы правильно работал порог по цене)
        """
        rearrs_dict = get_default_rearrs_dict()
        cgi_params = get_cgi_params_dict_cpa_shop_incut(text="воздух")

        # Сначала не используем фильтрацию по литералу - найдутся обе msku
        rearrs_dict["market_cpa_shop_inuct_search_by_has_adv_bid"] = 0
        response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "marketSku": "201301",
                    },
                    {
                        "entity": "offer",
                        # На этой msku нет литерала, но фильтрация по нему отключена
                        "marketSku": "201302",
                    },
                ],
            },
            allow_different_len=False,
        )

        # Включаем фильтрацию по литералу - найдётся лишь одна msku из двух
        rearrs_dict["market_cpa_shop_inuct_search_by_has_adv_bid"] = 1
        response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "marketSku": "201301",
                    },
                ],
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "marketSku": "201302",
                    },
                ],
            },
        )

    @classmethod
    def prepare_sku_aware_data(cls):
        """
        Часть индекса для проверки получения skuAware данных
        """
        # Наполнение для выдачи по запросу "камень"
        hid = 2014
        cls.index.gltypes += [  # Фильтр для проверки skuAware данных
            GLType(
                hid=hid,
                param_id=10123,
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='gl_filter_value1'),
                ],
                model_filter_index=1,
                xslname='sku_filter',
            ),
        ]
        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=hid,
                friendlymodel=['model friendly {sku_filter}'],
                model=[("Основное", {'model full': '{sku_filter}'})],
            ),
        ]

        cls.index.models += [
            Model(hid=hid, hyperid=2014, ts=2014, title='model_2014', vbid=11),
        ]

        cls.index.mskus += [
            MarketSku(
                title="2014 msku_1 камень",
                hid=hid,
                hyperid=2014,
                sku=201401,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1640,
                        feedid=1,
                        fee=200,
                        waremd5="BLUE-201401-FEED-0001Q",
                        title="201401 3P offer 1 камень",
                        ts=2014011,
                    ),
                ],
                # Подклеиваем skuAware данные
                glparams=[GLParam(param_id=10123, value=1)],
                ts=201401,
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 201401).respond(0.01)

    def test_sku_aware_data(self):
        """
        Проверяем, что skuAware данные приезжают сразу вместе с метадоками
        и попадают в выдачу
        """
        rearrs_dict = get_default_rearrs_dict()
        cgi_params = get_cgi_params_dict_cpa_shop_incut(text="камень")
        # Указываем в запросе, что хотим models-specs (для msku)
        cgi_params["show-models-specs"] = "msku-friendly,msku-full"

        response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "wareId": "BLUE-201401-FEED-0001Q",
                        "skuAwareTitles": {
                            "raw": "2014 msku_1 камень",
                            "highlighted": [
                                {
                                    "value": "2014 msku_1 ",
                                },
                                {
                                    "value": "камень",
                                    "highlight": True,
                                },
                            ],
                        },
                        "skuAwarePictures": [
                            {
                                "entity": "picture",
                                "original": NotEmpty(),
                            },
                        ],
                        "skuAwareSpecs": {
                            "full": [
                                {
                                    "groupName": "Основное",
                                    "groupSpecs": [
                                        {
                                            "name": "model full",
                                            "value": "gl_filter_value1",
                                        },
                                    ],
                                },
                            ],
                            "friendly": [
                                "model friendly gl_filter_value1",
                            ],
                            "friendlyext": [
                                {
                                    "value": "model friendly gl_filter_value1",
                                    "usedParams": [10123],  # id GL-фильтра
                                },
                            ],
                        },
                    },
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_metadoc_total_pruncount(cls):
        """
        metadoc_total_pruncount - суммарное кол-во офферов, которые переберём во всех sku
        Будут 2 msku:
          1. много офферов, их скоры релевантности убывают, но ставка растёт, победитель байбокса - последний оффер
          2. тоже много офферов, их скоры тоже убывают, ставка растёт, но из-за прюнинга в байбокс не попадёт "лучший" оффер
        """
        # Наполнение выдачи по запросу "шнурок" (или по hid=2015)
        cls.index.models += [
            Model(hid=2015, hyperid=2015, ts=2015, title='model_2015', vbid=11),
        ]

        offers_count_per_msku = 5  # по 5 офферов на msku
        cls.index.mskus += [
            MarketSku(
                title="2015 msku_1 шнурок",
                hid=2015,
                hyperid=2015,
                sku=201501,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=2000,
                        # Чередуем feed_id у офферов
                        feedid=1 if offer_idx % 2 == 0 else 2,
                        fee=200 + offer_idx * 20,
                        waremd5="BLUE-201501-FEED-000{}Q".format(offer_idx + 1),
                        title="201501 3P offer {} шнурок".format(offer_idx + 1),
                        ts=2015010 + offer_idx,
                    )
                    for offer_idx in range(offers_count_per_msku)
                ],
                ts=201501,
            ),
            MarketSku(
                title="2015 msku_2 шнурок",
                hid=2015,
                hyperid=2015,
                sku=201502,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=2000 - offer_idx * 4,
                        # Чередуем feed_id у офферов
                        feedid=1 if offer_idx % 2 == 0 else 2,
                        fee=100 + offer_idx * 20,
                        waremd5="BLUE-201502-FEED-000{}Q".format(offer_idx + 1),
                        title="201502 3P offer {} шнурок".format(offer_idx + 1),
                        ts=2015020 + offer_idx,
                    )
                    for offer_idx in range(offers_count_per_msku)
                ],
                ts=201502,
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 201501).respond(0.65)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 201502).respond(0.25)
        for offer_idx in range(offers_count_per_msku):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2015010 + offer_idx).respond(0.6 + offer_idx * 0.04)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2015020 + offer_idx).respond(0.2 + offer_idx * 0.04)

    def test_metadoc_total_pruncount(self):
        """
        Проверяем работу флагов market_metadoc_total_pruncount_cpa_shop_incut
        (суммарное кол-во офферов, которые перебираем во всех sku)
        """
        rearrs_dict = get_default_rearrs_dict()

        def _check_response(flag_name, cgi_params):
            """
            Выставляем flag_name, делаем запрос и проверяем, что в выдаче первый байбокс выбрался правильно,
            а второй неправильно, т.к. лучший оффер во второй байбокс не прошёл из-за прюнинга
            """
            # Выставляем effective_pruncount побольше, чтобы не мешал
            rearrs_dict["market_metadoc_effective_pruncount_cpa_shop_incut"] = 20
            # Значения 7 хватит, чтобы показать обе msku, но не хватит на правильный байбокс во второй msku
            rearrs_dict[flag_name] = 7
            response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))

            # Проверяем, что первый байбокс выбрался правильно, а во втором оффер неоптимальный
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            # Первая msku - правильный байбокс (последний оффер)
                            "wareId": "BLUE-201501-FEED-0005Q",
                            "debug": {
                                "sale": {
                                    "shopFee": 280,
                                },
                            },
                        },
                        {
                            # Вторая msku - неоптимальный байбокс (первый оффер)
                            "wareId": "BLUE-201502-FEED-0001Q",
                            "debug": {
                                "sale": {
                                    "shopFee": 100,
                                },
                            },
                        },
                    ],
                },
                allow_different_len=False,
                preserve_order=True,
            )

            # Ослабляем ограничения, чтобы для второго msku байбокс выбрался правильно
            rearrs_dict[flag_name] = 200
            response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            # Первая msku - правильный байбокс (последний оффер)
                            "wareId": "BLUE-201501-FEED-0005Q",
                            "debug": {
                                "sale": {
                                    "shopFee": 280,
                                },
                            },
                        },
                        {
                            # Вторая msku - правильный байбокс (последний оффер)
                            "wareId": "BLUE-201502-FEED-0005Q",
                            "debug": {
                                "sale": {
                                    "shopFee": 180,
                                },
                            },
                        },
                    ],
                },
                allow_different_len=False,
                preserve_order=True,
            )

        # Проверяем бестекстовый поиск на вебе
        cgi_params = get_cgi_params_dict_cpa_shop_incut(hid=2015)
        cgi_params["client"] = "frontend"
        cgi_params["platform"] = "desktop"
        _check_response("market_metadoc_total_pruncount_textless_web_cpa_shop_incut", cgi_params)

        # Проверяем бестекстовый поиск в аппах
        cgi_params["pp"] = 1809
        cgi_params["client"] = "IOS"
        del cgi_params["platform"]
        _check_response("market_metadoc_total_pruncount_textless_app_cpa_shop_incut", cgi_params)

        # Проверяем текстовый поиск
        cgi_params = get_cgi_params_dict_cpa_shop_incut(text="шнурок")
        _check_response("market_metadoc_total_pruncount_cpa_shop_incut", cgi_params)

    def test_metadoc_effective_and_simple_pruncounts(self):
        """
        Провряем флаги market_metadoc_effective_pruncount_cpa_shop_incut
        и market_metadoc_pruncount_cpa_shop_incut
        Первый задаёт кол-во офферов внутри метадока, из которых выбирается байбокс (после фильтров);
        Второй задаёт кол-во офферов, которые перебираются внутри метадока

        В данном тесте фильтров нет, поэтому флаги должны работать одинаково
        """

        def _check_response(flag_name):
            rearrs_dict = get_default_rearrs_dict()
            # Сначала выставляем большой pruncount, чтобы байбоксы выбирались из всех офферов
            rearrs_dict["market_metadoc_effective_pruncount_cpa_shop_incut"] = 200
            cgi_params = get_cgi_params_dict_cpa_shop_incut(text="шнурок")
            response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))

            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            # Первая msku - правильный байбокс (последний оффер)
                            "wareId": "BLUE-201501-FEED-0005Q",
                            "debug": {
                                "sale": {
                                    "shopFee": 280,
                                },
                            },
                        },
                        {
                            # Вторая msku - правильный байбокс (последний оффер)
                            "wareId": "BLUE-201502-FEED-0005Q",
                            "debug": {
                                "sale": {
                                    "shopFee": 180,
                                },
                            },
                        },
                    ],
                },
                allow_different_len=False,
                preserve_order=True,
            )

            # Снижаем pruncount, чтобы в байбоксы не попали лучшие офферы
            rearrs_dict["market_metadoc_effective_pruncount_cpa_shop_incut"] = 2
            response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            # Первая msku - неоптимальный байбокс (второй оффер)
                            "wareId": "BLUE-201501-FEED-0002Q",
                            "debug": {
                                "sale": {
                                    "shopFee": 220,
                                },
                            },
                        },
                        {
                            # Вторая msku - неоптимальный байбокс (второй оффер)
                            "wareId": "BLUE-201502-FEED-0002Q",
                            "debug": {
                                "sale": {
                                    "shopFee": 120,
                                },
                            },
                        },
                    ],
                },
                allow_different_len=False,
                preserve_order=True,
            )

        # Проверяем флаги
        _check_response("market_metadoc_effective_pruncount_cpa_shop_incut")
        _check_response("market_metadoc_pruncount_cpa_shop_incut")

    @classmethod
    def prepare_metadoc_effective_pruncount(cls):
        """
        Подготавливаем выдачу, которую будем фильтровать (по гарантии),
        после чего из оставшихся в метадоке офферов разыгрывать байбокс.
        Прюнинг effective ограничивает кол-во офферов после фильтров
        """

        # Наполнение выдачи по запросу "кожа"
        cls.index.models += [
            Model(hid=2016, hyperid=2016, ts=2016, title='model_2016', vbid=11),
        ]

        offers_count_with_filter = 4  # 4 оффера, проходящие фильтр по гарантии
        offers_count_no_filter = 4  # 4 оффера, не проходящие фильтр
        blue_offers = []
        timestamps_offers = []
        for offer_idx in range(offers_count_with_filter + offers_count_no_filter):
            needs_warranty = offer_idx % 2 == 0  # Чередуем проставление гарантии
            offer_ts = 2016010 + offer_idx
            blue_offers.append(
                BlueOffer(
                    price=2000,
                    # Чередуем feed_id у офферов
                    feedid=1 if needs_warranty else 2,
                    manufacturer_warranty=needs_warranty,
                    fee=200 + offer_idx * 20,
                    waremd5="BLUE-201601-FEED-000{}Q".format(offer_idx + 1),
                    title="201601 3P offer {} кожа".format(offer_idx + 1),
                    ts=offer_ts,
                )
            )
            timestamps_offers.append(offer_ts)
        cls.index.mskus += [
            MarketSku(
                title="2016 msku_1 кожа",
                hid=2016,
                hyperid=2016,
                sku=201601,
                delivery_buckets=[1234],
                blue_offers=blue_offers,
                ts=201601,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 201601).respond(0.65)
        for offer_idx, offer_ts in enumerate(timestamps_offers):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, offer_ts).respond(0.6 + offer_idx * 0.04)

    def test_metadoc_effective_pruncount(self):
        """
        Проверяем, что metadoc_effective_pruncount для cpa_shop_incut корректно работает с фильтрами
        Чем больше номер оффера внутри msku 201601, тем он "лучше" с точки зрения аукц. байбокса
        """

        def _expect_in_response(response, ware_md5):
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "wareId": ware_md5,
                        },
                    ],
                },
                allow_different_len=False,
            )

        rearrs_dict = get_default_rearrs_dict()
        cgi_params = get_cgi_params_dict_cpa_shop_incut(text="кожа", hid=2016)

        # Выставляем достаточно большой effective_pruncount, чтобы в байбокс попали все офферы
        rearrs_dict["market_metadoc_effective_pruncount_cpa_shop_incut"] = 20
        response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
        _expect_in_response(response, "BLUE-201601-FEED-0008Q")  # Побеждает 8-й оффер по аукциону

        # Выставляем фильтр по гарантии - 8-й оффер отфильтруется и победит 7-й
        cgi_params["manufacturer_warranty"] = "1"
        response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
        _expect_in_response(response, "BLUE-201601-FEED-0007Q")

        # Снижаем effective_pruncount, чтобы в байбокс не попал лучший оффер из фильтрованных
        rearrs_dict["market_metadoc_effective_pruncount_cpa_shop_incut"] = 3
        response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
        _expect_in_response(response, "BLUE-201601-FEED-0005Q")

        # Оставляя effective_pruncount, снижаем обычный metadoc_pruncount,
        # чтобы снизить число доков до фильтров и ещё сильнее ухудшить байбокс
        rearrs_dict["market_metadoc_pruncount_cpa_shop_incut"] = 4
        response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
        _expect_in_response(response, "BLUE-201601-FEED-0003Q")

    @classmethod
    def prepare_buybox_price_filter(cls):
        """
        Выдача для проверки работы ценового порога в байбоксе
        """

        # Наполнение выдачи по запросу "дверь"
        cls.index.models += [
            Model(hid=2017, hyperid=2017, ts=2017, title='model_2017', vbid=11),
        ]

        cls.index.reserveprice_fee += [
            # Выставляем rp_fee
            ReservePriceFee(hyper_id=2017, reserveprice_fee=0.001),
        ]

        cls.index.mskus += [
            MarketSku(
                title="2017 msku_1 дверь",
                hid=2017,
                hyperid=2017,
                sku=201701,
                delivery_buckets=[1234],
                # Выставляем на метадок (msku) литерал has_adv_bid
                set_has_adv_bid_literal=True,
                blue_offers=[
                    BlueOffer(
                        # Первый оффер, дешёвый, без ставки
                        # Он попадёт в байбокс, но в нём отфильтруется, т.к. нет ставки;
                        # Ещё этот оффер не должен проходить порог по rp_fee
                        # (но фильтр по rp применяется после байбокса)
                        price=2000,
                        feedid=1,
                        fee=0,
                        waremd5="BLUE-201701-FEED-0001Q",
                        title="201701 3P offer 1 дверь",
                        ts=2017011,
                    ),
                    BlueOffer(
                        # Второй оффер, дорогой, со ставкой
                        # Проигрывает байбокс из-за порога по цене
                        # Цена отличается от третьего оффера чуть меньше, чем на 110%,
                        # Если порог считать от третьего оффера, то этот оффер не должен фильтроваться по цене
                        price=2380,
                        feedid=1,
                        fee=500,
                        waremd5="BLUE-201701-FEED-0002Q",
                        title="201701 3P offer 2 дверь",
                        ts=2017012,
                    ),
                    BlueOffer(
                        # Третий оффер, дешёвый, со ставкой
                        # Он выигрывает байбокс
                        price=2180,
                        feedid=1,
                        fee=20,
                        waremd5="BLUE-201701-FEED-0003Q",
                        title="201701 3P offer 3 дверь",
                        ts=2017013,
                    ),
                ],
                ts=201701,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 201701).respond(0.65)
        # Первый оффер, дешёвый, без ставки - наименее релевантный запросу
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2017011).respond(0.4)
        # Второй оффер, дорогой, со ставкой - наиболее релевантный запросу
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2017012).respond(0.8)
        # Третий оффер, дешёвый, со ставкой - средняя релевантность
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2017013).respond(0.6)

    def test_buybox_price_filter(self):
        """
        Проверяем, что при поиске по метадокам в байбоксе правильно работает
        фильтрация по ценовому порогу.
        Ценовой порог вычисляется от минимальной цены в байбоксе (и наличие ставки не важно)
        """

        rearrs_dict = get_default_rearrs_dict()
        cgi_params = get_cgi_params_dict_cpa_shop_incut(text="дверь", hid=2017)

        response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        # Выигрывает оффер со средней ценой, т.к. у него есть ставка,
                        # и он проходит порог по мин. цене (и по rp_fee)
                        "wareId": "BLUE-201701-FEED-0003Q",
                        "debug": {
                            "buyboxDebug": {
                                "RejectedOffers": [
                                    {
                                        # Дорогой оффер не проходит порог по цене
                                        # Его цена = 2380
                                        # Самый дешёвый = 2000
                                        # Порог по цене = 2000 * 1.1 = 2200 < 2380
                                        "RejectReason": "TOO_HIGH_PRICE",
                                        "Offer": {
                                            "WareMd5": "BLUE-201701-FEED-0002Q",
                                        },
                                    },
                                    {
                                        # Дешёвый оффер фильтруется в байбоксе, т.к.
                                        # у него нет ставки
                                        # Но он попадает в байбокс и влияет на порог по цене
                                        "RejectReason": "NOBID_PRIORITY",
                                        "Offer": {
                                            "WareMd5": "BLUE-201701-FEED-0001Q",
                                        },
                                    },
                                ],
                            },
                        },
                    },
                ],
            },
        )

    def test_search_traces(self):
        """
        Проверяем работу трейсов поиска в cpa_shop_incut
        """

        rearrs_dict = get_default_rearrs_dict()
        # Включаем трейсы поиска
        rearrs_dict["market_enable_documents_search_trace_cpa_shop_incut"] = 1
        # Офферы, для которых собираем трейсы
        rearrs_dict[
            "market_documents_search_trace_cpa_shop_incut"
        ] = "BLUE-201701-FEED-0003Q,BLUE-201701-FEED-0002Q,BLUE-201701-FEED-0001Q"

        cgi_params = get_cgi_params_dict_cpa_shop_incut(text="дверь", hid=2017)
        response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))

        self.assertFragmentIn(
            response,
            {
                "docs_search_trace_cpa_shop_incut": {
                    "traces": [
                        {
                            "document": "BLUE-201701-FEED-0003Q",
                            "type": "OFFER_BY_WARE_MD5",
                            "in_index": True,
                            "in_accept_doc": False,  # т.к. поиск по метадокам
                            "in_metadoc": True,
                            "best_in_metadoc": True,  # победитель байбокса
                            "in_relevance": True,
                            "passed_relevance": True,
                            "in_rearrange": True,
                            # on_page не проставляется, т.к. в плейсе cpa_shop_incut
                            # неизвестно, окажется ли документ на странице.
                            # Оффер может отфильтроваться, например, из-за демукса
                            "on_page": False,
                        },
                        {
                            "document": "BLUE-201701-FEED-0002Q",
                            "in_metadoc": True,
                            "best_in_metadoc": False,  # проиграл байбокс
                        },
                        {
                            "document": "BLUE-201701-FEED-0001Q",
                            "best_in_metadoc": False,  # проиграл байбокс
                        },
                    ],
                },
            },
        )

        # Проверяем, как трейсы поиска работают на примерах со срабатывающими прюнингами и фильтрами
        cgi_params = get_cgi_params_dict_cpa_shop_incut(text="кожа", hid=2016)
        cgi_params["manufacturer_warranty"] = "1"  # Фильтр по гарантии
        # Выставляем прюнинги, чтобы часть офферов фильтровалась
        rearrs_dict["market_metadoc_effective_pruncount_cpa_shop_incut"] = 3
        rearrs_dict["market_metadoc_pruncount_cpa_shop_incut"] = 4
        # Отслеживаем трейсами поиска все офферы, которые как-то относятся к запросу
        rearrs_dict["market_documents_search_trace_cpa_shop_incut"] = ','.join(
            ["BLUE-201601-FEED-000{}Q".format(offer_idx + 1) for offer_idx in range(8)]
        )

        response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))
        self.assertFragmentIn(
            response,
            {
                "docs_search_trace_cpa_shop_incut": {
                    "traces": [
                        {
                            "document": "BLUE-201601-FEED-0001Q",
                            "type": "OFFER_BY_WARE_MD5",
                            "in_index": True,
                            "in_accept_doc": False,  # т.к. поиск по метадокам
                            "in_metadoc": True,
                            "best_in_metadoc": False,  # проиграл байбокс
                        },
                        {
                            "document": "BLUE-201601-FEED-0002Q",
                            "in_metadoc": True,
                            "in_metadoc_filtered_reason": "WARRANTY",  # отфильтровался по гарантии
                            "best_in_metadoc": False,  # даже не попал в байбокс
                        },
                        {
                            "document": "BLUE-201601-FEED-0003Q",
                            "best_in_metadoc": True,  # выиграл байбокс
                            "in_relevance": True,
                            "passed_relevance": True,
                            "in_rearrange": True,
                        },
                        {
                            "document": "BLUE-201601-FEED-0004Q",
                            "in_metadoc_filtered_reason": "WARRANTY",
                        },
                        {
                            "document": "BLUE-201601-FEED-0005Q",
                            "in_metadoc": True,
                            "best_in_metadoc": False,
                        },
                        {
                            "document": "BLUE-201601-FEED-0006Q",
                            "in_metadoc": False,  # Не прошёл из-за прюнинга
                        },
                        {
                            "document": "BLUE-201601-FEED-0007Q",
                            "in_metadoc": False,  # Не прошёл из-за прюнинга
                        },
                        {
                            "document": "BLUE-201601-FEED-0008Q",
                            "in_metadoc": False,  # Не прошёл из-за прюнинга
                        },
                    ],
                },
            },
        )

    def test_search_traces_with_blender(self):
        """
        Проверяем работу трейсов поиска в cpa_shop_incut,
        когда запрос идёт через блендер
        """
        cgi_params = get_cgi_params_dict_blender(text="скрепка")
        rearrs_dict = get_default_rearrs_dict()
        rearrs_dict["market_enable_documents_search_trace_cpa_shop_incut"] = 1
        # Оффер, для которого собираем трейсы (победитель байбокса)
        rearrs_dict["market_documents_search_trace_cpa_shop_incut"] = "BLUE-200501-FEED-0001Q"
        response = self.report.request_json(T.get_request(cgi_params, rearrs_dict))

        # Информация о трейсах поиска, которую ожидаем
        expected_search_trace = {
            "traces": [
                {
                    "document": "BLUE-200501-FEED-0001Q",
                    "type": "OFFER_BY_WARE_MD5",
                    "in_index": True,
                    "in_accept_doc": False,  # т.к. поиск по метадокам
                    "in_metadoc": True,
                    "best_in_metadoc": True,  # победил байбокс
                    "in_relevance": True,
                    "passed_relevance": True,
                    "in_rearrange": True,
                },
            ],
        }

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "metasearch": {
                        "subrequests": [
                            {
                                # Подзапрос в place=cpa_shop_incut
                                "docs_search_trace_cpa_shop_incut": expected_search_trace,
                            },
                        ],
                    },
                },
            },
        )


if __name__ == '__main__':
    main()
