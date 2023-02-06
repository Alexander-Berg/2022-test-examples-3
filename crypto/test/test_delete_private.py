import requests


FROZEN_TIME = 1600000000


def test_delete_private_ok(styx_client):
    response = styx_client.delete_private(puid=100500, service_ids=["matching"])

    assert requests.codes.ok == response.status_code
    assert '{"status":"ok"}' == response.text


def test_delete_private_no_service_ids(styx_client):
    response = styx_client.delete_private(puid=100500, service_ids=[])

    assert requests.codes.ok == response.status_code
    assert '{"status":"error","errors":[{"code":"no_service_ids","message":"No service ids in request"}]}' == response.text


def test_delete_private_unknown_service_id(styx_client):
    response = styx_client.delete_private(puid=100500, service_ids=["matching", "zzzzz"])

    assert requests.codes.ok == response.status_code
    assert '{"status":"error","errors":[{"code":"unknown_service_id","message":"Unknown service_id: zzzzz"}]}' == response.text
