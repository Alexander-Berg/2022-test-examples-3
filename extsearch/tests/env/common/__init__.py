import os
from collections import namedtuple

import yatest.common

LogPaths = namedtuple('LogPaths', ['reqans', 'trash_reqans'])
Metasearches = namedtuple('Metasearches', ['middle', 'upper', 'log_paths'])


def create_rearrange_data():
    os.mkdir('rearrange-data')
    os.symlink(yatest.common.binary_path('extsearch/geo/meta/rearrs/data'), 'rearrange-data/geosearch')


def get_log_paths():
    return LogPaths(
        reqans=yatest.common.output_path('reqanslog.txt'), trash_reqans=yatest.common.output_path('trash_reqans.txt')
    )


def set_up_middle(geometasearch, port, extra_params=[]):
    return [
        geometasearch,
        '-d',
        '-p',
        str(port),
        '-V',
        'ServerLog={}'.format(yatest.common.output_path('serverlog-addrsmiddle.txt')),
        '-V',
        'EventLog={}'.format(yatest.common.output_path('eventlog-addrsmiddle.bin')),
        '-V',
        'RearrangeDataDir=rearrange-data',
    ] + extra_params


def set_up_upper(geometasearch, port, extra_params=[]):
    log_paths = get_log_paths()
    return [
        geometasearch,
        '-d',
        '-p',
        str(port),
        '-V',
        'ServerLog={}'.format(yatest.common.output_path('serverlog-addrsupper.txt')),
        '-V',
        'EventLog={}'.format(yatest.common.output_path('eventlog-addrsupper.bin')),
        '-V',
        'ReqAnsLog={}'.format(log_paths.reqans),
        '-V',
        'ScarabReqAnsLog={}'.format(log_paths.trash_reqans),
        '-V',
        'RearrangeDataDir=rearrange-data',
    ] + extra_params


def set_up_v2(geometasearch_v2, port, extra_params=[]):
    return [
        geometasearch_v2,
        '-p',
        str(port),
        '-V',
        'EventLog={}'.format(yatest.common.output_path('eventlog.bin')),
    ] + extra_params
