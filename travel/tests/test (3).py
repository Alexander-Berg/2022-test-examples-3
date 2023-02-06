#!/usr/bin/env python
# encoding: utf-8

from travel.hotels.proto2 import hotels_pb2

import checkers
import time


def test_by_url(redir_app, label_codec):
    label, to_check = checkers.generate_proto_label(PartnerId=hotels_pb2.PI_BOOKING, RedirDestination=hotels_pb2.RD_Partner, CacheTimestamp=int(time.time()))
    resp = redir_app.redir_by_url(label_codec.encode_url('http://ya.ru'), label_codec.encode_label(label.SerializeToString()))
    label_hash = checkers.check_location(resp, 'ya.ru', https=False)
    checkers.check_reqans_record(redir_app, 'http://ya.ru', label_hash, to_check)


def test_by_url_ostrovok(redir_app, label_codec):
    label, to_check = checkers.generate_proto_label(PartnerId=hotels_pb2.PI_OSTROVOK, RedirDestination=hotels_pb2.RD_Partner, CacheTimestamp=int(time.time()))
    resp = redir_app.redir_by_url(label_codec.encode_url('http://ostrovok.ru'), label_codec.encode_label(label.SerializeToString()))
    label_hash = checkers.check_location(resp, 'ostrovok.ru', label_param='utm_term', https=False)
    checkers.check_reqans_record(redir_app, 'http://ostrovok.ru', label_hash, to_check)


def test_by_url_with_utm_override(redir_app, label_codec):
    label, to_check = checkers.generate_proto_label(PartnerId=hotels_pb2.PI_BOOKING, RedirDestination=hotels_pb2.RD_Partner, CacheTimestamp=int(time.time()))
    to_check['Source'] = 'utm_source_override_by_url'
    to_check['Medium'] = 'utm_medium_override_by_url'
    to_check['Campaign'] = 'utm_campaign_override_by_url'
    to_check['Content'] = 'utm_content_override_by_url'
    to_check['Term'] = 'utm_term_override_by_url'
    resp = redir_app.redir_by_url(label_codec.encode_url('http://ya.ru'),
                                  label_codec.encode_label(label.SerializeToString()),
                                  other_params={
                                      'utm_source': to_check['Source'],
                                      'utm_medium': to_check['Medium'],
                                      'utm_campaign': to_check['Campaign'],
                                      'utm_content': to_check['Content'],
                                      'utm_term': to_check['Term']
    })
    label_hash = checkers.check_location(resp, 'ya.ru', https=False)
    checkers.check_reqans_record(redir_app, 'http://ya.ru', label_hash, to_check)


def test_no_url(redir_app):
    resp = redir_app.get('redir?ProtoLabel=test')
    assert resp.status_code == 400
    checkers.check_no_reqans_records(redir_app)


def test_wrong_url(redir_app, label_codec):
    label, to_check = checkers.generate_proto_label()
    resp = redir_app.redir_by_url("hello", label_codec.encode_label(label.SerializeToString()))
    assert resp.status_code == 400
    checkers.check_no_reqans_records(redir_app)


def test_wrong_label(redir_app, label_codec):
    resp = redir_app.redir_by_url(label_codec.encode_url('http://ya.ru'), label_codec.encode_label("aaaa"))
    assert resp.status_code == 400
    checkers.check_no_reqans_records(redir_app)


def test_offer_id_is_written_to_bus_proto(redir_app, label_codec):
    redir_app.read_price_check_requests()
    label, to_check = checkers.generate_proto_label(OfferId='OfferIdValue2', PartnerId=hotels_pb2.PI_BOOKING, RedirDestination=hotels_pb2.RD_Partner, CacheTimestamp=int(time.time()))
    resp = redir_app.redir_by_url(label_codec.encode_url('http://ya.ru'), label_codec.encode_label(label.SerializeToString()))
    label_hash = checkers.check_location(resp, 'ya.ru', https=False)
    checkers.check_reqans_record(redir_app, 'http://ya.ru', label_hash, to_check)
    checkers.check_pricecheck_reqs(redir_app.read_price_check_requests(), ['OfferIdValue2'])


def test_empty_offer_id_is_not_written_to_bus(redir_app, label_codec):
    redir_app.read_price_check_requests()
    label, to_check = checkers.generate_proto_label(OfferId='', PartnerId=hotels_pb2.PI_BOOKING, RedirDestination=hotels_pb2.RD_Partner, CacheTimestamp=int(time.time()))
    resp = redir_app.redir_by_url(label_codec.encode_url('http://ya.ru'), label_codec.encode_label(label.SerializeToString()))
    label_hash = checkers.check_location(resp, 'ya.ru', https=False)
    checkers.check_reqans_record(redir_app, 'http://ya.ru', label_hash, to_check)
    checkers.check_pricecheck_reqs(redir_app.read_price_check_requests(), [])


def test_token_redir_to_book_page_with_label_hash(redir_app, label_codec):
    token = hotels_pb2.TTravelToken(
        OfferId='test',
        GeneratedAtSecondsSinceEpoch=123,
        CheckInDateDaysSinceEpoch=123,
        CheckOutDateDaysSinceEpoch=125,
        Occupancy='2',
        Permalink=1234123,
        Partner=hotels_pb2.PI_BOOKING,
        StringId="originalId"
    )
    label, to_check = checkers.generate_proto_label(PartnerId=hotels_pb2.PI_BOOKING,
                                                    Surface=hotels_pb2.S_MAPS,
                                                    RedirDestination=hotels_pb2.RD_UNUSED,
                                                    CacheTimestamp=int(time.time()))
    token_bytes = label_codec.encode_token(token.SerializeToString())
    resp = redir_app.redir_by_token(token_bytes, label_codec.encode_label(label.SerializeToString()), label_hash='TestLabelHash')
    label_hash = checkers.check_location(resp, 'travel-test.yandex.ru', path='/hotels/book/', token=token_bytes)
    assert label_hash == 'TestLabelHash'
    checkers.check_no_reqans_records(redir_app)


def test_token_redir_to_book_page_without_hash(redir_app, label_codec):
    token = hotels_pb2.TTravelToken(
        OfferId='test',
        GeneratedAtSecondsSinceEpoch=123,
        CheckInDateDaysSinceEpoch=123,
        CheckOutDateDaysSinceEpoch=125,
        Occupancy='2',
        Permalink=1234123,
        Partner=hotels_pb2.PI_BOOKING,
        StringId="originalId"
    )
    label, to_check = checkers.generate_proto_label(PartnerId=hotels_pb2.PI_BOOKING,
                                                    Surface=hotels_pb2.S_MAPS,
                                                    RedirDestination=hotels_pb2.RD_UNUSED,
                                                    CacheTimestamp=int(time.time()))
    token_bytes = label_codec.encode_token(token.SerializeToString())
    resp = redir_app.redir_by_token(token_bytes, label_codec.encode_label(label.SerializeToString()), label_hash='')
    label_hash = checkers.check_location(resp, 'travel-test.yandex.ru', path='/hotels/book/', token=token_bytes)
    to_check['RedirDestination'] = hotels_pb2.RD_PortalBookPage
    checkers.check_reqans_record(redir_app, 'https://travel-test.yandex.ru/hotels/book', label_hash, to_check)


def test_token_redir_to_hotel_page_without_hash(redir_app, label_codec):
    token = hotels_pb2.TTravelToken(
        OfferId='test',
        GeneratedAtSecondsSinceEpoch=123,
        CheckInDateDaysSinceEpoch=123,
        CheckOutDateDaysSinceEpoch=125,
        Occupancy='2-2',
        Permalink=1234123,
        Partner=hotels_pb2.PI_BOOKING,
        StringId="originalId"
    )
    label, to_check = checkers.generate_proto_label(PartnerId=hotels_pb2.PI_BOOKING,
                                                    Surface=hotels_pb2.S_PORTAL_SEARCH,
                                                    RedirDestination=hotels_pb2.RD_UNUSED,
                                                    CacheTimestamp=int(time.time()))
    token_bytes = label_codec.encode_token(token.SerializeToString())
    resp = redir_app.redir_by_token(token_bytes, label_codec.encode_label(label.SerializeToString()), label_hash='')
    label_hash = checkers.check_location(resp, 'travel-test.yandex.ru', path='/hotels/hotel/', token=token_bytes,
                                         params={
                                             'hotelPermalink': '1234123',
                                             'checkinDate': '2019-05-04',
                                             'checkoutDate': '2019-05-06',
                                             'adults': '2',
                                             'childrenAges': '2',

                                         })
    to_check['RedirDestination'] = hotels_pb2.RD_PortalHotelPage
    checkers.check_reqans_record(
        redir_app,
        'https://travel-test.yandex.ru/hotels/hotel/?adults=2&checkinDate=2019-05-04&checkoutDate=2019-05-06&childrenAges=2&hotelPermalink=1234123',
        label_hash, to_check)


def test_token_redir_to_hotel_page_without_hash_with_utm_override(redir_app, label_codec):
    token = hotels_pb2.TTravelToken(
        OfferId='test',
        GeneratedAtSecondsSinceEpoch=123,
        CheckInDateDaysSinceEpoch=123,
        CheckOutDateDaysSinceEpoch=125,
        Occupancy='2-2',
        Permalink=1234123,
        Partner=hotels_pb2.PI_BOOKING,
        StringId="originalId",
    )
    label, to_check = checkers.generate_proto_label(PartnerId=hotels_pb2.PI_BOOKING,
                                                    Surface=hotels_pb2.S_PORTAL_SEARCH,
                                                    RedirDestination=hotels_pb2.RD_UNUSED,
                                                    CacheTimestamp=int(time.time()))
    to_check['Source'] = 'utm_source_override'
    to_check['Medium'] = 'utm_medium_override'
    to_check['Campaign'] = 'utm_campaign_override'
    to_check['Content'] = 'utm_content_override'
    to_check['Term'] = 'utm_term_override'
    token_bytes = label_codec.encode_token(token.SerializeToString())
    resp = redir_app.redir_by_token(token_bytes, label_codec.encode_label(label.SerializeToString()), label_hash='',
                                    other_params={
                                        'utm_source': to_check['Source'],
                                        'utm_medium': to_check['Medium'],
                                        'utm_campaign': to_check['Campaign'],
                                        'utm_content': to_check['Content'],
                                        'utm_term': to_check['Term'],
    })
    label_hash = checkers.check_location(resp, 'travel-test.yandex.ru', path='/hotels/hotel/', token=token_bytes,
                                         params={
                                             'hotelPermalink': '1234123',
                                             'checkinDate': '2019-05-04',
                                             'checkoutDate': '2019-05-06',
                                             'adults': '2',
                                             'childrenAges': '2',
                                         })
    to_check['RedirDestination'] = hotels_pb2.RD_PortalHotelPage
    checkers.check_reqans_record(
        redir_app,
        'https://travel-test.yandex.ru/hotels/hotel/?adults=2&checkinDate=2019-05-04&checkoutDate=2019-05-06'
        '&childrenAges=2&hotelPermalink=1234123&utm_campaign=utm_campaign_override&utm_content=utm_content_override'
        '&utm_medium=utm_medium_override&utm_source=utm_source_override&utm_term=utm_term_override',
        label_hash, to_check)


def prepare_proto_label_for_by_offer_id():
    label, to_check = checkers.generate_proto_label(PartnerId=hotels_pb2.PI_BOOKING,
                                                    Surface=hotels_pb2.S_PORTAL_SEARCH,
                                                    RedirDestination=hotels_pb2.RD_UNUSED,
                                                    CacheTimestamp=int(time.time()),
                                                    Permalink=12345,
                                                    CheckInDate='2020-10-25',
                                                    Nights=5,
                                                    Occupancy='3-7,10')
    return label, to_check


def test_redir_by_offer_id_partner_link(redir_app, label_codec):
    offer_id = 'offer-id-with-partner-link'
    label, to_check = prepare_proto_label_for_by_offer_id()
    to_check['RedirDestination'] = hotels_pb2.RD_Partner

    resp = redir_app.redir_by_offer_id(offer_id, label_codec.encode_label(label.SerializeToString()))

    label_hash = checkers.check_location(resp, 'ya.ru', params={'a': '1', 'test': '2'}, https=False)
    checkers.check_reqans_record(redir_app, 'http://ya.ru/?a=1&test=2', label_hash, to_check)


def test_redir_by_offer_id_token(redir_app, label_codec):
    offer_id = 'offer-id-with-token'
    label, to_check = prepare_proto_label_for_by_offer_id()
    to_check['RedirDestination'] = hotels_pb2.RD_PortalHotelPage

    to_check['Source'] = 'utm_source_override'
    to_check['Medium'] = 'utm_medium_override'
    to_check['Campaign'] = 'utm_campaign_override'
    to_check['Content'] = 'utm_content_override'
    to_check['Term'] = 'utm_term_override'

    resp = redir_app.redir_by_offer_id(offer_id, label_codec.encode_label(label.SerializeToString()),
                                       other_params={
                                           'utm_source': to_check['Source'],
                                           'utm_medium': to_check['Medium'],
                                           'utm_campaign': to_check['Campaign'],
                                           'utm_content': to_check['Content'],
                                           'utm_term': to_check['Term']
    })

    label_hash = checkers.check_location(resp, 'travel-test.yandex.ru', path='/hotels/hotel/',
                                         token='Dt7LG2hyS-cPoqLiQqut1c-zqPmxT9VnwKF6TnY7OECpPirVWy0FqEdtiNc5',
                                         params={
                                             'hotelPermalink': '12345',
                                             'checkinDate': '2020-10-25',
                                             'checkoutDate': '2020-10-30',
                                             'adults': '3',
                                             'childrenAges': '7,10',
                                         })
    checkers.check_reqans_record(
        redir_app,
        'https://travel-test.yandex.ru/hotels/hotel/?adults=3&checkinDate=2020-10-25&checkoutDate=2020-10-30'
        '&childrenAges=7%2C10&hotelPermalink=12345&utm_campaign=utm_campaign_override&utm_content=utm_content_override'
        '&utm_medium=utm_medium_override&utm_source=utm_source_override&utm_term=utm_term_override',
        label_hash, to_check)


def test_redir_by_offer_id_not_found(redir_app, label_codec):
    offer_id = 'unexistent'
    label, to_check = prepare_proto_label_for_by_offer_id()
    to_check['RedirDestination'] = hotels_pb2.RD_PortalHotelPage

    resp = redir_app.redir_by_offer_id(offer_id, label_codec.encode_label(label.SerializeToString()))

    checkers.check_location(resp, 'travel-test.yandex.ru', label_param=None, path='/hotels/hotel/',
                                         params={
                                             'hotelPermalink': '12345',
                                             'checkinDate': '2020-10-25',
                                             'checkoutDate': '2020-10-30',
                                             'adults': '3',
                                             'childrenAges': '7,10',
                                         })
    checkers.check_no_reqans_records(redir_app)
