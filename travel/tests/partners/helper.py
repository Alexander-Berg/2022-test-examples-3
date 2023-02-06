# -*- coding: utf-8 -*-
from datetime import datetime, date
import itertools
import mimetypes
import os
import urlparse

import ujson
import requests
from library.python import resource
from mock import Mock
from six import ensure_str, text_type, binary_type

from travel.avia.ticket_daemon.ticket_daemon.daemon.importer import Importer
from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price
from travel.avia.ticket_daemon.ticket_daemon.api.flights import FlightFabric, OperatingFlight
from travel.avia.ticket_daemon.ticket_daemon.api.query import Query
from travel.avia.ticket_daemon.ticket_daemon.api.flights import IATAFlight, Variant, Flights, Segment


def get_mocked_response(content_path, **response_info):
    class MockResponse(requests.Response):
        def __init__(self, status_code=200):
            super(MockResponse, self).__init__()

            mimetypes.init()
            self.headers['content-type'] = mimetypes.types_map[os.path.splitext(content_path)[1]]

            self.status_code = status_code
            self._content = get_content(content_path)

            for k, v in response_info.iteritems():
                setattr(self, k, v)

    return MockResponse()


class QueryMock(Query):
    def fill_iata_codes(self, point, direction):
        if point:
            setattr(self, 'iata_{}'.format(direction), point.iata)
            setattr(self, 'iata_real_{}'.format(direction), point.iata)
            setattr(self, 'station_iatas_{}'.format(direction), [point.iata])
            setattr(self, 'country_code_{}'.format(direction), point.code)

    def prepare_attrs_for_import(self):
        self.fill_iata_codes(self.point_from, 'from')
        self.fill_iata_codes(self.point_to, 'to')


class SettlementMock(object):
    def __init__(self, iata, code, id):
        self.iata = iata
        self.code = code
        self.id = id
        self.point_key = "c{}".format(id)


def get_query(**kwargs):
    """
    :rtype: ticket_daemon.api.query.Query
    """
    default_query_params = {
        'point_from': SettlementMock(iata='MOW', code='RU', id=213),
        'point_to': SettlementMock(iata='SVX', code='RU', id=54),
        'date_forward': date(2017, 1, 21),
        'date_backward': date(2017, 1, 24),
        'klass': 'economy',
        'passengers': {'infants': 0, 'adults': 1, 'children': 0},
        'national_version': 'ru',
        'service': 'ticket',
        'user_settlement': None,
        'created': (datetime(2017, 1, 15) - datetime(1970, 1, 1)).total_seconds(),
    }
    if kwargs:
        default_query_params.update(kwargs)
    query = QueryMock(**default_query_params)
    query.prepare_attrs_for_import()
    importer = Importer(
        code=None,
        partners=[],
        query=query,
        response_collector=None,
        flight_fabric=FlightFabric(),
        big_beauty_collectors=[Mock()]
    )
    return importer.q


def variants_diff(expected, actual):
    assert actual is not None
    diffs = []
    if len(expected) != len(actual):
        diffs.append('Number of variants mismatch: {}, but expected {}'.format(len(actual), len(expected)))
    for i, (exp, act) in enumerate(zip(expected, actual)):
        diff = exp.diff(act)
        if diff:
            diffs.append('#{}:\n{}'.format(i, diff))

    return 'Variants differ:\n' + ('\n'.join(diffs))


def assert_variants_equal(expected, actual):
    actual = list(actual.variants)  # After TimedChunk fixes
    if not actual == expected:
        raise AssertionError(variants_diff(expected, actual))


def _ensure_str(v):
    if type(v) is str:
        return v
    if not isinstance(v, (text_type, binary_type)):
        return ensure_str(str(v))
    return ensure_str(v)


class ComparableVariant(Variant):
    FLIGHT_ATTRS_CHECKS = (
        'arrival', 'avia_company', 'local_departure', 'number',
        'company', 'company_tariff', 'departure', 'local_arrival', 'baggage',
        'station_to', 'fare_code', 'station_from_iata', 'station_to_iata',
        'station_from', 'company_iata', 'selfconnect', 'fare_family',
        'operating',
    )
    ORDER_DATA_URL_FIELDS = ('url', 'm_url')
    selfconnect = None

    def __init__(self, **compare_fields):
        super(ComparableVariant, self).__init__()
        self._to_compare_fields = compare_fields.keys()
        assert self._to_compare_fields, 'Nothing to compare'
        for k, v in compare_fields.iteritems():
            setattr(self, k, v)

    def __eq__(self, other):
        return all(self._compare_fields(other))

    def diff(self, other):
        diffs = []
        for result, diff in self._compare_fields_results(other):
            if not result:
                diffs.append(diff)
        return '\n'.join(diffs) if diffs else 'No diff'

    def _compare_fields(self, other):
        for result, diff in self._compare_fields_results(other):
            yield result

    def _compare_fields_results(self, other):
        for attr in self._to_compare_fields:
            self_attr = getattr(self, attr)
            other_attr = getattr(other, attr)
            result = True
            diff_msgs = []

            if attr == 'order_data':
                if not self.__order_data_is_equal(other, diff_msgs):
                    result = False
            elif isinstance(other_attr, Flights):
                for i, (self_flight, other_flight) in enumerate(itertools.izip_longest(self_attr, other_attr.segments)):
                    if other_flight is None:
                        diff_msgs.append('Missing {} segment #{}: {}'.format(attr, i, self_flight))
                        result = False
                        continue
                    if self_flight is None:
                        diff_msgs.append('Extra {} segment #{}: {}'.format(attr, i, other_flight))
                        result = False
                        continue
                    for _attr in self.FLIGHT_ATTRS_CHECKS:
                        _self_attr = getattr(self_flight, _attr) if self_flight else None
                        _other_attr = getattr(other_flight, _attr)
                        if _attr == 'baggage':
                            if str(_self_attr) != str(_other_attr):
                                diff_msgs.append(self._diff_fmt(
                                    _attr, _self_attr, _other_attr, path='{}.{}'.format(attr, i),
                                ))
                                result = False
                        elif _self_attr != _other_attr:
                            diff_msgs.append(self._diff_fmt(
                                _attr, _self_attr, _other_attr, path='{}.{}'.format(attr, i),
                            ))
                            result = False

            elif attr == 'tariff':
                if isinstance(other_attr, Price) and not self.__objects_attrs_equals(self_attr, other_attr):
                    diff_msgs.append(self._diff_fmt(attr, self_attr, other_attr))
                    result = False
            elif self_attr != other_attr:
                diff_msgs.append(self._diff_fmt(attr, self_attr, other_attr))
                result = False
            yield result, '\n'.join(diff_msgs)

    @staticmethod
    def _diff_fmt(attr, self_attr, other_attr, path=None):
        attr_text = _ensure_str(attr)
        self_text = _ensure_str(self_attr)
        other_text = _ensure_str(other_attr)
        return 'Diff on {}.{} attribute. Expected: {} Actual: {}'.format(
            path or '', attr_text, self_text, other_text,
        )

    def __order_data_is_equal(self, other, diff_msgs):
        self_attr = self.order_data.copy()
        other_attr = other.order_data.copy()
        for url_field in self.ORDER_DATA_URL_FIELDS:
            self_url = urlparse.urlparse(self_attr.pop(url_field, ''))
            other_url = urlparse.urlparse(other_attr.pop(url_field, ''))

            for attr in ['scheme', 'netloc', 'path', 'params', 'fragment']:
                if getattr(self_url, attr) != getattr(other_url, attr):
                    diff_msgs.append('Diff on order_data {} query. Expected: {} Actual: {}'.format(
                        url_field, self_url, other_url,
                    ))
                    return False

            self_url_query = dict(urlparse.parse_qsl(self_url.query))
            other_url_query = dict(urlparse.parse_qsl(other_url.query))

            if self_url_query != other_url_query:
                diff_msgs.append('Diff on order_data {} query. Expected: {} Actual: {}'.format(
                    url_field, self_url_query, other_url_query,
                ))
                return False

        if self_attr == other_attr:
            return True

        dict_diff(self_attr, other_attr, diff_msgs, path='order_data')
        return False

    def __objects_attrs_equals(self, first, second):
        return first.__dict__ == second.__dict__


def dict_diff(expected, actual, diff_msgs, path=None):
    if not path:
        path = 'root'

    def __looks_like_dict(d):
        if not hasattr(d, '__getitem__'):
            return False
        if not hasattr(d, 'keys'):
            return False
        return True

    if not __looks_like_dict(expected) or not __looks_like_dict(actual):
        return

    expected_keyset = set(expected.keys())
    actual_keyset = set(actual.keys())
    if not expected_keyset == actual_keyset:
        for missing_key in expected_keyset - actual_keyset:
            diff_msgs.append('---- {}.{}'.format(path, missing_key))
        for extra_key in actual_keyset - expected_keyset:
            diff_msgs.append('++++ {}.{}'.format(path, extra_key))

    for k in (expected_keyset & actual_keyset):
        expected_value = expected[k]
        actual_value = actual[k]
        if expected_value == actual_value:
            continue
        if __looks_like_dict(expected_value) and __looks_like_dict(actual_value):
            dict_diff(expected_value, actual_value, diff_msgs, path='{}.{}'.format(path, k))
            continue
        diff_msgs.append('---- {path}.{key} {expected}\n++++ {path}.{key} {actual}'.format(
            path=path, key=_ensure_str(k), expected=_ensure_str(expected_value), actual=_ensure_str(actual_value),
        ))


def create_flight(**kwargs):
    selfconnect = kwargs.pop('selfconnect', None)
    baggage = kwargs.pop('baggage', None)
    fare_family = kwargs.pop('fare_family', None)
    flight = IATAFlight()

    for k, v in kwargs.iteritems():
        if k in ['local_departure', 'local_arrival'] and isinstance(v, basestring):
            v = datetime.strptime(v, '%Y-%m-%dT%H:%M')
        if k == 'operating' and v is not None:
            v = OperatingFlight(**v)
        setattr(flight, k, v)

    return Segment(flight, selfconnect=selfconnect, baggage=baggage, fare_family=fare_family)


def prepare_variant(variant_info):
    result_info = variant_info.copy()
    for key in variant_info.iterkeys():
        if key in ['forward', 'backward']:
            result_info[key] = [create_flight(**flight) for flight in variant_info[key]]
        elif key == 'tariff':
            tariff = {'currency': 'RUR'}
            try:
                tariff['value'] = float(variant_info[key])
            except TypeError:
                tariff.update(variant_info[key])
            result_info[key] = Price(**tariff)
        elif key == 'charter':
            result_info[key] = bool(variant_info[key])

    if 'klass' not in variant_info:
        result_info['klass'] = 'economy'
    return result_info


def create_variants(variants_info):
    return [ComparableVariant(**prepare_variant(variant)) for variant in variants_info]


def get_content(_file):
    filename = os.path.join('resfs/file', os.path.dirname(__file__), 'fixtures', _file)
    return resource.find(filename)


def get_data(_file):
    return ujson.loads(get_content(_file))


def expected_variants(json_fixture_path):
    return create_variants(get_data(json_fixture_path))
