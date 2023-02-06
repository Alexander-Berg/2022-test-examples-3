# coding: utf-8
import uuid
import pytest
from hamcrest import assert_that

from yt.wrapper.ypath import ypath_join
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from market.idx.pylibrary.s3_awaps.yatf.test_envs.s3_awaps_uploader_env import S3AwapsCategoriesUploaderTestEnv
from market.idx.pylibrary.s3_awaps.yatf.matchers.s3_awaps_matchers import S3AwapsCategoriesMatcher
from market.idx.pylibrary.s3_awaps.yatf.utils.s3_awaps_uploader import s3_client, s3_awaps_uploader, BUCKET_NAME

from market.pylibrary.s3.yatf.matchers.s3_matchers import ExistFilesInS3Bucket, S3ContentMatcher

assert s3_awaps_uploader
assert s3_client


GENERATION = '20180101_0101'


categories_info = [
    {
        'hyper_id': 90401,
        'name': 'Все товары',
        'uniq_name': 'Все товары',
        'parent': 0,
        'no_search': True,
        'visual': False,
    },
    {
        'hyper_id': 198118,
        'name': 'Бытовая техника',
        'uniq_name': 'Бытовая техника',
        'parent': 90401,
        'no_search': False,
        'visual': False,
    },
    {
        'hyper_id': 14879737,
        'name': 'Носки',
        'uniq_name': 'Носки юник',
        'parent': 90401,
        'no_search': False,
        'visual': True,
    },
]


@pytest.fixture(scope="module")
def categories_table(yt_stuff):
    schema = [
        dict(name="hyper_id", type="int64"),
        dict(name="name", type="string"),
        dict(name="uniq_name", type="string"),
        dict(name="parent", type="int64"),
        dict(name="no_search", type="boolean"),
        dict(name="visual", type="boolean"),
    ]
    tablepath = ypath_join('//home', str(uuid.uuid4()), 'in', 'categories')

    table = YtTableResource(yt_stuff, tablepath, categories_info, attributes={'schema': schema})
    table.dump()

    return table


@pytest.yield_fixture(scope="module")
def s3_awaps_categories_workflow(yt_stuff, s3_client, s3_awaps_uploader, categories_table):
    resources = {
        's3_client': s3_client,
        's3_bucket_name': BUCKET_NAME,
        's3_awaps_uploader': s3_awaps_uploader
    }

    with S3AwapsCategoriesUploaderTestEnv(**resources) as env:
        env.execute(yt_stuff, GENERATION, input_table=categories_table.get_path())
        env.verify()
        yield env


def _get_s3_path(filename):
    return 'awaps/{0}/{1}'.format(GENERATION, filename)


def test_upload_awaps_categories_to_s3_files(s3_awaps_categories_workflow):
    """
    Тест проверяет, что файл с информацией о категориях и файл со списком всех выгруженных файлов сгенерился
    """
    assert_that(
        s3_awaps_categories_workflow,
        ExistFilesInS3Bucket([
            _get_s3_path('files'),
            _get_s3_path('categories'),
        ]),
        'Проверка наличия выходного файла для категорий'
    )


def test_upload_awaps_files_content(s3_awaps_categories_workflow):
    """
    Тест проверяет содержимое для файла files
    Должен содержаться только один выгруженный файл - categories
    """
    path = _get_s3_path('files')

    assert_that(
        s3_awaps_categories_workflow,
        S3ContentMatcher(path, 'categories'),
        'Проверка корректности создания файла files со списокм файлов в бакете'
    )


def test_upload_awaps_categories_to_s3_content(s3_awaps_categories_workflow):
    """
    Тест проверяет содержимое файла со списком всех категорий в формате протобуф
    """
    path = _get_s3_path('categories')

    assert_that(
        s3_awaps_categories_workflow,
        S3AwapsCategoriesMatcher(path, categories_info),
        'Проверка корректности выгрузки файла categories'
    )
