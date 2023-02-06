# coding=utf-8
"""
Тест используется для проверки построения индекса erf из YT.
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


def erf_data_all():
    return '''================== 0 ==================
DOCUMENT FEATURES:
AddTime: 1
DocStaticSignature1: 11
LastAccess: 111
FHeadingIdfSum: 0.1111

HOST FEATURES:

================== 1 ==================
DOCUMENT FEATURES:

HOST FEATURES:

================== 2 ==================
DOCUMENT FEATURES:
HostId: 2
AddTime: 2
DocStaticSignature1: 22
LastAccess: 222
FHeadingIdfSum: 0.222
Colorness: 0.2
ColornessAvg: 0.002

HOST FEATURES:
Mascot01: 2
Mascot02: 22
Mascot03: 222

================== 3 ==================
DOCUMENT FEATURES:
HostId: 1
Colorness: 0.3
ColornessAvg: 0.003

HOST FEATURES:
Mascot01: 1
Mascot02: 11
Mascot03: 111

================== 4 ==================
DOCUMENT FEATURES:
AddTime: 4
LongestText: 40
DocStaticSignature1: 44
LastAccess: 444
FHeadingIdfSum: 0.444
Colorness: 0.4
ColornessAvg: 0.004

HOST FEATURES:

================== 5 ==================
DOCUMENT FEATURES:
HostId: 2
AddTime: 5
DocStaticSignature1: 55
LastAccess: 555
FHeadingIdfSum: 0.555

HOST FEATURES:
Mascot01: 2
Mascot02: 22
Mascot03: 222

================== 6 ==================
DOCUMENT FEATURES:
HostId: 6666667

'''


@pytest.fixture(scope='module')
def static_features0():
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
    dict(name="elasticity_dm", type="string"),
]


# Table with web streams
@pytest.fixture(scope='module')
def erf_enrich_table(yt_stuff, static_features0, static_features2, static_features4, static_features5):
    yt_enriched_path = "//market/indexer/static_features/erf/offers/enriched/20191125_2213/0"
    # WARNING if you decide to add more rows, please, also update num_docs in output_data, I beg of you.
    rows = [
        {'doc_id': 4, 'Data': static_features4.SerializeToString(), 'colorness': 0.4, 'colornessAvg': 0.004},
        {'doc_id': 0, 'Data': static_features0.SerializeToString()},
        {'doc_id': 3, 'host_id': 0, 'colorness': 0.3, 'colornessAvg': 0.003},
        {'doc_id': 2, 'Data': static_features2.SerializeToString(), 'host_id': 1, 'colorness': 0.2, 'colornessAvg': 0.002},
        {'doc_id': 5, 'Data': static_features5.SerializeToString(), 'host_id': 1},
        {'doc_id': 6, 'host_id': 6666666},  # host_id некорректный
    ]

    return YtTableResource(yt_stuff, yt_enriched_path, data=rows, attributes={'schema': enrich_schema})


# web table with streams
@pytest.fixture(scope='module')
def input_data(erf_enrich_table):

    res = YTBuildErfData(erf_enrich_table)

    return res


@pytest.fixture(scope='module')
def output_data():
    # important it should be >= max docId in erf_enrich_table
    num_docs = 7
    index_name = "erf"

    res = YtBuildErfOutput(index_name, num_docs)

    return res


@pytest.yield_fixture(scope='module')
def erf_idx(yt_stuff, input_data, output_data):
    resources = {
        "input": input_data,
        "output": output_data
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
        {'Host': 'Host1', 'Data': static_herf_features1.SerializeToString()},
        {'Host': 'Host2', 'Data': static_herf_features2.SerializeToString()},
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


@pytest.yield_fixture(scope='module')
def erf_view_res_doc_2(erf_idx, herf_idx):
    docId = 2
    resources = {
    }

    with ErfViewTestEnv(**resources) as env:
        env.execute(erf_idx.index_dir, docId=docId, force_white=True)
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def erf_view_res_doc_all(erf_idx, herf_idx):
    resources = {
    }

    with ErfViewTestEnv(**resources) as env:
        env.execute(erf_idx.index_dir, force_white=True, with_herf=True)
        env.verify()
        yield env


def test_erf_idx_all(erf_view_res_doc_all):
    assert_that(erf_view_res_doc_all.std_out, equal_to(erf_data_all()), 'Wrong output from erf')
