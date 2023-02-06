import requests


def test_version(styx_client):
    response = styx_client.version()

    assert requests.codes.ok == response.status_code
    assert "Crypta Styx API\nBuilt from Arcadia rev. " in response.text


# TODO(r-andrey): tests with different sets of permissions
