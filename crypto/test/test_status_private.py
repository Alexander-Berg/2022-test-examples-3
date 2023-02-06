import requests


FROZEN_TIME = 1600000000


def test_status_private_ok(styx_client):
    response = styx_client.status_private(puid=100500)

    assert requests.codes.ok == response.status_code
    assert '{"status":"ok","data":[{"id":"matching","slug":"matching","state":"ready_to_delete","update_date":""}]}' == response.text


def test_status_private_zero_puid(styx_client):
    response = styx_client.status_private(puid=0)

    assert requests.codes.ok == response.status_code
    assert '{"status":"error","errors":[{"code":"zero_default_puid","message":"Default puid is zero"}]}' == response.text
