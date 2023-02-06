# coding: utf-8
import uuid
import pytest
from hamcrest import assert_that

from yt.wrapper.ypath import ypath_join
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from market.idx.pylibrary.s3_awaps.yatf.test_envs.s3_awaps_uploader_env import S3AwapsVendorsUploaderTestEnv
from market.idx.pylibrary.s3_awaps.yatf.matchers.s3_awaps_matchers import S3AwapsVendorsMatcher
from market.idx.pylibrary.s3_awaps.yatf.utils.s3_awaps_uploader import s3_client, s3_awaps_uploader, BUCKET_NAME

from market.pylibrary.s3.yatf.matchers.s3_matchers import ExistFilesInS3Bucket, S3ContentMatcher

assert s3_awaps_uploader
assert s3_client


GENERATION = '20180101_0101'


vendors_info = [
    {
        'id': 434163,
        'name': 'BaByliss',
        'site': 'http://www.babyliss.com',
        'picture': '//avatars.mds.yandex.net/get-mpic/96484/img_id5829911168111513832/orig',
    },
    {
        'id': 488709,
        'name': 'Bambino',
        'site': None,
        'picture': '//avatars.mds.yandex.net/get-mpic/96484/img_id2063147698318191247/orig',
    },
    {
        'id': 1006806,
        'name': 'B.O.N.E.',
        'site': 'http://www.bonesport.com',
        'picture': None,
    },
]

fake_vendors = [
    {
        'id': 99901,
        'name': 'Noname',
        'is-fake-vendor': True
    }
]


@pytest.fixture(scope="module")
def vendors_table(yt_stuff):
    schema = [
        dict(name="id", type="int64"),
        dict(name="name", type="string"),
        dict(name="site", type="string"),
        dict(name="picture", type="string"),
        dict(name="description", type="string"),
        dict(name="is-fake-vendor", type="boolean"),
    ]
    tablepath = ypath_join('//home', str(uuid.uuid4()), 'in', 'vendors')

    table = YtTableResource(yt_stuff, tablepath, vendors_info + fake_vendors, attributes={'schema': schema})
    table.dump()

    return table


@pytest.yield_fixture(scope="module")
def s3_awaps_vendors_workflow(yt_stuff, s3_client, s3_awaps_uploader, vendors_table):
    resources = {
        's3_client': s3_client,
        's3_bucket_name': BUCKET_NAME,
        's3_awaps_uploader': s3_awaps_uploader
    }

    with S3AwapsVendorsUploaderTestEnv(**resources) as env:
        env.execute(yt_stuff, GENERATION, input_table=vendors_table.get_path())
        env.verify()
        yield env


def _get_s3_path(filename):
    return 'awaps/{0}/{1}'.format(GENERATION, filename)


def test_upload_awaps_vendors_to_s3_files(s3_awaps_vendors_workflow):
    """
    Тест проверяет, что файл с информацией о вендерах и файл со списком всех файлов сгенерился
    """
    assert_that(
        s3_awaps_vendors_workflow,
        ExistFilesInS3Bucket([
            _get_s3_path('files'),
            _get_s3_path('vendors'),
        ]),
        'Проверка наличия выходного файла для вендеров'
    )


def test_upload_awaps_files_content(s3_awaps_vendors_workflow):
    """
    Тест проверяет содержимое для файла files
    В файле должен быть только один файл - vendors
    """
    path = _get_s3_path('files')

    assert_that(
        s3_awaps_vendors_workflow,
        S3ContentMatcher(path, 'vendors'),
        'Проверка корректности создания файла files со списокм файлов в бакете'
    )


def test_upload_awaps_vendors_to_s3_content(s3_awaps_vendors_workflow):
    """
    Тест проверяет содержимое файла со списком всех вендеров в формате протобуф
    """
    path = _get_s3_path('vendors')

    assert_that(
        s3_awaps_vendors_workflow,
        S3AwapsVendorsMatcher(path, vendors_info),
        'Проверка корректности выгрзуки файла vendors'
    )
