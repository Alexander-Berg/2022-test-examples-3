# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.proto.structures import test_pb2

from search.martylib.queue_processing.middleware.protobuf import ProtobufEncodingMiddleware, TypedProtobufEncodingMiddleware
from search.martylib.queue_processing.wrapper import QueueWrapperMock
from search.martylib.test_utils import TestCase


class TestProtobufMiddleware(TestCase):
    @classmethod
    def setUpClass(cls):
        cls.queue = QueueWrapperMock(middleware=(
            ProtobufEncodingMiddleware(),
        ))

    def test(self):
        expected = test_pb2.TestProtobufUtils(type=test_pb2.TestProtobufUtils.Type.CUSTOM)
        self.queue.send_message(expected)

        actual = list(self.queue.poll())[0]['Body']
        self.assertEqual(test_pb2.TestProtobufUtils.FromString(actual), expected)


class TestTypedProtobufMiddleware(TestCase):
    @classmethod
    def setUpClass(cls):
        cls.queue = QueueWrapperMock(middleware=(
            TypedProtobufEncodingMiddleware.from_message_type(test_pb2.TestProtobufUtils),
        ))

    def test(self):
        expected = test_pb2.TestProtobufUtils(type=test_pb2.TestProtobufUtils.Type.CUSTOM)
        self.queue.send_message(expected)

        actual = list(self.queue.poll())[0]['Body']
        self.assertEqual(actual, expected)
