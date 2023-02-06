#!/usr/bin/env python
# encoding: utf-8

import urllib.parse

import travel.hotels.proto.promo_service.promo_service_pb2 as promo_service_pb2
import travel.hotels.proto2.label_pb2 as label_pb2

from tools import *


def check_general(oc_app, req, resp, dt, nights, ages, is_finished=True, progress=(1,1)):
    assert resp['Date'] == dt
    assert resp['Nights'] == nights
    assert resp['Ages'] == ages
    assert resp['IsFinished'] == is_finished
    assert resp['Progress']['OperatorsComplete'] == progress[0]
    assert resp['Progress']['OperatorsTotal'] == progress[1]
    show = all(req.get(f) is None for f in ['Date', 'CheckInDate', 'Nights', 'CheckOutDate', 'Ages'])
    assert resp['ShowEmptyManyOrgForm'] == show
    expected_op_ids = set()
    have_missing_opid = False
    for permalink, hotel in resp['Hotels'].items():
        for price in hotel.get('Prices', []):
            op_id = price.get('OperatorId')
            if op_id is None:
                have_missing_opid = True  # В случае brief цены
            else:
                expected_op_ids.add(op_id)
    for op_id, resp_op_info in resp.get('Operators', {}).items():
        op_id = int(op_id)
        assert op_id in expected_op_ids
        expected_op_ids.remove(op_id)
        op_info = oc_app.operators[op_id]
        print(type(resp_op_info['Name']))
        print(type(op_info['Name']))
        assert resp_op_info['Name'] == op_info['Name']
        assert resp_op_info['FaviconUrl'] == op_info['FaviconUrl']
        assert resp_op_info['GreenUrl'] == op_info['GreenUrl']
    assert not expected_op_ids or have_missing_opid


def check_hotels(oc_app, req, resp, expected_hotels, check_link_pct=100, check_price_order=True):
    total_was_found = False
    for permalink, hotel in resp['Hotels'].items():
        for s_id, expected_hotel_data in expected_hotels.items():
            if get_permalink(s_id) == permalink:
                break
        else:
            raise Exception("Unknown permalink in prices: %s" % permalink)
        expected_was_found = expected_hotel_data.get('WasFound', True)
        assert hotel["WasFound"] == expected_was_found
        expected_is_finished = expected_hotel_data.get('IsFinished', True)
        assert hotel["IsFinished"] == expected_is_finished
        prices = hotel.get('Prices', [])
        is_brief = expected_hotel_data.get('IsBrief', False)
        if expected_was_found:
            expected_price_fields = expected_hotel_data.get('PriceFields', {})
            total_was_found = True
            _check_prices(oc_app, req, resp, prices, s_id, expected_price_fields, is_brief,
                          check_link_pct=check_link_pct, check_price_order=check_price_order)
        else:
            assert len(prices) == 0
        other_fields = expected_hotel_data.get('OtherFields')
        if other_fields is not None:
            _check_values(other_fields, hotel, 'Permalink_%s' % permalink)
    assert resp['WasFound'] == total_was_found


def _check_values(expected_values, actual_values, path):
    if isinstance(expected_values, dict):
        assert isinstance(actual_values, dict), f'Expected {actual_values} to be dict to match {expected_values}'
        for key, expected_value in expected_values.items():
            subpath = path + '.' + key
            actual_value = actual_values.get(key)
            _check_values(expected_value, actual_value, subpath)
    elif isinstance(expected_values, list):
        assert isinstance(actual_values, list), f'Expected {actual_values} to be list to match {expected_values}'
        assert len(expected_values) == len(actual_values)
        for idx, value_pair in enumerate(zip(expected_values, actual_values)):
            _check_values(value_pair[0], value_pair[1], path + '[%s]' % idx)
    elif isinstance(expected_values, float):
        if abs(actual_values - expected_values) > 0.001:
            fmt = 'wrong "{}" field value: expected "{}" but got "{}"'
            raise Exception(fmt.format(path, expected_values, actual_values))
    else:
        if actual_values != expected_values:
            fmt = 'wrong "{}" field value: expected "{}" but got "{}"'
            raise Exception(fmt.format(path, expected_values, actual_values))


def _check_prices(oc_app, req, resp, prices, s_id, prices_fields_to_check, is_brief, check_link_pct, check_price_order):
    actual_prices_count = len(prices)
    for field, values in prices_fields_to_check.items():
        if len(values) > actual_prices_count:
            raise Exception('expected more values for {} {}'.format(s_id, field))
        if len(values) < actual_prices_count:
            raise Exception('expected less values for {} {}'.format(s_id, field))
    prev = None
    check_link_budget = 0
    bumped_op_ids = list(oc_app.bumped_op_ids)  # Copy
    bump_allowed = True
    for idx, cur in enumerate(prices):
        try:
            for field_name, values in prices_fields_to_check.items():
                actual_val = cur
                parts = field_name.split('.')
                for part_idx, part in enumerate(parts):
                    actual_val = actual_val.get(part)
                    if actual_val is None and part_idx < len(parts) - 1:  # Last one may be None
                        raise Exception(f'Cannot find "{field_name}" field value, got None at part {part}')
                if actual_val != values[idx]:
                    raise Exception(f'wrong "{field_name}" field value at index {idx}, expected "{values[idx]}" but got "{actual_val}"')
            assert cur['Price'] is not None
            assert cur['OfferId'] is not None
            assert cur['Pansion'] != 'UNKNOWN'  # Просто есть
            assert cur['OperatorId'] is not None  # Просто есть
            # 'FreeCancellation' может отсутствовать, если мы про него ничего не знаем
            if not is_brief:
                assert cur['PartnerId'] is not None  # Просто есть
                assert cur['OperatorName'] == oc_app.operators[cur['OperatorId']]['Name']
                assert cur['RoomType'] is not None  # Просто есть
                assert cur['OperatorOfferCount'] is not None  # Просто есть
                check_link_budget += check_link_pct
                if check_link_budget >= 100:
                    check_link_budget -= 100
                    _check_link(oc_app, req, resp, cur, s_id)
                if check_price_order:
                    if prev is not None:
                        if bump_allowed and prev['OperatorId'] in bumped_op_ids:
                            # Разрешить однократное нарушение порядка
                            pos = bumped_op_ids.index(prev['OperatorId'])
                            bumped_op_ids = bumped_op_ids[pos + 1:]
                        else:
                            bump_allowed = False
                            if is_price_less(oc_app, cur, prev):   # должно быть всегда, что prev <= cur
                                raise Exception("Price order violation: ! (%r) <= (%r)" % (prev, cur))
                prev = cur
        except Exception as e:
            raise Exception("Exc while checking hotel %s price #%s: %s" % (s_id, idx, str(e)))


def is_price_less(oc_app, p1, p2):
    if p1['Price'] != p2['Price']:
        return p1['Price'] < p2['Price']
    order1 = oc_app.operators[p1['OperatorId']]['Order']
    order2 = oc_app.operators[p2['OperatorId']]['Order']
    return order1 < order2


def _check_link(oc_app, req, resp, price, s_id):
    permalink = get_permalink(s_id)
    orig_hotel_ids = get_partner_hotel_ids(oc_app, s_id)
    parts = urllib.parse.urlsplit(price['PartnerLink'])
    assert parts.scheme == 'https'
    assert parts.netloc == 'redir-host'
    assert parts.path == '/redir'
    params = urllib.parse.parse_qs(parts.query)
    assert params['OfferId'][0] == price['OfferId']
    assert params['LabelHash'][0] == req['LabelHash']
    proto_str = oc_app.lc.decode_label(params['ProtoLabel'][0])
    label = label_pb2.TLabel()
    label.ParseFromString(proto_str)
    assert label.Source == req['utm_source']
    assert label.Medium == req['utm_medium']
    assert label.Campaign == req['utm_campaign']
    assert label.Content == req['utm_content']
    assert label.Term == req['utm_term']
    assert label.YandexUid == req['YandexUid']
    assert label.OperatorId == price['OperatorId']
    assert label.Query == req['SearchQuery']
    assert label.SerpReqId == req['SerpReqId']
    assert label.Price != 0
    assert label.IntTestIds == parse_testids(req['TestIds'])
    assert label.IntTestBuckets == parse_testids(req['TestBuckets'])
    assert label.CacheTimestamp in (oc_app.now // 1000, FAKE_NOW)  # CacheTimestamp - или настоящий now (для обычных записей) или FAKE_NOW для ответов Searcher-а
    assert label.Permalink == int(permalink)
    assert label.OfferId == price['OfferId']
    assert label.SearcherReqId.startswith('Req')
    assert label.OriginalHotelId == orig_hotel_ids[price['PartnerId']]
    assert label.PassportUid == req['PassportUid']
    assert label.RequestRegion == req['RequestRegion']
    assert label.UserRegion == req['UserRegion']
    assert label.Uuid == req['Uuid']
    assert label.PartnerId == price['PartnerId']
    assert label.CheckInDate == resp['Date']
    assert label.Nights == resp['Nights']
    assert label.ICookie == req['ICookie']
    assert label.GeoClientId == req['GeoClientId']
    assert label.GeoOrigin == req['GeoOrigin']
    assert label.UserDevice == req['UserDevice']
    assert label.Gclid == req['gclid']
    assert label.YaTravelReqId == req['YaTravelReqId']
    assert occupancy_to_ages(label.Occupancy) == resp['Ages']


def assert_equals(a, b):
    if a != b:
        raise Exception(f'Expected {b} but got {a}')


def check_plus_promo(
    rsp,
    expected_points,
    eligibility=promo_service_pb2.YPE_ELIGIBLE,
    discount_percent=10,
    event_id=None,
    special_offer_end_utc=None,
    additional_fee_percent=None,
    additional_fee_value=None,
    max_pointsback=None,
    event_type=promo_service_pb2.YPET_COMMON,
):
    if event_id is not None:
        assert_equals(rsp.Plus.PromoInfo.EventId, event_id)
    assert_equals(rsp.Plus.Eligibility, eligibility)
    assert_equals(rsp.Plus.Points.value, expected_points)
    assert_equals(rsp.Plus.PromoInfo.MaxPointsback, max_pointsback if max_pointsback is not None else 3000)
    assert_equals(rsp.Plus.PromoInfo.DiscountPercent, discount_percent)
    if special_offer_end_utc is not None:
        assert_equals(rsp.Plus.PromoInfo.SpecialOfferEndUtc.seconds, special_offer_end_utc)
    if additional_fee_percent is not None:
        assert_equals(rsp.Plus.AdditionalFeeInfo.FeePercent, additional_fee_percent)
    if additional_fee_value is not None:
        assert_equals(rsp.Plus.AdditionalFeeInfo.FeeValue, additional_fee_value)
    if event_type is not None:
        assert_equals(rsp.Plus.PromoInfo.EventType, event_type)


def check_yandex_eda_promo(rsp, eligibility=promo_service_pb2.YEE_ELIGIBLE, promo_code_count=0, first_date='', last_date='', show_badge=True):
    assert_equals(rsp.YandexEda2022Status.Eligibility, eligibility)
    if rsp.YandexEda2022Status.Eligibility != promo_service_pb2.YEE_ELIGIBLE:
        return
    assert_equals(rsp.YandexEda2022Status.PromoInfo.PromoCodeNominal, 100)
    assert_equals(rsp.YandexEda2022Status.PromoInfo.PromoCodeCount, promo_code_count)
    assert_equals(rsp.YandexEda2022Status.PromoInfo.FirstDate, first_date)
    assert_equals(rsp.YandexEda2022Status.PromoInfo.LastDate, last_date)
    assert_equals(rsp.YandexEda2022Status.ShowBadge, show_badge)


def check_white_label_promo(
    rsp,
    eligibility=promo_service_pb2.EWhiteLabelEligibility.WLE_ELIGIBLE,
    partner_id=None,
    amount=None,
    points_type=None,
    linguistic=None,
    event_id=None
):
    assert rsp.WhiteLabelStatus.Eligibility == eligibility
    if partner_id is not None:
        assert rsp.WhiteLabelStatus.PartnerId == partner_id
    if amount is not None:
        assert rsp.WhiteLabelStatus.Points.Amount == amount
    if points_type is not None:
        assert rsp.WhiteLabelStatus.Points.PointsType == points_type
    if linguistic is not None:
        assert rsp.WhiteLabelStatus.PointsLinguistics.NameForNumeralNominative == linguistic
    if event_id is not None:
        assert rsp.WhiteLabelStatus.PromoInfo.EventId == event_id

