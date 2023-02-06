#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BookingAvailability,
    ClickType,
    DeliveryBucket,
    DynamicBookNowOffer,
    DynamicShop,
    GLParam,
    GLType,
    HyperCategory,
    Model,
    NewShopRating,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
)
from core.testcase import TestCase, main

from core.matcher import LikeUrl, NotEmpty
from unittest import skip


def prepare_book_now_data(cls):
    # rids: [1, 100]
    # fesh: [101, 200]
    # outlet id: [201, 300]
    # hyperid: [301, 400]

    cls.index.regiontree += [
        Region(
            rid=1,
            name='Нарния',
            region_type=Region.FEDERATIVE_SUBJECT,
            children=[
                Region(rid=3, name='Москва', children=[Region(rid=2, name='Таганка')]),
                Region(rid=4, name='Н-ск'),
                Region(rid=5, name='Село1'),
                # Для тестирования врезки https://st.yandex-team.ru/MARKETOUT-7990
                Region(rid=6, name='Село2'),
                # Для тестирования «батарейки» https://st.yandex-team.ru/MARKETOUT-8430
                Region(rid=9, name='Б. Васюки'),
            ],
        ),
    ]

    cls.settings.create_blue_shard = False
    cls.settings.use_no_snippet_arc = False

    cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']

    cls.index.shops += [
        Shop(fesh=101, priority_region=3, regions=[3, 5], name='Сеть Волшебные Товары'),
        Shop(fesh=102, priority_region=1),
        Shop(fesh=103, priority_region=3, regions=[3], name='Сеть Не Совсем Волшебные Товары'),
        # Для тестирования врезки https://st.yandex-team.ru/MARKETOUT-7990
        Shop(
            fesh=104,
            priority_region=6,
            regions=[6],
            name='Сельмаг1',
            new_shop_rating=NewShopRating(new_rating_total=5.0),
        ),
        Shop(
            fesh=105,
            priority_region=6,
            regions=[6],
            name='Сельмаг2',
            new_shop_rating=NewShopRating(new_rating_total=3.0),
        ),
        Shop(fesh=106, priority_region=6, regions=[6], name='Сельмаг3'),
        Shop(fesh=107, priority_region=6, regions=[6], name='Сельмаг4'),
        Shop(fesh=108, priority_region=6, regions=[6], name='Сельмаг5-не-в-программе'),
        # Для тестирования «батарейки» https://st.yandex-team.ru/MARKETOUT-8430
        Shop(fesh=130, priority_region=9, regions=[9], name='МирБатареек0'),
        Shop(fesh=131, priority_region=9, regions=[9], name='МирБатареек1'),
        Shop(fesh=132, priority_region=9, regions=[9], name='МирБатареек2'),
        Shop(fesh=133, priority_region=9, regions=[9], name='МирБатареек3'),
        Shop(fesh=134, priority_region=9, regions=[9], name='МирБатареек4'),
        Shop(fesh=135, priority_region=9, regions=[9], name='МирБатареек5'),
        Shop(fesh=136, priority_region=9, regions=[9], name='МирБатареек6'),
        Shop(fesh=137, priority_region=9, regions=[9], name='МирБатареек7'),
        Shop(fesh=138, priority_region=9, regions=[9], name='МирБатареек8'),
        Shop(fesh=139, priority_region=9, regions=[9], name='МирБатареек9'),
        # Для тестирования батарейки на карте при наличии двух офферов в одном аутлете
        # https://st.yandex-team.ru/MARKETOUT-9230#1473756420000
        Shop(fesh=150, priority_region=10, regions=[10], name='Weasleys\' Wizard Wheezes'),
        Shop(fesh=151, priority_region=10, regions=[10], name='Zonko\'s Joke Shop'),
    ]

    o1 = Outlet(point_id=201, fesh=101, region=3, point_type=Outlet.FOR_STORE)
    o2 = Outlet(point_id=202, fesh=102, region=4, point_type=Outlet.FOR_STORE)
    o3 = Outlet(point_id=203, fesh=102, region=5, point_type=Outlet.FOR_STORE)
    o4 = Outlet(point_id=204, fesh=103, region=3, point_type=Outlet.FOR_STORE)
    o5 = Outlet(point_id=205, fesh=103, region=3, point_type=Outlet.FOR_STORE)
    o6 = Outlet(point_id=206, fesh=103, region=3, point_type=Outlet.FOR_STORE)

    # Для тестирования врезки https://st.yandex-team.ru/MARKETOUT-7990
    o7 = Outlet(point_id=207, fesh=104, region=6, point_type=Outlet.FOR_STORE)
    o8 = Outlet(point_id=208, fesh=104, region=6, point_type=Outlet.FOR_STORE, working_days=[0])
    o9 = Outlet(point_id=209, fesh=105, region=6, point_type=Outlet.FOR_STORE)
    o10 = Outlet(point_id=210, fesh=105, region=6, point_type=Outlet.FOR_STORE, working_days=[0])
    o11 = Outlet(point_id=211, fesh=106, region=6, point_type=Outlet.FOR_STORE)
    o12 = Outlet(point_id=212, fesh=107, region=6, point_type=Outlet.FOR_STORE, working_days=[0])
    o13 = Outlet(point_id=213, fesh=107, region=6, point_type=Outlet.FOR_STORE)

    # Для тестирования «батарейки» https://st.yandex-team.ru/MARKETOUT-8430
    # Первые пять магазинов имеют по одному аутлету. Остальные по два.
    # Так мы проверим, что количества честно суммируются по аутлетам
    o230 = Outlet(point_id=230, fesh=130, region=9, point_type=Outlet.FOR_STORE)
    o231 = Outlet(point_id=231, fesh=131, region=9, point_type=Outlet.FOR_STORE)
    o232 = Outlet(point_id=232, fesh=132, region=9, point_type=Outlet.FOR_STORE)
    o233 = Outlet(point_id=233, fesh=133, region=9, point_type=Outlet.FOR_STORE)
    o234 = Outlet(point_id=234, fesh=134, region=9, point_type=Outlet.FOR_STORE)
    o235 = Outlet(point_id=235, fesh=135, region=9, point_type=Outlet.FOR_STORE)
    o2352 = Outlet(point_id=2352, fesh=135, region=9, point_type=Outlet.FOR_STORE)
    o236 = Outlet(point_id=236, fesh=136, region=9, point_type=Outlet.FOR_STORE)
    o2362 = Outlet(point_id=2362, fesh=136, region=9, point_type=Outlet.FOR_STORE)
    o237 = Outlet(point_id=237, fesh=137, region=9, point_type=Outlet.FOR_STORE)
    o2372 = Outlet(point_id=2372, fesh=137, region=9, point_type=Outlet.FOR_STORE)
    o238 = Outlet(point_id=238, fesh=138, region=9, point_type=Outlet.FOR_STORE)
    o2382 = Outlet(point_id=2382, fesh=138, region=9, point_type=Outlet.FOR_STORE)
    o239 = Outlet(point_id=239, fesh=139, region=9, point_type=Outlet.FOR_STORE)
    o2392 = Outlet(point_id=2392, fesh=139, region=9, point_type=Outlet.FOR_STORE)

    # Для тестирования батарейки на карте при наличии двух офферов в одном аутлете
    # https://st.yandex-team.ru/MARKETOUT-9230#1473756420000
    o240 = Outlet(point_id=240, fesh=150, region=10, point_type=Outlet.FOR_STORE)  # Здесь будет два оффера
    o241 = Outlet(point_id=241, fesh=151, region=10, point_type=Outlet.FOR_STORE)  # А здесь один, просто для массовки

    cls.index.outlets += [
        o1,
        o2,
        o3,
        o4,
        o5,
        o6,
        o7,
        o8,
        o9,
        o10,
        o11,
        o12,
        o13,  # https://st.yandex-team.ru/MARKETOUT-7990
        # https://st.yandex-team.ru/MARKETOUT-8430
        o230,
        o231,
        o232,
        o233,
        o234,
        o235,
        o2352,
        o236,
        o2362,
        o237,
        o2372,
        o238,
        o2382,
        o239,
        o2392,
        o240,
        o241,  # https://st.yandex-team.ru/MARKETOUT-9230#1473756420000
    ]

    cls.index.hypertree += [
        HyperCategory(hid=501, name='Вещи'),
        HyperCategory(hid=508, name='Книги', booking_enabled=True, quantity_threshold_low=5, quantity_threshold_high=8),
        # https://st.yandex-team.ru/MARKETOUT-9230#1473756420000
        HyperCategory(
            hid=510, name='Шуточные товары', booking_enabled=True, quantity_threshold_low=20, quantity_threshold_high=50
        ),
    ]

    cls.index.models += [
        Model(hyperid=301, title='Палочка-выручалочка (акваланг)', hid=501),
        Model(hyperid=302, title='Жаба'),
        Model(hyperid=303, title='Сова 5s 2кг+ белая'),
        Model(hyperid=304, title='Котел колдовской для колдовства. 3 литра/медь/с ручкой'),
        Model(hyperid=305, title='Ковырялка ручная (акваланг)'),
        Model(hyperid=306, title='Невидимая книга о Невидимости'),
        Model(hyperid=307, title='Мантия великолепная'),
        # https://st.yandex-team.ru/MARKETOUT-7990
        Model(hyperid=308, title='Книга-монстр о монстрах', hid=508),
        Model(hyperid=309, title='Фантастические звери: места обитания', hid=508),
        # https://st.yandex-team.ru/MARKETOUT-8430
        Model(hyperid=330, title='Зарядное устройство универсальное'),
        # https://st.yandex-team.ru/MARKETOUT-9230#1473756420000
        Model(hyperid=340, title='You-No-Poo', hid=510),
    ]

    cls.index.gltypes += [
        GLType(param_id=1201, hid=501, gltype=GLType.ENUM, cluster_filter=True),
        GLType(param_id=1202, hid=501, gltype=GLType.ENUM, cluster_filter=True),
    ]

    cls.index.offers += [
        Offer(fesh=101, title='Ковырялка садовая (акваланг)', hyperid=305, pickup_buckets=[5001]),
        Offer(fesh=101, title='Невидимая книга о Невидимости в магазине1', hyperid=306, pickup_buckets=[5001]),
        Offer(
            fesh=101,
            title='Палочка-выручалочка x (акваланг)',
            hyperid=301,
            booking_availabilities=[
                BookingAvailability(outlet_id=o1.point_id, region_id=o1.region_id, amount=5),
            ],
            glparams=[GLParam(param_id=1201, value=1), GLParam(param_id=1202, value=2)],
            pickup_buckets=[5001],
        ),
        Offer(
            fesh=102,
            title='Жаба x',
            hyperid=302,
            booking_availabilities=[
                BookingAvailability(outlet_id=o2.point_id, region_id=o2.region_id, amount=3),
            ],
            pickup_buckets=[5002],
        ),
        Offer(
            fesh=102,
            title='Совушка x',
            hyperid=303,
            cmagic='6b77b8385d6b9a7fe05df9b0afc03eae',
            booking_availabilities=[
                BookingAvailability(outlet_id=o2.point_id, region_id=o2.region_id, amount=1),
                BookingAvailability(outlet_id=o3.point_id, region_id=o3.region_id, amount=2),
            ],
            pickup_buckets=[5002],
        ),
        Offer(
            fesh=102,
            title='Котелок x',
            hyperid=304,
            booking_availabilities=[
                BookingAvailability(outlet_id=o1.point_id, region_id=o1.region_id, amount=15),
                BookingAvailability(outlet_id=o3.point_id, region_id=o3.region_id, amount=20),
            ],
            pickup_buckets=[5002],
        ),
        # Для тестрования врезки https://st.yandex-team.ru/MARKETOUT-7990
        Offer(
            fesh=104,
            title='Книга-монстр в сельмаге1',
            hyperid=308,
            price=50,
            booking_availabilities=[
                BookingAvailability(outlet_id=o7.point_id, region_id=o7.region_id, amount=1),
                BookingAvailability(outlet_id=o8.point_id, region_id=o8.region_id, amount=2),
            ],
            # [!] специфичное значение gl-параметра
            glparams=[
                GLParam(param_id=1201, value=555),
                GLParam(param_id=1201, value=1),
                GLParam(param_id=1202, value=2),
            ],
            pickup_buckets=[5004],
        ),
        Offer(
            fesh=105,
            title='Книга-монстр недорагая в сельмаге2',
            hyperid=308,
            price=60,
            booking_availabilities=[
                BookingAvailability(outlet_id=o9.point_id, region_id=o9.region_id, amount=3),
            ],
            glparams=[GLParam(param_id=1201, value=1), GLParam(param_id=1202, value=2)],
            pickup_buckets=[5005],
        ),
        Offer(
            fesh=105,
            title='Книга-монстр дорогая в сельмаге2',
            hyperid=308,
            price=1500,
            booking_availabilities=[
                BookingAvailability(outlet_id=o10.point_id, region_id=o10.region_id, amount=5),
            ],
            glparams=[GLParam(param_id=1201, value=1), GLParam(param_id=1202, value=2)],
            pickup_buckets=[5005],
        ),
        Offer(
            fesh=106,
            title='Книга-монстр в сельмаге3',
            hyperid=308,
            price=70,
            booking_availabilities=[
                BookingAvailability(outlet_id=o11.point_id, region_id=o11.region_id, amount=8),
            ],
            glparams=[GLParam(param_id=1201, value=1), GLParam(param_id=1202, value=2)],
            pickup_buckets=[5006],
        ),
        Offer(
            fesh=107,
            title='Книга-монстр дешевая в сельмаге4',
            hyperid=308,
            price=80,
            booking_availabilities=[
                BookingAvailability(outlet_id=o12.point_id, region_id=o12.region_id, amount=13),
                BookingAvailability(outlet_id=o13.point_id, region_id=o13.region_id, amount=21),
            ],
            glparams=[GLParam(param_id=1201, value=1), GLParam(param_id=1202, value=2)],
            pickup_buckets=[5007],
        ),
        Offer(
            fesh=107,
            title='Книга-монстр подороже в сельмаге4',
            hyperid=308,
            price=120,
            booking_availabilities=[
                BookingAvailability(outlet_id=o12.point_id, region_id=o12.region_id, amount=13),
                BookingAvailability(outlet_id=o13.point_id, region_id=o13.region_id, amount=21),
            ],
            glparams=[GLParam(param_id=1201, value=1), GLParam(param_id=1202, value=2)],
            pickup_buckets=[5007],
        ),
        Offer(
            fesh=108,
            title='Книга-монстр зеленая в сельмаге5 не в программе',
            hyperid=308,
            glparams=[GLParam(param_id=1201, value=1), GLParam(param_id=1202, value=2)],
        ),
        Offer(
            fesh=108,
            title='Книга-монстр синяя в сельмаге5 не в программе',
            hyperid=308,
            glparams=[GLParam(param_id=1201, value=1), GLParam(param_id=1202, value=2)],
        ),
        Offer(
            fesh=108,
            title='Книга-монстр красная в сельмаге5 не в программе',
            hyperid=308,
            glparams=[GLParam(param_id=1201, value=1), GLParam(param_id=1202, value=2)],
        ),
        Offer(
            fesh=108,
            title='Книга-монстр желтая в сельмаге5 не в программе',
            hyperid=308,
            glparams=[GLParam(param_id=1201, value=1), GLParam(param_id=1202, value=2)],
        ),
        Offer(
            fesh=108,
            title='Книга-монстр фиолетовая в сельмаге5 не в программе',
            hyperid=308,
            glparams=[GLParam(param_id=1201, value=1), GLParam(param_id=1202, value=2)],
        ),
        Offer(
            fesh=108,
            title='Книга-монстр сиреневая в сельмаге5 не в программе',
            hyperid=308,
            glparams=[GLParam(param_id=1201, value=1), GLParam(param_id=1202, value=2)],
        ),
        Offer(
            fesh=108,
            title='Книга-монстр черная! в сельмаге5 не в программе',
            hyperid=308,
            glparams=[GLParam(param_id=1201, value=1), GLParam(param_id=1202, value=2)],
        ),
        Offer(
            fesh=103,
            title='Мантия великолепная (на самом деле нет)',
            hyperid=307,
            booking_availabilities=[
                BookingAvailability(outlet_id=o5.point_id, region_id=o5.region_id, amount=5),
            ],
            pickup_buckets=[5003],
        ),
        Offer(
            fesh=103,
            title='Мантия великолепная (плюс восхитительные штаны)',
            hyperid=307,
            booking_availabilities=[
                BookingAvailability(outlet_id=o6.point_id, region_id=o6.region_id, amount=42),
            ],
            pickup_buckets=[5003],
        ),
        # Для тестирования «батарейки» https://st.yandex-team.ru/MARKETOUT-8430
        # у первых пяти магазинов оффер лежит в единственном их аутлете
        # А у следующих пяти аутлетов два и оффер есть и там и там
        Offer(
            fesh=130,
            title='Зарядник',
            hyperid=330,
            booking_availabilities=[BookingAvailability(outlet_id=o230.point_id, region_id=9, amount=1)],
            pickup_buckets=[5030],
        ),
        Offer(
            fesh=131,
            title='Зарядник',
            hyperid=330,
            booking_availabilities=[BookingAvailability(outlet_id=o231.point_id, region_id=9, amount=2)],
            pickup_buckets=[5031],
        ),
        Offer(
            fesh=132,
            title='Зарядник',
            hyperid=330,
            booking_availabilities=[BookingAvailability(outlet_id=o232.point_id, region_id=9, amount=3)],
            pickup_buckets=[5032],
        ),
        Offer(
            fesh=133,
            title='Зарядник',
            hyperid=330,
            booking_availabilities=[BookingAvailability(outlet_id=o233.point_id, region_id=9, amount=4)],
            pickup_buckets=[5033],
        ),
        Offer(
            fesh=134,
            title='Зарядник',
            hyperid=330,
            booking_availabilities=[BookingAvailability(outlet_id=o234.point_id, region_id=9, amount=5)],
            pickup_buckets=[5034],
        ),
        Offer(
            fesh=135,
            title='Зарядник',
            hyperid=330,
            booking_availabilities=[
                BookingAvailability(outlet_id=o235.point_id, region_id=9, amount=5),
                BookingAvailability(outlet_id=o2352.point_id, region_id=9, amount=1),
            ],
            pickup_buckets=[5035],
        ),
        Offer(
            fesh=136,
            title='Зарядник',
            hyperid=330,
            booking_availabilities=[
                BookingAvailability(outlet_id=o236.point_id, region_id=9, amount=5),
                BookingAvailability(outlet_id=o2362.point_id, region_id=9, amount=2),
            ],
            pickup_buckets=[5036],
        ),
        Offer(
            fesh=137,
            title='Зарядник',
            hyperid=330,
            booking_availabilities=[
                BookingAvailability(outlet_id=o237.point_id, region_id=9, amount=5),
                BookingAvailability(outlet_id=o2372.point_id, region_id=9, amount=3),
            ],
            pickup_buckets=[5037],
        ),
        Offer(
            fesh=138,
            title='Зарядник',
            hyperid=330,
            booking_availabilities=[
                BookingAvailability(outlet_id=o238.point_id, region_id=9, amount=5),
                BookingAvailability(outlet_id=o2382.point_id, region_id=9, amount=4),
            ],
            pickup_buckets=[5038],
        ),
        Offer(
            fesh=139,
            title='Зарядник',
            hyperid=330,
            booking_availabilities=[
                BookingAvailability(outlet_id=o239.point_id, region_id=9, amount=5),
                BookingAvailability(outlet_id=o2392.point_id, region_id=9, amount=5),
            ],
            pickup_buckets=[5039],
        ),
        # Для тестирования батарейки на карте при наличии двух офферов в одном аутлете
        # https://st.yandex-team.ru/MARKETOUT-9230#1473756420000
        Offer(
            fesh=150,
            title='You-no-poo #1',
            hyperid=340,
            booking_availabilities=[
                BookingAvailability(outlet_id=o240.point_id, region_id=o240.region_id, amount=30),
            ],
            pickup_buckets=[5050],
            randx=2,
        ),
        Offer(
            fesh=150,
            title='You-no-poo #2',
            hyperid=340,
            booking_availabilities=[
                BookingAvailability(outlet_id=o240.point_id, region_id=o240.region_id, amount=8),
            ],
            pickup_buckets=[5050],
            randx=1,
        ),
        Offer(
            fesh=151,
            title='You-no-poo (replica)',
            hyperid=340,
            booking_availabilities=[
                BookingAvailability(outlet_id=o241.point_id, region_id=o241.region_id, amount=135),
            ],
            pickup_buckets=[5051],
        ),
    ]

    cls.index.pickup_buckets += [
        PickupBucket(
            bucket_id=5001,
            fesh=101,
            carriers=[99],
            options=[PickupOption(outlet_id=201)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5002,
            fesh=102,
            carriers=[99],
            options=[PickupOption(outlet_id=202), PickupOption(outlet_id=203)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5003,
            fesh=103,
            carriers=[99],
            options=[PickupOption(outlet_id=204), PickupOption(outlet_id=205), PickupOption(outlet_id=206)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5004,
            fesh=104,
            carriers=[99],
            options=[PickupOption(outlet_id=207), PickupOption(outlet_id=208)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5005,
            fesh=105,
            carriers=[99],
            options=[PickupOption(outlet_id=209), PickupOption(outlet_id=210)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5006,
            fesh=106,
            carriers=[99],
            options=[PickupOption(outlet_id=211)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5007,
            fesh=107,
            carriers=[99],
            options=[PickupOption(outlet_id=212), PickupOption(outlet_id=213)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5030,
            fesh=130,
            carriers=[99],
            options=[PickupOption(outlet_id=230)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5031,
            fesh=131,
            carriers=[99],
            options=[PickupOption(outlet_id=231)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5032,
            fesh=132,
            carriers=[99],
            options=[PickupOption(outlet_id=232)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5033,
            fesh=133,
            carriers=[99],
            options=[PickupOption(outlet_id=233)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5034,
            fesh=134,
            carriers=[99],
            options=[PickupOption(outlet_id=234)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5035,
            fesh=135,
            carriers=[99],
            options=[PickupOption(outlet_id=235), PickupOption(outlet_id=2352)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5036,
            fesh=136,
            carriers=[99],
            options=[PickupOption(outlet_id=236), PickupOption(outlet_id=2362)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5037,
            fesh=137,
            carriers=[99],
            options=[PickupOption(outlet_id=237), PickupOption(outlet_id=2372)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5038,
            fesh=138,
            carriers=[99],
            options=[PickupOption(outlet_id=238), PickupOption(outlet_id=2382)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5039,
            fesh=139,
            carriers=[99],
            options=[PickupOption(outlet_id=239), PickupOption(outlet_id=2392)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5050,
            fesh=150,
            carriers=[99],
            options=[PickupOption(outlet_id=240)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
        PickupBucket(
            bucket_id=5051,
            fesh=151,
            carriers=[99],
            options=[PickupOption(outlet_id=241)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        ),
    ]


class T(TestCase):
    @classmethod
    def prepare(cls):
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()
        cls.index.fixed_index_generation = '20200101_0300'

        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        prepare_book_now_data(cls)

    def test_filtering_with_not_eligible_docs(self):
        # Проверить фильтрацию офферов и дебаг-счетчик отфильтрованных из-за неучастия в программе BookNow;
        #
        response = self.report.request_json('place=prime&text=Ковырялка&rids=3&debug=1&show-book-now-only=0')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "offer",
                        },
                        {
                            "type": "model",
                        },
                    ],
                }
            },
        )
        self.assertFragmentNotIn(response, {"brief": {"filters": {"BOOK_NOW": NotEmpty()}}})
        response = self.report.request_json('place=prime&text=Ковырялка&rids=3&debug=1&show-book-now-only=1')

        self.assertFragmentIn(response, {"search": {"total": 0}})
        self.assertFragmentIn(response, {"brief": {"filters": {"BOOK_NOW": 2}}})

    def test_filtering_with_eligible_docs(self):
        # Проверить, что BookNow-офферы выводятся при выключенном и включенном фильтре
        #
        response = self.report.request_json('place=prime&text=Палочка&rids=3&show-book-now-only=0')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "offer",
                        },
                        {
                            "type": "model",
                        },
                    ],
                }
            },
        )
        response = self.report.request_json('place=prime&text=Палочка&rids=3&show-book-now-only=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "offer",
                        },
                        {
                            "type": "model",
                        },
                    ],
                }
            },
        )

    def test_incut_page1(self):
        # Проверяем, что врезка отображается.
        # Количество аутлетов, страниц, номер текущей страницы
        # Внутри блока врезки отображается shop_id и друие данные
        # По цифрам видно, что офферы магазина-не-в-программе не влияют на врезку
        # Проверяем, что отображается название самого дешевого оффера
        #
        response = self.report.request_xml('place=book_now_incut&rids=6&hyperid=308&yandexuid=1')
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <book-now-incut>
                <total-outlets>7</total-outlets>
                <total-blocks>4</total-blocks>
                <total-pages>2</total-pages>
                <items-per-page>3</items-per-page>
                <current-page>1</current-page>
                <user-currency>RUR</user-currency>
                <blocks incut-page="1">
                  <block>
                    <shop-id>107</shop-id>
                    <outlets-in-user-region>2</outlets-in-user-region>
                    <available-amount>68</available-amount>
                    <price-min>80</price-min>
                    <title>Книга-монстр дешевая в сельмаге4</title>
                  </block>
                  <block>
                    <shop-id>104</shop-id>
                    <outlets-in-user-region>2</outlets-in-user-region>
                    <available-amount>3</available-amount>
                    <price-min>50</price-min>
                    <title>Книга-монстр в сельмаге1</title>
                  </block>
                  <block>
                    <shop-id>105</shop-id>
                    <outlets-in-user-region>2</outlets-in-user-region>
                    <available-amount>8</available-amount>
                    <price-min>60</price-min>
                    <title>Книга-монстр недорагая в сельмаге2</title>
                  </block>
                </blocks>
              </book-now-incut>
            </search_results>
            ''',
        )

    def test_incut_page2(self):
        # Запрашиваем страницу 2000
        # Но выведена будет только страница №2
        # На вторую страницу должен попасть только один магазин
        response = self.report.request_xml(
            'place=book_now_incut&rids=6&hyperid=308&book-now-incut-page=2000&yandexuid=1'
        )
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <book-now-incut>
                <total-outlets>7</total-outlets>
                <total-blocks>4</total-blocks>
                <total-pages>2</total-pages>
                <items-per-page>3</items-per-page>
                <current-page>2</current-page>
                <user-currency>RUR</user-currency>
                <blocks incut-page="2">
                  <block>
                    <shop-id>106</shop-id>
                    <outlets-in-user-region>1</outlets-in-user-region>
                    <available-amount>8</available-amount>
                    <price-min>70</price-min>
                    <outlet>
                        <PointId>211</PointId>
                    </outlet>
                  </block>
                </blocks>
              </book-now-incut>
            </search_results>
            ''',
        )

    def test_incut_empty_output(self):
        # Запрашиваем с регионом, где нету офферов в программе (rids=777).
        # Проверяем, что не упало. Проверяем, что <total-blocks>0</total-blocks>
        response = self.report.request_xml('place=book_now_incut&rids=1&hyperid=308&yandexuid=1')
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <book-now-incut>
                <total-outlets>0</total-outlets>
                <total-blocks>0</total-blocks>
                <total-pages>1</total-pages>
                <items-per-page>3</items-per-page>
                <current-page>1</current-page>
                <blocks incut-page="1"/>
              </book-now-incut>
            </search_results>
            ''',
        )

    def test_incut_no_yandexuid(self):
        # Напомним, что yandexuid подмешивается в ранг блока при сортировке блоков. То есть, влияет на порядок в выдаче.
        # Проверим это утверждение.
        # Тест: Не передаем &yandexuid= вообще. По-умолчанию будет взято значение "0".
        # Проверяем:
        #   1) что не упало и выдало вторую страницу
        #   2) что порядок в выдаче не такой, как при был &yandexuid=1
        response = self.report.request_xml('place=book_now_incut&rids=6&hyperid=308&book-now-incut-page=2000')
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <book-now-incut>
                <total-outlets>7</total-outlets>
                <total-blocks>4</total-blocks>
                <total-pages>2</total-pages>
                <items-per-page>3</items-per-page>
                <current-page>2</current-page>
                <blocks incut-page="2">
                  <block>
                    <shop-id>107</shop-id>
                    <outlets-in-user-region>2</outlets-in-user-region>
                    <available-amount>68</available-amount>
                    <price-min>80</price-min>
                    <title>Книга-монстр дешевая в сельмаге4</title>
                  </block>
                </blocks>
              </book-now-incut>
            </search_results>
            ''',
        )

    def test_incut_bad_params(self):
        # Задаем запросы с разными корректными и некорректными значениями параметров
        # Репорт не должен падать
        self.report.request_xml('place=book_now_incut&rids=6&hyperid=12345&yandexuid=1')
        self.report.request_xml(
            'place=book_now_incut&rids=6&hyperid=308&yandexuid=sdkfhdjfhsdfkdshfkjdsahfkjdshfkjdshfkjdsdsfkjh'
        )
        self.report.request_xml('place=book_now_incut&rids=6&hyperid=308&yandexuid=-1')
        self.report.request_xml('place=book_now_incut&rids=6&hyperid=308&yandexuid=9999999999999999999')

    def test_geo_offers(self):
        response = self.report.request_json(
            'place=prime&hyperid=307&rids=0&show-book-now-only=1&show-booking-outlets=1&geo=1'
        )
        # Outlet with available booking.
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"entity": "offer", "bookingOutlets": [{"entity": "bookingOutlet", "id": 205}]}]}},
        )
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"entity": "offer", "bookingOutlets": [{"entity": "bookingOutlet", "id": 206}]}]}},
        )
        self.assertFragmentNotIn(
            response,
            {"search": {"results": [{"entity": "offer", "bookingOutlets": [{"entity": "bookingOutlet", "id": 204}]}]}},
        )

    def test_geo_outlets(self):
        response = self.report.request_json(
            'place=prime&hyperid=307&rids=0&show-book-now-only=1&geo=1&show-booking-outlets=1'
        )
        # Outlet with available booking.
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"entity": "offer", "bookingOutlets": [{"entity": "bookingOutlet", "id": 205}]}]}},
        )
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"entity": "offer", "bookingOutlets": [{"entity": "bookingOutlet", "id": 206}]}]}},
        )
        # Outlet without available booking.
        self.assertFragmentNotIn(
            response,
            {"search": {"results": [{"entity": "offer", "bookingOutlets": [{"entity": "bookingOutlet", "id": 204}]}]}},
        )

    def test_geo_outlets_from_different_offers(self):
        response = self.report.request_json('place=geo&hyperid=307&rids=0&show-book-now-only=1')
        # Offers count in outlet.
        self.assertFragmentIn(
            response,
            [
                {"shop": {"id": 103}},
                {"shop": {"id": 103}},
            ],
            preserve_order=True,
        )

    def test_show_booking_outlets(self):
        # Проверяем случай, когда по запросу оффер найдется, но для него не
        # будет возможности забронировать сейчас ни в одном аутлете, т.е. тег
        # <booking-outlets/> должен отсутствовать
        response = self.report.request_json('place=prime' '&text=Ковырялка' '&show-booking-outlets=1' '')
        self.assertFragmentNotIn(response, {"bookingOutlets": [{}]})
        # Проверяем, когда находится оффер с ровно одним аутлетом,
        # удостоверяемся, что найден нужный аутлет с правильным количеством
        # Offer(fesh=101, title='Палочка-выручалочка x (акваланг)', hyperid=301, booking_availabilities=[
        #         BookingAvailability(outlet_id=o1.point_id, region_id=o1.region_id, amount=5),
        #     ]
        # ),
        response = self.report.request_json('place=prime' '&text=Палочка-выручалочка' '&show-booking-outlets=1' '')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "shop": {
                                "entity": "shop",
                                "id": 101,
                            },
                            "bookingOutlets": [{"entity": "bookingOutlet", "id": 201, "amount": 5}],
                        }
                    ]
                }
            },
        )
        # Проверяем случай одного оффера с двумя аутлетами
        # Offer(fesh=102, title='Совушка x', hyperid=303, cmagic='6b77b8385d6b9a7fe05df9b0afc03eae',
        #     booking_availabilities=[
        #         BookingAvailability(outlet_id=o2.point_id, region_id=o2.region_id, amount=1),
        #         BookingAvailability(outlet_id=o3.point_id, region_id=o3.region_id, amount=2),
        #     ]
        # ),
        response = self.report.request_json('place=prime' '&text=Совушка' '&show-booking-outlets=da' '')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "shop": {
                                "entity": "shop",
                                "id": 102,
                            },
                            "bookingOutlets": [
                                {"entity": "bookingOutlet", "id": 202, "amount": 1},
                                {"entity": "bookingOutlet", "id": 203, "amount": 2},
                            ],
                        }
                    ]
                }
            },
        )
        # А теперь случай, когда офферов много и с разным количеством аутлетов
        # Offer(fesh=104, title='Книга-монстр в сельмаге1', hyperid=308, price=50, booking_availabilities=[
        #         BookingAvailability(outlet_id=o7.point_id, region_id=o7.region_id, amount=1),
        #         BookingAvailability(outlet_id=o8.point_id, region_id=o8.region_id, amount=2),
        #     ]
        # ),
        # Нижеследующий оффер будет отсутствовать, т.к. за ним есть еще один в
        # этом же магазине
        # Offer(fesh=105, title='Книга-монстр недорагая в сельмаге2', hyperid=308, price=60, booking_availabilities=[
        #         BookingAvailability(outlet_id=o9.point_id, region_id=o9.region_id, amount=3),
        #     ]
        # ),
        # Offer(fesh=105, title='Книга-монстр дорогая в сельмаге2', hyperid=308, price=1500, booking_availabilities=[
        #         BookingAvailability(outlet_id=o10.point_id, region_id=o10.region_id, amount=5),
        #     ]
        # ),
        # Offer(fesh=106, title='Книга-монстр в сельмаге3', hyperid=308, price=70, booking_availabilities=[
        #         BookingAvailability(outlet_id=o11.point_id, region_id=o11.region_id, amount=8),
        #     ]
        # ),
        # Нижеследующий оффер будет отсутствовать, т.к. за ним есть еще один в
        # этом же магазине
        # Offer(fesh=107, title='Книга-монстр дешевая в сельмаге4', hyperid=308, price=80, booking_availabilities=[
        #         BookingAvailability(outlet_id=o12.point_id, region_id=o12.region_id, amount=13),
        #         BookingAvailability(outlet_id=o13.point_id, region_id=o13.region_id, amount=21),
        #     ]
        # ),
        # Offer(fesh=107, title='Книга-монстр подороже в сельмаге4', hyperid=308, price=120, booking_availabilities=[
        #         BookingAvailability(outlet_id=o12.point_id, region_id=o12.region_id, amount=13),
        #         BookingAvailability(outlet_id=o13.point_id, region_id=o13.region_id, amount=21),
        #     ]
        # ),
        response = self.report.request_json('place=prime' '&text=Книга-монстр' '&show-booking-outlets=da' '')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "shop": {
                                "entity": "shop",
                                "id": 104,
                            },
                            "bookingOutlets": [
                                {"entity": "bookingOutlet", "id": 207, "amount": 1},
                                {"entity": "bookingOutlet", "id": 208, "amount": 2},
                            ],
                        },
                        {
                            "entity": "offer",
                            "shop": {
                                "entity": "shop",
                                "id": 105,
                            },
                            "bookingOutlets": [
                                {"entity": "bookingOutlet", "id": 210, "amount": 5},
                            ],
                        },
                        {
                            "entity": "offer",
                            "shop": {
                                "entity": "shop",
                                "id": 106,
                            },
                            "bookingOutlets": [
                                {"entity": "bookingOutlet", "id": 211, "amount": 8},
                            ],
                        },
                        {
                            "entity": "offer",
                            "shop": {
                                "entity": "shop",
                                "id": 107,
                            },
                            "bookingOutlets": [
                                {"entity": "bookingOutlet", "id": 212, "amount": 13},
                                {"entity": "bookingOutlet", "id": 213, "amount": 21},
                            ],
                        },
                    ]
                }
            },
        )
        # Проверяем случай, когда офферов много и с разным количеством аутлетов , но
        # show-booking-outlets=net/0/отсутствует
        responses = []
        responses.append(self.report.request_json('place=prime' '&text=Книга-монстр' '&show-booking-outlets=net' ''))
        responses.append(self.report.request_json('place=prime' '&text=Книга-монстр' '&show-booking-outlets=0' ''))
        responses.append(self.report.request_json('place=prime' '&text=Книга-монстр' ''))
        for response in responses:
            self.assertFragmentNotIn(
                response,
                {
                    "bookingOutlets": [
                        {"entity": "bookingOutlet", "id": 207, "amount": 1},
                        {"entity": "bookingOutlet", "id": 208, "amount": 2},
                    ]
                },
            )
            self.assertFragmentNotIn(
                response, {"bookingOutlets": [{"entity": "bookingOutlet", "id": 210, "amount": 5}]}
            )
            self.assertFragmentNotIn(
                response, {"bookingOutlets": [{"entity": "bookingOutlet", "id": 211, "amount": 8}]}
            )
            self.assertFragmentNotIn(
                response,
                {
                    "bookingOutlets": [
                        {"entity": "bookingOutlet", "id": 212, "amount": 13},
                        {"entity": "bookingOutlet", "id": 213, "amount": 21},
                    ]
                },
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {"entity": "offer", "shop": {"entity": "shop", "id": 104}},
                            {"entity": "offer", "shop": {"entity": "shop", "id": 105}},
                            {"entity": "offer", "shop": {"entity": "shop", "id": 106}},
                            {"entity": "offer", "shop": {"entity": "shop", "id": 107}},
                        ]
                    }
                },
            )

    def test_battery_10shops(self):
        # Пройтись по всем страницам врезки и проверить, что availability-level каждого магазина
        # правильный.
        # Согласно эвристике отображение количества товара на «уровень наличия» должно распределиться так:
        # 1, 2, 3, 4, [5, 6, 7], 8, 9, 10 где то что в квадратных скобках - это «medium» а по бокам от него «low» и «high»
        # Итак, поехали проверять каждую страницу. Смотрим на <available-amount> и проверяем, что ему соответствует
        # првильный <availability-level>:
        # Страница №1
        response = self.report.request_xml('place=book_now_incut&rids=9&hyperid=330&yandexuid=1&book-now-incut-page=1')
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <book-now-incut>
                <blocks incut-page="1">
                  <block>
                    <shop-id>138</shop-id>
                    <available-amount>9</available-amount>
                    <availability-level>high</availability-level>
                  </block>
                  <block>
                    <shop-id>130</shop-id>
                    <available-amount>1</available-amount>
                    <availability-level>low</availability-level>
                  </block>
                  <block>
                    <shop-id>136</shop-id>
                    <available-amount>7</available-amount>
                    <availability-level>medium</availability-level>
                  </block>
                </blocks>
              </book-now-incut>
            </search_results>''',
        )
        # Страница №2
        response = self.report.request_xml('place=book_now_incut&rids=9&hyperid=330&yandexuid=1&book-now-incut-page=2')
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <book-now-incut>
                <blocks incut-page="2">
                  <block>
                    <shop-id>132</shop-id>
                    <available-amount>3</available-amount>
                    <availability-level>low</availability-level>
                  </block>
                  <block>
                    <shop-id>135</shop-id>
                    <available-amount>6</available-amount>
                    <availability-level>medium</availability-level>
                  </block>
                  <block>
                    <shop-id>131</shop-id>
                    <available-amount>2</available-amount>
                    <availability-level>low</availability-level>
                  </block>
                </blocks>
              </book-now-incut>
            </search_results>''',
        )
        # Страница №3
        response = self.report.request_xml('place=book_now_incut&rids=9&hyperid=330&yandexuid=1&book-now-incut-page=3')
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <book-now-incut>
                <blocks incut-page="3">
                  <block>
                    <shop-id>139</shop-id>
                    <available-amount>10</available-amount>
                    <availability-level>high</availability-level>
                  </block>
                  <block>
                    <shop-id>133</shop-id>
                    <available-amount>4</available-amount>
                    <availability-level>low</availability-level>
                  </block>
                  <block>
                    <shop-id>137</shop-id>
                    <available-amount>8</available-amount>
                    <availability-level>high</availability-level>
                  </block>
                </blocks>
              </book-now-incut>
            </search_results>''',
        )
        # Страница №4
        response = self.report.request_xml('place=book_now_incut&rids=9&hyperid=330&yandexuid=1&book-now-incut-page=4')
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <book-now-incut>
                <blocks incut-page="4">
                  <block>
                    <shop-id>134</shop-id>
                    <available-amount>5</available-amount>
                    <availability-level>medium</availability-level>
                  </block>
                </blocks>
              </book-now-incut>
            </search_results>''',
        )

    def test_battery_1shop(self):
        # Сформировать запрос, когда доступен только один магазин и убедиться,
        # что availability-level всегда "medium" не зависимо от количества офферов

        # На КМ один магазин с 1 (одинм) товаром
        # Убеждаемся, что availability-level=medium
        response = self.report.request_xml('place=book_now_incut&rids=9&hyperid=330&yandexuid=1&fesh=130')
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <book-now-incut>
                <blocks incut-page="1">
                  <block>
                    <shop-id>130</shop-id>
                    <available-amount>1</available-amount>
                    <availability-level>medium</availability-level>
                  </block>
                </blocks>
              </book-now-incut>
            </search_results>''',
        )

        # На КМ один магазин с 10 единицами товара
        # Убеждаемся, что availability-level все равно «medium» (хотя товара ого-го как много)
        response = self.report.request_xml('place=book_now_incut&rids=9&hyperid=330&yandexuid=1&fesh=139')
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <book-now-incut>
                <blocks incut-page="1">
                  <block>
                    <shop-id>139</shop-id>
                    <available-amount>10</available-amount>
                    <availability-level>medium</availability-level>
                  </block>
                </blocks>
              </book-now-incut>
            </search_results>''',
        )

    @skip("Booknow doesn't have required files in market_dynamic in prod")
    def test_cut_off_shop(self):
        # Проверить отключение магазина из программы
        # Сначала ничего не отключено и проверяем, что оффер находится
        # Затем помечаем магазин оффера, как отключенный от программы.
        # И видим, что оффер перестал быть в программе
        # Итак, сначала оффер показывается
        response = self.report.request_json('place=prime&text=Палочка&rids=3&show-book-now-only=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "offer",
                            "eligibleForBookingInUserRegion": True,
                            "shop": {"entity": "shop", "id": 101},
                        },
                        {"type": "model", "eligibleForBookingInUserRegion": True},
                    ],
                }
            },
        )
        # А теперь оффер не должен показываться. Проверяем это:
        self.dynamic.disabled_booknow_shops.append(DynamicShop(101))  # Помечаем оффер магазина, как отключенный

        response = self.report.request_json('place=prime&text=Палочка&rids=3&show-book-now-only=1')
        self.assertFragmentIn(
            response, {"search": {"total": 1, "results": [{"type": "model", "eligibleForBookingInUserRegion": True}]}}
        )
        # Тот же запрос, но разрешен пока офферов-вне-программы
        response = self.report.request_json('place=prime&text=Палочка&rids=3&debug=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "offer",
                            "eligibleForBookingInUserRegion": False,
                            "shop": {"entity": "shop", "id": 101},
                        },
                        {"type": "model", "eligibleForBookingInUserRegion": True},
                    ],
                }
            },
        )

    @skip("Booknow doesn't have required files in market_dynamic in prod")
    def test_cut_off_offer_in_specific_outlet(self):
        # Проверить отключение оффера в аутлете
        # Сначала проверяем, что оффер доступен в обоих регионах
        # А заодно запоминаем айди оффера
        for query in [
            'place=prime&text=Совушка&rids=4&show-book-now-only=1',
            'place=prime&text=Совушка&rids=5&show-book-now-only=1',
        ]:
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 1,
                        "results": [
                            {
                                "entity": "offer",
                                "eligibleForBookingInUserRegion": True,
                                "shop": {"entity": "shop", "id": 102},
                            }
                        ],
                    }
                },
            )

        # Отключаем оффер в аутлете №202 (регион 4)
        self.dynamic.disabled_booknow_offers.append(
            DynamicBookNowOffer(cmagic_id='6b77b8385d6b9a7fe05df9b0afc03eae', outlet_ids=[202])
        )

        # Проверяем, что оффер не находится в программе в регионе 4 (оффер вообще не должен найтись)
        response = self.report.request_json('place=prime&text=Совушка&rids=4&show-book-now-only=1')
        self.assertFragmentIn(response, {"search": {"total": 0}})
        # Тот же запрос, но разрешаем показ офферов-вне-программы
        # Оффер должен быть найден, но он должен быть не доступен для букинга
        response = self.report.request_json('place=prime&text=Совушка&rids=4&debug=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "eligibleForBookingInUserRegion": False,
                            "shop": {"entity": "shop", "id": 102},
                        }
                    ],
                }
            },
        )

        # При этом в другом регионе по прежнему все ок (там аутлет не отключали)
        response = self.report.request_json('place=prime&text=Совушка&rids=5&show-book-now-only=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "eligibleForBookingInUserRegion": True,
                            "shop": {"entity": "shop", "id": 102},
                        }
                    ],
                }
            },
        )

    def test_incut_json(self):
        # Проверяем, что врезка отображет данные в JSON-формате
        # Данные:
        #   Унаследованы от теста test_incut_page1, см. описание данных: https://st.yandex-team.ru/MARKETOUT-8355#1461957068000
        #   Только двум магазинам добавлен рейтинг:
        #       Shop(fesh=104, priority_region=6, regions=[6], name='Сельмаг1', new_shop_rating=NewShopRating(new_rating_total=5.0)),
        #       Shop(fesh=105, priority_region=6, regions=[6], name='Сельмаг2', new_shop_rating=NewShopRating(new_rating_total=3.0)),
        #       У остальных магазинов рейтинг по-умолчанию равен 3 (в выдаче увидим значение 0)
        # Тестируется все то же самое, что в test_incut_page1, но выдача в JSON-формате.
        # На что смотрим:
        # Пейджер: всего элементов 4, по 3 на страницу, значит страниц должно быть 2 (две). Текущая страница имеет номер 1
        # Список блоков. На перовой странице должно быть три блока.
        # В каждом блоке описание магазина (элемент "shop"). В описании магазина его айди, рейтинг и название.
        #
        response = self.report.request_json('place=book_now_incut&rids=6&hyperid=308&yandexuid=1')
        self.assertFragmentIn(
            response,
            {
                "incut": {
                    "entity": "bookNowIncut",  # Какой-то айди для фронтов
                    "pager": {  # Пейджер
                        "currentPage": 1,
                        "entity": "pager",
                        "itemsPerPage": 3,
                        "totalItems": 4,
                        "totalPages": 2,
                    },
                    "results": [  # Список блоков врезки. Максимум три штки на страницу
                        {
                            "availabilityLevel": "high",
                            # Правильность работы этого индикатора тестируется в test_battery_*
                            "availableAmount": 68,  # Сумма по аутлетам
                            "entity": "bookNowIncutBlock",  # Какой-то айди для фронтов
                            "outletsInUserRegion": 2,
                            # Количество аутлетов этого магазина, где есть эта модель в этом регионе
                            "priceMin": 80,  # Минимальная цена товара модели среди всех аутлетов магазина в этом регионе
                            "priceMax": 120,  # Максимальная цена товара модели среди всех аутлетов магазина в этом регионе
                            "title": "Книга-монстр дешевая в сельмаге4",
                            "shop": {"name": u"Сельмаг4", "qualityRating": 0, "shopId": 107},  # дефолтный рейтинг
                        },
                        {
                            "availabilityLevel": "low",
                            "availableAmount": 3,
                            "entity": "bookNowIncutBlock",
                            "outletsInUserRegion": 2,
                            "priceMin": 50,
                            "priceMax": 50,
                            "title": "Книга-монстр в сельмаге1",
                            "shop": {
                                "name": u"Сельмаг1",
                                "qualityRating": 5,  # рейтинг пробрасывается верно
                                "shopId": 104,
                            },
                        },
                        {
                            "availabilityLevel": "medium",
                            "availableAmount": 8,
                            "entity": "bookNowIncutBlock",
                            "outletsInUserRegion": 2,
                            "priceMin": 60,
                            "priceMax": 1500,
                            "title": "Книга-монстр недорагая в сельмаге2",
                            "shop": {
                                "name": u"Сельмаг2",
                                "qualityRating": 3,  # рейтинг пробрасывается верно
                                "shopId": 105,
                            },
                        },
                    ],
                    "userCurrency": "RUR",
                    # Общее количество аутлетов в регионе пользователя для этой модели (по всем шопам)
                    "totalOutlets": 7,
                }
            },
        )

        # Запрашиваем 2000ю страницу врезки. Но репорт должен отдать вторую страницу, т.к. страниц всего две.
        # Так же, здесь же видно, что у магазина Сельмаг3 всего один аутлет с этой моделью в этом регионе и поэтому
        # для него мы сразу можем отрисовать данные точки, где можно забрать товар.
        response = self.report.request_json(
            'place=book_now_incut&rids=6&hyperid=308&yandexuid=1&book-now-incut-page=2000'
        )
        self.assertFragmentIn(
            response,
            {
                "incut": {
                    "entity": "bookNowIncut",
                    "pager": {
                        "currentPage": 2,  # Вот! Номер текущей страницы 2
                        "entity": "pager",
                        "itemsPerPage": 3,
                        "totalItems": 4,
                        "totalPages": 2,
                    },
                    "results": [
                        {
                            "availabilityLevel": "medium",
                            "availableAmount": 8,
                            "outlet": {  # У магазина один аутлет с товаром. Поэтому выводим его сразу
                                "entity": "outlet",
                                "id": "211",
                            },
                            "entity": "bookNowIncutBlock",
                            "outletsInUserRegion": 1,
                            "priceMin": 70,
                            "priceMax": 70,
                            "title": u"Книга-монстр в сельмаге3",
                            "shop": {"name": u"Сельмаг3", "qualityRating": 0, "shopId": 106},
                        }
                    ],
                    "userCurrency": "RUR",
                    "totalOutlets": 7,  # одинаковое на всех страницах
                }
            },
        )

        # И, традиционно, делаем такой запрос, чтобы ничего не нашлось. Смотрим на пустую выдачу
        # Для этого спросим несуществующую модель hyperid=11111
        # На что смотрим:
        #   1. Репорт не упал
        #   2. Пейджер: страниц ноль
        #   3. totalOutlets=0
        response = self.report.request_json('place=book_now_incut&rids=6&hyperid=11111&yandexuid=1')
        self.assertFragmentIn(
            response,
            {
                "incut": {
                    "entity": "bookNowIncut",
                    "pager": {"currentPage": 1, "entity": "pager", "itemsPerPage": 3, "totalItems": 0, "totalPages": 1},
                    "results": [],
                    "userCurrency": "RUR",
                    "totalOutlets": 0,
                }
            },
        )

    def test_show_and_click_log_page1(self):
        # Цель теста: проверять, что в логи показов и логи кликов пишутся корректные вещи.
        # Как тестируем:
        # Запрашиваем первую страницу врезки
        # Что проверяем в логах и сгенерированных URL:
        #   * Наличие записи айдишников: магазина, модели, категории.
        #   * Позиции показов нумеруются корректно (поле position=)
        #   * В клик-урлах dtype='book_now' (ожидаемое значение: ClickType.BOOK_NOW_GO_BOOKING)
        #   * В show_log_tskv поле record_type=2
        #   * URL фронтенда, куда будет перенаправлен пользователь при клике:
        #       * Перенаправляем на страницу geo
        #       * Наличие и значения параметров: glfilters, hid/hyperid, modelid, show-book-now-only=1

        # Поехали, запрос:
        response = self.report.request_json(
            'place=book_now_incut&rids=6&hyperid=308&yandexuid=1&glfilter=1201%3A1;1202%3A2&hid=501'
        )

        # Вот, что ожидаем увидеть в выдаче
        # Должно быть три блока с разными магазинами
        self.assertFragmentIn(
            response,
            {
                "incut": {
                    "pager": {
                        "currentPage": 1,  # Находимся на первой странице. Это значит, что нумерация показов идет от 1
                    },
                    "results": [
                        {
                            "shop": {
                                "shopId": 107,
                            }
                        },
                        {"shop": {"shopId": 104}},
                        {"shop": {"shopId": 105}},
                    ],
                }
            },
        )

        # Проверяем корректность данных в логе показов и кликов для каждого блока врезки
        # Для MSTAT важны поля: Айди магазина (shop_id), айди категории (hyper_cat_id), айди модели (hyper_id)

        # Первый блок. Показан и кликнут магазин shop_id=107
        self.click_log.expect(
            ClickType.BOOK_NOW_GO_BOOKING,
            position=1,
            shop_id=107,
            hyper_id=308,
            hyper_cat_id=501,
            # URL, куда будет направлен пользователь при клике в блоке врезки:
            data_url=LikeUrl.of(
                '//market.yandex.ru/product/308/geo?fesh=107&glfilter=1201%3A1&glfilter=1202%3A2&hid=501&show-book-now-only=1',
                unquote=True,
            ),
        )
        self.show_log_tskv.expect(
            shop_id=107,
            hyper_cat_id=501,
            record_type=2,
            hyper_id=308,
            yandex_uid=1,
            gl_filters='1201:1;1202:2',
            position=1,
            index_generation=self.index.fixed_index_generation,
        )

        # Второй блок. Показан и кликнут магазин shop_id=104
        self.click_log.expect(
            ClickType.BOOK_NOW_GO_BOOKING,
            position=2,
            shop_id=104,
            hyper_id=308,
            hyper_cat_id=501,
            data_url=LikeUrl.of(
                '//market.yandex.ru/product/308/geo?fesh=104&glfilter=1201%3A1&glfilter=1202%3A2&hid=501&show-book-now-only=1',
                unquote=True,
            ),
        )
        self.show_log_tskv.expect(
            shop_id=104,
            hyper_cat_id=501,
            record_type=2,
            hyper_id=308,
            yandex_uid=1,
            gl_filters='1201:1;1202:2',
            position=2,
            index_generation=self.index.fixed_index_generation,
        )

        # Третий блок. Показан и кликнут магазин shop_id=105
        self.click_log.expect(
            ClickType.BOOK_NOW_GO_BOOKING,
            position=3,
            shop_id=105,
            hyper_id=308,
            hyper_cat_id=501,
            data_url=LikeUrl.of(
                '//market.yandex.ru/product/308/geo?fesh=105&glfilter=1201%3A1&glfilter=1202%3A2&hid=501&show-book-now-only=1',
                unquote=True,
            ),
        )
        self.show_log_tskv.expect(
            shop_id=105,
            hyper_cat_id=501,
            record_type=2,
            hyper_id=308,
            yandex_uid=1,
            gl_filters='1201:1;1202:2',
            position=3,
            index_generation=self.index.fixed_index_generation,
        )

    def test_show_and_click_log_page2(self):
        # Здесь тестируем поведение на второй странице врезки
        # Главное, что надо проверять — это то, что номер показа postiton=4

        # Запрашиваем вторую страницу
        response = self.report.request_json(
            'place=book_now_incut&rids=6&hyperid=308&yandexuid=1&glfilter=1201%3A1&glfilter=1202%3A2&hid=501&book-now-incut-page=2'
        )

        # Вот, что ожидаем увидеть в выдаче
        self.assertFragmentIn(
            response,
            {
                "incut": {
                    "pager": {
                        # Вторая страница. Позиция показа начинается с 4 (с четырех)
                        "currentPage": 2,
                    },
                    "results": [
                        {"shop": {"shopId": 106}},
                    ],
                }
            },
        )

        # Первый блок на второй странице. Поверяем, что postiton=4
        self.click_log.expect(
            ClickType.BOOK_NOW_GO_BOOKING,
            position=4,
            shop_id=106,
            hyper_id=308,
            hyper_cat_id=501,
            data_url=LikeUrl.of(
                '//market.yandex.ru/product/308/geo?fesh=106&glfilter=1201%3A1&glfilter=1202%3A2&hid=501&show-book-now-only=1',
                unquote=True,
            ),
        )
        self.show_log_tskv.expect(
            shop_id=106,
            hyper_cat_id=501,
            record_type=2,
            hyper_id=308,
            yandex_uid=1,
            gl_filters='1201:1;1202:2',
            position=4,
        )

    def test_prime_positive(self):
        # Тест из группы тестов, проверяющих выдачу в place=prime
        #
        # Проверяем, что признак наличия выставляется для оффера и модели, если
        # указать регион, где есть аутлеты с наличием.
        # Проверяем, что "eligibleForBookingInUserRegion": True
        # Так же, сличаяем айди моделей и проверяем количество найденного

        # Первый товар. Запрашиваем "Палочку" в регионе 3.
        response = self.report.request_json('place=prime&text=Палочка&rids=3')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,  # Нашли две сущности (модель и оффер)
                    "results": [
                        {
                            "type": "model",  # Нашлась модель
                            "id": 301,  # Да, модель именно та, что и ожидалось
                            "eligibleForBookingInUserRegion": True,  # Ура, признак есть!
                        },
                        {
                            "entity": "offer",  # Нашелся оффер
                            "titles": {"raw": u"Палочка-выручалочка x (акваланг)"},
                            "eligibleForBookingInUserRegion": True,  # Ура, признак есть!
                            "model": {"id": 301},  # Модель та, что надо
                        },
                    ],
                }
            },
        )

        # Второй товар. "Жаба" в регионе 4
        response = self.report.request_json('place=prime&text=Жаба&rids=4')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"type": "model", "id": 302, "eligibleForBookingInUserRegion": True},
                        {
                            "entity": "offer",
                            "titles": {"raw": u"Жаба x"},
                            "eligibleForBookingInUserRegion": True,
                            "model": {"id": 302},
                        },
                    ],
                }
            },
        )

    def test_prime_negative(self):
        # Тест из группы тестов, проверяющих выдачу в place=prime
        #
        # При запросе указываем регион, где нет аутелтов с товарами в наличии.
        # Делаем это для "Палочки" и "Жабы".
        # Проверяем, что "eligibleForBookingInUserRegion": False
        # Так же, сличаяем айди моделей и проверяем количество найденного

        # Первый товар
        response = self.report.request_json('place=prime&text=Палочка&rids=5&debug=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,  # Нашли две сущности (модель и оффер)
                    "results": [
                        {
                            "type": "model",
                            "id": 301,  # Да, модель именно та, что и ожидалось
                            "eligibleForBookingInUserRegion": False,
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": u"Палочка-выручалочка x (акваланг)"},
                            "eligibleForBookingInUserRegion": False,
                            "model": {"id": 301},  # Оффер сматчен к корректной модели
                        },
                    ],
                }
            },
        )

        # Второй товар
        response = self.report.request_json('place=prime&text=Жаба&rids=5&debug=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"type": "model", "id": 302, "eligibleForBookingInUserRegion": False},
                        {
                            "entity": "offer",
                            "titles": {"raw": u"Жаба x"},
                            "eligibleForBookingInUserRegion": False,
                            "model": {"id": 302},
                        },
                    ],
                }
            },
        )

    def test_prime_offer_without_outlet(self):
        # Тест из группы тестов, проверяющих выдачу в place=prime
        #
        # Запрашиваем оффер, у кторого нет првязки к аутлетам и нет никаких данных о
        # доступности в программе Забронировать Сейчас.
        # Ожидаем, что оффер и его модель будут помечены в выдаче, как недоступные для бронирования
        # Т.е. "eligibleForBookingInUserRegion": False
        # Попутно проверяем, что мы нашли именно модель с id=305 и что оффер сматчен с этой моделью
        response = self.report.request_json('place=prime&text=Ковырялка&rids=3')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"type": "model", "id": 305, "eligibleForBookingInUserRegion": False},
                        {
                            "entity": "offer",
                            "titles": {"raw": u"Ковырялка садовая (акваланг)"},
                            "eligibleForBookingInUserRegion": False,
                            "model": {"id": 305},
                        },
                    ],
                }
            },
        )

    def test_prime_filtering_with_mix_of_eligible_and_not_eligible_docs(self):
        # Тест из группы тестов, проверяющих выдачу в place=prime
        #
        # Проверить, как отображается смешанная выдача: документы пригодные для BookNow и документы
        # не принимающе участия в программе. Потом отфильтровать, чтобы отобразились только сущности
        # принимающие участие в программе.

        # Первый запрос без фильтрации по признаку book-now. Ожидаем, что найдуется четыре сущности независимо
        # от того, в программе они или нет.
        response = self.report.request_json('place=prime&text=акваланг&rids=3&debug=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 4,  # Нашли четыре сущности
                    "results": [
                        {
                            "type": "model",  # Нашлась модель
                            "id": 301,  # Да, модель именно та, что и ожидалось
                            "eligibleForBookingInUserRegion": True,  # Эта в программе
                        },
                        {
                            "entity": "offer",  # Нашелся оффер
                            "titles": {"raw": u"Палочка-выручалочка x (акваланг)"},
                            "eligibleForBookingInUserRegion": True,  # Эта в программе
                            "model": {"id": 301},  # Модель та, что надо
                        },
                        {
                            "type": "model",
                            "id": 305,
                            "titles": {"raw": u"Ковырялка ручная (акваланг)"},
                            "eligibleForBookingInUserRegion": False,  # Не в программе
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": u"Ковырялка садовая (акваланг)"},
                            "eligibleForBookingInUserRegion": False,  # Не в программе
                            "model": {"id": 305},
                        },
                    ],
                }
            },
        )

        # А теперь выставляем флаг &show-book-now-only=1 и теперь должны найтись только офферы, которые в программе
        response = self.report.request_json('place=prime&text=акваланг&rids=3&debug=1&show-book-now-only=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,  # Теперь нашлось только две сущности
                    "results": [
                        {
                            "type": "model",  # Нашлась модель
                            "id": 301,  # Да, модель именно та, что и ожидалось
                            "eligibleForBookingInUserRegion": True,  # В программе
                        },
                        {
                            "entity": "offer",  # Нашелся оффер
                            "titles": {"raw": u"Палочка-выручалочка x (акваланг)"},
                            "eligibleForBookingInUserRegion": True,  # В программе
                            "model": {"id": 301},  # Модель та, что надо
                        },
                    ],
                }
            },
        )

    def test_prime_filter(self):
        # Фильтр больше НЕ показывается, см. MARKETOUT-14694
        #
        # Тестируем отрисовку фильтра book-now в блоке фильтов в плейсе prime.
        # https://st.yandex-team.ru/MARKETOUT-8848
        # Задаем запрос в place=prime и проверяем, что фльтра в блоке фильтров нет
        response = self.report.request_json('place=prime&text=акваланг&rids=3&debug=1&show-book-now-only=1')
        self.assertFragmentNotIn(response, {"filters": [{"id": "show-book-now-only"}]})

    def test_prime_filter_filter_not_set(self):
        # Фильтр больше НЕ показывается, см. MARKETOUT-14694
        #
        # Тестируем отрисовку фильтра book-now в блоке фильтов в плейсе prime при невзведенном фильтре
        # https://st.yandex-team.ru/MARKETOUT-8848#1468398118000
        response = self.report.request_json('place=prime&text=акваланг&rids=3&debug=1')
        self.assertFragmentNotIn(response, {"filters": [{"id": "show-book-now-only"}]})

    def test_productoffers_filtering_positive(self):
        # Тестируем плейс &place=productoffers.
        # Аутлет существующий, офферы имеются
        # Ожидаем, что отобразится блок информации про аутлет и что будут найдены офферы

        response = self.report.request_json('place=productoffers&rids=3&hyperid=301&book-now-outlet-id=201')
        # Проверяем, что отобразился блок информации по аутлету
        self.assertFragmentIn(
            response,
            {
                "bookNowRelatedInfo": {
                    "shopName": u"Сеть Волшебные Товары",
                    "outlet": {"id": "201"},
                }
            },
        )

        # Проверяем, что оффер нашелся
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {
                        "entity": "offer",
                        "eligibleForBookingInUserRegion": True,
                        "model": {"id": 301},
                        "shop": {"id": 101},
                    }
                ],
            },
        )

    def test_productoffers_filtering_negative(self):
        # Тестируем плейс &place=productoffers.
        # Случай, когда запрошен фильр по аутлету, в котором нет офферов этой модели
        # Ожидаем, что отобразится блок информации про аутлет, но офферы найдены не будут

        response = self.report.request_json('place=productoffers&rids=3&hyperid=301&book-now-outlet-id=202')
        # Проверяем, что отобразился блок информации по аутлету (офферы не найдены, но данные по аутлету мы показываем все равно)
        self.assertFragmentIn(
            response,
            {
                "bookNowRelatedInfo": {
                    "shopName": u"SHOP-102",
                    "outlet": {"id": "202"},
                }
            },
        )

        # Проверяем, что офферы НЕ нашлись
        self.assertFragmentIn(
            response,
            {
                "total": 0,
            },
        )

    def test_productoffers_filtering_book_now_only(self):
        # Тестируем плейс &place=productoffers.
        # Проверяем отображение только тех офферов, которые можно забронировать в регионе пользователя (аутлет любой)
        # Будем запрашивать офферы модели hyperid=308 сначала без фильтрации по букнау, потом с фильтрацией.
        # Ожидаем, что:
        #  * без фильтрации будет показано много офферов в том числе и те, которые нельзя забронировать в
        #    регионе пользователя.
        #  * с включенной фильтрацией будут показаны только офферы доступные для бронирования в регионе

        # Сначала без фильтра
        response = self.report.request_json('place=productoffers&rids=6&hyperid=308')
        # Проверяем, что нашлось много офферов.
        # И проверяем в выдаче наличие одного оффера с букингом в регионе и без букинга
        self.assertFragmentIn(
            response,
            {
                "total": 13,  # Много, хорошо!
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": u"Книга-монстр дешевая в сельмаге4"},
                        "eligibleForBookingInUserRegion": True,  # Можно букать
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": u"Книга-монстр черная! в сельмаге5 не в программе"},
                        "eligibleForBookingInUserRegion": False,  # Не букается
                    },
                ],
            },
        )

        # Теперь с фильтром
        response = self.report.request_json('place=productoffers&rids=6&hyperid=308&show-book-now-only=1')
        # Проверяем, что нашлось много офферов.
        # И проверяем в выдаче наличие одного оффера с букингом в регионе и без букинга
        self.assertFragmentIn(
            response,
            {
                "total": 6,  # Со включенной фильтрацией найдено меньше
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": u"Книга-монстр дешевая в сельмаге4"},
                        "eligibleForBookingInUserRegion": True,  # Можно букать
                    },
                ],
            },
        )
        # Убедимся, что оффер, который нельзя забукать исчез
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": u"Книга-монстр черная! в сельмаге5 не в программе"},
                    },
                ]
            },
            preserve_order=False,
        )

    def test_now_open_parameter(self):
        # Тестируем работу фильтра &now-open=0/1 https://st.yandex-team.ru/MARKETOUT-9122
        # Примечание:
        #   Реализация фильтрации по настоящим календарям работы будет делаться здесь: https://st.yandex-team.ru/MARKETOUT-9167
        # Тестировать будем так:
        #   1) Запросим place=geo для модели hyperid=308 и без фильтра. У этой модели офферы разложены по 7 аутлетам.
        #       Проверим данный факт (что офферов в выдаче 7 штук). Так же проверим айди всех аутлетов выдачи.
        #   2) Запросим то же самое с фильтром. И проверим, что офферов в выдаче стало 3 штуки

        # Два урла, т.к. мы проверяем, что будет одинаковый результат, если не указать фильтр или указать его равным нулю
        for url in (
            'place=geo&hyperid=308&rids=0&now-open=0&show-book-now-only=1',
            'place=geo&hyperid=308&rids=0&show-book-now-only=1',
        ):
            response = self.report.request_json(url)
            # Проверяем, что в выдаче 7 оффероаутлетов
            self.assertEqual(response.count({'entity': 'offer'}), 7)
            self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "207"}})
            self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "208"}})
            self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "209"}})
            self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "210"}})
            self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "211"}})
            self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "212"}})
            self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "213"}})

        # Теперь влючаем фильтр: &now-open=1
        response = self.report.request_json('place=geo&hyperid=308&rids=0&now-open=1&show-book-now-only=1')
        # Проверяем, что в выдаче 3 оффероаутлета
        self.assertEqual(response.count({'entity': 'offer'}), 3)
        # Вот эти оффероаутлеты остаются
        self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "208"}})
        self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "210"}})
        self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "212"}})
        # Нижеследующие оффероаутлеты должны исчезнуть из выдачи:
        self.assertFragmentNotIn(response, {'entity': 'offer', 'outlet': {'id': "211"}})
        self.assertFragmentNotIn(response, {'entity': 'offer', 'outlet': {'id': "209"}})
        self.assertFragmentNotIn(response, {'entity': 'offer', 'outlet': {'id': "207"}})
        self.assertFragmentNotIn(response, {'entity': 'offer', 'outlet': {'id': "213"}})

    @skip("Booknow doesn't have required files in market_dynamic in prod")
    def test_shop_cut_off_on_the_map(self):
        # Проверяем, что на карте отсекаются аутлеты отключенных магазинов https://st.yandex-team.ru/MARKETOUT-9353
        # Как тестируем:
        #   1) Запрашиваем оффероаутлеты для модели hyperid=308 когда все магазины включены. Проверяем, что
        #       в выдаче 7 обьектов
        #   2) Включаем фильтр. Запрашиваем то же самое. Проверяем, что стало 5 обьектов

        response = self.report.request_json('place=geo&hyperid=308&rids=0&show-book-now-only=1')
        self.assertEqual(response.count({'entity': 'offer'}), 7)  # В выдаче 7 офферов
        self.assertFragmentIn(response, {'entity': 'offer', 'shop': {'id': 104}})  # Магазин shop_id=104 представлен
        self.assertEqual(
            response.count({'entity': 'offer', 'shop': {'id': 104}}), 2
        )  # и представлен в количестве 2 оффероаутлетов

        # А теперь отключаем от программы магазин shop_id=104
        # Его офферы должны исчезнуть из выдачи

        # Помечаем оффер магазина 104, как отключенный
        self.dynamic.disabled_booknow_shops += [DynamicShop(104)]

        response = self.report.request_json('place=geo&hyperid=308&rids=0&show-book-now-only=1')
        self.assertEqual(response.count({'entity': 'offer'}), 5)  # Стало на два оффера меньше
        self.assertFragmentNotIn(
            response, {'entity': 'offer', 'shop': {'id': 104}}
        )  # Проверяем, что ушел именно отключенный магаз
        self.assertEqual(response.count({'entity': 'offer', 'shop': {'id': 104}}), 0)

    def test_now_open_parameter_without_booknow_mode(self):
        # Тестируем работу фильтра &now-open=0/1 БЕЗ фильтра по BookNow https://st.yandex-team.ru/MARKETOUT-9357
        #
        # Казалось бы, этот тест очень похож на test_now_open_parameter, но пусть внимательный читатель не будет введен в заблуждение
        #   таким сходством. Здесь мы проверяем сильно другие ветки кода Репорта.
        #
        # Примечание:
        #   Реализация фильтрации по настоящим календарям работы будет делаться здесь: https://st.yandex-team.ru/MARKETOUT-9167
        # Тестировать будем так:
        #   1) Запросим place=geo для модели hyperid=308 и без фильтра. У этой модели офферы разложены по 7 аутлетам.
        #       Проверим данный факт (что офферов в выдаче 7 штук). Так же проверим айди всех аутлетов выдачи.
        #   2) Запросим то же самое с фильтром. И проверим, что офферов в выдаче стало 3 штуки

        response = self.report.request_json('place=geo&hyperid=308&rids=0')
        # Проверяем, что в выдаче 7 оффероаутлетов
        self.assertEqual(response.count({'entity': 'offer'}), 7)
        self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "207"}})
        self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "208"}})
        self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "209"}})
        self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "210"}})
        self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "211"}})
        self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "212"}})
        self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "213"}})

        # Теперь влючаем фильтр: &now-open=1
        response = self.report.request_json('place=geo&hyperid=308&rids=0&now-open=1')
        # Проверяем, что в выдаче 3 оффероаутлета
        self.assertEqual(response.count({'entity': 'offer'}), 3)
        # Вот эти оффероаутлеты остаются
        self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "208"}})
        self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "210"}})
        self.assertFragmentIn(response, {'entity': 'offer', 'outlet': {'id': "212"}})
        # Нижеследующие оффероаутлеты должны исчезнуть из выдачи:
        self.assertFragmentNotIn(response, {'entity': 'offer', 'outlet': {'id': "211"}})
        self.assertFragmentNotIn(response, {'entity': 'offer', 'outlet': {'id': "209"}})
        self.assertFragmentNotIn(response, {'entity': 'offer', 'outlet': {'id': "207"}})
        self.assertFragmentNotIn(response, {'entity': 'offer', 'outlet': {'id': "213"}})

    def test_prime_filter_is_shown_in_testing_and_in_production(self):
        # Фильтр больше НЕ показывается, см. MARKETOUT-14694
        #
        # Тестируем, что фильтр show-book-now-only НЕ показывается ни в тестинге ни в проде
        # https://st.yandex-team.ru/MARKETOUT-9389
        # Задаем запрос в place=prime и проверяем, что фльтра в блоке фильтров нет
        # Потом добавляем к запросу &disable-testing-features=1 и удостовериваемся, что
        # фильтра нет
        response = self.report.request_json('place=prime&text=акваланг&rids=3&debug=1&show-book-now-only=1')

        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {"id": "show-book-now-only"},
                ]
            },
        )

        response = self.report.request_json(
            'place=prime&text=акваланг&rids=3&debug=1&show-book-now-only=1&disable-testing-features=1'
        )
        self.assertFragmentNotIn(response, {"filters": [{"id": "show-book-now-only"}]})

    def test_incut_glfilters_really_filter(self):
        # Проверяем работу gl-фильтров во врезке.
        # Данные:
        #  * Работаем рамках модели hyperid=308
        #  * У всех офферов, установлено значение параметра GLParam(param_id=1201, value=1).
        #  * У одного дополнительно еще и GLParam(param_id=1201, value=555)
        # Как проверяем:
        # 1) Сначала запросим без gl-фильтрации. Должно найтись 4 блока врезки.
        # 2) Запросим со значением фильтра &glfilter=1201:1. Должно найтись 4 блока врезки.
        # 3) Запросим со значением фильтра &glfilter=1201:555. Должно вернуть 1 блок врезки.
        # 3) Запросим со значением фильтра &glfilter=1201:777. Должна быть выдача без результатов.
        # Для того, чтобы gl-фильтрация заработала надо передавать так же айди категории в запросе: &hid=501

        # Проверяем что с hid=, что без него — влиять на число блоков не должно
        response = self.report.request_json('place=book_now_incut&rids=6&hyperid=308&yandexuid=1')
        self.assertFragmentIn(response, {'entity': 'pager', 'totalItems': 4})
        response = self.report.request_json('place=book_now_incut&rids=6&hyperid=308&yandexuid=1&hid=501')
        self.assertFragmentIn(response, {'entity': 'pager', 'totalItems': 4})

        # Добавляем фильтрацию по значению параметр 1201:1
        # Т.к. этот параметр с этим значением есть у всех офферов, то должно найтись так же 4 блока
        response = self.report.request_json(
            'place=book_now_incut&rids=6&hyperid=308&yandexuid=1&hid=501&glfilter=1201:1'
        )
        self.assertFragmentIn(response, {'entity': 'pager', 'totalItems': 4})

        # Добавляем фильтрацию по значению параметр 1201:555
        # Такое значение есть только у одного оффера и в результате найдется только один блок врезки
        response = self.report.request_json(
            'place=book_now_incut&rids=6&hyperid=308&yandexuid=1&hid=501&glfilter=1201:555'
        )
        self.assertFragmentIn(response, {'entity': 'pager', 'totalItems': 1})

        # Добавляем фильтрацию по значению параметр 1201:777
        # Офферов с таким значением параметра нету и в результате блоков должно найтись ноль штук
        response = self.report.request_json(
            'place=book_now_incut&rids=6&hyperid=308&yandexuid=1&hid=501&glfilter=1201:777'
        )
        self.assertFragmentIn(response, {'entity': 'pager', 'totalItems': 0})

    def test_invalid_glfilter_log_message(self):
        self.report.request_xml('place=book_now_incut&rids=6&hyperid=308&yandexuid=1&glfilter=123:456')
        self.error_log.expect('Error in glfilters syntax:').once()

    def test_offer_availability_level_on_place_geo(self):
        # Тест: проверяем "батарейку" наличия офферов на карте (place=geo)
        # https://st.yandex-team.ru/MARKETOUT-9230
        # Как тестируем
        # 1. Для категории 508 установили границы для классификации наличия:  3шт и менее: low, от 8шт: high; посередине: medium
        # 2. Запрашиваем карту с фильтром &show-book-now-only=1 и проверяем количество офферов и корректность расстновки low|medium|high
        response = self.report.request_json('place=geo&hyperid=308&rids=0&show-book-now-only=1')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': u'Книга-монстр в сельмаге1'},
                'outlet': {'id': '207'},
                'offerAvailability': {'amount': 1, 'level': 'low'},
            },
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': u'Книга-монстр в сельмаге1'},
                'outlet': {'id': '208'},
                'offerAvailability': {'amount': 2, 'level': 'low'},
            },
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': u'Книга-монстр недорагая в сельмаге2'},
                'outlet': {'id': '209'},
                'offerAvailability': {'amount': 3, 'level': 'low'},
            },
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': u'Книга-монстр дорогая в сельмаге2'},
                'outlet': {'id': '210'},
                'offerAvailability': {'amount': 5, 'level': 'medium'},
            },
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': u'Книга-монстр в сельмаге3'},
                'outlet': {'id': '211'},
                'offerAvailability': {'amount': 8, 'level': 'medium'},
            },
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': u'Книга-монстр дешевая в сельмаге4'},
                'outlet': {'id': '212'},
                'offerAvailability': {'amount': 13, 'level': 'high'},
            },
        )

    def test_missing_pp(self):
        response = self.report.request_xml(
            'place=book_now_incut&rids=6&hyperid=308&yandexuid=1&ip=127.0.0.1', strict=False, add_defaults=False
        )
        self.error_log.expect('Some client has not set PP value. Find and punish him violently').once()
        self.assertEqual(500, response.code)

    def test_offer_availability_level_on_place_geo_absent(self):
        # Тест: проверяем, что "батарейки" наличия офферов на карте (place=geo) нету, если не включен фильтр BookNow
        # https://st.yandex-team.ru/MARKETOUT-9230
        # Данные: из тета test_offer_availability_level_on_place_geo
        # Как тестируем
        #  * Запрашиваем карту без фильтра &show-book-now-only= и проверяем, что нету offerAvailability
        response = self.report.request_json('place=geo&hyperid=308&rids=0')
        self.assertFragmentIn(response, {'search': {'total': 7}})
        self.assertFragmentNotIn(response, {'entity': 'offer', 'offerAvailability': {}})

    def test_offer_availability_level_on_place_geo_is_not_shown_when_levels_in_category_are_not_set(self):
        # Тест: проверяем, что "батарейки" наличия офферов на карте (place=geo) нету, если в категории не указаны лимиты наличия
        # https://st.yandex-team.ru/MARKETOUT-9230
        # Данные: Работаем с категорией 501, где не установлены границы для батарейки
        # Как тестируем
        #  * Запрашиваем карту с фильтром &show-book-now-only=1 и проверяем, что offerAvailability все равно нету.
        response = self.report.request_json('place=geo&hyperid=301&rids=0&show-book-now-only=1')
        self.assertFragmentNotIn(response, {'entity': 'offer', 'offerAvailability': {}})

    def test_counts_are_not_added_in_same_outlet(self):
        # Нужно проверить такой кейс:
        #   если в одном аутлете есть несколько офферов с заданным amount, то посмотреть,
        #   что в выдаче в amount в offerAvailability соответствует amount из оффера, а не
        #   суммируется по всем офферам
        response = self.report.request_json('place=geo&hyperid=340&rids=0&show-book-now-only=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {
                            'titles': {'raw': 'You-no-poo #1'},
                            'offerAvailability': {
                                "level": "medium",
                                "amount": 30,
                            },  # 30 штук данного оффера. Ничего не слиплось
                        },
                        {
                            'titles': {'raw': 'You-no-poo (replica)'},
                            'offerAvailability': {"level": "high", "amount": 135},
                        },
                    ],
                }
            },
        )

    def test_book_now_filter_present_if_found_non_zero(self):
        # Фильтр больше НЕ показывается, см. MARKETOUT-14694
        #
        # Что тестируем: отсутствие фильтра "Забрать сейчас", если найдены подходящие под него офферы
        for place in ['prime', 'productoffers', 'geo']:
            response = self.report.request_json('place={}&hyperid=301&rids=3&debug=1'.format(place))
            self.assertFragmentNotIn(response, {"filters": [{"id": "show-book-now-only"}]})

    def test_book_now_filter_hidden_if_found_zero(self):
        # Что тестируем: отсутствие фильтра "Забрать сейчас", если не найдены подходящие под него офферы
        for place in ['prime', 'productoffers', 'geo']:
            response = self.report.request_json('place={}&hyperid=305&rids=3&debug=1'.format(place))
            self.assertFragmentNotIn(response, {"filters": [{"id": "show-book-now-only"}]})

    def test_prime_filtering_positive(self):
        # Тестируем плейс &place=prime.
        # Аутлет существующий, офферы имеются
        # Ожидаем, что отобразится блок информации про аутлет и что будут найдены офферы

        response = self.report.request_json('place=prime&rids=3&text=акваланг&book-now-outlet-id=201')

        # Проверяем, что отобразился блок информации по аутлету
        self.assertFragmentIn(
            response,
            {
                "bookNowRelatedInfo": {
                    "shopName": u"Сеть Волшебные Товары",
                    "outlet": {"id": "201"},
                }
            },
        )

        # Проверяем, что оффер нашелся и он один, т.е. офферы без BookNow отфильтровались
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {
                        "entity": "offer",
                        "eligibleForBookingInUserRegion": True,
                        "model": {"id": 301},
                        "shop": {"id": 101},
                    }
                ],
            },
        )

    def test_prime_filtering_negative(self):
        # Тестируем плейс &place=prime.
        # Случай, когда запрошен фильр по аутлету, в котором нет офферов этой модели
        # Ожидаем, что отобразится блок информации про аутлет, но офферы найдены не будут

        response = self.report.request_json('place=prime&rids=3&hyperid=301&book-now-outlet-id=202')

        # Проверяем, что отобразился блок информации по аутлету (офферы не найдены, но данные по аутлету мы показываем все равно)
        self.assertFragmentIn(
            response,
            {
                "bookNowRelatedInfo": {
                    "shopName": u"SHOP-102",
                    "outlet": {"id": "202"},
                }
            },
        )

        # Проверяем, что офферы НЕ нашлись
        self.assertFragmentIn(
            response,
            {
                "total": 0,
            },
        )


if __name__ == '__main__':
    main()
