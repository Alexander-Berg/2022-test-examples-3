import json

from crypta.lib.python.test_utils import flask_mock_server


class MockCryptaApiBase(flask_mock_server.FlaskMockServer):
    def __init__(self, swagger_json):
        super(MockCryptaApiBase, self).__init__('Crypta API')
        self.swagger = json.loads(swagger_json)

        @self.app.route('/swagger.json')
        @self.app.route('/')
        def swagger():
            return self.swagger
