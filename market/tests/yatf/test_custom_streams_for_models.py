# coding=utf-8
"""
Тест используется для проверки работы подклейки кастомных стримов
к вебовским для моделей в вызове collect_streams.
Проверяется, что модельные алисы корректно подклеиваются к вебоским стримам.
"""

import pytest

from hamcrest import assert_that, has_items, has_entries, all_of

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.streams.yatf.test_env import YtStreamsTestEnv
from market.idx.streams.yatf.resources.yt_streams_data import YTStreamsData
from market.idx.streams.yatf.resources.yt_streams_output import YtStreamsOutput

from kernel.indexann.protos.data_pb2 import (
    TIndexAnnSiteData,
    TAnnotationRec,
    TRegionData,
    TMarketStreams,
    TFloatStream,
    TDummyData,
)

from market.idx.streams.yatf.utils import ModelFactory


def _model_url(id):
    return "market.yandex.ru/product/" + id


# Model ids
MODEL_ID_BOTH_ALIASES_AND_STREAMS = "11"
MODEL_ID_ONLY_ALIASES = "7"
MODEL_ID_BOTH_ONE_ALIAS = "77"
MODEL_ID_ONLY_WEB1 = "1"
MODEL_ID_ONLY_WEB2 = "2"
MODEL_ID_WITHOUT_STREAMS = "666"
MODEL_ID_IMAGE_STREAMS = "998"
MODEL_ID_ALL_WEB_IMAGE_ALIAS_TITLE_MDESCR_STREAMS = "999"
MODEL_ID_ONLY_TITLES = "123"
MODEL_ID_ONLY_MARKETING_DESCR = "100500"
MODEL_ID_ONLY_MICRO_MODEL_DESCR = "100501"
MODEL_ID_ONLY_CPA_QUERIES = "31"


RUSSIA = 225
LANG_RUS = 1


@pytest.fixture(scope='module')
def model_aliases_path():
    return "//home/indexer/streams/in/models/aliases_streams"


# Table with prepared aliases sorted by model_id
@pytest.fixture(scope='module')
def model_aliases_table(yt_stuff, model_aliases_path):

    schema = [dict(name="model_id", type="string"),
              dict(name="region_id", type="uint64"),
              dict(name="url", type="string"),
              dict(name="text", type="string"),
              dict(name="value", type="string"),
              dict(name="part", type="uint64"),
              ]
    rows = [
        {'model_id': MODEL_ID_BOTH_ALIASES_AND_STREAMS, 'region_id': RUSSIA, 'url': _model_url(MODEL_ID_BOTH_ALIASES_AND_STREAMS),
         'text': 'alias11_1', 'value': '1', 'part': 0},  # should see both web and aliases in a result table
        {'model_id': MODEL_ID_BOTH_ALIASES_AND_STREAMS, 'region_id': RUSSIA, 'url': _model_url(MODEL_ID_BOTH_ALIASES_AND_STREAMS),
         'text': 'alias11_2', 'value': '1', 'part': 0},  # the next one alias stream (see above)
        {'model_id': MODEL_ID_ONLY_ALIASES,  'region_id': RUSSIA, 'url': _model_url(MODEL_ID_ONLY_ALIASES),
         'text': 'alias7_1 only aliases', 'value': '1', 'part': 0},  # no model_id in web table so should be added
        {'model_id': MODEL_ID_ONLY_ALIASES,  'region_id': RUSSIA, 'url': _model_url(MODEL_ID_ONLY_ALIASES),
         'text': 'alias7_2 only aliases', 'value': '1', 'part': 0},  # the next one alias stream (see above)
        {'model_id': MODEL_ID_BOTH_ONE_ALIAS, 'region_id': RUSSIA, 'url': _model_url(MODEL_ID_BOTH_ONE_ALIAS),
         'text': 'alias77', 'value': '1', 'part': 0},  # should see both web and aliases in a result table. Only one stream alias
        {'model_id': MODEL_ID_ALL_WEB_IMAGE_ALIAS_TITLE_MDESCR_STREAMS, 'region_id': RUSSIA, 'url': _model_url(MODEL_ID_ALL_WEB_IMAGE_ALIAS_TITLE_MDESCR_STREAMS),
         'text': 'alias999 all streams', 'value': '1', 'part': 0},  # should see all streams: web, alias, image, title, marketing_description
    ]

    return YtTableResource(yt_stuff, model_aliases_path, data=rows, sort_key="model_id", attributes={'schema': schema})


@pytest.fixture(scope='module')
def model_titles_path():
    return "//home/indexer/streams/in/models/titles_streams"


@pytest.fixture(scope='module')
def model_titles_table(yt_stuff, model_titles_path):

    schema = [dict(name="model_id", type="string"),
              dict(name="region_id", type="uint64"),
              dict(name="url", type="string"),
              dict(name="text", type="string"),
              dict(name="value", type="string"),
              dict(name="part", type="uint64"),
              ]
    rows = [
        {'model_id': MODEL_ID_ALL_WEB_IMAGE_ALIAS_TITLE_MDESCR_STREAMS, 'region_id': RUSSIA, 'url': _model_url(MODEL_ID_ALL_WEB_IMAGE_ALIAS_TITLE_MDESCR_STREAMS),
         'text': 'title999', 'value': '1', 'part': 0},  # should see all treams: web, alias, image, title, marketing_description
    ]

    return YtTableResource(yt_stuff, model_titles_path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def marketing_descriptions_path():
    return "//home/indexer/streams/in/models/marketing_description_streams"


@pytest.fixture(scope='module')
def marketing_descriptions_table(yt_stuff, marketing_descriptions_path):

    schema = [dict(name="model_id", type="string"),
              dict(name="region_id", type="uint64"),
              dict(name="url", type="string"),
              dict(name="text", type="string"),
              dict(name="value", type="string"),
              dict(name="part", type="uint64"),
              ]
    rows = [
        {'model_id': MODEL_ID_ONLY_MARKETING_DESCR, 'region_id': RUSSIA, 'url': _model_url(MODEL_ID_ONLY_MARKETING_DESCR),
         'text': 'marketing descr', 'value': '1', 'part': 0},  # should streams: marketing_description
        {'model_id': MODEL_ID_ALL_WEB_IMAGE_ALIAS_TITLE_MDESCR_STREAMS, 'region_id': RUSSIA, 'url': _model_url(MODEL_ID_ONLY_MARKETING_DESCR),
         'text': 'another marketing descr', 'value': '1', 'part': 0},  # should see all streams: web, alias, image, title, marketing_description
    ]

    return YtTableResource(yt_stuff, marketing_descriptions_path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def micro_model_descriptions_path():
    return "//home/indexer/streams/in/models/micro_model_description_streams"


@pytest.fixture(scope='module')
def micro_model_descriptions_table(yt_stuff, micro_model_descriptions_path):

    schema = [dict(name="model_id", type="string"),
              dict(name="region_id", type="uint64"),
              dict(name="url", type="string"),
              dict(name="text", type="string"),
              dict(name="value", type="string"),
              dict(name="part", type="uint64"),
              ]
    rows = [
        {'model_id': MODEL_ID_ONLY_MICRO_MODEL_DESCR, 'region_id': RUSSIA, 'url': _model_url(MODEL_ID_ONLY_MICRO_MODEL_DESCR),
         'text': 'micro model descr', 'value': '1', 'part': 0},  # should streams: micro_model_description
    ]

    return YtTableResource(yt_stuff, micro_model_descriptions_path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def cpa_queries_path():
    return "//home/indexer/streams/cpa_queries"


@pytest.fixture(scope='module')
def cpa_queries_table(yt_stuff, cpa_queries_path):
    schema = [dict(name="model_id", type="string"),
              dict(name="region_id", type="uint64"),
              dict(name="text", type="string"),
              dict(name="value", type="string"),
              dict(name="part", type="uint64"),
              dict(name="url", type="string"),
              ]
    rows = [
        {'model_id': MODEL_ID_ONLY_CPA_QUERIES,
         'region_id': RUSSIA,
         'text': 'ordered cpa query',
         'value': '1',
         'url': _model_url(MODEL_ID_ONLY_CPA_QUERIES),
         'part': 0},  # should streams: cpa_query
    ]
    return YtTableResource(yt_stuff, cpa_queries_path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def web_streams_pb1():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="BrowserPageRank",
                       Data=[TRegionData(Region=0, BrowserPageRank=TFloatStream(Value=0.5))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="BrowserPageRankSample",
                       Data=[TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.7))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def web_streams_pb2():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="FirstClickDtXf",
                       Data=[TRegionData(Region=0, FirstClickDtXf=TFloatStream(Value=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="BrowserPageRankSample",
                       Data=[TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.7))],
                       TextLanguage=LANG_RUS),
    ])


# Table with web streams
@pytest.fixture(scope='module')
def web_streams_table(yt_stuff, web_streams_pb1, web_streams_pb2):
    web_streams_path = "//userfeat/exports/web/IndexAnnSourceData"
    rows = [
        {'key': _model_url(MODEL_ID_BOTH_ALIASES_AND_STREAMS), 'value': web_streams_pb1.SerializeToString()},  # there are both web and aliases streams in a result table
        {'key': _model_url(MODEL_ID_BOTH_ONE_ALIAS), 'value': web_streams_pb1.SerializeToString()},  # should see both web and aliases in a result table. Only one stream alias
        {'key': _model_url(MODEL_ID_ONLY_WEB1), 'value': web_streams_pb1.SerializeToString()},  # there are only web streams in a result table
        {'key': _model_url(MODEL_ID_ONLY_WEB2), 'value': web_streams_pb2.SerializeToString()},  # there are only web streams in a result table
        {'key': _model_url(MODEL_ID_ALL_WEB_IMAGE_ALIAS_TITLE_MDESCR_STREAMS), 'value': web_streams_pb2.SerializeToString()},  # should see all treams: web, alias, image, marketing_description
    ]

    return YtTableResource(yt_stuff, web_streams_path, data=rows)


@pytest.fixture(scope='module')
def input_path():
    return "//home/indexer/streams/models"


# /in/models table which is used by collect_stream to obtain associated web streams
@pytest.fixture(scope='module')
def input_shard(yt_stuff, input_path):
    rows = []

    model_urls = []
    model_urls.append(_model_url(MODEL_ID_BOTH_ALIASES_AND_STREAMS))
    model_urls.append(_model_url(MODEL_ID_ONLY_ALIASES))
    model_urls.append(_model_url(MODEL_ID_BOTH_ONE_ALIAS))
    model_urls.append(_model_url(MODEL_ID_ONLY_WEB1))
    model_urls.append(_model_url(MODEL_ID_ONLY_WEB2))
    model_urls.append(_model_url(MODEL_ID_WITHOUT_STREAMS))
    model_urls.append(_model_url(MODEL_ID_IMAGE_STREAMS))
    model_urls.append(_model_url(MODEL_ID_ALL_WEB_IMAGE_ALIAS_TITLE_MDESCR_STREAMS))
    model_urls.append(_model_url(MODEL_ID_ONLY_MARKETING_DESCR))
    model_urls.append(_model_url(MODEL_ID_ONLY_MICRO_MODEL_DESCR))
    model_urls.append(_model_url(MODEL_ID_ONLY_CPA_QUERIES))

    rows.extend(ModelFactory.models(model_urls))

    shard = YtTableResource(yt_stuff, input_path, data=rows)

    return [shard]


@pytest.fixture(scope='module')
def only_image_stream():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="ImageDocDwellTime only images",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(ImageDocDwellTime=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="ImageAltData only images",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(ImageAltData=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def image_all_streams():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="ImageQueryClicks all streams",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(ImageQueryClicks=0.11))],
                       TextLanguage=LANG_RUS),
    ])


# Table with prepared image streams sorted by model_id.
@pytest.fixture(scope='module')
def model_image_stream_table(yt_stuff, only_image_stream, image_all_streams):
    model_image_path = "//home/indexer/streams/image_streams"
    schema = [dict(name="model_id", type="string"),
              dict(name="url", type="string"),
              dict(name="ann_data", type="string"),
              dict(name="part", type="uint64"),
              ]
    rows = [
        # there is only one image stream for model
        {'model_id': MODEL_ID_IMAGE_STREAMS, 'url': _model_url(MODEL_ID_IMAGE_STREAMS), 'ann_data': only_image_stream.SerializeToString(), 'part': 0},
        # should see all treams: web, alias, image
        {
            'model_id': MODEL_ID_ALL_WEB_IMAGE_ALIAS_TITLE_MDESCR_STREAMS,
            'url': _model_url(MODEL_ID_ALL_WEB_IMAGE_ALIAS_TITLE_MDESCR_STREAMS),
            'ann_data': image_all_streams.SerializeToString(),
            'part': 0
        },
    ]

    return YtTableResource(yt_stuff, model_image_path, data=rows, sort_key="model_id", attributes={'schema': schema})


@pytest.fixture(scope='module')
def model_streams_tables(model_image_stream_table):
    return [model_image_stream_table]


# web table with streams and offers table
@pytest.fixture(scope='module')
def input_data(web_streams_table,
               input_shard,
               input_path,
               model_aliases_table,
               model_titles_table,
               marketing_descriptions_table,
               model_streams_tables,
               micro_model_descriptions_table,
               cpa_queries_table):
    res = YTStreamsData(web_streams=web_streams_table,
                        input_data=input_shard,
                        input_path=input_path,
                        model_alias_input_table=model_aliases_table,
                        titles_input_table=model_titles_table,
                        custom_streams_input_tables=model_streams_tables,
                        marketing_descriptions_input_table=marketing_descriptions_table,
                        micro_model_descriptions_input_table=micro_model_descriptions_table,
                        cpa_queries_input_table=cpa_queries_table)
    return res


# output info and YT tables
@pytest.fixture(scope='module')
def stream_output():
    parts_count = 0
    working_path = "//indexer/streams/tmp"
    streams_path = "//indexer/streams/ann_data"
    output_path = streams_path

    return YtStreamsOutput(working_path, streams_path, output_path, parts_count, is_model=True)


# Execution of binary

@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data, stream_output):
    resources = {
        "input": input_data,
        "output": stream_output
    }

    with YtStreamsTestEnv(**resources) as env:
        env.execute(yt_stuff, is_models=True)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_shard_table(workflow):
    return workflow.result_tables[0]


def test_result_table_exist(yt_stuff, result_shard_table):
    assert_that(yt_stuff.get_yt_client().exists(result_shard_table.get_path()), 'Table doen\'t exist')


# Result data

@pytest.fixture(scope='module')
def result_stream_both_aliases_and_streams():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="alias11_1",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MarketModelAlias=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="alias11_2",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MarketModelAlias=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="BrowserPageRank",
                       Data=[TRegionData(Region=0, BrowserPageRank=TFloatStream(Value=0.5))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="BrowserPageRankSample",
                       Data=[TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.7))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def result_stream_both_one_alias():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="alias77",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MarketModelAlias=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="BrowserPageRank",
                       Data=[TRegionData(Region=0, BrowserPageRank=TFloatStream(Value=0.5))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="BrowserPageRankSample",
                       Data=[TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.7))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def result_stream_only_aliases():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="alias7_1 only aliases",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MarketModelAlias=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="alias7_2 only aliases",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MarketModelAlias=1))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def result_stream_all_web_image_alias_title_mdescr():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="alias999 all streams",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MarketModelAlias=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="title999",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MarketTitle=TDummyData()))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="another marketing descr",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MarketingDescription=TDummyData()))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="ImageQueryClicks all streams",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(ImageQueryClicks=0.11))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="FirstClickDtXf",
                       Data=[TRegionData(Region=0, FirstClickDtXf=TFloatStream(Value=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="BrowserPageRankSample",
                       Data=[TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.7))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def result_stream_image_streams():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="ImageDocDwellTime only images",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(ImageDocDwellTime=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="ImageAltData only images",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(ImageAltData=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def result_stream_marketing_description_streams():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="marketing descr",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MarketingDescription=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def result_stream_micro_model_descriptions():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="micro model descr",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MicroDescriptionOfModel=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def result_stream_cpa_queries():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="ordered cpa query",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(CPAQuery=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


# Check that result table consist of expected streams
def test_model_alias_streams(result_shard_table,
                             result_stream_both_aliases_and_streams,
                             result_stream_both_one_alias,
                             result_stream_only_aliases,
                             web_streams_pb1,
                             web_streams_pb2,
                             result_stream_image_streams,
                             result_stream_all_web_image_alias_title_mdescr,
                             result_stream_marketing_description_streams,
                             result_stream_micro_model_descriptions,
                             result_stream_cpa_queries):
    assert_that(list(result_shard_table.data), has_items(
        has_entries({
            'url': _model_url(MODEL_ID_ONLY_WEB1),
            'model_id': MODEL_ID_ONLY_WEB1,
            'part': 0,
            'ann_data': web_streams_pb1.SerializeToString(),
        }),
        has_entries({
            'url': _model_url(MODEL_ID_BOTH_ALIASES_AND_STREAMS),
            'model_id': MODEL_ID_BOTH_ALIASES_AND_STREAMS,
            'part': 0,
            'ann_data': result_stream_both_aliases_and_streams.SerializeToString(),
        }),
        has_entries({
            'url': _model_url(MODEL_ID_ONLY_WEB2),
            'model_id': MODEL_ID_ONLY_WEB2,
            'part': 0,
            'ann_data': web_streams_pb2.SerializeToString(),
        }),
        has_entries({
            'url': _model_url(MODEL_ID_ONLY_ALIASES),
            'model_id': MODEL_ID_ONLY_ALIASES,
            'part': 0,
            'ann_data': result_stream_only_aliases.SerializeToString(),
        }),
        has_entries({
            'url': _model_url(MODEL_ID_BOTH_ONE_ALIAS),
            'model_id': MODEL_ID_BOTH_ONE_ALIAS,
            'part': 0,
            'ann_data': result_stream_both_one_alias.SerializeToString(),
        }),
        has_entries({
            'url': _model_url(MODEL_ID_IMAGE_STREAMS),
            'model_id': MODEL_ID_IMAGE_STREAMS,
            'part': 0,
            'ann_data': result_stream_image_streams.SerializeToString(),
        }),
        has_entries({
            'url': _model_url(MODEL_ID_ALL_WEB_IMAGE_ALIAS_TITLE_MDESCR_STREAMS),
            'model_id': MODEL_ID_ALL_WEB_IMAGE_ALIAS_TITLE_MDESCR_STREAMS,
            'part': 0,
            'ann_data': result_stream_all_web_image_alias_title_mdescr.SerializeToString(),
        }),
        has_entries({
            'url': _model_url(MODEL_ID_ONLY_MARKETING_DESCR),
            'model_id': MODEL_ID_ONLY_MARKETING_DESCR,
            'part': 0,
            'ann_data': result_stream_marketing_description_streams.SerializeToString(),
        }),
        has_entries({
            'url': _model_url(MODEL_ID_ONLY_MICRO_MODEL_DESCR),
            'model_id': MODEL_ID_ONLY_MICRO_MODEL_DESCR,
            'part': 0,
            'ann_data': result_stream_micro_model_descriptions.SerializeToString(),
        }),
        has_entries({
            'url': _model_url(MODEL_ID_ONLY_CPA_QUERIES),
            'model_id': MODEL_ID_ONLY_CPA_QUERIES,
            'part': 0,
            'ann_data': result_stream_cpa_queries.SerializeToString(),
        }),
    ), "No expected result streams")


# Check that result table has row and inv fields
def test_result_table_has_row_inv(result_shard_table):
    assert_that(all_of(
        *[data['row'] is not None and data['inv'] is not None for data in list(result_shard_table.data)]
    ), "No row/inv")
