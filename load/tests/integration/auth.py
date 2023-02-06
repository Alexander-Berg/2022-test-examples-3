import requests

basic_url = 'http://localhost:5000'


def test_index_with_cookies():
    resp = requests.get('{}/'.format(basic_url), headers={'Authorization': 'OAuth edfhwrq'})
    assert resp.status_code == 200
    assert b'pysch pysch' in resp.content


def test_handler_without_auth_headers():
    resp = requests.get('{}/ping'.format(basic_url))
    assert resp.status_code == 401


def test_handler_with_invalid_oauth():
    resp = requests.get('{}/ping'.format(basic_url), headers={'Authorization': 'OAuth edfhwrq'})
    assert resp.status_code == 403


def test_handler_with_valid_oauth():
    pass


def test_no_connect_with_oauth():
    pass


def test_blackbox_error_on_oauth():
    pass


def test_no_login_in_oauth_data():
    pass
