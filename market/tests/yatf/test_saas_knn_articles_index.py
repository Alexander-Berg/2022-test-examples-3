# coding=utf-8

"""Тест проверяет prepare_articles_index подготавки данных для построения индекса по статьям из cms выгрузок
"""

import json
import pytest
import six

from hamcrest import assert_that, equal_to, has_entries, is_not, has_item

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.ugc.yatf.test_envs.prepare_articles_index import YtPrepareArticlesIndexTestEnv
from market.idx.ugc.yatf.resources.prepare_articles_index.articles_input_data import (
    DEFAULT_IMAGE_JSON,
    DEFAULT_NIDS,
    DEFAULT_SEMANTIC_ID,
    DEFAULT_TITLE,
    YTArticlesInputData,
    content_json,
    properties_json,
)


TITLE_WITH_NEW_LINES = "Как выбрать\\n\\nгамак"
FAKE_TITLE = "Это не тот тайтл, который используется в индеке"

TYPE = "knowledge"
URL = "https://market.yandex.ru/journal/knowledge/kak-vybrat-gamak"

CONTENT_JSON_NO_HUB_ENTRYPOINT = {
    "entrypoints": [
        {
            "entity": "entrypoint",
            "name": "nohub",
            "image": DEFAULT_IMAGE_JSON,
            "title": "some title",
        }
    ]
}

ARTICLES_DSSM_HOST = "market.yandex.ru"

PAGE_ID_GOOD = 37520
PAGE_ID_GOOD_NEW_LINES_TITLE = 37521

PAGE_ID_FILTERED_NO_TYPE = 37522
PAGE_ID_FILTERED_NO_PROPERTIES = 37523
PAGE_ID_FILTERED_NO_URL = 37524
PAGE_ID_FILTERED_INVALID_TYPE = 37525
PAGE_ID_FILTERED_NO_SEMANTIC_ID = 37526
PAGE_ID_FILTERED_WRONG_COMPILED_NAME = 37527
PAGE_ID_FILTERED_WRONG_DEVICE = 37528
PAGE_ID_FILTERED_NO_HUB_ENTRYPOINTS = 37529
PAGE_ID_FILTERED_NO_TITLE = 37530
PAGE_ID_FILTERED_NO_CONTENT = 37531

INVALID_ARTICLE_TYPE = "invalid_type"


@pytest.fixture(scope='module')
def cms_pages_path():
    return "//home/cms/pages"


@pytest.fixture(scope='module')
def cms_pages_table(yt_stuff, cms_pages_path):
    schema = [
        dict(name="page_id", type="int64"),
        dict(name="title", type="string"),
        dict(name="type", type="string"),
        dict(name="url", type="string"),
        dict(name="properties", type="string"),
    ]
    rows = [
        {
            "page_id": PAGE_ID_GOOD, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json()),
            "type": TYPE, "url": URL
        },
        {
            "page_id": PAGE_ID_GOOD_NEW_LINES_TITLE, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json()),
            "type": TYPE, "url": URL
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_TYPE, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json()),
            "type": None, "url": URL
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_URL, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json()),
            "type": TYPE, "url": None
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_PROPERTIES, "title": FAKE_TITLE,
            "properties": None,
            "type": TYPE, "url": URL
        },
        {
            "page_id": PAGE_ID_FILTERED_INVALID_TYPE, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json()),
            "type": INVALID_ARTICLE_TYPE, "url": URL
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_SEMANTIC_ID, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json(semantic_ids=[])),
            "type": TYPE, "url": URL
        },
        {
            "page_id": PAGE_ID_FILTERED_WRONG_COMPILED_NAME, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json()),
            "type": TYPE, "url": URL
        },
        {
            "page_id": PAGE_ID_FILTERED_WRONG_DEVICE, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json()),
            "type": TYPE, "url": URL
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_HUB_ENTRYPOINTS, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json()),
            "type": TYPE, "url": URL
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_TITLE, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json()),
            "type": TYPE, "url": URL
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_CONTENT, "title": FAKE_TITLE,
            "properties": json.dumps(properties_json()),
            "type": TYPE, "url": URL
        },
    ]

    return YtTableResource(yt_stuff, cms_pages_path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def cms_compiled_path():
    return "//home/cms/compiled"


@pytest.fixture(scope='module')
def cms_compiled_table(yt_stuff, cms_compiled_path):
    schema = [
        dict(name="page_id", type="int64"),
        dict(name="device", type="string"),
        dict(name="name", type="string"),
        dict(name="content", type="string"),
    ]
    rows = [
        {
            "page_id": PAGE_ID_GOOD, "device": "desktop", "name": "entrypoints",
            "content": json.dumps(content_json(aliases=["гамак", "кресло гамак"])),
        },
        {
            "page_id": PAGE_ID_GOOD, "device": "wrong device, will be skipped", "name": "entrypoints",
            "content": json.dumps(content_json(aliases=["это не попадет", "в индекс"])),
        },
        {
            "page_id": PAGE_ID_GOOD_NEW_LINES_TITLE, "device": "desktop", "name": "entrypoints",
            "content": json.dumps(content_json(title=TITLE_WITH_NEW_LINES)),
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_TYPE, "device": "desktop", "name": "entrypoints",
            "content": json.dumps(content_json()),
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_URL, "device": "desktop", "name": "entrypoints",
            "content": json.dumps(content_json()),
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_PROPERTIES, "device": "desktop", "name": "entrypoints",
            "content": json.dumps(content_json()),
        },
        {
            "page_id": PAGE_ID_FILTERED_INVALID_TYPE, "device": "desktop", "name": "entrypoints",
            "content": json.dumps(content_json()),
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_SEMANTIC_ID, "device": "desktop", "name": "entrypoints",
            "content": json.dumps(content_json()),
        },
        {
            "page_id": PAGE_ID_FILTERED_WRONG_COMPILED_NAME, "device": "desktop", "name": "not entrypoints",
            "content": json.dumps(content_json()),
        },
        {
            "page_id": PAGE_ID_FILTERED_WRONG_DEVICE, "device": "phone", "name": "entrypoints",
            "content": json.dumps(content_json()),
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_HUB_ENTRYPOINTS, "device": "desktop", "name": "entrypoints",
            "content": json.dumps(CONTENT_JSON_NO_HUB_ENTRYPOINT),
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_TITLE, "device": "desktop", "name": "entrypoints",
            "content": json.dumps(content_json(title="")),
        },
        {
            "page_id": PAGE_ID_FILTERED_NO_CONTENT, "device": "desktop", "name": "entrypoints",
            "content": None,
        },
    ]

    return YtTableResource(yt_stuff, cms_compiled_path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def input_data(cms_pages_table, cms_compiled_table):
    return YTArticlesInputData(cms_pages_table, cms_compiled_table)


prepared_articles_path = "//home/prepared_articles"


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data):
    resources = {
        "input": input_data,
    }

    with YtPrepareArticlesIndexTestEnv(**resources) as env:
        env.execute(yt_stuff, result_path=prepared_articles_path)
        env.verify()
        yield env


def test_result_table_exists(workflow, yt_stuff):
    assert_that(
        yt_stuff.get_yt_client().exists(
            workflow.result_table.get_path()
        ),
        'Prepared articles table doen\'t exist'
    )


def test_prepared_articles_count(workflow, yt_stuff):
    assert_that(
        len(workflow.result_data),
        equal_to(2),
        'Prepared articles table wrong data len'
    )


def _get_articles_dssm_path(url):
    _, _, path = url.partition(ARTICLES_DSSM_HOST)
    return path


def _correct_title(title):
    return ' '.join(title.replace('\n', ' ').split())


@pytest.fixture(scope="module")
def expected_articles():
    return {
        PAGE_ID_GOOD: {
            "doc_id": PAGE_ID_GOOD,
            "shard_id": 0,
            "url": URL,
            "host": ARTICLES_DSSM_HOST,
            "path": _get_articles_dssm_path(URL),
            "semantic_id": DEFAULT_SEMANTIC_ID,
            "type": TYPE,
            "title": DEFAULT_TITLE,
            "aliases": "гамак;кресло гамак",
        },
        PAGE_ID_GOOD_NEW_LINES_TITLE: {
            "doc_id": PAGE_ID_GOOD_NEW_LINES_TITLE,
            "shard_id": 0,
            "url": URL,
            "host": ARTICLES_DSSM_HOST,
            "path": _get_articles_dssm_path(URL),
            "semantic_id": DEFAULT_SEMANTIC_ID,
            "type": TYPE,
            "title": _correct_title(TITLE_WITH_NEW_LINES),
            "aliases": "",
        }
    }


@pytest.mark.parametrize(
    'page_id',
    [
        PAGE_ID_GOOD,
        PAGE_ID_GOOD_NEW_LINES_TITLE,
    ]
)
def test_prepared_articles(workflow, page_id, expected_articles):
    assert_that(
        workflow.result_data,
        has_item(has_entries(expected_articles[page_id])),
        'Wrong article'
    )


def _check_image(actual, expected):
    assert_that(json.loads(actual), has_entries(expected))


def _check_nid(actual, expected):
    assert_that(json.loads(actual), equal_to(expected))


@pytest.fixture(scope="module")
def expected_saas_message():
    return {
        PAGE_ID_GOOD: {
            "type": TYPE,
            "semantic_id": DEFAULT_SEMANTIC_ID,
            "title": DEFAULT_TITLE,
            "url": URL,
            "image": DEFAULT_IMAGE_JSON,
            "nid": DEFAULT_NIDS,
        },
        PAGE_ID_GOOD_NEW_LINES_TITLE: {
            "type": TYPE,
            "semantic_id": DEFAULT_SEMANTIC_ID,
            "title": _correct_title(TITLE_WITH_NEW_LINES),
            "url": URL,
            "image": DEFAULT_IMAGE_JSON,
            "nid": DEFAULT_NIDS,
        }
    }


@pytest.mark.parametrize(
    'page_id',
    [
        PAGE_ID_GOOD,
        PAGE_ID_GOOD_NEW_LINES_TITLE,
    ]
)
def test_prepared_articles_saas_doc(workflow, page_id, expected_saas_message):
    actual_saas_doc = workflow.result_saas_docs[page_id]

    props_to_check = dict(expected_saas_message[page_id])
    props_to_check.pop("image", "")  # проверяется отдельно
    props_to_check.pop("title", "")  # проверяется отдельно
    props_to_check.pop("nid", "")  # проверяется отдельно

    props_to_check.update({
        "page_id": str(page_id),
        "pages_generation": "test-pages",
        "compiled_generation": "test-compiled",
    })

    assert_that(
        actual_saas_doc,
        has_entries(props_to_check),
        'Wrong saas message'
    )
    assert_that(
        six.ensure_text(actual_saas_doc['title']),
        equal_to(
            six.ensure_text(expected_saas_message[page_id]['title'])
        ),
        'Wrong title'
    )
    _check_image(
        actual_saas_doc["image"],
        expected_saas_message[page_id]["image"]
    )
    _check_nid(
        actual_saas_doc["nid"],
        expected_saas_message[page_id]["nid"]
    )


@pytest.mark.parametrize(
    'page_id',
    [
        PAGE_ID_FILTERED_NO_TYPE,
        PAGE_ID_FILTERED_NO_PROPERTIES,
        PAGE_ID_FILTERED_NO_URL,
        PAGE_ID_FILTERED_INVALID_TYPE,
        PAGE_ID_FILTERED_NO_SEMANTIC_ID,
        PAGE_ID_FILTERED_WRONG_COMPILED_NAME,
        PAGE_ID_FILTERED_WRONG_DEVICE,
        PAGE_ID_FILTERED_NO_HUB_ENTRYPOINTS,
        PAGE_ID_FILTERED_NO_TITLE,
        PAGE_ID_FILTERED_NO_CONTENT,
    ]
)
def test_should_not_see_articles(workflow, page_id):
    """Проверяем, что все статьи без необходимых данных не попали в индекс
    """
    assert_that(
        workflow.result_data,
        is_not(
            has_item(
                has_entries({
                    'doc_id': page_id
                })
            )
        )
    )
