import re
import requests


def test_response_pattern(cm_client):
    response = cm_client.version()

    assert response.status_code == requests.codes.ok
    assert re.search(r"Built from Arcadia rev\. -?\d+", response.text)


def test_valid_subclient(cm_client):
    response = cm_client.version(subclient='test')
    assert requests.codes.ok == response.status_code, response.text


def test_invalid_client(cm_client):
    response = cm_client.version(subclient='cm_client.with.dots')
    assert requests.codes.bad_request == response.status_code, response.text
