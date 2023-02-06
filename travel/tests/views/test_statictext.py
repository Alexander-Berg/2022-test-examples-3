# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
from django.test import Client

from common.models.disclaimers import StaticText
from common.tester.testcase import TestCase


class TestStaticText(TestCase):
    def setUp(self):
        self.client = Client()

    def test_disclaimer(self):
        text1 = StaticText(code='code1', announce_ru='анонс', content_ru='контент', announce_uk='український дебют 1',
                           content_uk='українське вміст 1')
        text2 = StaticText(code='code2', announce_ru='анонс', content_ru='контент', announce_uk='український дебют 2',
                           content_uk='українське вміст 2')

        def get_static_text_side_effect(code):
            if code == 'code1':
                return text1
            if code == 'code2':
                return text2
            return None

        with mock.patch('common.models.disclaimers.StaticText.objects.get',
                        side_effect=get_static_text_side_effect) as m_get_static_text:
            response = self.client.get('/uk/statictext/disclaimers/?codes=code1,code2')

            assert response.status_code == 200

            data = json.loads(response.content)

            assert data == {
                'code1': {
                    'announce': 'український дебют 1',
                    'content': 'українське вміст 1'
                },
                'code2': {
                    'announce': 'український дебют 2',
                    'content': 'українське вміст 2'
                }
            }

        assert m_get_static_text.call_count == 2
        m_get_static_text.assert_any_call(code='code1')
        m_get_static_text.assert_any_call(code='code2')

    def test_absent_disclaimers(self):
        with mock.patch('common.models.disclaimers.StaticText.objects.get',
                        side_effect=StaticText.DoesNotExist) as m_get_static_text:
            response = self.client.get('/uk/statictext/disclaimers/?codes=code1,code2')

            assert response.status_code == 200

            data = json.loads(response.content)

            assert data == {
                'code1': None,
                'code2': None
            }

        assert m_get_static_text.call_count == 2
        m_get_static_text.assert_any_call(code='code1')
        m_get_static_text.assert_any_call(code='code2')
