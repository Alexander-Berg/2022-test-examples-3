import requests


def test_ping(styx_client):
    response = styx_client.ping()

    assert requests.codes.ok == response.status_code
    assert "pong" == response.text


# TODO(r-andrey): tests with different sets of permissions
