import pytest

from .blackbox import UpperGeometasearchBlackbox, GeometasearchV2Blackbox
from .searcher import Context, PbSearcher, ProtoSearcher

# metasearches() fixture is defined externally


@pytest.fixture
def upper(metasearches):
    return UpperGeometasearchBlackbox(
        metasearches.upper, metasearches.log_paths.reqans if metasearches.log_paths else None
    )


@pytest.fixture
def metasearch(metasearches):
    return GeometasearchV2Blackbox(metasearches.upper)


@pytest.fixture
def context(metasearches):
    return Context(metasearches.upper)


@pytest.fixture
def pb_searcher(metasearches):
    return PbSearcher(metasearches.upper)


@pytest.fixture
def proto_searcher(metasearches):
    return ProtoSearcher(metasearches.upper)


@pytest.fixture(params=['proto', 'pb'])
def searcher(metasearches, request):
    return {
        'pb': PbSearcher(metasearches.upper),
        'proto': ProtoSearcher(metasearches.upper),
    }.get(request.param)
