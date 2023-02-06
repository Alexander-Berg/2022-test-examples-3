import json
from typing import Type

import pytest
from aiohttp import web
from apispec import APISpec
from marshmallow import Schema, fields

from sendr_aiohttp import (
    ApplicationSpecUpdater, create_apispec, request_schema, response_schema, setup_swagger_route, spec_to_dict
)


def pass_through_json(value):
    return json.loads(json.dumps(value))


def assert_equal_json(a, b):
    json_a = json.dumps(a, sort_keys=True, indent=0, ensure_ascii=False)
    json_b = json.dumps(b, sort_keys=True, indent=0, ensure_ascii=False)

    assert json_a == json_b


@pytest.fixture
def swagger_route():
    return '/api/docs'


@pytest.fixture
def setup_app():
    def setup(app: web.Application):
        pass

    return setup


@pytest.fixture
def app(setup_app):
    class PathSchema(Schema):
        user_id = fields.Integer(required=True)
        user_age = fields.Integer(required=False)

    class QuerySchema(Schema):
        is_deleted = fields.Boolean()

    class UserSchema(Schema):
        user_id = fields.Integer()
        login = fields.String()

    class Handler(web.View):
        @request_schema(PathSchema(), location='match_info')
        @request_schema(QuerySchema(), location='query')
        @response_schema(UserSchema())
        async def get(self):
            return web.json_response({'user_id': 1, 'login': 'riskingh'})

    app = web.Application()

    app.router.add_route(
        method='*',
        path='/user/{user_id}',
        handler=Handler,
        name='you_cant_handle_it',
    )

    setup_app(app)

    return app


@pytest.fixture
async def client(test_client, app):
    return await test_client(app)


def test_create_apispec(app):
    spec = create_apispec(
        app=app,
        title='App-title',
        version='App-version',
    )
    expected_spec = {
        'definitions': {
            'UserSchema': {
                'properties': {
                    'login': {
                        'type': 'string'
                    },
                    'user_id': {
                        'format': 'int32',
                        'type': 'integer',
                    }
                },
                'type': 'object'
            }
        },
        'info': {'title': 'App-title', 'version': 'App-version'},
        'parameters': {},
        'paths': {
            '/user/{user_id}': {
                'get': {
                    'parameters': [
                        {
                            'in': 'query',
                            'name': 'is_deleted',
                            'required': False,
                            'type': 'boolean'
                        },
                        {
                            'format': 'int32',
                            'in': 'path',
                            'name': 'user_id',
                            'required': True,
                            'type': 'integer'
                        },
                        {
                            'format': 'int32',
                            'in': 'path',
                            'name': 'user_age',
                            'required': False,
                            'type': 'integer'
                        }
                    ],
                    'produces': ['application/json'],
                    'responses': {
                        '200': {
                            'schema': {'$ref': '#/definitions/UserSchema'}
                        }
                    }
                }
            }
        },
        'security': [{'TVMTicket': []}],
        'securityDefinitions': {
            'TVMTicket': {
                'in': 'header',
                'name': 'X-Ya-Service-Ticket',
                'type': 'apiKey'
            }
        },
        'swagger': '2.0',
        'tags': []
    }
    assert_equal_json(spec_to_dict(spec), expected_spec)


def test_create_apispec_with_custom_updater(app):
    class Updater(ApplicationSpecUpdater):
        def get_operation_spec(self, view_cls: Type[web.View], method: str) -> dict:
            operation_spec = super().get_operation_spec(view_cls, method)
            operation_spec['tags'] = ['a']
            self.spec.options.setdefault('securityDefinitions', {})['Token'] = {
                'in': 'header',
                'name': 'X-Token',
                'type': 'apiKey',
            }
            operation_spec['security'] = [{'Token': []}]
            return operation_spec

    spec = create_apispec(app, title='Title', version='1', security_definitions={}, get_spec_updater=Updater)

    spec_json = pass_through_json(spec_to_dict(spec))
    assert spec_json == {
        'info': {'title': 'Title', 'version': '1'},
        'paths': {
            '/user/{user_id}': {
                'get': {
                    'parameters': [
                        {'in': 'query', 'name': 'is_deleted', 'required': False, 'type': 'boolean'},
                        {'in': 'path', 'name': 'user_id', 'required': True, 'type': 'integer', 'format': 'int32'},
                        {'in': 'path', 'name': 'user_age', 'required': False, 'type': 'integer', 'format': 'int32'}
                    ],
                    'responses': {'200': {'schema': {'$ref': '#/definitions/UserSchema'}}},
                    'produces': ['application/json'],
                    'tags': ['a'],
                    'security': [{'Token': []}],
                }
            }
        },
        'tags': [],
        'swagger': '2.0',
        'definitions': {
            'UserSchema': {
                'type': 'object',
                'properties': {'login': {'type': 'string'}, 'user_id': {'type': 'integer', 'format': 'int32'}}
            }
        },
        'parameters': {},
        'securityDefinitions': {
            'Token': {
                'in': 'header',
                'name': 'X-Token',
                'type': 'apiKey',
            }},
        'security': [{}],
    }


class TestSetSwaggerRoute:
    @pytest.fixture
    def setup_app(self):
        class DummyAPISpec(APISpec):
            def __init__(self):
                super(DummyAPISpec, self).__init__(title='', version='')

            def to_dict(self):
                return {'a': 1, 'b': 2}

        def setup(app: web.Application):
            setup_swagger_route(
                app=app,
                spec=DummyAPISpec(),
                swagger_route='/one/two',
            )

        return setup

    async def test_swagger_route_is_set(self, client):
        response = await client.get('/one/two')
        spec = await response.json()
        expected = {'a': 1, 'b': 2}
        assert_equal_json(spec, expected)
