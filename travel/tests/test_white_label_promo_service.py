from tools import FAKE_NOW, build_price
from checkers import check_white_label_promo

from google.protobuf.timestamp_pb2 import Timestamp
import travel.proto.white_label.white_label_pb2 as white_label_pb2
import travel.hotels.proto2.hotels_pb2 as hotels_pb2
import travel.hotels.proto.promo_service.promo_service_pb2 as promo_service_pb2


def prepare_determine_promos_for_offer_req(
    now_ts, partner, checkin, checkout, price_amount, original_id='4', partner_id=None
):
    return promo_service_pb2.TDeterminePromosForOfferReq(
        Now=Timestamp(seconds=now_ts, nanos=0),
        OfferInfo=promo_service_pb2.TOfferInfo(
            HotelId=hotels_pb2.THotelId(PartnerId=partner, OriginalId=original_id),
            CheckInDate=checkin,
            CheckOutDate=checkout,
            PriceFromPartnerOffer=build_price(price_amount)
        ),
        WhiteLabelInfo=promo_service_pb2.TWhiteLabelInfo(
            PartnerId=partner_id
        )
    )


def prepare_white_label_points_props_req(points_type, amount):
    return promo_service_pb2.TGetWhiteLabelPointsPropsReq(
        PointsType=points_type,
        Amount=amount
    )


def check_white_label_points_props(rsp, name_for_numeral_nominative):
    assert rsp.PointsLinguistics.NameForNumeralNominative == name_for_numeral_nominative


def test_s7_promo_action(oc_app):
    req = prepare_determine_promos_for_offer_req(FAKE_NOW, hotels_pb2.PI_OSTROVOK, "2021-11-11", "2021-11-12", 100,
                                                 partner_id=white_label_pb2.EWhiteLabelPartnerId.WL_S7)
    rsp = oc_app.promo_service_determine_promos_for_offer(req)
    check_white_label_promo(rsp, partner_id=white_label_pb2.EWhiteLabelPartnerId.WL_S7, amount=10,
                            points_type=white_label_pb2.EWhiteLabelPointsType.WLP_S7, linguistic="миль",
                            event_id="default")


def test_s7_promo_action_better_event(oc_app):
    ts = 1641081600 # 2022-01-02T00:00:00
    req = prepare_determine_promos_for_offer_req(ts, hotels_pb2.PI_OSTROVOK, "2022-02-01", "2022-02-03", 100,
                                                 partner_id=white_label_pb2.EWhiteLabelPartnerId.WL_S7)
    rsp = oc_app.promo_service_determine_promos_for_offer(req)
    check_white_label_promo(rsp, partner_id=white_label_pb2.EWhiteLabelPartnerId.WL_S7, amount=15,
                            points_type=white_label_pb2.EWhiteLabelPointsType.WLP_S7, linguistic="миль",
                            event_id="3-per-20")


def test_s7_white_label_points_props(oc_app):
    req = prepare_white_label_points_props_req(white_label_pb2.EWhiteLabelPointsType.WLP_S7, 321)
    rsp = oc_app.promo_service_get_white_label_points_props(req)
    check_white_label_points_props(rsp, "миля")

    req = prepare_white_label_points_props_req(white_label_pb2.EWhiteLabelPointsType.WLP_S7, 322)
    rsp = oc_app.promo_service_get_white_label_points_props(req)
    check_white_label_points_props(rsp, "мили")
