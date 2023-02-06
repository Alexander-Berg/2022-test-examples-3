# coding=utf-8

from hamcrest import assert_that, equal_to, all_of, has_item, has_entries
from lxml import etree
import pytest
import six

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.generation.yatf.test_envs.arc2ferryman import Arc2FerrymanTestEnv
from market.idx.generation.yatf.test_envs.cards_indexer import CardsIndexerTestEnv

from yt.wrapper import ypath_join


def make_ferrykey(card):
    if card['doc_type'] == '4':
        # category
        # hard to find for tests
        pass

    if card['doc_type'] == '5':
        # vendor
        # hard to find for tests
        pass

    if card['doc_type'] == '6':
        # category vendor
        # hard to find for tests
        pass

    if card['doc_type'] == '7':
        # navigation virtual card
        if card.get('nid_card', None):
            return 'navigation_virtual_card-{}'.format(card['nid'])

    if card['doc_type'] == '13':
        # visual card
        if card.get('c_card', None):
            # category card
            return 'c_card-{}-0'.format(
                card.get('category_id', None)
            )
        if card.get('v_card', None):
            # vendor card
            return 'v_card-0-{}'.format(
                card.get('vendor_id', None)
            )
        if card.get('cv_card', None):
            # category-vendor card
            return 'cv_card-{}-{}'.format(
                card.get('category_id', None),
                card.get('vendor_id', None)
            )

    raise RuntimeError(card)


def get_aliases(root, vendor_id):
    vendor_aliases = root.xpath('/vendors/vendor[@id="{}"]/alias'.format(vendor_id))
    return [alias.text.strip() for alias in vendor_aliases]


@pytest.yield_fixture(scope="module")
def cards_workflow():
    resources = {}
    with CardsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def visual_vendor_aliases_xml(cards_workflow):
    with open(cards_workflow.vendor_aliases, 'rb') as xml_file:
        xml = xml_file.read()
        root = etree.fromstring(xml)
        yield root


def test_run(cards_workflow):
    assert_that(len(cards_workflow.cards), equal_to(9376))


def test_vendor_aliases(cards_workflow, visual_vendor_aliases_xml):
    cv_card = cards_workflow.cards[4609]
    assert_that(
        cv_card,
        has_entries({
            'cv_card': '1',
            'doc_type': '13',
            'ferrykey': 'cv_card-8468853-8340766',
            'vcluster_wizard': '1',
        }),
        'VisualCard is correct'
    )
    assert_that(cv_card.get('category_id', ''), '8468853')
    assert_that(cv_card.get('vendor_id', ''), '8340766')
    assert_that(cv_card.get('vendor_name', ''), 'LC Waikiki')

    aliases = get_aliases(visual_vendor_aliases_xml, 8340766)
    sentences = [six.ensure_text(s) for s in cv_card.get('sentences', [])]
    assert_that(
        ' '.join(aliases) in
        ' '.join(sentences)
    )


def test_all_cards_has_ferrykey(cards_workflow):
    actual = [
        card['ferrykey']
        for card
        in cards_workflow.cards.values()
    ]
    expected = [
        make_ferrykey(card)
        for card
        in cards_workflow.cards.values()
    ]
    assert_that(actual, equal_to(expected))


@pytest.yield_fixture(scope="module")
def ferryman_workflow(yt_server, cards_workflow):
    resources = {}
    with Arc2FerrymanTestEnv(
        yt_server,
        archive_type='cards',
        archive_dir=cards_workflow.index_dir,
        yt_output_table=ypath_join(get_yt_prefix(), 'cards'),
        **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_ferryman_output_rows_count(ferryman_workflow, cards_workflow):
    assert_that(len(cards_workflow.cards), equal_to(len(ferryman_workflow.rows)))


def test_ferryman_output(ferryman_workflow, cards_workflow):
    expecteds = []
    for doc_id, card in cards_workflow.cards.items():
        expected = {
            # https://a.yandex-team.ru/arc/trunk/arcadia/market.idx.generation.snippet_builder/types.h?rev=5186689#L6
            'doc_type': 3,  # CARDS
            'key': card['ferrykey'],
            'value': {
                'Document': {
                    'Url': make_ferrykey(card),
                    'DocumentProperties': [
                        {
                            'Name': 'DocId',
                            'Value': str(doc_id),
                        },
                        {
                            'Name': '_Url',
                            'Value': card['url'],
                        },
                        {
                            'Name': 'Body',
                            'Value': '\n'.join(card['sentences'])
                        },
                        {
                            'Name': 'LANG',
                            'Value': 'ru',
                        },
                    ],
                },
            },
        }
        # visual card
        if card.get('c_card', None):
            # category card
            expected['value']['Document']['DocumentProperties'].append(
                {'Name': 'category_id', 'Value': card['category_id']})
            expected['value']['Document']['DocumentProperties'].append(
                {'Name': 'category_title', 'Value': card['category_title']})
        if card.get('v_card', None):
            # vendor card
            expected['value']['Document']['DocumentProperties'].append(
                {'Name': 'category_id', 'Value': card['category_id']})
            expected['value']['Document']['DocumentProperties'].append(
                {'Name': 'vendor_filter_id', 'Value': card['vendor_filter_id']})
            expected['value']['Document']['DocumentProperties'].append(
                {'Name': 'vendor_id', 'Value': card['vendor_id']})
            expected['value']['Document']['DocumentProperties'].append(
                {'Name': 'vendor_name', 'Value': card['vendor_name']})
        if card.get('cv_card', None):
            # category-vendor card
            expected['value']['Document']['DocumentProperties'].append(
                {'Name': 'category_id', 'Value': card['category_id']})
            expected['value']['Document']['DocumentProperties'].append(
                {'Name': 'category_title', 'Value': card['category_title']})
            expected['value']['Document']['DocumentProperties'].append(
                {'Name': 'vendor_filter_id', 'Value': card['vendor_filter_id']})
            expected['value']['Document']['DocumentProperties'].append(
                {'Name': 'vendor_id', 'Value': card['vendor_id']})
            expected['value']['Document']['DocumentProperties'].append(
                {'Name': 'vendor_name', 'Value': card['vendor_name']})
        expecteds.append(expected)

    actuals = [
        {
            'doc_type': row['doc_type'],
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

    for actual, expected in zip(actuals, expecteds):
        assert_that(actual['doc_type'], equal_to(expected['doc_type']))
        assert_that(actual['key'], equal_to(expected['key']))
        assert_that(
            actual['value']['Document']['Url'],
            equal_to(expected['value']['Document']['Url'])
        )

        assert_that(
            actual['value']['Document']['DocumentProperties'],
            all_of(
                *[has_item(has_entries(docProps))
                  for docProps
                  in expected['value']['Document']['DocumentProperties']]
            ),
        )
