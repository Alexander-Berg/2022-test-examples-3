from library.python.pytest.plugins.collection import LoadedModule


class CollectionPlugin(object):
    def __init__(self, test_modules):
        self._test_modules = test_modules

    def pytest_sessionstart(self, session):
        def collect(*args, **kwargs):
            for test_module in self._test_modules:
                module = LoadedModule.from_parent(name=test_module, parent=session)
                yield module

        session.collect = collect
