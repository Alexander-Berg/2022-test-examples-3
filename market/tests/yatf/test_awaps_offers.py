# coding: utf-8
import uuid
import pytest
import json
from hamcrest import assert_that, is_not
import yt.yson as yson

from yt.wrapper.ypath import ypath_join
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.export.awaps.yatf.test_envs.awaps_offers import YtAwapsOffersTestEnv

from market.idx.export.awaps.yatf.matchers.env_matchers import HasAwapsOffer, IsOutputTableSortedBy, IsPicturesEqual


CUT_PRICE_FLAG = 1 << 25
CPA_FLAG = 1 << 24

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
        'regions': 'I am regions',
        'int_regions' : [yson.YsonUint64(0), yson.YsonUint64(1)],
        'priority_regions': 'I am priority regions',
        'geo_regions': 'I am geo regions',
        'int_geo_regions': [yson.YsonUint64(0), yson.YsonUint64(1)],
        'vendor_id': 7,
        'pictures': '[{"group_id":805400,"id":"9OsYsTTP9aMcEMGzia1JXw","thumb_mask":4611686018427650047,"width":590,"height":700}]',  # JSON
        'picture_crcs': '["crcfromimage1"]',
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
        'regions': 'I am regions',
        'int_regions' : [yson.YsonUint64(0), yson.YsonUint64(1)],
        'priority_regions': 'I am priority regions',
        'geo_regions': 'I am geo regions',
        'int_geo_regions': [yson.YsonUint64(0), yson.YsonUint64(1)],
        'vendor_id': 8,
        'pictures': '[{"group_id":406026,"id":"T9ctP9uAbMdZm5Ys8ZJb8A","thumb_mask":4611686018427650047,"width":280,"height":109}]',  # JSON
        'flags': CPA_FLAG,
        'picture_crcs': '["crcfromimage2"]',
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
        'regions': 'I am regions',
        'int_regions' : [yson.YsonUint64(0), yson.YsonUint64(1)],
        'priority_regions': 'I am priority regions',
        'geo_regions': 'I am geo regions',
        'int_geo_regions': [yson.YsonUint64(0), yson.YsonUint64(1)],
        'vendor_id': 9,
        'pictures': '[{"group_id":399640,"id":"_m8-jSZj07Kvgnj9qvGOYA","thumb_mask":4611686018427650047,"width":200,"height":166}]',  # JSON
        'picture_crcs': '["crcfromimage1"]',
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
        'regions': 'I am regions',
        'int_regions' : [yson.YsonUint64(0), yson.YsonUint64(1)],
        'priority_regions': 'I am priority regions',
        'geo_regions': 'I am geo regions',
        'int_geo_regions': [yson.YsonUint64(0), yson.YsonUint64(1)],
        'vendor_id': 9,
        'pictures': '[{"group_id":399640,"id":"_m8-jSZj07Kvgnj9qvGOYA","thumb_mask":4611686018427650047,"width":200,"height":166}]',  # JSON
        'flags': CUT_PRICE_FLAG
    },
]


def regions_to_string(regions):
    return ' '.join(map(str, regions))


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
        dict(name='int_regions', type='any', type_v3=dict(
            type_name='list',
            item='uint64'
        )),
        dict(name="priority_regions", type="string"),
        dict(name="geo_regions", type="string"),
        dict(name='int_geo_regions', type='any', type_v3=dict(
            type_name='list',
            item='uint64'
        )),
        dict(name="vendor_id", type="uint64"),
        dict(name="price", type="string"),
        dict(name="pictures", type="string"),
        dict(name="picture_crcs", type="string"),
        dict(name='flags', type='uint64'),
    ]

    tablepath = ypath_join('//home', str(uuid.uuid4()), 'in', 'offers')

    table = YtTableResource(yt_stuff, tablepath, data + filtered_data, attributes={'schema': schema})
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

    copied_attr = ['feed_id', 'url', 'title', 'model_id', 'cluster_id', 'priority_regions', 'vendor_id']

    for attr in copied_attr:
        awaps_offer[attr] = offer.get(attr)
    awaps_offer['regions'] = regions_to_string(offer.get('int_regions'))
    awaps_offer['geo_regions'] = regions_to_string(offer.get('int_geo_regions'))

    awaps_offer['shop_offer_id'] = offer.get('offer_id')
    awaps_offer['is_cpa'] = (offer.get('flags', 0) & CPA_FLAG) != 0

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
    pic_json = json.loads(offer['pictures'])
    pic_crcs = json.loads(offer['picture_crcs'])
    assert(len(pic_json) >= len(pic_crcs))
    for i in range(len(pic_crcs)):
        pic_json[i]['crc'] = pic_crcs[i]
    assert_that(
        awaps_offers_workflow,
        IsPicturesEqual(offer['offer_id'], pic_json),
        u'Набор картинок должен совпадать'
    )
