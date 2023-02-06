import json

import flask
from library.python import resource

from crypta.lib.python.test_utils import api_mock


class MockCryptaApi(api_mock.MockCryptaApiBase):
    def __init__(self, example):
        super(MockCryptaApi, self).__init__(resource.find('/swagger.json'))

        self.data = {
            section: json.loads(resource.find('/{}/{}.json'.format(example, section)))
            for section in ('segments', 'groups', 'rules')
        }

        @self.app.route('/lab/segment')
        def get_all_segments():
            return flask.jsonify(self.data['segments'])

        @self.app.route('/lab/segment/groups')
        def get_all_groups():
            return flask.jsonify(self.data['groups'])

        @self.app.route('/lab/constructor/rule')
        def get_all_rules():
            return flask.jsonify(self.data['rules'])
