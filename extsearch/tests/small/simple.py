#!/usr/bin/env python
# -*- coding: utf-8 -*-

import yatest.common


class Suite(object):

        def __init__(self, db, requests, daemon_config='daemon_config.cfg'):
            self.requests = requests
            self.db = db
            self.daemon_config = daemon_config


ARGS = [
    ['simple', Suite(db='db', requests='request_simple.json')],
    ['override_surplus_filter', Suite(db='db', requests='request_override_surplus_filter.json')],
    ['blender_commands', Suite(db='db_with_blender_commands', requests='request_with_blender_command.json')],
    ['rtmr_data', Suite(db='db_with_data_requests', requests='request_with_rtmr_data_request.json')],
]


def get_args(suite):
    servant_client_path = yatest.common.binary_path('apphost/tools/servant_client/servant_client')
    app_host_ops_path = yatest.common.binary_path('search/tools/app_host_ops/app_host_ops')
    daemon_path = yatest.common.binary_path('extsearch/wizards/fastres2/daemon/daemon')
    return [
        '--servant', servant_client_path,
        '--apphost_ops', app_host_ops_path,
        '--daemon', daemon_path,
        '--daemon_cfg', suite.daemon_config,
        '--geodb', 'geodb.data',
        '--db', 'file://' + suite.db,
        '--requests', suite.requests
    ]


def get_test(i):
    args = ARGS[i]
    get_responses_path = yatest.common.source_path('extsearch/wizards/fastres2/daemon/tests/tools/get_responses.py')
    return lambda: yatest.common.canonical_py_execute(get_responses_path, file_name=args[0], args=get_args(args[1]))


for idx in range(len(ARGS)):
    vars()['test_' + ARGS[idx][0]] = get_test(idx)
