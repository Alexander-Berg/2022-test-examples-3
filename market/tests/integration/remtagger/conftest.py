import uuid

import pytest

from yamarec1.beans import settings
from yamarec1.remtagger import RemTagger


@pytest.fixture
def tag1():
    return "some_tag1_" + uuid.uuid4().hex


@pytest.fixture
def tag2():
    return "some_tag2_" + uuid.uuid4().hex


@pytest.fixture
def remtagger():
    return RemTagger(config=settings.remtagger)
