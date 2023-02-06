import pytest

from extsearch.geo.meta.tests.blackbox.report_wrapper.geometasearch import GeometasearchV2Blackbox


@pytest.fixture
def metasearch(metasearches):
    return GeometasearchV2Blackbox(metasearches.upper)
