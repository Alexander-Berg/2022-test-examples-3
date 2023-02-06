# coding: utf-8

import pytest
import json
import time
from hamcrest import assert_that, equal_to

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.proto.api.SetOtraceFlag_pb2 import SetOtraceFlagRequest
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row
from market.pylibrary.proto_utils import message_from_data


SERVICE_OFFERS_TABLE_DATA = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=1,
            offer_id='T1001',
            shop_id=101,
        ),
        meta=DTC.OfferMeta(
            rgb=DTC.WHITE,
        )
    )),
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=2,
            offer_id='T2001',
            shop_id=201,
        ),
        meta=DTC.OfferMeta(
            rgb=DTC.WHITE,
        )
    )),
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=3,
            offer_id='T3001',
            shop_id=301,
        ),
        meta=DTC.OfferMeta(
            rgb=DTC.WHITE,
        )
    )),
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=4,
            offer_id='T4001',
            shop_id=401,
        ),
        meta=DTC.OfferMeta(
            rgb=DTC.WHITE,
        )
    ))
    ]


@pytest.fixture(scope='module')
def service_offers():
    return SERVICE_OFFERS_TABLE_DATA


@pytest.fixture(scope='module')
def actual_service_offers():
    return []


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        service_offers_table,
        actual_service_offers_table
):
    with make_stroller(
        config,
        yt_server,
        log_broker_stuff,
        service_offers_table=service_offers_table,
        actual_service_offers_table=actual_service_offers_table
    ) as stroller_env:
        yield stroller_env


def get_should_trace(json_data):
    return json_data["offers"][0]["tech_info"]["otrace_info"]["should_trace"]


def test_set_flag(stroller, service_offers_table):
    """Проверяем выставление флажка на нескольких офферах, одного из которых не существует"""
    message = message_from_data(
        {
            "offers" : [
                {
                    'business_id' : 1,
                    'offer_id' : 'T1001',
                    'shop_id' : 101,
                },
                {
                    'business_id' : 2,
                    'offer_id' : 'T2001',
                    'shop_id' : 201,
                },
                {
                    'business_id' : 1337,
                    'offer_id' : 'non-existent-offer',
                    'shop_id' : 1338,
                }
            ]
        },
        SetOtraceFlagRequest()
    )
    # Сам запрос
    response = stroller.post(path='/v1/set_otrace_flag', data=message.SerializeToString())
    assert_that(response, HasStatus(200))
    json_data = json.loads(response.data)
    assert_that(json_data["requested"], equal_to(3))
    assert_that(json_data["found"], equal_to(2))
    assert_that(json_data["changed"], equal_to(2))
    assert_that(json_data["updated"], equal_to(2))
    # Проверяем, что флажки и авторство проставились
    check_response1 = stroller.get(path='/v1/partners/1/offers/services/101?offer_id=T1001&format=json')
    should_trace1 = get_should_trace(json.loads(check_response1.data))
    assert_that(should_trace1["flag"], equal_to(True))
    assert_that(should_trace1["meta"]["applier"], equal_to("STROLLER"))

    check_response2 = stroller.get(path='/v1/partners/2/offers/services/201?offer_id=T2001&format=json')
    should_trace2 = get_should_trace(json.loads(check_response2.data))
    assert_that(should_trace2["flag"], equal_to(True))
    assert_that(should_trace2["meta"]["applier"], equal_to("STROLLER"))


def test_unset_flag(stroller, service_offers_table):
    """Проверяем сброс флажка на нескольких офферах
        - Один с выставленным заранее флажком
        - Один с флажком, выставленным через хендлер тут же
        - Один без флажка
        - Одного нет"""
    # Сперва выставляем один из офферов
    set_message = message_from_data(
        {
            "offers" : [
                {
                    'business_id' : 3,
                    'offer_id' : 'T3001',
                    'shop_id' : 301,
                }
            ]
        },
        SetOtraceFlagRequest()
    )
    stroller.post(path='/v1/set_otrace_flag', data=set_message.SerializeToString())
    # Проверяем, что флажок выставился
    check_response3 = stroller.get(path='/v1/partners/3/offers/services/301?offer_id=T3001&format=json')
    should_trace3 = get_should_trace(json.loads(check_response3.data))
    assert_that(should_trace3["flag"], equal_to(True))
    assert_that(should_trace3["meta"]["applier"], equal_to("STROLLER"))
    # Чтобы штамп сброса не совпал со штампов выставления
    time.sleep(2)
    # И теперь проверяем сброс сразу на всех
    unset_message = message_from_data(
        {
            "offers" : [
                # Должен быть выставлен
                {
                    'business_id' : 3,
                    'offer_id' : 'T3001',
                    'shop_id' : 301,
                },
                # Не должен быть выставлен
                {
                    'business_id' : 4,
                    'offer_id' : 'T4001',
                    'shop_id' : 401,
                },
                # Не существует
                {
                    'business_id' : 1337,
                    'offer_id' : 'non-existent-offer',
                    'shop_id' : 1338,
                }
            ]
        },
        SetOtraceFlagRequest()
    )
    # Сам запрос
    unset_response = stroller.post(path='/v1/set_otrace_flag?unset', data=unset_message.SerializeToString())
    assert_that(unset_response, HasStatus(200))
    unset_json_data = json.loads(unset_response.data)
    assert_that(unset_json_data["requested"], equal_to(3))
    assert_that(unset_json_data["found"], equal_to(2))
    assert_that(unset_json_data["changed"], equal_to(1))
    # Проверяем, что сбросились флаг и автор
    check_response4 = stroller.get(path='/v1/partners/3/offers/services/301?offer_id=T3001&format=json')
    should_trace4 = get_should_trace(json.loads(check_response4.data))
    assert_that(should_trace4["flag"], equal_to(False))
    assert_that("applier" in should_trace4["meta"], equal_to(False))
