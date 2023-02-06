import requests


def test_ping_positive(cm_client):
    response = cm_client.ping()

    assert requests.codes.ok == response.status_code, response.text
    assert "OK" == response.text.rstrip()
