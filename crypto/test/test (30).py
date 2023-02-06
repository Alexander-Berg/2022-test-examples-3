import json
import os

import pytest
import requests


@pytest.mark.parametrize("type,released", [
    ("TEST_RESOURCE", "testing"),
    ("TEST_RESOURCE", "stable"),
    ("NOT_FOUND", "stable"),
])
def test_server(type, released):
    url = "{}/resource".format(os.environ["SANDBOX_API_URL_PREFIX"])
    return requests.get(url, params={
        "type": type,
        "attrs": json.dumps({
            "released": released
        }),
        "order": "-id",
        "limit": 1,
        "state": "READY"
    }).json()
