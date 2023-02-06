#!/usr/bin/python -tt
import unittest
import json
import mock
import pytest
from hamcrest import assert_that, all_of, equal_to

import utils
from test_input import imap_solog, wmi_solog, mops_solog, fastsrv_solog
from logbroker_client_common.handler import CommonHandler


@pytest.fixture
def handler():
    conf = utils.get_conf("configs/report-so/development.json")
    cls_conf = conf['workers']['args']['handler']['args']
    return CommonHandler(**cls_conf)


@pytest.fixture
def userinfo_mock(mocker):
    userinfo = mocker.patch('logbroker_processors.so_report.processor.Blackbox.userinfo')
    return userinfo


@pytest.fixture
def so_request_mock(mocker):
    return mocker.patch('logbroker_processors.so_report.base.so.SoAPI._make_request')


@pytest.fixture
def meta_request_mock(mocker):
    return mocker.patch('logbroker_processors.so_report.base.meta.MetaMailAPI._make_request')


@pytest.mark.parametrize('meta,header,data,expected_payload', (
    (imap_solog.spam_meta, imap_solog.header, imap_solog.spam, imap_solog.spam_payload),
    (imap_solog.move_meta, imap_solog.header, imap_solog.move, imap_solog.move_payload),
    (imap_solog.not_spam_meta, imap_solog.header, imap_solog.not_spam, imap_solog.not_spam_payload),
    (mops_solog.move_meta, wmi_solog.header, mops_solog.move, mops_solog.move_payload),
    (mops_solog.foo_meta, wmi_solog.header, mops_solog.foo, mops_solog.foo_payload),
    (mops_solog.antifoo_meta, wmi_solog.header, mops_solog.antifoo, mops_solog.antifoo_payload),
    (mops_solog.delete_meta, wmi_solog.header, mops_solog.delete, mops_solog.delete_payload),
    (fastsrv_solog.move_meta, fastsrv_solog.header, fastsrv_solog.move, fastsrv_solog.move_payload),
    (fastsrv_solog.flag_meta, fastsrv_solog.header, fastsrv_solog.flag, fastsrv_solog.flag_payload),
    (fastsrv_solog.filtered_meta, fastsrv_solog.header, fastsrv_solog.filtered, fastsrv_solog.filtered_payload),
))
def test_handler(handler, userinfo_mock, so_request_mock, meta_request_mock, meta, header, data, expected_payload):
    meta_request_mock.return_value = meta
    userinfo_mock.return_value = {'users': [{
        'login': 'test_user',
        'karma': {'value': 0},
        'karma_status': {'value': 0}
    }]}

    handler.process(header, data)
    handler.flush(True)

    assert so_request_mock.called

    payload = so_request_mock.call_args[0][0]
    called_json = json.loads(payload['json'])
    excepted_json = json.loads(expected_payload['json'])

    assert called_json == excepted_json
