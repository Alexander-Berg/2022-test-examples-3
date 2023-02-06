#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CategoryStatsRecord,
    Currency,
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    Shop,
    Tax,
    YamarecCategoryRanksPartition,
    YamarecPlace,
)
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.crypta import CryptaName, CryptaFeature

# ranged list of categories for coldstart hardcoded into report_bin resources

COLDSTART_CATEGORIES = [
    (90799, "Подгузники"),
    (13360751, "Молочные смеси"),
    (90796, "Коляски"),
    (10470548, "Конструкторы"),
    (989042, "Стульчики для кормления"),
    (512743, "Автокресла"),
    (10682550, "Куклы и пупсы"),
    (13360765, "Пюре"),
    (13360738, "Каши"),
    (90589, "Кофеварки и кофемашины"),
    (90586, "Электрочайники и термопоты"),
    (90564, "Пылесосы"),
    (90568, "Утюги"),
    (90569, "Фены и приборы для укладки"),
    (91033, "Жесткие диски, SSD и сетевые накопители"),
    (91013, "Ноутбуки"),
    (6427100, "Планшеты"),
    (138608, "Принтеры и МФУ"),
    (91052, "Мониторы"),
    (90639, "Телевизоры"),
    (90555, "Наушники и Bluetooth-гарнитуры"),
    (91148, "Фотоаппараты"),
    (91491, "Мобильные телефоны"),
    (10498025, "Умные часы и браслеты"),
    (14994593, "Бритвы и лезвия"),
    (13334231, "Зубная паста"),
    (91183, "Шампуни"),
    (15002303, "Туалетная бумага и полотенца"),
    (13314855, "Прокладки и тампоны"),
    (91176, "Для душа"),
    (8476097, "Маски"),
    (8476098, "Очищение и снятие макияжа"),
    (7693914, "Ватные палочки и диски"),
    (91329, "Макароны"),
    (91392, "Чай"),
    (15714102, "Конфеты в коробках, подарочные наборы"),
    (16011677, "Шоколадная плитка"),
    (15368134, "Капсулы для кофемашин"),
    (15726400, "Вода"),
    (15726402, "Лимонады и газированные напитки"),
    (13337703, "Готовые завтраки, мюсли, гранола"),
    (15685457, "Корма для кошек"),
    (15685787, "Корма для собак"),
    (12766642, "Наполнители для кошачьих туалетов"),
    (13518990, "Средства от блох и клещей"),
    (4922657, "Витамины и добавки для кошек и собак"),
    (14245094, "Средства от глистов"),
    (12718255, "Миски, кормушки и поилки"),
    (12718332, "Лакомства для собак"),
    (13196790, "Для посудомоечных машин"),
    (91650, "Шуруповерты"),
    (91610, "Смесители"),
    (90698, "Сковороды и сотейники"),
    (90713, "Лампочки"),
    (90490, "Шины"),
    (6269371, "Видеорегистраторы"),
    (90462, "Радар-детекторы"),
    (90404, "Автомагнитолы"),
    (90478, "Моторные масла"),
    (7815007, "Кроссовки и кеды"),
    (91259, "Наручные часы"),
    (7812158, "Толстовки"),
    (7812157, "Джинсы"),
    (7814994, "Кроссовки и кеды"),
    (7811901, "Платья"),
    (7812201, "Сумки"),
    (7811911, "Домашняя одежда"),
    (7070735, "Самокаты"),
    (91529, "Велосипеды"),
    (14334539, "Спортивное питание"),
    (91522, "Рюкзаки"),
    (1009489, "Пульсометры и шагомеры"),
    (91244, "Электрогитары и бас-гитары"),
    (91243, "Акустические гитары"),
    (5048602, "Микшерные пульты"),
    (91248, "Синтезаторы и MIDI-клавиатуры"),
    (91767, "Кассовые аппараты"),
    (91766, "Холодильное оборудование"),
    (6144280, "Оборудование для автосервисов"),
    (6202209, "Видеокамеры"),
    (1009493, "Оборудование и мебель для медучреждений"),
    (15988962, "Сувениры Сбербанк"),
    (15885450, "Сувениры Яндекс"),
]

# coldstart categories ids
COLDSTART_CATEGORY_IDS = [x[0] for x in COLDSTART_CATEGORIES]
COLDSTART_CATEGORY_IDS_1 = [hid for hid in COLDSTART_CATEGORY_IDS[::-1] if hid != 15885450]
COLDSTART_PERMUTED_CATEGORY_IDS = sorted(COLDSTART_CATEGORY_IDS, key=lambda x: x % 47)


class T(TestCase):
    """
    Набор тестов для выдачи рекомендаций на колдстарте,
    когда нет персональных данных для пользователя
    """

    @classmethod
    def prepare(cls):
        """coldstart categories"""
        cls.index.hypertree += [
            HyperCategory(hid=hid, output_type=HyperCategoryType.GURU)
            for hid in COLDSTART_CATEGORY_IDS[:-1] + [101, 102]
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COLDSTART_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY_RANKS,
                partitions=[
                    YamarecCategoryRanksPartition(category_list=COLDSTART_CATEGORY_IDS, splits=['*']),
                    YamarecCategoryRanksPartition(category_list=COLDSTART_CATEGORY_IDS_1, splits=['14']),
                ],
            )
        ]

        cls.index.shops += [
            Shop(fesh=1, regions=[1]),
            Shop(fesh=2, regions=[2]),
            Shop(
                fesh=1886710,
                datafeed_id=188671001,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
            ),
            Shop(
                fesh=3,
                datafeed_id=1,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
            ),
        ]
        for yandexuid in [11, 12, 13, 14]:
            cls.crypta.on_request_profile(yandexuid=yandexuid).respond(
                features=[
                    CryptaFeature(name=CryptaName.GENDER_FEMALE, value=100),
                ]
            )

    @classmethod
    def prepare_popular_products(cls):
        """
        Yamarec конфигурация с данными для popular_products,
        и настройка ichwill, управляющая флагом coldstart

        Данные содержат модели всех категорий из набора для coldstart
        При этом для каждой категории есть модели, как имеющие офферы в регионе, так и модели без офферов
        """

        # models for normal mode
        cls.index.models += [
            Model(ts=10, hyperid=1, hid=101),
            Model(ts=9, hyperid=2, hid=102),
            Model(ts=8, hyperid=3, hid=COLDSTART_CATEGORY_IDS[0]),
        ]
        cls.index.offers += [
            Offer(hyperid=1, fesh=2),
            Offer(hyperid=2, fesh=2),
            Offer(hyperid=3, fesh=2),
        ]

        # 2 models per category for coldstart
        # 'randomly' permute categories to distinguish from models ts ordering
        all_hids = COLDSTART_PERMUTED_CATEGORY_IDS
        size = len(all_hids)
        model_ids = list(range(1000, 1000 + 2 * size))
        model_hids = []
        for hid in all_hids:
            model_hids += [hid] * 2

        cls.index.models += [
            Model(ts=100500 - i, hyperid=entry[0], hid=entry[1]) for i, entry in enumerate(zip(model_ids, model_hids))
        ]

        # for each category we've got 2 models
        # add offers from two shops:
        #   shop#1 for 2nd
        #   shop#2 for 1st model
        # shop#2 will play role of foreign region shop
        # and we get first model within each category being filtered out
        cls.index.offers += [Offer(hyperid=hyperid, fesh=2 if i % 2 == 0 else 1) for i, hyperid in enumerate(model_ids)]

        # popular_products config
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:12').respond({'models': ['1', '2']})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:12', item_count=40, with_timestamps=True
        ).respond({'models': ['1', '2'], 'timestamps': ['1', '2']})
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:11').respond({'models': []})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:11', item_count=40, with_timestamps=True
        ).respond({'models': [], 'timestamps': []})
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:13').respond({'models': ['3']})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:13', item_count=40, with_timestamps=True
        ).respond({'models': ['3'], 'timestamps': ['1']})
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:14').respond({'models': []})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:14', item_count=40, with_timestamps=True
        ).respond({'models': [], 'timestamps': []})

    def test_popular_products(self):
        """
        Проверка режима работы popular_products для cold start

        Для пользователя 11 coldstart, ожидаем
            1) Модели выводятся в порядке, сооветствующем рангу категории из списка
            2) Не более одной модели в каждой категории
            3) Фильтр по офферам региона всё ещё работает
        """
        response = self.report.request_json(
            'place=popular_products&rearr-factors=split=popular_products_coldstart&yandexuid=11&rids=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        # 3rd models are best
        hid_to_model = dict([hid, 1000 + 2 * i + 1] for i, hid in enumerate(COLDSTART_PERMUTED_CATEGORY_IDS))

        # there is small limit on recom request which cut models before base search and filtering, so make smaller test
        hids = COLDSTART_CATEGORY_IDS[:12]
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'product', 'id': hid_to_model[hid], 'categories': [{'id': hid}]} for hid in hids
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=True,
        )

        """
        Для пользователя 14 ожидаем другой порядок моделей (другое ранжирование категорий)
        """
        response = self.report.request_json(
            'place=popular_products&rearr-factors=split=popular_products_coldstart&yandexuid=14&rids=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        hids_1 = COLDSTART_CATEGORY_IDS_1[:12]
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'product', 'id': hid_to_model[hid], 'categories': [{'id': hid}]} for hid in hids_1
                    ]
                }
            },
            preserve_order=False,
            allow_different_len=True,
        )

        """
        Для пользователя 12 есть данные истории
        Ожидаем все модели из конфигурации и дублирование категории
        """
        response = self.report.request_json(
            'place=popular_products&rearr-factors=split=popular_products&yandexuid=12&rids=2&numdoc=2&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'product', 'id': 1, 'categories': [{'id': 101}]},
                        {'entity': 'product', 'id': 2, 'categories': [{'id': 102}]},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем, что совпадение категорий в models of interest и в колдстарте
        # не приводит к дублям на выдаче (совпадает 91491 и 91072 как аксессуарная)

        response = self.report.request_json(
            'place=popular_products&rearr-factors=split=popular_products_coldstart&yandexuid=13&rids=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'entity': 'product', 'categories': [{'id': hid}]} for hid in hids]}},
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_non_leaf(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=COLDSTART_CATEGORY_IDS[-1],
                children=[
                    HyperCategory(hid=9999, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]
        cls.index.blue_category_region_stat += [
            CategoryStatsRecord(hid=9999, region=213, n_offers=3, n_discounts=3),
        ]
        cls.index.models += [
            Model(hyperid=9999001, hid=9999),
        ]
        # shops
        cls.index.offers += [Offer(hyperid=9999001, fesh=2)]
        blue_offer = BlueOffer(price=1350, offerid='shop_sku_1', feedid=1, waremd5='BlueOffer-1-WithDisc-w')
        cls.index.mskus += [
            MarketSku(
                title='blue_market_sku_1',
                hyperid=9999001,
                sku=1111001,
                waremd5='Sku1-wdDYWsIiLVm1goleg',
                blue_offers=[blue_offer],
            ),
        ]

    def test_non_leaf(self):
        """
        Проверяем, что находятся модели нелистовой категории
        из колдстарта
        """
        hid = COLDSTART_CATEGORY_IDS[-1]
        # rgb=blue
        response = self.report.request_json(
            'place=popular_products&rids=213&hid={parent_hid}&rgb=blue&yandexuid=11&numdoc=100&debug=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'.format(
                parent_hid=hid
            )
        )
        self.assertFragmentIn(response, {'search': {'results': [{'entity': 'product', 'id': 9999001}]}})
        # rgb=green
        response = self.report.request_json(
            'place=popular_products&rids=2&hid={parent_hid}&rgb=green&yandexuid=11&numdoc=100&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'.format(
                parent_hid=hid
            )
        )
        self.assertFragmentIn(response, {'search': {'results': [{'entity': 'product', 'id': 9999001}]}})


if __name__ == '__main__':
    main()
