# coding: utf-8

import datetime
import freezegun
import mock
import pytest
import requests

from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import ChangeOfferRequest
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import PUSH_PARTNER_OFFICE
from market.proto.common.common_pb2 import PriceExpression

from market.idx.admin.system_offers.lib.config import make_object
from market.idx.admin.system_offers.lib.offer_updaters.offer_blue_1p import OfferBlue1P
from market.idx.admin.system_offers.lib.util import UPDATE_URL_TEMPLATE


@pytest.fixture(scope='module')
def config():
    yield make_object({
        'datacamp': {
            'host': 'test_dc_host',
            'timeout': 5,
            'tvm_client_id': None,
        },
        'tvm': {
            'client_id': None,
            'secret_path': None,
        },
        'offer_blue_1p': {
            'run_period': 1,
            'warehouse_id': 100,
            'shop_id': 200,
            'offer_id': 300,
        }
    })


@pytest.mark.parametrize('current_time', [
    '2020-04-10 00:00:00',
    '2020-04-10 00:00:01',
])
def test_update_offer(mocker, config, current_time):
    with freezegun.freeze_time(time_to_freeze=current_time):
        mocker.patch('requests.Session.put')
        offer_updater = OfferBlue1P(config)
        offer_updater.update()
        url = UPDATE_URL_TEMPLATE.format(
            host=config.datacamp.host, shop_id=config.offer_blue_1p.shop_id,
            offer_id=config.offer_blue_1p.offer_id, warehouse_id=config.offer_blue_1p.warehouse_id)

        requests.Session.put.assert_called_once_with(url, data=mock.ANY, timeout=config.datacamp.timeout, headers=None)

        request = ChangeOfferRequest()
        request.ParseFromString(requests.Session.put.call_args.kwargs['data'])
        assert len(request.offer) == 1

        offer = request.offer[0]
        timestamp = int(datetime.datetime.now().timestamp())
        assert offer.identifiers.shop_id == config.offer_blue_1p.shop_id
        assert offer.price.basic.meta.timestamp.seconds == timestamp
        assert offer.price.basic.meta.source == PUSH_PARTNER_OFFICE
        assert offer.price.basic.binary_price == PriceExpression(price=timestamp * pow(10, 7))
        assert offer.price.basic.vat == 7

        disabled = offer.status.disabled[0]
        assert disabled.meta.timestamp.seconds == timestamp
        assert disabled.meta.source == PUSH_PARTNER_OFFICE
        assert disabled.flag == (timestamp % 2 != 0)
