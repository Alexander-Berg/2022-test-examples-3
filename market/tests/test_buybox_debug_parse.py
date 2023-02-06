import pytest

from market.dynamic_pricing.buybox.buybox_report_parser.lib.buybox_debug_parse import (
    parse_buybox_debug,
    parse_buybox_debug_new,
    OldFormatBuyboxFeatures,
    SKU_OFFERS_URL_TEMPLATE,
    SKU_OFFERS_URL_TEMPLATE_WITH_CART,
    make_sku_offers_url,
    DEFAULT_REARR_FLAGS
)


def test_buybox_debug():
    input = "TOO_HIGH_PRICE for offer d9566586s472311w172: price2880 > 1.1 * minPrice2490; " \
            "TOO_HIGH_PRICE for offer d9566597s472311w145: price2880 > 1.1 * minPrice2490;  " \
            "GetWonOffer_BuyboxByGmvUe_trace " \
            "d9566573s600514w145p2490u0:400e4.51636f0.08lts0.01can0.01ret0.02pp0rr0.02PredictedElasticity:5.57568;" \
            "ConversionByDeliveryDayCoef:1.18;ShopRatingCoef:0.968478;IsWarehouseInUserCartCoef:0.95;" \
            "Conversion:6.05332;PromoBoostCoefficient:1;" \
            "GMV:15072.76182;CourierShopPrice:134;UE:-0.0357108; " \
            "dcp:249;dcd:1-1;dpp:0;dpd:0-0;dwe:0.6;dl:22;dwi:33;dh:7; " \
            "d9566581s580133w47866p2490u0:200e4.58952f0.08lts0can0.01ret0pp0PredictedElasticity:5.57568;" \
            "ConversionByDeliveryDayCoef:1.18;ShopRatingCoef:0.98972;IsWarehouseInUserCartCoef:0.95;Conversion:6.18608;" \
            "PromoBoostCoefficient:1;GMV:15403.34617;CourierShopPrice:150;UE:-0.068241; " \
            "dcp:249;dcd:1-1;dpp:0;dpd:0-0;dwe:0.3;dl:31;dwi:6;dh:22; " \
            "o:9566573 deltaUe:0.0325301 deltaGmv:0.0214618 exchange:0.659753 o:9566581 deltaUe:0 o:9566600 " \
            "deltaUe:0.0325301 deltaGmv:0.00503719 exchange:0.154847 o:9566602 deltaUe:0.0325301 deltaGmv:0.0139251 " \
            "exchange:0.428067 o:9566603 deltaUe:0.00523762 deltaGmv:0.0884879 " \
            "exchange:16.8947 WonByGMV:9566581"

    first = OldFormatBuyboxFeatures(doc_id=9566573,
                                    supplier_id=600514,
                                    warehouse_id=145,
                                    price=2490,
                                    is_user_cart_warehouse=0,
                                    elasticity=4.51636,
                                    fee=0.08,
                                    return_rate=0.02,
                                    late_shipment_rate=0.01,
                                    cancellation_rate=0.01,
                                    purchase_price=0.,
                                    is_kgt=False,
                                    predicted_elasticity=5.57568,
                                    conversion_coef=1.18,
                                    shop_rating=0.968478,
                                    is_warehouse_in_user_cart_coef=0.95,
                                    conversion=6.05332,
                                    promo=1,
                                    gmv=15072.76182,
                                    ue=-0.0357108,
                                    courier_shop_price=134.,
                                    courier_price=249,
                                    courier_days_min=1,
                                    courier_days_max=1,
                                    is_won=False)

    result = parse_buybox_debug_new(input)
    assert len(result) == 2
    result = list(map(lambda x: x.__dict__, result))
    assert first.__dict__ == result[0]


def test_old_format_buybox_case():
    input = "TOO_HIGH_PRICE for offer d4845s10696733w48373: price4666 > 1.1 * minPrice111; " \
            "TOO_HIGH_PRICE for offer d4848s10457276w48255: price4666 > 1.1 * minPrice111;" + \
            "d4843s10696093w48372p112u0:22e0f0.07lts0.22can0.07ret0.01pp0 " \
            "dcp:249;dcd:1-1;dpp:0;dpd:0-0;dwe:1;dl:14;dwi:15;dh:16;" + \
            "d4843s10696093w48372p112u0:22e0f0.07lts0.22can0.07ret0.01pp0" + \
            "geo213:213;1;3;225;won:4847"

    first = OldFormatBuyboxFeatures(doc_id=4843,
                                    supplier_id=10696093,
                                    warehouse_id=48372,
                                    price=112,
                                    is_user_cart_warehouse=0,
                                    elasticity=0.,
                                    fee=0.07,
                                    return_rate=0.01,
                                    late_shipment_rate=0.22,
                                    cancellation_rate=0.07,
                                    purchase_price=0.,
                                    is_kgt=False,
                                    gmv=-1,
                                    courier_price=249,
                                    courier_days_min=1,
                                    courier_days_max=1,
                                    is_won=False)

    second = OldFormatBuyboxFeatures(doc_id=4843,
                                     supplier_id=10696093,
                                     warehouse_id=48372,
                                     price=112,
                                     is_user_cart_warehouse=0,
                                     elasticity=0.,
                                     fee=0.07,
                                     return_rate=0.01,
                                     late_shipment_rate=0.22,
                                     cancellation_rate=0.07,
                                     purchase_price=0.,
                                     is_kgt=False,
                                     gmv=-1,
                                     is_won=False)

    result = parse_buybox_debug(input)
    assert len(result) == 2
    result = list(map(lambda x: x.__dict__, result))
    assert first.__dict__ == result[0]
    assert second.__dict__ == result[1]

def test_old_format_buybox_case_line_contains_rr():
    input = "d7145421s465852w145p101589u0:12800e0f0.04lts0.01can0.01ret0.1pp97688rr0.1KGT dcp:249;dcd:1-1;dpp:0;dpd:0-0;dwe:0.5;dl:6;dwi:20;dh:12;" \
            "d9323740s465852w145p101990u0:31e0f0.04lts0.01can0.01ret0pp97678rr0 dcp:249;dcd:1-1;dpp:0;dpd:0-0;dwe:0.5;dl:11;dwi:18;dh:8; geo2:2;10174;17;225;won:7145421"

    # first = BuyboxFeatures(doc_id=4843,
    #                        supplier_id=10696093,
    #                        warehouse_id=48372,
    #                        price=112,
    #                        is_user_cart_warehouse=0,
    #                        elasticity=0.,
    #                        fee=0.07,
    #                        return_rate=0.01,
    #                        late_shipment_rate=0.22,
    #                        cancellation_rate=0.07,
    #                        purchase_price=0.,
    #                        is_kgt=False,
    #                        gmv=-1,
    #                        courier_price=249,
    #                        courier_days_min=1,
    #                        courier_days_max=1,
    #                        is_won=False)
    #
    # second = BuyboxFeatures(doc_id=4843,
    #                         supplier_id=10696093,
    #                         warehouse_id=48372,
    #                         price=112,
    #                         is_user_cart_warehouse=0,
    #                         elasticity=0.,
    #                         fee=0.07,
    #                         return_rate=0.01,
    #                         late_shipment_rate=0.22,
    #                         cancellation_rate=0.07,
    #                         purchase_price=0.,
    #                         is_kgt=False,
    #                         gmv=-1,
    #                         is_won=False)

    result = parse_buybox_debug(input)
    assert len(result) == 2
    # result = list(map(lambda x: x.__dict__, result))
    # assert first.__dict__ == result[0]
    # assert second.__dict__ == result[1]


def test_make_url_batch():
    report_host = "warehouse-report.blue.vs.market.yandex.net"
    msku_str = "100547293866,84440401"
    user_region_id = 213
    assert SKU_OFFERS_URL_TEMPLATE % (report_host, user_region_id, msku_str, DEFAULT_REARR_FLAGS) == \
           make_sku_offers_url(host=report_host, msku_ids=[100547293866, 84440401], cart_str="",
                               rids=user_region_id, rearr_flags=DEFAULT_REARR_FLAGS)

    cart_str = "KYW9SFj-PpIs-4mAtY_uKw,KYW9SFj-PpIs-4mAtY_uKw"
    assert SKU_OFFERS_URL_TEMPLATE_WITH_CART % (report_host, user_region_id, msku_str, cart_str, DEFAULT_REARR_FLAGS) \
           == make_sku_offers_url(host=report_host, msku_ids=[100547293866, 84440401], cart_str=cart_str,
                                  rids=user_region_id, rearr_flags=DEFAULT_REARR_FLAGS)
