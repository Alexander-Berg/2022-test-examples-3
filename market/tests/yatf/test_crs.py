import pytest

from yt.wrapper import ypath_join

from hamcrest import assert_that, equal_to, has_entries


from market.idx.tools.market_yt_data_upload.yatf.test_env import YtDataUploadTestEnv
from market.idx.yatf.resources.text_file import TextFileResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from mapreduce.yt.python.table_schema import extract_column_attributes

fields = [
    'category',
    'region',
    'noffers',
    'nvclusters',
    'minprice',
    'maxdiscount',
    'ndiscounts',
    'nmodels',
    'nmodels_discount',
]


def crs_record(base):
    return {fields[i]: base + i for i in range(len(fields))}

NUM_RECORDS = 4


@pytest.fixture(scope='module')
def crs_records():
    r1 = crs_record(10)
    r2 = crs_record(20)
    r3 = crs_record(30)
    r4 = crs_record(40)
    r2['minprice'] += 0.5
    r2['maxdiscount'] += 0.75
    r3['region'] = -1

    return [r1, r2, r3, r4]


@pytest.fixture(scope='module')
def crs_text(crs_records):
    arr = []
    for rec in crs_records:
        a = [str(rec[k]) for k in fields]
        arr.append(a)

    arr[3].append('100')  # good enough

    bad_rec = crs_record(100)
    bad_a = [str(bad_rec[k]) for k in fields]
    bad_a.pop()          # bad record

    arr.insert(3, bad_a)

    text = '\n'.join(['\t'.join(ar) for ar in arr])

    return text


@pytest.yield_fixture(scope='module')
def workflow(yt_server, crs_text):
    resources = {
        'crs': TextFileResource('category_region_stats.csv', crs_text),
    }

    with YtDataUploadTestEnv(**resources) as env:
        env.execute(yt_server, type='crs', output_table=ypath_join(get_yt_prefix(), 'crs'))
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_result_table_schema(result_yt_table):
    assert_that(extract_column_attributes(list(result_yt_table.schema)),
                equal_to([
                    {'required': False, 'name': 'category', 'type': 'int64'},
                    {'required': False, 'name': 'region', 'type': 'int64'},
                    {'required': False, 'name': 'noffers', 'type': 'int64'},
                    {'required': False, 'name': 'nvclusters', 'type': 'int64'},
                    {'required': False, 'name': 'minprice', 'type': 'double'},
                    {'required': False, 'name': 'maxdiscount', 'type': 'double'},
                    {'required': False, 'name': 'ndiscounts', 'type': 'int64'},
                    {'required': False, 'name': 'nmodels', 'type': 'int64'},
                    {'required': False, 'name': 'nmodels_discount', 'type': 'int64'},
                ]), 'Schema is incorrect')


def test_result_table_row_count(result_yt_table, crs_records):
    assert len(crs_records) == NUM_RECORDS
    assert_that(len(result_yt_table.data), equal_to(NUM_RECORDS), "Rows count equal count of crs records in table")


@pytest.mark.parametrize('rec_n', range(NUM_RECORDS))
def test_result_row(result_yt_table, crs_records, rec_n):
    record = crs_records[rec_n]
    expected = {name: equal_to(record[name]) for name in fields}

    assert_that(result_yt_table.data[rec_n],
                has_entries(expected),
                'crs is the same as the given')
