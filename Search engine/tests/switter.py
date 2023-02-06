# coding: utf-8

from components_app.api.switter.api import SwitterApi
from components_app.tests.base import BaseApiTestCase


def get_attr_values(obj):
    return set((v for k, v in obj.__dict__.items() if not k.startswith('__') and not k.endswith('__')))


class TestSwitterApi(BaseApiTestCase):
    def __init__(self, methodName='runTest'):
        super(TestSwitterApi, self).__init__(methodName=methodName)
        self.api = SwitterApi()
        self.api.load_config({
            'url': 'https://switter-beta.yandex-team.ru/switter/api/v1',
            'http_client': {
                'headers': {
                    'Authorization': 'OAuth AQAD-qJSJoc6AAACMJOmGTQYI09mulPbbpUe9hk'
                }
            }
        })
        self.api.start()

    def test_list(self):
        messages = self.api.messages.list()
        self.assertNotEmptyList(messages)

    def test_create_message(self):
        result = self.api.messages.create(
            info='test', description='test event', tags=['test'], event_type='рантайм', priority='обычное',
            author='robot-maestro', dont_show=True
        )
        self.assertNotEmptyDict(result)

    def test_permissions(self):
        permissions = self.api.permissions.list()
        self.assertNotEmptyList(permissions)
