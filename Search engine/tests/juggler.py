# coding: utf-8
# do not run local

import json
from components_app.api.juggler.api import JugglerApi
from components_app.api.juggler.api import JugglerEvent
from components_app.api.juggler.constants import Levels
from components_app.configs.base import juggler as juggler_config
from components_app.tests.base import BaseApiTestCase


def get_attr_values(obj):
    return set((v for k, v in obj.__dict__.items() if not k.startswith('__') and not k.endswith('__')))


class TestJugglerApi(BaseApiTestCase):
    def __init__(self, methodName='runTest'):
        super(TestJugglerApi, self).__init__(methodName=methodName)
        self.api = JugglerApi()
        self.api.load_config(juggler_config)
        self.event = JugglerEvent(host='test', service='test_services', status=Levels.OK, description='some text',
                                  tags=['test_components_app'])

    def test_event(self):
        json.dumps(self.event)

    def test_push(self):
        result = self.api.push(self.event)
        self.assertEqual(result, {"message": "OK", "code": 200})

    def test_push_list(self):
        result = self.api.push_list([self.event, self.event])
        self.assertEqual(result, [{"message": "OK", "code": 200}, {"message": "OK", "code": 200}])
