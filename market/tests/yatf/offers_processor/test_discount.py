# coding=utf-8

"""
Скидка = (1 - price/oldprice) * 100%
Позитивные кейсы:
    * Размер скидки попадает в [5; 95] - в тесте рассматриваем граничные случаи
        + округления
--
https://yandex.ru/support/partnermarket/oldprice.html
"""

import math
import pytest
from hamcrest import assert_that

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecordRecursive
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
from market.idx.pylibrary.offer_flags.flags import OfferFlags

from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
    generate_binary_price_dict
)

import yt.wrapper as yt


test_data = [
    {  # valid price & oldprice, discount: 5%
        'offer_id': '1',
        'price': 95,
        'oldprice': 100,
        'price_history': 1200
    },
    {  # valid price & oldprice, discount: 4,5%
        'offer_id': '2',
        'price': 955,
        'oldprice': 1000,
        'price_history': 1200
    },
    {  # valid price & oldprice, discount: 95%
        'offer_id': '3',
        'price': 5,
        'oldprice': 100,
        'price_history': 1200
    },
    {  # valid price & oldprice, discount: 95,4%
        'offer_id': '4',
        'price': 46,
        'oldprice': 1000,
        'price_history': 1200
    },
    {  # valid price & oldprice, discount: 95,45%
        'offer_id': '5',
        'price': 455,
        'oldprice': 10000,
        'price_history': 12000
    },
    {  # valid price & oldprice
        'offer_id': '6',
        'price': 600,
        'oldprice': 1000,
        'price_history': 1200
    },
    {  # equal price_history
        'offer_id': '9',
        'price': 70,
        'oldprice': 100,
        'price_history': 100
    },
    {  # less price_history
        'offer_id': '10',
        'price': 70,
        'oldprice': 100,
        'price_history': 75
    },
    {  # too small price delta, less than 1 min currency item (rub, byr, etc)
        'offer_id': '11',
        'price': 1.55,
        'oldprice': 2.44,
        'price_history': 1200
    },
    {  # valid price & oldprice, discount: 5%, blue offer
        'offer_id': '12',
        'price': 95,
        'oldprice': 100,
        'price_history': 1200,
        'is_blue_offer': True,
    },
    {  # valid price & oldprice, discount: 1% but > 500rub, blue offer - valid discount
        'offer_id': '15',
        'price': 99400,
        'oldprice': 100000,
        'price_history': 100000,
        'is_blue_offer': True,
    },
    {  # valid price & oldprice, discount > 500rub but < 1% , blue offer - invalid discount
        'offer_id': '16',
        'price': 999400,
        'oldprice': 1000000,
        'price_history': 1000000,
        'is_blue_offer': True,
    },
    {  # discount in 1 rub - it's okey
        'offer_id': '17',
        'price': 1,
        'oldprice': 2,
        'price_history': 1200
    },
]


history_price_date = yt.yson.YsonUint64(20180102)


@pytest.fixture(scope="module")
def genlog_rows():
    rows = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            binary_price=generate_binary_price_dict(data['price']),
            binary_oldprice=generate_binary_price_dict(data['oldprice']),
            flags=OfferFlags.IS_CUTPRICE,
        )
        if 'price_history' in data:
            offer['binary_history_price'] = generate_binary_price_dict(
                price=data['price_history'],
            )
            offer['history_price_date_yyyy_mm_dd'] = history_price_date
        if 'is_blue_offer' in data:
            offer['is_blue_offer'] = data['is_blue_offer']

        rows.append(offer)

    return rows


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_price_and_oldprice(workflow):
    for data in test_data:
        expected_price = None
        expected_oldprice = None

        if 'oldprice' in data and 'price_history' in data and data['oldprice'] != 0:
            discount = math.floor((1.0 - float(data['price']) / float(data['oldprice'])) * 100.0 + 0.5)
            # difference between price and old price should be more than 1 rub
            # upped valid discount boarder: <= 95%
            # lower valid discount boarder: >= 5%, but if it is blue offer if is valid is absolute sum of discount is >= 500pub and >= 1%
            # check history only for White offers now
            if (
                data['price'] <= data['oldprice'] - 1
                and discount <= 95
                and (discount >= 5 or (data.get('is_blue_offer', False) and data['oldprice'] - data['price'] >= 500 and discount >= 1))
                and (data['price_history'] >= data['oldprice'] or data.get('is_blue_offer', False))
            ):
                expected_oldprice = yt.yson.YsonUint64(int(data['oldprice']) * 10000000)

        if 'price' in data:
            expected_price = yt.yson.YsonUint64(data['price'] * 10000000)
        offer = {
            'offer_id': data['offer_id'],
            'binary_price': {'price': expected_price},
        }
        if expected_oldprice:
            offer['binary_oldprice'] = {'price': expected_oldprice}
        assert_that(
            workflow,
            HasGenlogRecordRecursive(
                offer
            )
        )
