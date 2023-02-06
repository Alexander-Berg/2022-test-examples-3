import requests
import pytest


@pytest.mark.usefixtures('public_api')
def test_healthcheck(port_for_healthcheck):
    resp = requests.get(f'http://localhost:{port_for_healthcheck}/health', timeout=2)
    resp.raise_for_status()
