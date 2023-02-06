# coding=utf-8
"""
Тест используется для проверки пайплайна аннотационного индекса в YT.
"""


import pytest

from hamcrest import assert_that, equal_to
from market.idx.offers.yatf.utils.fixtures import default_offer
from yt import wrapper as yt

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from mrindexann.yatf.resources.yt_streams_data import YTStreamsData

from mrindexann.yatf.mrann_env import YtMRAnnTestEnv
from mrindexann.yatf.mergerann_env import YtMergeAnnTestEnv
from mrindexann.yatf.remapann_env import YtRemapAnnTestEnv
from mrindexann.yatf.userdata_view import UserDataTestEnv

from kernel.indexann.protos.data_pb2 import TIndexAnnSiteData, TAnnotationRec, TRegionData, TClickMachineData, TMarketStreams, TFloatStream, TDummyData

from market.idx.yatf.matchers.env_matchers import HasOutputFiles

assert yt


merged_exts = ["inv", "key"]
exts = ["data.wad", "inv", "key", "sent"]


def _offer_url(id):
    return "handsome.gromov.ru/" + id


OFFER_0 = "jaPRZC2qhM8tmb0yQQapzA"
OFFER_1 = "cxlZ3cTeKNxAzS6OMWX51g"
OFFER_2 = "WBxjfubDtEU49gfweJaA1Q"
OFFER_3 = "XwMatVDskhEwoWXenZRifQ"
OFFER_WITHOUT_STREAMS = "wymqSwv6wBsx5XlChPmrwQ"

RUSSIA = 225
LANG_RUS = 1


# Sorted in right order streams.
# doc_id = 1 is offer2 (marker is "DT_FIRST_CLICK_DT_XF":25)
# doc_id = 3 is offer0
# doc_id = 4 is offer1 (marker is "DT_FIRST_CLICK_DT_XF":255)
# offer3 was removed by market filter
# offer4 isn't in index cause it doesn't have any streams
def user_data():
    return '''{
  "Breaks":
    [
      {
        "Id":1,
        "Regions":
          [
            {
              "DT_BQPR_SAMPLE":178,
              "Region":225
            }
          ]
      },
      {
        "Id":2,
        "Regions":
          [
            {
              "DT_FIRST_CLICK_DT_XF":25,
              "Region":225
            }
          ]
      }
    ],
  "DocId":1
}
{
  "Breaks":
    [
      {
        "Id":1,
        "Regions":
          [
            {
              "DT_BQPR":127,
              "Region":225
            }
          ]
      },
      {
        "Id":2,
        "Regions":
          [
            {
              "DT_BQPR_SAMPLE":178,
              "Region":225
            }
          ]
      },
      {
        "Id":3,
        "Regions":
          [
            {
              "DT_MRKT_IMG_LINK_DATA":255,
              "Region":225
            }
          ]
      },
      {
        "Id":4,
        "Regions":
          [
            {
              "DT_MRKT_IMG_QUERY_DWELL_TIME":178,
              "Region":225
            }
          ]
      }
    ],
  "DocId":3
}
{
  "Breaks":
    [
      {
        "Id":1,
        "Regions":
          [
            {
              "DT_BQPR_SAMPLE":178,
              "Region":225
            }
          ]
      },
      {
        "Id":2,
        "Regions":
          [
            {
              "DT_FIRST_CLICK_DT_XF":255,
              "Region":225
            }
          ]
      }
    ],
  "DocId":4
}
'''


@pytest.fixture(scope='module')
def feed():
    offer0 = default_offer(
        ware_md5=OFFER_0,
        URL=_offer_url(OFFER_0),
    )

    offer1 = default_offer(
        URL=_offer_url(OFFER_1),
        ware_md5=OFFER_1,
    )

    offer2 = default_offer(
        URL=_offer_url(OFFER_2),
        ware_md5=OFFER_2,
    )

    offer3 = default_offer(
        URL=_offer_url(OFFER_3),
        ware_md5=OFFER_3,
    )

    offer4 = default_offer(
        URL=_offer_url(OFFER_WITHOUT_STREAMS),
        ware_md5=OFFER_WITHOUT_STREAMS,
    )

    return [offer3, offer2, offer4, offer0, offer1]


@pytest.fixture(scope='module')
def web_streams0():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="Хорошо",
                       Data=[TRegionData(Region=RUSSIA, BrowserPageRank=TFloatStream(Value=0.5))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="Плохо",
                       Data=[TRegionData(Region=RUSSIA, BrowserPageRankSample=TFloatStream(Value=0.7))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="ImageDocDwellTime only images",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(ImageLinkData=TDummyData()))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="ImageQueryDwellTime only images",
                       Data=[TRegionData(Region=RUSSIA, MarketStreams=TMarketStreams(ImageQueryDwellTime=0.7))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def web_streams1():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="Нормально",
                       Data=[TRegionData(Region=RUSSIA, FirstClickDtXf=TFloatStream(Value=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="Отлично",
                       Data=[TRegionData(Region=RUSSIA, BrowserPageRankSample=TFloatStream(Value=0.7))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def web_streams2():
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="Улётно",
                       Data=[TRegionData(Region=RUSSIA, FirstClickDtXf=TFloatStream(Value=0.1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="Интересно",
                       Data=[TRegionData(Region=RUSSIA, BrowserPageRankSample=TFloatStream(Value=0.7))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def web_streams3():  # won't be in factor index cause TMarketFactorProfile should filter it
    return TIndexAnnSiteData(Recs=[
        TAnnotationRec(Text="Вкусно",
                       Data=[TRegionData(Region=RUSSIA, ClickMachine=TClickMachineData(RawClicks=1))],
                       TextLanguage=LANG_RUS),
        TAnnotationRec(Text="пролетая",
                       Data=[TRegionData(Region=RUSSIA, ClickMachine=TClickMachineData(CorrectedFrcSLP=1))],
                       TextLanguage=LANG_RUS),
    ])


@pytest.fixture(scope='module')
def yt_indexfactorann_path():
    return "//market/indexer/streams/offers/indexfactorann/0"


# Table with web streams
@pytest.fixture(scope='module')
def web_streams_table(yt_stuff, web_streams0, web_streams1, web_streams2, web_streams3):
    web_streams_path = "//market/indexer/streams/offers/ann_data/0"
    rows = [
        {'url': _offer_url(OFFER_0), 'ware_md5': OFFER_0, 'ann_data': web_streams0.SerializeToString(), 'part': 0},
        {'url': _offer_url(OFFER_1), 'ware_md5': OFFER_1, 'ann_data': web_streams1.SerializeToString(), 'part': 0},
        {'url': _offer_url(OFFER_2), 'ware_md5': OFFER_2, 'ann_data': web_streams2.SerializeToString(), 'part': 0},
        {'url': _offer_url(OFFER_3), 'ware_md5': OFFER_3, 'ann_data': web_streams3.SerializeToString(), 'part': 0},
    ]

    return YtTableResource(yt_stuff, web_streams_path, data=rows)


# web table with streams
@pytest.fixture(scope='module')
def input_data(web_streams_table):
    res = YTStreamsData(web_streams_table)

    return res


# Execution of binary
@pytest.yield_fixture(scope="module")
def idx(feed):
    # with OffersIndexerTestEnv(**resources) as env:
    # env.execute()
    # yield env

    # must be reimplemented with MrMindexer
    return None


@pytest.yield_fixture(scope='module')
def mr_ann_idx(yt_stuff, input_data, yt_indexfactorann_path):
    resources = {
        "input": input_data
    }

    with YtMRAnnTestEnv(**resources) as env:
        env.execute(yt_stuff, yt_indexfactorann_path)
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def merge_ann_idx(yt_stuff, mr_ann_idx, idx):
    resources = {
    }

    with YtMergeAnnTestEnv(**resources) as env:
        env.execute(yt_stuff, idx.index_dir, mr_ann_idx.table_path)
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def remap_ann_idx(yt_stuff, mr_ann_idx, merge_ann_idx, idx):
    resources = {
    }

    with YtRemapAnnTestEnv(**resources) as env:
        env.execute(yt_stuff, idx.index_dir, mr_ann_idx.table_path, merge_ann_idx.ann_prefix)
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def user_data_res(remap_ann_idx):
    resources = {
    }

    with UserDataTestEnv(**resources) as env:
        env.execute(remap_ann_idx.result_prefix)
        env.verify()
        yield env


# Check that mergerann has been generated necessary index files
@pytest.mark.skip(reason='MARKETINDEXER-41733 please fix me')
def test_merge_ann_idx_files(merge_ann_idx):
    files = set()
    for ext in merged_exts:
        for p in range(0, 8):
            files.add(merge_ann_idx.index_prefix + str(p) + ".0" + ext)

    matcher = HasOutputFiles(files)
    merge_ann_idx.verify([matcher])


# Check that remapann has been generated necessary index files
@pytest.mark.skip(reason='MARKETINDEXER-41733 please fix me')
def test_remap_ann_idx_files(remap_ann_idx):
    files = set()
    for ext in exts:
        files.add(remap_ann_idx.index_prefix + ext)

    matcher = HasOutputFiles(files)
    remap_ann_idx.verify([matcher])


# Check that remapann has built correct index and it is in right order
@pytest.mark.skip(reason='MARKETINDEXER-41733 please fix me')
def test_remap_ann_idx(user_data_res):
    assert_that(user_data_res.std_out, equal_to(user_data()), 'Wrong streams or streams order')
