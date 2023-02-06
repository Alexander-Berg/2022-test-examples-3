#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
    Model,
    UrlType,
    MnPlace,
    VirtualModel,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.types.recommended_fee import RecommendedFee
from core.matcher import Capture, NotEmpty, Contains


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"
    return result


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=1, home_region=213),
            DynamicWarehouseInfo(id=2, home_region=213),
            DynamicWarehouseInfo(id=3, home_region=213),
            DynamicWarehouseInfo(id=4, home_region=213),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=2,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=3,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=4,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=213, warehouses=[1, 2, 3, 4]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=15, shop_delivery_price=15, day_from=1, day_to=2)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=5678,
                carriers=[157],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=15, shop_delivery_price=15, day_from=1, day_to=6)]
                    ),
                ],
            ),
        ]

        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[213],
                warehouse_with_priority=[
                    WarehouseWithPriority(1, 0),
                    WarehouseWithPriority(2, 1),
                    WarehouseWithPriority(3, 1),
                    WarehouseWithPriority(4, 0),
                ],
            )
        ]

        cls.index.shops += [
            Shop(
                fesh=3100 + i,
                datafeed_id=3100 + i,
                priority_region=213,
                regions=[213],
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                warehouse_id=i % 4 + 1,
            )
            for i in range(1, 11)
        ]

    @classmethod
    def prepare_for_virtual_models_on_search(cls):
        # Будем проверять модельные ссылки для виртуальных моделей
        cls.index.virtual_models += [
            VirtualModel(virtual_model_id=100001),
            VirtualModel(virtual_model_id=100002),
            VirtualModel(virtual_model_id=100003),
            VirtualModel(virtual_model_id=100004),
        ]

        # Создаём белые магазины
        cls.index.shops += [
            Shop(
                fesh=4100 + i,
                priority_region=213,
                cpa=Shop.CPA_REAL,
                supplier_type=Shop.THIRD_PARTY,
                name='CPA Shop 266',
            )
            for i in range(4)
        ]

        # Выдача будет формироваться по текстовому запросу 'sausage'
        # Важно, чтобы в выдаче не было обычных моделей (должны быть только виртуальные),
        #   так как хотим проверить модельные клик-ссылки именно виртуальных моделей
        cls.index.offers += [
            # Оффер на виртуальную модель 100001
            Offer(
                title="virt_sausage_100001",
                hid=5000,
                virtual_model_id=100001,
                price=1000,
                fee=200,
                fesh=4100,
                cpa=Offer.CPA_REAL,
                waremd5='sausage100001_4TT-lbqQ',
                delivery_buckets=[1234],
                ts=100001,
            ),
            # Оффер на виртуальную модель 100002
            Offer(
                title="virt_sausage_100002",
                hid=5000,
                virtual_model_id=100002,
                price=1000,
                fee=180,
                fesh=4101,
                cpa=Offer.CPA_REAL,
                waremd5='sausage100002_4TT-lbqQ',
                delivery_buckets=[1234],
                ts=100002,
            ),
            # Оффер на виртуальную модель 100003
            Offer(
                title="virt_sausage_100003",
                hid=5000,
                virtual_model_id=100003,
                price=1000,
                fee=170,
                fesh=4102,
                cpa=Offer.CPA_REAL,
                waremd5='sausage100003_4TT-lbqQ',
                delivery_buckets=[1234],
                ts=100003,
            ),
            # Оффер на виртуальную модель 100004
            Offer(
                title="virt_sausage_100004",
                hid=5000,
                virtual_model_id=100004,
                price=1000,
                fee=160,
                fesh=4103,
                cpa=Offer.CPA_REAL,
                waremd5='sausage100004_4TT-lbqQ',
                delivery_buckets=[1234],
                ts=100004,
            ),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 100001).respond(0.9)
            cls.matrixnet.on_place(place, 100002).respond(0.8)
            cls.matrixnet.on_place(place, 100003).respond(0.7)
            cls.matrixnet.on_place(place, 100004).respond(0.6)

    @classmethod
    def prepare_search_sponsored_places(cls):
        # Хотим, чтобы в трафареты на поиске попали офферы разных цветов,
        # от разных типов поставщиков

        cls.index.shops += [
            Shop(
                fesh=801,
                priority_region=213,
                cpa=Shop.CPA_REAL,
                cis=Shop.CIS_REAL,
                cpc=Shop.CPC_NO,
                name='DSBS магазин',
                warehouse_id=1,
            ),
            Shop(
                fesh=802,
                priority_region=213,
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                cpa=Shop.CPA_REAL,
                blue=Shop.BLUE_REAL,
                name='3P поставщик blue real',
                warehouse_id=2,
            ),
            Shop(
                fesh=803,
                priority_region=213,
                fulfillment_program=False,
                cpa=Shop.CPA_REAL,
                supplier_type=Shop.THIRD_PARTY,
                name='3P поставщик без blue real',
                warehouse_id=3,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                regions=[213],
                name="1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
        ]

        # Для каждого магазина хотим 4 сниппета в выдаче,
        # из них будет 2 трафарета

        # 1p - текстовый поиск по mouse или бестекстовый по hid=5001
        model_id_start = 5100
        models_count_per_shop = 4

        cls.index.models += [
            Model(hid=5001, ts=hyperid, hyperid=hyperid, title='model_mouse_{}'.format(hyperid), vbid=11)
            for hyperid in range(model_id_start, model_id_start + models_count_per_shop)
        ]

        # Проставляем значения базовой и мета формул
        # Тут hyperid совпадает с ts модели
        for pos, hyperid in enumerate(range(model_id_start, model_id_start + models_count_per_shop)):
            for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
                # Значения формул делаем одинаковыми, чтобы было ранжирование по ставке
                cls.matrixnet.on_place(place, hyperid).respond(0.8)

        # Это нужно, чтобы рекомендованные ставки проставлялись 1p-офферам
        # TODO: но всё равно не работает (ставки не прокидываются) - разобраться
        cls.index.hypertree += [
            HyperCategory(hid=5001, output_type=HyperCategoryType.GURULIGHT),
            HyperCategory(hid=1, output_type=HyperCategoryType.GURULIGHT),
        ]

        # Создаём синие 1p оффера
        for model_idx in range(models_count_per_shop):
            hyper_id = model_id_start + model_idx
            msku_id = hyper_id + 100000
            cls.index.mskus += [
                MarketSku(
                    hid=5001,
                    hyperid=hyper_id,
                    delivery_buckets=[1234],
                    sku=msku_id,
                    blue_offers=[
                        # Оффер-победитель байбокса
                        BlueOffer(
                            ts=msku_id,
                            price=1000,
                            feedid=2,
                            title='msku_mouse_{}_pos_{}_offer_winner'.format(msku_id, model_idx + 1),
                            waremd5='OFF1_{}_SKU1_SUP1P_Q'.format(hyper_id),
                        ),
                        # TODO: на данный момент не получилось назначить 1p офферу магазинную ставку,
                        #   когда получится - нужно вернуть подпирающий оффер и в тестах на 1p проверить,
                        #   что выигрывает именно 1p с ненулевой brokered_fee
                        # Оффер, проигрывающий байбокс, от другого поставщика (чтобы выставить другой fee)
                        # BlueOffer(
                        #     ts=msku_id + 1000,
                        #     price=1000,
                        #     feedid=802,
                        #     fee=160,
                        #     title='msku_mouse_{}_pos_{}_offer_loser'.format(msku_id, model_idx + 1),
                        #     waremd5='OFF2_{}_SKU1_SUP1P_Q'.format(hyper_id),
                        # ),
                    ],
                ),
            ]
            # Выставляем рекомендованную ставку (будет поставлена на 1p)
            cls.index.recommended_fee += [RecommendedFee(hyper_id=hyper_id, recommended_bid=0.0220)]

        # Дальше создаём модели с ДО от магазинов остальных типов
        for shop_id, hid, text in zip(
            [801, 802, 803],
            [5002, 5003, 5004],  # Для бестекстового поиска по hid
            ['keyboard', 'processor', 'headphones'],  # Для текстового поиска
        ):
            # Обновляем используемые id-шники моделей
            model_id_start += models_count_per_shop
            # Создаём модели
            cls.index.models += [
                Model(hid=hid, ts=hyperid, hyperid=hyperid, title='model_{}'.format(text), vbid=11)
                for hyperid in range(model_id_start, model_id_start + models_count_per_shop)
            ]

            # Заполняем значения базовой и мета формул для моделей
            # Тут hyperid совпадает с ts модели
            for pos, hyperid in enumerate(range(model_id_start, model_id_start + models_count_per_shop)):
                for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
                    # Значения формул делаем одинаковыми, чтобы было ранжирование по ставке
                    cls.matrixnet.on_place(place, hyperid).respond(0.8)

            # Создаём оффера - по 2 оффера на каждый тип магазина (чтобы был аукцион в байбоксе)
            for pos, hyperid in enumerate(range(model_id_start, model_id_start + models_count_per_shop)):
                cls.index.offers += [
                    # Оффер, выигрывающий "текущий" байбокс
                    Offer(
                        hid=hid,
                        hyperid=hyperid,
                        price=1000,
                        title='{}_{}_pos_{}_winner'.format(text, hyperid, pos + 1),
                        cpa=Offer.CPA_REAL,
                        fesh=shop_id,
                        # Ставки должны убывать от позиции, чтобы был нужный порядок
                        fee=200 - pos * 20,
                        ts=200000 + hyperid,
                        delivery_buckets=[1234],
                        waremd5='{}_winner___4TT-lbqQ'.format(hyperid),
                    ),
                    # Оффер, проигрывающий "текущий" байбокс
                    Offer(
                        hid=hid,
                        hyperid=hyperid,
                        price=1000,
                        title='{}_{}_pos_{}_loser'.format(text, hyperid, pos + 1),
                        cpa=Offer.CPA_REAL,
                        fesh=shop_id,
                        fee=200 - pos * 20 - 10,
                        ts=210000 + hyperid,
                        delivery_buckets=[1234],
                        waremd5='{}_loser____4TT-lbqQ'.format(hyperid),
                    ),
                ]

    def check_search_sponsored_places(
        self,
        text_to_request=None,
        hid_to_request=None,
        rearr_flags_dict={},
        allow_all_zero_brokered_fees=False,
        virtual_models=False,
    ):
        """
        Служебная функция, котора позволяет конкретного текста (или hid) сформировать поисковый запрос,
        распарсить выдачу и сверить выдачу со сгенерированными модельными ссылками.
        Парсим только трафареты, так как аукцион именно в трафаретах.

        Нужно для модели атрибуции в рекламе https://st.yandex-team.ru/MADV-1233

        Если задан text_to_request, то подставим его в text=
        Если задан hid_to_request, то подставим его в hid=

        Флажок allow_all_zero_brokered_fees нужен, чтобы ослабить условия проверки - сейчас из-за бага
        brokered_fee нулевые у офферов под виртуальными моделями. К тому же не удаётся прокинуть shop_fee на 1p офферы.
        Когда баги пофиксим, флажок нужно убрать

        Флажок virtual_models нужен, чтобы не проверять именно encrypted-модельные ссылки у виртуальных моделей

        Запрос отправляем в блендер, чтобы сразу проверить, что ничего не потерялось по пути

        Функция осуществляет проверки и возвращает пару (response, sponsored_offers_cnt), где:
            - response - сам ответ, который можно продолжить проверять
            - sponsored_offers_cnt - кол-во спонсорских документов в этом ответе
        """

        text_part = '&text={}'.format(text_to_request) if text_to_request is not None else ''
        hid_part = '&hid={}'.format(hid_to_request) if hid_to_request is not None else ''
        request = (
            'pp=7&place=prime&blender=1&rgb=green_with_blue&rids=213'
            '&show-urls=productVendorBid,cpa&bsformat=2&viewtype=list'
            '&puid=12345&uuid=789001'  # Нужно проверить, что puid'ы и uuid'ы попадают в клик-лог
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            + text_part
            + hid_part
            + '&rearr-factors='
            + dict_to_rearr(rearr_flags_dict)
        )
        response = self.report.request_json(request)
        # Сначала хотим узнать кол-во офферов в трафаретах, чтобы правильно их распарсить
        # Для этого надо обойти выдачу и посчитать кол-во sponsored-моделей
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": NotEmpty(),
                },
            },
        )
        sponsored_offers_cnt = 0  # Счётчик спонсорских документов в трафаретах
        results = response["search"]["results"]
        for product in results:
            if "sponsored" in product and product["sponsored"]:
                sponsored_offers_cnt += 1

        # Парсим характеристики офферов в трафаретах
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
            for _ in range(sponsored_offers_cnt)
        ]
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "sponsored": True,  # Парсим только оффера из трафаретов
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
            },
        )

        # TODO: когда разберёмся, из-за чего списанные ставки у офферов под виртуальными моделями
        # нулевые, надо всегда проверять, что есть ненулевой brokered_fee
        if not allow_all_zero_brokered_fees and not virtual_models:
            # Смотрим, что индекс для теста правильно сконфигурирован -
            #   есть мерчовые списанные ставки
            has_brokered_fees = False
            for capture_dict in capture_list:
                if capture_dict["shop_fee_ab"].value:
                    has_brokered_fees = True
                    break
            self.assertTrue(has_brokered_fees)  # Тест корректный, если есть списанные ставки

        # Проверяем, что требуемые характеристики офферов попали в record (и попали в shows-log)
        for capture_dict in capture_list:
            # Ссылки типа MODEL (url_type = 16) пишутся в shows-log
            self.show_log.expect(
                url_type=UrlType.MODEL,
                # Вытаскиваем value из capture-ов и передаем как kwargs
                **{prop: capture_dict[prop].value for prop in capture_dict}
            ).once()  # С помощью once проверяем, что нет дублей

        # Проверяем, что требуемые характеристики офферов попали в encrypted-ссылку (модельная ссылка)
        if not virtual_models:
            for capture_dict in capture_list:
                # В ссылке id магазина пишется именно как shop_id, поэтому в словарике меняем ключ
                # original_shop_id на shop_id
                capture_dict["shop_id"] = capture_dict["original_shop_id"]
                del capture_dict["original_shop_id"]
                # Удаляем ware_md5, чтобы не проверять его наличие в ссылке
                del capture_dict["ware_md5"]
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "urls": {
                                    "encrypted": Contains(
                                        'url_type={}'.format(UrlType.MODEL),
                                        'puid=12345',  # Проверяем, что puid попадает в лог
                                        'uuid=789001',
                                        # Проверяем, что все характеристики, которые парсили, есть в ссылке
                                        # Выражение развернётся в список аргументов-строк вида:
                                        # 'field_1=value_1', 'field_2=value_2', ...
                                        *[str(prop) + '=' + str(capture_dict[prop].value) for prop in capture_dict]
                                    ),
                                },
                            }
                            for capture_dict in capture_list
                        ],
                    },
                },
            )

        # Возвращаем ответ и кол-во трафаретов, чтобы тест, запустивший эту проверку,
        # мог допроверять ответ в зависимости от своей специфики запроса
        return response, sponsored_offers_cnt

    def test_for_virtual_models_on_search(self):
        """
        Проверяем, что в vendor-clicks-log в модельной клик-ссылке (для виртуальной модели)
        отправляется мерчовая информация
        """

        # Прокидываем флажок allow_all_zero_brokered_fees=True, так как по запросу sausage в этом индексе
        # в трафареты должны попадать оффера с виртуальными моделями, для которых brokered_fee
        # почему-то нулевые
        self.check_search_sponsored_places(
            text_to_request='sausage',
            allow_all_zero_brokered_fees=True,
            virtual_models=True,
        )

    def test_for_usual_models_on_search_dsbs_text(self):
        """
        Проверяем, что в vendor-clicks-log в модельной клик-ссылке отправляется мерчовая
        информация.
        Проверяем, когда оффера в выдаче от dsbs-магазинов
        Проверяем для текстового поиска
        """

        self.check_search_sponsored_places(text_to_request='keyboard')

    def test_for_usual_models_on_search_dsbs_textless(self):
        """
        Проверяем, что в vendor-clicks-log в модельной клик-ссылке отправляется мерчовая
        информация.
        Проверяем, когда оффера в выдаче от dsbs-магазинов
        Проверяем для бестекстового поиска
        """

        self.check_search_sponsored_places(hid_to_request=5002)

    def test_for_usual_models_on_search_1p_text(self):
        """
        Проверяем, что в vendor-clicks-log в модельной клик-ссылке отправляется мерчовая
        информация.
        Проверяем, когда оффера в выдаче от 1p-магазинов
        Проверяем для текстового поиска
        """

        # На 1p оффер не получается прокинуть shop_fee, поэтому не будем требовать ненулевых brokered_fee
        self.check_search_sponsored_places(text_to_request='mouse', allow_all_zero_brokered_fees=True)

    def test_for_usual_models_on_search_1p_textless(self):
        """
        Проверяем, что в vendor-clicks-log в модельной клик-ссылке отправляется мерчовая
        информация.
        Проверяем, когда оффера в выдаче от 1p-магазинов
        Проверяем для бестекстового поиска
        """

        # На 1p оффер не получается прокинуть shop_fee, поэтому не будем требовать ненулевых brokered_fee
        self.check_search_sponsored_places(hid_to_request=5001, allow_all_zero_brokered_fees=True)

    def test_for_usual_models_on_search_3p_blue_text(self):
        """
        Проверяем, что в vendor-clicks-log в модельной клик-ссылке отправляется мерчовая
        информация.
        Проверяем, когда оффера в выдаче от 3p-магазинов (синих)
        Проверяем для текстового поиска
        """

        self.check_search_sponsored_places(text_to_request='processor')

    def test_for_usual_models_on_search_3p_blue_textless(self):
        """
        Проверяем, что в vendor-clicks-log в модельной клик-ссылке отправляется мерчовая
        информация.
        Проверяем, когда оффера в выдаче от 3p-магазинов (синих)
        Проверяем для бестекстового поиска
        """

        self.check_search_sponsored_places(hid_to_request=5003)

    def test_for_usual_models_on_search_3p_white_text(self):
        """
        Проверяем, что в vendor-clicks-log в модельной клик-ссылке отправляется мерчовая
        информация.
        Проверяем, когда оффера в выдаче от 3p-магазинов
        Проверяем для текстового поиска
        """

        self.check_search_sponsored_places(text_to_request='headphones')

    def test_for_usual_models_on_search_3p_white_textless(self):
        """
        Проверяем, что в vendor-clicks-log в модельной клик-ссылке отправляется мерчовая
        информация.
        Проверяем, когда оффера в выдаче от 3p-магазинов
        Проверяем для бестекстового поиска
        """

        self.check_search_sponsored_places(hid_to_request=5004)


if __name__ == '__main__':
    main()
