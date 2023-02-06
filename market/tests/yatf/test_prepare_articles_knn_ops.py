# coding=utf-8

"""Интеграционный тест на постороение saas-knn индекса по статьям. Паплайн:
 - обработка cms-выгрузок (prepare_articles_index)
 - построение saas-knn индекса (knn_ops build)
 - преоразование индекса к виду, пригоному для загрузки в rtyserver (knn_ops publish)
 - для получения результатов - поиск по индексу (knn_ops small_knn)
"""

import json
import pytest
import six

from hamcrest import assert_that, equal_to, has_entries

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.ugc.yatf.test_envs.prepare_articles_index import YtPrepareArticlesIndexTestEnv
from market.idx.ugc.yatf.resources.prepare_articles_index.articles_input_data import (
    DEFAULT_IMAGE_JSON,
    DEFAULT_NIDS,
    DEFAULT_SEMANTIC_ID,
    YTArticlesInputData,
    content_json,
    properties_json,
)

from market.idx.ugc.yatf.resources.knn_ops.knn_ops_options import (
    KnnOpsBuildOptions,
    KnnOpsSmallKnnSearchOptions,
)
from market.idx.ugc.yatf.test_envs.knn_ops import (
    KnnOpsPublishTestEnv,
    KnnOpsSearchTestEnv,
    KnnOpsTestEnv,
    StartupCustomizedLocalYt,
)

from saas.protos.rtyserver_pb2 import TParsedDoc

FAKE_TITLE = "Это не тот тайтл, который используется в индеке"
TYPE = "knowledge"
URL = "https://market.yandex.ru/journal/knowledge/"

DSSM_EMBEDDINGS_SIZE = 50

PAGE_ID_1 = 37520
PAGE_ID_2 = 37521
PAGE_ID_3 = 37522

TITLE1 = 'Как выбрать гамак'
TITLE2 = 'Теплица или парник: что выбрать?'
TITLE3 = 'Чем кормить кота: советы ветеринарного врача'

cms_pages_path = "//home/cms/pages"
cms_compiled_path = "//home/cms/compiled"
prepared_articles_path = "//home/prepared_articles"
knn_ops_build_result_index_prefix = '//home/ugc_index/index'
knn_ops_publish_path = '//home/ugc_index/published'
knn_ops_found_path = '//home/ugc_index/found'


@pytest.fixture(scope='module')
def cms_pages_table(yt_stuff):

    schema = [
        dict(name="page_id", type="int64"),
        dict(name="title", type="string"),
        dict(name="type", type="string"),
        dict(name="url", type="string"),
        dict(name="properties", type="string"),
    ]
    rows = [
        {
            "page_id": PAGE_ID_1, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json()),
            "type": TYPE, "url": URL + str(PAGE_ID_1)
        },
        {
            "page_id": PAGE_ID_2, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json()),
            "type": TYPE, "url": URL + str(PAGE_ID_2)
        },
        {
            "page_id": PAGE_ID_3, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json()),
            "type": TYPE, "url": URL + str(PAGE_ID_3)
        },
    ]
    return YtTableResource(yt_stuff, cms_pages_path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def cms_compiled_table(yt_stuff):
    schema = [
        dict(name="page_id", type="int64"),
        dict(name="device", type="string"),
        dict(name="name", type="string"),
        dict(name="content", type="string"),
    ]
    rows = [
        {
            "page_id": PAGE_ID_1, "device": "desktop", "name": "entrypoints",
            "content": json.dumps(content_json(title=TITLE1)),
        },
        {
            "page_id": PAGE_ID_2, "device": "desktop", "name": "entrypoints",
            "content": json.dumps(content_json(title=TITLE2)),
        },
        {
            "page_id": PAGE_ID_3, "device": "desktop", "name": "entrypoints",
            "content": json.dumps(content_json(title=TITLE3)),
        },
    ]
    return YtTableResource(yt_stuff, cms_compiled_path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def prepare_articles_input_data(cms_pages_table, cms_compiled_table):
    return YTArticlesInputData(cms_pages_table, cms_compiled_table)


@pytest.yield_fixture(scope='module')
def prepare_articles_workflow(yt_stuff, prepare_articles_input_data):
    resources = {
        "input": prepare_articles_input_data,
    }

    with YtPrepareArticlesIndexTestEnv(**resources) as env:
        env.execute(yt_stuff, result_path=prepared_articles_path)
        env.verify()
        yield env


def test_result_table_exists(prepare_articles_workflow, yt_stuff):
    assert_that(
        yt_stuff.get_yt_client().exists(prepare_articles_workflow.result_table.get_path()),
        'Prepared articles table doen\'t exist'
    )


def test_prepared_articles_count(prepare_articles_workflow, yt_stuff, cms_pages_table):
    assert_that(
        len(prepare_articles_workflow.result_data),
        equal_to(len(cms_pages_table.data)),
        'Prepared articles table wrong data len'
    )


@pytest.fixture(scope="module")
def yt_config(request):
    return StartupCustomizedLocalYt(RequiredCpus=7)


@pytest.yield_fixture(scope='module')
def knn_ops_build_options(prepare_articles_workflow):
    return KnnOpsBuildOptions(prepare_articles_workflow.result_table)


@pytest.yield_fixture(scope='module')
def knn_ops_build_workflow(yt_stuff, yt_config, knn_ops_build_options):
    resources = {
        "build_options": knn_ops_build_options,
    }

    with KnnOpsTestEnv(**resources) as env:
        env.execute(yt_stuff, yt_config, result_path=knn_ops_build_result_index_prefix)
        env.verify()
        yield env


def test_knn_ops_build_result_tables(yt_stuff, knn_ops_build_workflow):
    assert_that(
        yt_stuff.get_yt_client().exists(
            knn_ops_build_workflow.index_table.get_path()
        ),
        'Index table doesn\'t exist'
    )
    assert_that(
        yt_stuff.get_yt_client().exists(
            knn_ops_build_workflow.index_meta_table.get_path()
        ),
        'Index meta table doesn\'t exist'
    )
    assert_that(
        yt_stuff.get_yt_client().exists(
            knn_ops_build_workflow.index_meta_build_cachelog_table.get_path()
        ),
        'Index cachelog table doesn\'t exist'
    )


def test_knn_ops_build_meta(yt_stuff, knn_ops_build_workflow, cms_pages_table):
    assert_that(
        len(knn_ops_build_workflow.index_meta_data) >= 1,
        'Knn ops build meta data doesnt exist'
    )
    meta_data = knn_ops_build_workflow.index_meta_data[0]
    assert_that(meta_data['Dim'], equal_to(DSSM_EMBEDDINGS_SIZE))
    assert_that(meta_data['DocsNum'], equal_to(len(cms_pages_table.data)))


@pytest.yield_fixture(scope='module')
def knn_ops_publish_workflow(yt_stuff, knn_ops_build_workflow):
    resources = {}
    with KnnOpsPublishTestEnv(**resources) as env:
        env.execute(
            yt_stuff,
            build_index_path=knn_ops_build_workflow.index_table.get_path(),
            result_path=knn_ops_publish_path
        )
        env.verify()
        yield env


def test_knn_ops_publish_result_tables(yt_stuff, knn_ops_publish_workflow):
    assert_that(
        yt_stuff.get_yt_client().exists(
            knn_ops_publish_workflow.published_table.get_path()
        ),
        'Published table doesn\'t exist'
    )
    assert_that(
        len(yt_stuff.get_yt_client().list(
            knn_ops_publish_workflow.meta_data_dir
        )),
        equal_to(1)
    )


def _get_expected_saas_message(title):
    return {
        'type': TYPE,
        'semantic_id': DEFAULT_SEMANTIC_ID,
        'title': title,
        'image': DEFAULT_IMAGE_JSON,
        'nid': DEFAULT_NIDS,
    }


@pytest.yield_fixture(
    scope='module',
    params=[
        {'page_id': PAGE_ID_1, 'expected_saas_properties': _get_expected_saas_message(TITLE1)},
        {'page_id': PAGE_ID_2, 'expected_saas_properties': _get_expected_saas_message(TITLE2)},
        {'page_id': PAGE_ID_3, 'expected_saas_properties': _get_expected_saas_message(TITLE3)},
    ],
    ids=[
        'test_page1',
        'test_page2',
        'test_page3',
    ]
)
def gen_data(request):
    return request.param


@pytest.yield_fixture()
def knn_ops_query_tablename(gen_data):
    return '//home/ugc_index/query' + str(gen_data['page_id'])


@pytest.yield_fixture()
def knn_ops_query_table(yt_stuff, prepare_articles_workflow, knn_ops_query_tablename, gen_data):
    """В качестве "запроса" в саас-кнн берем нужную строку
    из подготовленной на первом этапе из cms-выгрузок
    таблицы - в ней есть колонка с посчитанными эмбеддингами,
    которые и будут использованы для knn-поиска
    """
    schema = prepare_articles_workflow.result_table.schema

    query_rows = []
    for row in prepare_articles_workflow.result_table.data:
        if row['doc_id'] == gen_data['page_id']:
            query_rows.append(row)
            return YtTableResource(
                yt_stuff,
                knn_ops_query_tablename,
                data=query_rows,
                attributes={'schema': schema}
            )

    raise RuntimeError(
        (
            'Error while generating knn_ops search query: '
            'page_id = {} was not found in prepared articles'
        ).format(
            gen_data['page_id']
        )
    )


@pytest.yield_fixture()
def knn_ops_search_data(knn_ops_query_table):
    return KnnOpsSmallKnnSearchOptions(
        input_query_table=knn_ops_query_table,
        output_result_table_path=knn_ops_found_path,
        result_size=1)


@pytest.yield_fixture()
def knn_ops_search_workflow(
        yt_stuff,
        knn_ops_build_options,
        knn_ops_build_workflow,
        knn_ops_search_data
):
    resources = {
        "build_options": knn_ops_build_options,
        "search_options": knn_ops_search_data,
    }

    with KnnOpsSearchTestEnv(**resources) as env:
        env.execute(yt_stuff, index_path=knn_ops_build_workflow.index_table.get_path())
        env.verify()
        yield env


def _check_image(actual, expected):
    assert_that(json.loads(actual), has_entries(expected))


def _check_nid(actual, expected):
    assert_that(json.loads(actual), equal_to(expected))


def test_saas_knn_articles_result(yt_stuff, knn_ops_search_workflow, gen_data):
    assert_that(
        yt_stuff.get_yt_client().exists(
            knn_ops_search_workflow.search_result_table.get_path()
        ),
        'knn ops search table doesn\'t exist'
    )
    assert_that(
        len(knn_ops_search_workflow.search_result_data),
        equal_to(1),
        'knn ops search table wrong len'
    )

    saas_doc = TParsedDoc()
    saas_doc.ParseFromString(
        knn_ops_search_workflow.search_result_data[0]['RawNeighborIndexData']
    )

    result_saas_props = {}
    for prop in saas_doc.Document.DocumentProperties:
        result_saas_props[prop.Name] = prop.Value

    props_to_check = dict(gen_data['expected_saas_properties'])
    props_to_check.pop("image", "")  # проверяется отдельно
    props_to_check.pop("title", "")  # проверяется отдельно
    props_to_check.pop("nid", "")  # проверяется отдельно

    props_to_check.update({
        "page_id": str(gen_data['page_id']),
        "pages_generation": "test-pages",
        "compiled_generation": "test-compiled",
    })

    assert_that(result_saas_props, has_entries(props_to_check), 'Wrong saas message')
    assert_that(
        six.ensure_text(result_saas_props['title']),
        equal_to(
            six.ensure_text(gen_data['expected_saas_properties']['title'])
        ),
        'Wrong title'
    )
    _check_image(
        result_saas_props["image"],
        gen_data['expected_saas_properties']['image']
    )
    _check_nid(
        result_saas_props["nid"],
        gen_data['expected_saas_properties']['nid']
    )
