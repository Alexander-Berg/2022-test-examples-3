import pytest

from yamarec_metarouter.agent import Agent
from yamarec_metarouter.dispatcher import Dispatcher
from yamarec_metarouter.loaders import FolderLoader
from yamarec_metarouter.loaders import JSONLoader
from yamarec_metarouter.pattern import Pattern
from yamarec_metarouter.tries import NaiveTrie
from yamarec_metarouter.tries import SmartTrie


@pytest.fixture
def agent(loader):
    handlers = {
        "/market/model": (lambda p: "model"),
    }
    return Agent(loader, handlers)


@pytest.fixture
def dispatcher():
    return Dispatcher(
        {
            Pattern("/product/([0-9]+)(\?|$)"): lambda p: ("model card", p),
            Pattern("/product//?\?id=([0-9]+)($|&|#)"): lambda p: ("weird model card", p),
            Pattern("/search(\?|$)"): lambda p: ("search", p),
            Pattern("/summertime(\?|$)"): lambda p: ("summertime", p),
        })


@pytest.fixture(params=[FolderLoader, JSONLoader])
def loader(request):
    data_folder = "resfs/file/market/yamarec/metarouter/tests/data/"
    static_folder = data_folder + "static/"
    dynamic_folder = data_folder + "dynamic/"
    result = FolderLoader([static_folder, dynamic_folder])
    if request.param is JSONLoader:
        result.load()
        result = JSONLoader(JSONLoader.serialize(result))
    return result


@pytest.fixture(params=[NaiveTrie, SmartTrie])
def trie(request):
    return request.param(
        {
            "/": 0,
            "/a": 1,
            "/ab": 2,
            "/abc": 3,
            "/ab/zzz": 4,
            "/c/": 5,
        })
