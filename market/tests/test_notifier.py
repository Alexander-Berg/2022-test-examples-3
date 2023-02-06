# coding: utf-8

import mock
import os
import sys

import pytest

from core import Core
import notifier
import yatest.common


COREDUMP = '/coredumps/ISS-AGENT%report%iss_hook_start%report.782010.S11.20180420T153848.core'


@pytest.fixture(scope='function')
def mocked_core(mocker):
    return mocker.patch.object(Core, 'request_info', new_callable=mock.PropertyMock, return_value=None)


def test_parse_test_ids():
    url = (
        'http://man2-3209.yandex.net:17051/yandsearch?use-multi-navigation-trees=1&'
        'ip=176.226.200.174%2C176.226.200.174%2C176.226.200.174&ip-rids=56&pg=18&'
        'test-buckets=24%2C0%2C0%3B26%2C0%2C8%3B2a%2C0%2C0%3B25%2C098%3B19%2C%2C0%2C66%3B24%2C0%2C0%3B27%2C0%2C16&'
        'x-yandex-icookie=8786212691581964519&'
    )
    # 24,0,0;26,0,8;2a,0,0;25,098;19,,0,66;24,0,0;27,0,16
    assert {'24', '26', '27'} == notifier.parse_test_ids(url)

    url = (
        'http://man2-3209.yandex.net:17051/yandsearch?use-multi-navigation-trees=1&'
        'test-buckets=271244,0,74;266229,0,63;228149,0,38;200208,0,76;101610,0,20'
    )
    if sys.version_info >= (3, 9, 2):
        assert {'271244', '266229', '228149', '200208', '101610'} == notifier.parse_test_ids(url)
    else:
        assert {'271244'} == notifier.parse_test_ids(url)

    url = (
        'http://man2-3209.yandex.net:17051/yandsearch?use-multi-navigation-trees=1&'
        'ip=176.226.200.174%2C176.226.200.174%2C176.226.200.174&ip-rids=56&pg=18&'
    )
    assert len(notifier.parse_test_ids(url)) == 0


@pytest.mark.parametrize('req_info_filename, target_thread_only, test_ids', [
    ('request_info', False, [101609, 200206, 268839, 268859, 271815, 272587, 273261]),
    ('request_info', True, [268839, 200206, 101609]),
    ('request_info_wo_thread', False, {200206, 268859, 271815, 272587, 273261}),
    ('request_info_wo_thread', True, []),
    ('request_info_empty', False, []),
    ('request_info_empty', True, []),
])
def test_parse_core_request_info(mocked_core, req_info_filename, target_thread_only, test_ids):
    def to_str(s):
        return {str(i) for i in s}
    path = yatest.common.source_path(os.path.join('market/tools/corewatcher-rtc/tests/data', req_info_filename))
    with open(path) as f:
        mocked_core.return_value = f.read()
        assert to_str(test_ids) == notifier.get_test_ids(Core(COREDUMP), target_thread_only)
