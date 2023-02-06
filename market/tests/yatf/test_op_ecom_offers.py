# coding: utf-8
import uuid
import pytest
import json
import yt.yson as yson
from hamcrest import assert_that, is_not

from yt.wrapper.ypath import ypath_join, ypath_split

from market.idx.pylibrary.offer_flags.flags import OfferFlags

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.export.awaps.yatf.test_envs.awaps_offers import YtAwapsOffersTestEnv

from market.idx.export.awaps.yatf.matchers.env_matchers import HasAwapsOffer

from market.proto.indexer.GenerationLog_pb2 import Record as GLRecord
from google.protobuf.json_format import Parse as MessageFromJson


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
        'int_regions' : [yson.YsonUint64(0), yson.YsonUint64(1)],
        'priority_regions': 'priority regions',
        'int_geo_regions': [yson.YsonUint64(0), yson.YsonUint64(1)],
        'vendor_id': 17,
        'pictures': '[{"group_id":399640,"id":"_m8-jSZj07Kvgnj9qvGOYA","thumb_mask":4611686018427650047,"width":200,"height":166}]',  # JSON
        'flags': OfferFlags.IS_LAVKA.value
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
        'int_regions' : [yson.YsonUint64(0), yson.YsonUint64(1)],
        'priority_regions': 'priority regions',
        'int_geo_regions': [yson.YsonUint64(0), yson.YsonUint64(1)],
        'vendor_id': 27,
        'pictures': '[{"group_id":399640,"id":"_m8-jSZj07Kvgnj9qvGOYA","thumb_mask":4611686018427650047,"width":200,"height":166}]',  # JSON
        'flags': OfferFlags.IS_EDA.value
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
        'int_regions' : [yson.YsonUint64(0), yson.YsonUint64(1)],
        'priority_regions': 'priority regions',
        'int_geo_regions': [yson.YsonUint64(0), yson.YsonUint64(1)],
        'vendor_id': 37,
        'pictures': '[{"group_id":399640,"id":"_m8-jSZj07Kvgnj9qvGOYA","thumb_mask":4611686018427650047,"width":200,"height":166}]',  # JSON
        'flags': OfferFlags.IS_DIRECT.value
    },
]


def dict_to_genlog(data):
    data['pictures'] = []
    res = MessageFromJson(json.dumps(data), GLRecord())
    return res.SerializeToString()


@pytest.fixture(scope='module')
def genlogs():
    return [dict_to_genlog(d) for d in filtered_data]


def add_mbo(r):
    r['numeric_params'] = []
    r['category_color_filters'] = []
    return r


@pytest.fixture(scope='module')
def genlog_table(yt_stuff, genlogs):
    schema = [
        dict(name='shop_id', type='uint32'),
        dict(name='feed_id', type='uint32'),
        dict(name='offer_id', type='string'),
        dict(name='url', type='string'),
        dict(name='title', type='string'),
        dict(name='model_id', type='uint32'),
        dict(name='cluster_id', type='uint32'),
        dict(name='category_id', type='uint32'),
        dict(name='priority_regions', type='string'),
        dict(name='int_geo_regions', type='any', type_v3=dict(
            type_name='list',
            item='uint64'
        )),
        dict(name='int_regions', type='any', type_v3=dict(
            type_name='list',
            item='uint64'
        )),
        dict(name='vendor_id', type='uint64'),
        dict(name='pictures', type='any', type_v3=dict(
            type_name='list',
            item=dict(
                type_name='struct', members=[
                    dict(name='group_id', type='uint32'),
                    dict(name='id', type='string'),
                    dict(name='thumb_mask', type='uint64'),
                    dict(name='width', type='uint32'),
                    dict(name='height', type='uint32')
                ]
            )
        )),
        dict(name='red_status', type='uint32'),
        dict(name='flags', type='uint64'),

        dict(name='numeric_params', type='any', type_v3=dict(
            type_name='list',
            item=dict(
                type_name='struct', members=[
                    dict(name='id', type='uint64'),
                    dict(name='precision', type='uint32'),
                    dict(name='ranges', type=dict(
                        type_name='list',
                        item='double'
                    )),
                ]
            )
        )),
        dict(name='category_color_filters', type='any', type_v3=dict(
            type_name='list',
            item=dict(
                type_name='struct', members=[
                    dict(name='param_id', type='uint64'),
                    dict(name='count', type='uint32'),
                    dict(name='id_sizes', type=dict(
                        type_name='list',
                        item=dict(
                            type_name='struct', members=[
                                dict(name='id', type='uint64'),
                                dict(name='size', type='uint64'),
                            ]
                        )
                    )),
                ]
            )
        )),
    ]

    tablepath = ypath_join('//home', str(uuid.uuid4()), 'genlog', '0000')
    table = YtTableResource(
        yt_stuff,
        tablepath,
        [add_mbo(d) for d in filtered_data],
        attributes={'schema': schema}
    )
    table.dump()
    return table


@pytest.yield_fixture(scope='module')
def awaps_offers_workflow(yt_stuff, genlog_table):
    resources = {}

    with YtAwapsOffersTestEnv(use_op=True, **resources) as env:
        output_table = ypath_join('//home', str(uuid.uuid4()), 'out', 'awaps', 'offers')
        env.execute(yt_stuff, output_table=output_table, input_table=ypath_split(genlog_table.get_path())[0])
        env.verify()
        yield env


def _gen_test_offer(offer):
    awaps_offer = {}

    copied_attr = ['feed_id', 'url', 'title', 'model_id', 'cluster_id', 'int_regions', 'priority_regions',
                   'int_geo_regions', 'vendor_id']

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
        )),
    )
