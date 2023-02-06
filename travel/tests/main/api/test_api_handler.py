# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import

from marshmallow import Schema, fields

from travel.avia.backend.main.api.api_handler import ApiHandler
from travel.avia.backend.main.api.api_schema import TypeSchema
from travel.avia.backend.main.api.register import register_type, unregister_type
from travel.avia.backend.main.lib.exceptions import ApiException
from travel.avia.backend.tests.main.api_test import TestApiHandler


class TEmptyParams(Schema):
    pass


class TEmptySchema(TypeSchema):
    pass


class TEmptyHandler(ApiHandler):
    PARAMS_SCHEMA = TEmptyParams
    TYPE_SCHEMA = TEmptySchema

    def process(self, params, fields):
        return {}


class TestHandlerParams(TestApiHandler):
    def setUp(self):
        super(TestHandlerParams, self).setUp()

        class TParams(Schema):
            some_int = fields.Int(required=True)
            other_int = fields.Int(required=True)

        class THandler(ApiHandler):
            PARAMS_SCHEMA = TParams
            TYPE_SCHEMA = TEmptySchema

        register_type('testMain', THandler)

    def tearDown(self):
        unregister_type('testMain')

    def test_validation(self):
        payload = {
            'name': 'testMain',
            'params': {
                'other_int': 'sdf'
            }
        }

        data = self.api_data(payload, status_code=400)

        assert data == self.wrap_error_expect(payload, {
            'status': 'error',
            'reason': 'params are not valid',
            'description': {
                'some_int': ['Missing data for required field.'],
                'other_int': ['Not a valid integer.']
            }
        })

    def test_not_exist(self):
        payload = {
            'name': 'testMain',
            'params': {
                'someInt': 1,
                'otherInt': 2,
                'notExistInt': 3
            }
        }

        data = self.api_data(payload, status_code=400)

        assert data == self.wrap_error_expect(payload, {
            'status': 'error',
            'reason': 'not exist params {\'params\': [u\'not_exist_int\']}'
        })


class TestHandlerFields(TestApiHandler):
    def setUp(self):
        super(TestHandlerFields, self).setUp()

    def test_correct_returned(self):
        class TSchema(TypeSchema):
            my_field = fields.Str()
            my_other = fields.Str()

        class THandler(ApiHandler):
            PARAMS_SCHEMA = TEmptyParams
            TYPE_SCHEMA = TSchema

            def process(self, parama, fields):
                return {
                    'my_field': 'result',
                    'my_other': 'result'
                }

        register_type('testMain', THandler)

        payload = {
            'name': 'testMain',
            'fields': ['myField', {'name': 'myOther'}]
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'myField': 'result',
            'myOther': 'result',
        })

        unregister_type('testMain')

    def test_not_exist(self):
        register_type('testMain', TEmptyHandler)

        payload = {
            'name': 'testMain',
            'fields': ['notExist']
        }

        data = self.api_data(payload, status_code=400)

        assert data == self.wrap_error_expect(payload, {
            'status': 'error',
            'reason': 'not exist fields',
            'description': {
                'fields': ['not_exist']
            }
        })

        unregister_type('testMain')

    def test_wrong_type(self):
        register_type('testMain', TEmptyHandler)

        payload = {
            'name': 'testMain',
            'fields': [['hello']]
        }

        data = self.api_data(payload, status_code=400)

        assert data == self.wrap_error_expect(payload, {
            'status': 'error',
            'reason': 'wrong object in fields',
            'description': {
                'fields': [['hello']]
            }
        })

        unregister_type('testMain')


class TestHandlerExceptions(TestApiHandler):
    def test_process_exception(self):
        class THandler(TEmptyHandler):
            def process(self, params, fields):
                raise ValueError('hello')

        register_type('testMain', THandler)

        data = self.api_data({'name': 'testMain'}, status_code=500)

        assert data['status'] == 'error'
        assert data['reason'] == 'Unknown error: hello'

        unregister_type('testMain')

    def test_process_api_exception(self):
        class THandler(TEmptyHandler):
            def process(self, params, fields):
                raise ApiException('hello')

        register_type('testMain', THandler)

        payload = {'name': 'testMain'}
        data = self.api_data({'name': 'testMain'}, status_code=400)

        assert data == self.wrap_error_expect(payload, {
            'status': 'error',
            'reason': 'hello',
        })

        unregister_type('testMain')

    def test_bad_dump(self):
        class TSchema(TypeSchema):
            my_field = fields.Int()
            my_other = fields.Str()

        class THandler(TEmptyHandler):
            TYPE_SCHEMA = TSchema

            def process(self, params, fields):
                return {
                    'my_field': 'string',
                    'my_other': 1
                }

        register_type('testMain', THandler)

        payload = {'name': 'testMain'}
        data = self.api_data(payload, status_code=400)

        assert data == self.wrap_error_expect(payload, {
            'status': 'error',
            'reason': 'dump error',
            'description': {
                'my_field': ['Not a valid integer.']
            }
        })

        unregister_type('testMain')

    def test_error_body_must_be_array(self):
        payload = {'key': 'value'}
        self.api_data(payload, status_code=400, body_autocorrection=False)

    def test_unknown_handler(self):
        payload = [{'name': 'some-bad-name'}]
        self.api_data(payload, status_code=400)
