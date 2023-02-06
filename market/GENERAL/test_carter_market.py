from dataclasses import dataclass

from market.mars.lib.morda_cart_widget.proto.source_type_pb2 import ESourceType
from market.mars.lib.external.report.proto.price_and_promos_pb2 import TPriceAndPromos
from market.mars.lib.morda_cart_widget.proto.market_cart_type_pb2 import EMarketCartType
from market.mars.lib.morda_cart_widget.promo.proto.promo_types_pb2 import (
    ECouponDiscountType as DiscountType,
    EPromoMechanics,
)
from market.mars.lib.pers_basket.proto.offers_info_pb2 import (
    TPersBasketItems,
    TOfferInfo,
    TPersBasketItem,
)
from market.mars.lite.core.tools import get_json_name
from market.mars.lite.core.cart_widget_utils.util import (
    CarterHandlerPath,
    CarterUrlParams,
    CarterRearrFactors,
    create_raw_cart,
    create_cart_item,
)
from market.mars.lite.core.carter import PromoType, UserIdType

from dj.services.market.rtmr_actions.front_events.proto.source_type_pb2 import EShowReason
from dj.unity.lib.processors.market.proto.ids_pb2 import TItemPromoID
from market.library.shiny.lite.log import Severity
from market.proto.recom.item_promo_dj_response_pb2 import TResult
from market.pylibrary.lite.matcher import Regex, NotEmptyList, NotEmptyDict, Empty, Absent, EmptyList

import market.mars.lite.env as env


@dataclass
class MarketCarterUrlParams(CarterUrlParams):
    cart_type: int = EMarketCartType.MKT_ALL_ITEM_PROMO_PAIRS

    def to_query_mapping(self):
        self.carter_handler_path = CarterHandlerPath.MARKET
        mapping = super().to_query_mapping()
        mapping['cartType'] = get_json_name(self.cart_type, EMarketCartType)
        return mapping


class T(env.TestSuite):
    @classmethod
    def connect(cls):
        return {
            'carter': cls.mars.carter,
            'api_report': cls.mars.api_report,
            'bigb': cls.mars.bigb,
            'loyalty': cls.mars.loyalty,
            'dyno': cls.mars.dyno,
            'dj': cls.mars.dj,
        }

    @classmethod
    def prepare_simple_carts(cls):
        """
        Here we create:
        1) cart with 3 items.
           id=10, promo code, items discount
           id=20, blue flash (the best promo), items discount (will not be shown because it is equal to blue-flash)
           id=30, none
        2) pers basket with 1 item
           id=40, promo code

        Also we create show counters:
        1) (id=10, items-discount, cart) pair exceeded show limit
        2) (id=20, blue flash, cart) pair exceeded show limit
        3) (id=40, promo code, pers basket) pair exceeded show limit
        """
        offer_info = cls.mars.api_report.offer_info()
        cart = cls.mars.carter.get_cart()
        bigb_profiles = cls.mars.bigb.bigb_profiles()
        dyno = cls.mars.dyno
        item_promo_show_counters = cls.mars.dj.models_recommendation()

        # create cart items
        raw_cart = create_raw_cart(
            id=1,
            items=[
                create_cart_item(id=10, obj_id='10', price=110),
                create_cart_item(id=20, obj_id='20', price=1000),
                create_cart_item(id=30, obj_id='30', price=1300),
            ],
        )
        cart.add(raw_cart, user_id_type=UserIdType.UID, user_any_id=1)

        # create pers basket items
        pers_basket_items = TPersBasketItems(
            Items=[
                TPersBasketItem(OfferInfo=TOfferInfo(Msku=40, WareMd5='40')),
            ]
        )
        dyno.add_pers_basket(UserIdType.UID, '1', pers_basket_items)
        bigb_profile = bigb_profiles.create_pers_basket_offers()
        bigb_profiles.add(puid=1, profile=bigb_profile)

        # create report data with prices and promos for the cart
        carter_price_and_promos = TPriceAndPromos()

        res = carter_price_and_promos.Search.Results.add()
        res.WareId = '10'
        # simple discount
        res.Prices.Value = 110
        res.Prices.Discount.OldMin = 110
        res.Prices.Discount.Percent = 11
        res.Prices.Discount.Absolute = 10
        promo = res.Promos.add()
        promo.Type = PromoType.PROMO_CODE.value
        promo.PromoCode = 'promo_code'
        promo.ItemsInfo.DiscountType = get_json_name(DiscountType.DT_PERCENT, DiscountType)
        promo.ItemsInfo.PromoPrice.Discount.OldMin = 100
        promo.ItemsInfo.PromoPrice.Discount.Percent = 81
        promo.ItemsInfo.PromoPrice.Discount.Absolute = 81
        promo.Conditions = 'test_conditions_pc'

        res = carter_price_and_promos.Search.Results.add()
        res.WareId = '20'
        promo = res.Promos.add()
        promo.Type = PromoType.BLUE_FLASH.value
        promo.EndDate = 'end_date'
        promo.PromoKey = 'promo_key'
        promo.ItemsInfo.DiscountType = get_json_name(DiscountType.DT_ABSOLUTE, DiscountType)
        promo.ItemsInfo.Discount.OldMin = 1000
        promo.ItemsInfo.Discount.Percent = 20
        promo.ItemsInfo.PromoPrice.Value = 800
        promo.Key = 'bl_key_1'
        offer_info.assign_price(res.Prices, promo)

        res = carter_price_and_promos.Search.Results.add()
        res.WareId = '30'
        res.Prices.Value = 1300

        offer_info.add(carter_price_and_promos, user_id_type=UserIdType.UID, user_any_id=1)

        # create report data with prices and promos for the pers basket
        pers_basket_price_and_promos = TPriceAndPromos()

        res = pers_basket_price_and_promos.Search.Results.add()
        res.WareId = '40'
        res.Prices.Value = 120
        promo = res.Promos.add()
        promo.Type = PromoType.PROMO_CODE.value
        promo.PromoCode = 'promo_code_40'
        promo.ItemsInfo.DiscountType = get_json_name(DiscountType.DT_PERCENT, DiscountType)
        promo.ItemsInfo.PromoPrice.Discount.OldMin = 120
        promo.ItemsInfo.PromoPrice.Discount.Percent = 10
        promo.ItemsInfo.PromoPrice.Discount.Absolute = 12
        promo.Conditions = 'test_conditions_pc_40'
        promo.Key = 'pc_key_1'

        offer_info.add(pers_basket_price_and_promos, user_id_type=UserIdType.UID, user_any_id=1)

        # create item-promo show counters
        item_promo_id = TItemPromoID()
        item_promo_id.Msku = 10
        item_promo_id.PromoKey = 'items-discount_0_10_11'
        item_promo_id.ShowReason = EShowReason.ItemPromoInCart

        item_promo_counters = TResult()
        item_promo_counters.ItemPromoFeatures[item_promo_id.SerializeToString()].CounterValues['RT_Sum_30d'] = 10

        item_promo_id.Msku = 20
        item_promo_id.PromoKey = 'bl_key_1'
        item_promo_counters.ItemPromoFeatures[item_promo_id.SerializeToString()].CounterValues['RT_Sum_30d'] = 10

        item_promo_id.Msku = 40
        item_promo_id.PromoKey = 'pc_key_1'
        item_promo_id.ShowReason = EShowReason.ItemPromoInPersBasket
        item_promo_counters.ItemPromoFeatures[item_promo_id.SerializeToString()].CounterValues['RT_Sum_30d'] = 11

        item_promo_show_counters.add_item_promo_counters(counters=item_promo_counters, puid=1)

    def test_error_handling(self):
        """
        Check that if the request fails, the client returns an error message
        """
        # 1000 - invalid user id. Response will be an empty string
        response = self.mars.request_json(MarketCarterUrlParams(user_id=1000, debug=True).to_url(), fail_on_error=False)
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'rawCarterResponse': Empty(),
                    'rawDynoPersBasketResponse': Empty(),
                    'rawReportCarterOfferInfoResponse': Empty(),
                    'rawReportPersBasketOfferInfoResponse': Empty(),
                },
                'message': Regex('.*Failed to obtain original cart and pers basket'),
                'status': 'error',
            },
        )
        self.assertEqual(response.code, 430)
        self.common_log.expect(message=Regex('Error response.*'), severity=Severity.ERROR)

        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(stat_respones, ['carter_proxy_failed_dmmm', 1])
        self.assertFragmentIn(stat_respones, ['carter_actualizer_failed_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['promo_comparator_failed_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['market_widget_get_promo_show_counters_failed_dmmm', 0])
        self.assertFragmentIn(stat_respones, ['market_widget_transformer_failed_to_obtain_dj_counter_value_dmmm', 0])

    def test_debug_info_flag(self):
        """Checking the presence of debug information in the response if the debug flag is set"""
        response = self.mars.request_json(
            MarketCarterUrlParams(
                user_id=1,
                allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET],
                debug=True,
                rearr_factors=CarterRearrFactors(use_promo_counter_limits=True),
            ).to_url(),
            fail_on_error=False,
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'rawCarterResponse': NotEmptyList(),
                    'rawBigbProfile': NotEmptyDict(),
                    'rawReportCarterOfferInfoResponse': NotEmptyDict(),
                    'rawDynoPersBasketResponse': NotEmptyDict(),
                    'rawReportPersBasketOfferInfoResponse': NotEmptyDict(),
                    'rawDjProfile': NotEmptyDict(),
                }
            },
        )
        response = self.mars.request_json(
            MarketCarterUrlParams(
                user_id=1, allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET]
            ).to_url()
        )
        self.assertFragmentIn(response, {'debug': Absent(), 'totalCount': 4})

    @staticmethod
    def get_expected_offer_item_id_10(discount_absolute: int, discount_price: int):
        return {
            'count': 1,
            'source': 'offer',
            'discountAbsolute': discount_absolute,
            'discountPrice': discount_price,
            'id': 10,
            'isInStock': True,
            'objId': '10',
            'price': 110,
        }

    @staticmethod
    def get_expected_offer_item_id_20():
        return {
            'count': 1,
            'source': 'offer',
            'discountAbsolute': 200,
            'discountPrice': 800,
            'id': 20,
            'isInStock': True,
            'objId': '20',
            'price': 1000,
        }

    @staticmethod
    def get_expected_offer_item_id_40():
        return {
            'count': 1,
            'source': 'offer',
            'discountAbsolute': 12,
            'discountPrice': 108,
            'isInStock': True,
            'objId': '40',
            'price': 120,
        }

    @staticmethod
    def get_expected_promo_code(discount_absolute: int, discount_percent: int, promo_code: str, condition: str):
        return {
            'discountAbsolute': discount_absolute,
            'discountPercent': discount_percent,
            'discountType': 'percent',
            'meta': {'promoCode': promo_code},
            'conditions': condition,
            'promoType': 'promo-code',
        }

    @staticmethod
    def get_expected_blue_flash(discount_absolute: int, discount_percent: int):
        return {
            'discountAbsolute': discount_absolute,
            'discountPercent': discount_percent,
            'discountType': 'absolute',
            'promoType': 'blue-flash',
        }

    @staticmethod
    def get_expected_item_discount(discount_absolute: int, discount_percent: int, is_personal: bool = False):
        return {
            'discountAbsoluteTotal': discount_absolute,
            'discountPercent': discount_percent,
            'isPersonal': is_personal,
            'promoType': 'items-discount',
        }

    def test_all_item_promo_pairs(self):
        """
        Checking that all item-promo pairs are present in the response
        """
        response = self.mars.request_json(
            MarketCarterUrlParams(
                user_id=1, allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET]
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'items': [
                    {
                        'offerInfo': self.get_expected_offer_item_id_10(discount_absolute=91, discount_price=19),
                        # we expect that discount_absolute will not contain discount_absolute of the items-discount
                        'promo': self.get_expected_promo_code(
                            discount_absolute=81,
                            discount_percent=81,
                            promo_code='promo_code',
                            condition='test_conditions_pc',
                        ),
                        'sourceType': 'carter',
                    },
                    {
                        'offerInfo': self.get_expected_offer_item_id_10(discount_absolute=10, discount_price=100),
                        'promo': self.get_expected_item_discount(discount_absolute=10, discount_percent=11),
                        'sourceType': 'carter',
                    },
                    {
                        'offerInfo': self.get_expected_offer_item_id_20(),
                        'promo': self.get_expected_blue_flash(discount_absolute=200, discount_percent=20),
                        'sourceType': 'carter',
                    },
                    {
                        'offerInfo': self.get_expected_offer_item_id_40(),
                        'promo': self.get_expected_promo_code(
                            discount_absolute=12,
                            discount_percent=10,
                            promo_code='promo_code_40',
                            condition='test_conditions_pc_40',
                        ),
                        'sourceType': 'persBasket',
                    },
                ],
                'totalCount': 4,
            },
            allow_different_len=False,
        )

    def test_all_item_promo_pairs_shuffle(self):
        """
        Checking shuffling of item-promo pairs
        """
        response = self.mars.request_json(
            MarketCarterUrlParams(
                user_id=1,
                allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET],
                # it must be enough to check shuffling
                allowed_promos_carter=[EPromoMechanics.PM_BLUE_FLASH, EPromoMechanics.PM_ITEMS_DISCOUNT],
                allowed_promos_pers_basket=[EPromoMechanics.PM_BLUE_FLASH, EPromoMechanics.PM_ITEMS_DISCOUNT],
                rearr_factors=CarterRearrFactors(shuffle_preview=True),
                random_seed=1,
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'items': [
                    {
                        'offerInfo': self.get_expected_offer_item_id_10(discount_absolute=10, discount_price=100),
                        'promo': self.get_expected_item_discount(discount_absolute=10, discount_percent=11),
                        'sourceType': 'carter',
                    },
                    {
                        'offerInfo': self.get_expected_offer_item_id_20(),
                        'promo': self.get_expected_blue_flash(discount_absolute=200, discount_percent=20),
                        'sourceType': 'carter',
                    },
                ],
                'totalCount': 2,
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.mars.request_json(
            MarketCarterUrlParams(
                user_id=1,
                allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET],
                allowed_promos_carter=[EPromoMechanics.PM_BLUE_FLASH, EPromoMechanics.PM_ITEMS_DISCOUNT],
                allowed_promos_pers_basket=[EPromoMechanics.PM_BLUE_FLASH, EPromoMechanics.PM_ITEMS_DISCOUNT],
                rearr_factors=CarterRearrFactors(shuffle_preview=True),
                random_seed=3,
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'items': [
                    {
                        'offerInfo': self.get_expected_offer_item_id_20(),
                        'promo': self.get_expected_blue_flash(discount_absolute=200, discount_percent=20),
                        'sourceType': 'carter',
                    },
                    {
                        'offerInfo': self.get_expected_offer_item_id_10(discount_absolute=10, discount_price=100),
                        'promo': self.get_expected_item_discount(discount_absolute=10, discount_percent=11),
                        'sourceType': 'carter',
                    },
                ],
                'totalCount': 2,
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_all_item_promo_pairs_with_allowed_promos(self):
        """
        Checking that only allowed promos will be returned
        """
        response = self.mars.request_json(
            MarketCarterUrlParams(
                user_id=1,
                allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET],
                allowed_promos_carter=[EPromoMechanics.PM_BLUE_FLASH],
                allowed_promos_pers_basket=[EPromoMechanics.PM_BLUE_FLASH],
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'items': [
                    {
                        'offerInfo': self.get_expected_offer_item_id_20(),
                        'promo': self.get_expected_blue_flash(discount_absolute=200, discount_percent=20),
                        'sourceType': 'carter',
                    },
                ],
                'totalCount': 1,
            },
            allow_different_len=False,
        )

    def test_all_item_promo_pairs_with_counters(self):
        """
        Checking that all item-promo pairs are present in the response, except for three:
        1) id=10, items-discount
        2) id=20, blue flash
        3) id=40, promo code
        They've exceeded show limits.
        """
        response = self.mars.request_json(
            MarketCarterUrlParams(
                user_id=1,
                allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET],
                rearr_factors=CarterRearrFactors(use_promo_counter_limits=True),
            ).to_url(),
        )
        self.assertFragmentIn(
            response,
            {
                'items': [
                    {
                        'offerInfo': self.get_expected_offer_item_id_10(discount_absolute=91, discount_price=19),
                        'promo': self.get_expected_promo_code(
                            discount_absolute=81,
                            discount_percent=81,
                            promo_code='promo_code',
                            condition='test_conditions_pc',
                        ),
                        'sourceType': 'carter',
                    },
                ],
                'totalCount': 1,
            },
            allow_different_len=False,
        )

    def test_all_item_promo_pairs_with_counters_with_allowed_promos(self):
        """
        Checking that all item-promo pairs are present in the response, except for:
        1) id=10, items-discount
        2) id=20, blue flash
        3) id=40, promo code
        They've exceeded show limits.
        and
        4) id=10, promo-code
        It is not allowed
        """
        response = self.mars.request_json(
            MarketCarterUrlParams(
                user_id=1,
                allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET],
                rearr_factors=CarterRearrFactors(use_promo_counter_limits=True),
                allowed_promos_carter=[EPromoMechanics.PM_ITEMS_DISCOUNT],
            ).to_url(),
        )
        self.assertFragmentIn(
            response,
            {'items': EmptyList(), 'totalCount': 0},
            allow_different_len=False,
        )

    def test_best_item_promo_pair(self):
        """
        Checking that only one item-promo pair (with the best promo) is present in the response
        """
        response = self.mars.request_json(
            MarketCarterUrlParams(
                user_id=1,
                allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET],
                cart_type=EMarketCartType.MKT_BEST_ITEM_PROMO_PAIR,
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'items': [
                    {
                        'offerInfo': self.get_expected_offer_item_id_20(),
                        'promo': self.get_expected_blue_flash(discount_absolute=200, discount_percent=20),
                        'sourceType': 'carter',
                    },
                ],
                'totalCount': 1,
            },
            allow_different_len=False,
        )

    def test_best_item_promo_pair_with_allowed_promos(self):
        """
        Checking that only one item-promo pair (with the best allowed promo) is present in the response
        """
        response = self.mars.request_json(
            MarketCarterUrlParams(
                user_id=1,
                allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET],
                cart_type=EMarketCartType.MKT_BEST_ITEM_PROMO_PAIR,
                allowed_promos=[EPromoMechanics.PM_PROMO_CODE],
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'items': [
                    {
                        'offerInfo': self.get_expected_offer_item_id_10(discount_absolute=91, discount_price=19),
                        'promo': self.get_expected_promo_code(
                            discount_absolute=81,
                            discount_percent=81,
                            promo_code='promo_code',
                            condition='test_conditions_pc',
                        ),
                        'sourceType': 'carter',
                    },
                ],
                'totalCount': 1,
            },
            allow_different_len=False,
        )

    def test_best_item_promo_pair_with_counters(self):
        """
        Checking that only one item-promo pair (with the best promo) is present in the response.
        Since (id=20, blue flash) pair has exceeded its show limit, here we'll get (id=10, promo code) pair.
        """
        response = self.mars.request_json(
            MarketCarterUrlParams(
                user_id=1,
                allowed_source_types=[ESourceType.ST_CARTER, ESourceType.ST_PERS_BASKET],
                cart_type=EMarketCartType.MKT_BEST_ITEM_PROMO_PAIR,
                rearr_factors=CarterRearrFactors(use_promo_counter_limits=True),
            ).to_url()
        )
        self.assertFragmentIn(
            response,
            {
                'items': [
                    {
                        'offerInfo': self.get_expected_offer_item_id_10(discount_absolute=91, discount_price=19),
                        'promo': self.get_expected_promo_code(
                            discount_absolute=81,
                            discount_percent=81,
                            promo_code='promo_code',
                            condition='test_conditions_pc',
                        ),
                        'sourceType': 'carter',
                    },
                ],
                'totalCount': 1,
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    env.main()
