# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from google.protobuf import wrappers_pb2

from search.martylib.proto.structures import test_pb2

from search.martylib.test_utils import TestCase
from search.martylib.protobuf_utils import clear_disallowed_in_api, clear_empty_fields, message_hash


class TestCheckUUID(TestCase):
    def test_check_id(self):
        message = test_pb2.TestProtobufUtils(
            id='id',
            disallowed_id='disallowed_id',
            type=test_pb2.TestProtobufUtils.Type.BASE,
            disallowed_type=test_pb2.TestProtobufUtils.Type.BASE,
            container=test_pb2.TestProtobufUtils.Container(
                key='key_0',
                value='value_0',
            ),
            disallowed_container=test_pb2.TestProtobufUtils.Container(
                key='key_0',
                value='value_0',
            ),
            repeated_container=[
                test_pb2.TestProtobufUtils.Container(
                    key='key_1',
                    value='value_1',
                ),
            ],
            disallowed_repeated_container=[
                test_pb2.TestProtobufUtils.Container(
                    key='key_1',
                    value='value_1',
                ),
            ],
            map={
                'key_2': test_pb2.TestProtobufUtils.Container(
                    key='key_2',
                    value='value_2',
                ),
            },
            disallowed_map={
                'key_2': test_pb2.TestProtobufUtils.Container(
                    key='key_2',
                    value='value_2',
                ),
            },
        )
        res = test_pb2.TestProtobufUtils(
            id='id',
            type=test_pb2.TestProtobufUtils.Type.BASE,
            container=test_pb2.TestProtobufUtils.Container(
                key='key_0',
            ),
            repeated_container=[
                test_pb2.TestProtobufUtils.Container(
                    key='key_1',
                ),
            ],
            map={
                'key_2': test_pb2.TestProtobufUtils.Container(
                    key='key_2',
                ),
            },
        )
        clear_disallowed_in_api(message)
        self.assertEqual(message, res)

        res.container.value = 'value'
        self.assertNotEqual(message, res)

    def test_clear_empty_fields(self):
        message = test_pb2.TestProtobufUtils(
            id='',
            repeated_container=[
                test_pb2.TestProtobufUtils.Container(
                ),
            ],
            map={
                'key_1': test_pb2.TestProtobufUtils.Container(),
                'key_2': test_pb2.TestProtobufUtils.Container(
                    key='key_2',
                    value='value_2',
                ),
            },
            container=test_pb2.TestProtobufUtils.Container(),
        )
        res = test_pb2.TestProtobufUtils(
            id='',
            repeated_container=[
                test_pb2.TestProtobufUtils.Container(
                ),
            ],
            map={
                'key_1': test_pb2.TestProtobufUtils.Container(),
                'key_2': test_pb2.TestProtobufUtils.Container(
                    key='key_2',
                    value='value_2',
                ),
            },
        )
        clear_empty_fields(message)
        self.assertEqual(res, message)

    def test_message_hash(self):
        m = test_pb2.TestMessageHash(
            field_1=wrappers_pb2.BoolValue(value=True),
            field_2=wrappers_pb2.BytesValue(value=b'a'),
            field_3=wrappers_pb2.DoubleValue(value=15.1),
            field_4=wrappers_pb2.FloatValue(value=15.1),
            field_5=wrappers_pb2.Int32Value(value=10),
            field_6=wrappers_pb2.Int64Value(value=10),
            field_7=wrappers_pb2.StringValue(value='a'),
            field_8=wrappers_pb2.UInt32Value(value=10),
            field_9=wrappers_pb2.UInt64Value(value=10),
        )

        assert message_hash(m)
