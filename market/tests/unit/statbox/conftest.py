import pytest

from yamarec1.config.loader import load_from_string
from yamarec1.statbox import StatboxClient


@pytest.fixture
def settings():
    return """
---
dimensions:
- fielddate: date
- one: string
- two: number
measures:
- three: string
"""


@pytest.fixture
def report():
    return "Hola"


@pytest.fixture
def statbox_client():
    config = '''
url = "https://test.com"
user = "admin"
password = "admin"
token = None
request_timeout = 360
upload:
  attempt_timeout = 2
  num_of_attempts = 2
report_prefix = "Market_RU/Recommendations/Test/"
'''
    return StatboxClient(config=load_from_string(config))


@pytest.fixture
def statbox_client_with_token():
    config = '''
url = "https://test.com"
user = None
password = None
token = "XXXX-token"
request_timeout = 360
upload:
  attempt_timeout = 2
  num_of_attempts = 2
report_prefix = "Market_RU/Recommendations/Test/"
'''
    return StatboxClient(config=load_from_string(config))
