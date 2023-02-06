import os

import pytest
import requests


@pytest.mark.parametrize("path", [
    "version/rtmr-vla/present",
    "get/rtmr-vla/present/1",
    "get/rtmr-vla/present/2",
    "version/rtmr-vla/not_found",
    "get/rtmr-vla/not_found/1",
])
def test_server(path):
    url = "{prefix}/{path}".format(prefix=os.environ["RESOURCE_SERVICE_URL_PREFIX"], path=path)
    response = requests.get(url)
    return {
        "body": response.text,
        "code": response.status_code
    }
