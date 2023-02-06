import pytest
import requests

basic_url = 'http://localhost:5000'


def test_index():
    resp = requests.get('{}/'.format(basic_url))
    assert resp.status_code == 200
    assert b'pysch pysch' in resp.content


@pytest.mark.xfail(reason='Swagger branch not yet fully merged')
def test_swagger():
    resp = requests.get('{}/api/v3/swagger.json'.format(basic_url))
    assert resp.status_code == 200
