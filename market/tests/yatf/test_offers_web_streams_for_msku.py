# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to, has_entries, has_key

from market.idx.streams.src.streams_converter.yatf.test_env import StreamsConverterTestEnv, StreamsConverterMode
from market.idx.streams.src.streams_converter.yatf.resources.streams_converter_input import JoinAnnDataStreamsToMskuInput
from market.idx.streams.src.streams_converter.yatf.resources.streams_converter_output import JoinAnnDataStreamsToMskuOutput
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.streams.yatf.utils import offer_url, prepared_web_streams_schema

from yt import wrapper as yt

from kernel.indexann.protos.data_pb2 import (
    TIndexAnnSiteData,
    TAnnotationRec,
    TRegionData,
    TFloatStream,
    TMarketMetadocStreams,
    TClickMachineData,
    TNHopSiteData,
)

from mapreduce.yt.python.table_schema import extract_column_attributes


RUSSIA = 225
LANG_RUS = 1

# ware_md5
OFFER0_MSKU1 = "offer0_msku1"
OFFER1_MSKU1 = "offer1_msku1"
OFFER2_MSKU1_DUPLICATE_FEATURES = "offer2_msku1"
MSKU1 = "msku1"

OFFER3_MSKU2_MSKU_NOT_PRESENT = "offer3_msku2_not_present"
MSKU2 = "msku2"

OFFER4_MSKU_ABSENT = "offer4_msku_absent"

MSKU3 = "msku3_no_offers"

MSKU4 = "msku4_empty_web"
OFFER5_MSKU4_EMPTY_WEB = "offer5_msku4_empty_web"
OFFER6_MSKU4_EMPTY_TEXT = "offer6_msku4_empty_text"
OFFER7_MSKU4_EMPTY_REG_DATA = "offer7_msku4_empty_reg_data"

OFFER8_MSKU5 = "offer8_msku5"
OFFER9_MSKU5 = "offer9_msku5"
MSKU5 = "msku5"


"""
Тест проверяет YT джобу, которая джойнит офферные веб стримы по мску и кладет в отдельную таблицу со стримами для fake-msku офферов.
Дубликаты по тексту выкидываются, остается одна запись с максимальным кол-вом факторов
"""


@pytest.fixture(scope='module')
def web_streams0():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="useful factors part 0",   # filter, because same text but less factors
                       Data=[
                           TRegionData(Region=0, FirstClickDtXf=TFloatStream(Value=2), BrowserPageRank=TFloatStream(Value=3.1)),
                           TRegionData(Region=1, NHop=TNHopSiteData(DwellTime=1, IsFinal=1.1, Total=11, ChainLength=12, Position=13)),
                           TRegionData(Region=2, ClickMachine=TClickMachineData(LongClickSP=6.7)),
                       ],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="useful factors part 0",
                       Data=[
                           TRegionData(Region=0, FirstClickDtXf=TFloatStream(Value=1), BrowserPageRank=TFloatStream(Value=2.1)),
                           TRegionData(Region=1, NHop=TNHopSiteData(DwellTime=1, IsFinal=1.1, Total=11, ChainLength=12, Position=13)),
                           TRegionData(Region=2, ClickMachine=TClickMachineData(LongClickSP=6.7, SimpleClick=7.3)),
                       ],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="filter out unuseful factors",
                       Data=[
                           TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.7), BannerPhraseXFactor=TFloatStream(Value=0.8)),
                           TRegionData(Region=1, BannerCommQueriesXF=TFloatStream(Value=0.9)),
                       ],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="unuseful factors, filter out all TAnnotationRec",
                       Data=[TRegionData(Region=2, BannerPhraseXFactor=TFloatStream(Value=0.8))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def web_streams1():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="useful factors part 1",
                       Data=[
                           TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=3.2)),
                           TRegionData(Region=1, ClickMachine=TClickMachineData(SplitDt=2.3, LongClick=3.3, OneClick=4.5)),
                       ],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="Language = 2",
                       Data=[TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.8))],
                       TextLanguage=2),
        TAnnotationRec(Text="Empty language",
                       Data=[TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.8))]),
        TAnnotationRec(Text="unuseful factors, filter out all TAnnotationRec",
                       Data=[TRegionData(Region=2, BannerCommQueriesXF=TFloatStream(Value=0.9))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def web_streams_msku_will_be_filtered():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="msku factors",
                       Data=[
                           TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=4.4)),
                           TRegionData(Region=1, ClickMachine=TClickMachineData(SplitDt=5.5, LongClick=6.6, OneClick=7.7)),
                       ],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="msku factors 2",
                       Data=[TRegionData(Region=0, BrowserPageRankSample=TFloatStream(Value=0.8))],
                       TextLanguage=2),
    ])


@pytest.fixture(scope='module')
def web_streams_empty():
    return TIndexAnnSiteData(Recs=[])


@pytest.fixture(scope='module')
def web_streams_empty_text():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="",
                       Data=[TRegionData(Region=0, FirstClickDtXf=TFloatStream(Value=2))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Data=[TRegionData(Region=1, FirstClickDtXf=TFloatStream(Value=2))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def web_streams_empty_reg_data():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="FirstClickDtXf1",
                       Data=[],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="FirstClickDtXf1",
                       TextLanguage=LANG_RUS),
    ])


# expected data
@pytest.fixture(scope='module')
def web_streams0_expected():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="useful factors part 0",
                       Data=[
                           TRegionData(Region=0, MarketMetadocStreams=TMarketMetadocStreams(FirstClickDtXf=1, BrowserPageRank=2.1)),
                           TRegionData(Region=1, MarketMetadocNHop=TNHopSiteData(DwellTime=1, IsFinal=1.1, Total=11, ChainLength=12, Position=13)),
                           TRegionData(Region=2, MarketMetadocStreams=TMarketMetadocStreams(LongClickSP=6.7, SimpleClick=7.3)),
                       ],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="filter out unuseful factors",
                       Data=[
                           TRegionData(Region=0, MarketMetadocStreams=TMarketMetadocStreams(BrowserPageRankSample=0.7)),
                       ],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def web_streams1_expected():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="useful factors part 1",
                       Data=[
                           TRegionData(Region=0, MarketMetadocStreams=TMarketMetadocStreams(BrowserPageRankSample=3.2)),
                           TRegionData(Region=1, MarketMetadocStreams=TMarketMetadocStreams(SplitDt=2.3, LongClick=3.3, OneClick=4.5)),
                       ],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="Language = 2",
                       Data=[TRegionData(Region=0, MarketMetadocStreams=TMarketMetadocStreams(BrowserPageRankSample=0.8))],
                       TextLanguage=2),
        TAnnotationRec(Text="Empty language",
                       Data=[TRegionData(Region=0, MarketMetadocStreams=TMarketMetadocStreams(BrowserPageRankSample=0.8))]),
    ])


@pytest.fixture(scope='module')
def msku_expected_data(web_streams0_expected, web_streams1_expected):
    return {
        MSKU1: {
            'data': {'msku': 1, 'part': 0, 'url': offer_url(MSKU1)},
            'web_streams': [web_streams0_expected.Recs, web_streams1_expected.Recs],
        },
        MSKU5: {
            'data': {'msku': 5, 'part': 1, 'url': offer_url(MSKU5)},
            'web_streams': [web_streams0_expected.Recs],
        },
    }


STREAMS_PATH = "//indexer/streams/ann_data"


@pytest.fixture(scope='module')
def prepared_web_streams_table_shard0(yt_stuff, web_streams0, web_streams1, web_streams_empty, web_streams_empty_text,
                                      web_streams_empty_reg_data, web_streams_msku_will_be_filtered):
    web_streams_table_shard1_path = yt.ypath_join(STREAMS_PATH, '0')
    rows = [
        {'ware_md5': OFFER0_MSKU1, 'url': offer_url(OFFER0_MSKU1), 'url_not_normalized': offer_url(OFFER0_MSKU1),
         'ann_data': web_streams0.SerializeToString(), 'part': 0, 'table_index': 1, 'msku': 1, 'is_fake_msku_offer': False},
        {'ware_md5': OFFER1_MSKU1, 'url': offer_url(OFFER1_MSKU1), 'url_not_normalized': offer_url(OFFER1_MSKU1),
         'ann_data': web_streams1.SerializeToString(), 'part': 0, 'table_index': 1, 'msku': 1, 'is_fake_msku_offer': False},
        {'ware_md5': OFFER2_MSKU1_DUPLICATE_FEATURES, 'url': offer_url(OFFER2_MSKU1_DUPLICATE_FEATURES), 'url_not_normalized': offer_url(OFFER2_MSKU1_DUPLICATE_FEATURES),
         'ann_data': web_streams0.SerializeToString(), 'part': 0, 'table_index': 1, 'msku': 1, 'is_fake_msku_offer': False},
        {'ware_md5': MSKU1, 'url': offer_url(MSKU1), 'url_not_normalized': offer_url(MSKU1),
         'ann_data': web_streams_msku_will_be_filtered.SerializeToString(), 'part': 0, 'table_index': 1, 'msku': 1, 'is_fake_msku_offer': True},

        {'ware_md5': OFFER4_MSKU_ABSENT, 'url': offer_url(OFFER4_MSKU_ABSENT), 'url_not_normalized': offer_url(OFFER4_MSKU_ABSENT),
         'ann_data': web_streams0.SerializeToString(), 'part': 1, 'table_index': 1, 'msku': None, 'is_fake_msku_offer': False},

        {'ware_md5': MSKU4, 'url': offer_url(MSKU4), 'url_not_normalized': offer_url(MSKU4),
         'ann_data': '', 'part': 0, 'table_index': 1, 'msku': 4, 'is_fake_msku_offer': True},
        {'ware_md5': OFFER5_MSKU4_EMPTY_WEB, 'url': offer_url(OFFER5_MSKU4_EMPTY_WEB), 'url_not_normalized': offer_url(OFFER5_MSKU4_EMPTY_WEB),
         'ann_data': web_streams_empty.SerializeToString(), 'part': 0, 'table_index': 1, 'msku': 4, 'is_fake_msku_offer': False},
        {'ware_md5': OFFER6_MSKU4_EMPTY_TEXT, 'url': offer_url(OFFER6_MSKU4_EMPTY_TEXT), 'url_not_normalized': offer_url(OFFER6_MSKU4_EMPTY_TEXT),
         'ann_data': web_streams_empty_text.SerializeToString(), 'part': 0, 'table_index': 1, 'msku': 4, 'is_fake_msku_offer': False},
        {'ware_md5': OFFER7_MSKU4_EMPTY_REG_DATA, 'url': offer_url(OFFER7_MSKU4_EMPTY_REG_DATA), 'url_not_normalized': offer_url(OFFER7_MSKU4_EMPTY_REG_DATA),
         'ann_data': web_streams_empty_reg_data.SerializeToString(), 'part': 0, 'table_index': 1, 'msku': 4, 'is_fake_msku_offer': False},

    ]
    return YtTableResource(yt_stuff, web_streams_table_shard1_path, data=rows, attributes={'schema': prepared_web_streams_schema()})


@pytest.fixture(scope='module')
def prepared_web_streams_table_shard1(yt_stuff, web_streams0, web_streams_empty_reg_data):
    web_streams_table_shard1_path = yt.ypath_join(STREAMS_PATH, '1')
    rows = [
        {'ware_md5': OFFER3_MSKU2_MSKU_NOT_PRESENT, 'url': offer_url(OFFER3_MSKU2_MSKU_NOT_PRESENT), 'url_not_normalized': offer_url(OFFER3_MSKU2_MSKU_NOT_PRESENT),
         'ann_data': web_streams0.SerializeToString(), 'part': 1, 'table_index': 1, 'msku': 2, 'is_fake_msku_offer': False},
        {'ware_md5': MSKU2, 'url': offer_url(MSKU2), 'url_not_normalized': offer_url(MSKU2),
         'ann_data': '', 'part': 1, 'table_index': 1, 'msku': 2, 'is_fake_msku_offer': True},

        {'ware_md5': MSKU3, 'url': offer_url(MSKU3), 'url_not_normalized': offer_url(MSKU3),
         'ann_data': '', 'part': 1, 'table_index': 1, 'msku': 3, 'is_fake_msku_offer': True},

        {'ware_md5': OFFER8_MSKU5, 'url': offer_url(OFFER8_MSKU5), 'url_not_normalized': offer_url(OFFER8_MSKU5),
         'ann_data': web_streams0.SerializeToString(), 'part': 1, 'table_index': 1, 'msku': 5, 'is_fake_msku_offer': False},
        {'ware_md5': OFFER9_MSKU5, 'url': offer_url(OFFER9_MSKU5), 'url_not_normalized': offer_url(OFFER9_MSKU5),
         'ann_data': web_streams_empty_reg_data.SerializeToString(), 'part': 1, 'table_index': 1, 'msku': 5, 'is_fake_msku_offer': False},
        {'ware_md5': MSKU5, 'url': offer_url(MSKU5), 'url_not_normalized': offer_url(MSKU5),
         'ann_data': '', 'part': 1, 'table_index': 1, 'msku': 5, 'is_fake_msku_offer': True},
    ]
    return YtTableResource(yt_stuff, web_streams_table_shard1_path, data=rows, attributes={'schema': prepared_web_streams_schema()})


def sharded_blue_urls_as_in_white_schema():
    return [
        dict(name="ware_md5", type="string"),
        dict(name="url", type="string"),
        dict(name="part", type="uint64"),
        dict(name="msku", type="int64"),
        dict(name="is_fake_msku_offer", type="boolean"),
    ]


@pytest.fixture(scope='module')
def sharded_blue_urls_as_in_white(yt_stuff):
    blue_urls_path = yt.ypath_join('//indexer/blue_urls')
    rows = [
        {'ware_md5': MSKU1, 'url': offer_url(MSKU1), 'part': 0, 'msku': 1, 'is_fake_msku_offer': True},
        {'ware_md5': MSKU4, 'url': offer_url(MSKU4), 'part': 0, 'msku': 4, 'is_fake_msku_offer': True},
        {'ware_md5': MSKU2, 'url': offer_url(MSKU2), 'part': 1, 'msku': 2, 'is_fake_msku_offer': False},
        {'ware_md5': MSKU3, 'url': offer_url(MSKU3), 'part': 1, 'msku': 3, 'is_fake_msku_offer': True},
        {'ware_md5': MSKU5, 'url': offer_url(MSKU5), 'part': 1, 'msku': 5, 'is_fake_msku_offer': True},
    ]
    return YtTableResource(yt_stuff, blue_urls_path, data=rows, attributes={'schema': sharded_blue_urls_as_in_white_schema()})


# Offers YT tables split by shards
@pytest.fixture(scope='module')
def prepared_web_streams_tables(prepared_web_streams_table_shard0, prepared_web_streams_table_shard1):
    return [prepared_web_streams_table_shard0, prepared_web_streams_table_shard1]


@pytest.fixture(scope='module')
def input_data(prepared_web_streams_tables, sharded_blue_urls_as_in_white):
    return JoinAnnDataStreamsToMskuInput(sharded_blue_urls_as_in_white, STREAMS_PATH, prepared_web_streams_tables)


# output info and YT tables
@pytest.fixture(scope='module')
def output_data():
    dst_path = '//indexer/streams/offers/msku/web'
    return JoinAnnDataStreamsToMskuOutput(dst_path)


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data, output_data):
    resources = {
        "input": input_data,
        "output": output_data,
    }

    with StreamsConverterTestEnv(**resources) as env:
        env.execute(StreamsConverterMode.JOIN_ANN_DATA_STREAMS_TO_MSKU, yt_stuff)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def offer_web_stream_for_msku_table(workflow):
    return workflow.outputs.get('offer_web_stream_for_msku_table')


@pytest.fixture(scope='module')
def offer_web_stream_for_msku_data(workflow):
    return workflow.outputs.get('offer_web_stream_for_msku_data')


def check_result_table_schema(table):
    assert_that(extract_column_attributes(list(table.schema)),
                equal_to([
                    {'required': False, "name": "ware_md5", "type": "string"},
                    {'required': False, "name": "part", "type": "uint64"},
                    {'required': False, "name": "ann_data", "type": "string"},
                    {'required': False, "name": "url", "type": "string"},
                    {'required': False, "name": "msku", "type": "int64"},
                ]), "Schema is incorrect")


def test_offer_titles_stream_table_schema(offer_web_stream_for_msku_table):
    check_result_table_schema(offer_web_stream_for_msku_table)


def test_offer_titles_stream_table_len(offer_web_stream_for_msku_data, msku_expected_data, yt_stuff):
    assert_that(len(offer_web_stream_for_msku_data), equal_to(len(msku_expected_data)), 'Wrong count of msku with web features')


def _check_web_features_present_in_any_order(expexted_proto_recs, actual_str):
    actual_proto = TIndexAnnSiteData()
    actual_proto.ParseFromString(actual_str)

    actual_list = [rec.SerializeToString() for rec in actual_proto.Recs]

    expected_list = []
    for expexted_proto_rec in expexted_proto_recs:
        expected_list.extend([rec.SerializeToString() for rec in expexted_proto_rec])
    assert_that(frozenset(actual_list), equal_to(frozenset(expected_list)), 'No expected msku web streams')


# Check that result tables consist of expected streams
@pytest.mark.parametrize('msku', [MSKU1, MSKU5])
def test_msku_streams(offer_web_stream_for_msku_data, msku_expected_data, msku):

    assert_that(offer_web_stream_for_msku_data, has_key(msku), "No expected msku")
    assert_that(offer_web_stream_for_msku_data[msku], has_entries(msku_expected_data[msku]["data"]), "No expected msku data")

    actual_ann_data_str = offer_web_stream_for_msku_data[msku]["ann_data"]
    _check_web_features_present_in_any_order(msku_expected_data[msku]["web_streams"], actual_ann_data_str)
