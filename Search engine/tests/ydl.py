# coding: utf-8

import time
import datetime

from components_app.api.ydl.api import YdlApi
from components_app.configs.base import ydl as ydl_config
from components_app.tests.base import BaseApiTestCase


def get_attr_values(obj):
    return set((v for k, v in obj.__dict__.items() if not k.startswith('__') and not k.endswith('__')))


class TestYdlApi(BaseApiTestCase):
    def __init__(self, methodName='runTest'):
        super(TestYdlApi, self).__init__(methodName=methodName)
        self.api = YdlApi()
        self.api.load_config(ydl_config)

    def test_story_create_get(self):
        story = self.api.story.create(name='test_story', description='Test story')
        self.assertNotEmptyDict(story)
        result = self.api.story.get(id=story['id'])
        self.assertNotEmptyDict(result)
        self.api.story.close(id=story['id'])

    def test_story_list(self):
        result = self.api.story.list(limit=1)
        self.assertNotEmptyList(result)

    def test_event_create(self):
        story = self.api.story.create(name='test', description='test descr')
        result = self.api.event.create(name='test', story=story['id'],
                                       tags=[{'text': 'ydl-api:test'}], level=50)
        self.assertNotEmptyDict(result)

        _time = datetime.datetime.now().isoformat()
        result = self.api.event.create(name='test', story=story['id'],
                                       tags=[{'text': 'ydl-api:test'}], level=50, time=_time)
        self.assertNotEmptyDict(result)
        self.api.story.close(story['id'])

    def test_tag_list(self):
        result = self.api.tag.list(limit=30)
        self.assertNotEmptyList(result)
        self.assertEqual(len(result), 30)

