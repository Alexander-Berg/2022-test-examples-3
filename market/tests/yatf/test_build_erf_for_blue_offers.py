# coding=utf-8
"""
Тест используется для проверки построения индекса erf из YT для синих офферов и синего шарда.
"""

import pytest

from hamcrest import assert_that, equal_to

from yt import wrapper as yt

from market.idx.yatf.resources.yt_table_resource import YtTableResource

from market.idx.generation.indexerf.yatf.test_env import YtIndexErfTestEnv, ErfMode
from market.idx.generation.indexerf.yatf.resources.yt_erf_data import YTBuildErfData
from market.idx.generation.indexerf.yatf.resources.erf_output import YtBuildErfOutput
from market.idx.generation.indexerf.yatf.indexerf_view import ErfViewTestEnv
from market.idx.generation.indexerf.src.proto.IndexErf_pb2 import TErfFeatures, THerfFeatures
from market.idx.yatf.matchers.env_matchers import HasOutputFiles

assert yt


WAREMD5_DOCID0 = 'Njk5ZDA1MmNlYzhjNDI5YQ'
WAREMD5_DOCID1 = 'jaPRZC2qhM8tmb0yQQapzA'
WAREMD5_DOCID2 = 'cxlZ3cTeKNxAzS6OMWX51g'
WAREMD5_DOCID3 = 'XwMatVDskhEwoWXenZRifQ'
WAREMD5_DOCID4 = 'WBxjfubDtEU49gfweJaA1Q'
WAREMD5_DOCID5 = 'wymqSwv6wBsx5XlChPmrwQ'
WAREMD5_DOCID6 = 'nomkVuHL2Q0wYGPdnvvfKg'


def erf_data_all():
    return '''================== 0 ==================
DOCUMENT FEATURES:
HostId: 1
Elasticity: 66.66

HOST FEATURES:
Mascot01: 1
Mascot02: 11
Mascot03: 111

SKU FEATURES:

================== 1 ==================
DOCUMENT FEATURES:
HostId: 1
AddTime: 1
DocStaticSignature1: 11
LastAccess: 111
FHeadingIdfSum: 0.1111
Elasticity: 11.11
SkuErfId: 2

HOST FEATURES:
Mascot01: 1
Mascot02: 11
Mascot03: 111

SKU FEATURES:
PredictItemsToday: 2.1
PredictItemsIn5Days: 2.15
PredictItemsIn10Days: 2.2
PredictItemsIn15Days: 20
PredictItemsIn20Days: 21

================== 2 ==================
DOCUMENT FEATURES:
HostId: 1
AddTime: 2
DocStaticSignature1: 22
LastAccess: 222
FHeadingIdfSum: 0.222
Elasticity: 22.22

HOST FEATURES:
Mascot01: 1
Mascot02: 11
Mascot03: 111

SKU FEATURES:

================== 3 ==================
DOCUMENT FEATURES:

HOST FEATURES:

SKU FEATURES:

================== 4 ==================
DOCUMENT FEATURES:
HostId: 1
AddTime: 4
LongestText: 40
DocStaticSignature1: 44
LastAccess: 444
FHeadingIdfSum: 0.444
SkuErfId: 1

HOST FEATURES:
Mascot01: 1
Mascot02: 11
Mascot03: 111

SKU FEATURES:
PredictItemsToday: 1.1
PredictItemsIn5Days: 1.15
PredictItemsIn10Days: 1.2
PredictItemsIn15Days: 10
PredictItemsIn20Days: 11
PredictItemsIn25Days: 12

================== 5 ==================
DOCUMENT FEATURES:
HostId: 1
AddTime: 5
DocStaticSignature1: 55
LastAccess: 555
FHeadingIdfSum: 0.555

HOST FEATURES:
Mascot01: 1
Mascot02: 11
Mascot03: 111

SKU FEATURES:

================== 6 ==================
DOCUMENT FEATURES:
HostId: 1

HOST FEATURES:
Mascot01: 1
Mascot02: 11
Mascot03: 111

SKU FEATURES:

'''


@pytest.fixture(scope='module')
def static_features1():
    return TErfFeatures(AddTime=1,
                        DocStaticSignature1=11,
                        LastAccess=111,
                        FHeadingIdfSum=0.1111)


@pytest.fixture(scope='module')
def static_features2():
    return TErfFeatures(AddTime=2,
                        DocStaticSignature1=22,
                        LastAccess=222,
                        FHeadingIdfSum=0.222)


@pytest.fixture(scope='module')
def static_features4():
    return TErfFeatures(AddTime=4,
                        DocStaticSignature1=44,
                        LastAccess=444,
                        LongestText=40,
                        FHeadingIdfSum=0.444)


@pytest.fixture(scope='module')
def static_features5():
    return TErfFeatures(AddTime=5,
                        DocStaticSignature1=55,
                        LastAccess=555,
                        FHeadingIdfSum=0.555)

enrich_schema = [
    dict(name="doc_id", type="uint64"),
    dict(name="ware_md5", type="string"),
    dict(name="Data", type="string"),
    dict(name="host_id", type="uint64"),
    dict(name="colorness", type="double"),
    dict(name="colornessAvg", type="double"),
    dict(name="elasticity_dm", type="double"),
    dict(name="sku_erf_id", type="uint64"),
]


# Table static features
@pytest.fixture(scope='module')
def erf_enrich_table(yt_stuff, static_features1, static_features2, static_features4, static_features5):
    yt_enriched_path = "//market/indexer/static_features/erf/offers/enriched/20191125_2213/0"
    # WARNING if you decide to add more rows, please, also update num_docs in output_data, I beg of you.
    rows = [
        {'ware_md5': WAREMD5_DOCID4, 'Data': static_features4.SerializeToString(), 'sku_erf_id': 0, 'doc_id': 4},  # Should see SkuErfId = 1 because of fake record
        {'ware_md5': WAREMD5_DOCID5, 'Data': static_features5.SerializeToString(), 'doc_id': 5},
        {'ware_md5': WAREMD5_DOCID6, 'host_id': 6, 'doc_id': 6},  # host_id will be ignored because the only one host in blue mode
        {'ware_md5': WAREMD5_DOCID0, 'elasticity_dm': 66.66, 'doc_id': 0},
        {'ware_md5': WAREMD5_DOCID1, 'Data': static_features1.SerializeToString(), 'elasticity_dm': 11.11, 'sku_erf_id': 1, 'doc_id': 1},  # Should see SkuErfId = 1 because of fake record
        {'ware_md5': WAREMD5_DOCID2, 'Data': static_features2.SerializeToString(), 'elasticity_dm': 22.22, 'doc_id': 2},
    ]

    return YtTableResource(yt_stuff, yt_enriched_path, data=rows, attributes={'schema': enrich_schema})


@pytest.fixture(scope='module')
def output_data():
    # important it should be >= max docId in erf_enrich_table
    num_docs = 7
    index_name = "blue_erf"
    res = YtBuildErfOutput(index_name, num_docs, is_blue=True)

    return res


@pytest.fixture(scope='module')
def input_data(erf_enrich_table):
    res = YTBuildErfData(erf_enrich_table)
    return res


@pytest.yield_fixture(scope='module')
def erf_idx(yt_stuff, input_data, output_data):
    resources = {
        "input": input_data,
        "output": output_data,
    }

    with YtIndexErfTestEnv(**resources) as env:
        env.execute(ErfMode.BUILD, yt_stuff)
        env.verify()
        yield env


# Check that indexerf has been generated necessary index file
def test_erf_idx_files(erf_idx):

    file = erf_idx.index_name
    matcher = HasOutputFiles({file})
    erf_idx.verify([matcher])


@pytest.fixture(scope='module')
def static_herf_features1():
    return THerfFeatures(Mascot01=1,
                         Mascot02=11,
                         Mascot03=111)


@pytest.fixture(scope='module')
def static_herf_features2():
    return THerfFeatures(Mascot01=2,
                         Mascot02=22,
                         Mascot03=222)


join_herf_schema = [
    dict(name="Host", type="string"),
    dict(name="Data", type="string"),
]


# Table with web streams
@pytest.fixture(scope='module')
def herf_joined_table(yt_stuff, static_herf_features1, static_herf_features2):
    yt_herf_joined_path = "//market/indexer/static_features/herf/offers/joined/20191125_2213/0"
    # WARNING if you decide to add more rows, please, also update num_docs in output_data, I beg of you.
    rows = [
        {'Host': 'beru.ru', 'Data': static_herf_features1.SerializeToString()},
        {'Host': 'unused', 'Data': static_herf_features2.SerializeToString()},
    ]

    return YtTableResource(yt_stuff, yt_herf_joined_path, data=rows, attributes={'schema': join_herf_schema})


# web table with streams
@pytest.fixture(scope='module')
def herf_input_data(herf_joined_table):

    res = YTBuildErfData(herf_joined_table)
    return res


@pytest.fixture(scope='module')
def herf_output_data(herf_joined_table):
    # important it should be >= max docId in herf_joined_table
    index_name = "herf"
    res = YtBuildErfOutput(index_name, num_docs=len(herf_joined_table.data))
    return res


@pytest.yield_fixture(scope='module')
def herf_idx(yt_stuff, herf_input_data, herf_output_data):
    resources = {
        "input": herf_input_data,
        "output": herf_output_data
    }

    with YtIndexErfTestEnv(**resources) as env:
        env.execute(ErfMode.BUILD, yt_stuff, is_herf=True)
        env.verify()
        yield env


# Check that indexerf has been generated necessary index file
def test_herf_idx_files(herf_idx):

    file = herf_idx.index_name
    matcher = HasOutputFiles({file})
    herf_idx.verify([matcher])


sku_erf_schema = [
    dict(name='sku', type='int64'),
    dict(name='today_demand', type='double'),
    dict(name='in_5_days_demand', type='double'),
    dict(name='in_10_days_demand', type='double'),
    dict(name='in_15_days_demand', type='double'),
    dict(name='in_20_days_demand', type='double'),
    dict(name='in_25_days_demand', type='double'),
]


# Table sku erf features
@pytest.fixture(scope='module')
def sku_erf_table(yt_stuff):
    yt_sku_erf_path = "//market/indexer/static_features/sku_erf/joined/20191125_2213/0"
    rows = [
        # skuerf_id = 0
        {'sku': 1, 'today_demand': 1.1, 'in_5_days_demand': 1.15, 'in_10_days_demand': 1.2,
         'in_15_days_demand': 10.0, 'in_20_days_demand': 11.0, 'in_25_days_demand': 12.0},
        # skuerf_id = 1
        {'sku': 2, 'today_demand': 2.1, 'in_5_days_demand': 2.15, 'in_10_days_demand': 2.2,
         'in_15_days_demand': 20.0, 'in_20_days_demand': 21.0},
    ]

    return YtTableResource(yt_stuff, yt_sku_erf_path, data=rows, attributes={'schema': sku_erf_schema})


@pytest.fixture(scope='module')
def input_data_sku_erf(sku_erf_table):
    res = YTBuildErfData(sku_erf_table)
    return res


@pytest.fixture(scope='module')
def output_data_sku_erf(sku_erf_table):
    index_name = "sku_erf"
    res = YtBuildErfOutput(index_name, num_docs=len(sku_erf_table.data), is_sku=True)
    return res


@pytest.yield_fixture(scope='module')
def erf_idx_sku_erf(yt_stuff, input_data_sku_erf, output_data_sku_erf):
    resources = {
        "input": input_data_sku_erf,
        "output": output_data_sku_erf,
    }

    with YtIndexErfTestEnv(**resources) as env:
        env.execute(ErfMode.BUILD, yt_stuff)
        env.verify()
        yield env


# Check that indexerf has been generated necessary index file
def test_erf_idx_files_sku_erf(erf_idx_sku_erf):

    file = erf_idx_sku_erf.index_name
    matcher = HasOutputFiles({file})
    erf_idx_sku_erf.verify([matcher])


@pytest.yield_fixture(scope='module')
def erf_view_res_doc_all(erf_idx, herf_idx, erf_idx_sku_erf):
    resources = {
    }

    with ErfViewTestEnv(**resources) as env:
        env.execute(erf_idx.index_dir, force_blue=True, with_herf=True)
        env.verify()
        yield env


def test_erf_idx_all(erf_view_res_doc_all):
    assert_that(erf_view_res_doc_all.std_out, equal_to(erf_data_all()), 'Wrong output from erf')
