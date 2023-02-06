# coding: utf-8
import uuid
import pytest
from hamcrest import assert_that

from market.proto.indexer import awaps_pb2

from yt.wrapper.ypath import ypath_join
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from market.idx.pylibrary.s3_awaps.yatf.test_envs.s3_awaps_uploader_env import S3AwapsOffersUploaderTestEnv
from market.idx.pylibrary.s3_awaps.yatf.matchers.s3_awaps_matchers import S3AwapsOffersMatcher, S3AwapsShopVendorsMatcher
from market.idx.pylibrary.s3_awaps.yatf.utils.s3_awaps_uploader import s3_client, s3_awaps_uploader, BUCKET_NAME

from market.pylibrary.s3.yatf.matchers.s3_matchers import ExistFilesInS3Bucket, S3ContentMatcher

assert s3_awaps_uploader
assert s3_client


GENERATION = '20180101_0101'
WORKERS_COUNT = 2


@pytest.fixture(scope="module")
def awaps_offers_data():
    offer = awaps_pb2.Offer()
    offer.title = 'title'
    offer.feed_id = 1

    offer_str = offer.SerializeToString()

    data = [
        {
            'shop_id': 1,
            'offer': offer_str,
            'model_id': 1,
            'vendor_id': 1,
        },
        {
            'shop_id': 1,
            'offer': offer_str,
            'model_id': 2,
            'vendor_id': 2,
        },
        {
            'shop_id': 2,
            'offer': offer_str,
            'model_id': 3,
            'vendor_id': 3,
        },
    ]

    return data


@pytest.fixture(scope="module")
def awaps_offers_table(yt_stuff, awaps_offers_data):
    tablepath = ypath_join('//home', str(uuid.uuid4()), 'in', 'offers')
    table = YtTableResource(yt_stuff, tablepath, awaps_offers_data)
    table.dump()

    return table


@pytest.fixture(scope="module")
def s3_awaps_upload_workflow(yt_stuff, s3_client, s3_awaps_uploader, awaps_offers_table):
    resources = {
        's3_client': s3_client,
        's3_bucket_name': BUCKET_NAME,
        's3_awaps_uploader': s3_awaps_uploader
    }

    with S3AwapsOffersUploaderTestEnv(**resources) as env:
        env.execute(yt_stuff, GENERATION, awaps_offers_table.get_path(), WORKERS_COUNT)
        env.verify()
        yield env


def _get_s3_path(filename):
    return 'awaps/{0}/{1}'.format(GENERATION, filename)


def test_upload_awaps_offers_to_s3_files(s3_awaps_upload_workflow):
    """
    Тест проверяет, что выгрузка в авапс в s3 сработала и сгенерила соответствующее количество файлов.
    Все оффера разбиваются на равные части(кроме, возможно, для последнего воркера), затем каждый воркер обрабатывает
    свою часть офферов.
    files - файл со списком всех файлов
    offers-<shop_id>-<worker_id>-0-0 - для каждого магазина создается файл, со списком awaps.Offers
    shop-vendors-<worker_id> - каждый воркер создает файл с набором awaps.ShopVendors(списком вендеров для каждого магазина)
    """
    assert_that(
        s3_awaps_upload_workflow,
        ExistFilesInS3Bucket([
            _get_s3_path('files'),
            _get_s3_path('offers-1-0-0-0'),
            _get_s3_path('offers-1-1-0-0'),
            _get_s3_path('offers-2-1-0-0'),
            _get_s3_path('shop-vendors-0'),
            _get_s3_path('shop-vendors-1'),
        ]),
        'Проверка наличия выходных файлов'
    )


def test_upload_awaps_files_content(s3_awaps_upload_workflow, awaps_offers_data):
    """
    Тест проверяет содержимое для файла files
    В файле должен быть список всех файлов в бакете: вначале идут файлы для нулевого воркера, потом для первого,
    внутри одного воркера - вначале файл shop-vendors потом подряд список офферов
    """
    path = _get_s3_path('files')

    assert_that(
        s3_awaps_upload_workflow,
        S3ContentMatcher(path, '\n'.join(['shop-vendors-0', 'offers-1-0-0-0', 'shop-vendors-1', 'offers-1-1-0-0', 'offers-2-1-0-0'])),
        'Проверка корректности создания файла files со списокм файлов в бакете'
    )


# check offers content
def test_upload_awaps_offers_to_s3_content_1_0(s3_awaps_upload_workflow, awaps_offers_data):
    """
    Тест проверяет содержимое для файла offers-1-0-0-0
    В файл должен попасть только один оффер, так как нулевому воркеру достался только один оффер
    """
    path = _get_s3_path('offers-1-0-0-0')

    assert_that(
        s3_awaps_upload_workflow,
        S3AwapsOffersMatcher(path, [awaps_offers_data[0]['offer']]),
        'Проверка корректности выгрзки offers-1-0-0-0'
    )


def test_upload_awaps_offers_to_s3_content_1_1(s3_awaps_upload_workflow, awaps_offers_data):
    """
    Тест проверяет содержимое для файла offers-1-1-0-0
    В файл должен попасть один оффер, так как первому оферу досталось один оффер с shop_id == 1
    """
    path = _get_s3_path('offers-1-1-0-0')

    assert_that(
        s3_awaps_upload_workflow,
        S3AwapsOffersMatcher(path, [awaps_offers_data[1]['offer']]),
        'Проверка корректности выгрзки offers-1-0-0-0'
    )


def test_upload_awaps_offers_to_s3_content_2_1(s3_awaps_upload_workflow, awaps_offers_data):
    """
    Тест проверяет содержимое для файла offers-2-1-0-0
    В файл должен попасть один оффер, так как первому оферу досталось один оффер с shop_id == 2
    """
    path = _get_s3_path('offers-2-1-0-0')

    assert_that(
        s3_awaps_upload_workflow,
        S3AwapsOffersMatcher(path, [awaps_offers_data[2]['offer']]),
        'Проверка корректности выгрзки offers-1-0-0-0'
    )


# check shop-vendors content
def test_upload_awaps_shop_vendors_to_s3_content_0(s3_awaps_upload_workflow):
    """
    Тест проверяет содержимое shop-vendors-0
    Файл должен содержать список вендеров только для магазина с shop_id == 1, так как только один оффер достался
    нулевому воркеру
    """
    path = _get_s3_path('shop-vendors-0')

    assert_that(
        s3_awaps_upload_workflow,
        S3AwapsShopVendorsMatcher(path, [{
            'shop_id': 1,
            'vendors': [1],
        }]),
        'Проверка корректности выгрзки shop-vendors-0'
    )


def test_upload_awaps_shop_vendors_to_s3_content_1(s3_awaps_upload_workflow):
    """
    Тест проверяет содержимое shop-vendors-1
    Файл должен содержать список вендеров для магазинов с shop_id == 1 и shop_id == 2, так как два последних оффера
    достались первому воркеру
    """
    path = _get_s3_path('shop-vendors-1')

    assert_that(
        s3_awaps_upload_workflow,
        S3AwapsShopVendorsMatcher(path, [
            {
                'shop_id': 1,
                'vendors': [2],
            },
            {
                'shop_id': 2,
                'vendors': [3],
            },
        ]),
        'Проверка корректности выгрзки shop-vendors-1'
    )
