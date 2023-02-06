import os

import pytest
import requests


@pytest.mark.parametrize("path", [
    "1/resource.txt",
    "2",
    "3"
])
def test_server(path):
    url = "{prefix}/{path}".format(prefix=os.environ["SANDBOX_PROXY_URL_PREFIX"], path=path)
    response = requests.get(url)
    return {
        "body": response.text,
        "code": response.status_code
    }
