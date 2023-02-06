#!/usr/bin/env python
# encoding: utf-8

from tools import FAKE_NOW, build_price

from checkers import check_plus_promo, check_yandex_eda_promo

from google.protobuf.timestamp_pb2 import Timestamp
import travel.proto.order_type.order_type_pb2 as order_type_pb2
import travel.hotels.proto2.hotels_pb2 as hotels_pb2
import travel.hotels.proto.promo_service.promo_service_pb2 as promo_service_pb2


def test_promo_service_ping(oc_app):
    req = hotels_pb2.TPingRpcReq()
    resp = oc_app.promo_service_ping(req)
    assert resp.IsReady is True


def prepare_determine_promos_for_offer_req(
        now_ts, partner, checkin, checkout, price_amount, is_plus, is_logged_in, passport_id=None,
        price_amount_before_promocodes=None, price_amount_after_promocodes=None, original_id='4',
        existing_order_types=None, use_existing_order_types=False, kv_experiments=None
):
    req = promo_service_pb2.TDeterminePromosForOfferReq(
        Now=Timestamp(seconds=now_ts, nanos=0),
        OfferInfo=promo_service_pb2.TOfferInfo(
            HotelId=hotels_pb2.THotelId(PartnerId=partner, OriginalId=original_id),
            CheckInDate=checkin,
            CheckOutDate=checkout,
            PriceFromPartnerOffer=build_price(price_amount),
            PriceBeforePromocodes=build_price(price_amount_before_promocodes),
            PriceAfterPromocodes=build_price(price_amount_after_promocodes),
        ),
        UserInfo=promo_service_pb2.TUserInfo(
            IsPlus=is_plus,
            IsLoggedIn=is_logged_in,
        ),
        ExperimentInfo=promo_service_pb2.TExperimentInfo(
            StrikeThroughPrices=False,
        )
    )
    if kv_experiments is None:
        kv_experiments = list()
    for kv_exp in kv_experiments:
        exp = req.ExperimentInfo.KVExperiments.add()
        exp.Key = kv_exp['key']
        exp.Value = kv_exp['value']
    if passport_id is not None:
        req.UserInfo.PassportId = passport_id
    req.UserInfo.UseExistingOrderTypes = use_existing_order_types
    if existing_order_types is not None:
        for existing_order_type in map(order_type_pb2.EOrderType.Value, existing_order_types):
            req.UserInfo.ExistingOrderTypes.append(existing_order_type)
    return req


def test_promo_service_determine_promos_for_offer_plus_wrong_partner(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_OSTROVOK, "2021-11-11", "2021-11-12", 100,
                                                 is_plus=False, is_logged_in=False)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    assert resp.Plus.Eligibility == promo_service_pb2.YPE_WRONG_PARTNER


def test_promo_service_determine_promos_for_offer_plus_boy_partner_not_logged_in(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2021-11-11", "2021-11-12", 150,
                                                 is_plus=False, is_logged_in=False)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 15, promo_service_pb2.YPE_USER_NOT_LOGGED_IN)


def test_promo_service_determine_promos_for_offer_plus_boy_partner_not_plus(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2021-11-11", "2021-11-12", 160,
                                                 is_plus=False, is_logged_in=True)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 16, promo_service_pb2.YPE_USER_NOT_PLUS)


def test_promo_service_determine_promos_for_offer_plus_boy_partner_plus(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2021-11-11", "2021-11-12", 170,
                                                 is_plus=True, is_logged_in=True)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 17)


def test_promo_service_determine_promos_for_offer_plus_boy_partner_plus_price_before_promocodes(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2021-11-11", "2021-11-12", 170,
                                                 is_plus=True, is_logged_in=True, price_amount_before_promocodes=140)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 14)


def test_promo_service_determine_promos_for_offer_plus_boy_partner_plus_price_after_promocodes(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2021-11-11", "2021-11-12", 170,
                                                 is_plus=True, is_logged_in=True, price_amount_before_promocodes=140,
                                                 price_amount_after_promocodes=130)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 13)


def test_promo_service_determine_promos_for_offer_plus_bl_15_ok(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-03-01", "2022-03-02", 200,
                                                 is_plus=True, is_logged_in=True, passport_id=oc_app.__user_4__)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 30, discount_percent=15)


def test_promo_service_determine_promos_for_offer_plus_bl_15_not_ok(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-03-01", "2022-03-02", 200,
                                                 is_plus=True, is_logged_in=True, passport_id=oc_app.__user_2__)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 20, discount_percent=10)


def test_promo_service_determine_promos_for_offer_plus_wl_20(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-03-01", "2022-03-02", 300,
                                                 is_plus=True, is_logged_in=True, passport_id=oc_app.__user_3__)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 60, discount_percent=20)


def test_promo_service_determine_promos_for_offer_plus_wbl_25(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-03-01", "2022-03-02", 400,
                                                 is_plus=True, is_logged_in=True, passport_id=oc_app.__user_1__)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 100, discount_percent=25)


def test_promo_service_determine_promos_for_offer_plus_first_order_ok(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-04-16", "2022-04-17", 400,
                                                 is_plus=True, is_logged_in=True, passport_id=oc_app.__user_1__)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 120, discount_percent=30, event_id="test-first-order-30")


def test_promo_service_determine_promos_for_offer_plus_first_order_with_hotel_order(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-04-16", "2022-04-17", 400,
                                                 is_plus=True, is_logged_in=True, passport_id=oc_app.__user_1__,
                                                 existing_order_types=["OT_HOTEL"], use_existing_order_types=True)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    assert resp.Plus.PromoInfo.EventId != "test-first-order-30"


def test_promo_service_determine_promos_for_offer_plus_first_order_with_bus_order(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-04-16", "2022-04-17", 400,
                                                 is_plus=True, is_logged_in=True, passport_id=oc_app.__user_1__,
                                                 existing_order_types=["OT_BUS"], use_existing_order_types=True)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 120, discount_percent=30, event_id="test-first-order-30")


def test_promo_service_determine_promos_for_offer_plus_first_order_user_in_config(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-04-16", "2022-04-17", 400,
                                                 is_plus=True, is_logged_in=True, passport_id=oc_app.__user_4__)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    assert resp.Plus.PromoInfo.EventId != "test-first-order-30"


def test_promo_service_determine_promos_for_offer_plus_first_order_user_in_config_with_old_order(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-04-16", "2022-04-17", 400,
                                                 is_plus=True, is_logged_in=True, passport_id=oc_app.__user_4__,
                                                 existing_order_types=[], use_existing_order_types=True)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 120, discount_percent=30, event_id="test-first-order-30")


def test_promo_service_determine_promos_for_offer_plus_no_first_order_for_blacklisted_user(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-04-16", "2022-04-17", 400,
                                                 is_plus=True, is_logged_in=True, passport_id=oc_app.__user_2__)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    assert resp.Plus.PromoInfo.EventId != "test-first-order-30"


def test_promo_service_determine_promos_for_offer_eda_wl_miss_match(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2018-01-01", "2018-01-02", 100,
                                                 is_plus=True, is_logged_in=True)
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_yandex_eda_promo(resp, eligibility=promo_service_pb2.YEE_WL_MISS_MATCH)


def test_promo_service_determine_promos_for_offer_eda_blacklisted(oc_app):
    req = prepare_determine_promos_for_offer_req(
        FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2018-01-01", "2018-01-02", 100, is_plus=True, is_logged_in=True,
        original_id=oc_app.__hotel_2__['OriginalId']
    )
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_yandex_eda_promo(resp, eligibility=promo_service_pb2.YEE_BLACKLISTED)


def test_promo_service_determine_promos_for_offer_eda_ok(oc_app):
    req = prepare_determine_promos_for_offer_req(
        FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2018-01-01", "2018-01-02", 100, is_plus=True, is_logged_in=True,
        original_id=oc_app.__hotel_1__['OriginalId']
    )
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_yandex_eda_promo(resp, promo_code_count=1, first_date="2018-01-02", last_date="2018-01-02", show_badge=True )


def test_promo_service_determine_promos_for_offer_eda_last_date(oc_app):
    req = prepare_determine_promos_for_offer_req(
        FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2018-01-04", "2018-01-07", 100, is_plus=True, is_logged_in=True,
        original_id=oc_app.__hotel_1__['OriginalId']
    )
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_yandex_eda_promo(resp, promo_code_count=2, first_date="2018-01-05", last_date="2018-01-06", show_badge=True )


def test_promo_service_kv_exp_ok(oc_app):
    req = prepare_determine_promos_for_offer_req(
        FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-05-02", "2022-05-05", 100, is_plus=True, is_logged_in=True,
        original_id=oc_app.__hotel_1__['OriginalId'], kv_experiments=[{"key": "MARKETING_elasticity", "value": "13"}]
    )
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 13, discount_percent=13, event_id="test-kv-exp", event_type=promo_service_pb2.YPET_SPECIAL, special_offer_end_utc=2524694400)


def test_promo_service_kv_exp_not_ok(oc_app):
    req = prepare_determine_promos_for_offer_req(
        FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-05-02", "2022-05-05", 100, is_plus=True, is_logged_in=True,
        original_id=oc_app.__hotel_1__['OriginalId'], kv_experiments=[{"key": "MARKETING_elasticity", "value": "14"}]
    )
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 10, discount_percent=10, event_id="", event_type=promo_service_pb2.YPET_COMMON)


def test_promo_service_plus_additional_fee_ok(oc_app):
    req = prepare_determine_promos_for_offer_req(
        FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-05-02", "2022-05-05", 100, is_plus=True, is_logged_in=True,
        original_id=oc_app.__hotel_3__['OriginalId']
    )
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 12, discount_percent=11, event_id="", additional_fee_percent=1.5, additional_fee_value=2)


def test_promo_service_hotels_list(oc_app):
    req = prepare_determine_promos_for_offer_req(
        FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2022-05-02", "2022-05-05", 100, is_plus=True, is_logged_in=True,
        original_id="hotels_list_test"
    )
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 20, discount_percent=20, event_id="test-hotels-list-20", max_pointsback=20000)


def test_promo_service_hotel_whitelist(oc_app):
    req = prepare_determine_promos_for_offer_req(
        FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2018-01-04", "2018-01-07", 100, is_plus=True, is_logged_in=True,
        original_id=oc_app.__hotel_5__['OriginalId']
    )
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 30, discount_percent=30, event_id="cultural-dreams-30", event_type=promo_service_pb2.YPET_CULTURAL_DREAMS)


def test_promo_service_event_priority(oc_app):
    req = prepare_determine_promos_for_offer_req(
        FAKE_NOW, hotels_pb2.PI_TRAVELLINE, "2018-01-04", "2018-01-07", 40000, is_plus=True, is_logged_in=True,
        original_id=oc_app.__hotel_5__['OriginalId']
    )
    resp = oc_app.promo_service_determine_promos_for_offer(req)
    check_plus_promo(resp, 3000, discount_percent=30, event_id="cultural-dreams-30", event_type=promo_service_pb2.YPET_CULTURAL_DREAMS)
