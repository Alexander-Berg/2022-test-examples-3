from dataclasses import dataclass
from datetime import datetime
from functools import partial
from typing import Iterable, Optional

from ads.bsyeti.libs.primitives.counter_proto.counter_ids_pb2 import ECounterId
from market.library.shiny.lite.log import Severity
from market.mars.lib.external.carter.proto.basic_types_pb2 import EUserIdType, EPlace as Place
from market.mars.lib.external.loyalty.proto.perk_status_pb2 import TPerkStatus
from market.mars.lib.external.report.proto.price_and_promos_pb2 import TPriceAndPromos, TPrices
from market.mars.lib.external.report.proto.sku_offers_pb2 import (
    TSkuOffers,
    TSearch as TSkuOffersSearch,
    TResult as TSkuOffersResult,
    TOffers as TSkuOffersOffers,
    TItem as TSkuOffersItem,
)
from market.mars.lib.morda_cart_widget.carter.proto.carter_response_pb2 import TRawCart
from market.mars.lib.morda_cart_widget.promo.proto.promo_types_pb2 import (
    ECouponDiscountType as DiscountType,
    EPromoMechanics,
    EDiscountPromoMechanicTypes,
)
from market.mars.lib.morda_cart_widget.proto.source_type_pb2 import ESourceType
from market.mars.lib.morda_cart_widget.promo.promo_triggers.proto.promo_triggers_pb2 import (
    ECartWidgetPositionType,
    EEventType,
    EPlatformType,
    ETriggerType,
)
from market.mars.lite.core.bigb import BigbCounter
from market.mars.lib.pers_basket.proto.offers_info_pb2 import (
    TPersBasketItems,
    TOfferInfo,
    TImageMeta,
    TPersBasketItem,
    TSkuInfo,
)
from market.mars.lite.core.carter import PromoType, UserIdType
from market.mars.lite.core.loyalty import PerkTypes
from market.mars.lite.core.report_places.util import create_promo_code

from market.mars.lite.core.tools import get_json_name
from market.pylibrary.lite.matcher import Regex, NotEmptyList, EmptyList, NotEmptyDict, Empty, LikeUrl, Absent, Capture
from market.mars.lite.core.cart_widget_utils.util import (
    CarterHandlerPath,
    CarterRearrFactors,
    CarterUrlParams,
    create_raw_cart,
    create_cart_item,
    get_promo_name,
)
from yabs.proto.user_profile_pb2 import Profile
import market.mars.lite.env as env
import pytz


DEFAULT_TZ_NAME = 'Europe/Moscow'
DEFAULT_TZ = pytz.timezone(DEFAULT_TZ_NAME)
DEFAULT_COUPON_END_DATE = datetime(2051, 11, 11, 11, 11, 11, tzinfo=DEFAULT_TZ)


@dataclass
class MordaCarterUrlParams(CarterUrlParams):
    device: int = EPlatformType.PT_WEB_DESKTOP
    places: Iterable[int] = (Place.P_MORDA_CART_ICON,)
    use_widget_position_counter_limits: bool = False
    return_out_of_stock_items: bool = False

    def to_query_mapping(self):
        self.carter_handler_path = CarterHandlerPath.MORDA
        mapping = super().to_query_mapping()
        mapping['device'] = get_json_name(self.device, EPlatformType)
        mapping['useWidgetPositionCounterLimits'] = self.use_widget_position_counter_limits
        mapping['place'] = [get_json_name(p, Place) for p in self.places]
        mapping['returnOutOfStockItems'] = self.return_out_of_stock_items
        return mapping


class Skus:
    sku1: str = "22210983"
    sku2: str = "22210984"
    sku3: str = "22210985"
    sku4: str = "22210986"
    sku5: str = "22210987"


class T(env.TestSuite):
    @classmethod
    def connect(cls):
        return {
            'carter': cls.mars.carter,
            'api_report': cls.mars.api_report,
            'bigb': cls.mars.bigb,
            'loyalty': cls.mars.loyalty,
            'dyno': cls.mars.dyno,
        }

    @classmethod
    def prepare(cls):
        offer_info = cls.mars.api_report.offer_info()

        raw_cart = create_raw_cart(
            id=1,
            items=[
                create_cart_item(id=10, obj_id='10', price=90),
                create_cart_item(id=20, obj_id='20', price=1300),
                create_cart_item(id=30, obj_id='30', price=10),
                create_cart_item(id=40, obj_id='40', price=111),
                # out of stock item
                create_cart_item(id=1000, obj_id='1000', price=10000, count=2),
            ],
        )

        cart = cls.mars.carter.get_cart()
        cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=1)

        price_and_promos = TPriceAndPromos()

        res = price_and_promos.Search.Results.add()
        res.WareId = '10'
        res.Prices.Value = 90
        promo = res.Promos.add()
        promo.Type = PromoType.PROMO_CODE.value
        promo.PromoCode = 'promo_code'
        promo.ItemsInfo.DiscountType = get_json_name(DiscountType.DT_PERCENT, DiscountType)
        promo.ItemsInfo.PromoPrice.Discount.OldMin = 90
        promo.ItemsInfo.PromoPrice.Discount.Percent = 90
        promo.ItemsInfo.PromoPrice.Discount.Absolute = 81
        promo.ItemsInfo.OrderMinPrice.Currency = "RUR"
        promo.ItemsInfo.OrderMinPrice.Value = 111
        promo.ItemsInfo.OrderMaxPrice.Currency = "RUR"
        promo.ItemsInfo.OrderMaxPrice.Value = 222
        promo.Conditions = 'test_conditions_pc'

        res = price_and_promos.Search.Results.add()
        res.WareId = '20'
        promo = res.Promos.add()
        promo.Type = PromoType.BLUE_FLASH.value
        promo.EndDate = 'end_date'
        promo.PromoKey = 'promo_key'
        promo.ItemsInfo.DiscountType = get_json_name(DiscountType.DT_ABSOLUTE, DiscountType)
        promo.ItemsInfo.Discount.OldMin = 1000
        promo.ItemsInfo.Discount.Percent = 20
        promo.ItemsInfo.PromoPrice.Value = 800
        offer_info.assign_price(res.Prices, promo)

        res = price_and_promos.Search.Results.add()
        res.WareId = '30'
        res.Prices.Value = 10
        res.Prices.Discount.Percent = 50
        res.Prices.Discount.Absolute = 5
        res.Prices.Discount.OldMin = 10

        res = price_and_promos.Search.Results.add()
        res.WareId = '40'
        res.Prices.Value = 111
        promo = res.Promos.add()
        promo.Type = PromoType.PROMO_CODE.value
        promo.PromoCode = 'promo_code'
        promo.ItemsInfo.DiscountType = get_json_name(DiscountType.DT_PERCENT, DiscountType)
        promo.ItemsInfo.PromoPrice.Discount.OldMin = 111
        promo.ItemsInfo.PromoPrice.Discount.Percent = 99
        promo.ItemsInfo.PromoPrice.Discount.Absolute = 110
        promo = res.Promos.add()
        promo.Type = PromoType.GENERIC_BUNDLE_SECONDARY.value

        offer_info.add(price_and_promos, out_of_stock_ware_ids=['1000'], user_id_type=UserIdType.UID, user_any_id=1)

    def test_error_handling(self):
        """Проверяем, что при ошибке в запросе, клиент возвращает сообщение об ошибке"""
        # 1000 - invalid user id. Response will be an empty string
        response = self.mars.request_json(MordaCarterUrlParams(user_id=1000, debug=True).to_url(), fail_on_error=False)
        self.assertFragmentIn(
            response, {'message': Regex('.*Can\'t create TAvailableCarts when carts are empty.'), 'status': 'error'}
        )
        self.assertFragmentIn(
            response, {'debug': {'rawCarterResponse': Empty(), 'rawReportCarterOfferInfoResponse': Empty()}}
        )
        self.assertEqual(response.code, 430)
        self.common_log.expect(message=Regex('Error response.*'), severity=Severity.ERROR)

        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(stat_respones, ['carter_proxy_failed_dmmm', 1])
        self.assertFragmentIn(stat_respones, ['carter_actualizer_failed_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['promo_comparator_failed_dmmm', 0])

    def test_debug_info_flag(self):
        """Проверяем наличие дебажной инфы в ответе при наличии флага debug"""
        response = self.mars.request_json(MordaCarterUrlParams(user_id=1, debug=True).to_url(), fail_on_error=False)
        self.assertFragmentIn(response, {'debug': {'rawCarterResponse': NotEmptyList()}})
        response = self.mars.request_json(MordaCarterUrlParams(user_id=1).to_url())
        # FIXME(smorzhov): id check is a workaround and
        # can be removed when https://st.yandex-team.ru/MSI-1056 will be closed
        self.assertFragmentIn(response, {'debug': Absent(), 'id': 1})

    def cart_request_tester(self, allowed_promos_carter: Iterable[int], expected_response: dict):
        """Вспомогательная функция для проверки ответа от картера"""
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=1,
                places=[Place.P_MORDA_CART_WIDGET, Place.P_MORDA_CART_ICON],
                allowed_promos_carter=allowed_promos_carter,
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'cartUrls': {
                    'morda_cart_widget': LikeUrl.of('https://market.yandex.ru/my/cart?clid=991&lr=0'),
                    'morda_cart_icon': LikeUrl.of(
                        'https://market.yandex.ru/my/cart?utm_source=morda_header_icon'
                        '&clid=956&purchase-referrer=morda_header_icon&lr=0'
                    ),
                },
            },
        )
        self.assertFragmentIn(response, expected_response)

        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(stat_respones, ['carter_proxy_time_hgram'])
        self.assertFragmentIn(stat_respones, ['carter_proxy_failed_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['carter_actualizer_time_hgram'])
        self.assertFragmentIn(stat_respones, ['carter_actualizer_failed_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['promo_comparator_failed_dmmm', 0])
        # Due to MordaExecTimeThresholdMs = 100
        self.assertFragmentIn(stat_respones, ['too_long_get_cart_execution_time_dmmm', 0])

    def test_cart_request_blue_flash(self):
        """Проверяем ответ от картера. Требуем вернуть blue-flash"""
        allowed_promos_carter = [EPromoMechanics.PM_BLUE_FLASH]
        expected_response = {
            'id': 1,
            'totalCount': 4,
            'totalPrice': 895,
            'items': [
                {'id': 10, 'objId': '10', 'price': 90, 'discountPrice': 90, 'discountAbsolute': 0},
                {'id': 20, 'objId': '20', 'price': 1000, 'discountPrice': 800, 'discountAbsolute': 200},
                {'id': 30, 'objId': '30', 'price': 10, 'discountPrice': 5, 'discountAbsolute': 5},
                {'id': 40, 'objId': '40', 'price': 0, 'discountPrice': 0, 'discountAbsolute': 0},
            ],
            'promo': {
                'discountAbsolute': 200,
                'discountPercent': 20,
                'discountType': 'absolute',
                'items': [20],
                'promoType': 'blue-flash',
                'meta': {'endDate': 'end_date'},
            },
        }
        self.cart_request_tester(allowed_promos_carter, expected_response)

    def test_cart_request_promo_code(self):
        """Проверяем ответ от картера. Требуем вернуть promo-code"""
        allowed_promos_carter = [EPromoMechanics.PM_PROMO_CODE]
        expected_response = {
            'id': 1,
            'totalCount': 4,
            'totalPrice': 814,
            'items': [
                {'id': 10, 'objId': '10', 'price': 90, 'discountPrice': 9, 'discountAbsolute': 81},
                {'id': 20, 'objId': '20', 'price': 1000, 'discountPrice': 800, 'discountAbsolute': 200},
                {'id': 30, 'objId': '30', 'price': 10, 'discountPrice': 5, 'discountAbsolute': 5},
                {'id': 40, 'price': 0, 'discountPrice': 0, 'discountAbsolute': 0},
            ],
            'promo': {
                'discountAbsolute': 81,
                'discountPercent': 90,
                'discountType': 'percent',
                'items': [10],
                'promoType': 'promo-code',
                'meta': {'promoCode': 'promo_code', 'subtype': 'single'},
                'conditions': 'test_conditions_pc',
                'orderMinPrice': {'currency': 'RUR', 'value': 111},
                'orderMaxPrice': {'currency': 'RUR', 'value': 222},
            },
        }
        self.cart_request_tester(allowed_promos_carter, expected_response)

    def test_cart_request_price_drop(self):
        """Проверяем ответ от картера. Требуем вернуть price-drop"""
        allowed_promos_carter = [EPromoMechanics.PM_PRICE_DROP]
        expected_response = {
            'id': 1,
            'totalCount': 4,
            'totalPrice': 895,
            'items': [
                {'id': 10, 'objId': '10', 'price': 90, 'discountPrice': 90, 'discountAbsolute': 0},
                {'id': 20, 'objId': '20', 'price': 1000, 'discountPrice': 800, 'discountAbsolute': 200},
                {'id': 30, 'objId': '30', 'price': 10, 'discountPrice': 5, 'discountAbsolute': 5},
                {'id': 40, 'price': 0, 'discountPrice': 0, 'discountAbsolute': 0},
            ],
            'promo': {
                'promoType': 'price-drop',
                'priceDeltaType': 'absolute',
                'priceDeltaAbsolute': 300,
                'items': [20],
            },
        }
        self.cart_request_tester(allowed_promos_carter, expected_response)

    def test_cart_request_random_promo(self):
        """Check that carter can return random promo"""
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=1,
                rearr_factors=CarterRearrFactors(return_random_promo=True),
                random_seed=13,
            ).to_url()
        )
        self.assertFragmentIn(response, {'promo': {'promoType': 'price-drop'}})

        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=1,
                rearr_factors=CarterRearrFactors(return_random_promo=True),
                random_seed=2,
            ).to_url()
        )
        # workaround to make Absent works as expected
        self.assertFragmentIn(response, {'promo': Absent(), 'sourceType': 'carter'})

        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=1,
                rearr_factors=CarterRearrFactors(return_random_promo=True),
                random_seed=8,
            ).to_url()
        )
        self.assertFragmentIn(response, {'promo': {'promoType': 'promo-code'}})

    def test_allowed_promos_over_allowed_promos_carter(self):
        """Test allowed_promos and allowed_promos_carter together"""
        # TODO(smorzhov): can be removed in https://st.yandex-team.ru/MARKETYA-827
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=1,
                places=[Place.P_MORDA_CART_WIDGET, Place.P_MORDA_CART_ICON],
                allowed_promos=[EPromoMechanics.PM_BLUE_FLASH],
                allowed_promos_carter=[EPromoMechanics.PM_PROMO_CODE],
            ).to_url()
        )
        # allowed_promos and allowed_promos_carter are set, allowed_promos_carter is more important
        self.assertFragmentIn(response, {'promo': {'promoType': 'promo-code'}})

        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=1,
                places=[Place.P_MORDA_CART_WIDGET, Place.P_MORDA_CART_ICON],
                allowed_promos=[EPromoMechanics.PM_PROMO_CODE],
            ).to_url()
        )
        # allowed_promos_carter is not set, allowed_promos will be used
        self.assertFragmentIn(response, {'promo': {'promoType': 'promo-code'}})

        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=1,
                places=[Place.P_MORDA_CART_WIDGET, Place.P_MORDA_CART_ICON],
            ).to_url()
        )
        # allowed_promos, allowed_promos_carter are not set, all promos can be returned
        self.assertFragmentIn(response, {'promo': {'promoType': 'blue-flash'}})

    def test_trace_log_contains_out_request(self):
        self.mars.request_json(MordaCarterUrlParams(user_id=1).to_url())
        self.trace_log.expect(request_method=f'/cart/{UserIdType.UID.value}/1/light-list', http_code=200, type='OUT')

    def test_promo_triggers_log_format(self):
        """In this test we will check that promo-triggers log will contain all needed information"""
        self.mars.request_json(MordaCarterUrlParams(user_id=1).to_url())
        self.promo_triggers_log.expect(
            user_id_type=EUserIdType.UIT_UID,
            user_id=1,
            trigger_type=ETriggerType.TT_SHOW_FLASH,
            platform_type=EPlatformType.PT_WEB_DESKTOP,
            event_type=EEventType.ET_RETURNED_FROM_MARS,
            widget_position=0xFFFFFFFF,
            widget_position_type=ECartWidgetPositionType.CWPT_TOP_POSITION_PP,
            # TODO(smorzhov): MARKETYA-819
            available_carter_promo_mechanics=[
                ETriggerType.TT_SHOW_PRICE_DROP,
                ETriggerType.TT_SHOW_DISCOUNT,
                ETriggerType.TT_SHOW_PROMO_CODE,
                ETriggerType.TT_SHOW_FLASH,
                # There is 2 entries in promo triggers log.
                # Due to limitations of BinaryLogBackend, available_carter_promo_mechanics contains doubled values
                ETriggerType.TT_SHOW_PRICE_DROP,
                ETriggerType.TT_SHOW_DISCOUNT,
                ETriggerType.TT_SHOW_PROMO_CODE,
                ETriggerType.TT_SHOW_FLASH,
            ],
            available_pers_basket_promo_mechanics=None,
            best_cart_source_type='carter',
            query_params=(
                '/carter?userIdType=UID&userId=1&rgb=WHITE&ignoreTvmCheck=1&randomSeed=42&rids=0&useBigb=False'
                '&showAdult=False&pinSku=False&useSmartTimeouts=False&allowedPromos=blue-flash'
                '&allowedPromos=promo-code&allowedPromos=items-discount&allowedPromos=coupon&allowedPromos=price-drop'
                '&allowedPromos=direct-discount&device=desktop&useWidgetPositionCounterLimits=False&place=morda_cart_icon'
                '&returnOutOfStockItems=False&format=json'
            ),
        )

    def test_shuffle_items(self):
        """Проверяем, что применяется перемешивание для возвращаемого списка items"""
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=1, rearr_factors=CarterRearrFactors(shuffle_preview=True), random_seed=1
            ).to_url()
        )
        items_before_shuffle = Capture()
        self.assertFragmentIn(
            response,
            {'items': NotEmptyList(capture=items_before_shuffle)},
            preserve_order=True,
        )

        items_after_shuffle = Capture()
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=1, rearr_factors=CarterRearrFactors(shuffle_preview=True), random_seed=42
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {'items': NotEmptyList(capture=items_after_shuffle)},
            preserve_order=True,
        )

        self.assertNotEqual(
            [item['id'] for item in items_before_shuffle.value], [item['id'] for item in items_after_shuffle.value]
        )

    def test_return_out_of_stock_item(self):
        """Проверяем, что возвращаются закончившиеся товары"""
        response = self.mars.request_json(MordaCarterUrlParams(user_id=1, return_out_of_stock_items=True).to_url())
        self.assertFragmentIn(
            response,
            {
                'id': 1,
                'totalCount': 6,
                'totalPrice': 895,
                'items': [
                    {'id': 10, 'count': 1, 'discountPrice': 90, 'isInStock': True},
                    {'id': 20, 'count': 1, 'discountPrice': 800, 'isInStock': True},
                    {'id': 30, 'count': 1, 'discountPrice': 5, 'isInStock': True},
                    {'id': 40, 'count': 1, 'discountPrice': 0, 'isInStock': True},
                    {'id': 1000, 'count': 2, 'discountPrice': 0, 'isInStock': False},
                ],
                'promo': {
                    'discountAbsolute': 200,
                    'discountPercent': 20,
                    'discountType': 'absolute',
                    'items': [20],
                    'promoType': 'blue-flash',
                    'meta': {'endDate': 'end_date'},
                },
            },
        )

    def test_smart_timeouts(self):
        """"""
        response = self.mars.request_json(MordaCarterUrlParams(user_id=1, use_smart_timeouts=True).to_url())
        self.assertFragmentIn(
            response,
            {
                'id': 1,
                'totalCount': 4,
                'totalPrice': 895,
                'promo': {
                    'promoType': 'blue-flash',
                },
            },
        )

        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(stat_respones, ['bigb_request_missed_deadline_counter_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['bigb_request_timeout_counter_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['perk_request_missed_deadline_counter_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['perk_request_timeout_counter_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['carter_proxy_missed_deadline_counter_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['carter_proxy_timeout_counter_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['carter_actualizer_missed_deadline_counter_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['carter_actualizer_timeout_counter_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['pers_basket_data_provider_missed_deadline_counter_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['pers_basket_data_provider_timeout_counter_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['pers_basket_actualizer_missed_deadline_counter_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['pers_basket_actualizer_timeout_counter_dmmm', 0])

    @classmethod
    def prepare_carter_coupons(cls):
        cart = cls.mars.carter.get_cart()
        bigb_profiles = cls.mars.bigb.bigb_profiles()

        coupon_coin = bigb_profiles.create_coupon_coin(
            discount_price=90, condition_price=150, end_date=DEFAULT_COUPON_END_DATE
        )

        # профиль с только с купоном => купон
        raw_cart = create_raw_cart(id=3, items=[create_cart_item(id=42, obj_id='42', price=160)])
        cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=3)
        bigb_profiles.add(puid=3, profile=Profile(market_loyalty_coins=[coupon_coin]))

        # ответ репорта для купонов
        price_and_promos = TPriceAndPromos()
        res = price_and_promos.Search.Results.add()
        res.WareId = '42'
        res.Prices.Value = 140

        # The new backend
        offer_info = cls.mars.api_report.offer_info()
        offer_info.add(price_and_promos, user_id_type=UserIdType.UID, user_any_id=3)

    def test_carter_coupon_selected(self):
        """Проверяем, что выбирается купон, так как больше промомеханик нет"""
        response = self.mars.request_json(
            MordaCarterUrlParams(user_id=3, use_bigb=True, allowed_promos_carter=[EPromoMechanics.PM_COUPON]).to_url()
        )
        # Купон выбрался, так как фильтрация происходит по неактуализированной цене из корзины. А в корзине лежит товаров на 160 рублей.
        self.assertFragmentIn(
            response,
            {
                'items': [{'objId': '42', 'count': 1, 'price': 140, 'discountPrice': 140}],
                'promo': {
                    'discountAbsolute': 90,
                    'discountType': 'absolute',
                    'promoType': 'coupon',
                    'items': [42],
                    'meta': {'endDate': '2051-11-11T08:41:11Z'},
                    'conditions': 'Скидка 90 ₽ на заказ от 150 ₽',
                    'orderMinPrice': {'currency': 'RUR', 'value': 150},
                },
                'totalPrice': 50,
            },
        )

    @classmethod
    def prepare_carter_cashback(cls):
        """Формируем два запроса в репорт:
        1. Для puid=4 возрващаем товар с кешбеком 99. 99 < 100, значит мы отдаём нулевой кешбек
        2. Для puid=5 возрващаем два товара с суммарным кешбеком 999.
           999 > 100, значит мы отдаём кешбек равный 999
        """
        cart = cls.mars.carter.get_cart()
        offer_info = cls.mars.api_report.offer_info()

        # кешбек за один товар меньше чем threshold => не формируем totalCashback
        raw_cart = create_raw_cart(id=4, items=[create_cart_item(id=53, obj_id='53', price=100)])
        cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=4)

        # кешбек за два товара больше чем threshold => формируем totalCashback
        raw_cart = create_raw_cart(
            id=5,
            items=[create_cart_item(id=52, obj_id='52', price=160), create_cart_item(id=53, obj_id='53', price=100)],
        )
        cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=5)

        # кешбек с округлением
        cart_item_list = create_raw_cart(
            id=9,
            items=[
                create_cart_item(id=54, obj_id='54', count=5),
                create_cart_item(id=55, obj_id='55', count=5),
            ],
        )
        cart.add(cart_item_list, user_id_type=UserIdType.UID, user_any_id=9)

        # Добавляем кешбек для одного товара
        price_and_promos = TPriceAndPromos()
        res = price_and_promos.Search.Results.add()
        res.WareId = '53'
        res.Prices.Value = 100
        promo = res.Promos.add()
        promo.Type = PromoType.BLUE_CASHBACK.value
        promo.Share = 0.99

        offer_info.add(price_and_promos, user_id_type=UserIdType.UID, user_any_id=4)

        # Добавляем кешбек для двух товаров
        res = price_and_promos.Search.Results.add()
        res.WareId = '52'
        res.Prices.Value = 1800
        promo = res.Promos.add()
        promo.Type = PromoType.BLUE_CASHBACK.value
        promo.Share = 0.5

        offer_info.add(price_and_promos, user_id_type=UserIdType.UID, user_any_id=5)

        # Добавляем кешбек для двух товаров
        price_and_promos = TPriceAndPromos()
        res = price_and_promos.Search.Results.add()
        res.WareId = '54'
        res.Prices.Value = 1423
        promo = res.Promos.add()
        promo.Type = PromoType.BLUE_CASHBACK.value
        promo.Share = 0.25

        res = price_and_promos.Search.Results.add()
        res.WareId = '55'
        res.Prices.Value = 1160
        promo = res.Promos.add()
        promo.Type = PromoType.BLUE_CASHBACK.value
        promo.Share = 0.07000000007

        offer_info.add(price_and_promos, user_id_type=UserIdType.UID, user_any_id=9)

    def test_cashback_zero(self):
        """Проверяем, что кешбек приходит в корне ответа картера и он равен 0"""
        response = self.mars.request_json(
            MordaCarterUrlParams(user_id=4, allowed_promos_carter=[EPromoMechanics.PM_BLUE_CASHBACK]).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'items': [
                    {'id': 53},
                ],
                'totalCashback': 0,
            },
        )
        self.trace_log.expect(
            request_method='/yandsearch',
            query_params=Regex('.*perks=yandex_cashback.*'),
            http_code=200,
            type='OUT',
        )

    def test_cashback_non_zero(self):
        """Проверяем, что кешбек приходит в корне ответа картера и он равен суммарному кешбеку"""
        response = self.mars.request_json(
            MordaCarterUrlParams(user_id=5, allowed_promos_carter=[EPromoMechanics.PM_BLUE_CASHBACK]).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'items': [
                    {'id': 52},
                    {'id': 53},
                ],
                'totalCashback': 999,
            },
        )
        self.trace_log.expect(
            request_method='/yandsearch',
            query_params=Regex('.*perks=yandex_cashback.*'),
            http_code=200,
            type='OUT',
        )

    def test_cashback_rounding(self):
        """Проверяем работу округления.
        1. Товар 54 стоит 1423 и его 5 в корзине. При этом скидка на товар 25%,
        что представляется в double без ошбики:

        ceil(1423 * 0.25 * 5) = 1779 != 1780 = ceil(1423 * 0.25) * 5

        2. Товар 55 стоит 1160 и его 5 в корзине. При этом скидка на товар 7%, но при
        парсинге протобуфа из json ответа репорта к процентам добавилась маленькая ошибка:

        ceil(1160 * 0.07 * 5) = 406 ! 407 = ceil(1160 * 0.07000000007 * 5)

        """
        response = self.mars.request_json(
            MordaCarterUrlParams(user_id=9, allowed_promos_carter=[EPromoMechanics.PM_BLUE_CASHBACK]).to_url()
        )
        self.assertFragmentIn(
            response,
            {'items': [{'id': 54}, {'id': 55}], 'totalCashback': 2185},  # 1779 + 406
        )

    @classmethod
    def prepare_cashback_with_additional_promo_mechanic(cls):
        cart = cls.mars.carter.get_cart()
        offer_info = cls.mars.api_report.offer_info()

        promo_mechanics = (
            (10, lambda _: None),
            (11, partial(offer_info.add_discount, percent=10, old_min=9000)),
            (12, partial(offer_info.add_personal_discount, percent=10, old_min=9000)),
            (13, partial(offer_info.add_flash, percent=10, old_min=9000)),
            (14, partial(offer_info.add_promo_code, percent=10, old_min=9000)),
        )

        perk_status_data = TPerkStatus()
        perk_status = cls.mars.loyalty.perk_status()
        perk_status.add(perk_status_data, uid=12)

        for ware_id, (uid, promo_mechanic_handler) in enumerate(promo_mechanics, start=60):
            cart_item_list = create_raw_cart(id=uid, items=[create_cart_item(id=ware_id, obj_id=f"{ware_id}")])
            cart.add(cart_item_list, user_id_type=UserIdType.UID, user_any_id=uid)

            price_and_promos = TPriceAndPromos()
            res = price_and_promos.Search.Results.add()
            res.WareId = f"{ware_id}"
            res.Prices.Value = 9000

            promo_mechanic_handler(res)

            promo = res.Promos.add()
            promo.Type = PromoType.BLUE_CASHBACK.value
            promo.Share = 0.1

            offer_info.add(price_and_promos, user_id_type=UserIdType.UID, user_any_id=uid)

    def assert_cashback_value(self, uid, cashback_value):
        response = self.mars.request_json(MordaCarterUrlParams(user_id=uid).to_url())
        self.assertFragmentIn(response, {'totalCashback': cashback_value})

    def test_cashback_with_additional_promo_mechanic(self):
        """Проверяем, что при применении промомеханик кешбек правильно рассчитывается"""

        # без дополнительной промомеханики кешбек считается от полной стоимости
        self.assert_cashback_value(10, 900)

        # с дополнительной промомеханикой кешбек считается от цены со скидкой

        # обычная скидка
        self.assert_cashback_value(11, 810)

        # персональная скидка
        self.assert_cashback_value(12, 810)

        # флеш скидка
        self.assert_cashback_value(13, 810)

        # промокод
        self.assert_cashback_value(14, 810)

        # купон мы не проверяем, так как сейчас мы не раскидываем скидку от купона на все товары

    @classmethod
    def prepare_empty_cart(cls):
        raw_cart = create_raw_cart(id=6, items=[])
        cart = cls.mars.carter.get_cart()
        cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=6)
        # We don't add report data (TPriceAndPromos) since
        # we would like to check that the new backend will not actualize the empty cart

    def test_empty_cart_no_actualization(self):
        """We check that an empty cart will not be actualized"""
        response = self.mars.request_json(MordaCarterUrlParams(user_id=6).to_url())
        self.assertFragmentIn(
            response,
            {
                'id': 6,
                'totalCount': 0,
                'totalPrice': 0,
                'items': [],
            },
        )
        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(stat_respones, ['carter_proxy_time_hgram'])
        self.assertFragmentIn(stat_respones, ['carter_proxy_failed_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['carter_actualizer_time_hgram'])
        # No actualization, no error
        self.assertFragmentIn(stat_respones, ['carter_actualizer_failed_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['promo_comparator_failed_dmmm', 0])

    @classmethod
    def prepare_personal_discount(cls):
        raw_cart = create_raw_cart(
            id=7,
            items=[create_cart_item(id=71, obj_id='71', price=100), create_cart_item(id=72, obj_id='72', price=90)],
        )
        cart = cls.mars.carter.get_cart()
        cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=7)
        # Will be used without perks
        cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=8)

        price_and_promos = TPriceAndPromos()

        res = price_and_promos.Search.Results.add()
        res.WareId = '71'
        # simple discount
        res.Prices.Discount.OldMin = 100
        res.Prices.Discount.Percent = 10
        res.Prices.Discount.Absolute = 90

        res = price_and_promos.Search.Results.add()
        res.WareId = '72'
        # simple discount
        res.Prices.Discount.OldMin = 90
        res.Prices.Discount.Percent = 10
        res.Prices.Discount.Absolute = 81
        promo = res.Promos.add()
        promo.Type = PromoType.DIRECT_DISCOUNT.value
        promo.IsPersonal = True
        promo.ItemsInfo.Price.Discount.OldMin = 90
        promo.ItemsInfo.Price.Discount.Percent = 10
        promo.ItemsInfo.Price.Discount.Absolute = 81

        offer_info = cls.mars.api_report.offer_info()
        offer_info.add(price_and_promos, user_id_type=UserIdType.UID, user_any_id=7)
        offer_info.add(price_and_promos, user_id_type=UserIdType.UID, user_any_id=8)

        perk_status_data = TPerkStatus()
        perk_status_data.Statuses.add().Perks.extend(["perk11", "perk12"])
        perk_status_data.Statuses.add().Perks.extend(["perk21", "perk22"])

        perk_status = cls.mars.loyalty.perk_status()
        perk_status.add(perk_status_data, uid=7, perk_types=sorted([perk_type.value for perk_type in PerkTypes]))

    def test_all_perk_types_in_request(self):
        """Check that perk provider request will contain all perk types"""
        self.mars.request_json(MordaCarterUrlParams(user_id=7).to_url())
        # we need sorted here because TSet is used to store perk types in C++ code
        perks = '&perkType='.join(sorted([perk_type.value for perk_type in PerkTypes]))
        self.trace_log.expect(
            request_method='/perk/status',
            query_params=Regex(f'.*{perks}.*'),
            http_code=200,
            type='OUT',
        )

    def test_personal_discount(self):
        """Check that we can get personal discount"""
        response = self.mars.request_json(MordaCarterUrlParams(user_id=7).to_url())
        self.assertFragmentIn(
            response,
            {
                'promo': {
                    'promoType': 'items-discount',
                    'discountAbsoluteTotal': 81,
                    'isPersonal': True,
                    'items': [72],
                }
            },
        )
        self.trace_log.expect(
            request_method='/yandsearch',
            query_params=Regex('.*perks=perk11%2Cperk12%2Cperk21%2Cperk22.*'),
            http_code=200,
            type='OUT',
        )

        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=7,
                allowed_promos_carter=[
                    EPromoMechanics.PM_BLUE_FLASH,
                    EPromoMechanics.PM_PROMO_CODE,
                    EPromoMechanics.PM_ITEMS_DISCOUNT,
                    EPromoMechanics.PM_COUPON,
                    EPromoMechanics.PM_PRICE_DROP,
                    EPromoMechanics.PM_BLUE_CASHBACK,
                ],
                rearr_factors=CarterRearrFactors(
                    discount_promo_mechanic_type=EDiscountPromoMechanicTypes.DT_AGGREGATED
                ),
            ).to_url()
        )
        # Check that all items-discount will be returned when personal discount is not allowed
        self.assertFragmentIn(
            response,
            {
                'promo': {
                    'promoType': 'items-discount',
                    'discountAbsoluteTotal': 171,
                    'isPersonal': False,
                    'items': [71, 72],
                }
            },
        )

        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(stat_respones, ['perk_request_failed_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['perk_request_time_hgram'])

    def test_per_item_discount(self):
        """
        Check that if DT_PER_ITEM rearr factor has been passed,
        only one discount (with the biggest absolute total discount) will be returned
        """
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=7,
                allowed_promos_carter=[EPromoMechanics.PM_ITEMS_DISCOUNT],
                rearr_factors=CarterRearrFactors(discount_promo_mechanic_type=EDiscountPromoMechanicTypes.DT_PER_ITEM),
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'promo': {
                    'promoType': 'items-discount',
                    'discountAbsoluteTotal': 90,
                    'discountPercent': 10,
                    'isPersonal': False,
                    'items': [71],
                }
            },
        )

    def test_raw_loyalty_response_availability(self):
        """Check that raw mars response contains raw loyalty response when debug=1"""
        response = self.mars.request_json(MordaCarterUrlParams(user_id=7, debug=True).to_url())
        self.assertFragmentIn(
            response,
            {'debug': {'rawPerksResponse': NotEmptyDict()}},
        )

    def test_discount_with_broken_perks_provider(self):
        """Check that the discount (not personal) will be returned even with the broken perks provider"""
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=8,
                debug=True,
                rearr_factors=CarterRearrFactors(
                    discount_promo_mechanic_type=EDiscountPromoMechanicTypes.DT_AGGREGATED
                ),
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {'rawPerksResponse': Empty()},
                'promo': {
                    'promoType': 'items-discount',
                    'discountAbsoluteTotal': 171,
                    'isPersonal': False,
                    'items': [71, 72],
                },
            },
        )
        self.common_log.expect(message=Regex('.*Failed to get perks.*'), severity=Severity.WARN)

    @classmethod
    def prepare_pers_basket_without_carter(cls):
        offers_info = TPersBasketItems(
            Items=[
                TPersBasketItem(
                    OfferInfo=TOfferInfo(
                        WareMd5="offer1",
                        Msku=1,
                        Name="offer1",
                        ImageMeta=TImageMeta(GroupId=88, Namespace="mpic", Key="img_smth-lol.jpeg"),
                    )
                )
            ]
        )

        cls.mars.dyno.add_pers_basket(UserIdType.UID, "2220", offers_info)

        price_and_promos = TPriceAndPromos()
        res = price_and_promos.Search.Results.add()
        res.WareId = "offer1"
        res.Prices.Value = 100
        promo = res.Promos.add()
        promo.Type = PromoType.PROMO_CODE.value
        promo.PromoCode = "promo_code"
        promo.ItemsInfo.DiscountType = get_json_name(DiscountType.DT_PERCENT, DiscountType)
        promo.ItemsInfo.PromoPrice.Discount.OldMin = 90
        promo.ItemsInfo.PromoPrice.Discount.Percent = 10
        promo.ItemsInfo.PromoPrice.Discount.Absolute = 81

        offer_info = cls.mars.api_report.offer_info()
        offer_info.add(price_and_promos, user_id_type=UserIdType.UID, user_any_id='2220')

        bigb_profiles = cls.mars.bigb.bigb_profiles()
        bigb_profiles.add(puid=2220, profile=Profile())

    def test_pers_basket_without_carter(self):
        """Проверяем, что ответ формируется, когда товары есть только в избранном и для них есть промомеханика"""
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2220, allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET]
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketItems": [{"objId": "offer1"}],
                "persBasketPromo": {"promoType": "promo-code"},
            },
        )

    @classmethod
    def prepare_pers_basket_without_bigb_profile(cls):
        offer_info = cls.mars.api_report.offer_info()
        carter = cls.mars.carter.get_cart()
        dyno = cls.mars.dyno

        offer_info.add(TPriceAndPromos(), user_id_type=UserIdType.UID, user_any_id='2233')
        dyno.add_pers_basket(UserIdType.UID, "2233", TPersBasketItems())
        carter.add(create_raw_cart(id=1, items=[]), user_id_type=UserIdType.UID, user_any_id=2233)

    def test_pers_basket_without_bigb_profile(self):
        self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2233, allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET]
            ).to_url(),
            fail_on_error=False,
        )
        self.common_log.expect(message=Regex("Can't create pers basket cart.*"), severity=Severity.WARN)

    @classmethod
    def prepare_pers_basket_right_format(cls):
        offer_info = cls.mars.api_report.offer_info()
        bigb_profiles = cls.mars.bigb.bigb_profiles()
        carter = cls.mars.carter.get_cart()
        dyno = cls.mars.dyno

        raw_cart = create_raw_cart(
            id=1,
            items=[
                create_cart_item(id=1, obj_id='offer3', price=200, count=2),
                create_cart_item(id=2, obj_id='offer4', price=110, count=3),
            ],
        )
        carter.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=2221)

        offers_info = TPersBasketItems(
            Items=[
                TPersBasketItem(
                    OfferInfo=TOfferInfo(
                        WareMd5="offer1",
                        Msku=22210983,
                        Name="#1 Самый первый в мире...",
                        ImageMeta=TImageMeta(GroupId=88, Namespace="mpic", Key="img_smth-lol.jpeg"),
                    )
                ),
                TPersBasketItem(
                    OfferInfo=TOfferInfo(
                        WareMd5="offer2",
                        Msku=22210984,
                        Name="#2 Следующий сразу за...",
                        ImageMeta=TImageMeta(GroupId=89, Namespace="mpec", Key="img_smth-kek.jpeg"),
                    )
                ),
                # out of stock item
                TPersBasketItem(
                    OfferInfo=TOfferInfo(
                        WareMd5="offer5",
                        Msku=22210985,
                        Name="#5 Следующий сразу за...",
                        ImageMeta=TImageMeta(GroupId=90, Namespace="mpec", Key="img_smth-kek2.jpeg"),
                    )
                ),
            ]
        )

        dyno.add_pers_basket(UserIdType.UID, "2221", offers_info)

        price_and_promos = TPriceAndPromos()
        res = price_and_promos.Search.Results.add()
        res.WareId = "offer1"
        res.Prices.Value = 100
        promo = res.Promos.add()
        promo.Type = PromoType.BLUE_CASHBACK.value
        promo.Share = 1

        # report response for test_max_pers_basket_offers_rearr
        offer_info.add(price_and_promos, user_id_type=UserIdType.UID, user_any_id="2221")

        res = price_and_promos.Search.Results.add()
        res.WareId = "offer2"
        res.Prices.Value = 120
        promo = res.Promos.add()
        promo.Type = PromoType.BLUE_CASHBACK.value
        promo.Share = 1
        promo = res.Promos.add()
        promo.Type = PromoType.PROMO_CODE.value
        promo.ItemsInfo.DiscountType = get_json_name(DiscountType.DT_PERCENT, DiscountType)
        promo.ItemsInfo.PromoPrice.Discount.OldMin = 120
        promo.ItemsInfo.PromoPrice.Discount.Percent = 10
        promo.ItemsInfo.PromoPrice.Discount.Absolute = 12

        offer_info.add(
            price_and_promos, user_id_type=UserIdType.UID, user_any_id="2221", out_of_stock_ware_ids=["offer5"]
        )

        price_and_promos = TPriceAndPromos()
        res = price_and_promos.Search.Results.add()
        res.WareId = "offer3"
        res.Prices.Value = 199
        res = price_and_promos.Search.Results.add()
        res.WareId = "offer4"
        res.Prices.Value = 109

        offer_info.add(price_and_promos, user_id_type=UserIdType.UID, user_any_id="2221")
        bigb_profiles.add(puid=2221, profile=Profile())

    def test_pers_basket_right_format(self):
        """Проверяем, что ответ имеет правильный формат:
        1. Каждый элемент в items имеет свою imageMeta
        2. Формируются урлы в избранное
        3. В корне ответа есть поле с типом источника ответа
        4. Есть поля persBasketItems и они совпадают по формату с items
           оригинального ответа для корзины
        5. Id элементов items идут в возрастающем порядке, начиная с 0
        6. Добавлено поле с кешбеком - persBasketTotalCashback
        7. Добавлено поле carter с totalCount и totalPrice для оригинальной корзины.
        7.1. По умолчанию заполняется нулями.
        """
        response = self.mars.request_json(
            MordaCarterUrlParams(user_id=2221, allowed_source_types=[ESourceType.ST_PERS_BASKET]).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketUrls": {"url": LikeUrl.of("https://market.yandex.ru/my/wishlist?lr=0")},
                "persBasketItems": [
                    {
                        "count": 1,
                        "id": 0,
                        "price": 100,
                        "objId": "offer1",
                        "msku": 22210983,
                        "name": "#1 Самый первый в мире...",
                        "imageMeta": {
                            "groupId": 88,
                            "key": "img_smth-lol.jpeg",
                            "namespace": "mpic",
                        },
                    },
                    {
                        "count": 1,
                        "id": 1,
                        "price": 120,
                        "objId": "offer2",
                        "msku": 22210984,
                        "name": "#2 Следующий сразу за...",
                        "imageMeta": {
                            "groupId": 89,
                            "key": "img_smth-kek.jpeg",
                            "namespace": "mpec",
                        },
                    },
                ],
                "persBasketTotalCashback": 208,
                "sourceType": "persBasket",
            },
            allow_different_len=False,
        )

        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2221, allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET]
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "carter": {"totalCount": 5, "totalPrice": 725},
            },
        )

        response = self.mars.request_json(
            MordaCarterUrlParams(user_id=2221, allowed_source_types=[ESourceType.ST_PERS_BASKET]).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "carter": {"totalCount": 0, "totalPrice": 0},
            },
        )

    def test_out_of_stock_items_not_filtered_out_from_pers_basket(self):
        """out-of-stock items must not be returned even if return_out_of_stock_items=True"""
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2221, allowed_source_types=[ESourceType.ST_PERS_BASKET], return_out_of_stock_items=True
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketItems": [
                    {"count": 1, "id": 0, "objId": "offer1", "msku": 22210983, "isInStock": True},
                    {"count": 1, "id": 1, "objId": "offer2", "msku": 22210984, "isInStock": True},
                ],
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            {"persBasketItems": [{"objId": "offer5", "msku": 22210985}]},
        )

    def test_max_pers_basket_offers_rearr(self):
        """persBasketItems must contain no more than max_pers_basket_offers items"""
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2221,
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
            ).to_url(),
        )
        self.assertEqual(len(response['persBasketItems']), 2)

        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2221,
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
                rearr_factors=CarterRearrFactors(max_pers_basket_offers=1),
            ).to_url(),
        )
        self.assertEqual(len(response['persBasketItems']), 1)

    def test_allowed_promos_over_allowed_promos_pers_basket(self):
        """Test allowed_promos and allowed_promos_carter together"""
        # TODO(smorzhov): can be removed in https://st.yandex-team.ru/MARKETYA-827
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2221,
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
                allowed_promos=[EPromoMechanics.PM_BLUE_FLASH],
                allowed_promos_pers_basket=[EPromoMechanics.PM_PROMO_CODE],
            ).to_url(),
        )
        # allowed_promos and allowed_promos_pers_basket are set, allowed_promos_pers_basket is more important
        self.assertFragmentIn(response, {'persBasketPromo': {'promoType': 'promo-code'}})

        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2221,
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
                allowed_promos=[EPromoMechanics.PM_BLUE_FLASH],
            ).to_url(),
        )
        # allowed_promos_pers_basket is not set, allowed_promos will be used
        self.assertFragmentIn(response, {'sourceType': 'persBasket', 'persBasketPromo': Absent()})

        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2221,
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
                allowed_promos=[EPromoMechanics.PM_PROMO_CODE],
            ).to_url(),
        )
        # allowed_promos_pers_basket is not set, allowed_promos will be used
        self.assertFragmentIn(response, {'persBasketPromo': {'promoType': 'promo-code'}})

    def test_promo_triggers_log_format_pers_basket(self):
        """In this test we will check that promo-triggers log will contain all needed information"""
        self.mars.request_json(
            MordaCarterUrlParams(user_id=2221, allowed_source_types=[ESourceType.ST_PERS_BASKET]).to_url()
        )
        self.promo_triggers_log.expect(
            user_id_type=EUserIdType.UIT_UID,
            user_id=2221,
            trigger_type=ETriggerType.TT_SHOW_PERS_BASKET_PROMO_CODE,
            platform_type=EPlatformType.PT_WEB_DESKTOP,
            event_type=EEventType.ET_RETURNED_FROM_MARS,
            widget_position=0xFFFFFFFF,
            widget_position_type=ECartWidgetPositionType.CWPT_TOP_POSITION_PP,
            available_carter_promo_mechanics=None,
            # TODO(smorzhov): MARKETYA-819
            available_pers_basket_promo_mechanics=[
                ETriggerType.TT_SHOW_PERS_BASKET_PROMO_CODE,
                ETriggerType.TT_SHOW_PERS_BASKET_PROMO_CODE,
            ],
            best_cart_source_type='persBasket',
            query_params=(
                '/carter?userIdType=UID&userId=2221&rgb=WHITE&ignoreTvmCheck=1&randomSeed=42&rids=0'
                '&useBigb=False&showAdult=False&pinSku=False&useSmartTimeouts=False&allowedPromos=blue-flash'
                '&allowedPromos=promo-code&allowedPromos=items-discount&allowedPromos=coupon'
                '&allowedPromos=price-drop&allowedPromos=direct-discount&allowedSourceTypes=persBasket'
                '&device=desktop&useWidgetPositionCounterLimits=False&place=morda_cart_icon&returnOutOfStockItems=False&format=json'
            ),
        )

    @classmethod
    def prepare_pers_basket_without_promo(cls):
        offers_info = TPersBasketItems(Items=[TPersBasketItem(OfferInfo=TOfferInfo(WareMd5="offer1"))])
        cls.mars.dyno.add_pers_basket(UserIdType.UID, "2222", offers_info)

        raw_cart = create_raw_cart(id=4, items=[create_cart_item(id=3, obj_id='offer3')])
        cls.mars.carter.get_cart().add(raw_cart, user_id_type=UserIdType.UID, user_any_id=2223)

        pers_basket_report_response = TPriceAndPromos()
        res = pers_basket_report_response.Search.Results.add()
        res.WareId = "offer1"
        res.Prices.Value = 120

        carter_report_response = TPriceAndPromos()
        res = carter_report_response.Search.Results.add()
        res.WareId = "offer3"
        res.Prices.Value = 130

        offer_info = cls.mars.api_report.offer_info()
        offer_info.add(pers_basket_report_response, user_id_type=UserIdType.UID, user_any_id="2222")
        offer_info.add(carter_report_response, user_id_type=UserIdType.UID, user_any_id="2223")

        bigb_profiles = cls.mars.bigb.bigb_profiles()
        bigb_profiles.add(puid=2222, profile=Profile())
        bigb_profiles.add(puid=2223, profile=Profile())

    def test_pers_basket_without_promo(self):
        """Проверяем, что ответ формируется, когда товары есть в одном из источников, но ни в одном нет promo:
        1. Есть в корзине, но нет в избранном
        2. Есть в избранном, но нет в корзине
        """
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2222, allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET]
            ).to_url()
        )
        self.assertFragmentIn(response, {"persBasketItems": [{"id": 0, "price": 120}]})

        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2223, allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET]
            ).to_url()
        )
        self.assertFragmentIn(response, {"items": [{"id": 3, "price": 130}]})

    def test_pers_basket_error_when_all_empty(self):
        """Проверяем, что формируется ошибка, когда нет ответа ни в корзине, ни в избранном"""
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2225, debug=True, allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET]
            ).to_url(),
            fail_on_error=False,
        )
        self.assertFragmentIn(
            response, {"message": Regex(".*Can\'t create TAvailableCarts when carts are empty."), "status": "error"}
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "rawCarterResponse": Empty(),
                    "rawReportCarterOfferInfoResponse": Empty(),
                    "rawReportPersBasketOfferInfoResponse": Empty(),
                    "rawDynoPersBasketResponse": Empty(),
                }
            },
        )
        self.assertEqual(response.code, 430)
        self.common_log.expect(message=Regex("Error response.*"), severity=Severity.ERROR)

    @classmethod
    def prepare_basket_with_best_priority(cls):
        offer_info_place = cls.mars.api_report.offer_info()
        bigb_profiles = cls.mars.bigb.bigb_profiles()
        cart = cls.mars.carter.get_cart()
        dyno = cls.mars.dyno

        promo_code_respones = TPriceAndPromos()
        res = promo_code_respones.Search.Results.add()
        res.WareId = 'offer4'
        res.Prices.Value = 190
        promo = res.Promos.add()
        promo.Type = PromoType.PROMO_CODE.value
        promo.ItemsInfo.DiscountType = get_json_name(DiscountType.DT_PERCENT, DiscountType)

        blue_flash_response = TPriceAndPromos()
        res = blue_flash_response.Search.Results.add()
        res.WareId = 'offer5'
        res.Prices.Value = 210
        promo = res.Promos.add()
        promo.Type = PromoType.BLUE_FLASH.value
        promo.ItemsInfo.DiscountType = get_json_name(DiscountType.DT_ABSOLUTE, DiscountType)

        for cart_item, offer_info, uid in [
            # Лучшая промомеханика у картера
            (create_cart_item(obj_id='offer5'), TOfferInfo(WareMd5="offer4"), 2226),
            # Лучшая промомеханика у избранного
            (create_cart_item(obj_id='offer4'), TOfferInfo(WareMd5="offer5"), 2227),
        ]:
            offer_info_place.add(promo_code_respones, user_id_type=UserIdType.UID, user_any_id=uid)
            offer_info_place.add(blue_flash_response, user_id_type=UserIdType.UID, user_any_id=uid)

            raw_cart = create_raw_cart(id=-1, items=[cart_item])
            offers_info = TPersBasketItems(Items=[TPersBasketItem(OfferInfo=offer_info)])

            cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=uid)
            dyno.add_pers_basket(UserIdType.UID, str(uid), offers_info)
            bigb_profiles.add(puid=uid, profile=Profile())

    def test_pers_basket_with_best_priority(self):
        """Проверяем, логику выбора источника корзины:
        1. Выберается картер для формирования ответа, когда промомеханика из картера с более высоким приоритетом
        2. Выберается избранное для формирования ответа, когда промомеханика из избранного с более высоким приоритетом
        3. При форсировании корзины или избранного - выбирается только тот источник, который мы форсировали.
        """
        # В корзине лежит blue-flash, а в избранном promo-code => выбираем корзину
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2226, allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET]
            ).to_url()
        )
        self.assertFragmentIn(
            response, {"items": [{"objId": "offer5"}], "promo": {"promoType": "blue-flash"}, "sourceType": "carter"}
        )
        # В корзине лежит blue-flash, а в избранном promo-code, форсируем избранное => выбираем избранное
        response = self.mars.request_json(
            MordaCarterUrlParams(user_id=2226, allowed_source_types=[ESourceType.ST_PERS_BASKET]).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketItems": [{"objId": "offer4"}],
                "persBasketPromo": {"promoType": "promo-code"},
                "sourceType": "persBasket",
            },
        )
        # В избранном лежит blue-flash, а в корзине promo-code => выбираем избранное
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2227, allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET]
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketItems": [{"objId": "offer5"}],
                "persBasketPromo": {"promoType": "blue-flash"},
                "sourceType": "persBasket",
            },
        )
        # В избранном лежит blue-flash, а в корзине promo-code, форсируем корзину => выбираем корзину
        response = self.mars.request_json(
            MordaCarterUrlParams(user_id=2227, allowed_source_types=[ESourceType.ST_CARTER]).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "items": [{"objId": "offer4"}],
                "promo": {"promoType": "promo-code"},
                "sourceType": "carter",
            },
        )

    def test_random_source_type(self):
        """Test that both carter and persBasket can be returned when random flag is set"""
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2226,
                allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET],
                rearr_factors=CarterRearrFactors(return_random_promo=True),
                random_seed=1,
            ).to_url()
        )
        self.assertFragmentIn(response, {'sourceType': 'carter'})

        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2226,
                allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET],
                rearr_factors=CarterRearrFactors(return_random_promo=True),
                random_seed=2,
            ).to_url()
        )
        self.assertFragmentIn(response, {'sourceType': 'persBasket'})

    @classmethod
    def prepare_pers_basket_filter_by_bigb(cls):
        offer_info_place = cls.mars.api_report.offer_info()
        dyno = cls.mars.dyno
        bigb_profiles = cls.mars.bigb.bigb_profiles()

        price_and_promo_respones = TPriceAndPromos()
        res = price_and_promo_respones.Search.Results.add()
        res.WareId = "offer6"
        res.Prices.Value = 210

        offers_info = TPersBasketItems(
            Items=[
                TPersBasketItem(OfferInfo=TOfferInfo(WareMd5="offer6")),
                TPersBasketItem(OfferInfo=TOfferInfo(WareMd5="offer7")),
            ]
        )

        profile = bigb_profiles.create_pers_basket_offers(["offer7"])

        offer_info_place.add(price_and_promo_respones, user_id_type=UserIdType.UID, user_any_id=2228)
        dyno.add_pers_basket(UserIdType.UID, "2228", offers_info)
        bigb_profiles.add(puid=2228, profile=profile)

    def test_pers_basket_filter_by_bigb(self):
        """Проверяем, что происходит фильтрация по профилю bigb:
        1. По счётчикам профиля оффера не добавляются - только убираются.
        """
        response = self.mars.request_json(
            MordaCarterUrlParams(user_id=2228, allowed_source_types=[ESourceType.ST_PERS_BASKET]).to_url()
        )
        self.assertFragmentIn(
            response, {"persBasketItems": [{"objId": "offer6"}], "sourceType": "persBasket"}, allow_different_len=False
        )

    @classmethod
    def prepare_pers_basket_empty_cart_when_no_items(cls):
        offer_info_place = cls.mars.api_report.offer_info()
        cart = cls.mars.carter.get_cart()
        bigb_profiles = cls.mars.bigb.bigb_profiles()
        dyno = cls.mars.dyno

        offers_info = TPersBasketItems(Items=[TPersBasketItem(OfferInfo=TOfferInfo(WareMd5="offer1"))])
        raw_cart = create_raw_cart(id=-1, items=[])
        report_response = TPriceAndPromos()
        profile = bigb_profiles.create_pers_basket_offers(["offer1"])

        dyno.add_pers_basket(UserIdType.UID, "2229", offers_info)
        cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=2229)
        offer_info_place.add(report_response, user_id_type=UserIdType.UID, user_any_id=2229)
        bigb_profiles.add(puid=2229, profile=profile)

    def test_pers_basket_empty_cart_when_no_items(self):
        """Проверяем, что формируется пустой ответ оригинального картера, когда товаров нет ни в корзине, ни в избранном.
        Если в избранном товары могут отфильтроваться только через bigb, то из картера может прийти пустой ответ.
        При форсировании ответа, формируется ответ для разрешенных источников.
        Если указано избранное, и товаров не набралось, то выдаётся ошибка, так как нет промомеханики пустого избранного."""
        # есть корзина и избранное - выбирается избранное
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2229, allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET]
            ).to_url()
        )
        self.assertFragmentIn(response, {"items": EmptyList(), "sourceType": "carter"})

        # только корзина - выбирается корзина
        response = self.mars.request_json(
            MordaCarterUrlParams(user_id=2229, allowed_source_types=[ESourceType.ST_CARTER]).to_url()
        )
        self.assertFragmentIn(response, {"items": EmptyList(), "sourceType": "carter"})

        # только избранное + флаг debug - выбирается избранное
        response = self.mars.request_json(
            MordaCarterUrlParams(user_id=2229, debug=True, allowed_source_types=[ESourceType.ST_PERS_BASKET]).to_url()
        )
        self.assertFragmentIn(response, {"persBasketItems": EmptyList(), "sourceType": "persBasket"})

        # только избранное + флаг debug - ответ не формируется
        response = self.mars.request_json(
            MordaCarterUrlParams(user_id=2229, debug=False, allowed_source_types=[ESourceType.ST_PERS_BASKET]).to_url(),
            fail_on_error=False,
        )
        self.assertFragmentIn(
            response, {"message": Regex(".*Can\'t create TAvailableCarts when carts are empty."), "status": "error"}
        )
        self.common_log.expect(message=Regex("Error response.*"), severity=Severity.ERROR)

    def test_pers_basket_debug_response(self):
        """Проверяем, что в дебаг выдаче есть сырые ответы:
        1. Репорта, при запросе офферов из избранного
        2. Dyno, при запросе офферов из таблицы
        3. Профиль bigb без пустых полей
        """
        response = self.mars.request_json(
            MordaCarterUrlParams(user_id=2228, allowed_source_types=[ESourceType.ST_PERS_BASKET], debug=True).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "rawDynoPersBasketResponse": NotEmptyDict(),
                    "rawReportPersBasketOfferInfoResponse": NotEmptyDict(),
                    "rawBigbProfile": NotEmptyDict(),
                    "rawCarterResponse": None,
                    "rawPerksResponse": None,
                    "rawReportCarterOfferInfoResponse": None,
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_pers_basket_error_metrics(cls):
        cart = cls.mars.carter.get_cart()
        offer_info_place = cls.mars.api_report.offer_info()
        bigb_profiles = cls.mars.bigb.bigb_profiles()
        dyno = cls.mars.dyno

        offers_info = TPersBasketItems(Items=[TPersBasketItem(OfferInfo=TOfferInfo(WareMd5="offer1"))])
        raw_cart = create_raw_cart(id=-1, items=[])
        report_response = TPriceAndPromos()
        res = report_response.Search.Results.add()
        res.WareId = "offer1"
        res.Prices.Value = 210
        profile = bigb_profiles.create_pers_basket_offers()

        # error at bigb
        cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=2231)
        dyno.add_pers_basket(UserIdType.UID, "2231", offers_info)
        offer_info_place.add(report_response, user_id_type=UserIdType.UID, user_any_id=2231)

        # error at report
        cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=2232)
        dyno.add_pers_basket(UserIdType.UID, "2232", offers_info)
        bigb_profiles.add(puid=2232, profile=profile)

    def test_pers_basket_error_metrics(self):
        """Проверяем, что метрики ошибок считаются для запросов в:
        1. Dyno - для офферов избранного
        2. Bigb - для профиля пользователя
        3. Report - для товаров избранного
        """
        for uid, (dyno_error, bigb_error, report_error) in [
            (2228, (0, 0, 0)),
            (2231, (0, 1, 0)),
            (2232, (0, 0, 1)),
        ]:
            with self.subTest(uid=uid, dyno_error=dyno_error, bigb_error=bigb_error, report_error=report_error):
                stat_respones = self.mars.request_json('stat')
                self.assertFragmentIn(stat_respones, ['pers_basket_data_provider_failed_dmmm', 0])
                self.assertFragmentIn(stat_respones, ['bigb_request_failed_dmmm', 0])
                self.assertFragmentIn(stat_respones, ['pers_basket_actualizer_failed_dmmm', 0])

                self.mars.request_json(
                    MordaCarterUrlParams(
                        user_id=uid, allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET]
                    ).to_url()
                )

                stat_respones = self.mars.request_json('stat')
                self.assertFragmentIn(stat_respones, ['pers_basket_data_provider_failed_dmmm', dyno_error])
                self.assertFragmentIn(stat_respones, ['bigb_request_failed_dmmm', bigb_error])
                self.assertFragmentIn(stat_respones, ['pers_basket_actualizer_failed_dmmm', report_error])

                self.assertFragmentIn(stat_respones, ['pers_basket_data_provider_time_hgram'])
                self.assertFragmentIn(stat_respones, ['bigb_request_time_hgram'])
                self.assertFragmentIn(stat_respones, ['pers_basket_actualizer_time_hgram'])

                self.mars.request_text("stat/reset", "POST")

    @classmethod
    def prepare_pers_basket_filter_adult(cls):
        bigb_profiles = cls.mars.bigb.bigb_profiles()
        dyno = cls.mars.dyno

        offers_info = TPersBasketItems(
            Items=[
                TPersBasketItem(OfferInfo=TOfferInfo(WareMd5="offer1", IsAdult=False)),
                TPersBasketItem(OfferInfo=TOfferInfo(WareMd5="offer2", IsAdult=True)),
            ]
        )

        profile = bigb_profiles.create_pers_basket_offers()

        dyno.add_pers_basket(UserIdType.UID, "2233", offers_info)
        bigb_profiles.add(puid=2233, profile=profile)

    def test_pers_basket_filter_adult(self):
        """Проверяем, что в запрос для репорта не попадают отфильтрованные офферы"""
        self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2233, allowed_source_types=[ESourceType.ST_PERS_BASKET], show_adult=False
            ).to_url(),
            fail_on_error=False,
        )
        self.common_log.expect(message=Regex("Error response.*"), severity=Severity.ERROR)
        self.trace_log.expect(
            request_method='/yandsearch',
            query_params=Regex('.*(?<!offerid=offer2&)offerid=offer1(?!&offerid=offer2).*'),
            http_code=400,
            type='OUT',
        )

    @classmethod
    def prepare_cart_with_all_promos_counters(cls):
        """
        Creating a cart with one item with the following promos:
        promo-code, blue-flash, discount, price-drop, coupon
        """
        bigb_profiles = cls.mars.bigb.bigb_profiles()
        cart = cls.mars.carter.get_cart()
        offer_info = cls.mars.api_report.offer_info()
        perk_status = cls.mars.loyalty.perk_status()
        dyno = cls.mars.dyno

        raw_cart = create_raw_cart(id=5, items=[create_cart_item(id=50, obj_id='50', price=110)])

        price_and_promos = TPriceAndPromos()
        res = price_and_promos.Search.Results.add()
        res.WareId = '50'
        res.Prices.Value = 100
        # simple discount
        res.Prices.Discount.OldMin = 100
        res.Prices.Discount.Percent = 10
        res.Prices.Discount.Absolute = 90
        promo = res.Promos.add()
        promo.Type = PromoType.DIRECT_DISCOUNT.value
        promo.IsPersonal = True
        promo = res.Promos.add()
        promo.Type = PromoType.PROMO_CODE.value
        promo = res.Promos.add()
        promo.Type = PromoType.BLUE_FLASH.value
        promo = res.Promos.add()
        promo.Type = PromoType.DIRECT_DISCOUNT.value
        # price-drop
        promo.ItemsInfo.Discount.OldMin = 100
        promo.ItemsInfo.Discount.Percent = 20
        promo.ItemsInfo.PromoPrice.Value = 80

        profile_with_coins = Profile(market_loyalty_coins=[bigb_profiles.create_coupon_coin(1, 1)])

        perk_status_data = TPerkStatus()
        perk_status_data.Statuses.add().Perks.extend(['perk1', 'perk2'])

        offers_info = TPersBasketItems(Items=[TPersBasketItem(OfferInfo=TOfferInfo(WareMd5='50'))])

        carter_counters = [
            BigbCounter(
                counter_id=ECounterId.CI_MARKET_PROMO_TRIGGERS_FLASH_SHOW_COUNT,
                platform_types=[EPlatformType.PT_WEB_DESKTOP],
                values=[1],
            ),
            BigbCounter(
                counter_id=ECounterId.CI_MARKET_PROMO_TRIGGERS_PROMOCODE_SHOW_COUNT,
                platform_types=[EPlatformType.PT_WEB_DESKTOP],
                values=[1],
            ),
            BigbCounter(
                counter_id=ECounterId.CI_MARKET_PROMO_TRIGGERS_DISCOUNT_SHOW_COUNT,
                platform_types=[EPlatformType.PT_WEB_DESKTOP],
                values=[1],
            ),
            BigbCounter(
                counter_id=ECounterId.CI_MARKET_PROMO_TRIGGERS_PRICE_DROP_SHOW_COUNT,
                platform_types=[EPlatformType.PT_WEB_DESKTOP],
                values=[1],
            ),
            BigbCounter(
                counter_id=ECounterId.CI_MARKET_PROMO_TRIGGERS_PERS_DISCOUNT_SHOW_COUNT,
                platform_types=[EPlatformType.PT_WEB_DESKTOP],
                values=[1],
            ),
            BigbCounter(
                counter_id=ECounterId.CI_MARKET_PROMO_TRIGGERS_COUPON_SHOW_COUNT,
                platform_types=[EPlatformType.PT_WEB_DESKTOP],
                values=[1],
            ),
        ]
        pers_basket_counters = [
            BigbCounter(
                counter_id=ECounterId.CI_MARKET_PERS_BASKET_PROMO_TRIGGERS_FLASH_SHOW_COUNT,
                platform_types=[EPlatformType.PT_WEB_DESKTOP],
                values=[1],
            ),
            BigbCounter(
                counter_id=ECounterId.CI_MARKET_PERS_BASKET_PROMO_TRIGGERS_PROMOCODE_SHOW_COUNT,
                platform_types=[EPlatformType.PT_WEB_DESKTOP],
                values=[1],
            ),
            BigbCounter(
                counter_id=ECounterId.CI_MARKET_PERS_BASKET_PROMO_TRIGGERS_DISCOUNT_SHOW_COUNT,
                platform_types=[EPlatformType.PT_WEB_DESKTOP],
                values=[1],
            ),
            BigbCounter(
                counter_id=ECounterId.CI_MARKET_PERS_BASKET_PROMO_TRIGGERS_PERS_DISCOUNT_SHOW_COUNT,
                platform_types=[EPlatformType.PT_WEB_DESKTOP],
                values=[1],
            ),
        ]

        for puid_base, counters, add_pers_basket in [
            (5000, carter_counters, False),
            (6000, pers_basket_counters, True),
        ]:
            for idx, counter in enumerate(counters):
                puid = puid_base + idx

                cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=puid)
                offer_info.add(price_and_promos, user_id_type=UserIdType.UID, user_any_id=puid)
                perk_status.add(
                    perk_status_data, uid=puid, perk_types=sorted([perk_type.value for perk_type in PerkTypes])
                )

                counter.values[0] = 0
                bigb_profiles.assign_promo_shows_counters(profile_with_coins, [*carter_counters, *pers_basket_counters])
                bigb_profiles.add(puid=puid, profile=profile_with_coins)
                counter.values[0] = 1

                if add_pers_basket:
                    dyno.add_pers_basket(UserIdType.UID, str(puid), offers_info)

    # ----------------------------------
    # Testing promo counters for carter
    # ----------------------------------

    def test_promo_counter_blue_flash(self):
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=5000,
                rearr_factors=CarterRearrFactors(use_promo_counters=True),
                allowed_source_types=[ESourceType.ST_CARTER],
            ).to_url()
        )
        self.assertFragmentIn(response, {'sourceType': 'carter', 'promo': {'promoType': 'blue-flash'}})
        self.promo_triggers_log.expect(
            user_id_type=EUserIdType.UIT_UID,
            user_id=5000,
            trigger_type=ETriggerType.TT_SHOW_FLASH,
            platform_type=EPlatformType.PT_WEB_DESKTOP,
            event_type=EEventType.ET_RETURNED_FROM_MARS,
            widget_position=0xFFFFFFFF,
            widget_position_type=ECartWidgetPositionType.CWPT_TOP_POSITION_PP,
        )

    def test_promo_counter_promo_code(self):
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=5001,
                rearr_factors=CarterRearrFactors(use_promo_counters=True),
                allowed_source_types=[ESourceType.ST_CARTER],
            ).to_url()
        )
        self.assertFragmentIn(response, {'sourceType': 'carter', 'promo': {'promoType': 'promo-code'}})
        self.promo_triggers_log.expect(
            user_id_type=EUserIdType.UIT_UID,
            user_id=5001,
            trigger_type=ETriggerType.TT_SHOW_PROMO_CODE,
            platform_type=EPlatformType.PT_WEB_DESKTOP,
            event_type=EEventType.ET_RETURNED_FROM_MARS,
            widget_position=0xFFFFFFFF,
            widget_position_type=ECartWidgetPositionType.CWPT_TOP_POSITION_PP,
        )

    def test_promo_counter_discount(self):
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=5002,
                rearr_factors=CarterRearrFactors(use_promo_counters=True),
                allowed_source_types=[ESourceType.ST_CARTER],
            ).to_url()
        )
        self.assertFragmentIn(
            response, {'sourceType': 'carter', 'promo': {'promoType': 'items-discount', 'isPersonal': False}}
        )
        self.promo_triggers_log.expect(
            user_id_type=EUserIdType.UIT_UID,
            user_id=5002,
            trigger_type=ETriggerType.TT_SHOW_DISCOUNT,
            platform_type=EPlatformType.PT_WEB_DESKTOP,
            event_type=EEventType.ET_RETURNED_FROM_MARS,
            widget_position=0xFFFFFFFF,
            widget_position_type=ECartWidgetPositionType.CWPT_TOP_POSITION_PP,
        )

    def test_promo_counter_price_drop(self):
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=5003,
                rearr_factors=CarterRearrFactors(use_promo_counters=True),
                allowed_source_types=[ESourceType.ST_CARTER],
            ).to_url()
        )
        self.assertFragmentIn(response, {'sourceType': 'carter', 'promo': {'promoType': 'price-drop'}})
        self.promo_triggers_log.expect(
            user_id_type=EUserIdType.UIT_UID,
            user_id=5003,
            trigger_type=ETriggerType.TT_SHOW_PRICE_DROP,
            platform_type=EPlatformType.PT_WEB_DESKTOP,
            event_type=EEventType.ET_RETURNED_FROM_MARS,
            widget_position=0xFFFFFFFF,
            widget_position_type=ECartWidgetPositionType.CWPT_TOP_POSITION_PP,
        )

    def test_promo_counter_pers_discount(self):
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=5004,
                rearr_factors=CarterRearrFactors(use_promo_counters=True),
                allowed_source_types=[ESourceType.ST_CARTER],
            ).to_url()
        )
        self.assertFragmentIn(
            response, {'sourceType': 'carter', 'promo': {'promoType': 'items-discount', 'isPersonal': True}}
        )
        self.promo_triggers_log.expect(
            user_id_type=EUserIdType.UIT_UID,
            user_id=5004,
            trigger_type=ETriggerType.TT_SHOW_PERS_DISCOUNT,
            platform_type=EPlatformType.PT_WEB_DESKTOP,
            event_type=EEventType.ET_RETURNED_FROM_MARS,
            widget_position=0xFFFFFFFF,
            widget_position_type=ECartWidgetPositionType.CWPT_TOP_POSITION_PP,
        )

    def test_promo_counter_coupon(self):
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=5005,
                rearr_factors=CarterRearrFactors(use_promo_counters=True),
                use_bigb=True,
                allowed_source_types=[ESourceType.ST_CARTER],
            ).to_url()
        )
        self.assertFragmentIn(response, {'sourceType': 'carter', 'promo': {'promoType': 'coupon'}})
        self.promo_triggers_log.expect(
            user_id_type=EUserIdType.UIT_UID,
            user_id=5005,
            trigger_type=ETriggerType.TT_SHOW_COUPON,
            platform_type=EPlatformType.PT_WEB_DESKTOP,
            event_type=EEventType.ET_RETURNED_FROM_MARS,
            widget_position=0xFFFFFFFF,
            widget_position_type=ECartWidgetPositionType.CWPT_TOP_POSITION_PP,
        )

    # ---------------------------------------
    # Testing promo counters for pers basket
    # ---------------------------------------

    def test_pers_basket_promo_counter_blue_flash(self):
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=6000,
                rearr_factors=CarterRearrFactors(use_promo_counters=True),
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
            ).to_url()
        )
        self.assertFragmentIn(response, {'sourceType': 'persBasket', 'persBasketPromo': {'promoType': 'blue-flash'}})
        self.promo_triggers_log.expect(
            user_id_type=EUserIdType.UIT_UID,
            user_id=6000,
            trigger_type=ETriggerType.TT_SHOW_PERS_BASKET_FLASH,
            platform_type=EPlatformType.PT_WEB_DESKTOP,
            event_type=EEventType.ET_RETURNED_FROM_MARS,
            widget_position=0xFFFFFFFF,
            widget_position_type=ECartWidgetPositionType.CWPT_TOP_POSITION_PP,
        )

    def test_pers_basket_promo_counter_promo_code(self):
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=6001,
                rearr_factors=CarterRearrFactors(use_promo_counters=True),
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
            ).to_url()
        )
        self.assertFragmentIn(response, {'sourceType': 'persBasket', 'persBasketPromo': {'promoType': 'promo-code'}})
        self.promo_triggers_log.expect(
            user_id_type=EUserIdType.UIT_UID,
            user_id=6001,
            trigger_type=ETriggerType.TT_SHOW_PERS_BASKET_PROMO_CODE,
            platform_type=EPlatformType.PT_WEB_DESKTOP,
            event_type=EEventType.ET_RETURNED_FROM_MARS,
            widget_position=0xFFFFFFFF,
            widget_position_type=ECartWidgetPositionType.CWPT_TOP_POSITION_PP,
        )

    def test_pers_basket_promo_counter_discount(self):
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=6002,
                rearr_factors=CarterRearrFactors(use_promo_counters=True),
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {'sourceType': 'persBasket', 'persBasketPromo': {'promoType': 'items-discount', 'isPersonal': False}},
        )
        self.promo_triggers_log.expect(
            user_id_type=EUserIdType.UIT_UID,
            user_id=6002,
            trigger_type=ETriggerType.TT_SHOW_PERS_BASKET_DISCOUNT,
            platform_type=EPlatformType.PT_WEB_DESKTOP,
            event_type=EEventType.ET_RETURNED_FROM_MARS,
            widget_position=0xFFFFFFFF,
            widget_position_type=ECartWidgetPositionType.CWPT_TOP_POSITION_PP,
        )

    def test_pers_basket_promo_counter_pers_discount(self):
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=6003,
                rearr_factors=CarterRearrFactors(use_promo_counters=True),
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {'sourceType': 'persBasket', 'persBasketPromo': {'promoType': 'items-discount', 'isPersonal': True}},
        )
        self.promo_triggers_log.expect(
            user_id_type=EUserIdType.UIT_UID,
            user_id=6003,
            trigger_type=ETriggerType.TT_SHOW_PERS_BASKET_PERS_DISCOUNT,
            platform_type=EPlatformType.PT_WEB_DESKTOP,
            event_type=EEventType.ET_RETURNED_FROM_MARS,
            widget_position=0xFFFFFFFF,
            widget_position_type=ECartWidgetPositionType.CWPT_TOP_POSITION_PP,
        )

    def test_promo_show_counter_limits(self):
        """
        In this test, we check that neither promo nor persBasketPromo will be returned
        because PromoShowCounterLimit for each promo is 1 (set in service.py).
        Show counters for each promo except one has been set in prepare_cart_with_all_promos_counters
        and they are equal to 1.
        Thus, in the test for each id we removed from the allowed_promos a promo for which the counter is 0.
        It means that promo comparator will get promos with counter equal to 1 and filter them out.
        """
        allowed_promos = {
            EPromoMechanics.PM_BLUE_FLASH,
            EPromoMechanics.PM_PROMO_CODE,
            EPromoMechanics.PM_ITEMS_DISCOUNT,
            EPromoMechanics.PM_COUPON,
            EPromoMechanics.PM_PRICE_DROP,
            EPromoMechanics.PM_PERSONAL_ITEMS_DISCOUNT,
        }
        for id, source_type, forbidden_promo in [
            (5000, ESourceType.ST_CARTER, {EPromoMechanics.PM_BLUE_FLASH}),
            (5001, ESourceType.ST_CARTER, {EPromoMechanics.PM_PROMO_CODE}),
            (5002, ESourceType.ST_CARTER, {EPromoMechanics.PM_ITEMS_DISCOUNT}),
            (5003, ESourceType.ST_CARTER, {EPromoMechanics.PM_PRICE_DROP}),
            (5004, ESourceType.ST_CARTER, {EPromoMechanics.PM_PERSONAL_ITEMS_DISCOUNT}),
            (5005, ESourceType.ST_CARTER, {EPromoMechanics.PM_COUPON}),
            (6000, ESourceType.ST_PERS_BASKET, {EPromoMechanics.PM_BLUE_FLASH}),
            (6001, ESourceType.ST_PERS_BASKET, {EPromoMechanics.PM_PROMO_CODE}),
            (6002, ESourceType.ST_PERS_BASKET, {EPromoMechanics.PM_ITEMS_DISCOUNT}),
            (6003, ESourceType.ST_PERS_BASKET, {EPromoMechanics.PM_PERSONAL_ITEMS_DISCOUNT}),
        ]:
            response = self.mars.request_json(
                MordaCarterUrlParams(
                    user_id=id,
                    use_bigb=True,
                    rearr_factors=CarterRearrFactors(use_promo_counters=True, use_promo_counter_limits=True),
                    allowed_source_types=[source_type],
                    allowed_promos_carter=allowed_promos - forbidden_promo,
                    allowed_promos_pers_basket=allowed_promos - forbidden_promo,
                ).to_url()
            )
            # FIXME(smorzhov): sourceType check is a workaround and
            # can be removed when https://st.yandex-team.ru/MSI-1056 will be closed
            self.assertFragmentIn(
                response,
                {get_promo_name(source_type): Absent(), 'sourceType': get_json_name(source_type, ESourceType)},
            )

    @classmethod
    def prepare_cart_with_widget_position_counters(cls):
        """
        Creating a cart with one item without any promos and a cart with one promo.
        Creating bigb profiles with widget position counters to check how limits works
        """
        bigb_profiles = cls.mars.bigb.bigb_profiles()
        cart = cls.mars.carter.get_cart()
        offer_info = cls.mars.api_report.offer_info()
        dyno = cls.mars.dyno

        # cart without promo
        raw_cart_without_promo = create_raw_cart(id=7, items=[create_cart_item(id=70, obj_id='70', price=100)])

        cart_price = TPriceAndPromos()
        res = cart_price.Search.Results.add()
        res.WareId = '70'
        res.Prices.Value = 100

        # cart with promo code
        raw_cart_with_promo = create_raw_cart(id=8, items=[create_cart_item(id=80, obj_id='80', price=100)])

        cart_price_and_promo = TPriceAndPromos()
        res = cart_price_and_promo.Search.Results.add()
        res.WareId = '80'
        res.Prices.Value = 100
        promo = res.Promos.add()
        promo.Type = PromoType.PROMO_CODE.value
        promo = res.Promos.add()

        pers_basket_items_with_promo = TPersBasketItems(Items=[TPersBasketItem(OfferInfo=TOfferInfo(WareMd5='80'))])

        @dataclass
        class TestData:
            puid_base: int
            raw_cart: TRawCart
            report_data: TPriceAndPromos
            counters: list[BigbCounter]
            pers_basket_items: Optional[TPersBasketItems] = None

        test_data_array = [
            TestData(
                puid_base=7000,
                raw_cart=raw_cart_without_promo,
                report_data=cart_price,
                counters=[
                    # Empty counter. A full cart and a cart with promo can be shown at the top
                    BigbCounter(),
                    BigbCounter(
                        counter_id=ECounterId.CI_MARKET_PROMO_TRIGGERS_TOP_CART_POSITION_PP_SHOW_COUNT,
                        platform_types=[EPlatformType.PT_WEB_DESKTOP],
                        # Limit for a full cart
                        values=[2],
                    ),
                ],
            ),
            TestData(
                puid_base=8000,
                raw_cart=raw_cart_with_promo,
                report_data=cart_price_and_promo,
                counters=[
                    BigbCounter(),
                    BigbCounter(
                        counter_id=ECounterId.CI_MARKET_PROMO_TRIGGERS_TOP_CART_POSITION_PP_SHOW_COUNT,
                        platform_types=[EPlatformType.PT_WEB_DESKTOP],
                        # Limit for a cart with promo
                        values=[3],
                    ),
                ],
            ),
            TestData(
                puid_base=8100,
                raw_cart=raw_cart_with_promo,
                report_data=cart_price_and_promo,
                pers_basket_items=pers_basket_items_with_promo,
                counters=[
                    BigbCounter(),
                    BigbCounter(
                        counter_id=ECounterId.CI_MARKET_PROMO_TRIGGERS_TOP_CART_POSITION_PP_SHOW_COUNT,
                        platform_types=[EPlatformType.PT_WEB_DESKTOP],
                        # Limit for a cart with promo
                        values=[3],
                    ),
                ],
            ),
        ]

        for test_data in test_data_array:
            for idx, counter in enumerate(test_data.counters):
                puid = test_data.puid_base + idx

                cart.add(test_data.raw_cart, user_id_type=UserIdType.UID, user_any_id=puid)
                offer_info.add(test_data.report_data, user_id_type=UserIdType.UID, user_any_id=puid)

                profile = bigb_profiles.create_promo_shows_counters([counter])
                bigb_profiles.add(puid=puid, profile=profile)

                if test_data.pers_basket_items is not None:
                    dyno.add_pers_basket(UserIdType.UID, str(puid), test_data.pers_basket_items)

    def test_full_cart_widget_position_counters(self):
        """
        The limit for full cart to be shown on the top position is 1 (see service.py)
        That is why we expect to get CWPT_UNDEFINED for 7001
        (corresponding counter is 2, see prepare_cart_with_widget_position_counters)
        """
        for uid, shortcut_on_top, widget_position_type, source_type in [
            (7000, True, ECartWidgetPositionType.CWPT_TOP_POSITION_PP, ESourceType.ST_CARTER),
            (7001, False, ECartWidgetPositionType.CWPT_UNDEFINED, ESourceType.ST_CARTER),
        ]:
            response = self.mars.request_json(
                MordaCarterUrlParams(
                    user_id=uid, use_widget_position_counter_limits=True, allowed_source_types=[source_type]
                ).to_url()
            )
            self.assertFragmentIn(
                response,
                {
                    get_promo_name(source_type): Absent(),
                    'flags': {'shortcutOnTop': shortcut_on_top},
                    # TODO(smorzhov) check event objects instead of webhooks
                    # 'webhooks': {
                    #     'click': LikeUrl.of(
                    #         'http://testing-mars/carter/logger?event=click&platform=web_desktop&trigger=full_carter'
                    #         f'&userId={uid}&userIdType=UID'
                    #         f'&widgetPositionType={get_json_name(widget_position_type, ECartWidgetPositionType)}'
                    #     ),
                    #     'show': LikeUrl.of(
                    #         'http://testing-mars/carter/logger?event=show&platform=web_desktop&trigger=full_carter'
                    #         f'&userId={uid}&userIdType=UID'
                    #         f'&widgetPositionType={get_json_name(widget_position_type, ECartWidgetPositionType)}'
                    #     ),
                    # },
                },
            )

    def test_cart_with_promo_widget_position_counters(self):
        """
        The limit for full cart to be shown on the top position is 2 (see service.py)
        That is why we expect to get CWPT_UNDEFINED for 8001
        (corresponding counter is 3, see prepare_cart_with_widget_position_counters)
        """
        for uid, shortcut_on_top, widget_position_type, source_type, trigger in [
            (8000, True, ECartWidgetPositionType.CWPT_TOP_POSITION_PP, ESourceType.ST_CARTER, 'promo_code'),
            (8001, False, ECartWidgetPositionType.CWPT_UNDEFINED, ESourceType.ST_CARTER, 'promo_code'),
            (
                8100,
                True,
                ECartWidgetPositionType.CWPT_TOP_POSITION_PP,
                ESourceType.ST_PERS_BASKET,
                'pers_basket_promo_code',
            ),
            (8101, False, ECartWidgetPositionType.CWPT_UNDEFINED, ESourceType.ST_PERS_BASKET, 'pers_basket_promo_code'),
        ]:
            response = self.mars.request_json(
                MordaCarterUrlParams(
                    user_id=uid, use_widget_position_counter_limits=True, allowed_source_types=[source_type]
                ).to_url()
            )
            self.assertFragmentIn(
                response,
                {
                    get_promo_name(source_type): {'promoType': 'promo-code'},
                    'flags': {'shortcutOnTop': shortcut_on_top},
                    # TODO(smorzhov) check event objects instead of webhooks
                    # 'webhooks': {
                    #     'click': LikeUrl.of(
                    #         f'http://testing-mars/carter/logger?event=click&platform=web_desktop&trigger={trigger}'
                    #         f'&userId={uid}&userIdType=UID'
                    #         f'&widgetPositionType={get_json_name(widget_position_type, ECartWidgetPositionType)}'
                    #     ),
                    #     'show': LikeUrl.of(
                    #         f'http://testing-mars/carter/logger?event=show&platform=web_desktop&trigger={trigger}&'
                    #         f'userId={uid}&userIdType=UID'
                    #         f'&widgetPositionType={get_json_name(widget_position_type, ECartWidgetPositionType)}'
                    #     ),
                    # },
                },
            )

    @classmethod
    def prepare_pers_basket_url_format(cls):
        bigb_profiles = cls.mars.bigb.bigb_profiles()
        offer_info = cls.mars.api_report.offer_info()
        sku_offers = cls.mars.api_report.sku_offers()
        dyno = cls.mars.dyno

        pers_basket_items = TPersBasketItems(
            Items=[
                TPersBasketItem(
                    OfferInfo=TOfferInfo(
                        WareMd5="offer1",
                        Msku=int(Skus.sku1),
                        Name="#1 Самый первый в мире...",
                        ImageMeta=TImageMeta(GroupId=88, Namespace="mpic", Key="img_smth-lol.jpeg"),
                    )
                ),
                TPersBasketItem(
                    OfferInfo=TOfferInfo(
                        WareMd5="offer2",
                        Msku=int(Skus.sku2),
                        Name="#2 Следующий сразу за...",
                        ImageMeta=TImageMeta(GroupId=89, Namespace="mpec", Key="img_smth-kek.jpeg"),
                    )
                ),
                TPersBasketItem(SkuInfo=TSkuInfo(Sku=Skus.sku3)),
                TPersBasketItem(SkuInfo=TSkuInfo(Sku=Skus.sku4)),
                TPersBasketItem(SkuInfo=TSkuInfo(Sku=Skus.sku5)),
            ]
        )

        price_and_promos = TPriceAndPromos()

        # offer1 - приходит от оффера и без промокода - не будет пиниться из-за того, что от оффера
        res = price_and_promos.Search.Results.add()
        res.WareId = "offer1"
        res.Prices.Value = 100

        # offer2 - приходит от оффера и с промокода - не будет пиниться из-за того, что от оффера
        res = price_and_promos.Search.Results.add()
        res.WareId = "offer2"
        res.Prices.Value = 120
        offer_info.add_promo_code(res, percent=10, old_min=120, promo_code="NIHAO")

        # offer3 - приходит от sku и с промокодом - запинется потому что от sku и с промокодом
        offer_item_3 = TSkuOffersItem(
            WareId="offer3",
            Prices=TPrices(Value=140),
            Promos=[create_promo_code(percent=10, old_min=140, promo_code="NIHAO")],
        )
        # offer4 - приходит от sku и без промокода - не запинется потому что от sku, но без промокода.
        # может также отфильтроваться, так как от репорта приходит метка IsAdult
        offer_item_4 = TSkuOffersItem(WareId="offer4", Prices=TPrices(Value=1400))
        # offer5 - приходит от sku и с промокодом - запинется потому что от sku и с промокодом
        offer_item_5 = TSkuOffersItem(
            WareId="offer5",
            Prices=TPrices(Value=1900),
            Promos=[create_promo_code(percent=10, old_min=1900, promo_code="NIHAO")],
        )
        sku_offers_response = TSkuOffers(
            Search=TSkuOffersSearch(
                Results=[
                    TSkuOffersResult(
                        Id=Skus.sku3,
                        IsAdult=False,
                        Titles=TSkuOffersResult.TTitles(Raw="#3 Последний, но не ..."),
                        Pictures=[
                            TSkuOffersResult.TPicture(
                                Original=TSkuOffersResult.TPicture.TOriginalPicture(
                                    GroupId=90, Namespace="mpec", Key="img_smth-pic.jpeg"
                                )
                            )
                        ],
                        Offers=TSkuOffersOffers(Items=[offer_item_3]),
                    ),
                    TSkuOffersResult(
                        Id=Skus.sku4,
                        IsAdult=True,
                        Titles=TSkuOffersResult.TTitles(Raw="#4 билет на рейв"),
                        Pictures=[
                            TSkuOffersResult.TPicture(
                                Original=TSkuOffersResult.TPicture.TOriginalPicture(
                                    GroupId=91, Namespace="mpec", Key="img_smth-😎🤙🏻.jpeg"
                                )
                            )
                        ],
                        Offers=TSkuOffersOffers(Items=[offer_item_4]),
                    ),
                    TSkuOffersResult(
                        Id=Skus.sku5,
                        IsAdult=False,
                        Titles=TSkuOffersResult.TTitles(Raw="#4 билет с рейва"),
                        Pictures=[
                            TSkuOffersResult.TPicture(
                                Original=TSkuOffersResult.TPicture.TOriginalPicture(
                                    GroupId=92, Namespace="mpec", Key="img_smth-🤨👉.jpeg"
                                )
                            )
                        ],
                        Offers=TSkuOffersOffers(Items=[offer_item_5]),
                    ),
                ]
            )
        )

        dyno.add_pers_basket(UserIdType.UID, "2234", pers_basket_items)
        offer_info.add(price_and_promos, user_id_type=UserIdType.UID, user_any_id="2234")
        sku_offers.add(sku_offers_response, user_id_type=UserIdType.UID, user_any_id="2234")
        bigb_profiles.add(puid=2234, profile=Profile())

    def test_pers_basket_filter_out_adult_sku(self):
        """
        Проверяем, что adult айтемы фильтруются для sku.
        """
        # show_adult = True
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234, allowed_source_types=[ESourceType.ST_PERS_BASKET], show_adult=True
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketItems": [
                    {"objId": "offer4", "price": 1400},
                ]
            },
        )
        # show_adult = False
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234, allowed_source_types=[ESourceType.ST_PERS_BASKET], show_adult=False
            ).to_url()
        )
        self.assertFragmentNotIn(
            response,
            {
                "persBasketItems": [
                    {"objId": "offer4", "price": 1400},
                ]
            },
        )

    def test_pers_basket_sku_item_format(self):
        """
        1. Проверяем, что формат для айтемов из sku содержит все поля.
        2. Проверяем, что несколько айтемов пинятся в persBasketUrls
        """
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234, allowed_source_types=[ESourceType.ST_PERS_BASKET], pin_sku=True, show_adult=True
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketUrls": {
                    "url": LikeUrl.of(
                        f"https://market.yandex.ru/my/wishlist?skuOffer={Skus.sku3};offer3&skuOffer={Skus.sku5};offer5"
                    )
                },
                "persBasketItems": [
                    {
                        "count": 1,
                        "discountAbsolute": 0,
                        "discountPrice": 1400,
                        "imageMeta": {"groupId": 91, "key": "img_smth-😎🤙🏻.jpeg", "namespace": "mpec"},
                        "isExpired": False,
                        "isInStock": True,
                        "msku": 22210986,
                        "name": "#4 билет на рейв",
                        "objId": "offer4",
                        "price": 1400,
                        "pricedropPromoEnabled": False,
                    }
                ],
            },
        )

    def test_pers_basket_url_format(self):
        """Проверяем, что в урл до избранного в cgi-параметрах sku передаются sku тех айтемов,
        которые во время подготовки пришли из sku (second reference) и для которых применяется промомеханика
        """
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234, allowed_source_types=[ESourceType.ST_PERS_BASKET], pin_sku=True, show_adult=True
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketUrls": {
                    "url": LikeUrl.of(
                        f"https://market.yandex.ru/my/wishlist?skuOffer={Skus.sku3};offer3&skuOffer={Skus.sku5};offer5"
                    )
                },
                "persBasketPromo": {"promoType": "promo-code", "items": [1, 2, 4]},
            },
        )

        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234, allowed_source_types=[ESourceType.ST_PERS_BASKET], pin_sku=False, show_adult=True
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketUrls": {"url": LikeUrl.of("https://market.yandex.ru/my/wishlist", no_params=['skuOffer'])},
                "persBasketPromo": {"promoType": "promo-code", "items": [1, 2, 4]},
            },
        )

    def test_pers_basket_clids(self):
        """Проверяем, что clid проставляется правильно в зависимости от переданных параметров"""

        # Desktop
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234,
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
                pin_sku=False,
                show_adult=True,
                device=EPlatformType.PT_WEB_DESKTOP,
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketUrls": {"url": LikeUrl.of("https://market.yandex.ru/my/wishlist?clid=991")},
                "persBasketPromo": {"promoType": "promo-code", "items": [1, 2, 4]},
            },
        )

        # Touch
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234,
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
                pin_sku=False,
                show_adult=True,
                device=EPlatformType.PT_WEB_TOUCH,
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketUrls": {"url": LikeUrl.of("https://m.market.yandex.ru/my/wishlist?clid=990")},
                "persBasketPromo": {"promoType": "promo-code", "items": [1, 2, 4]},
            },
        )

        # App
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234,
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
                pin_sku=False,
                show_adult=True,
                device=EPlatformType.PT_APP,
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketUrls": {"url": LikeUrl.of("https://m.market.yandex.ru/my/wishlist?clid=992")},
                "persBasketPromo": {"promoType": "promo-code", "items": [1, 2, 4]},
            },
        )

        # PP_ios
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234,
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
                pin_sku=False,
                show_adult=True,
                device=EPlatformType.PT_PP_IOS,
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketUrls": {"url": LikeUrl.of("https://m.market.yandex.ru/my/wishlist?clid=992")},
                "persBasketPromo": {"promoType": "promo-code", "items": [1, 2, 4]},
            },
        )

        # PP_android
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234,
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
                pin_sku=False,
                show_adult=True,
                device=EPlatformType.PT_PP_ANDROID,
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketUrls": {"url": LikeUrl.of("https://m.market.yandex.ru/my/wishlist?clid=992")},
                "persBasketPromo": {"promoType": "promo-code", "items": [1, 2, 4]},
            },
        )

        # Edadil_app
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234,
                allowed_source_types=[ESourceType.ST_PERS_BASKET],
                pin_sku=False,
                show_adult=True,
                device=EPlatformType.PT_EDADIL_APP,
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                "persBasketUrls": {"url": LikeUrl.of("https://m.market.yandex.ru/my/wishlist?clid=1320")},
                "persBasketPromo": {"promoType": "promo-code", "items": [1, 2, 4]},
            },
        )

    @classmethod
    def prepare_carter_pin_coupons_promo_id(cls):
        cart = cls.mars.carter.get_cart()
        bigb_profiles = cls.mars.bigb.bigb_profiles()
        offer_info = cls.mars.api_report.offer_info()

        coupon_coins = [
            bigb_profiles.create_coupon_coin(
                id=200, discount_price=200, condition_price=4000, end_date=DEFAULT_COUPON_END_DATE
            ),
            bigb_profiles.create_coupon_coin(
                id=500, discount_price=500, condition_price=10000, end_date=DEFAULT_COUPON_END_DATE
            ),
            bigb_profiles.create_coupon_coin(
                id=1000, discount_price=1000, condition_price=20000, end_date=DEFAULT_COUPON_END_DATE
            ),
        ]

        raw_cart = create_raw_cart(
            id=3,
            items=[
                create_cart_item(id=100, obj_id='100', price=38000),
            ],
        )

        price_and_promos = TPriceAndPromos()
        res = price_and_promos.Search.Results.add()
        res.WareId = '100'
        res.Prices.Value = 37999

        bigb_profiles.add(profile=Profile(market_loyalty_coins=coupon_coins), puid=2234)
        cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=2234)
        offer_info.add(price_and_promos, user_id_type=UserIdType.UID, user_any_id=2234)

    def test_carter_filter_coupons_by_promo_id(self):
        """Проверяем, что под флагом rearr-factors=filter_coupons_by_promo_id=smth1,smth2
        при выборе купона будут только те, что перечислены в filter_coupons_by_promo_id"""
        # Когда передан только один купон и он есть у пользователя, то выбертся он
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234,
                use_bigb=True,
                allowed_promos_carter=[EPromoMechanics.PM_COUPON],
                rearr_factors=CarterRearrFactors(filter_coupons_by_promo_id=["200"]),
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'promo': {
                    'promoType': 'coupon',
                    'discountAbsolute': 200,
                    'orderMinPrice': {'currency': 'RUR', 'value': 4000},
                },
            },
        )
        # Когда передано несколько купонов, то победитель будет выбираться среди переданных по обычным правилам (с макс скидкой)
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234,
                use_bigb=True,
                allowed_promos_carter=[EPromoMechanics.PM_COUPON],
                rearr_factors=CarterRearrFactors(filter_coupons_by_promo_id=["200", "500"]),
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'promo': {
                    'promoType': 'coupon',
                    'discountAbsolute': 500,
                    'orderMinPrice': {'currency': 'RUR', 'value': 10000},
                },
            },
        )
        # Когда не передано ни одного купона, то победитель будет выбираться по обычным правилам (с макс скидкой)
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234,
                use_bigb=True,
                allowed_promos_carter=[EPromoMechanics.PM_COUPON],
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'promo': {
                    'promoType': 'coupon',
                    'discountAbsolute': 1000,
                    'orderMinPrice': {'currency': 'RUR', 'value': 20000},
                },
            },
        )
        # Когда передан несуществующий у пользователя купон, то купона не должно быть в ответе
        response = self.mars.request_json(
            MordaCarterUrlParams(
                user_id=2234,
                use_bigb=True,
                allowed_promos_carter=[EPromoMechanics.PM_COUPON],
                rearr_factors=CarterRearrFactors(filter_coupons_by_promo_id=["123"]),
            ).to_url()
        )
        self.assertFragmentNotIn(
            response,
            {
                'promo': {
                    'promoType': 'coupon',
                },
            },
        )


if __name__ == '__main__':
    env.main()
