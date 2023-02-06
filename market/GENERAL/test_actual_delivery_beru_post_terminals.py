#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa
import six

from core.types import (
    Currency,
    DeliveryBucket,
    GpsCoord,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
)
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax

'''
https://st.yandex-team.ru/MARKETOUT-31963
Эксперимент по скрытию постаматов других СД в пользу ПВЗ Беру
Заведем оутлеты в 4х городах.
Эксперимент действует:
* Москва
* Питер
* Ростов-На-Дону

Эксперимент не действует:
* Екатеринбург

'''

# Службы доставки
BERU_DS = 1005288
BERU_2_DS = 1005111
OTHER_DS = 100500

# Смещение в градусах, гарантируещее попадание в круг определенного радиуса
GPS_OFFSET_200 = 0.002
GPS_OFFSET_400 = 0.0045
GPS_OFFSET_500 = 0.007

# Регионы
MOSCOW = 213
PITER = 2
ROSTOV = 39
EKAT = 59

CENTRAL_MOSCOW = 2131


# Флаг эксперимента
EXP_FLAG = "&rearr-factors=beru_outlets_priority=1"
EMPTY_FLAG = ""


class _Outlets(object):
    @staticmethod
    def create_outlet(info):
        return Outlet(
            point_id=info["id"],
            delivery_service_id=info["ds"],
            delivery_option=OutletDeliveryOption(
                shipper_id=info["ds"], day_from=info['days'], day_to=info['days'], price=400
            ),
            region=info["region"],
            point_type=info["type"],
            working_days=[i for i in range(10)],
            bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
            gps_coord=info["gps"],
        )

    @classmethod
    def extract_outlets(cls):
        return [cls.create_outlet(item) for item in cls.ALL_OUTLETS]

    @staticmethod
    def create_bucket(info):
        return PickupBucket(
            bucket_id=info['id'],
            dc_bucket_id=info['id'],
            carriers=[info['ds']],
            options=[PickupOption(outlet_id=info['id'], day_from=info['days'], day_to=info['days'], price=5)],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

    @classmethod
    def extract_buckets(cls):
        return [cls.create_bucket(item) for item in cls.ALL_OUTLETS]

    @classmethod
    def extract_ids(cls):
        return [item['id'] for item in cls.ALL_OUTLETS]

    @staticmethod
    def move_gps(base_gps, distance_meter):
        return GpsCoord(base_gps.longitude + distance_meter * 360 / 30000000.0, base_gps.latitude)

    @staticmethod
    def create_beru_postomates(base_id, region, gps):
        return {
            # 1 постамат от Беру (показывается всегда)
            'beru1': {
                'id': base_id + 0,
                'ds': BERU_DS,
                'region': region,
                'type': Outlet.FOR_POST_TERM,
                'gps': gps,
                'days': 5,
            },
            # Оба постамата беру будут показаны
            'beru2': {
                'id': base_id + 1,
                'ds': BERU_2_DS,
                'region': region,
                'type': Outlet.FOR_POST_TERM,
                'gps': gps,
                'days': 6,
            },
        }

    @classmethod
    def create_other_outlets(cls, base_id, region, base_gps, distance):
        gps = cls.move_gps(base_gps, distance)
        prefix = 'other_{}'.format(distance)
        return {
            # 1 Доставка позже, чем в Беру (будет скрыт с флагом)
            prefix
            + '_later_delivery': {
                'id': base_id + 0,
                'ds': OTHER_DS,
                'region': region,
                'type': Outlet.FOR_POST_TERM,
                'gps': gps,
                'days': 6,
            },
            # 2 Доставка в тот же день, что и 1 (будет скрыт с флагом)
            prefix
            + '_same_delivery': {
                'id': base_id + 1,
                'ds': OTHER_DS,
                'region': region,
                'type': Outlet.FOR_POST_TERM,
                'gps': gps,
                'days': 5,
            },
            # 3 Доставка раньше, чем в 1 (будет показан всегда)
            prefix
            + '_earlier_delivery': {
                'id': base_id + 2,
                'ds': OTHER_DS,
                'region': region,
                'type': Outlet.FOR_POST_TERM,
                'gps': gps,
                'days': 4,
            },
            # 4 ПВЗ (будет показан всегда, независимо от срока доставки)
            prefix
            + '_pickup': {
                'id': base_id + 3,
                'ds': OTHER_DS,
                'region': region,
                'type': Outlet.FOR_PICKUP,
                'gps': gps,
                'days': 6,
            },
            # 5 Почтовое отделение (будет показан всегда)
            prefix
            + '_post': {
                'id': base_id + 4,
                'ds': OTHER_DS,
                'region': region,
                'type': Outlet.FOR_POST,
                'gps': gps,
                'days': 6,
            },
        }

    @classmethod
    def create_outlets_for_region(cls, base_id, region, base_gps):
        result = cls.create_beru_postomates(base_id, region, base_gps)
        result.update(cls.create_other_outlets(base_id + 10, region, base_gps, 200))
        result.update(cls.create_other_outlets(base_id + 20, region, base_gps, 400))
        result.update(cls.create_other_outlets(base_id + 30, region, base_gps, 500))
        return result


_Outlets.MOSCOW_OUTLETS = _Outlets.create_outlets_for_region(1000, MOSCOW, GpsCoord(37.622504, 55.753226))
_Outlets.PITER_OUTLETS = _Outlets.create_outlets_for_region(2000, PITER, GpsCoord(30.317328, 59.939158))
_Outlets.ROSTOV_OUTLETS = _Outlets.create_outlets_for_region(3000, ROSTOV, GpsCoord(39.651254, 47.251302))
_Outlets.EKAT_OUTLETS = _Outlets.create_outlets_for_region(4000, EKAT, GpsCoord(60.599383, 56.838192))

_Outlets.ALL_OUTLETS = [v for _, v in six.iteritems(_Outlets.MOSCOW_OUTLETS)]
_Outlets.ALL_OUTLETS += [v for _, v in six.iteritems(_Outlets.PITER_OUTLETS)]
_Outlets.ALL_OUTLETS += [v for _, v in six.iteritems(_Outlets.ROSTOV_OUTLETS)]
_Outlets.ALL_OUTLETS += [v for _, v in six.iteritems(_Outlets.EKAT_OUTLETS)]


class _Shops(object):
    virtual_shop_blue = Shop(
        fesh=1,
        datafeed_id=1,
        priority_region=213,
        name='virtual_shop',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        delivery_service_outlets=_Outlets.extract_ids(),
    )

    blue_shop_1 = Shop(
        fesh=3,
        datafeed_id=3,
        priority_region=2,
        name='blue_shop_1',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        blue='REAL',
        warehouse_id=145,
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        cls.index.regiontree += [
            Region(rid=PITER, name="Питер"),
            Region(
                rid=MOSCOW,
                name='Москва',
                children=[Region(rid=CENTRAL_MOSCOW, name="ЦАО", region_type=Region.CITY_DISTRICT)],
            ),
            Region(rid=ROSTOV, name='Ростов-на-Дону'),
            Region(rid=EKAT, name='Екатеринбург'),
        ]

        cls.index.shops += [_Shops.virtual_shop_blue, _Shops.blue_shop_1]

        cls.settings.lms_autogenerate = True

        cls.index.outlets += _Outlets.extract_outlets()

        cls.index.pickup_buckets += _Outlets.extract_buckets()

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=100500,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.1.1',
                        waremd5='BlueOfferForTest_____g',
                        weight=100,
                        dimensions=OfferDimensions(length=100, width=100, height=100),
                    )
                ],
                pickup_buckets=_Outlets.extract_ids(),
                post_term_delivery=True,
                store=True,
                pickup=True,
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=100, width=100, height=100, length=100).respond(
            [], _Outlets.extract_ids(), []
        )

        cls.settings.loyalty_enabled = True

    @staticmethod
    def create_answer(outlets, show_200, show_400):
        def beru1():
            # Постамат Беру от СД1. Показывается всегда
            return [
                outlets['beru1']['id'],
            ]

        def beru2():
            # Постамат Беру от СД2. Показывается всегда
            return [
                outlets['beru2']['id'],
            ]

        def earlier_delivery():
            # Постаматы с доставкой быстрее, чем в Беру. Показывается всегда
            return [
                outlets['other_200_earlier_delivery']['id'],
                outlets['other_400_earlier_delivery']['id'],
                outlets['other_500_earlier_delivery']['id'],
            ]

        def same_delivery():
            # Постаматы с доставкой в тот же день, что и в Беру. Показывается только вне радиуса от Беру
            result = [outlets['other_500_same_delivery']['id']]
            if show_200:
                result += [outlets['other_200_same_delivery']['id']]
            if show_400:
                result += [outlets['other_400_same_delivery']['id']]
            return result

        def later_delivery():
            # Постаматы с доставкой позже, чем в Беру. Показывается только вне радиуса от Беру.
            # Сюда же добавлены оутлеты других типов. Они показываются всегда
            result = [
                outlets['other_500_later_delivery']['id'],
                outlets['other_500_pickup']['id'],
                outlets['other_500_post']['id'],
                outlets['other_200_pickup']['id'],
                outlets['other_200_post']['id'],
                outlets['other_400_pickup']['id'],
                outlets['other_400_post']['id'],
            ]

            if show_200:
                result += [outlets['other_200_later_delivery']['id']]
            if show_400:
                result += [outlets['other_400_later_delivery']['id']]

            return result

        return {
            "pickupOptions": [
                {"serviceId": BERU_DS, "dayFrom": 5, "dayTo": 5, "outletIds": beru1()},
                {"serviceId": BERU_2_DS, "dayFrom": 6, "dayTo": 6, "outletIds": beru2()},
                {
                    "serviceId": OTHER_DS,
                    "dayFrom": 4,
                    "dayTo": 4,
                    "outletIds": earlier_delivery(),
                },
                {
                    "serviceId": OTHER_DS,
                    "dayFrom": 5,
                    "dayTo": 5,
                    "outletIds": same_delivery(),
                },
                {
                    "serviceId": OTHER_DS,
                    "dayFrom": 6,
                    "dayTo": 6,
                    "outletIds": later_delivery(),
                },
            ]
        }

    def __check_beru_outlets(self, region, flag, result):
        response = self.report.request_json(
            "place=actual_delivery&offers-list=BlueOfferForTest_____g:1&rids={}&force-use-delivery-calc=1&pickup-options=grouped&pickup-options-extended-grouping=1&rgb=blue".format(
                region
            )
            + flag
        )
        self.assertFragmentIn(response, result, allow_different_len=False)

    def test_beru_outlets_priority_witout_flags(self):
        '''
        Проверяем, что без флага все оутлеты показываются на выдаче
        '''
        self.__check_beru_outlets(MOSCOW, EMPTY_FLAG, self.create_answer(_Outlets.MOSCOW_OUTLETS, True, True))
        self.__check_beru_outlets(PITER, EMPTY_FLAG, self.create_answer(_Outlets.PITER_OUTLETS, True, True))
        self.__check_beru_outlets(ROSTOV, EMPTY_FLAG, self.create_answer(_Outlets.ROSTOV_OUTLETS, True, True))
        self.__check_beru_outlets(EKAT, EMPTY_FLAG, self.create_answer(_Outlets.EKAT_OUTLETS, True, True))

    def test_beru_outlets_priority(self):
        '''
        Проверяем, что при наличии флага идет скрытие оутлетов вблизи Берушных ПВЗ
        '''
        self.__check_beru_outlets(MOSCOW, EXP_FLAG, self.create_answer(_Outlets.MOSCOW_OUTLETS, False, True))
        self.__check_beru_outlets(PITER, EXP_FLAG, self.create_answer(_Outlets.PITER_OUTLETS, False, True))
        # В Ростове-На-Дону эксперимент работает в радиусе 400м
        self.__check_beru_outlets(ROSTOV, EXP_FLAG, self.create_answer(_Outlets.ROSTOV_OUTLETS, False, False))
        # В Екатеринбурге эксперимент не работает
        self.__check_beru_outlets(EKAT, EXP_FLAG, self.create_answer(_Outlets.EKAT_OUTLETS, True, True))

    def test_beru_outlets_priority_subregion(self):
        '''
        Проверяем, что эксперимент работает, даже если пришли с подрегионом
        '''
        self.__check_beru_outlets(CENTRAL_MOSCOW, EMPTY_FLAG, self.create_answer(_Outlets.MOSCOW_OUTLETS, True, True))
        self.__check_beru_outlets(CENTRAL_MOSCOW, EXP_FLAG, self.create_answer(_Outlets.MOSCOW_OUTLETS, False, True))


if __name__ == '__main__':
    main()
