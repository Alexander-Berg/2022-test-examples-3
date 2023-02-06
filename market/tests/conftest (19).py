import logging

import pytest
from _pytest.monkeypatch import MonkeyPatch


class YqlClientMock:
    class QueryMock:
        def __init__(self, *args, **kwargs) -> None:
            super().__init__()

        def run(self, *args, **kwargs):
            return True

    def __init__(self, *args, **kwargs) -> None:
        super().__init__()

    def query(self, *args, **kwargs):
        logging.info('YqlClientMock mock query')
        return self.QueryMock()


def return_true():
    return True


class ChytMock:
    @staticmethod
    def execute(*args, **kwargs):
        logging.info('ChytMock mock query')
        return ({'value': 1} for _ in range(1))


@pytest.fixture
def yql_mock():
    def _get_yql_mock(*args, **kwargs):
        return YqlClientMock()

    from market.monetize.stapler.v1.tasks import yql
    mp = MonkeyPatch()
    mp.setattr(yql, 'get_yqt_client', _get_yql_mock)

    yield mp

    mp.undo()


@pytest.fixture
def chyt_mock():
    from market.monetize.stapler.v1.tasks import chyt

    mp = MonkeyPatch()
    mp.setattr(chyt, 'get_yt_client', return_true)
    mp.setattr(chyt, 'chyt', ChytMock)

    yield mp

    mp.undo()
