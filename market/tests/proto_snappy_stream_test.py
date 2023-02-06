# -*- coding: utf-8 -*-
import six
from six import BytesIO
from google.protobuf.text_format import MessageToString

from market.pylibrary.snappy_protostream import (
    PbsnDataFile,
    PbsnDataTyped,
    pbsn_reader,
    SnappyProtoWriter,
    SnappyStream,
)
from market.pylibrary.snappy_protostream.tests.proto_snappy_test_pb2 import TestMessage
from market.proto.indexer.FeedLog_pb2 import Feed

import yatest.common


FEEDLOG_FILE_PATH = "market/pylibrary/snappy_protostream/tests/feedlog.pbuf.sn"


def test_string_stream_proto():
    inputData = "MBOC\x11\x00\x00\x00\x0a\x07Samsung\x12\x06Galaxy\x0f\x00\x00\x00\x0a\x05Apple\x12\x06iPhone"
    inputStream = BytesIO(six.ensure_binary(inputData))

    return do_proto_test("test_string_stream_proto.out", PbsnDataTyped(inputStream, "MBOC", TestMessage))


def test_file_stream_proto():
    filename = yatest.common.source_path(FEEDLOG_FILE_PATH)
    with PbsnDataFile.open(filename, "FLOG") as input:
        return do_proto_test("test_file_stream_proto.out", input.reader(Feed))


def test_file_stream_proto2():
    filename = yatest.common.source_path(FEEDLOG_FILE_PATH)
    input = PbsnDataFile.open(filename, "FLOG")
    ret = do_proto_test("test_file_stream_proto2.out", input.reader(Feed))
    input.close()
    return ret


def test_file_stream_snappy():
    filename = yatest.common.source_path(FEEDLOG_FILE_PATH)
    with SnappyStream.open(filename) as input:
        return do_bstream_test("test_file_stream_snappy.out", input)


def test_file_stream_snappy2():
    filename = yatest.common.source_path(FEEDLOG_FILE_PATH)
    input = SnappyStream.open(filename)
    ret = do_bstream_test("test_file_stream_snappy2.out", input)
    input.close()
    return ret


def do_proto_test(out_path, generator):
    with open(out_path, "w") as out:
        for data in generator:
            res = MessageToString(data, as_utf8=True)
            six.print_(res, file=out)

    return yatest.common.canonical_file(out_path)


def do_bstream_test(out_path, stream):
    with open(out_path, "wb") as out:
        out.write(stream.read())

    return yatest.common.canonical_file(out_path)


def test_snappy_proto_writer():
    stream = BytesIO()
    with SnappyProtoWriter(stream, 'TEST') as writer:
        for i in range(2):
            msg = TestMessage()
            msg.vendor = 'v{}'.format(i)
            msg.model = 'm{}'.format(i)
            writer.write(msg)

    stream.seek(0)
    msg_list = list(pbsn_reader(stream, 'TEST', TestMessage))
    assert len(msg_list) == 2
    for i, msg in enumerate(msg_list):
        assert msg.vendor == 'v{}'.format(i)
        assert msg.model == 'm{}'.format(i)
