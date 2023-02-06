# coding: utf-8
import uuid
import pytest
from hamcrest import assert_that, is_not

from yt.wrapper.ypath import ypath_join

from market.idx.pylibrary.offer_flags.flags import OfferFlags

from market.idx.export.awaps.yatf.matchers.env_matchers import HasAwapsOffer
from market.idx.export.awaps.yatf.test_envs.awaps_offers import YtAwapsOffersTestEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource


filtered_data = [
    # Lavka offer, should be filtered out
    {
        'shop_id': 11,
        'feed_id': 12,
        'offer_id': '13',
        'url': 'url',
        'title': 'title',
        'model_id': 14,
        'cluster_id': 15,
        'category_id': 16,
        'regions': 'regions',
        'priority_regions': 'priority regions',
        'geo_regions': 'geo regions',
        'vendor_id': 17,
        'pictures': '[{"group_id":399640,"id":"_m8-jSZj07Kvgnj9qvGOYA","thumb_mask":4611686018427650047,"width":200,"height":166}]',  # JSON
        'flags': OfferFlags.IS_LAVKA
    },
    # Eda offer, should be filtered out
    {
        'shop_id': 21,
        'feed_id': 22,
        'offer_id': '23',
        'url': 'url',
        'title': 'title',
        'model_id': 24,
        'cluster_id': 25,
        'category_id': 26,
        'regions': 'regions',
        'priority_regions': 'priority regions',
        'geo_regions': 'geo regions',
        'vendor_id': 27,
        'pictures': '[{"group_id":399640,"id":"_m8-jSZj07Kvgnj9qvGOYA","thumb_mask":4611686018427650047,"width":200,"height":166}]',  # JSON
        'flags': OfferFlags.IS_EDA
    },
    # Direct offer, should be filtered out
    {
        'shop_id': 31,
        'feed_id': 32,
        'offer_id': '33',
        'url': 'url',
        'title': 'title',
        'model_id': 34,
        'cluster_id': 35,
        'category_id': 36,
        'regions': 'regions',
        'priority_regions': 'priority regions',
        'geo_regions': 'geo regions',
        'vendor_id': 37,
        'pictures': '[{"group_id":399640,"id":"_m8-jSZj07Kvgnj9qvGOYA","thumb_mask":4611686018427650047,"width":200,"height":166}]',  # JSON
        'flags': OfferFlags.IS_DIRECT | OfferFlags.OFFER_HAS_GONE
    },
]


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

    table = YtTableResource(yt_stuff, tablepath, filtered_data, attributes={'schema': schema})
    table.dump()

    return table


@pytest.yield_fixture(scope='module')
def awaps_offers_workflow(yt_stuff, genlog_table):
    resources = {}

    with YtAwapsOffersTestEnv(use_op=False, **resources) as env:
        output_table = ypath_join('//home', str(uuid.uuid4()), 'out', 'awaps', 'offers')
        env.execute(yt_stuff, output_table=output_table, input_table=genlog_table.get_path())
        env.verify()
        yield env


def _gen_test_offer(offer):
    awaps_offer = {}

    copied_attr = ['feed_id', 'url', 'title', 'model_id', 'cluster_id', 'regions', 'priority_regions',
                   'geo_regions', 'vendor_id']

    for attr in copied_attr:
        awaps_offer[attr] = offer.get(attr)

    awaps_offer['shop_offer_id'] = offer.get('offer_id')

    return {
        'shop_id': offer.get('shop_id'),
        'offer': awaps_offer,
        'vendor_id': offer.get('vendor_id'),
        'model_id': offer.get('model_id'),
    }


@pytest.mark.parametrize('offer', filtered_data)
def test_filter_ecom_offers(awaps_offers_workflow, offer):
    """
    Тест на проверку фильтрации e-com офферов
    """
    assert_that(
        awaps_offers_workflow,
        is_not(HasAwapsOffer(
            _gen_test_offer(offer)
        ))
    )
