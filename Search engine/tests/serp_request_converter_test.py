from serp_request_converter import convert_sr

pqp_string_key = "per-query-parameters"
pqp = {
    "additional-cgi": {},
    "additional-cookies": None,
    "allow-experiments": False,
    "iso-country": "RU",
    "map-bounds": None,
    "map-position": None,
    "query-text": "вий",
    "region-id": 54,
    "user-coordinates": None,
    "yandex-uid": "sg1WfP5F6OunOVqP7"
}


def test_convert_sr():
    assert convert_sr({pqp_string_key: pqp, "id": 3684})[pqp_string_key] == pqp


def test_convert_sr_with_intable_id():
    assert convert_sr({pqp_string_key: pqp, 'id': "1"})[pqp_string_key] == pqp


def test_convert_sr_with_uuid_id():
    assert convert_sr({pqp_string_key: pqp, 'id': "0d735a3b-50c7-4eab-8dc5-85dba101574a"})[pqp_string_key] == pqp


def test_convert_sr_geo():
    pqp_geo_input = {
        "additional-cgi": {},
        "additional-cookies": None,
        "allow-experiments": False,
        "map-bounds": None,
        "map-position": {
            "spn-x": 1.4,
            "spn-y": 0.6,
            "x": 37.542599,
            "y": 55.803312,
            "zoom": None
        },
        "query-text": "Театральная площадь, 5с3",
        "region-id": 213,
        "user-coordinates": None,
        "yandex-uid": None
    }
    expected_query = {
        "mapInfo": {
            "spnX": 1.4,
            "spnY": 0.6,
            "x": 37.542599,
            "y": 55.803312,
            "zoom": None
        },
        "text": "Театральная площадь, 5с3",
        "region": {
            "id": 213,
            "name": None
        },
        "country": None,
        "device": "DESKTOP",
        "uid": None,
        pqp_string_key: pqp_geo_input,
        "params": [],
        "id": 1,
    }
    assert convert_sr({pqp_string_key: pqp_geo_input, "id": "1"}) == expected_query
