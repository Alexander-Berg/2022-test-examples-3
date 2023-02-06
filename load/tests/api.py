import requests

basic_url = 'http://localhost:5000'


def test_index():
    resp = requests.get('{}/'.format(basic_url))
    assert resp.status_code == 200
    assert b'pysch pysch' in resp.content
