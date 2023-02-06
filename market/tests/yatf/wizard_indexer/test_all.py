# coding=utf-8

from hamcrest import assert_that, equal_to, all_of
import pytest

from market.idx.generation.yatf.matchers.arc2ferryman.env_matchers import ContainsDocument
from market.idx.generation.yatf.test_envs.wizard_indexer import WizardIndexerTestEnv
from market.idx.generation.yatf.test_envs.arc2ferryman import Arc2FerrymanTestEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from yt.wrapper import ypath_join


@pytest.yield_fixture(scope="module")
def wizard_workflow():
    resources = {}
    with WizardIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_run(wizard_workflow):
    first_category = wizard_workflow.categories[0]
    expected = {
        'LANG': 'ru',
        'tmar': '1',
        'doc_type': '100',
        'url': 'http://market.yandex.ru/catalog.xml?hid=198118',
        'category_hid': '198118',
        'host': '',
        'sentences': [
            'Бытовая техника',
            'Бытовая техника Побутова техніка Побутова техніка',
        ],
        'ferrykey': 'wizard-198118',
        'size': 29,
    }
    assert_that(first_category, equal_to(expected))


def test_all_categories_has_ferrykey(wizard_workflow):
    actual = [
        category['ferrykey']
        for category
        in wizard_workflow.categories.itervalues()
    ]
    expected = [
        'wizard-{}'.format(category['category_hid'])
        for category
        in wizard_workflow.categories.itervalues()
    ]
    assert_that(actual, equal_to(expected))


@pytest.yield_fixture(scope="module")
def ferryman_workflow(yt_server, wizard_workflow):
    resources = {}
    with Arc2FerrymanTestEnv(
        yt_server,
        archive_type='wizard',
        archive_dir=wizard_workflow.index_dir,
        yt_output_table=ypath_join(get_yt_prefix(), 'wizard'),
        **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_ferryman_output_rows_count(ferryman_workflow, wizard_workflow):
    assert_that(len(wizard_workflow.categories), equal_to(len(ferryman_workflow.rows)))


def test_ferryman_output(ferryman_workflow, wizard_workflow):
    expected = [
        {
            # https://a.yandex-team.ru/arc/trunk/arcadia/market/indexer/snippet_builder/types.h?rev=5186689#L7
            'doc_type': 4,  # wizard
            'key': 'wizard-{}'.format(category['category_hid']),
            'value': {
                'Document': {
                    'Url': 'wizard-{}'.format(category['category_hid']),
                    'DocumentProperties': [
                        {
                            'Name': 'DocId',
                            'Value': str(doc_id),
                        },
                        {
                            'Name': '_Url',
                            'Value': 'http://market.yandex.ru/catalog.xml?hid={}'.format(category['category_hid']),
                        },
                        {
                            'Name': '_Title',
                            'Value': category['sentences'][0],
                        },
                        {
                            'Name': 'Body',
                            'Value': '\n'.join(category['sentences'])
                        },
                        {
                            'Name': 'category_hid',
                            'Value': category['category_hid'],
                        },
                        {
                            'Name': 'LANG',
                            'Value': 'ru',
                        },
                    ],
                },
            },
        }
        for doc_id, category
        in wizard_workflow.categories.iteritems()
    ]
    actual = [
        {
            'doc_type': row['doc_type'],  # wizard
            'key': row['key'],
            'value': {
                'Document': {
                    'Url': row['value']['Document']['Url'],
                    'DocumentProperties': row['value']['Document']['DocumentProperties'],
                },
            },
        }
        for row
        in ferryman_workflow.rows
    ]

    assert_that(
        expected,
        all_of(
            *[ContainsDocument(doc) for doc in actual]
        )
    )
