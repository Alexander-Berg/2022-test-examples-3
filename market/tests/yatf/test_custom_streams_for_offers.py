# coding=utf-8
"""
Тест используется для проверки работы подклейки кастомных стримов
к вебовским для офферов в вызове collect_streams.
Проверяется, что кастомные стримы корректно подклеиваются к вебоским стримам.
"""

import pytest

from hamcrest import assert_that, equal_to, has_items, has_entries, all_of, has_length
from yt import wrapper as yt

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.streams.yatf.test_env import YtStreamsTestEnv
from market.idx.streams.yatf.resources.yt_streams_data import YTStreamsData
from market.idx.streams.yatf.resources.yt_streams_output import YtStreamsOutput
from market.idx.yatf.utils.genlog import genlog_table_schema

from kernel.indexann.protos.data_pb2 import (
    TIndexAnnSiteData,
    TAnnotationRec,
    TRegionData,
    TClickMachineData,
    TFloatStream,
    TMarketStreams,
    TDummyData,
    TMarketMetadocStreams,
    TNHopSiteData,
)

from market.idx.streams.yatf.utils import OfferFactory, offer_url, prepared_web_streams_schema


# ware_md5
OFFER_BOTH_CUSTOM_AND_WEB0 = "both0"
OFFER_BOTH_CUSTOM_AND_WEB1 = "both1"
OFFER_ONLY_WEB0 = "web0"
OFFER_ONLY_WEB1 = "web1"
OFFER_ONLY_CUSTOM0 = "custom0"
OFFER_WITHOUT_STREAMS0 = "no_streams0"
OFFER_WITHOUT_STREAMS1 = "no_streams1"
OFFER_TITLE1 = "title1"
OFFER_BLUE_MARKETING_DESCR = "marketing_descr"
OFFER_BLUE_MICRO_MODEL_DESCR = "micro_model_descr"
OFFER_ONLY_WEB_STREAM_FOR_MSKU = "custom_web_stream_for_msku"
FAKE_MSKU_OFFER_TITLE = "msku_with_offer_title"
FAKE_MSKU_OFFER_SEARCH_TEXT = "msku_with_offer_search_text"
OFFER_CPA_QUERY = "cpa_query"

OFFER_VENDOR_NAME = "vendor_name"
OFFER_VENDOR_NAME_FAST_SKU = "vendor_name_fast_sku"
OFFER_VENDOR_NAME_SKU_OFFER = "vendor_name_sku_offer"
OFFER_VENDOR_CODE = "vendor_code"

RUSSIA = 225
LANG_RUS = 1


PREPARED_STREAM_TABLE_SCHEMA = [
    dict(name="ware_md5", type="string"),
    dict(name="region_id", type="uint64"),
    dict(name="url", type="string"),
    dict(name="text", type="string"),
    dict(name="value", type="string"),
    dict(name="part", type="uint64"),
]


@pytest.fixture(scope='module')
def offer_titles_path():
    return "//home/indexer/streams/in/offers/titles_streams"


@pytest.fixture(scope='module')
def offer_titles_table(yt_stuff, offer_titles_path):

    rows = [
        {'ware_md5': OFFER_TITLE1, 'region_id': RUSSIA, 'url': offer_url(OFFER_TITLE1),
         'text': 'title1_0', 'value': '1', 'part': 1},  # should see  streams: title
        {'ware_md5': OFFER_TITLE1, 'region_id': RUSSIA, 'url': offer_url(OFFER_TITLE1),
         'text': 'title1_1', 'value': '1', 'part': 1},  # should see  streams: title
    ]

    return YtTableResource(yt_stuff, offer_titles_path, data=rows, sort_key="ware_md5", attributes={'schema': PREPARED_STREAM_TABLE_SCHEMA})


@pytest.fixture(scope='module')
def cpa_queries_path():
    return "//home/indexer/streams/cpa_queries"


@pytest.fixture(scope='module')
def cpa_queries_table(yt_stuff, cpa_queries_path):
    rows = [
        {'ware_md5': OFFER_CPA_QUERY,
         'region_id': RUSSIA,
         'text': 'ordered cpa query',
         'value': '1',
         'url': offer_url(OFFER_CPA_QUERY),
         'part': 0},  # should see  streams: cpa query
    ]
    return YtTableResource(yt_stuff, cpa_queries_path, data=rows, attributes={'schema': PREPARED_STREAM_TABLE_SCHEMA})


@pytest.fixture(scope='module')
def blue_offer_marketing_descr_path():
    return "//home/indexer/streams/in/offers/marketing_descr_streams"


@pytest.fixture(scope='module')
def blue_offer_marketing_descr_table(yt_stuff, blue_offer_marketing_descr_path):

    rows = [
        {'ware_md5': OFFER_BLUE_MARKETING_DESCR, 'region_id': RUSSIA, 'url': offer_url(OFFER_BLUE_MARKETING_DESCR),
         'text': 'marketing_descr', 'value': '1', 'part': 0},  # should see  streams: marketing_descr
    ]

    return YtTableResource(yt_stuff, blue_offer_marketing_descr_path, data=rows, attributes={'schema': PREPARED_STREAM_TABLE_SCHEMA})


@pytest.fixture(scope='module')
def blue_offer_micro_model_descr_path():
    return "//home/indexer/streams/in/offers/micro_model_descr_streams"


@pytest.fixture(scope='module')
def blue_offer_micro_model_descr_table(yt_stuff, blue_offer_micro_model_descr_path):

    rows = [
        {'ware_md5': OFFER_BLUE_MICRO_MODEL_DESCR, 'region_id': RUSSIA, 'url': offer_url(OFFER_BLUE_MICRO_MODEL_DESCR),
         'text': 'micro_model_descr', 'value': '1', 'part': 1},  # should see  streams: micro_model descr
    ]

    return YtTableResource(yt_stuff, blue_offer_micro_model_descr_path, data=rows, attributes={'schema': PREPARED_STREAM_TABLE_SCHEMA})


@pytest.fixture(scope='module')
def msku_offer_title_streams_path():
    return "//home/indexer/streams/in/offers/msku/offer_titles"


@pytest.fixture(scope='module')
def msku_offer_title_streams_table(yt_stuff, msku_offer_title_streams_path):

    rows = [
        {'ware_md5': FAKE_MSKU_OFFER_TITLE, 'region_id': RUSSIA, 'url': offer_url(FAKE_MSKU_OFFER_TITLE),
         'text': 'msku_offer_titles', 'value': '1', 'part': 0},  # should see  streams: msku_offer_titles
    ]

    return YtTableResource(yt_stuff, msku_offer_title_streams_path, data=rows, attributes={'schema': PREPARED_STREAM_TABLE_SCHEMA})


@pytest.fixture(scope='module')
def msku_offer_search_text_streams_path():
    return "//home/indexer/streams/in/offers/msku/offer_texts"


@pytest.fixture(scope='module')
def msku_offer_search_text_streams_table(yt_stuff, msku_offer_search_text_streams_path):

    rows = [
        {'ware_md5': FAKE_MSKU_OFFER_SEARCH_TEXT, 'region_id': RUSSIA, 'url': offer_url(FAKE_MSKU_OFFER_SEARCH_TEXT),
         'text': 'msku_offer_search_texts', 'value': '1', 'part': 1},  # should see  streams: msku_offer_search_texts
    ]

    return YtTableResource(yt_stuff, msku_offer_search_text_streams_path, data=rows, attributes={'schema': PREPARED_STREAM_TABLE_SCHEMA})


@pytest.fixture(scope='module')
def offer_aliases_path():
    return "//home/indexer/streams/in/offers/custom_streams"


@pytest.fixture(scope='module')
def only_custom_offer_stream():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="ImageDocDwellTime only images stream",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(ImageDocDwellTime=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="only_custom_offer_stream_2",
                       Data=[TRegionData(Region=RUSSIA, ClickMachine=TClickMachineData(CorrectedFrcSLP=1))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def custom_offer_stream_both0():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="custom_offer_stream_0_1",
                       Data=[TRegionData(Region=RUSSIA, ClickMachine=TClickMachineData(RawClicks=0.1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="custom_offer_stream_0_2",
                       Data=[TRegionData(Region=RUSSIA, ClickMachine=TClickMachineData(CorrectedFrcSLP=0.1))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def custom_offer_stream_both1():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="custom_offer_stream_1_1",
                       Data=[TRegionData(Region=RUSSIA, ClickMachine=TClickMachineData(RawClicks=0.2))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="custom_offer_stream_1_2",
                       Data=[TRegionData(Region=RUSSIA, ClickMachine=TClickMachineData(CorrectedFrcSLP=0.2))],
                       TextLanguage=LANG_RUS),
    ])


# Table with prepared custom streams sorted by ware_md5.
@pytest.fixture(scope='module')
def offers_custom_stream_table(yt_stuff, offer_aliases_path, only_custom_offer_stream, custom_offer_stream_both0, custom_offer_stream_both1):
    schema = [dict(name="ware_md5", type="string"),
              dict(name="url", type="string"),
              dict(name="ann_data", type="string"),
              dict(name="part", type="uint64"),
              ]
    rows = [
        # there are both custom and web streams in a result table (shard #0)
        {'ware_md5': OFFER_BOTH_CUSTOM_AND_WEB0, 'url': offer_url(OFFER_BOTH_CUSTOM_AND_WEB0), 'ann_data': custom_offer_stream_both0.SerializeToString(), 'part': 0},
        # there is only custom stream in a result table
        {'ware_md5': OFFER_ONLY_CUSTOM0, 'url': offer_url(OFFER_ONLY_CUSTOM0), 'ann_data': only_custom_offer_stream.SerializeToString(), 'part': 0},
        # there are both custom and web streams in a result table (shard #1)
        {'ware_md5': OFFER_BOTH_CUSTOM_AND_WEB1, 'url': offer_url(OFFER_BOTH_CUSTOM_AND_WEB1), 'ann_data': custom_offer_stream_both1.SerializeToString(), 'part': 1},
    ]

    return YtTableResource(yt_stuff, offer_aliases_path, data=rows, sort_key="ware_md5", attributes={'schema': schema})


@pytest.fixture(scope='module')
def offers_streams_tables(offers_custom_stream_table):
    return [offers_custom_stream_table]


@pytest.fixture(scope='module')
def web_streams_only():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="BrowserPageRank_only",
                       Data=[TRegionData(Region=0, BrowserPageRank=TFloatStream(Value=0.5))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="BrowserPageRankSample_only",
                       Data=[TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.7))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def web_streams_both0():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="FirstClickDtXf_both0",
                       Data=[TRegionData(Region=0, FirstClickDtXf=TFloatStream(Value=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="BrowserPageRankSample_both0",
                       Data=[TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.7))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def web_streams_both1():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="FirstClickDtXf_both1",
                       Data=[TRegionData(Region=0, FirstClickDtXf=TFloatStream(Value=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="BrowserPageRankSample_both1",
                       Data=[TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.7))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def web_streams_for_msku_both0():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="web_streams_for_msku_both0",
                       Data=[
                           TRegionData(Region=0, MarketMetadocStreams=TMarketMetadocStreams(FirstClickDtXf=1, BrowserPageRank=2.1)),
                           TRegionData(Region=1, MarketMetadocNHop=TNHopSiteData(DwellTime=1, IsFinal=1.1, Total=11, ChainLength=12, Position=13)),
                           TRegionData(Region=2, MarketMetadocStreams=TMarketMetadocStreams(LongClickSP=6.7, SimpleClick=7.3)),
                       ],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def web_streams_for_msku_only():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="web_streams_for_msku_only",
                       Data=[
                           TRegionData(Region=0, MarketMetadocStreams=TMarketMetadocStreams(FirstClickDtXf=2, BrowserPageRank=3.1)),
                       ],
                       TextLanguage=LANG_RUS),
    ])


# Table with web streams
@pytest.fixture(scope='module')
def web_streams_table(yt_stuff, web_streams_only, web_streams_both0, web_streams_both1):
    web_streams_path = "//userfeat/exports/web/IndexAnnSourceData"
    # dont prepare web, not needed
    return YtTableResource(yt_stuff, web_streams_path, data=[])


STREAMS_PATH = "//indexer/streams/ann_data"


ANN_DATA_STREAMS_SCHEMA = [
    dict(name="ware_md5", type="string"),
    dict(name="url", type="string"),
    dict(name="ann_data", type="string"),
    dict(name="part", type="uint64"),
]


@pytest.fixture(scope='module')
def prepared_web_streams_table_shard0(yt_stuff, web_streams_only, web_streams_both0):
    web_streams_table_shard1_path = yt.ypath_join(STREAMS_PATH, '0')
    rows = [
        {'ware_md5': OFFER_BOTH_CUSTOM_AND_WEB0, 'url': offer_url(OFFER_BOTH_CUSTOM_AND_WEB0), 'url_not_normalized': offer_url(OFFER_BOTH_CUSTOM_AND_WEB0),
         'ann_data': web_streams_both0.SerializeToString(), 'part': 0, 'table_index': 1, 'msku': 1, 'is_fake_msku_offer': False},  # Both web and custom streams in a result table (shard 0)
        {'ware_md5': OFFER_ONLY_WEB0, 'url': offer_url(OFFER_ONLY_WEB0), 'url_not_normalized': offer_url(OFFER_ONLY_WEB0),
         'ann_data': web_streams_only.SerializeToString(), 'part': 0, 'table_index': 1, 'msku': 0, 'is_fake_msku_offer': False},  # Only web streams in a result table (shard 0)

    ]

    return YtTableResource(yt_stuff, web_streams_table_shard1_path, data=rows, attributes={'schema': prepared_web_streams_schema()})


@pytest.fixture(scope='module')
def prepared_web_streams_table_shard1(yt_stuff, web_streams_only, web_streams_both1):
    web_streams_table_shard1_path = yt.ypath_join(STREAMS_PATH, '1')
    rows = [
        {'ware_md5': OFFER_BOTH_CUSTOM_AND_WEB1, 'url': offer_url(OFFER_BOTH_CUSTOM_AND_WEB1), 'url_not_normalized': offer_url(OFFER_BOTH_CUSTOM_AND_WEB1),
         'ann_data': web_streams_both1.SerializeToString(), 'part': 1, 'table_index': 1, 'msku': 1, 'is_fake_msku_offer': False},  # Both web and custom streams in a result table (shard 1)
        {'ware_md5': OFFER_ONLY_WEB1, 'url': offer_url(OFFER_ONLY_WEB1), 'url_not_normalized': offer_url(OFFER_ONLY_WEB1),
         'ann_data': web_streams_only.SerializeToString(), 'part': 1, 'table_index': 1, 'msku': 0, 'is_fake_msku_offer': False},  # Only web streams in a result table (shard 1)
    ]

    return YtTableResource(yt_stuff, web_streams_table_shard1_path, data=rows, attributes={'schema': prepared_web_streams_schema()})


# Offers YT tables split by shards
@pytest.fixture(scope='module')
def prepared_web_streams_tables(prepared_web_streams_table_shard0, prepared_web_streams_table_shard1):
    return [prepared_web_streams_table_shard0, prepared_web_streams_table_shard1]


OFFERS_WITH_GLUE_PATH = "//indexer/mi3/main/offers_with_glue"


@pytest.fixture(scope='module')
def offer_with_glue_table_shard0(yt_stuff, web_streams_only, web_streams_both0):
    offers_with_glue_shard1_path = yt.ypath_join(OFFERS_WITH_GLUE_PATH, '0000')
    rows = [
        # непоскутченный оффер - попадает в стримы
        {'ware_md5': OFFER_VENDOR_NAME, 'url': offer_url(OFFER_VENDOR_NAME), 'market_sku': 0, 'vendor_name': 'vendor_name'},
        # fast sku оффер - попадает в стримы
        {'ware_md5': OFFER_VENDOR_NAME_FAST_SKU, 'url': offer_url(OFFER_VENDOR_NAME_FAST_SKU),
         'market_sku': 1, 'is_fast_sku': True, 'vendor_name': 'vendor_name_fast_sku'},
    ]

    return YtTableResource(yt_stuff, offers_with_glue_shard1_path, data=rows, attributes={'schema': genlog_table_schema()})


@pytest.fixture(scope='module')
def offer_with_glue_table_shard1(yt_stuff, web_streams_only, web_streams_both1):
    offers_with_glue_shard2_path = yt.ypath_join(OFFERS_WITH_GLUE_PATH, '0001')
    rows = [
        # мску - попадает в стримы
        {'ware_md5': OFFER_BOTH_CUSTOM_AND_WEB1, 'url': offer_url(OFFER_BOTH_CUSTOM_AND_WEB1),
         'market_sku': 1, 'is_fake_msku_offer': True, 'vendor_name': 'vendor_name2'},
        # поскутченный оффер - НЕ попадает в стримы
        {'ware_md5': OFFER_VENDOR_NAME_SKU_OFFER, 'url': offer_url(OFFER_VENDOR_NAME_SKU_OFFER),
         'market_sku': 1, 'is_fake_msku_offer': False, 'vendor_name': 'vendor_name_sku_offer'},
        # поле без аттрибута idx_stream - НЕ попадает в стримы
        {'ware_md5': OFFER_VENDOR_CODE, 'url': offer_url(OFFER_VENDOR_CODE),
         'market_sku': 1, 'is_fake_msku_offer': True, 'vendor_code': 'vendor_code'},
    ]

    return YtTableResource(yt_stuff, offers_with_glue_shard2_path, data=rows, attributes={'schema': genlog_table_schema()})


# Offers YT tables split by shards
@pytest.fixture(scope='module')
def offer_with_glue_tables(offer_with_glue_table_shard0, offer_with_glue_table_shard1):
    return [offer_with_glue_table_shard0, offer_with_glue_table_shard1]


@pytest.fixture(scope='module')
def msku_web_streams_table(yt_stuff, web_streams_for_msku_both0, web_streams_for_msku_only):
    msku_web_streams_table_path = "//home/indexer/streams/in/offers/msku/web"
    rows = [
        {'ware_md5': OFFER_BOTH_CUSTOM_AND_WEB0, 'url': offer_url(OFFER_BOTH_CUSTOM_AND_WEB0),
         'ann_data': web_streams_for_msku_both0.SerializeToString(), 'part': 0},
        {'ware_md5': OFFER_ONLY_WEB_STREAM_FOR_MSKU, 'url': offer_url(OFFER_ONLY_WEB_STREAM_FOR_MSKU),
         'ann_data': web_streams_for_msku_only.SerializeToString(), 'part': 1},
    ]

    return YtTableResource(yt_stuff, msku_web_streams_table_path, data=rows, attributes={'schema': ANN_DATA_STREAMS_SCHEMA})


@pytest.fixture(scope='module')
def input_path():
    return "//home/indexer/streams/offers"


# offers table (shard0) table which is used by collect_stream to obtain associated web streams
@pytest.fixture(scope='module')
def input_shard0(yt_stuff, input_path):
    shard_id = 0
    table_name = "000" + str(shard_id)
    table_path = yt.ypath_join(input_path, table_name)

    offers = []
    offers.append(OfferFactory.offer(offer_url(OFFER_BOTH_CUSTOM_AND_WEB0), shard_id, OFFER_BOTH_CUSTOM_AND_WEB0))
    offers.append(OfferFactory.offer(offer_url(OFFER_ONLY_WEB0), shard_id, OFFER_ONLY_WEB0))
    offers.append(OfferFactory.offer(offer_url(OFFER_ONLY_CUSTOM0), shard_id, OFFER_ONLY_CUSTOM0))
    offers.append(OfferFactory.offer(offer_url(OFFER_WITHOUT_STREAMS0), shard_id, OFFER_WITHOUT_STREAMS0))
    offers.append(OfferFactory.offer(offer_url(OFFER_BLUE_MARKETING_DESCR), shard_id, OFFER_BLUE_MARKETING_DESCR))
    offers.append(OfferFactory.offer(offer_url(FAKE_MSKU_OFFER_TITLE), shard_id, FAKE_MSKU_OFFER_TITLE))
    offers.append(OfferFactory.offer(offer_url(OFFER_CPA_QUERY), shard_id, OFFER_CPA_QUERY))

    shard0 = YtTableResource(yt_stuff, table_path, data=offers)
    return shard0


# offers table (shard1) table which is used by collect_stream to obtain associated web streams
@pytest.fixture(scope='module')
def input_shard1(yt_stuff, input_path):
    shard_id = 1
    table_name = "000" + str(shard_id)
    table_path = yt.ypath_join(input_path, table_name)

    offers = []
    offers.append(OfferFactory.offer(offer_url(OFFER_BOTH_CUSTOM_AND_WEB1), shard_id, OFFER_BOTH_CUSTOM_AND_WEB1))
    offers.append(OfferFactory.offer(offer_url(OFFER_ONLY_WEB1), shard_id, OFFER_ONLY_WEB1))
    offers.append(OfferFactory.offer(offer_url(OFFER_WITHOUT_STREAMS1), shard_id, OFFER_WITHOUT_STREAMS1))
    offers.append(OfferFactory.offer(offer_url(OFFER_TITLE1), shard_id, OFFER_TITLE1))
    offers.append(OfferFactory.offer(offer_url(OFFER_BLUE_MICRO_MODEL_DESCR), shard_id, OFFER_BLUE_MICRO_MODEL_DESCR))
    offers.append(OfferFactory.offer(offer_url(FAKE_MSKU_OFFER_SEARCH_TEXT), shard_id, FAKE_MSKU_OFFER_SEARCH_TEXT))
    offers.append(OfferFactory.offer(offer_url(OFFER_ONLY_WEB_STREAM_FOR_MSKU), shard_id, OFFER_ONLY_WEB_STREAM_FOR_MSKU))

    shard1 = YtTableResource(yt_stuff, table_path, data=offers)
    return shard1


# Offers YT tables split by shards
@pytest.fixture(scope='module')
def input_shards(input_shard0, input_shard1):
    return [input_shard0, input_shard1]


# web table with streams and offers table
@pytest.fixture(scope='module')
def input_data(web_streams_table,
               input_shards,
               input_path,
               offers_streams_tables,
               offer_titles_table,
               blue_offer_marketing_descr_table,
               blue_offer_micro_model_descr_table,
               msku_offer_title_streams_table,
               msku_offer_search_text_streams_table,
               prepared_web_streams_tables,
               msku_web_streams_table,
               cpa_queries_table,
               offer_with_glue_tables):
    res = YTStreamsData(web_streams=web_streams_table,
                        input_data=input_shards,
                        input_path=input_path,
                        model_alias_input_table=None,
                        titles_input_table=offer_titles_table,
                        blue_model_marketing_descriptions_input_table=blue_offer_marketing_descr_table,
                        blue_micro_model_descriptions_input_table=blue_offer_micro_model_descr_table,
                        msku_offer_titles_input_table=msku_offer_title_streams_table,
                        msku_offer_search_texts_input_table=msku_offer_search_text_streams_table,
                        custom_streams_input_tables=offers_streams_tables,
                        prepared_web_streams_tables=prepared_web_streams_tables,
                        custom_streams_input_tables_no_optimization=[msku_web_streams_table],
                        cpa_queries_input_table=cpa_queries_table,
                        offers_with_glue_tables=offer_with_glue_tables,
                        offers_with_glue_path=OFFERS_WITH_GLUE_PATH)
    return res


# output info and YT tables
@pytest.fixture(scope='module')
def stream_output(input_shards):
    working_path = "//indexer/streams/tmp"
    streams_path = STREAMS_PATH
    output_path = "//indexer/streams/factorann"

    return YtStreamsOutput(working_path, streams_path, output_path, len(input_shards))


# Execution of binary

@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data, stream_output):
    resources = {
        "input": input_data,
        "output": stream_output
    }

    with YtStreamsTestEnv(**resources) as env:
        env.execute(yt_stuff, dont_prepare_web=True)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_shard0_table(workflow):
    return workflow.result_tables[0]


@pytest.fixture(scope='module')
def result_shard1_table(workflow):
    return workflow.result_tables[1]


def test_result_table_count(workflow, stream_output):
    assert_that(len(workflow.result_tables), equal_to(stream_output.parts_cnt), "output number shards != input number of shards")


# Result data
@pytest.fixture(scope='module')
def result_stream_both_custom_and_streams0():

    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="custom_offer_stream_0_1",
                       Data=[TRegionData(Region=RUSSIA, ClickMachine=TClickMachineData(RawClicks=0.1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="custom_offer_stream_0_2",
                       Data=[TRegionData(Region=RUSSIA, ClickMachine=TClickMachineData(CorrectedFrcSLP=0.1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="web_streams_for_msku_both0",
                       Data=[
                           TRegionData(Region=0, MarketMetadocStreams=TMarketMetadocStreams(FirstClickDtXf=1, BrowserPageRank=2.1)),
                           TRegionData(Region=1, MarketMetadocNHop=TNHopSiteData(DwellTime=1, IsFinal=1.1, Total=11, ChainLength=12, Position=13)),
                           TRegionData(Region=2, MarketMetadocStreams=TMarketMetadocStreams(LongClickSP=6.7, SimpleClick=7.3)),
                       ],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="FirstClickDtXf_both0",
                       Data=[TRegionData(Region=0, FirstClickDtXf=TFloatStream(Value=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="BrowserPageRankSample_both0",
                       Data=[TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.7))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def result_stream_both_custom_and_streams1():

    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="custom_offer_stream_1_1",
                       Data=[TRegionData(Region=RUSSIA, ClickMachine=TClickMachineData(RawClicks=0.2))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="custom_offer_stream_1_2",
                       Data=[TRegionData(Region=RUSSIA, ClickMachine=TClickMachineData(CorrectedFrcSLP=0.2))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="FirstClickDtXf_both1",
                       Data=[TRegionData(Region=0, FirstClickDtXf=TFloatStream(Value=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="BrowserPageRankSample_both1",
                       Data=[TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.7))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="vendor_name2",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(VendorName=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def title_streams():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="title1_0",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MarketTitle=TDummyData()))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="title1_1",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MarketTitle=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def blue_marketing_descr_streams():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="marketing_descr",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(BlueMarketingDescriptionOfModel=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def blue_micro_model_descr_streams():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="micro_model_descr",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(BlueMicroDescriptionOfModel=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def cpa_queries_streams():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="ordered cpa query",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(CPAQuery=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def msku_offer_title_streams():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="msku_offer_titles",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MskuOfferTitle=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def msku_offer_search_text_streams():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="msku_offer_search_texts",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(MskuOfferSearchText=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def vendor_name_streams():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="vendor_name",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(VendorName=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def vendor_name_fast_sku_streams():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="vendor_name_fast_sku",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(VendorName=TDummyData()))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def msku_web_streams():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="web_streams_for_msku_only",
                       Data=[
                           TRegionData(Region=0, MarketMetadocStreams=TMarketMetadocStreams(FirstClickDtXf=2, BrowserPageRank=3.1)),
                       ],
                       TextLanguage=LANG_RUS),
    ])


# Check that result tables consist of expected streams (shard 0)
def test_offer_custom_stream_shard0(result_shard0_table,
                                    result_stream_both_custom_and_streams0,
                                    only_custom_offer_stream,
                                    web_streams_only,
                                    blue_marketing_descr_streams,
                                    msku_offer_title_streams,
                                    cpa_queries_streams,
                                    vendor_name_streams,
                                    vendor_name_fast_sku_streams):

    assert_that(list(result_shard0_table.data), has_items(
        has_entries({
            'url': offer_url(OFFER_BOTH_CUSTOM_AND_WEB0),
            'ware_md5': OFFER_BOTH_CUSTOM_AND_WEB0,
            'part': 0,
            'ann_data': result_stream_both_custom_and_streams0.SerializeToString(),
        }),
        has_entries({
            'url': offer_url(OFFER_ONLY_CUSTOM0),
            'ware_md5': OFFER_ONLY_CUSTOM0,
            'part': 0,
            'ann_data': only_custom_offer_stream.SerializeToString(),
        }),
        has_entries({
            'url': offer_url(OFFER_ONLY_WEB0),
            'ware_md5': OFFER_ONLY_WEB0,
            'part': 0,
            'ann_data': web_streams_only.SerializeToString(),
        }),
        has_entries({
            'url': offer_url(OFFER_BLUE_MARKETING_DESCR),
            'ware_md5': OFFER_BLUE_MARKETING_DESCR,
            'part': 0,
            'ann_data': blue_marketing_descr_streams.SerializeToString(),
        }),
        has_entries({
            'url': offer_url(FAKE_MSKU_OFFER_TITLE),
            'ware_md5': FAKE_MSKU_OFFER_TITLE,
            'part': 0,
            'ann_data': msku_offer_title_streams.SerializeToString(),
        }),
        has_entries({
            'url': offer_url(OFFER_CPA_QUERY),
            'ware_md5': OFFER_CPA_QUERY,
            'part': 0,
            'ann_data': cpa_queries_streams.SerializeToString(),
        }),
        has_entries({
            'url': offer_url(OFFER_VENDOR_NAME),
            'ware_md5': OFFER_VENDOR_NAME,
            'part': 0,
            'ann_data': vendor_name_streams.SerializeToString(),
        }),
        has_entries({
            'url': offer_url(OFFER_VENDOR_NAME_FAST_SKU),
            'ware_md5': OFFER_VENDOR_NAME_FAST_SKU,
            'part': 0,
            'ann_data': vendor_name_fast_sku_streams.SerializeToString(),
        }),
    ), "No expected result streams")
    assert_that(list(result_shard0_table.data), has_length(8))


# Check that result tables consist of expected streams (shard 1)
def test_offer_custom_stream_shard1(result_shard1_table,
                                    result_stream_both_custom_and_streams1,
                                    web_streams_only,
                                    title_streams,
                                    blue_micro_model_descr_streams,
                                    msku_offer_search_text_streams,
                                    msku_web_streams):

    assert_that(list(result_shard1_table.data), has_items(
        has_entries({
            'url': offer_url(OFFER_BOTH_CUSTOM_AND_WEB1),
            'ware_md5': OFFER_BOTH_CUSTOM_AND_WEB1,
            'part': 1,
            'ann_data': result_stream_both_custom_and_streams1.SerializeToString(),
        }),
        has_entries({
            'url': offer_url(OFFER_TITLE1),
            'ware_md5': OFFER_TITLE1,
            'part': 1,
            'ann_data': title_streams.SerializeToString(),
        }),
        has_entries({
            'url': offer_url(OFFER_ONLY_WEB1),
            'ware_md5': OFFER_ONLY_WEB1,
            'part': 1,
            'ann_data': web_streams_only.SerializeToString(),
        }),
        has_entries({
            'url': offer_url(OFFER_BLUE_MICRO_MODEL_DESCR),
            'ware_md5': OFFER_BLUE_MICRO_MODEL_DESCR,
            'part': 1,
            'ann_data': blue_micro_model_descr_streams.SerializeToString(),
        }),
        has_entries({
            'url': offer_url(FAKE_MSKU_OFFER_SEARCH_TEXT),
            'ware_md5': FAKE_MSKU_OFFER_SEARCH_TEXT,
            'part': 1,
            'ann_data': msku_offer_search_text_streams.SerializeToString(),
        }),
        has_entries({
            'url': offer_url(OFFER_ONLY_WEB_STREAM_FOR_MSKU),
            'ware_md5': OFFER_ONLY_WEB_STREAM_FOR_MSKU,
            'part': 1,
            'ann_data': msku_web_streams.SerializeToString(),
        })
    ), "No expected result streams")
    assert_that(list(result_shard1_table.data), has_length(6))


# Check that result table has row and inv fields(shard 0)
def test_result_table_has_row_inv_shard0(result_shard0_table):
    assert_that(all_of(
        *[data['row'] is not None and data['inv'] is not None for data in list(result_shard0_table.data)]
    ), "No row/inv")


# Check that result table has row and inv fields(shard 1)
def test_result_table_has_row_inv_shard1(result_shard1_table):
    assert_that(all_of(
        *[data['row'] is not None and data['inv'] is not None for data in list(result_shard1_table.data)]
    ), "No row/inv")
