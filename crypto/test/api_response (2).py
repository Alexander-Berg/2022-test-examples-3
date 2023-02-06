import json

import flask
from library.python import resource

from crypta.lib.python.test_utils import api_mock


class MockCryptaApi(api_mock.MockCryptaApiBase):
    def __init__(self, segments_path):
        super(MockCryptaApi, self).__init__(resource.find('/swagger.json'))

        self.segments = json.loads(resource.find(segments_path))

        @self.app.route('/lab/segment')
        def get_all_segments():
            return flask.jsonify(self.segments)
