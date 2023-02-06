# coding=utf-8

from base64 import (
    urlsafe_b64encode,
)
from copy import deepcopy
from hamcrest import (
    assert_that,
)
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.resources.offers_indexer.feed import fill_from_dict
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv

from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
)

from market.proto.content.pictures_pb2 import Picture
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


test_data = [
    {
        'offer_id': '1',
    },
    {
        'offer_id': '2',
        'picture_urls': ['http://ya.ru'],
    },
    {
        'offer_id': '3',
        'picture_urls': ['http://shock_content.jpg'],
    },
    {
        'offer_id': '4',
        'picture_urls': ['http://market.jpg', 'https://beru.png'],
    },
]


@pytest.yield_fixture(scope='module')
def genlog_rows():
    feed = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            picture_urls=data.get('picture_urls', None),
        )
        feed.append(offer)
    return feed


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope='module')
def genlog_snippet_workflow(yt_server, offers_processor_workflow):
    genlogs = []
    for id, glProto in enumerate(offers_processor_workflow.genlog_dicts):
        genlogs.append(glProto)

    with SnippetDiffBuilderTestEnv(
        'genlog_snippet_workflow',
        yt_server,
        offers=[],
        genlogs=genlogs,
        models=[],
        state=[],
    ) as env:
        env.execute()
        env.verify()
        yield env


def make_pic(data, clear):
    cdata = deepcopy(data)
    if clear and 'signatures' in cdata:
        del cdata['signatures']
    return urlsafe_b64encode(
        fill_from_dict(cdata, Picture).SerializeToString()
    ).replace('=', ',')


def make_pics(data, clear=True):
    if data is None:
        return None
    return '|'.join([
        make_pic(picture, clear)
        for picture in data
    ])


@pytest.yield_fixture(scope="module")
def expected_PictureProtoBase64_snippet():
    return [
        {
            'offer_id': x['offer_id'],
            'PicturesProtoBase64': make_pics(
                x.get('pic', None),
            ),
        }
        for x in test_data
    ]


def test_PictureProtoBase64_snippet(
        genlog_snippet_workflow,
        expected_PictureProtoBase64_snippet
):
    for expected in expected_PictureProtoBase64_snippet:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
