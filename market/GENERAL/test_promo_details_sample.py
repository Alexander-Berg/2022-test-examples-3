# coding: utf-8

import os
import pytest
import yatest

from market.idx.offers.yatf.resources.offers_indexer.promo_details import PromoDetails
from market.idx.offers.yatf.utils.fixtures import generate_default_blue_3p_promo, generate_default_msku
from market.idx.yatf.utils.mmap.promo_indexer_write_mmap import write_promo_json_to_mmap


@pytest.fixture
def resource():
    promos = [
        {
            'promo_md5': '10204',
            'promo_details': generate_default_blue_3p_promo(
                start_date=10000,
                end_date=20000)
        },
        {
            'msku': '112201',
            'msku_details': generate_default_msku(
                market_promo_price=150,
                market_old_price=1500,
                source_promo_id='10204'
            ),
        },
    ]
    json_path = yatest.common.output_path('yt_promo_details.json')
    return PromoDetails(write_promo_json_to_mmap, json_path, promos)


def test_promo_details(resource):
    resource.dump(os.path.join(yatest.common.test_output_path(), 'promo.mmap'))
    assert os.path.exists(resource.path)
