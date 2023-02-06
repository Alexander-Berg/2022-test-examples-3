import pytest

from yamarec1.config.loader import load_from_string
from yamarec1.remtagger import RemTagger


@pytest.fixture
def tag():
    return "some_tag"


@pytest.fixture
def remtagger():
    config = '''
url = "https://test.com"
prefix = "market_recom_test_"
'''
    return RemTagger(config=load_from_string(config))
