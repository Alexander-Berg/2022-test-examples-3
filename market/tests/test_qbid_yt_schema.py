#!/usr/bin/env python
# -*- coding: utf-8 -*-

import unittest

from market.idx.pylibrary.mindexer_core.qbid.yt_schema import make_schema, make_bids_schema
import market.idx.pylibrary.mindexer_core.qbid.tests.proto.qbid_test_schema_pb2 as schemas


class WarnChecker(object):
    def __init__(self):
        self.had_warnings = False

    def info(self, *args, **kwargs):
        pass

    def warn(self, *args, **kwargs):
        self.had_warnings = True


class TestQBidYtSchema(unittest.TestCase):
    COMMON_COLUMNS = frozenset([
        (('name', 'mi_generation'), ('type', 'string')),
        (('name', 'mi_delta'), ('type', 'string')),
        (('name', 'mi_pub_date'), ('type', 'int64')),
        (('name', 'mi_mbi_id'), ('type', 'int64')),
        (('name', 'mi_value_for'), ('type', 'string')),
    ])

    def test_good_empty(self):
        schema, had_warnings, had_elog_warnings = self.make_proto_schema(
            schemas.GoodEmpty
        )
        self.assertFalse(had_warnings)
        self.assertFalse(had_elog_warnings)
        self.check_common(schema)
        self.assertEqual(self.COMMON_COLUMNS, schema)

    def test_good_simple(self):
        schema, had_warnings, had_elog_warnings = self.make_proto_schema(
            schemas.GoodSimple
        )
        self.assertFalse(had_warnings)
        self.assertFalse(had_elog_warnings)
        self.check_common(schema)
        self.assertEqual(
            self.COMMON_COLUMNS | frozenset([
                (('name', 'x'), ('type', 'int64')),
                (('name', 'ys'), ('type', 'any')),
            ]),
            schema
        )

    def test_good_with_value(self):
        schema, had_warnings, had_elog_warnings = self.make_proto_schema(
            schemas.GoodWithValue
        )
        self.assertFalse(had_warnings)
        self.assertFalse(had_elog_warnings)
        self.check_common(schema)
        self.assertEqual(
            self.COMMON_COLUMNS | frozenset([
                (('name', 'value_a'), ('type', 'string')),
                (('name', 'value_b'), ('type', 'string')),
                (('name', 'u'), ('type', 'string')),
                (('name', 'vs'), ('type', 'any')),
                (('name', 'w'), ('type', 'int64')),
            ]),
            schema
        )

    def test_bad_float(self):
        schema, had_warnings, had_elog_warnings = self.make_proto_schema(
            schemas.BadFloat
        )
        self.assertTrue(had_warnings)
        self.assertTrue(had_elog_warnings)
        self.check_common(schema)
        self.assertEqual(self.COMMON_COLUMNS, schema)

    def test_bad_repeated_int(self):
        schema, had_warnings, had_elog_warnings = self.make_proto_schema(
            schemas.BadRepeatedInt
        )
        self.assertTrue(had_warnings)
        self.assertTrue(had_elog_warnings)
        self.check_common(schema)
        self.assertEqual(self.COMMON_COLUMNS, schema)

    def test_bad_repeated_value(self):
        schema, had_warnings, had_elog_warnings = self.make_proto_schema(
            schemas.BadRepeatedValue
        )
        self.assertTrue(had_warnings)
        self.assertTrue(had_elog_warnings)
        self.check_common(schema)
        self.assertEqual(self.COMMON_COLUMNS, schema)

    def test_bids_schema(self):
        """Bids schema should be generated without warnings.
        """
        log = WarnChecker()
        event_log = WarnChecker()
        schema = make_bids_schema(
            log=log,
            event_log=event_log,
        )
        self.assertEqual(schema.attributes, {'strict': True})

        schema_set = frozenset(
            tuple(sorted(column.items()))
            for column in schema
        )
        self.check_common(schema_set)

    def make_proto_schema(self, proto):
        log = WarnChecker()
        event_log = WarnChecker()
        schema = make_schema(
            proto.DESCRIPTOR,
            proto.Value.DESCRIPTOR,
            log=log,
            event_log=event_log,
        )
        self.assertEqual(schema.attributes, {'strict': True})

        schema_set = frozenset(
            tuple(sorted(column.items()))
            for column in schema
        )

        return schema_set, log.had_warnings, event_log.had_warnings

    def check_common(self, schema):
        for column in self.COMMON_COLUMNS:
            self.assertTrue(column in schema)


if __name__ == '__main__':
    unittest.main()
