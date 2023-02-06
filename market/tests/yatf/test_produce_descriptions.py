# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to

from market.idx.streams.src.prepare_description_streams.yatf.test_env import (
    YtDescriptionStreamsTestEnv
)

from market.idx.yatf.resources.yt_table_resource import YtTableResource

from yt.wrapper.ypath import ypath_join

from market.idx.streams.yatf.utils import offer_url


# ware_md5
OFFER_BOTH_OFFER_AND_WEB0 = "both0"
OFFER_ONLY_OFFER_DESC0 = "offer_desc0"
OFFER_ONLY_WEB0 = "web0"
OFFER_WEB_WITHOUT_OFFER = "web_no_offer"


PARTS_COUNT = "1"


@pytest.fixture(scope='module')
def working_path():
    return '//home/test_descriptions'


@pytest.fixture(scope='module')
def input_offers_with_description(yt_stuff, working_path):
    table_path = ypath_join(working_path, 'offers_with_descriptions')
    data = [{
        'ware_md5': 'web0',
        'part': 0L,
        'subkey': 'handsome.gromov.ru/web0'}]

    attributes = attributes=dict(schema=[
        dict(name='subkey', type='string'),
        dict(name='part', type='uint64'),
        dict(name='ware_md5', type='string')])

    table = YtTableResource(yt_stuff, table_path, data=data, attributes=attributes)
    table.dump()

    return table


BOTH_DESC = OFFER_BOTH_OFFER_AND_WEB0 + " Should see only web description, not offer"
ONLY_WEB_DESC = OFFER_ONLY_WEB0 + " description"


@pytest.fixture(scope='module')
def descriptions_table(yt_stuff):
    desc_path = "//home/test_descriptions/web_data/descriptions"
    data = [
        {'key': offer_url(OFFER_BOTH_OFFER_AND_WEB0),
         'subkey': offer_url(OFFER_BOTH_OFFER_AND_WEB0),
         'value': BOTH_DESC},
        {'key': offer_url(OFFER_ONLY_WEB0),
         'subkey': offer_url(OFFER_ONLY_WEB0),
         'value': ONLY_WEB_DESC},
        {'key': offer_url(OFFER_WEB_WITHOUT_OFFER),
         'subkey': offer_url(OFFER_WEB_WITHOUT_OFFER),
         'value': OFFER_WEB_WITHOUT_OFFER},
    ]
    table = YtTableResource(yt_stuff,
                            desc_path,
                            data)
    table.dump()

    return table


@pytest.fixture(scope='module')
def desc_streams_output():
    return "//home/test_descriptions/web_data/descriptions"


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_offers_with_description, working_path, descriptions_table, desc_streams_output):
    resources = {}

    with YtDescriptionStreamsTestEnv(**resources) as env:
        env.execute_synch_part(
            yt_stuff,
            descriptions_table.get_path(),
            working_path,
            desc_streams_output
        )
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_description_streams(workflow):
    return workflow.outputs.get('description_streams')


def test_result_tables_exist(result_description_streams,
                             yt_stuff):
    assert_that(yt_stuff.get_yt_client().exists(
                result_description_streams.get_path()),
                'Table with descriptions doesn\'t exist')


def test_descriptions(result_description_streams):
    sorted_by_key = lambda lst: sorted(lst, key=lambda row: row['ware_md5'])
    assert_that(sorted_by_key(list(result_description_streams.data)),
                equal_to(sorted_by_key([{
                    'region_id': 225,
                    'url': offer_url(OFFER_ONLY_WEB0),
                    'text': ONLY_WEB_DESC,
                    'value': "1",
                    'ware_md5': OFFER_ONLY_WEB0,
                    'part': 0
                }])), 'Wrong descriptions')
