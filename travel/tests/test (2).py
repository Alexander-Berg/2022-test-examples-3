#!/usr/bin/env python
# encoding: utf-8

from test_context import TestContext, TravellineRatePlanType, DolphinItemType, BNovoRatePlanType, RefundRule

import travel.proto.commons_pb2 as commons_pb2
import travel.hotels.proto2.hotels_pb2 as hotels_pb2
import uuid
import logging

from checkers import *

# TODO
# EnableOpId
# тесты на сложный выбор best subkey, когда записей много. Пожалуй, через рандомный (но фиксированный набор данных) и канонизацию


def test_nonexistent_hotel(oc_app):
    s_id = '123456777~ytravel_booking.777'
    req = prepare_request(SHotelId=s_id)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, DEFAULT_DATE, DEFAULT_NIGHTS, DEFAULT_AGES, is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {s_id: {'WasFound': False}})


def test_single_hotel(oc_app):
    s_id = '234567891~ytravel_ostrovok.1'
    req = prepare_request(SHotelId=s_id)
    resp = oc_app.read(req)
    # Проверяем автовыбор наилучшей даты
    check_general(oc_app, req, resp, '2018-01-02', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {
            'OperatorId': [4, 4, 21, 24],
            'Price': [1001, 1002, 1005, 1005],
    }}})


def test_single_hotel_USD(oc_app):
    s_id = '234567891~ytravel_ostrovok.1'
    req = prepare_request(SHotelId=s_id, Currency='USD')
    resp = oc_app.read(req)
    # Проверяем автовыбор наилучшей даты
    check_general(oc_app, req, resp, '2018-01-02', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {
            'OperatorId': [4, 4, 21, 24],
            'Price': [101, 102, 105, 105],
    }}})


def test_single_hotel_full(oc_app):
    s_id = '234567891~ytravel_ostrovok.1'
    req = prepare_request(SHotelId=s_id, Full=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-02', 1, '88,88', is_finished=True, progress=(1, 1))
    # Проверяем, правильный порядок партнёров при одинаковой цене
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {
            'OperatorId': [4, 4, 4, 4, 21, 24],
            'Price': [1001, 1002, 1003, 1004, 1005, 1005],
        }},
    })


def test_single_hotel_with_date(oc_app):
    s_id = '234567891~ytravel_ostrovok.1'
    req = prepare_request(SHotelId=s_id, Date='2018-01-03')
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-03', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {
            'OperatorId': [4, 4],
            'Price': [2001, 2002],
        }},
    })


def test_single_hotel_with_checkin_date(oc_app):
    s_id = '234567891~ytravel_ostrovok.1'
    req = prepare_request(SHotelId=s_id, CheckInDate='2018-01-03')
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-03', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {
            'OperatorId': [4, 4],
            'Price': [2001, 2002],
        }},
    })


def test_single_hotel_with_checkin_and_out_date(oc_app):
    s_id = '234567891~ytravel_ostrovok.1'
    req = prepare_request(SHotelId=s_id, CheckInDate='2018-01-03', CheckOutDate='2018-01-15')
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-03', 12, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {'WasFound': False},
    })


def test_single_hotel_with_nondistinguishable_offers(oc_app):
    s_id = '234567891~ytravel_ostrovok.1'
    req = prepare_request(SHotelId=s_id, Date='2018-01-04', Nights=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-04', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {
            'OperatorId': [4, 4],
            'Price': [2001, 2005],
        }},
    })


def test_single_hotel_wrong_date(oc_app):
    s_id = '234567891~ytravel_ostrovok.1'
    req = prepare_request(SHotelId=s_id, Date='2019-01-01')
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2019-01-01', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {s_id: {'WasFound': False}})


def test_single_hotel_no_offers(oc_app):
    # This hotel is in cache, but the only record has no offers -> return default key
    s_id = '234567891~ytravel_ostrovok.6'
    req = prepare_request(SHotelId=s_id)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, DEFAULT_DATE, DEFAULT_NIGHTS, DEFAULT_AGES, is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {s_id: {'WasFound': False}})


# tests with searcher
def test_single_hotel_with_usesearcher_from_cache(oc_app):
    s_id = '234567891~ytravel_ostrovok.1'
    req = prepare_request(SHotelId=s_id, UseSearcher=1, RequestId=0)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-02', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {
            'OperatorId': [4, 4, 21, 24],
            'Price': [1001, 1002, 1005, 1005]
        }}
    })


def test_single_hotel_with_usesearcher_from_searcher_unfinished(oc_app, searcher_session):
    s_id = '234567891~ytravel_ostrovok.1'
    req = prepare_request(SHotelId=s_id, UseSearcher=1, Nights=2, RequestId=0)
    searcher_session.expect_request(
        {'30.1'},
        CheckInDate='2018-01-02',
        CheckOutDate='2018-01-04',
        Occupancy='2',
        Permalink=int(get_permalink(s_id)),
    )
    resp = oc_app.read(req)
    oc_app.wait_flush()
    check_general(oc_app, req, resp, DEFAULT_DATE, 2, '88,88', is_finished=False, progress=(0, 1))
    check_hotels(oc_app, req, resp, {s_id: {'WasFound': False, 'IsFinished': False}})


def test_single_hotel_with_usesearcher_placeholder_interactive(oc_app):
    s_id = '123~ytravel_booking.2'
    req = prepare_request(SHotelId=s_id, Date='2018-09-20', Nights=1, Ages='88,88', UseSearcher=1, RequestId=0)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-09-20', 1, '88,88', is_finished=False, progress=(0, 1))
    check_hotels(oc_app, req, resp, {s_id: {'WasFound': False, 'IsFinished': False}})


def test_single_hotel_with_usesearcher_placeholder_background(oc_app, searcher_session):
    # Интерактивный запрос должен делаться, даже если есть background placeholder
    s_id = '123~ytravel_booking.2'
    req = prepare_request(SHotelId=s_id, Date='2018-09-25', Nights=1, Ages='88,88', UseSearcher=1, RequestId=0)
    resp = oc_app.read(req)
    oc_app.wait_flush()
    searcher_session.expect_request(
        {'2.2'},
        CheckInDate='2018-09-25',
        CheckOutDate='2018-09-26',
        Occupancy='2',
        Permalink=int(get_permalink(s_id)),
    )
    check_general(oc_app, req, resp, '2018-09-25', 1, '88,88', is_finished=False, progress=(0, 1))
    check_hotels(oc_app, req, resp, {s_id: {'WasFound': False, 'IsFinished': False}})


# HOTELS-3350 Tests for WasFound flag, which is complex...
def test_wasfound_partial_true_with_offers(oc_app):
    s_id = '123~ytravel_booking.3~ytravel_ostrovok.5'
    req = prepare_request(SHotelId=s_id, Full=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-03', 1, '88,88', is_finished=True, progress=(2, 2))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {
            'OperatorId': [4, 4, 4, 4, 24, 21],
            'Price': [5001, 5002, 5003, 5004, 5005, 5006],
        }, 'WasFound': True},
    })


def test_wasfound_partial_false(oc_app):
    # HOTELS-3350
    s_id = '123~ytravel_booking.3~ytravel_ostrovok.6'
    req = prepare_request(SHotelId=s_id, Full=1, Date='2018-01-03', Nights=1, Ages='88,88')
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-03', 1, '88,88', is_finished=True, progress=(2, 2))
    check_hotels(oc_app, req, resp, {
        s_id: {"WasFound": False}
    })


def test_wasfound_complete_true(oc_app):
    # HOTELS-3350
    s_id = '123~ytravel_ostrovok.6'
    req = prepare_request(SHotelId=s_id, Full=1, Date='2018-01-03', Nights=1, Ages='88,88')
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-03', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {'WasFound': True},
    })


# multihotel tests
def test_multi_hotel(oc_app):
    s_id_0 = '234567894~ytravel_ostrovok.4'
    s_id_1 = '234567895~ytravel_ostrovok.5'
    req = prepare_request(SHotelId=[s_id_0, s_id_1], Full=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-03', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id_0: {'PriceFields': {
            'OperatorId': [4, 4, 4, 4, 24, 21],
            'Price': [4001, 4002, 4003, 4004, 4005, 4006],
        }},
        s_id_1: {'PriceFields': {
            'OperatorId': [4, 4, 4, 4, 24, 21],
            'Price': [5001, 5002, 5003, 5004, 5005, 5006],
        }},
    })


def test_multi_hotel_with_single_org_and_similar(oc_app):
    s_id_0 = '234567894~ytravel_ostrovok.4~f.single_org'
    s_id_1 = '234567895~ytravel_ostrovok.5~f.similar'
    req = prepare_request(SHotelId=[s_id_0, s_id_1], Full=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-03', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id_0: {'PriceFields': {
            'OperatorId': [4, 4, 4, 4, 24, 21],
            'Price': [4001, 4002, 4003, 4004, 4005, 4006],
        }},
        s_id_1: {
            'IsBrief': True,
            'PriceFields': {
                'Price': [5001],
            }
        },
    })


def test_multi_hotel_with_single_org(oc_app):
    s_id_0 = '234567894~ytravel_ostrovok.4~f.single_org'
    s_id_1 = '234567895~ytravel_ostrovok.5'
    req = prepare_request(SHotelId=[s_id_0, s_id_1], Full=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-03', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id_0: {'PriceFields': {
            'OperatorId': [4, 4, 4, 4, 24, 21],
            'Price': [4001, 4002, 4003, 4004, 4005, 4006],
        }},
        s_id_1: {
            'IsBrief': True,
            'PriceFields': {
                'OperatorId': [4, 4, 4, 4, 24, 21],
                'Price': [5001, 5002, 5003, 5004, 5005, 5006],
            }
        },
    })


def test_multi_hotel_with_date(oc_app):
    s_id_0 = '234567894~ytravel_ostrovok.4'
    s_id_1 = '234567895~ytravel_ostrovok.5'
    req = prepare_request(SHotelId=[s_id_0, s_id_1], Full=1, Date='2018-01-03')
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-03', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id_0: {'PriceFields': {
            'OperatorId': [4, 4, 4, 4, 24, 21],
            'Price': [4001, 4002, 4003, 4004, 4005, 4006],
        }},
        s_id_1: {'PriceFields': {
            'OperatorId': [4, 4, 4, 4, 24, 21],
            'Price': [5001, 5002, 5003, 5004, 5005, 5006],
        }},
    })


def test_multi_subhotel_with_date(oc_app):
    s_id = '234567894~ytravel_ostrovok.1~ytravel_booking.1'
    req = prepare_request(SHotelId=s_id, Full=1, Date='2018-01-02')
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-02', 1, '88,88', is_finished=True, progress=(2, 2))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {
            'OperatorId': [4, 4, 4, 4, 21, 24, 2, 2, 2, 2],
            'Price': [1001, 1002, 1003, 1004, 1005, 1005, 1101, 1102, 1103, 1104],
        }},
    })


def test_multi_hotel_with_use_searcher(oc_app, searcher_session):
    s_id_0 = '234567894~ytravel_ostrovok.4'  # already in cache
    s_id_1 = '234567897~ytravel_ostrovok.7'
    req = prepare_request(SHotelId=[s_id_0, s_id_1], Full=1, UseSearcher=1, RequestId=0)
    searcher_session.expect_request(
        {'30.7'},
        CheckInDate='2018-01-03',
        CheckOutDate='2018-01-04',
        Occupancy='2',
        Permalink=int(get_permalink(s_id_1)),
    )
    resp = oc_app.read(req)
    oc_app.wait_flush()
    check_general(oc_app, req, resp, '2018-01-03', DEFAULT_NIGHTS, DEFAULT_AGES, is_finished=False, progress=(0, 1))
    check_hotels(oc_app, req, resp, {
        s_id_0: {'PriceFields': {'OperatorId': [4, 4, 4, 4, 24, 21], 'Price': [4001, 4002, 4003, 4004, 4005, 4006]}},
        s_id_1: {'WasFound': False, 'IsFinished': False}
    })


def test_capacity_ages_3(oc_app):
    # not found due to capacity not match occupancy
    s_id = '1~ytravel_expedia.6'
    req = prepare_request(SHotelId=s_id, Ages='88,88,88', Date='2018-09-22', Nights=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-09-22', 1, '88,88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {'WasFound': False},
    })


def test_capacity_ages_4(oc_app):
    s_id = '1~ytravel_expedia.6'
    req = prepare_request(SHotelId=s_id, Ages='88,88,88,88', Date='2018-09-22', Nights=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-09-22', 1, '88,88,88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {'OperatorId': [40, 40, 40], 'Price': [6009, 6010, 6011]}},
    })


def test_capacity_ages_3_1(oc_app):
    # not found due to capacity not match occupancy
    s_id = '1~ytravel_expedia.6'
    req = prepare_request(SHotelId=s_id, Ages='5,88,88,88', Date='2018-09-22', Nights=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-09-22', 1, '5,88,88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {'WasFound': False},
    })


def test_capacity_ages_5(oc_app):
    # not found due to capacity not match occupancy
    s_id = '1~ytravel_expedia.6'
    req = prepare_request(SHotelId=s_id, Ages='88,88,88,88,88', Date='2018-09-22', Nights=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-09-22', 1, '88,88,88,88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {'WasFound': False},
    })


def test_hotel_wo_plus(oc_app):
    s_id = '234567891~ytravel_ostrovok.1'
    req = prepare_request(SHotelId=s_id)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2018-01-02', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {
            'OtherFields': {'IsPlusAvailable': False}
        }
    })


def test_boy_travelline_precedence(oc_app):
    # Travelline is the coolest
    s_id = '4408~ytravel_expedia.HOTELS-4408~ytravel_travelline.HOTELS-4408~ytravel_dolphin.HOTELS-4408'
    req = prepare_request(SHotelId=s_id, Ages='88,88', Date='2019-09-25', Nights=1, Full=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2019-09-25', 1, '88,88', is_finished=True, progress=(3, 3))
    check_hotels(oc_app, req, resp, {
        s_id: {
            'PriceFields': {'OperatorId': [44, 44], 'Price': [6020, 6070]},
            'OtherFields': {'IsPlusAvailable': True},
        },
    })


def test_boy_bnovo_precedence(oc_app):
    # BNovo is the coolest too
    s_id = '4408~ytravel_expedia.HOTELS-4408~ytravel_bnovo.TRAVELBACK-268~ytravel_dolphin.HOTELS-4408'
    req = prepare_request(SHotelId=s_id, Ages='88,88', Date='2019-09-25', Nights=1, Full=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2019-09-25', 1, '88,88', is_finished=True, progress=(3, 3))
    check_hotels(oc_app, req, resp, {
        s_id: {
            'PriceFields': {'OperatorId': [45, 45], 'Price': [6020, 6070]},
            'OtherFields': {'IsPlusAvailable': True},
        },
    })


def test_boy_bnovo_and_travelline_kill_each_other(oc_app):
    # But when bnovo and tl meet, they kill each other
    # By price
    s_id = '4408~ytravel_expedia.HOTELS-4408~ytravel_travelline.HOTELS-4408~ytravel_bnovo.TRAVELBACK-268~ytravel_dolphin.HOTELS-4408'
    req = prepare_request(SHotelId=s_id, Ages='88,88', Date='2019-09-25', Nights=1, Full=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2019-09-25', 1, '88,88', is_finished=True, progress=(4, 4))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {'OperatorId': [40, 40], 'Price': [6011, 6012]}},
    })


def test_boy_dolphin_expedia_precedence(oc_app):
    # By price
    s_id = '4408~ytravel_expedia.HOTELS-4408~ytravel_dolphin.HOTELS-4408'
    req = prepare_request(SHotelId=s_id, Ages='88,88', Date='2019-09-25', Nights=1, Full=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2019-09-25', 1, '88,88', is_finished=True, progress=(2, 2))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {'OperatorId': [40, 40], 'Price': [6011, 6012]}},
    })


def test_boy_dolphin_alone(oc_app):
    # By price
    s_id = '4408~ytravel_dolphin.HOTELS-4408'
    req = prepare_request(SHotelId=s_id, Ages='88,88', Date='2019-09-25', Nights=1, Full=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2019-09-25', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {'OperatorId': [43, 43], 'Price': [6040, 6050]}},
    })


def test_boy_precedence_disabled(oc_app):
    # All boys are here
    s_id = '4408~ytravel_expedia.HOTELS-4408~ytravel_travelline.HOTELS-4408~ytravel_dolphin.HOTELS-4408'
    req = prepare_request(SHotelId=s_id, Ages='88,88', Date='2019-09-25', Nights=1, Full=1, ShowAllBoY=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2019-09-25', 1, '88,88', is_finished=True, progress=(3, 3))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {'OperatorId': [40, 40, 44, 43, 43, 44], 'Price': [6011, 6012, 6020, 6040, 6050, 6070],
                          'SkipReason': [None, None, None, None, None, None]},
               }
    })


def test_boy_skip_disabled(oc_app):
    # All boys are here
    s_id = '4408~ytravel_expedia.HOTELS-4408~ytravel_travelline.HOTELS-4408~ytravel_dolphin.HOTELS-4408'
    req = prepare_request(SHotelId=s_id, Ages='88,88', Date='2019-09-25', Nights=1, Full=1, ShowSkipped=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2019-09-25', 1, '88,88', is_finished=True, progress=(3, 3))
    check_hotels(oc_app, req, resp, {
        s_id: {'PriceFields': {'OperatorId': [44, 44, 40, 40, 43, 43], 'Price': [6020, 6070, 6011, 6012, 6040, 6050],
                          'SkipReason': [None, None, 'SR_OtherBoYOperatorWon', 'SR_OtherBoYOperatorWon',
                                         'SR_OtherBoYOperatorWon', 'SR_OtherBoYOperatorWon']},
               }
    }, check_price_order=False)


def test_permaroom_order(ctx: TestContext):
    hotel = ctx.create_hotel()
    permarooms = [hotel.add_permaroom() for _ in range(2)]
    partner_hotel_booking = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_BOOKING)
    partner_hotel_expedia = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_EXPEDIA, whitelisted=True)

    original_id, date, nights, occupancy = ctx.create_search_params()
    offer_booking_1 = partner_hotel_booking.add_offer(date=date, nights=nights, occupancy=occupancy, price=4002)
    offer_booking_2 = partner_hotel_booking.add_offer(date=date, nights=nights, occupancy=occupancy, price=4001)
    offer_expedia_1 = partner_hotel_expedia.add_offer(date=date, nights=nights, occupancy=occupancy, price=4002)
    offer_expedia_2 = partner_hotel_expedia.add_offer(date=date, nights=nights, occupancy=occupancy, price=4001)
    permarooms[0].add_mapping(offer_booking_2)
    permarooms[0].add_mapping(offer_expedia_2)
    permarooms[1].add_mapping(offer_booking_1)
    permarooms[1].add_mapping(offer_expedia_1)

    result = ctx.get_oc().req_by_hotel(hotel, CatRoomDataSourceId=1, UseNewCatRoom=1, EnableCatRoom=1)
    result.expect_result_with_permarooms([
        (hotel, [
            (permarooms[0], [offer_expedia_2, offer_booking_2]),
            (permarooms[1], [offer_expedia_1, offer_booking_1]),
        ])
    ], strict_permaroom_order=True)


def test_grpc_request_inexistent(oc_app, searcher_session):
    req = hotels_pb2.TSearchOffersRpcReq(
        Subrequest=[
            hotels_pb2.TSearchOffersReq(
                HotelId=hotels_pb2.THotelId(PartnerId=hotels_pb2.PI_BOOKING, OriginalId='2'),
                CheckInDate='2018-06-06', CheckOutDate='2018-06-07', Occupancy='2',
                Currency=commons_pb2.C_RUB,
                OfferCacheUseCache=True,
                OfferCacheUseSearcher=True,
                Id=str(uuid.uuid4()),
                Attribution = hotels_pb2.TRequestAttribution(OfferCacheClientId='boiler'),
            )
        ], Sync=False)
    searcher_session.expect_request(
        {'2.2'},
        CheckInDate='2018-06-06',
        CheckOutDate='2018-06-07',
        Occupancy='2'
    )
    resp = oc_app.grpc_search_offers(req)
    oc_app.wait_flush()
    assert len(resp.Subresponse) == 1
    subresp = resp.Subresponse[0]
    assert subresp.Placeholder is not None


def test_grpc_request_existent(oc_app):
    req = hotels_pb2.TSearchOffersRpcReq(
        Subrequest=[
            hotels_pb2.TSearchOffersReq(
                HotelId=hotels_pb2.THotelId(PartnerId=hotels_pb2.PI_OSTROVOK, OriginalId='4'),
                CheckInDate='2018-01-03', CheckOutDate='2018-01-04', Occupancy='2',
                Currency=commons_pb2.C_RUB,
                OfferCacheUseCache=True,
                OfferCacheUseSearcher=True,
                Id=str(uuid.uuid4()),
                Attribution=hotels_pb2.TRequestAttribution(OfferCacheClientId='boiler'),
            )
        ], Sync=False)
    resp = oc_app.grpc_search_offers(req)
    assert len(resp.Subresponse) == 1
    subresp = resp.Subresponse[0]
    assert subresp.Placeholder is not None


def test_grpc_ping(oc_app):
    req = hotels_pb2.TPingRpcReq()
    resp = oc_app.grpc_ping(req)
    assert resp.IsReady is True


def test_random_requests(oc_app):
    # Шлём кучу разнообразных запросов, проверяем что в них все всегда хорошо
    all_partner_codes = set(oc_app.partnercode2id.keys())
    all_partner_codes &= set(oc_app.partner2operators.keys())  # только те партнёры, у которых есть операторы
    for checkin, nights, occup, ages in iterate_random_params():
        for partner_count in range(4, len(all_partner_codes) + 1):
            partner_codes = random.sample(all_partner_codes, partner_count)
            s_id = '999'
            for p in partner_codes:
                s_id += '~%s.random' % p
            req = prepare_request(SHotelId=s_id, Date=format_date(checkin), Nights=nights, Ages=ages, Full=1)
            resp = oc_app.read(req)
            check_general(oc_app, req, resp, format_date(checkin), nights, ages, is_finished=True,
                          progress=(partner_count, partner_count))
            check_hotels(oc_app, req, resp, {s_id: {'PriceFields': {}}}, check_link_pct=5)  # Too long to check all links


def test_blacklist(oc_app, searcher_session):
    s_id_0 = '403000001~ytravel_booking.40301'  # blacklisted, in cache
    s_id_1 = '403000002~ytravel_booking.40302'  # blacklisted, not in cache
    s_id_2 = '234567894~ytravel_ostrovok.4'  # already in cache

    req = prepare_request(SHotelId=[s_id_0, s_id_1, s_id_2], Full=1, UseSearcher=1, RequestId=0)
    resp = oc_app.read(req)
    oc_app.wait_flush()
    check_general(oc_app, req, resp, '2018-01-03', DEFAULT_NIGHTS, DEFAULT_AGES, is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id_0: {'WasFound': False},
        s_id_1: {'WasFound': False},
        s_id_2: {'PriceFields': {'OperatorId': [4, 4, 4, 4, 24, 21], 'Price': [4001, 4002, 4003, 4004, 4005, 4006]}},
    })
    _ = searcher_session  # is used to check that no requests to searcher sent


def test_ignore_blacklist(oc_app, searcher_session):
    s_id_0 = '403000001~ytravel_booking.40301'  # blacklisted, in cache
    s_id_1 = '403000002~ytravel_booking.40302'  # blacklisted, not in cache

    req = prepare_request(SHotelId=[s_id_0, s_id_1], Full=1, UseSearcher=1, RequestId=0, IgnoreBlacklist=1)
    searcher_session.expect_request(
        {'2.40302'},
        CheckInDate='2018-01-03',
        CheckOutDate='2018-01-04',
        Occupancy='2',
        Permalink=int(get_permalink(s_id_1)),
    )
    resp = oc_app.read(req)
    oc_app.wait_flush()
    check_general(oc_app, req, resp, '2018-01-03', DEFAULT_NIGHTS, DEFAULT_AGES, is_finished=False, progress=(0, 1))
    check_hotels(oc_app, req, resp, {
        s_id_0: {'PriceFields': {'OperatorId': [2], 'Price': [4002]}},
        s_id_1: {'WasFound': False, 'IsFinished': False},
    })


def test_whitelist(oc_app, searcher_session):
    s_id_e0 = '255000001~ytravel_expedia.255e04'  # not in whitelist at all, not in cache
    s_id_e1 = '255000002~ytravel_expedia.255e02'  # not in whitelist for expedia, but in cache
    s_id_e2 = '255000003~ytravel_expedia.255e03'  # in whitelist, already in cache

    s_id_d0 = '255000011~ytravel_dolphin.255d04'  # not in whitelist at all, not in cache
    s_id_d1 = '255000012~ytravel_dolphin.255d02'  # in whitelist, already in cache

    s_id_o0 = '255000021~ytravel_ostrovok.255o01' # already in cache

    ids = [s_id_e0, s_id_e1, s_id_e2, s_id_d0, s_id_d1, s_id_o0]
    req = prepare_request(SHotelId=ids, Nights=1, Full=1, UseSearcher=1, RequestId=0)
    resp = oc_app.read(req)
    oc_app.wait_flush()
    check_general(oc_app, req, resp, DEFAULT_DATE, DEFAULT_NIGHTS, DEFAULT_AGES, is_finished=True, progress=(3, 3))
    check_hotels(oc_app, req, resp, {
        s_id_e0: {'WasFound': False},
        s_id_e1: {'WasFound': False},
        s_id_e2: {'PriceFields': {'OperatorId': [40], 'Price': [4001]}},

        s_id_d0: {'WasFound': False},
        s_id_d1: {'PriceFields': {'OperatorId': [43], 'Price': [4002]}},

        s_id_o0: {'PriceFields': {'OperatorId': [4], 'Price': [4003]}},
    })

    _ = searcher_session  # is used to check that no requests to searcher sent


def test_blacklist_and_whitelist(oc_app, searcher_session):
    s_id_0 = '403255001~ytravel_expedia.40325501'  # blacklisted and whitelisted, in cache
    s_id_1 = '403255002~ytravel_expedia.40325502'  # blacklisted and whitelisted, not in cache
    s_id_2 = '255000003~ytravel_expedia.255e03'  # in whitelist, already in cache

    req = prepare_request(SHotelId=[s_id_0, s_id_1, s_id_2], Full=1, UseSearcher=1, RequestId=0)
    resp = oc_app.read(req)
    oc_app.wait_flush()
    check_general(oc_app, req, resp, DEFAULT_DATE, DEFAULT_NIGHTS, DEFAULT_AGES, is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id_0: {'WasFound': False},
        s_id_1: {'WasFound': False},
        s_id_2: {'PriceFields': {'OperatorId': [40], 'Price': [4001]}},
    })
    _ = searcher_session  # is used to check that no requests to searcher sent


def test_travelline_rate_plans(ctx: TestContext):
    hotel = ctx.create_hotel()
    partner_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_TRAVELLINE, whitelisted=True)
    original_id, date, nights, occupancy = ctx.create_search_params()
    offer1 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, travelline_rate_plan_type=TravellineRatePlanType.BLOCKED)
    offer2 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, travelline_rate_plan_type=TravellineRatePlanType.ALLOWED)
    offer3 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, travelline_rate_plan_type=TravellineRatePlanType.UNKNOWN)

    result = ctx.get_oc().req_by_hotel(hotel)
    result.expect_result([
        (hotel, [offer2, offer3])
    ])


def test_dolphin_partner_data(ctx: TestContext):
    for dolphin_item_name in ['dolphin_tour_type', 'dolphin_pansion_type', 'dolphin_room_type', 'dolphin_room_cat_type']:
        hotel = ctx.create_hotel()
        partner_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_DOLPHIN, whitelisted=True)
        original_id, date, nights, occupancy = ctx.create_search_params()
        offer1 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, **{dolphin_item_name: DolphinItemType.BLOCKED})
        offer2 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, **{dolphin_item_name: DolphinItemType.ALLOWED})
        offer3 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, **{dolphin_item_name: DolphinItemType.UNKNOWN})

        result = ctx.get_oc().req_by_hotel(hotel)
        result.expect_result([
            (hotel, [offer2, offer3])
        ])


def test_bnovo_rate_plans(ctx: TestContext):
    hotel = ctx.create_hotel()
    partner_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_BNOVO, whitelisted=True)
    original_id, date, nights, occupancy = ctx.create_search_params()
    offer1 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, bnovo_rate_plan_type=BNovoRatePlanType.BLOCKED)
    offer2 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, bnovo_rate_plan_type=BNovoRatePlanType.ALLOWED)
    offer3 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, bnovo_rate_plan_type=BNovoRatePlanType.UNKNOWN)

    result = ctx.get_oc().req_by_hotel(hotel)
    result.expect_result([
        (hotel, [offer2, offer3])
    ])


def test_no_restricted_offer_for_usual_user(oc_app):
    s_id = '10~ytravel_expedia.10~f.single_org'
    req = prepare_request(SHotelId=s_id, Ages='88,88', Date='2019-10-04', Nights=1, Full=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2019-10-04', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {
            'WasFound': True,
            'PriceFields': {
                'OperatorId': [40],
                'Price': [4002],
            },
        }
    })


def test_restricted_offer_for_restricted_user(oc_app):
    s_id = '10~ytravel_expedia.10~f.single_org'
    req = prepare_request(SHotelId=s_id, Ages='88,88', Date='2019-10-04', Nights=1, Full=1, AllowMobileRates=1)
    resp = oc_app.read(req)
    check_general(oc_app, req, resp, '2019-10-04', 1, '88,88', is_finished=True, progress=(1, 1))
    check_hotels(oc_app, req, resp, {
        s_id: {
            'WasFound': True,
            'PriceFields': {
                'OperatorId': [40],
                'Price': [4001],
                'StrikethroughPrice.Price': [4002]
            },
        }
    })


def test_old_date_ignore(ctx: TestContext):
    hotel = ctx.create_hotel()
    partner_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_BOOKING)
    now_date = format_date(datetime.date.fromtimestamp(FAKE_NOW))
    offer1 = partner_hotel.add_offer(date=date_days_after(now_date, -30))
    offer2 = partner_hotel.add_offer(date=date_days_after(now_date, 1))

    result = ctx.get_oc().req_by_hotel(hotel, date=offer1.date, override_expected_date=offer2.date)
    result.expect_result([(hotel, [offer2])])

    result = ctx.get_oc().req_by_hotel(hotel, date=offer1.date, override_expected_date=offer2.date, AllowPastDates=0)
    result.expect_result([(hotel, [offer2])])

    result = ctx.get_oc().req_by_hotel(hotel, date=offer1.date, AllowPastDates=1)
    result.expect_result([(hotel, [offer1])])


def test_pansion_alias_filters(ctx: TestContext):
    hotel = ctx.create_hotel()
    partner_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_BOOKING)

    original_id, date, nights, occupancy = ctx.create_search_params()
    offer1 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, pansion=hotels_pb2.PT_RO)
    offer2 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, pansion=hotels_pb2.PT_BB)

    result = ctx.get_oc().req_by_hotel(hotel)
    result.expect_result([(hotel, [offer1, offer2])])

    result = ctx.get_oc().req_by_hotel(hotel, FilterPansionAlias='BREAKFAST')
    result.expect_result([(hotel, [offer2])])


def test_new_permarooms_simple(ctx: TestContext):
    hotel = ctx.create_hotel()
    permaroom1 = hotel.add_permaroom()
    permaroom2 = hotel.add_permaroom()
    partner_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_BOOKING)

    original_id, date, nights, occupancy = ctx.create_search_params()
    offer1 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy)
    offer2 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy)
    permaroom1.add_mapping(offer1)
    permaroom1.add_mapping(offer2)

    offer3 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy)
    permaroom2.add_mapping(offer3)

    result = ctx.get_oc().req_by_hotel(hotel, CatRoomDataSourceId=1, UseNewCatRoom=1, EnableCatRoom=1)
    result.expect_result_with_permarooms([
        (hotel, [
            (permaroom1, [offer1, offer2]),
            (permaroom2, [offer3]),
        ])
    ])


def test_new_permarooms_too_many_other(ctx: TestContext):
    hotel = ctx.create_hotel()
    permaroom1 = hotel.add_permaroom()
    permaroom2 = hotel.add_permaroom()
    partner_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_BOOKING)

    original_id, date, nights, occupancy = ctx.create_search_params()
    offer1 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy)
    offer2 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy)
    permaroom1.add_mapping(offer1)
    permaroom1.add_mapping(offer2)

    offer3 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy)
    permaroom2.add_mapping(offer3)

    offer4 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy)

    result = ctx.get_oc().req_by_hotel(hotel, CatRoomDataSourceId=1, UseNewCatRoom=1, EnableCatRoom=1)

    result.expect_result([(hotel, [offer1, offer2, offer3, offer4])], catroom_status='CRS_TooMuchOther')


def test_new_permarooms_several_original_ids(ctx: TestContext):
    hotel = ctx.create_hotel()
    permarooms = [hotel.add_permaroom() for _ in range(2)]

    booking_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_BOOKING)
    original_id, date, nights, occupancy = ctx.create_search_params()
    offer1 = booking_hotel.add_offer(date=date, nights=nights, occupancy=occupancy)
    offer2 = booking_hotel.add_offer(date=date, nights=nights, occupancy=occupancy)
    permarooms[0].add_mapping(offer1)
    permarooms[1].add_mapping(offer2)

    expedia_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_EXPEDIA, whitelisted=True)
    offer3 = expedia_hotel.add_offer(date=date, nights=nights, occupancy=occupancy)
    offer4 = expedia_hotel.add_offer(date=date, nights=nights, occupancy=occupancy)
    permarooms[0].add_mapping(offer3)
    permarooms[1].add_mapping(offer4)

    result = ctx.get_oc().req_by_hotel(hotel, CatRoomDataSourceId=1, UseNewCatRoom=1, EnableCatRoom=1)

    result.expect_result_with_permarooms([
        (hotel, [
            (permarooms[0], [offer1, offer3]),
            (permarooms[1], [offer2, offer4]),
        ])
    ])


def test_new_permarooms_no_offers(ctx: TestContext):
    hotel = ctx.create_hotel()
    permaroom1 = hotel.add_permaroom("a: лексикографически меньше")
    permaroom2 = hotel.add_permaroom("б: лексикографически больше")
    partner_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_BOOKING)

    original_id, date, nights, occupancy = ctx.create_search_params()
    partner_hotel.add_no_offers(date, nights, occupancy)
    permaroom1.add_raw_mapping(hotels_pb2.OI_BOOKING, partner_hotel.original_id, "123")
    permaroom2.add_raw_mapping(hotels_pb2.OI_BOOKING, partner_hotel.original_id, "321")

    result = ctx.get_oc().req_by_hotel(hotel, date=date, nights=nights, occupancy=occupancy, expected_n_operators=1,
                                       CatRoomDataSourceId=1, UseNewCatRoom=1, EnableCatRoom=1, ShowPermaroomsWithNoOffers=1)
    result.expect_result_with_permarooms([
        (hotel, [
            (permaroom1, []),
            (permaroom2, []),
        ])
    ], strict_permaroom_order=True)


def test_new_permarooms_half_offers(ctx: TestContext):
    hotel = ctx.create_hotel()
    permaroom1 = hotel.add_permaroom("б: лексикографически больше")
    permaroom2 = hotel.add_permaroom("a: лексикографически меньше")
    partner_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_BOOKING)

    original_id, date, nights, occupancy = ctx.create_search_params()
    offer1 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy)
    permaroom1.add_mapping(offer1)
    permaroom2.add_raw_mapping(hotels_pb2.OI_BOOKING, partner_hotel.original_id, "123")

    result = ctx.get_oc().req_by_hotel(hotel, date=date, nights=nights, occupancy=occupancy, CatRoomDataSourceId=1,
                                       UseNewCatRoom=1, EnableCatRoom=1, ShowPermaroomsWithNoOffers=1)
    result.expect_result_with_permarooms([
        (hotel, [
            (permaroom1, [offer1]),
            (permaroom2, []),
        ])
    ], strict_permaroom_order=True)


def test_refund_rules(ctx: TestContext):
    hotel = ctx.create_hotel()
    partner_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_BOOKING)

    original_id, date, nights, occupancy = ctx.create_search_params()
    offer1 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, refund_rules=[
        RefundRule(hotels_pb2.RT_FULLY_REFUNDABLE, FAKE_NOW - 100, FAKE_NOW - 90, penalty=None),
        RefundRule(hotels_pb2.RT_REFUNDABLE_WITH_PENALTY, FAKE_NOW - 89, FAKE_NOW + 10, penalty=100),
        RefundRule(hotels_pb2.RT_NON_REFUNDABLE, FAKE_NOW + 11, FAKE_NOW + 100, penalty=None),
    ])

    result = ctx.get_oc().req_by_hotel(hotel)
    result.expect_result([(hotel, [offer1])], expected_refund_types=['RT_REFUNDABLE_WITH_PENALTY'], expected_refund_rules=[
        [
            {
                'Type': 'RT_REFUNDABLE_WITH_PENALTY',
                'StartsAtTimestampSec': FAKE_NOW - 89,
                'EndsAtTimestampSec': FAKE_NOW + 10,
                'Penalty': 100,
            },
            {
                'Type': 'RT_NON_REFUNDABLE',
                'StartsAtTimestampSec': FAKE_NOW + 11,
                'EndsAtTimestampSec': FAKE_NOW + 100,
            }
        ]
    ])


def test_empty_refund_rules(ctx: TestContext):
    hotel = ctx.create_hotel()
    partner_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_BOOKING)

    original_id, date, nights, occupancy = ctx.create_search_params()
    offer1 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy)

    result = ctx.get_oc().req_by_hotel(hotel)
    result.expect_result([(hotel, [offer1])], expected_refund_types=['RT_FULLY_REFUNDABLE'], expected_refund_rules=[[]])


def test_refund_rules_empty_start_end(ctx: TestContext):
    hotel = ctx.create_hotel()
    partner_hotel = hotel.add_partner_hotel(partner_id=hotels_pb2.PI_BOOKING)

    original_id, date, nights, occupancy = ctx.create_search_params()
    offer1 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, price=1000, refund_rules=[
        RefundRule(hotels_pb2.RT_REFUNDABLE_WITH_PENALTY, None, FAKE_NOW + 10, penalty=10),
        RefundRule(hotels_pb2.RT_NON_REFUNDABLE, FAKE_NOW + 11, FAKE_NOW + 100, penalty=None),
    ])
    offer2 = partner_hotel.add_offer(date=date, nights=nights, occupancy=occupancy, price=2000, refund_rules=[
        RefundRule(hotels_pb2.RT_REFUNDABLE_WITH_PENALTY, None, FAKE_NOW - 100, penalty=10),
        RefundRule(hotels_pb2.RT_NON_REFUNDABLE, FAKE_NOW - 99, None, penalty=None),
    ])

    result = ctx.get_oc().req_by_hotel(hotel)
    result.expect_result([(hotel, [offer1, offer2])], expected_refund_types=['RT_REFUNDABLE_WITH_PENALTY', 'RT_NON_REFUNDABLE'], expected_refund_rules=[
        [
            {
                'Type': 'RT_REFUNDABLE_WITH_PENALTY',
                'EndsAtTimestampSec': FAKE_NOW + 10,
                'Penalty': 10,
            },
            {
                'Type': 'RT_NON_REFUNDABLE',
                'StartsAtTimestampSec': FAKE_NOW + 11,
                'EndsAtTimestampSec': FAKE_NOW + 100,
            }
        ],
        [
            {
                'Type': 'RT_NON_REFUNDABLE',
                'StartsAtTimestampSec': FAKE_NOW - 99,
            }
        ]
    ])
