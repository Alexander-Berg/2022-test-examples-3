#!/usr/bin/python -tt
import unittest
import json
import mock
import pytest

import tests.utils as utils
from test_input import attach_log
from logbroker_client_common.handler import CommonHandler


@pytest.fixture
def handler():
    # Fake tnef parser: not available in testing environment.
    # (todo: perhaps, something more meaningful than just a mock?)
    tnef_parser = mock.MagicMock()
    tnef_parser.parse.return_value = True

    conf = utils.get_conf("configs/windat/development.json")
    cls_conf = conf['workers']['args']['handler']['args']
    cls_conf['stream']['windat_attaches']['args'].update({'tnef_parser_lib': tnef_parser})
    return CommonHandler(
        **cls_conf
    )


@pytest.fixture
def process_attachment_mock(mocker):
    return mocker.patch('logbroker_processors.windat.processor.process_windat')


@pytest.mark.parametrize('header,data,expected', (
    (attach_log.header, attach_log.data_non_windat, 0),
    (attach_log.header, attach_log.data_windat, 1),
    (attach_log.header, attach_log.data_crap, 0),
))
def test_handle_not_windat(handler, process_attachment_mock, header, data, expected):
    handler.process(header, data)
    assert process_attachment_mock.call_count == expected
