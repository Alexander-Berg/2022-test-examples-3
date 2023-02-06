# encoding: UTF-8

import dns.rdata
import dns.rdatatype
import marshmallow as ma
from hamcrest import *

from dns_hosting.dto.domains import DomainSchema, ASchema, AAAASchema, \
    CNAMESchema, MXSchema, TXTSchema, NSSchema, SRVSchema, CAASchema, \
    RecordSchema
from dns_hosting.models.domains import Domain, Record, RecordType
from tests.dns_hosting.test_app import BaseAppTestCase


class BaseSchemaTestCase(BaseAppTestCase):
    schema_cls = None

    @staticmethod
    def get_message(code, field=ma.fields.Field):
        return field.default_error_messages[code]

    @classmethod
    def create_schema(cls, *args, **kwargs):
        return cls.schema_cls(*args, **kwargs)


class DomainSchemaTestCase(BaseSchemaTestCase):
    schema_cls = DomainSchema

    def test_load(self):
        data = {'id': 100500, 'name': 'example.com.'}
        result, errors = self.create_schema().load(data)
        assert_that(errors, empty())
        assert_that(
            result,
            all_of(
                instance_of(Domain),
                has_properties(**data),
            ),
        )

    def test_load_empty_fields(self):
        data = {}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                id=has_item(self.get_message('required')),
                name=has_item(self.get_message('required')),
            ),
        )

    def test_load_name_without_tail_dot(self):
        data = {'id': 100500, 'name': 'example.com'}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                name=has_item(self.schema_cls.MSG_INVALID_DOMAIN_NAME),
            ),
        )


class ASchemaTestCase(BaseSchemaTestCase):
    schema_cls = ASchema

    def test_load(self):
        data = {'address': '192.168.1.1'}
        result, errors = self.create_schema().load(data)
        assert_that(errors, empty())
        assert_that(
            result,
            all_of(
                instance_of(dns.rdata.Rdata),
                has_properties(
                    rdtype=dns.rdatatype.A,
                    address=data['address'],
                ),
            ),
        )

    def test_load_empty_fields(self):
        data = {}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                address=has_item(self.get_message('required')),
            ),
        )

    def test_load_invalid_address(self):
        data = {'address': '256.168.1.1'}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                address=has_item(self.schema_cls.MSG_INVALID_ADDRESS),
            ),
        )


class AAAASchemaTestCase(BaseSchemaTestCase):
    schema_cls = AAAASchema

    def test_load(self):
        data = {'address': '::1'}
        result, errors = self.create_schema().load(data)
        assert_that(errors, empty())
        assert_that(
            result,
            all_of(
                instance_of(dns.rdata.Rdata),
                has_properties(
                    rdtype=dns.rdatatype.AAAA,
                    address=data['address'],
                ),
            ),
        )

    def test_load_empty_fields(self):
        data = {}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                address=has_item(self.get_message('required')),
            ),
        )

    def test_load_invalid_address(self):
        data = {'address': ':::3'}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                address=has_item(self.schema_cls.MSG_INVALID_ADDRESS),
            ),
        )


class CNAMESchemaTestCase(BaseSchemaTestCase):
    schema_cls = CNAMESchema

    def test_load(self):
        data = {'target': 'example.com.'}
        result, errors = self.create_schema().load(data)
        assert_that(errors, empty())
        assert_that(
            result,
            all_of(
                instance_of(dns.rdata.Rdata),
                has_properties(
                    rdtype=dns.rdatatype.CNAME,
                    target=dns.name.from_text(data['target']),
                ),
            ),
        )

    def test_load_empty_fields(self):
        data = {}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                target=has_item(self.get_message('required')),
            ),
        )

    def test_load_invalid_target(self):
        data = {'target': 'a-=sd-0as-0das96d//\\'}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                target=has_item(self.schema_cls.MSG_INVALID_TARGET),
            ),
        )


class MXSchemaTestCase(BaseSchemaTestCase):
    schema_cls = MXSchema

    def test_load(self):
        data = {'preference': 10, 'exchange': 'mx.example.com.'}
        result, errors = self.create_schema().load(data)
        assert_that(errors, empty())
        assert_that(
            result,
            all_of(
                instance_of(dns.rdata.Rdata),
                has_properties(
                    rdtype=dns.rdatatype.MX,
                    preference=data['preference'],
                    exchange=dns.name.from_text(data['exchange']),
                ),
            ),
        )

    def test_load_empty_fields(self):
        data = {}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                preference=has_item(self.get_message('required')),
                exchange=has_item(self.get_message('required')),
            ),
        )

    def test_load_invalid_preference(self):
        data = {'preference': 65536, 'exchange': 'mx.example.com.'}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                preference=has_item(self.schema_cls.MSG_INVALID_PREFERENCE),
            ),
        )

    def test_load_invalid_exchange(self):
        data = {'preference': 10, 'exchange': 'a-=sd-0as-0das96d//\\'}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                exchange=has_item(self.schema_cls.MSG_INVALID_EXCHANGE),
            ),
        )


class TXTSchemaTestCase(BaseSchemaTestCase):
    schema_cls = TXTSchema

    def test_load(self):
        data = {'strings': ['something', 'special']}
        result, errors = self.create_schema().load(data)
        assert_that(errors, empty())
        assert_that(
            result,
            all_of(
                instance_of(dns.rdata.Rdata),
                has_properties(**data),
            ),
        )

    def test_load_empty_fields(self):
        data = {}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                strings=has_item(self.get_message('required')),
            ),
        )

    def test_load_invalid_strings(self):
        data = {'strings': 'blah blah blah'}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                strings=has_item(self.get_message('invalid', ma.fields.List)),
            ),
        )

    def test_load_empty_strings(self):
        data = {'strings': []}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                strings=has_item(self.schema_cls.MSG_EMPTY_STRINGS),
            ),
        )


class SRVSchemaTestCase(BaseSchemaTestCase):
    schema_cls = SRVSchema

    def test_load(self):
        data = {
            'priority': 10,
            'weight': 10,
            'port': 80,
            'target': 'svc.example.com.'
        }
        result, errors = self.create_schema().load(data)
        assert_that(errors, empty())
        assert_that(
            result,
            all_of(
                instance_of(dns.rdata.Rdata),
                has_properties(
                    priority=data['priority'],
                    weight=data['weight'],
                    port=data['port'],
                    target=dns.name.from_text(data['target']),
                ),
            ),
        )

    def test_load_empty_fields(self):
        data = {}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                priority=has_item(self.get_message('required')),
                weight=has_item(self.get_message('required')),
                port=has_item(self.get_message('required')),
                target=has_item(self.get_message('required')),
            ),
        )

    def test_load_invalid_priority(self):
        data = {
            'priority': 65536,
            'weight': 10,
            'port': 80,
            'target': 'svc.example.com.'
        }
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                priority=has_item(self.schema_cls.MSG_INVALID_PRIORITY),
            ),
        )

    def test_load_invalid_weight(self):
        data = {
            'priority': 10,
            'weight': 65536,
            'port': 80,
            'target': 'svc.example.com.'
        }
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                weight=has_item(self.schema_cls.MSG_INVALID_WEIGHT),
            ),
        )

    def test_load_invalid_port(self):
        data = {
            'priority': 10,
            'weight': 10,
            'port': 65536,
            'target': 'svc.example.com.'
        }
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                port=has_item(self.schema_cls.MSG_INVALID_PORT),
            ),
        )

    def test_load_invalid_target(self):
        data = {
            'priority': 10,
            'weight': 10,
            'port': 80,
            'target': 'a-=sd-0as-0das96d//\\',
        }
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                target=has_item(self.schema_cls.MSG_INVALID_TARGET),
            ),
        )


class NSSchemaTestCase(BaseSchemaTestCase):
    schema_cls = NSSchema

    def test_load(self):
        data = {'target': 'example.com.'}
        result, errors = self.create_schema().load(data)
        assert_that(errors, empty())
        assert_that(
            result,
            all_of(
                instance_of(dns.rdata.Rdata),
                has_properties(
                    rdtype=dns.rdatatype.NS,
                    target=dns.name.from_text(data['target']),
                ),
            ),
        )

    def test_load_empty_fields(self):
        data = {}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                target=has_item(self.get_message('required')),
            ),
        )

    def test_load_invalid_target(self):
        data = {'target': 'a-=sd-0as-0das96d//\\'}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                target=has_item(self.schema_cls.MSG_INVALID_TARGET),
            ),
        )


class CAASchemaTestCase(BaseSchemaTestCase):
    schema_cls = CAASchema

    def test_load(self):
        data = {'flags': 0, 'tag': 't1', 'value': 'v1'}
        result, errors = self.create_schema().load(data)
        assert_that(errors, empty())
        assert_that(
            result,
            all_of(
                instance_of(dns.rdata.Rdata),
                has_properties(
                    rdtype=dns.rdatatype.CAA,
                    **data
                ),
            ),
        )

    def test_load_empty_fields(self):
        data = {}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                flags=has_item(self.get_message('required')),
                tag=has_item(self.get_message('required')),
                value=has_item(self.get_message('required')),
            ),
        )

    def test_load_invalid_flags(self):
        data = {'flags': 256, 'tag': 't1', 'value': 'v1'}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                flags=has_item(self.schema_cls.MSG_INVALID_FLAGS),
            ),
        )

    def test_load_invalid_tag(self):
        data = {'flags': 0, 'tag': 't-1', 'value': 'v1'}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                tag=has_item(self.schema_cls.MSG_INVALID_TAG),
            ),
        )


class RecordSchemaTestCase(BaseSchemaTestCase):
    schema_cls = RecordSchema

    def test_load(self):
        data = {
            'id': 1,
            'name': 'www',
            'type': 'CAA',
            'rdata': {'flags': 0, 'tag': 't1', 'value': 'v1'},
            'ttl': 900,
        }
        result, errors = self.create_schema().load(data)
        assert_that(errors, empty())
        assert_that(
            result,
            all_of(
                instance_of(Record),
                has_properties(
                    id=data['id'],
                    name=data['name'],
                    type=RecordType.CAA,
                    rdata=has_properties(
                        rdtype=dns.rdatatype.CAA,
                        **data['rdata']
                    ),
                    ttl=data['ttl'],
                ),
            ),
        )

    def test_load_empty_fields(self):
        data = {}
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                id=has_item(self.get_message('required')),
                name=has_item(self.get_message('required')),
                type=has_item(self.get_message('required')),
                rdata=has_item(self.get_message('required')),
                ttl=has_item(self.get_message('required')),
            ),
        )

    def test_load_invalid_name(self):
        data = {
            'id': 1,
            'name': 'a-=sd-0as-0das96d//\\',
            'type': 'CAA',
            'rdata': {'flags': 0, 'tag': 't1', 'value': 'v1'},
            'ttl': 900,
        }
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                name=has_item(self.schema_cls.MSG_INVALID_NAME),
            )
        )

    def test_load_invalid_name_with_tail_dot(self):
        data = {
            'id': 1,
            'name': 'www.',
            'type': 'CAA',
            'rdata': {'flags': 0, 'tag': 't1', 'value': 'v1'},
            'ttl': 900,
        }
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                name=has_item(self.schema_cls.MSG_INVALID_NAME),
            )
        )

    def test_load_invalid_ttl(self):
        data = {
            'id': 1,
            'name': 'www',
            'type': 'CAA',
            'rdata': {'flags': 0, 'tag': 't1', 'value': 'v1'},
            'ttl': -1,
        }
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                ttl=has_item(self.schema_cls.MSG_INVALID_TTL),
            )
        )

    def test_load_invalid_rdata(self):
        data = {
            'id': 1,
            'name': 'www',
            'type': 'CAA',
            'rdata': {'flags': 0, 'tag': 't-1', 'value': 'v1'},
            'ttl': 900,
        }
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                rdata=has_entries(
                    tag=has_item(CAASchema.MSG_INVALID_TAG),
                ),
            ),
        )

    def test_load_rdata_with_invalid_type(self):
        data = {
            'id': 1,
            'name': 'www',
            'rdata': {'flags': 0, 'tag': 't-1', 'value': 'v1'},
            'ttl': 900,
        }
        result, errors = self.create_schema().load(data)
        assert_that(
            errors,
            has_entries(
                type=has_item(self.get_message('required')),
            ),
        )

    def test_load_forbidden(self):
        for type in (RecordType.NS, RecordType.CNAME):
            data = {
                'id': 1,
                'name': '@',
                'type': type.value,
                'rdata': {'target': 'example.com.'},
                'ttl': 900,
            }
            result, errors = self.create_schema().load(data)
            assert_that(
                errors,
                has_entries(
                    _schema=has_item(self.schema_cls.MSG_FORBIDDEN),
                ),
            )
