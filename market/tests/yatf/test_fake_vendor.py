# coding: utf-8

"""
The test checks that the vendor_id parameter is not filled
for fake vendors (it should be filled only for real ones).
"""

import uuid
import pytest
from hamcrest import assert_that

from yt.wrapper.ypath import ypath_join
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.export.awaps.yatf.resources.global_vendors_xml import GlobalVendorsXml
from market.idx.export.awaps.yatf.test_envs.awaps_offers import YtAwapsOffersTestEnv

from market.idx.export.awaps.yatf.matchers.env_matchers import HasAwapsOffer


offer_real_vendor = [
    {
        'shop_id': 0,
        'offer_id': '1',
        'model_id': 101,
        'vendor_id': 1,
    }
]

offer_fake_vendor = [
    {
        'shop_id': 0,
        'offer_id': '2',
        'model_id': 102,
        'vendor_id': 2,
    }
]


@pytest.fixture(scope="module")
def global_vendors():
    return '''
        <global-vendors>
          <vendor id="1" name="name1">
            <site>site</site>
            <picture>picture</picture>
          </vendor>
          <vendor id="2" name="yandex">
            <is-fake-vendor>true</is-fake-vendor>
          </vendor>
        </global-vendors>
    '''


@pytest.fixture(scope='module')
def genlog_table(yt_stuff):
    schema = [
        dict(name="shop_id", type="uint64"),
        dict(name="feed_id", type="uint64"),
        dict(name="offer_id", type="string"),
        dict(name="url", type="string"),
        dict(name="title", type="string"),
        dict(name="model_id", type="uint64"),
        dict(name="cluster_id", type="uint64"),
        dict(name="category_id", type="uint64"),
        dict(name="regions", type="string"),
        dict(name="priority_regions", type="string"),
        dict(name="geo_regions", type="string"),
        dict(name="vendor_id", type="uint64"),
        dict(name="price", type="string"),
        dict(name="pictures", type="string"),
        dict(name='flags', type='uint64'),
    ]

    tablepath = ypath_join('//home', str(uuid.uuid4()), 'in', 'offers')
    table = YtTableResource(yt_stuff, tablepath, offer_real_vendor + offer_fake_vendor, attributes={'schema': schema})
    table.dump()
    return table


@pytest.yield_fixture(scope='module')
def awaps_offers_workflow(yt_stuff, genlog_table, global_vendors):
    resources = {
        'global_vendors_xml': GlobalVendorsXml.from_str(global_vendors)
    }
    with YtAwapsOffersTestEnv(use_op=False, **resources) as env:
        output_table = ypath_join('//home', str(uuid.uuid4()), 'out', 'awaps', 'offers')
        env.execute(yt_stuff, output_table=output_table, input_table=genlog_table.get_path())
        env.verify()
        yield env


def _gen_test_offer(offer, include_vendor_id=True):
    awaps_offer = {}

    copied_attr = ['model_id', 'vendor_id']
    for attr in copied_attr:
        if attr == 'vendor_id' and not include_vendor_id:
            continue
        awaps_offer[attr] = offer.get(attr)

    return {
        'offer': awaps_offer,
        'shop_id': offer.get('shop_id'),
        'model_id': offer.get('model_id'),
        'vendor_id': offer.get('vendor_id') if include_vendor_id else None
    }


@pytest.mark.parametrize('offer', offer_real_vendor)
def test_real_vendor(awaps_offers_workflow, offer):
    assert_that(
        awaps_offers_workflow,
        HasAwapsOffer(_gen_test_offer(offer)),
        'Awaps offer contains vendor_id'
    )


@pytest.mark.parametrize('offer', offer_fake_vendor)
def test_fake_vendor(awaps_offers_workflow, offer):
    assert_that(
        awaps_offers_workflow,
        HasAwapsOffer(_gen_test_offer(offer, include_vendor_id=False)),
        "Awaps offer doesn't contain vendor_id"
    )
