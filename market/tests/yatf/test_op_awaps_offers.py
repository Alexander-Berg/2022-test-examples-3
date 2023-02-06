# coding: utf-8
import yt.yson as yson
import uuid
import pytest
import json
from hamcrest import assert_that, is_not

from yt.wrapper.ypath import ypath_join, ypath_split
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.export.awaps.yatf.test_envs.awaps_offers import YtAwapsOffersTestEnv

from market.idx.export.awaps.yatf.matchers.env_matchers import HasAwapsOffer, IsOutputTableSortedBy, IsPicturesEqual

from market.proto.indexer.GenerationLog_pb2 import Record as GLRecord
from google.protobuf.json_format import Parse as MessageFromJson


CUT_PRICE_FLAG = 1 << 25

data = [
    {
        'shop_id': 1,
        'feed_id': 2,
        'offer_id': '2',
        'url': 'I am url',
        'title': 'I am title',
        'model_id': 4,
        'cluster_id': 5,
        'category_id': 6,
        'int_regions' : [yson.YsonUint64(0), yson.YsonUint64(1)],
        'priority_regions': 'I am priority regions',
        'int_geo_regions': [yson.YsonUint64(0), yson.YsonUint64(1)],
        'vendor_id': 7,
        'picture_crcs': ['thisiscrc1'],
        'pictures': [{
            "group_id": yson.YsonUint64(805400),
            "thumb_mask": yson.YsonUint64(4611686018427650047),
            "width": yson.YsonUint64(64590),
            "height": yson.YsonUint64(64700)
        }]  # JSON
    },
    {
        'shop_id': 11,
        'feed_id': 2,
        'offer_id': '3',
        'url': 'I am url',
        'title': 'I am title',
        'model_id': 4,
        'cluster_id': 5,
        'category_id': 6,
        'int_regions' : [yson.YsonUint64(0), yson.YsonUint64(1)],
        'priority_regions': 'I am priority regions',
        'int_geo_regions': [yson.YsonUint64(0), yson.YsonUint64(1)],
        'vendor_id': 8,
        'picture_crcs': ['thisiscrc2'],
        'pictures': [{
            "group_id": yson.YsonUint64(406026),
            "thumb_mask": yson.YsonUint64(4611686018427650047),
            "width": yson.YsonUint64(280),
            "height": yson.YsonUint64(109)
        }]  # JSON
    },
    {
        'shop_id': 1,
        'feed_id': 3,
        'offer_id': '4',
        'url': 'I am url',
        'title': 'I am title',
        'model_id': 4,
        'cluster_id': 5,
        'category_id': 6,
        'int_regions' : [yson.YsonUint64(0), yson.YsonUint64(1)],
        'priority_regions': 'I am priority regions',
        'int_geo_regions': [yson.YsonUint64(0), yson.YsonUint64(1)],
        'vendor_id': 9,
        'picture_crcs': ['thisiscrc3'],
        'pictures': [{
            "group_id": yson.YsonUint64(399640),
            "thumb_mask": yson.YsonUint64(4611686018427650047),
            "width": yson.YsonUint64(200),
            "height": yson.YsonUint64(166)
        }]  # JSON
    },
]


filtered_data = [
    # cut price offer, should be filtered
    {
        'shop_id': 1,
        'feed_id': 4,
        'offer_id': '7',
        'url': 'cutpriceurl',
        'title': 'cutpricetitle',
        'model_id': 4,
        'cluster_id': 5,
        'category_id': 6,
        'int_regions' : [yson.YsonUint64(0), yson.YsonUint64(1)],
        'priority_regions': 'I am priority regions',
        'int_geo_regions': [yson.YsonUint64(0), yson.YsonUint64(1)],
        'vendor_id': 9,
        'picture_crcs': ['thisiscrc4'],
        'pictures': [{
            "group_id": yson.YsonUint64(399640),
            "thumb_mask": yson.YsonUint64(4611686018427650047),
            "width": yson.YsonUint64(200),
            "height": yson.YsonUint64(166)
        }],  # JSON
        'flags': CUT_PRICE_FLAG,
    },
]


def dict_to_genlog(data):
    res = MessageFromJson(json.dumps(data), GLRecord())
    return res.SerializeToString()


def regions_to_string(regions):
    return ' '.join(map(str, regions))


@pytest.fixture(scope='module')
def genlogs():
    return [dict_to_genlog(d) for d in filtered_data + data]


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
        dict(name='int_regions', type='any', type_v3=dict(
            type_name='list',
            item='uint64'
        )),
        dict(name='priority_regions', type='string'),
        dict(name='int_geo_regions', type='any', type_v3=dict(
            type_name='list',
            item='uint64'
        )),
        dict(name='vendor_id', type='uint64'),
        dict(name='pictures', type='any', type_v3=dict(
            type_name='list',
            item=dict(
                type_name='struct', members=[
                    dict(name='group_id', type='uint32'),
                    dict(name='thumb_mask', type='uint64'),
                    dict(name='width', type='uint32'),
                    dict(name='height', type='uint32')
                ]
            )
        )),
        dict(name='picture_crcs', type='any', type_v3=dict(
            type_name='list',
            item='string'
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
        [add_mbo(d) for d in filtered_data + data],
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

    copied_attr = ['feed_id', 'url', 'title', 'model_id', 'cluster_id', 'priority_regions', 'vendor_id']

    for attr in copied_attr:
        awaps_offer[attr] = offer.get(attr)

    awaps_offer['shop_offer_id'] = offer.get('offer_id')

    return {
        'shop_id': offer.get('shop_id'),
        'offer': awaps_offer,
        'vendor_id': offer.get('vendor_id'),
        'model_id': offer.get('model_id'),
    }


@pytest.mark.parametrize('offer', data)
def test_generate_awaps_offers(awaps_offers_workflow, offer):
    """
    Тест на проверку генерации авапс офферов
    Проверят поля, которые просто копируются из генлог таблицы в соответствующие поля в протобуфе
    """
    assert_that(
        awaps_offers_workflow,
        HasAwapsOffer(
            _gen_test_offer(offer)
        ),
        u'Awaps оффера должны быть корректно созданы'
    )


@pytest.mark.parametrize('offer', filtered_data)
def test_filter_awaps_offers(awaps_offers_workflow, offer):
    """
    Тест на проверку фильтрацию авапс офферов, которые не соответствуют условиям
    """
    assert_that(
        awaps_offers_workflow,
        is_not(HasAwapsOffer(
            _gen_test_offer(offer)
        )),
        u'Awaps оффера должны быть корректно отфильтрованны'
    )


def test_sorted_awaps_offers(awaps_offers_workflow):
    """
    Тест на проверку того, что выходная таблица должна быть отсортирована
    нужно для того, чтобы эту таблицу выгрузить в s3, сгрупировав офера по shop_id
    """
    assert_that(
        awaps_offers_workflow,
        IsOutputTableSortedBy('shop_id'),
        u'Выходная таблица должна быть отсортирована по shop_id'
    )


@pytest.mark.parametrize('offer', data)
def test_pictures_awaps_offer(awaps_offers_workflow, offer):
    """
    Тест на проверки корректности обработки json'а с картинками
    """
    pic_json = offer['pictures']
    pic_crcs = offer['picture_crcs']
    for i in range(len(pic_crcs)):
        pic_json[i]['crc'] = pic_crcs[i]
    assert_that(
        awaps_offers_workflow,
        IsPicturesEqual(offer['offer_id'], pic_json),
        u'Набор картинок должен совпадать'
    )
