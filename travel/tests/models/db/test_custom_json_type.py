# coding=utf-8
from __future__ import unicode_literals

import pytest

from dataclasses import dataclass, field
from marshmallow import Schema, fields, ValidationError
from marshmallow_dataclass import class_schema
from sqlalchemy import Column
from sqlalchemy.dialects import sqlite, postgresql
from typing import List, Optional

from travel.avia.subscriptions.app.model.db import Base, DBInstanceMixin, Json, JSONEncodedDict
from travel.avia.subscriptions.app.model.storage import DatabaseStorage


def test_mutable_column_is_tracked(Table, session_provider):
    with session_provider() as session:
        Table(session).create()
    with session_provider() as session:
        assert Table(session).get(id=1).json_list == []

    with session_provider() as session:
        t = Table(session).get(id=1)
        t.json_list.append(DataClass('1'))
    with session_provider() as session:
        assert Table(session).get(id=1).json_list == [DataClass('1')]

    with session_provider() as session:
        t = Table(session).get(id=1)
        t.json_list.insert(0, DataClass('2'))
    with session_provider() as session:
        assert Table(session).get(id=1).json_list == [
            DataClass('2'), DataClass('1')
        ]

    with session_provider() as session:
        t = Table(session).get(id=1)
        t.json_list[1] = DataClass('3')
    with session_provider() as session:
        assert Table(session).get(id=1).json_list == [
            DataClass('2'), DataClass('3')
        ]

    with session_provider() as session:
        t = Table(session).get(id=1)
        del t.json_list[0]
    with session_provider() as session:
        assert Table(session).get(id=1).json_list == [DataClass('3')]

    with session_provider() as session:
        t = Table(session).get(id=1)
        t.json_list.remove(DataClass('3'))
    with session_provider() as session:
        assert Table(session).get(id=1).json_list == []

    with session_provider() as session:
        t = Table(session).get(id=1)
        t.json_list.extend([DataClass('1'), DataClass('2'), DataClass('1')])
    with session_provider() as session:
        assert Table(session).get(id=1).json_list == [
            DataClass('1'), DataClass('2'), DataClass('1')
        ]

    with session_provider() as session:
        t = Table(session).get(id=1)
        t.json_list.pop()
    with session_provider() as session:
        assert Table(session).get(id=1).json_list == [
            DataClass('1'), DataClass('2')
        ]


def test_load_dialect_impl(json_type):
    # Для постгреса отдаем JSONB
    assert isinstance(
        json_type.load_dialect_impl(postgresql.dialect()),
        postgresql.JSONB
    )
    # Для остальных кастомный класс JSONEncodedDict,
    # под катом которого VARCHAR
    assert isinstance(
        json_type.load_dialect_impl(sqlite.dialect()),
        JSONEncodedDict
    )


def test_serialize(json_type):
    actual = json_type.process_bind_param(
        DataClass('test'), None
    )
    assert actual == {'test': 'test', 'list_1': [], 'list_2': None}

    actual = json_type.process_bind_param(
        DataClass('test', [1, 2, 3], [True, False]), None
    )
    assert actual == {'test': 'test', 'list_1': [1, 2, 3], 'list_2': [True, False]}

    with pytest.raises(ValueError):
        json_type.process_bind_param(1, None)


def test_serialize_many(json_type_many):
    actual = json_type_many.process_bind_param(
        [DataClass('test1'), DataClass('test2', list_2=[])], None
    )
    assert actual == [
        {'test': 'test1', 'list_1': [], 'list_2': None},
        {'test': 'test2', 'list_1': [], 'list_2': []},
    ]

    with pytest.raises(ValueError):
        json_type_many.process_bind_param([1, 2, 3], None)


def test_serialize_many_empty_list(json_type_many):
    actual = json_type_many.process_bind_param([], None)
    assert actual == []


def test_deserialize(json_type):
    assert_deserialize(json_type)


def test_deserialize_many(json_type_many):
    assert_deserialize_many(json_type_many)


def test_deserialize_without_post_load(json_type_without_post_load):
    assert_deserialize(json_type_without_post_load)


def test_deserialize_without_post_load_many(json_type_without_post_load_many):
    assert_deserialize_many(json_type_without_post_load_many)


def test_deserialize_many_empty_list(json_type_many):
    actual = json_type_many.process_result_value([], None)
    assert actual == []


def assert_deserialize(json_type):
    actual = json_type.process_result_value(
        {'test': 'test1', 'list_1': [1, 2], 'list_2': None}, None
    )
    assert actual == DataClass('test1', [1, 2])

    with pytest.raises(ValidationError):
        json_type.process_result_value(
            {'test': 'test1', 'list_1': [1, 2], 'list_2': True}, None
        )


def assert_deserialize_many(json_type_many):
    actual = json_type_many.process_result_value(
        [
            {'test': 'test1', 'list_1': [1, 2], 'list_2': None},
            {'test': 'test2', 'list_1': [], 'list_2': []}
        ], None
    )
    assert actual == [
        DataClass('test1', [1, 2]), DataClass('test2', [], [])
    ]

    with pytest.raises(ValidationError):
        json_type_many.process_result_value(
            [{'test': 'test1', 'list_1': [1, 2], 'list_2': True}], None
        )


@pytest.fixture()
def json_type():
    return Json(dataclass=DataClass, schema=DataClassSchema)


@pytest.fixture()
def json_type_many():
    return Json(dataclass=DataClass, schema=DataClassSchema(many=True))


@pytest.fixture()
def json_type_without_post_load():
    return Json(dataclass=DataClass, schema=DataClassSchemaWithoutPostLoad)


@pytest.fixture()
def json_type_without_post_load_many():
    return Json(dataclass=DataClass, schema=DataClassSchemaWithoutPostLoad(many=True))


@pytest.fixture()
def Table():
    return DatabaseStorage(TableModel)


@dataclass
class DataClass:
    test: str
    list_1: List[int] = field(default_factory=list)
    list_2: Optional[List[bool]] = None


DataClassSchema = class_schema(DataClass)


class DataClassSchemaWithoutPostLoad(Schema):
    test = fields.Str()
    list_1 = fields.List(fields.Int, default=[])
    list_2 = fields.List(fields.Bool, missing=None, default=None)


class TableModel(DBInstanceMixin, Base):
    __tablename__ = 'table_model'
    json_list = Column(
        'json_list',
        Json(dataclass=DataClass, schema=DataClassSchema(many=True)),
        default=[]
    )
