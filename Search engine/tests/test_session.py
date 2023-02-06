# -*- coding: utf-8 -*-
from rtcc.core.dataprovider import DataProvider
from rtcc.core.session import Session


def test_session():
    class TestProvider(DataProvider):
        name = "test"

        def get(self, param1, **kwargs):
            return param1

        def is_get_failed(self, get_result):
            return False

    session = Session(None)
    provider = TestProvider()
    provider._session = session
    value = provider.get(param1=1)
    assert value == 1


def test_session_call_count():
    class TestProvider(DataProvider):
        name = "test"

        def get(self, param1, **kwargs):
            self.__class__.call_count += 1
            return param1

        def is_get_failed(self, get_result):
            return False

        call_count = 0

    session = Session(None)
    assert session.register(TestProvider()).get(param1=1) == 1
    assert session.register(TestProvider()).get(param1=2) == 2
    assert session.register(TestProvider()).get(param1=2) == 2
    assert session.register(TestProvider()).call_count == 2
