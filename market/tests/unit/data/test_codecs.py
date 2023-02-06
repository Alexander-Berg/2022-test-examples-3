import six

from yamarec1.data import ArrayData
from yamarec1.data import DataColumnSchema
from yamarec1.data import DataRecord
from yamarec1.data import DataSchema
from yamarec1.data import IterableData
from yamarec1.data.codecs import DSVCodec
from yamarec1.data.codecs import JSONCodec


def test_default_dsv_codec_decodes_headerful_tsv():
    stream = six.StringIO("1\t2\none\ttwo\nthree\tfour")
    result = DSVCodec().decode(stream)
    assert isinstance(result, IterableData)
    records = list(result)
    assert all(isinstance(record, DataRecord) for record in records)
    assert records == [("one", "two"), ("three", "four")]


def test_default_dsv_codec_encodes_headerful_tsv():
    schema = DataSchema([DataColumnSchema("1", "String"), DataColumnSchema("2", "String")])
    data = ArrayData([DataRecord("one", "two"), DataRecord("three", "four")], schema)
    chunks = list(DSVCodec().encode(data))
    assert all(isinstance(chunk, bytes) for chunk in chunks)
    result = b"".join(chunks)
    assert result == b"1\t2\none\ttwo\nthree\tfour\n"


def test_dsv_codec_can_decode_headerless_tsv():
    stream = six.StringIO("one\ttwo\nthree\tfour")
    result = DSVCodec(headerful=False).decode(stream)
    assert isinstance(result, IterableData)
    records = list(result)
    assert all(isinstance(record, DataRecord) for record in records)
    assert records == [("one", "two"), ("three", "four")]


def test_dsv_codec_can_encode_headerless_tsv():
    data = ArrayData([DataRecord("one", "two"), DataRecord("three", "four")])
    chunks = list(DSVCodec(headerful=False).encode(data))
    assert all(isinstance(chunk, bytes) for chunk in chunks)
    result = b"".join(chunks)
    assert result == b"one\ttwo\nthree\tfour\n"


def test_dsv_codec_can_decode_csv():
    stream = six.StringIO("one,two\nthree,four")
    result = DSVCodec(delimiter=",", headerful=False).decode(stream)
    assert isinstance(result, IterableData)
    records = list(result)
    assert all(isinstance(record, DataRecord) for record in records)
    assert records == [("one", "two"), ("three", "four")]


def test_dsv_codec_can_encode_csv():
    data = ArrayData([DataRecord("one", "two"), DataRecord("three", "four")])
    chunks = list(DSVCodec(delimiter=",", headerful=False).encode(data))
    assert all(isinstance(chunk, bytes) for chunk in chunks)
    result = b"".join(chunks)
    assert result == b"one,two\nthree,four\n"


def test_json_codec_decodes_data_from_list_of_lists():
    stream = six.StringIO("[[\"one\", 1], [\"two\", 2]]")
    result = JSONCodec().decode(stream)
    assert isinstance(result, IterableData)
    records = list(result)
    assert all(isinstance(record, DataRecord) for record in records)
    assert records == [("one", 1), ("two", 2)]


def test_json_codec_encodes_records_as_list_of_lists():
    data = ArrayData([DataRecord("one", 1), DataRecord("two", 2)])
    chunks = list(JSONCodec().encode(data))
    assert all(isinstance(chunk, bytes) for chunk in chunks)
    result = b"".join(chunks)
    assert result == b"[[\"one\", 1], [\"two\", 2]]"
