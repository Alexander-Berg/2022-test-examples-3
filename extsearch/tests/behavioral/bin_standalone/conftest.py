import pytest

from extsearch.geo.meta.tests.env.common import Metasearches
from extsearch.geo.meta.tests.behavioral.steps import *  # noqa
from extsearch.geo.meta.tests.search.fixtures import *  # noqa

from extsearch.geo.meta.tests.behavioral.bin_standalone import metasearch as ms


@pytest.fixture
def metasearches():
    return Metasearches(middle=None, upper=ms.ENDPOINT, log_paths=None)
