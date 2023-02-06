#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    BookingAvailability,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    MarketSku,
    Model,
    NavCategory,
    Offer,
    OfferDimensions,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    Vat,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
)
from core.matcher import NoKey, NotEmpty


def min_quantity_json_response_cpa(fesh, cpa, min=None, step=None):
    bundleSettings = (
        {'quantityLimit': {'minimum': min, 'step': step}}
        if min is not None and step is not None
        else NoKey('bundleSettings')
    )

    return {
        'search': {'results': [{'entity': 'offer', 'shop': {'id': fesh}, 'bundleSettings': bundleSettings, 'cpa': cpa}]}
    }


def blue_min_quantity_json_response(waremd5, min=None, step=None):
    return {
        'search': {
            'results': [
                {
                    'offers': {
                        'items': [
                            {
                                'entity': 'offer',
                                'wareId': waremd5,
                                'bundleSettings': {'quantityLimit': {'minimum': min, 'step': step}},
                            }
                        ]
                    }
                }
            ]
        }
    }


def min_quantity_xml_response_cpa(fesh, min=None, step=None, cpa=''):
    return '''
        <offer shop-id="{fesh}">
            <bundle-settings>
                <quantity-limit>
                    <minimum>{min}</minimum>
                    <step>{step}</step>
                </quantity-limit>
            </bundle-settings>
            {cpa}
        </offer>
    '''.format(
        fesh=fesh, min=min, step=step, cpa=cpa
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.regiontree += [Region(rid=1, region_type=Region.CITY)]

        cls.index.models += [Model(hyperid=100, hid=103), Model(hyperid=1000, hid=1000)]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=10003,
                fesh=333,
                regional_options=[RegionalDelivery(rid=1, options=[DeliveryOption(price=10, day_from=1, day_to=2)])],
            ),
            DeliveryBucket(
                bucket_id=10004,
                fesh=334,
                regional_options=[RegionalDelivery(rid=1, options=[DeliveryOption(price=10, day_from=1, day_to=2)])],
            ),
            DeliveryBucket(
                bucket_id=10005,
                fesh=335,
                regional_options=[RegionalDelivery(rid=1, options=[DeliveryOption(price=10, day_from=1, day_to=2)])],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=503, cpa=Shop.CPA_REAL, pickup_buckets=[5001]),
            Shop(fesh=504, cpa=Shop.CPA_REAL),
            Shop(fesh=505, cpa=Shop.CPA_REAL),
            Shop(fesh=1000, datafeed_id=1001, currency=Currency.RUR, cpa=Shop.CPA_REAL, blue=Shop.BLUE_REAL),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
        ]

        cls.index.mskus_for_count_restriction += [1234]

        '''
        Основной офер, имеющий настройки ограничения количества: минимум и шаг
        Нужен для проверки работы флага show-min-quantity=yes/no, а так же пропадания поля cpa, при show-min-quantity=cpa-to-cpc
        '''
        cls.index.offers += [
            Offer(
                min_quantity=2,  # Значения минимума и шага количества для этого офера
                step_quantity=3,
                hyperid=100,  # Основной критерий поиска
                fesh=503,
                price=1000,  # для place=accessories
                cpa=Offer.CPA_REAL,
                feedid=333,  # для place=offerinfo
                offerid=1,
                booking_availabilities=[BookingAvailability(outlet_id=1000, region_id=1, amount=1)],  # для place=geo
                delivery_buckets=[10003],
            ),
        ]

        '''
        Модели и предложения для тестирования accessories
        '''
        cls.index.models += [
            Model(hyperid=1, hid=101, accessories=[100, 101, 102]),
        ]

        cls.index.outlets += [Outlet(point_id=1000, fesh=503, region=1, point_type=Outlet.FOR_STORE)]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=503,
                carriers=[99],
                options=[PickupOption(outlet_id=1000)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=1, hid=101, price=1000, cpa=Offer.CPA_REAL, fesh=503, waremd5="BH8EPLtKmdLQhLUasgaOnA"),
        ]

        '''
        Предложения для проверки сохранности cpa в случае, если min-quantity отсутствует или равна 1
        '''
        cls.index.offers += [
            Offer(
                step_quantity=3,
                hyperid=100,
                fesh=504,
                price=2000,
                cpa=Offer.CPA_REAL,
                feedid=334,
                offerid=1,
                booking_availabilities=[BookingAvailability(outlet_id=1001, region_id=1, amount=1)],  # для place=geo
                delivery_buckets=[10004],
            ),
            Offer(
                min_quantity=1,
                hyperid=100,
                fesh=505,
                price=3000,
                cpa=Offer.CPA_REAL,
                feedid=335,
                offerid=1,
                booking_availabilities=[BookingAvailability(outlet_id=1002, region_id=1, amount=1)],  # для place=geo
                delivery_buckets=[10005],
            ),
        ]

        OFFER_DIMENSIONS = OfferDimensions(length=10, width=20, height=30)

        cls.index.mskus += [
            MarketSku(
                sku=1234,
                hyperid=1000,
                vat=Vat.VAT_10,
                delivery_buckets=[10003],
                blue_offers=[
                    BlueOffer(
                        price=5,
                        waremd5='detskoe_pitanie_banang',
                        feedid=1001,
                        offerid='detskoe.pitanie.banan',
                        stock_store_count=4,
                        weight=1,
                        dimensions=OFFER_DIMENSIONS,
                    ),
                    # Оффер должен быть отфильтрован из-за маленького стока
                    BlueOffer(
                        price=5,
                        waremd5='detskoe_pitanie_kokosg',
                        feedid=1001,
                        offerid='detskoe.pitanie.kokos',
                        stock_store_count=2,
                        weight=1,
                        dimensions=OFFER_DIMENSIONS,
                    ),
                ],
            )
        ]

    def __test_json_place(self, place, request=None, requestSafeCpa=None, cpa='real'):
        '''
        Тестирование одного place, у которого выдача в формате json
        Проверяется:
        1. Запрос без параметра show-min-quantity, возвращающий запись без поля QuantityLimit
        2. Запрос с параметром show-min-quantity=no, возвращающий запись без поля QuantityLimit
        3. Запрос с параметром show-min-quantity=yes, возвращающий запись с полем QuantityLimit
        4. Запрос с параметром show-min-quantity=cpa-to-cpc, возвращающий запись, в которой поле cpa отсутствует, т.к. min-quantity != 1

        5. Запрос с параметром show-min-quantity=cpa-to-cpc, возвращающий запись, в которой поле cpa сохраняется, если поле min-quantity в индексе отсутствует или равно 1
        '''

        if request:
            request += '&place=' + place

            response = self.report.request_json(request)
            self.assertFragmentIn(response, min_quantity_json_response_cpa(fesh=503, cpa=cpa))

            response = self.report.request_json(request + '&show-min-quantity=no')
            self.assertFragmentIn(response, min_quantity_json_response_cpa(fesh=503, cpa=cpa))

            response = self.report.request_json(request + '&show-min-quantity=yes')
            self.assertFragmentIn(response, min_quantity_json_response_cpa(fesh=503, min=2, step=3, cpa=cpa))

            response = self.report.request_json(request + '&show-min-quantity=cpa-to-cpc')
            self.assertFragmentIn(response, min_quantity_json_response_cpa(fesh=503, min=2, step=3, cpa=NoKey('cpa')))

        if requestSafeCpa:
            requestSafeCpa += '&place=' + place

            response = self.report.request_json(requestSafeCpa + '&show-min-quantity=cpa-to-cpc')
            self.assertFragmentIn(response, min_quantity_json_response_cpa(fesh=504, min=1, step=3, cpa='real'))
            self.assertFragmentIn(response, min_quantity_json_response_cpa(fesh=505, min=1, step=1, cpa='real'))

    def __test_xml_place(self, place, request=None, requestSafeCpa=None):
        '''
        Аналогичные запросы для xml ответов
        '''
        if request:
            request += '&place=' + place

            response = self.report.request_xml(request)
            self.assertFragmentNotIn(response, '<quantity-limit/>')

            response = self.report.request_xml(request + '&show-min-quantity=no')
            self.assertFragmentNotIn(response, '<quantity-limit/>')

            response = self.report.request_xml(request + '&show-min-quantity=yes')
            self.assertFragmentIn(response, min_quantity_xml_response_cpa(fesh=503, min=2, step=3, cpa='real'))

            response = self.report.request_xml(request + '&show-min-quantity=cpa-to-cpc')
            self.assertFragmentNotIn(response, '<cpa/>')

        if requestSafeCpa:
            requestSafeCpa += '&place=' + place

            response = self.report.request_xml(requestSafeCpa + '&show-min-quantity=cpa-to-cpc')
            self.assertFragmentIn(response, min_quantity_xml_response_cpa(fesh=504, min=1, step=3, cpa='real'))
            self.assertFragmentIn(response, min_quantity_xml_response_cpa(fesh=505, min=1, step=1, cpa='real'))

    @classmethod
    def prepare_load_values(cls):
        '''
        Данные для проверки загрузки значений по умолчанию.
        1. Нет данных о минимальном количестве и шаге
        2. Есть только минимальное количество
        3. Есть только шаг инкрементировния
        '''
        cls.index.offers += [
            Offer(hyperid=100, fesh=500),
            Offer(hyperid=100, fesh=501, min_quantity=4),
            Offer(hyperid=100, fesh=502, step_quantity=2),
        ]

    def test_load_values(self):
        '''
        Проверка загрузки значений.
        У магазина 500 нет установленных ограничений - данные по умолчанию (1, 1)
        У магазина 501 установлено только минимальное количество. Шаг берется по умолчанию равным 1
        У магазина 502 установлен только шаг. Минимальное количество берется по умолчанию равным 1
        У магазина 503 установлены оба параметра
        '''
        response = self.report.request_json('place=prime&hyperid=100&show-min-quantity=yes')

        self.assertFragmentIn(response, min_quantity_json_response_cpa(fesh=500, min=1, step=1, cpa=NoKey('cpa')))
        self.assertFragmentIn(response, min_quantity_json_response_cpa(fesh=501, min=4, step=1, cpa=NoKey('cpa')))
        self.assertFragmentIn(response, min_quantity_json_response_cpa(fesh=502, min=1, step=2, cpa=NoKey('cpa')))
        self.assertFragmentIn(response, min_quantity_json_response_cpa(fesh=503, min=2, step=3, cpa='real'))

    def test_prime(self):
        self.__test_json_place('prime', 'hyperid=100&fesh=503', 'hyperid=100')
        response = self.report.request_json('place=prime&hyperid=1000&rgb=blue&rearr-factors=get_min_count=1')
        self.assertFragmentIn(response, blue_min_quantity_json_response('detskoe_pitanie_banang', 3, 1))

    def test_offerinfo(self):
        '''
        Проверка выдачи для place=offerinfo
        '''
        self.__test_json_place(
            'offerinfo',
            'feed_shoffer_id=333-1&regset=1&show-urls=external&rids=1',
            'feed_shoffer_id=334-1&feed_shoffer_id=335-1&regset=1&show-urls=external&rids=1',
        )
        response = self.report.request_json(
            'place=offerinfo&market-sku=1234&rgb=blue&pp=18&rearr-factors=get_min_count=1&rids=1&regset=1&show-urls=external'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': 'detskoe_pitanie_banang',
                            'bundleSettings': {'quantityLimit': {'minimum': 3, 'step': 1}},
                        }
                    ]
                }
            },
        )

    def test_geo(self):
        '''
        Проверка выдачи для place=geo
        '''
        self.__test_json_place('geo', 'hyperid=100&regset=1&rids=1', cpa=NoKey('cpa'))

    def test_productoffers(self):
        '''
        Проверка выдачи для place=productoffers
        '''
        self.__test_json_place('productoffers', 'hyperid=100', 'hyperid=100')

    def test_accessories(self):
        '''
        Проверка выдачи для place=accessories
        '''
        self.__test_json_place('accessories', 'hyperid=1&fesh=503&offerid=BH8EPLtKmdLQhLUasgaOnA&price=1000')

        '''
        В этом place нужно запросить два разных запроса для проверки сохранности cpa
        '''
        response = self.report.request_json(
            'place=accessories&hyperid=1&fesh=504&offerid=BH8EPLtKmdLQhLUasgaOnA&price=2000&show-min-quantity=cpa-to-cpc'
        )
        self.assertFragmentIn(response, min_quantity_json_response_cpa(fesh=504, min=1, step=3, cpa='real'))

        response = self.report.request_json(
            'place=accessories&hyperid=1&fesh=505&offerid=BH8EPLtKmdLQhLUasgaOnA&price=2000&show-min-quantity=cpa-to-cpc'
        )
        self.assertFragmentIn(response, min_quantity_json_response_cpa(fesh=505, min=1, step=1, cpa='real'))

    def test_defaultoffer(self):
        '''
        Проверка выдачи для place=defaultoffer
        '''
        self.__test_xml_place('defaultoffer', 'hyperid=100&fesh=503')

        '''
        Для place=defaultoffer нельзя запросить два офера. Поэтому логика проверки сохранности cpa вынесена в отдельные запросы
        '''
        response = self.report.request_xml('place=defaultoffer&hyperid=100&fesh=504&show-min-quantity=cpa-to-cpc')
        self.assertFragmentIn(response, min_quantity_xml_response_cpa(fesh=504, min=1, step=3, cpa='real'))
        response = self.report.request_xml('place=defaultoffer&hyperid=100&fesh=505&show-min-quantity=cpa-to-cpc')
        self.assertFragmentIn(response, min_quantity_xml_response_cpa(fesh=505, min=1, step=1, cpa='real'))

    def test_debug_filter_count_restriction(self):
        '''
        Проверка дебажной выдачи о наличии фильтра оффера по причине отсутствия требуемого количества товара.
        '''
        response = self.report.request_json('place=prime&hyperid=1000&rgb=blue&rearr-factors=get_min_count=1&debug=da')
        self.assertFragmentIn(response, {'debug': {'brief': {'filters': {'OFFER_COUNT_RESTRICTION': 1}}}})

    def test_sku_offers_count_restriction(self):
        '''
        Проверка на плейсе sku_offers, что с оффером доезжают ограничения
        '''
        response = self.report.request_json(
            'place=sku_offers&rgb=blue&pp=18&rearr-factors=get_min_count=1&rids=1&market-sku=1234'
        )
        self.assertFragmentIn(response, blue_min_quantity_json_response('detskoe_pitanie_banang', 3, 1))

    def test_stat_numbers_count_restriction(self):
        '''
        Проверяем, что stat_numbers выдает корректные данные
        '''
        response = self.report.request_json(
            'rgb=blue&pp=18&rearr-factors=get_min_count=1&place=stat_numbers&rids=1&supplier-id=1000'
        )
        self.assertFragmentIn(response, {"result": {"offersCount": 1, "filters": {"OFFER_COUNT_RESTRICTION": 1}}})

    @classmethod
    def prepare_nids_info_count_restriction(cls):
        cls.index.navtree_blue += [NavCategory(nid=1000, hid=1000, name='NavDetPit')]
        cls.index.navtree += [NavCategory(nid=1000, hid=1000, name='NavDetPit')]

    def test_nids_info_count_restriction(self):
        '''
        Проверяем корректную работу nids_info
        '''
        response = self.report.request_json('place=nids_info&rgb=blue&pp=18&rearr-factors=get_min_count=1&rids=1')
        self.assertFragmentIn(response, {'allowedNids': [1000]})

    def test_actual_delivery_count_restriction(self):
        '''
        Проверяем, что если у пользователя был оффер, количество которых меньше 3-х (ограничение в эксперименте), то
        приходт ответ, что оффер разобрали
        '''
        response = self.report.request_json(
            'place=actual_delivery&rgb=blue&pp=18&rearr-factors=get_min_count=1&rids=1&offers-list=detskoe_pitanie_kokosg:1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 0,
                    'offerProblems': [{'wareId': 'detskoe_pitanie_kokosg', 'problems': ['NONEXISTENT_OFFER']}],
                }
            },
        )

        '''А с оффером, у которого стоки более 3 доставка есть'''
        response = self.report.request_json(
            'place=actual_delivery&rgb=blue&pp=18&rearr-factors=get_min_count=1&rids=1&offers-list=detskoe_pitanie_banang:1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "delivery": NotEmpty(),
                            "offers": [
                                {
                                    "entity": "offer",
                                    "wareId": 'detskoe_pitanie_banang',
                                    'bundleSettings': {'quantityLimit': {'minimum': 3, 'step': 1}},
                                }
                            ],
                        }
                    ],
                }
            },
        )


if __name__ == '__main__':
    main()
