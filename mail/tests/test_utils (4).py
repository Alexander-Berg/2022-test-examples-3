import datetime
import decimal
import json
import os
import random
import socket
from dataclasses import dataclass, field
from decimal import Decimal
from enum import Enum, unique
from typing import Any, List
from uuid import UUID, uuid4

import pytest

from sendr_pytest import *  # noqa
from sendr_utils import (
    MISSING, PREFERRED_DCS, SentinelMissing, alist, get_dc, get_hostname, json_value, sort_hosts_by_geo,
    str_to_underscore, temp_set, without_missing, without_none
)


@pytest.mark.parametrize('dc', PREFERRED_DCS.keys())
def test_sort_hosts_by_geo(dc):
    hosts = list(PREFERRED_DCS[dc])
    random.shuffle(hosts)
    assert sort_hosts_by_geo(hosts, dc) == list(PREFERRED_DCS[dc])


class TestGetHostName:
    ENV_VAR_NAMES = ('QLOUD_DISCOVERY_INSTANCE', 'DEPLOY_POD_PERSISTENT_FQDN')

    @pytest.fixture(params=ENV_VAR_NAMES + (None,))
    def env_var_name(self, request):
        return request.param

    @pytest.fixture
    def instance_name(self, rands):
        return rands()

    @pytest.fixture(autouse=True)
    def setup(self, env_var_name, instance_name):
        for env_var in self.ENV_VAR_NAMES:
            os.environ.pop(env_var, None)
        if env_var_name:
            os.environ[env_var_name] = instance_name

    def test_get_hostname(self, instance_name, env_var_name):
        assert get_hostname() == (instance_name if env_var_name else socket.getfqdn())


class TestGetDc:
    ENV_VAR_NAMES = ('DEPLOY_NODE_DC', 'QLOUD_DATACENTER')

    @pytest.fixture(params=ENV_VAR_NAMES + (None,))
    def env_var_name(self, request):
        return request.param

    @pytest.fixture
    def dc_name(self):
        return 'AAAaaa'

    @pytest.fixture(autouse=True)
    def setup(self, env_var_name, dc_name):
        for env_var in self.ENV_VAR_NAMES:
            os.environ.pop(env_var, None)
        if env_var_name:
            os.environ[env_var_name] = dc_name

    def test_get_hostname(self, dc_name, env_var_name):
        assert get_dc('default') == (dc_name.lower() if env_var_name else 'default')


class TestAlist:
    @pytest.fixture
    def expected(self):
        return ['a', 1, 'b', 2, 3]

    @pytest.fixture
    def async_generator(self, expected):
        async def dummy():
            for item in expected:
                yield item

        return dummy()

    @pytest.fixture
    async def result(self, async_generator):
        return await alist(async_generator)

    def test_result(self, result, expected):
        assert result == expected


class TestTempSet:
    @pytest.fixture
    def attr(self):
        return 'some_attr'

    @pytest.fixture
    def value(self):
        return 'some_value'

    @pytest.fixture
    def some_object(self):
        class SomeClass:
            pass

        return SomeClass()

    def test_missing(self, attr, value, some_object):
        with temp_set(some_object, attr, value):
            inside_value = getattr(some_object, attr)
        assert inside_value == value and not hasattr(some_object, attr)

    def test_with_old_value(self, attr, value, some_object):
        old_value = 'old_value'
        setattr(some_object, attr, old_value)
        with temp_set(some_object, attr, value):
            inside_value = getattr(some_object, attr)
        assert inside_value == value and getattr(some_object, attr) == old_value


@pytest.mark.parametrize('dict_,expected', (
    ({}, {}),
    (
        {'key': 'value', 'key2': None},
        {'key': 'value'},
    ),
    (
        {'key': None, 'key2': None},
        {},
    ),
    (
        {'key': {'nested_key': None}},
        {'key': {'nested_key': None}},
    ),
))
def test_without_none(dict_, expected):
    assert without_none(dict_) == expected


def test_without_missing():
    dictionary = {'key': 'value', 'isnone': None, 'missing': MISSING, 'sentinel': SentinelMissing()}
    expected = {'key': 'value', 'isnone': None}
    assert without_missing(dictionary) == expected


class TestJsonValue:
    """Желательно проверять, что значение успешно сериализуется"""

    @unique
    class MyEnum(Enum):
        X = 'x'

    pure_object = object()

    @pytest.mark.parametrize(
        'obj, expected',
        (
            pytest.param(pure_object, repr(pure_object), id='arbitrary-object'),
            # primitive scalars
            (1, 1),
            ("foo", "foo"),
            (1.42, 1.42),
            (False, False),
            (None, None),
            # vectors
            ([MyEnum.X], ["x"]),
            ({'key': MyEnum.X}, {'key': 'x'}),
            ({MyEnum.X}, ['x']),
            ((MyEnum.X,), ['x']),
            pytest.param({MyEnum.X: 1}, {'x': 1}, id='dict-with-unusual-key'),
            # complex scalars
            (decimal.Decimal('1.23456789'), '1.23456789'),
            (UUID('68f4ba0d-110f-4784-9651-0a29e00deb83'), '68f4ba0d-110f-4784-9651-0a29e00deb83'),
            (
                datetime.datetime(
                    year=2021,
                    month=12,
                    day=30,
                    hour=23,
                    minute=59,
                    second=59,
                    microsecond=999,
                    tzinfo=datetime.timezone.utc,
                ),
                '2021-12-30 23:59:59.000999+00:00'
            ),
            (datetime.date(2000, 12, 30), '2000-12-30'),
        )
    )
    def test_dumps(self, obj, expected):
        assert expected == json.loads(json.dumps(json_value(obj)))

    def test_dataclass_dump(self):
        @dataclass
        class D:
            name: str
            age: int
            list_of_strings: List[str]
            uuid: UUID
            dt: datetime.datetime
            date: datetime.date
            decimal: Decimal
            data: Any

        uuid = uuid4()

        d = D(
            name='Hey',
            age=100,
            list_of_strings=['a', 'b'],
            uuid=uuid,
            dt=datetime.datetime.utcnow(),
            date=datetime.date.today(),
            decimal=Decimal('10.00001'),
            data=dict(a='a'),
        )

        assert json.loads(json.dumps(json_value(d))) == dict(
            name=d.name,
            age=d.age,
            list_of_strings=['a', 'b'],
            uuid=str(uuid),
            dt=d.dt.isoformat(sep=' '),
            date=d.date.isoformat(),
            decimal='10.00001',
            data={'a': 'a'},
        )

    def test_dataclass_with_hidden_fields_dump(self):
        @dataclass
        class A:
            name: str
            password: str = field(init=True, repr=False)

        @dataclass
        class B:
            a: A
            secret_number: int = field(init=True, repr=False)
            default: str = field(init=False, default='default')

        b = B(A('bob', 'lol'), 42)
        assert json_value(b) == dict(
            a={'name': 'bob'},
            default='default',
        )


class TestStrToUnderscore:
    def test_str_to_underscore(self):
        assert str_to_underscore('fooBar') == 'foo_bar'
