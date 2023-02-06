# coding: utf-8

import datetime
import freezegun
import math
import mock
import pytest
import market.pylibrary.s3.s3 as s3
import xmltodict

from market.idx.admin.system_offers.lib.config import make_object
from market.idx.admin.system_offers.lib.offer_updaters.white_feed import WhiteFeed

from .util import UTC


@pytest.fixture(scope='function')
def mocked_s3_write(mocker):
    mocker.patch.object(s3.Client, '__init__', return_value=None)
    yield mocker.patch.object(s3.Client, 'write')


@pytest.fixture(scope='module')
def config():
    yield make_object({
        'check_mining': False,
        'datacamp': {
            'host': 'test_datacamp_host',
            'tvm_client_id': 666,
            'timeout': 30,
        },
        's3': {
            'host': 'test_s3_host',
            'access_keys_path': 'test_access_keys_path',
            'public_bucket': 'test_public_bucket',
        },
        'tvm': {
            'secret_path': '',
        },
        'white_feed': {
            'run_period': 1,
            'base_offer_id': 1000,
            'price_digits': 6,
            'feed_file_name': 'test_feed_file_name',
            'business_id': 1337,
        }
    })


@pytest.mark.parametrize('timestamp, price', [
    (1586476800, 476800),
    (1586476801, 476801)
])
def test_price(mocked_s3_write, config, timestamp, price):
    current_time = datetime.datetime.fromtimestamp(timestamp, tz=UTC())
    with freezegun.freeze_time(time_to_freeze=current_time):
        feed_updater = WhiteFeed(config)
        feed_updater.update()
        mocked_s3_write.assert_called_once_with(
            config.s3.public_bucket,
            config.white_feed.feed_file_name,
            mock.ANY)
        feed = xmltodict.parse(mocked_s3_write.call_args.args[2])
        offers = feed['yml_catalog']['shop']['offers']['offer']
        isOddTimestamp = int(datetime.datetime.now().timestamp()) % 2 != 0
        if isOddTimestamp:
            offers = [offers]

        expectedOfferNumber = 1 if isOddTimestamp else 2
        assert expectedOfferNumber == len(offers)

        for offer in offers:
            if isOddTimestamp:
                assert int(offer['@id']) % 2 == 0
            assert offer['price'] == str(price)
            assert offer['oldprice'] == str(math.ceil(price * 0.1 + price))
