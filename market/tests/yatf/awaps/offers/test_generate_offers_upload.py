# coding: utf-8
import pytest
import uuid

from hamcrest import assert_that

from yt.wrapper.ypath import ypath_join
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.export.awaps.yatf.test_envs.awaps_offers import YtAwapsOffersTestEnv

from market.idx.pylibrary.s3_awaps.yatf.matchers.s3_awaps_matchers import S3AwapsShopVendorsMatcher
from market.idx.pylibrary.s3_awaps.yatf.test_envs.s3_awaps_uploader_env import S3AwapsOffersUploaderTestEnv
from market.idx.pylibrary.s3_awaps.yatf.utils.s3_awaps_uploader import create_s3_awaps_test_client

from market.pylibrary.s3.yatf.matchers.s3_matchers import ExistFilesInS3Bucket
from market.pylibrary.s3.yatf.utils.s3_client import create_s3_test_client
from market.pylibrary.s3.s3.s3_api import clean_bucket

from market.idx.tests.yatf.awaps.matchers.awaps_matchers import HasCountOfAwapsOffer

BUCKET_NAME = 'awaps-uploader-test'
GENERATION = '20180101_0101'
WORKERS_COUNT = 2


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
        'priority_regions': 'I am priority regions',
        'geo_regions': 'I am geo regions',
        'vendor_id': 7,
        'pictures': '[{"group_id":805400,"id":"9OsYsTTP9aMcEMGzia1JXw","thumb_mask":4611686018427650047,"width":590,"height":700}]'  # JSON
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
        'priority_regions': 'I am priority regions',
        'geo_regions': 'I am geo regions',
        'vendor_id': 8,
        'pictures': '[{"group_id":406026,"id":"T9ctP9uAbMdZm5Ys8ZJb8A","thumb_mask":4611686018427650047,"width":280,"height":109}]'  # JSON
    },
    {
        'shop_id': 11,
        'feed_id': 3,
        'offer_id': '4',
        'url': 'I am url',
        'title': 'I am title',
        'model_id': 4,
        'cluster_id': 5,
        'category_id': 6,
        'regions': 'I am regions',
        'priority_regions': 'I am priority regions',
        'geo_regions': 'I am geo regions',
        'vendor_id': 9,
        'pictures': '[{"group_id":399640,"id":"_m8-jSZj07Kvgnj9qvGOYA","thumb_mask":4611686018427650047,"width":200,"height":166}]'  # JSON
    },
]


@pytest.yield_fixture(scope="module")
def s3_client():
    client = create_s3_test_client()

    clean_bucket(client, BUCKET_NAME)
    yield client
    clean_bucket(client, BUCKET_NAME)


@pytest.yield_fixture(scope="module")
def s3_awaps_uploader():
    up = create_s3_awaps_test_client(BUCKET_NAME)
    yield up


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
    ]

    tablepath = ypath_join('//home', str(uuid.uuid4()), 'in', 'offers')

    table = YtTableResource(yt_stuff, tablepath, data, attributes={'schema': schema})
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


@pytest.yield_fixture(scope='module')
def s3_awaps_uploader_workflow(awaps_offers_workflow, s3_client, s3_awaps_uploader, yt_stuff):
    resources = {
        's3_client': s3_client,
        's3_bucket_name': BUCKET_NAME,
        's3_awaps_uploader': s3_awaps_uploader
    }

    input_table = awaps_offers_workflow.result_table.get_path()

    with S3AwapsOffersUploaderTestEnv(**resources) as env:
        env.execute(yt_stuff, GENERATION, input_table, WORKERS_COUNT)
        env.verify()
        yield env


def _get_s3_path(filename):
    return 'awaps/{0}/{1}'.format(GENERATION, filename)


def test_intgr_awaps_offers_to_s3(s3_awaps_uploader_workflow):
    """
    Тест проверяет, что авапс оффера были сгенерины по генлогам и выгрзука в s3 сработала и сгенерила соответствующее количество файлов.
    Все оффера разбиваются на равные части(кроме, возможно, для последнего воркера), затем каждый воркер обрабатывает
    свою часть офферов.
    files - файл со списком всех файлов
    offers-<shop_id>-<worker_id>-0-0 - для каждого магазина создается файл, со списком awaps.Offers
    shop-vendors-<worker_id> - каждый воркер создает файл с набором awaps.ShopVendors(списком вендеров для каждого магазина)
    """
    assert_that(
        s3_awaps_uploader_workflow,
        ExistFilesInS3Bucket([
            _get_s3_path('files'),
            _get_s3_path('offers-1-0-0-0'),
            _get_s3_path('offers-11-1-0-0'),
            _get_s3_path('shop-vendors-0'),
            _get_s3_path('shop-vendors-1'),
        ]),
        u'Проверка наличия выходных файлов'
    )


def test_intgr_awaps_offers_to_s3_count(s3_awaps_uploader_workflow):
    """
    Тест проверяет, что все оффера, что пришли на вход генератору авапс офферов, попали в бакет
    """
    assert_that(
        s3_awaps_uploader_workflow,
        HasCountOfAwapsOffer(len(data)),
        u'Проверка колличества офферов'
    )


def test_intgr_awaps_to_s3_shop_vendors_0(s3_awaps_uploader_workflow):
    """
    Тест проверяет корректность генерации shop-vendors-0
    Нулевому воркеру достался только 1 оффер с shop_id == 1
    """
    path = _get_s3_path('shop-vendors-0')

    assert_that(
        s3_awaps_uploader_workflow,
        S3AwapsShopVendorsMatcher(path, [
            {
                'shop_id': 1,
                'vendors': [7],
            },
        ]),
        u'Проверка корректности выгрзки shop-vendors-0'
    )


def test_intgr_awaps_to_s3_shop_vendors_1(s3_awaps_uploader_workflow):
    """
    Тест проверяет корректность генерации shop-vendors-1
    Первому воркеру досталось 2 последних офера с одинаковыми shop_id == 11, вендера двух офферов должны попасть в один awaps.ShopVendors
    """
    path = _get_s3_path('shop-vendors-1')

    assert_that(
        s3_awaps_uploader_workflow,
        S3AwapsShopVendorsMatcher(path, [
            {
                'shop_id': 11,
                'vendors': [8, 9],
            },
        ]),
        u'Проверка корректности выгрзки shop-vendors-1'
    )
