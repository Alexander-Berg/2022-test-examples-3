from library.python import resource
import os
import random
import json
from extsearch.unisearch.medicine.data_processing.test_lib import resource_records, dump_result_records, cmp_tables

from extsearch.unisearch.medicine.data_processing.doctor_key.from_latest_doctors.lib import doctor_keys_from_latest_doctors


def resource_statistics(path):
    assert resource.find(path) is not None
    return json.loads(resource.find(path))


def cmp_statistics(expected, output, output_name):
    output = {k: list(sorted(v)) if isinstance(v, set) else v for k, v in output.items()}
    if 1:
        print(os.path.abspath(output_name))
        s = open(output_name, 'w')
        print('output = ' + str(output))
        json.dump(output, s, sort_keys=True, indent=4)
    assert expected == output


def make_keys_test(folder_name):
    prefix = 'resfs/file/extsearch/unisearch/medicine/data_processing/doctor_key/from_latest_doctors/ut/data/' + folder_name
    group_info_table = resource_records(prefix + 'group_info_table.yson', as_dict=True)
    dsu_table = resource_records(prefix + 'dsu_table.yson', as_dict=True)
    latest_doctors = resource_records(prefix + 'latest_doctors.yson', as_dict=True)
    overriden_cur_year = 2020
    random.seed(123)

    group_info_table = list(group_info_table)  # TODO: del it
    dsu_table = list(dsu_table)
    latest_doctors = list(latest_doctors)
    result = doctor_keys_from_latest_doctors(group_info_table, dsu_table, latest_doctors, overriden_cur_year)

    cmp_list = [
        ('group_info_table', 'group_info_table_output.yson'),
        ('dsu_table', 'dsu_table_output.yson'),
        ('latest_doctors', 'latest_doctors_output.yson'),
    ]
    for result_key, output_name in cmp_list:
        dump_result_records(result[result_key], output_name, is_dict=True)
        expected = list(resource_records(prefix + output_name, as_dict=True))
        cmp_tables(expected, result[result_key])

    expected_statistics = resource_statistics(prefix + 'statistics_output.yson')
    cmp_statistics(expected_statistics, result['statistics'], 'statistics_output.yson')


def test_make_keys1():
    make_keys_test('test1/')
