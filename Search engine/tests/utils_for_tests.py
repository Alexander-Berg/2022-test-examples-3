# -*- coding: utf-8 -*-

import os

import mock

import yatest.common

__author__ = 'denkoren'

resource_lines = {
    "correct": {"full": "{l0}\t"
                        "/path/to/archive/archive_{l2000:0>4d}.{l1:0>2d}.{l1:0>2d}_00-00\t"
                        "/path/to/extracted/data/{l2000:0>4d}.{l1:0>2d}.{l1:0>2d}_00-00\n",
                "part": "{l0}\t/path/to/archive/archive_{l2000:0>4d}.{l1:0>2d}.{l1:0>2d}_00-00\t\n"},
    "broken": {"spaces": "{l0} broken record\n",
               "empty_fields": "{l0}\t\t\n",
               "balderdash": "asdasjkdh{l1}asd{l2000}j23adklj1{l0}"}
}

datapack_lines = {
    "correct": {"3id": "{l0}\t{l1000}  \t  {l2000}\n",
                "2id": "{l0}\t{l1000}\n",
                "empty": "\t\t\t\n"},
    "broken": {"spaces": "{l0} {l1000} {l2000}\n",
               "not_numbers": "ghlh\ttyry\tasd",
               "balderdash": "asdasd{l2}ae39{l0}wow{l1}dd\n"}
}

test_config_path = yatest.common.source_path('search/pumpkin/yalite_service/tests/configurations/test.conf')


def update_testfile(tempfile, data):
    f = open(tempfile.name, "w")
    f.truncate(0)
    f.writelines(data)
    f.close()


def generate_file_data(generate_params):
    result_data = []
    line_number = 0
    for param in generate_params:
        for i in xrange(param["count"]):
            result_data.append(param["line"].format(l0=line_number,
                                                    l1=line_number + 1,
                                                    l2=line_number + 2,
                                                    l1000=line_number + 1000,
                                                    l2000=line_number + 2000,
                                                    l3000=line_number + 3000,
                                                    l4000=line_number + 4000))
            line_number += 1

    return result_data


def fill_testfile(temp_file, generate_params):
    data = generate_file_data(generate_params)
    update_testfile(tempfile=temp_file, data=data)


def generate_test_resource():
    """
    Prepare resource chain for tests: each resource has it's own ID,
     'main_resource' depends on 'depend_resource' by special attribute value.
    """
    depend_resource_dict = {
        u'id': 321,
        u'attributes': {}
    }

    main_resource_dict = {
        u'id': 123,
        u'attributes': {
            'index_archive_id': 321,
            'status_host': 'some_test_host_name',
            'status_timestamp': 100000,
            'status': 'TEST_OK'
        }
    }

    main_resource = mock.MagicMock()
    main_resource.__getitem__.side_effect = lambda k: main_resource_dict[k]

    depend_resource = mock.MagicMock()
    depend_resource.__getitem__.side_effect = lambda k: depend_resource_dict[k]
    return main_resource, depend_resource


def mock_core_collector_lookup(core, return_resource):
    # Patch collector 'lookup' function: return predefined resource instead of real
    # resource lookup
    lookup_res = mock.Mock()
    lookup_res.resource = return_resource
    lookup = mock.Mock(return_value=lookup_res)

    core.collector.lookup = lookup

    return lookup


def mock_core_collector_get_resource(core):
    main_resource, depend_resource = generate_test_resource()

    # Patch collector 'get_resource_failsafe' function: return predefined resource
    # instead of real resource getting from SandBox.
    get_resource = mock.Mock(return_value=main_resource)

    core.collector.get_resource_failsafe = get_resource

    return get_resource


def mock_core_collector_deploy(core):
    main_resource, depend_resource = generate_test_resource()

    # Patch collector 'deploy' function: return predefined ResourceRecord instead of
    # real resource deploying.
    deploy = mock.Mock()
    deploy.return_value = (main_resource, "archive_path", "data_path")

    core.collector.deploy = deploy

    return deploy


def mock_core_cachefiles(core, downloads, datapacks):
    # Change cache file paths to temporary test file instead of config's path.
    core.config.cache_success_downloads = downloads.name
    core.config.cache_datapacks = datapacks.name


def switch_flag_file(_file, status):
    if status:
        if not os.path.isfile(_file):
            file(_file, "w")
    else:
        if os.path.isfile(_file):
            os.remove(_file)
