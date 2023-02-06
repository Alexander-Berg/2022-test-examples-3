# coding: utf-8

import allure
import requests
import logging
import yatest
import pytest
import time

from google.protobuf.json_format import MessageToJson

from allure.constants import AttachmentType
from constants import (
    STROLLER_TVM_ID,
    TVM_TESTING_ID,
)

from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import FullOfferResponse
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC

from market.idx.pylibrary.datacamp.utils import get_tvm_secret


from library.python.svn_version import svn_revision

from market.pylibrary.putil.protector import retry
from market.idx.datacamp.controllers.stroller.yatf.request_utils import request_offer, StrollerClient


@retry(retries_count=4, exceptions=(requests.exceptions.HTTPError, ValueError), timeout=5)
def get_stroller_offer_response(stroller, shop_id, offer_id, whid):
    response = request_offer(stroller, shop_id, offer_id, whid)
    return response


def get_stroller_client(tvm=None):
    host = yatest.common.get_param("datacamp_host")
    port=80
    tvm_path = yatest.common.get_param("tvm_path")
    if tvm_path:
        tvm_secret = get_tvm_secret(yatest.common.get_param("tvm_path"))
    else:
        tvm_secret = tvm
    return StrollerClient(STROLLER_TVM_ID, TVM_TESTING_ID, tvm_secret, host, port)


class Offer:
    def __init__(self, shop_id, offer_id, whid):
        self.shop_id = shop_id
        self.offer_id = offer_id
        self.whid = whid

    def __str__(self):
        return "<shop_id: {} , offer_id: {} , whid: {} >".format(
            str(self.shop_id),
            str(self.offer_id.encode('utf-8')),
            str(self.whid)
        )


class DataCampResponse:
    def __init__(self):
        self.stroller = get_stroller_client()

    def stroller_response(self, offer):
        with allure.step('Получение результата от datacamp stroller для ' + str(offer)):
            logging.info('Revision {}'.format(str(svn_revision())))
            response = self.stroller.get_offer(offer.shop_id, offer.offer_id, offer.whid)
            allure.attach('Запрос', str(response.url))
            allure.attach('Статус код', str(response.url))
            proto = FullOfferResponse()
            proto.ParseFromString(response.data)
            allure.attach('Ответ', str(MessageToJson(proto).encode('utf-8')), type=AttachmentType.TEXT)
        return response

    def stroller_set_price(self, offer, price):
        source = DTC.PUSH_PARTNER_OFFICE
        timestamp = int(time.time())
        with allure.step('Установка цены ' + str(price) + ' через datacamp stroller для ' + str(offer)):
            logging.info('Revision {}'.format(str(svn_revision())))
            response = self.stroller.set_price(offer.shop_id, offer.offer_id, offer.whid, timestamp, source, price)
            allure.attach('Запрос', str(response.url))
            allure.attach('Статус код', str(response.url))
            allure.attach('Ответ', str(response.data), type=AttachmentType.TEXT)
        return response


stroller_only = pytest.mark.skipif(yatest.common.get_param('service') == 'stroller', reason="Only stroller test. Skip it!")
