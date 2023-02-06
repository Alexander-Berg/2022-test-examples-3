import os

import pytest
import requests


@pytest.mark.parametrize("events", [
    pytest.param([{}], id="invalid event"),
    pytest.param([{}, {"host": "localhost", "service": "service", "description": "description", "status": "OK"}], id="invalid and valid event"),
    pytest.param([{"host": "localhost", "service": "service", "description": "description", "status": "OK"}], id="valid event")
])
def test_server(events):
    url = "{}/events".format(os.environ["JUGGLER_PUSH_URL_PREFIX"])
    response = requests.post(url, json={
        "source": "test",
        "events": events
    })
    return {
        "status_code": response.status_code,
        "body": response.json()
    }
