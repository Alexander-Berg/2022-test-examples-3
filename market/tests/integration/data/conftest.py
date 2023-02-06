import os.path
import uuid

import pytest
import six

from yamarec1.beans import settings
from yamarec1.beans import yqlc
from yamarec1.beans import ytc
from yamarec1.data import DataColumnSchema
from yamarec1.data import DataQuery
from yamarec1.data import DataRecord
from yamarec1.data import DataSchema
from yamarec1.data import IterableData
from yamarec1.data import QueryableData
from yamarec1.data import StreamableData
from yamarec1.data.codecs import DSVCodec


class DummyIterableData(IterableData):

    def __iter__(self):
        yield DataRecord("god", "save", "the", "queen")
        yield DataRecord("all", "dressed", "in", "green")

    @property
    def schema(self):
        columns = [
            DataColumnSchema("first", "String"),
            DataColumnSchema("second", "String"),
            DataColumnSchema("third", "String"),
            DataColumnSchema("fourth", "String"),
        ]
        return DataSchema(columns)


class DummyStreamableData(StreamableData):

    @property
    def codec(self):
        return DSVCodec()

    @property
    def schema(self):
        columns = [
            DataColumnSchema("first", "String"),
            DataColumnSchema("second", "Bool"),
            DataColumnSchema("third", "Int64"),
        ]
        return DataSchema(columns)

    def stream(self):
        return six.BytesIO(b"first\tsecond\tthird\nmy\ttrue\t1\nyour\tfalse\t0\n")


class DummySchemelessQueryableData(QueryableData):

    @property
    def query(self):
        text = (
            "SELECT 'my' AS first, true AS second, 1L AS third\n"
            "UNION ALL\n"
            "SELECT 'your' AS first, false AS second, 2L AS third"
        )
        return DataQuery(text)


class DummyQueryableData(DummySchemelessQueryableData):

    @property
    def schema(self):
        columns = [
            DataColumnSchema("first", "String"),
            DataColumnSchema("second", "Bool"),
            DataColumnSchema("third", "Int64"),
        ]
        return DataSchema(columns)


@pytest.fixture
def iterable_data():
    return DummyIterableData()


@pytest.fixture
def streamable_data():
    return DummyStreamableData()


@pytest.fixture
def schemeless_queryable_data():
    return DummySchemelessQueryableData()


@pytest.fixture
def queryable_data():
    return DummyQueryableData()


@pytest.fixture
def data(iterable_data):
    return iterable_data


@pytest.yield_fixture
def random_yt_path():
    path = os.path.join(settings.yt.root, uuid.uuid4().hex)
    try:
        yield path
    finally:
        if ytc.exists(path):
            ytc.remove(path, recursive=True)


@pytest.fixture
def table_with_data(random_yt_path, data):
    path = random_yt_path
    ytc.create("table", path, attributes={"schema": data.schema.as_yt_schema()})
    yqlc.write_table(
        path,
        data,
        [column.name for column in data.schema.columns],
        [column.type for column in data.schema.columns],
        provider="yt_native")
    return path
