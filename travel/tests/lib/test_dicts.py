import asyncio
import json
import itertools
import pytest
import sys

from datetime import timedelta

from travel.avia.subscriptions.app.lib.dicts import Dict, PointKeyResolver
from travel.library.python.dicts.station_repository import StationRepository
from travel.library.python.dicts.settlement_repository import SettlementRepository
from travel.library.python.dicts.region_repository import RegionRepository
from travel.library.python.dicts.country_repository import CountryRepository


pytestmark = pytest.mark.asyncio

DICT_CLASS = (
    'travel.avia.subscriptions'
    '.app.lib.dicts.Dict'
)

BASE_REPO_CLASS = (
    'travel.library.python.dicts'
    '.base_repository.BaseRepository'
)

READ_RES_METHOD = 'sandbox.common.rest.Path.read'


async def test_dict_get_same_hash(mocked_dict, sandbox_response, download_res_response):
    # короткий период обновления
    dict_ = mocked_dict(timedelta(microseconds=1))
    sandbox_response.set({'items': [{'md5': 'val', 'type': 'res_type'}]})
    download_res_response.set(json.dumps({'id': 1, 'val': 'old_dict'}))
    assert await dict_.get(1) == {'id': 1, 'val': 'old_dict'}

    # то же значение хеша
    sandbox_response.set({'items': [{'md5': 'val', 'type': 'res_type'}]})
    download_res_response.set(json.dumps({'id': 1, 'val': 'new_dict'}))
    assert await dict_.get(1) == {'id': 1, 'val': 'old_dict'}


async def test_dict_get_not_updated_by_time(mocked_dict, sandbox_response, download_res_response):
    dict_ = mocked_dict(timedelta(minutes=5))
    sandbox_response.set({'items': [{'md5': 'val1', 'type': 'res_type'}]})
    download_res_response.set(json.dumps({'id': 1, 'val': 'old_dict'}))
    assert await dict_.get(1) == {'id': 1, 'val': 'old_dict'}

    # проверим, что при повторном вызове обновления не было
    sandbox_response.set({'items': [{'md5': 'val2', 'type': 'res_type'}]})
    download_res_response.set(json.dumps({'id': 1, 'val': 'new_dict'}))
    assert await dict_.get(1) == {'id': 1, 'val': 'old_dict'}


async def test_dict_get_updated_dict(mocked_dict, sandbox_response, download_res_response):
    # короткий период обновления
    dict_ = mocked_dict(interval=timedelta(microseconds=1))
    sandbox_response.set({'items': [{'md5': 'val1', 'type': 'res_type'}]})
    download_res_response.set(json.dumps({'id': 1, 'val': 'new_dict'}))
    assert await dict_.get(1) == {'id': 1, 'val': 'new_dict'}

    # Проверим, что повторный вызов также начнет обновление
    sandbox_response.set({'items': [{'md5': 'val2', 'type': 'res_type'}]})
    download_res_response.set(json.dumps({'id': 1, 'val': 'some_other_dict'}))
    assert await dict_.get(1) == {'id': 1, 'val': 'some_other_dict'}


async def test_dict_get_resource_not_found(mocked_dict, sandbox_response, download_res_response):
    dict_ = mocked_dict(timedelta(minutes=5))
    sandbox_response.set({'items': []})
    download_res_response.set(json.dumps({'id': 1, 'val': 'old_dict'}))
    assert await dict_.get(1) is None


async def test_dict_get_concurrently(mocked_dict, sandbox_response, download_res_response):
    concurrency_unit = 6
    dict_ = mocked_dict(timedelta(minutes=5))
    sandbox_response.set({'items': [{'md5': 'val1', 'type': 'res_type'}]})
    download_res_response.set(json.dumps({'id': 1, 'val': 'old_dict'}))

    # Проверим, что при одновременном доступе,
    # все запросы get получат свежий справочник
    # справочник свежий, так как это первый запрос
    expected = await asyncio.gather(*map(
        lambda _: dict_.get(1),
        range(concurrency_unit)
    ))
    assert expected == list(itertools.repeat(
        object={'id': 1, 'val': 'old_dict'},
        times=concurrency_unit
    ))

    # проверим, что при повторном вызове обновления не было
    sandbox_response.set({'items': [{'md5': 'val2', 'type': 'res_type'}]})
    download_res_response.set(json.dumps({'id': 1, 'val': 'new_dict'}))
    expected = await asyncio.gather(*map(
        lambda _: dict_.get(1),
        range(concurrency_unit)
    ))
    assert expected == list(itertools.repeat(
        object={'id': 1, 'val': 'old_dict'},
        times=concurrency_unit
    ))


async def test_point_key_resolver(mocker):
    mocker.patch(DICT_CLASS, side_effect=dict_factory)
    resolver = PointKeyResolver()

    assert await resolver.resolve('c21') == 'settlement'
    assert await resolver.resolve('s21') == 'station'
    assert await resolver.resolve('r21') == 'region'
    assert await resolver.resolve('l21') == 'country'


async def test_point_key_resolver_errors():
    resolver = PointKeyResolver()

    with pytest.raises(ValueError):
        await resolver.resolve('')

    with pytest.raises(ValueError):
        await resolver.resolve('c')

    with pytest.raises(ValueError):
        await resolver.resolve('cbadnumber')

    with pytest.raises(ValueError):
        await resolver.resolve('21')


@pytest.fixture()
def sandbox_response():
    return Result()


@pytest.fixture()
def download_res_response():
    return Result()


@pytest.fixture()
def mocked_dict(mocker, sandbox_response, download_res_response):
    def factory(interval=None):
        mocker.patch(
            READ_RES_METHOD,
            side_effect=sandbox_response.get
        )
        mock_cls = mocker.patch(
            BASE_REPO_CLASS,
            side_effect=MockRepository
        )
        dict_ = Dict(
            repository_cls=mock_cls,
            resource_type='SOME_TYPE',
            check_update_interval=interval
        )
        if sys.version_info >= (3, 8):
            mocker.patch.object(
                dict_, '_download_dict',
                side_effect=lambda *_: download_res_response.val
            )
        else:
            mocker.patch.object(
                dict_, '_download_dict',
                side_effect=a(download_res_response)
            )

        return dict_
    return factory


def dict_factory(*_, **kwargs):  # noqa
    repository_cls = kwargs.pop('repository_cls')
    if repository_cls is StationRepository:
        return MockDict('station')
    if repository_cls is SettlementRepository:
        return MockDict('settlement')
    if repository_cls is RegionRepository:
        return MockDict('region')
    if repository_cls is CountryRepository:
        return MockDict('country')


# pytest-mock не умеет работать с AsyncMock
def a(result: 'Result'):
    def async_wrapper(*_):  # noqa
        fut = asyncio.Future()
        fut.set_result(result.val)
        return fut

    return async_wrapper


class MockDict:
    def __init__(self, type_):
        self._type = type_

    async def get(self, _):  # noqa
        return self._type


class MockRepository:
    def __init__(self):
        self.storage = dict()

    def get(self, id_):
        return self.storage.get(id_)

    def load_from_string(self, content: str):
        entity = json.loads(content)
        self.storage[entity['id']] = entity


class Result:
    def __init__(self):
        self.val = None

    def set(self, val):
        self.val = val

    def get(self, *_, **__):  # noqa
        return self.val
