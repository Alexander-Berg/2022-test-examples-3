import json
import re
import time

import yatest.common


def test_debug(upper):
    # fake test
    if yatest.common.get_param('DEBUG_PORT') is not None:
        while True:
            time.sleep(1)


def test_origin_required(upper):
    upper.options.auto_add_origin = False
    upper.set_query(text='аптеки', ll='37,55', spn='0.1,0.1')

    r = upper.get_raw(ms='pb')
    assert r.status_code == 400

    r = upper.get_raw(ms='pb', origin='test')
    assert r.status_code == 200


def test_response_formats(upper):
    upper.set_query(text='москва')

    r = upper.get_raw(ms='pb', hr='yes')
    assert 'reply {' in r.text

    r = upper.get_raw(ms='proto', hr='yes')
    assert 'Grouping {' in r.text

    r = upper.get_raw(ms='json')
    assert '_json_geosearch_version' in r.text

    r = upper.get_raw(ms='xml', original_host='search.maps.yandex.net')
    assert r.status_code == 400

    r = upper.get_raw(ms='xml')
    assert r.status_code == 400


def test_unistat_handle(upper):
    def _value(data, signal):
        assert isinstance(data, list)
        for k, v in data:
            if k == signal:
                return v

    r = upper.get_raw('tass', level=-1)
    data = json.loads(r.text)

    for signal in ['all_dhhh', 'all-wizbiz_dhhh', 'georeport-error-500_num_dmmm']:
        assert _value(data, signal) is not None
    assert _value(data, 'response_error') is None

    # Perform a request and check that it is counted
    requests_before = sum(num for _, num in _value(data, 'all-mapsform_dhhh'))
    for _ in range(3):
        upper.get_pb(text='аптеки', origin='maps-form')

    r = upper.get_raw('tass')
    data = json.loads(r.text)

    requests_after = sum(num for _, num in _value(data, 'all-mapsform_dhhh'))
    assert requests_after > requests_before


def test_info_requests(upper):
    r = upper.get_raw(info='getversion')
    assert 'Build info' in r.text

    r = upper.get_raw(info='getconfig')
    assert '<Server>' in r.text


def test_context(upper):
    r = upper.get_pb(text='москва', ll='37,55', spn='1,1')
    assert re.match('[a-zA-Z0-9/=]+', r.context())

    ctx = r.unpack_context()
    assert ctx.InitialWindow.Ll.X == 37.0
    assert ctx.InitialWindow.Ll.Y == 55.0


def test_bad_ll_spn(upper):
    upper.set_query(text='минск')

    r = upper.get_raw(ms='pb', ll='27.6,53.9', spn='0.1,0.1')
    assert r.status_code == 200

    r = upper.get_raw(ms='pb', ll='27.6,53.9')
    assert r.status_code == 200

    r = upper.get_raw(ms='pb', ll='abacaba')
    assert r.status_code == 400
    assert r.text == "Malformed request: invalid 'll' parameter (equal to 'abacaba')"

    r = upper.get_raw(ms='pb', ll='37,55', spn='fake')
    assert r.status_code == 400
    assert r.text == "Malformed request: invalid 'spn' parameter (equal to 'fake')"

    r = upper.get_raw(ms='pb', ll='37,55', spn='180,180')
    assert r.status_code == 400
    assert r.text == "Malformed request: windows containing polar regions are not supported"


def test_timestamp(upper):
    upper.set_query(text='минск')

    r = upper.get_raw(timestamp='1582822945')
    assert r.status_code == 200

    r = upper.get_raw(timestamp='invalid_timestamp')
    assert r.status_code == 400


def test_lang(upper):
    upper.set_query(text='минск', ms='pb')

    r = upper.get_raw(lang='ru')
    assert r.status_code == 200

    r = upper.get_raw(lang='fake')
    assert r.status_code == 400
    assert r.text == "Malformed request: invalid 'lang' parameter (equal to 'fake')"


def test_metasearch_v2(upper):
    upper.options.auto_add_origin = False
    r = upper.get_raw('fake')
    assert r.status_code == 404
    assert r.text.strip() == 'Collection "fake" not found.'

    r = upper.get_raw('search')
    assert r.status_code == 400
    assert r.text.startswith('Malformed request')

    r = upper.get_raw('search', origin='test')
    assert r.status_code == 400

    r = upper.get_raw('search', origin='test', business_oid=1306328558)
    assert r.status_code == 200
