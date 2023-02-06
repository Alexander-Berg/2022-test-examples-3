# coding=utf-8
"""
Test is used for checking result of 'indexerf enrich'.

Тест используется для проверки работы вызова indexerf enrich
"""

import pytest

from hamcrest import assert_that, equal_to, has_item, has_entries, is_not

from yt import wrapper as yt
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.generation.indexerf.yatf.test_env import YtIndexErfTestEnv, ErfMode
from market.idx.generation.indexerf.yatf.resources.yt_erf_data import YTEnrichErfData
from market.idx.generation.indexerf.yatf.resources.erf_output import YtErfOutput


class ErfCheckData(object):
    DATA = " DATA "
    COLOR = " color "
    HOST = " host "
    BLUE_DATA = " blue_data "
    ELASTICITY = " elasticity "
    SKU_ERF_ID = " sku_erf_id "


ALL = ErfCheckData.DATA + ErfCheckData.COLOR + ErfCheckData.HOST
ALL_BLUE = ErfCheckData.BLUE_DATA + ErfCheckData.COLOR + ErfCheckData.HOST
DATA_HOST = ErfCheckData.DATA + ErfCheckData.HOST
DATA_COLOR = ErfCheckData.DATA + ErfCheckData.COLOR
HOST_COLOR = ErfCheckData.HOST + ErfCheckData.COLOR
ONLY_DATA = ErfCheckData.DATA
BLUE_DATA = ErfCheckData.BLUE_DATA
NO_DOC_ID = "no docId"
ONLY_COLOR = ErfCheckData.COLOR
ONLY_DOC_ID = "only docId"

HOST_COLOR_ELASTICITY = ErfCheckData.HOST + ErfCheckData.COLOR + ErfCheckData.ELASTICITY
DATA_ELASTICITY = ErfCheckData.DATA + ErfCheckData.ELASTICITY
ELASTICITY_HOST = ErfCheckData.ELASTICITY + ErfCheckData.HOST
ONLY_ELASTICITY = ErfCheckData.ELASTICITY
ONLY_SKU_ERF_ID = ErfCheckData.SKU_ERF_ID

SHARD_0 = 0
SHARD_1 = 1
SHARD_2 = 2
SHARD_3 = 3   # for enrich_erf_for_blue_offers / enrich_erf_for_blue_shard


web_features_dir = "//indexer/stratocaster/static_features/erf/offers/joined/20191111_1143"
blue_web_features_dir = "//indexer/stratocaster/static_features/erf/blue_features_for_white/joined/20191111_1143"
host_ids_dir = "//indexer/stratocaster/static_features/herf/offers/20191111_1143/joined_ids"
sku_erf_ids_dir = "//indexer/stratocaster/static_features/sku_erf/offers/20191111_1143/joined_ids"
doc_ids_dir = "//indexer/stratocaster/mi3/main/20191111_1143/genlog"
color_path = "//indexer/stratocaster/mi3/main/recent/erf/colorness"
elasticity_path = "//indexer/stratocaster/mi3/main/recent/erf/elasticity"


def ware_md5(data, shard, is_blue=False):
    blue = ""
    if is_blue:
        blue = "blue"
    return data + "_shard_" + str(shard) + blue


def add_feature(rows, data, shard, is_blue=False):
    rows.append({'Data': data, 'ware_md5': ware_md5(data, shard, is_blue)})


# Input YT web table with features data
@pytest.fixture(scope='module')
def web_features_shard0(yt_stuff):
    table_path = yt.ypath_join(web_features_dir, str(SHARD_0))
    rows = []

    add_feature(rows, ALL, SHARD_0)
    add_feature(rows, ONLY_DATA, SHARD_0)
    add_feature(rows, NO_DOC_ID, SHARD_0)
    add_feature(rows, DATA_HOST, SHARD_0)

    return YtTableResource(yt_stuff, table_path, data=rows)


# Input YT web table with features data
@pytest.fixture(scope='module')
def web_features_shard1(yt_stuff):
    table_path = yt.ypath_join(web_features_dir, str(SHARD_1))
    rows = []

    add_feature(rows, ONLY_DATA, SHARD_1)
    add_feature(rows, DATA_HOST, SHARD_1)

    return YtTableResource(yt_stuff, table_path, data=rows)


# Input YT web table with features data
@pytest.fixture(scope='module')
def web_features_shard2(yt_stuff):
    table_path = yt.ypath_join(web_features_dir, str(SHARD_2))
    rows = []

    add_feature(rows, ALL, SHARD_2)
    add_feature(rows, ONLY_DATA, SHARD_2)
    add_feature(rows, DATA_COLOR, SHARD_2)

    return YtTableResource(yt_stuff, table_path, data=rows)


@pytest.fixture(scope='module')
def web_features_shard3(yt_stuff):
    table_path = yt.ypath_join(web_features_dir, str(SHARD_3))
    rows = []

    add_feature(rows, DATA_ELASTICITY, SHARD_3)

    return YtTableResource(yt_stuff, table_path, data=rows)


@pytest.fixture(scope='module')
def web_features(web_features_shard0, web_features_shard1, web_features_shard2, web_features_shard3):
    return [web_features_shard0, web_features_shard1, web_features_shard2, web_features_shard3]


# Input YT web table with blue features data
@pytest.fixture(scope='module')
def blue_web_features_shard0(yt_stuff):
    table_path = yt.ypath_join(blue_web_features_dir, str(SHARD_0))
    rows = []

    add_feature(rows, ALL_BLUE, SHARD_0, is_blue=True)
    add_feature(rows, BLUE_DATA, SHARD_0, is_blue=True)

    return YtTableResource(yt_stuff, table_path, data=rows)


# Input YT web table with features data
@pytest.fixture(scope='module')
def blue_web_features_shard1(yt_stuff):
    table_path = yt.ypath_join(blue_web_features_dir, str(SHARD_1))
    rows = []

    add_feature(rows, ALL_BLUE, SHARD_1, is_blue=True)
    add_feature(rows, BLUE_DATA, SHARD_1, is_blue=True)

    return YtTableResource(yt_stuff, table_path, data=rows)


# Input YT web table with features data
@pytest.fixture(scope='module')
def blue_web_features_shard2(yt_stuff):
    table_path = yt.ypath_join(blue_web_features_dir, str(SHARD_2))
    rows = []

    add_feature(rows, ALL_BLUE, SHARD_2, is_blue=True)
    add_feature(rows, BLUE_DATA, SHARD_2, is_blue=True)

    return YtTableResource(yt_stuff, table_path, data=rows)


@pytest.fixture(scope='module')
def blue_web_features_shard3(yt_stuff):
    table_path = yt.ypath_join(blue_web_features_dir, str(SHARD_3))
    rows = []
    # still empty
    return YtTableResource(yt_stuff, table_path, data=rows)


@pytest.fixture(scope='module')
def blue_web_features(blue_web_features_shard0, blue_web_features_shard1, blue_web_features_shard2, blue_web_features_shard3):
    return [blue_web_features_shard0, blue_web_features_shard1, blue_web_features_shard2, blue_web_features_shard3]


blue_host = 77


host_schema = [
    dict(name="host_id", type="uint64"),
    dict(name="part", type="uint64"),
    dict(name="ware_md5", type="string"),
]


@pytest.fixture(scope='module')
def host_ids_shard0(yt_stuff):
    table_path = yt.ypath_join(host_ids_dir, str(SHARD_0))
    rows = []

    rows.append({'host_id': 2, 'part': SHARD_0, 'ware_md5': ware_md5(ALL, SHARD_0)})
    rows.append({'host_id': 0, 'part': SHARD_0, 'ware_md5': ware_md5(DATA_HOST, SHARD_0)})
    rows.append({'host_id': 1, 'part': SHARD_0, 'ware_md5': ware_md5(HOST_COLOR, SHARD_0)})
    rows.append({'host_id': blue_host, 'part': SHARD_0, 'ware_md5': ware_md5(ALL_BLUE, SHARD_0, is_blue=True)})
    rows.append({'host_id': blue_host, 'part': SHARD_0, 'ware_md5': ware_md5(BLUE_DATA, SHARD_0, is_blue=True)})

    return YtTableResource(yt_stuff, table_path, data=rows, attributes={'schema': host_schema})


@pytest.fixture(scope='module')
def host_ids_shard1(yt_stuff):
    table_path = yt.ypath_join(host_ids_dir, str(SHARD_1))
    rows = []

    rows.append({'host_id': 1, 'part': SHARD_1, 'ware_md5': ware_md5(DATA_HOST, SHARD_1)})
    rows.append({'host_id': 0, 'part': SHARD_1, 'ware_md5': ware_md5(HOST_COLOR, SHARD_1)})
    rows.append({'host_id': blue_host, 'part': SHARD_1, 'ware_md5': ware_md5(ALL_BLUE, SHARD_1, is_blue=True)})
    rows.append({'host_id': blue_host, 'part': SHARD_1, 'ware_md5': ware_md5(BLUE_DATA, SHARD_1, is_blue=True)})

    return YtTableResource(yt_stuff, table_path, data=rows, attributes={'schema': host_schema})


@pytest.fixture(scope='module')
def host_ids_shard2(yt_stuff):
    table_path = yt.ypath_join(host_ids_dir, str(SHARD_2))
    rows = []

    rows.append({'host_id': 0, 'part': SHARD_2, 'ware_md5': ware_md5(ALL, SHARD_2)})
    rows.append({'host_id': 1, 'part': SHARD_2, 'ware_md5': ware_md5(HOST_COLOR, SHARD_2)})
    rows.append({'host_id': blue_host, 'part': SHARD_2, 'ware_md5': ware_md5(ALL_BLUE, SHARD_2, is_blue=True)})
    rows.append({'host_id': blue_host, 'part': SHARD_2, 'ware_md5': ware_md5(BLUE_DATA, SHARD_2, is_blue=True)})

    return YtTableResource(yt_stuff, table_path, data=rows, attributes={'schema': host_schema})


@pytest.fixture(scope='module')
def host_ids_shard3(yt_stuff):
    table_path = yt.ypath_join(host_ids_dir, str(SHARD_3))
    rows = []

    rows.append({'host_id': 100, 'part': SHARD_3, 'ware_md5': ware_md5(HOST_COLOR_ELASTICITY, SHARD_3)})
    rows.append({'host_id': 101, 'part': SHARD_3, 'ware_md5': ware_md5(ELASTICITY_HOST, SHARD_3)})

    return YtTableResource(yt_stuff, table_path, data=rows, attributes={'schema': host_schema})


@pytest.fixture(scope='module')
def host_ids(host_ids_shard0, host_ids_shard1, host_ids_shard2, host_ids_shard3):
    return [host_ids_shard0, host_ids_shard1, host_ids_shard2, host_ids_shard3]


sku_erf_ids_schema = [
    dict(name="sku_erf_id", type="uint64"),
    dict(name="part", type="uint64"),
    dict(name="ware_md5", type="string"),
]


@pytest.fixture(scope='module')
def sku_erf_ids_shard0(yt_stuff):
    table_path = yt.ypath_join(sku_erf_ids_dir, str(SHARD_0))
    rows = [{'sku_erf_id': 17, 'part': SHARD_0, 'ware_md5': ware_md5(ALL, SHARD_0)}]
    return YtTableResource(yt_stuff, table_path, data=rows, attributes={'schema': sku_erf_ids_schema})


@pytest.fixture(scope='module')
def sku_erf_ids_shard1(yt_stuff):
    table_path = yt.ypath_join(sku_erf_ids_dir, str(SHARD_1))
    return YtTableResource(yt_stuff, table_path, data=[], attributes={'schema': sku_erf_ids_schema})


@pytest.fixture(scope='module')
def sku_erf_ids_shard2(yt_stuff):
    table_path = yt.ypath_join(sku_erf_ids_dir, str(SHARD_2))
    return YtTableResource(yt_stuff, table_path, data=[], attributes={'schema': sku_erf_ids_schema})


@pytest.fixture(scope='module')
def sku_erf_ids_shard3(yt_stuff):
    table_path = yt.ypath_join(sku_erf_ids_dir, str(SHARD_3))
    rows = [{'sku_erf_id': 18, 'part': SHARD_3, 'ware_md5': ware_md5(ONLY_SKU_ERF_ID, SHARD_3)}]
    return YtTableResource(yt_stuff, table_path, data=rows, attributes={'schema': sku_erf_ids_schema})


@pytest.fixture(scope='module')
def sku_erf_ids(sku_erf_ids_shard0, sku_erf_ids_shard1, sku_erf_ids_shard2, sku_erf_ids_shard3):
    return [sku_erf_ids_shard0, sku_erf_ids_shard1, sku_erf_ids_shard2, sku_erf_ids_shard3]


@pytest.fixture(scope='module')
def doc_ids_shard0(yt_stuff):
    table_path = yt.ypath_join(doc_ids_dir, "000" + str(SHARD_0))
    rows = []

    rows.append({'ware_md5': ware_md5(DATA_HOST, SHARD_0)})  # docId 0
    rows.append({'ware_md5': ware_md5(ONLY_DATA, SHARD_0)})  # docId 1
    rows.append({'ware_md5': ware_md5(HOST_COLOR, SHARD_0)})  # docId 2
    rows.append({'ware_md5': ware_md5(ALL, SHARD_0)})  # docId 3
    rows.append({'ware_md5': ware_md5(ALL_BLUE, SHARD_0, is_blue=True)})  # docId 4
    rows.append({'ware_md5': ware_md5(BLUE_DATA, SHARD_0, is_blue=True)})  # docId 5
    rows.append({'ware_md5': ware_md5(ONLY_DOC_ID, SHARD_0)})  # docId 6, should not see this

    return YtTableResource(yt_stuff, table_path, data=rows)


@pytest.fixture(scope='module')
def doc_ids_shard1(yt_stuff):
    table_path = yt.ypath_join(doc_ids_dir, "000" + str(SHARD_1))
    rows = []

    rows.append({'ware_md5': ware_md5(ONLY_DATA, SHARD_1)})  # docId 0
    rows.append({'ware_md5': ware_md5(HOST_COLOR, SHARD_1)})  # docId 1
    rows.append({'ware_md5': ware_md5(DATA_HOST, SHARD_1)})  # docId 2
    rows.append({'ware_md5': ware_md5(ALL_BLUE, SHARD_1, is_blue=True)})  # docId 3
    rows.append({'ware_md5': ware_md5(BLUE_DATA, SHARD_1, is_blue=True)})  # docId 4

    return YtTableResource(yt_stuff, table_path, data=rows)


@pytest.fixture(scope='module')
def doc_ids_shard2(yt_stuff):
    table_path = yt.ypath_join(doc_ids_dir, "000" + str(SHARD_2))
    rows = []

    rows.append({'ware_md5': ware_md5(ONLY_DATA, SHARD_2)})  # docId 0
    rows.append({'ware_md5': ware_md5(ALL, SHARD_2)})  # docId 1
    rows.append({'ware_md5': ware_md5(HOST_COLOR, SHARD_2)})  # docId 2
    rows.append({'ware_md5': ware_md5(DATA_COLOR, SHARD_2)})  # docId 3
    rows.append({'ware_md5': ware_md5(ALL_BLUE, SHARD_2, is_blue=True)})  # docId 4
    rows.append({'ware_md5': ware_md5(BLUE_DATA, SHARD_2, is_blue=True)})  # docId 5

    return YtTableResource(yt_stuff, table_path, data=rows)


@pytest.fixture(scope='module')
def doc_ids_shard3(yt_stuff):
    table_path = yt.ypath_join(doc_ids_dir, "000" + str(SHARD_3))
    rows = []
    rows.append({'ware_md5': ware_md5(ONLY_DOC_ID, SHARD_3)})  # docId 0, should not see this
    return YtTableResource(yt_stuff, table_path, data=rows)


@pytest.fixture(scope='module')
def doc_ids(doc_ids_shard0, doc_ids_shard1, doc_ids_shard2, doc_ids_shard3):
    return [doc_ids_shard0, doc_ids_shard1, doc_ids_shard2, doc_ids_shard3]


color_schema = [
    dict(name="part", type="uint64"),
    dict(name="ware_md5", type="string"),
    dict(name="colorness", type="double"),
    dict(name="colornessAvg", type="double"),
]


@pytest.fixture(scope='module')
def colors(yt_stuff):
    table_path = color_path
    rows = []

    rows.append({'ware_md5': ware_md5(HOST_COLOR, SHARD_1), 'part': SHARD_1, 'colorness': 11.01, 'colornessAvg': 0.01111})
    rows.append({'ware_md5': ware_md5(HOST_COLOR, SHARD_0), 'part': SHARD_0, 'colorness': 11.0, 'colornessAvg': 0.011})
    rows.append({'ware_md5': ware_md5(ALL, SHARD_2), 'part': SHARD_2, 'colorness': 111.1, 'colornessAvg': 0.1111})
    rows.append({'ware_md5': ware_md5(ALL, SHARD_0), 'part': SHARD_0, 'colorness': 111.0, 'colornessAvg': 0.111})
    rows.append({'ware_md5': ware_md5(HOST_COLOR, SHARD_2), 'part': SHARD_2, 'colorness': 11.1, 'colornessAvg': 0.0111})
    rows.append({'ware_md5': ware_md5(NO_DOC_ID, SHARD_0), 'part': SHARD_0, 'colorness': 6.0, 'colornessAvg': 0.6})
    rows.append({'ware_md5': ware_md5(DATA_COLOR, SHARD_2), 'part': SHARD_2, 'colorness': 101.1, 'colornessAvg': 0.1011})
    rows.append({'ware_md5': ware_md5(ONLY_COLOR, SHARD_1), 'part': SHARD_1, 'colorness': 7.7, 'colornessAvg': 7.7})
    rows.append({'ware_md5': ware_md5(ALL_BLUE, SHARD_0, is_blue=True), 'part': SHARD_0, 'colorness': 0.0, 'colornessAvg': 0.0})
    rows.append({'ware_md5': ware_md5(ALL_BLUE, SHARD_1, is_blue=True), 'part': SHARD_1, 'colorness': 1000.0, 'colornessAvg': 0.0001})
    rows.append({'ware_md5': ware_md5(ALL_BLUE, SHARD_2, is_blue=True), 'part': SHARD_2, 'colorness': 2000.0, 'colornessAvg': 0.0002})
    rows.append({'ware_md5': ware_md5(HOST_COLOR_ELASTICITY, SHARD_3), 'part': SHARD_3, 'colorness': 1234.5, 'colornessAvg': 678.9})

    return YtTableResource(yt_stuff, table_path, data=rows, attributes={'schema': color_schema})


elasticity_schema = [
    dict(name="part", type="uint64"),
    dict(name="ware_md5", type="string"),
    dict(name="elasticity_dm", type="double"),
]


@pytest.fixture(scope='module')
def elasticity_features(yt_stuff):
    rows = []

    rows.append({'ware_md5': ware_md5(NO_DOC_ID, SHARD_3), 'part': SHARD_3, 'elasticity_dm': 44.44})
    rows.append({'ware_md5': ware_md5(HOST_COLOR_ELASTICITY, SHARD_3), 'part': SHARD_3, 'elasticity_dm': 55.55})
    rows.append({'ware_md5': ware_md5(DATA_ELASTICITY, SHARD_3), 'part': SHARD_3, 'elasticity_dm': 66.66})
    rows.append({'ware_md5': ware_md5(ELASTICITY_HOST, SHARD_3), 'part': SHARD_3, 'elasticity_dm': 77.77})
    rows.append({'ware_md5': ware_md5(ONLY_ELASTICITY, SHARD_3), 'part': SHARD_3, 'elasticity_dm': 88.88})

    return YtTableResource(yt_stuff, elasticity_path, data=rows, attributes={'schema': elasticity_schema})


@pytest.fixture(scope='module')
def input_data(web_features, blue_web_features, host_ids, sku_erf_ids, doc_ids, colors, elasticity_features):
    res = YTEnrichErfData(web_features_dir, web_features, blue_web_features_dir, blue_web_features,
                          host_ids_dir, host_ids, sku_erf_ids_dir, sku_erf_ids, doc_ids_dir, doc_ids, colors, elasticity_features)
    return res


# output info and YT tables
@pytest.fixture(scope='module')
def erf_output(web_features):
    erf_path = "//indexer/stratocaster/static_features/erf/offers/enriched/20191112_0016"

    return YtErfOutput(erf_path, len(web_features))


# Execution of binary
@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data, erf_output):
    resources = {
        "input": input_data,
        "output": erf_output
    }

    with YtIndexErfTestEnv(**resources) as env:
        env.execute(ErfMode.ENRICH, yt_stuff)
        env.verify()
        yield env


# Helper functions
@pytest.fixture(scope='module')
def result_shard0_table(workflow):
    return workflow.result_tables[0]


@pytest.fixture(scope='module')
def result_shard1_table(workflow):
    return workflow.result_tables[1]


@pytest.fixture(scope='module')
def result_shard2_table(workflow):
    return workflow.result_tables[2]


@pytest.fixture(scope='module')
def result_shard3_table(workflow):
    return workflow.result_tables[3]


def should_see_erfs_features(res_data, expected_data):
    for item in expected_data:
        assert_that(res_data, has_item(equal_to(item)), "No match for enriched data")


def should_not_see_erfs_features(res_data, expected_data):
    for item in expected_data:
        assert_that(res_data, is_not(has_item(has_entries(item))), "No match for enriched data")


# Tests
def test_result_table_count(workflow, erf_output):
    assert_that(len(workflow.result_tables), equal_to(erf_output.parts_cnt), "output number shards != input number of shards")


def test_erf_enriched0(result_shard0_table):
    rows = []
    rows.append({'ware_md5': ware_md5(ALL, SHARD_0), 'Data': ALL, 'host_id': 2, 'doc_id': 3, 'colorness': 111.0, 'colornessAvg': 0.111, 'sku_erf_id': 17})
    rows.append({'ware_md5': ware_md5(ONLY_DATA, SHARD_0), 'Data': ONLY_DATA, 'doc_id': 1})
    rows.append({'ware_md5': ware_md5(ALL_BLUE, SHARD_0, is_blue=True), 'Data': ALL_BLUE, 'host_id': blue_host, 'doc_id': 4, 'colorness': 0.0, 'colornessAvg': 0.0})
    rows.append({'ware_md5': ware_md5(BLUE_DATA, SHARD_0, is_blue=True), 'Data': BLUE_DATA, 'host_id': blue_host, 'doc_id': 5})
    rows.append({'ware_md5': ware_md5(DATA_HOST, SHARD_0), 'Data': DATA_HOST, 'host_id': 0, 'doc_id': 0})
    rows.append({'ware_md5': ware_md5(HOST_COLOR, SHARD_0), 'host_id': 1, 'doc_id': 2, 'colorness': 11.0, 'colornessAvg': 0.011})
    rows.append({'ware_md5': ware_md5(NO_DOC_ID, SHARD_0), 'Data': NO_DOC_ID, 'colorness': 6.0, 'colornessAvg': 0.6})

    should_see_erfs_features(result_shard0_table.data, rows)


def test_not_see_erf_without_doc_enriched0(result_shard0_table):
    rows = []
    rows.append({'ware_md5': ware_md5(ONLY_DOC_ID, SHARD_0)})

    should_not_see_erfs_features(result_shard0_table.data, rows)


def test_erf_enriched1(result_shard1_table):
    rows = []
    rows.append({'ware_md5': ware_md5(ONLY_DATA, SHARD_1), 'Data': ONLY_DATA, 'doc_id': 0})
    rows.append({'ware_md5': ware_md5(DATA_HOST, SHARD_1), 'Data': DATA_HOST, 'host_id': 1, 'doc_id': 2})
    rows.append({'ware_md5': ware_md5(HOST_COLOR, SHARD_1), 'host_id': 0, 'doc_id': 1, 'colorness': 11.01, 'colornessAvg': 0.01111})
    rows.append({'ware_md5': ware_md5(ALL_BLUE, SHARD_1, is_blue=True), 'Data': ALL_BLUE, 'host_id': blue_host, 'doc_id': 3, 'colorness': 1000.0, 'colornessAvg': 0.0001})
    rows.append({'ware_md5': ware_md5(BLUE_DATA, SHARD_1, is_blue=True), 'Data': BLUE_DATA, 'host_id': blue_host, 'doc_id': 4})
    rows.append({'ware_md5': ware_md5(ONLY_COLOR, SHARD_1), 'colorness': 7.7, 'colornessAvg': 7.7})

    should_see_erfs_features(result_shard1_table.data, rows)


def test_erf_enriched2(result_shard2_table):
    rows = []
    rows.append({'ware_md5': ware_md5(ALL, SHARD_2), 'Data': ALL, 'host_id': 0, 'doc_id': 1, 'colorness': 111.1, 'colornessAvg': 0.1111})
    rows.append({'ware_md5': ware_md5(ONLY_DATA, SHARD_2), 'Data': ONLY_DATA, 'doc_id': 0})
    rows.append({'ware_md5': ware_md5(DATA_COLOR, SHARD_2), 'Data': DATA_COLOR, 'doc_id': 3, 'colorness': 101.1, 'colornessAvg': 0.1011})
    rows.append({'ware_md5': ware_md5(HOST_COLOR, SHARD_2), 'host_id': 1, 'doc_id': 2, 'colorness': 11.1, 'colornessAvg': 0.0111})
    rows.append({'ware_md5': ware_md5(ALL_BLUE, SHARD_2, is_blue=True), 'Data': ALL_BLUE, 'host_id': blue_host, 'doc_id': 4, 'colorness': 2000.0, 'colornessAvg': 0.0002})
    rows.append({'ware_md5': ware_md5(BLUE_DATA, SHARD_2, is_blue=True), 'Data': BLUE_DATA, 'host_id': blue_host, 'doc_id': 5})

    should_see_erfs_features(result_shard2_table.data, rows)


def test_erf_enriched3(result_shard3_table):
    rows = []
    rows.append({'ware_md5': ware_md5(HOST_COLOR_ELASTICITY, SHARD_3), 'host_id': 100, 'colorness': 1234.5, 'colornessAvg': 678.9, 'elasticity_dm': 55.55})
    rows.append({'ware_md5': ware_md5(DATA_ELASTICITY, SHARD_3), 'Data': DATA_ELASTICITY, 'elasticity_dm': 66.66})
    rows.append({'ware_md5': ware_md5(ELASTICITY_HOST, SHARD_3), 'host_id': 101, 'elasticity_dm': 77.77})
    rows.append({'ware_md5': ware_md5(ONLY_ELASTICITY, SHARD_3), 'elasticity_dm': 88.88})
    rows.append({'ware_md5': ware_md5(ONLY_SKU_ERF_ID, SHARD_3), 'sku_erf_id': 18})

    should_see_erfs_features(result_shard3_table.data, rows)


def test_not_see_erf_without_doc_enriched3(result_shard3_table):
    rows = []
    rows.append({'ware_md5': ware_md5(ONLY_DOC_ID, SHARD_3)})

    should_not_see_erfs_features(result_shard3_table.data, rows)
