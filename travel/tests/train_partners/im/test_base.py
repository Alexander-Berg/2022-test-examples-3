# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pickle

import mock
import pytest
import requests

import common.utils.yasmutil
from common.tester.utils.replace_setting import replace_setting
from common.utils.yasmutil import Metric
from travel.rasp.train_api import train_partners
from travel.rasp.train_api.train_partners import config
from travel.rasp.train_api.train_partners.im.base import (
    get_im_response, ImError, ImNonCriticalError, measurable, ImCredentialNotFoundError,
)
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_purchase.core.enums import TrainPartnerCredentialId


def _create_credentials():
    return {
        TrainPartnerCredentialId.IM: config.Credential('test_login', 'test_password', 'test_pos'),
        TrainPartnerCredentialId.IM_SUBURBAN: config.Credential('test_sub_login', 'test_sub_password', 'test_sub_pos'),
    }


@mock.patch.object(config, 'TRAIN_PARTNERS_IM_URL', 'https://test_url/')
@mock.patch.object(config, 'TRAIN_PARTNERS_CREDENTIALS', _create_credentials())
@pytest.mark.parametrize('credential_id, expected_pos', [
    (TrainPartnerCredentialId.IM, 'test_pos'),
    (TrainPartnerCredentialId.IM_SUBURBAN, 'test_sub_pos'),
])
def test_get_im_response(credential_id, expected_pos, httpretty):
    httpretty.register_uri(
        httpretty.POST,
        'https://test_url/Railway/V1/Foo/Bar',
        responses=[
            httpretty.Response('{"bar": {"foo": 1}}'),
            httpretty.Response('{"Code": 1, "Message": "FUUUU!", "MessageParams":[]}', status=500),
            httpretty.Response('{"Code": 311, "Message": "FUUUU!", "MessageParams": []}', status=400),
        ]
    )

    assert get_im_response('Railway/V1/Foo/Bar', {'foo': {'bar': 1}},
                           credential_id=credential_id) == {'bar': {'foo': 1}}
    request = httpretty.last_request
    assert 'Authorization' in request.headers
    assert request.headers['POS'] == expected_pos
    assert request.body == '{"foo": {"bar": 1}}'

    with pytest.raises(ImError):
        get_im_response('Railway/V1/Foo/Bar', {'foo': {'bar': 1}})
    with pytest.raises(ImNonCriticalError):
        get_im_response('Railway/V1/Foo/Bar', {'foo': {'bar': 1}})


def test_get_credential_not_found_error():
    with pytest.raises(ImCredentialNotFoundError):
        get_im_response('Railway/V1/Foo/Bar', {'foo': {'bar': 1}}, credential_id='undefined')


@replace_setting('YASMAGENT_ENABLE_MEASURABLE', True)
@pytest.mark.parametrize('exception, error_type', [
    (ImError(1, 'foo', None), 'communication'),
    (ImError(61, 'bar', None), 'other')
])
def test_measurable_handle_error(exception, error_type):
    @measurable(buckets=[10])
    def failing_func():
        raise exception

    with mock.patch.object(common.utils.yasmutil, 'YasmMetricSender') as m_yasm_metric_sender:
        with pytest.raises(exception.__class__):
            failing_func()

        assert m_yasm_metric_sender.mock_calls[0] == mock.call(prefix='im')
        assert m_yasm_metric_sender.mock_calls[1] == mock.call().send_many([
            Metric(name='failing_func.errors_cnt', value=1, suffix='ammm'),
            Metric(name='failing_func.{}_errors_cnt'.format(error_type), value=1, suffix='ammm'),
            Metric(name='errors_cnt', value=1, suffix='ammm'),
            Metric(name='{}_errors_cnt'.format(error_type), value=1, suffix='ammm'),
            Metric(name='failing_func.timings', value=[[0, 1]], suffix='ahhh')
        ])


class TestImError(object):
    @pytest.mark.parametrize('error, expected', [
        (ImError(1, 'error_message', None), False),
        (ImError(310, 'error_message', None), True),
        (ImError(311, 'error_message', None), False),
        (ImError(312, 'error_message', None), True),
        (ImError(313, 'error_message', None), True),
        (ImError(314, 'error_message', None), True),
        (ImError(315, 'error_message', None), True),
        (ImError(316, 'error_message', None), True),
        (ImError(317, 'error_message', None), False),
        (ImError(43, 'error_message', None), False),
        (ImError(43, 'error_message', ['request.DepartureDate']), True),
    ])
    def test_is_trains_not_found_error(self, error, expected):
        assert error.is_trains_not_found_error() == expected


def test_get_im_response_error_on_timeout(httpretty):
    def raise_timeout_exception(_request, _uri, _headers):
        raise requests.Timeout('Connection timed out.')

    mock_im(httpretty, 'SomeMethod', body=raise_timeout_exception)

    with pytest.raises(ImError):
        get_im_response('SomeMethod', params={})


def test_get_im_response_error_on_empty_response(httpretty):
    mock_im(httpretty, 'SomeMethod', body='')

    with pytest.raises(ImError):
        get_im_response('SomeMethod', params={})


def test_get_im_communication_error_on_502(httpretty):
    mock_im(httpretty, 'SomeMethod', body='', status=502)

    try:
        get_im_response('SomeMethod', params={})
    except ImError as e:
        assert e.is_communication_error()
    else:
        assert False


@replace_setting('YASMAGENT_ENABLE_MEASURABLE', True)
@pytest.mark.parametrize('method, metric, exception, error_type', [
    ('Search/TrainPricing', 'search_trainpricing', ImNonCriticalError(311, 'foo', None), 'non_critical'),
    ('Railway/V1/Search/TrainPricing', 'railway_v1_search_trainpricing', ImError(1, 'foo', None), 'communication'),
    ('Reservation/Create', 'reservation_create', ImError(-1, 'foo', None), 'communication'),
    ('Reservation/Cancel', 'reservation_cancel', ImError(61, 'bar', None), 'other')
])
def test_get_im_response_measurable_im(method, metric, exception, error_type):
    with mock.patch.object(common.utils.yasmutil, 'YasmMetricSender') as m_yasm_metric_sender, \
            mock.patch('common.utils.yasmutil.get_buckets', return_value=[10]):
        with pytest.raises(exception.__class__):
            with mock.patch.object(train_partners.im.base, '_get_im_response', side_effect=exception):
                get_im_response(method, {})

        assert m_yasm_metric_sender.mock_calls[0] == mock.call(prefix='im')
        assert m_yasm_metric_sender.mock_calls[1] == mock.call().send_many([
            Metric(name='{}.errors_cnt'.format(metric), value=1, suffix='ammm'),
            Metric(name='{}.{}_errors_cnt'.format(metric, error_type), value=1, suffix='ammm'),
            Metric(name='errors_new_cnt', value=1, suffix='ammm'),
            Metric(name='{}_errors_cnt'.format(error_type), value=1, suffix='ammm'),
            Metric(name='{}.im_response_codes'.format(metric), value=[[exception.code, 1]], suffix='ahhh'),
            Metric(name='{}.timings'.format(metric), value=[[0, 1]], suffix='ahhh'),
        ])


@pytest.mark.parametrize('im_error', [
    ImError(1, 'Ошибка', {'data': 42}),
    ImError(-1, 'Ошибка', None),
    ImNonCriticalError(311, 'foo', None),
])
def test_serialize_im_error(im_error):
    err = ImError(1, 'Ошибка', {'data': 42})
    _err = pickle.loads(pickle.dumps(err))
    assert err.get_data() == _err.get_data()
    assert type(err) == type(_err)
