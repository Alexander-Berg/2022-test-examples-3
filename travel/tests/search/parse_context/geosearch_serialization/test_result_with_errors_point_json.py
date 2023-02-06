# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization import (
    result_with_errors_point_json
)


class TestResultWithErrorsPointJson(TestCase):
    """
    Тесты на JSON-сериализацию пункта прибытия или отправления из результата разбора поискового контекста,
    содержащего ошибки.
    Значение по ключу parsed всегда должно быть None.
    """
    def setUp(self):
        self.name = 'point name'
        self.key = 'point key'
        self.slug = 'point slug'

    def test_name_and_key(self):
        """
        Во входных параметрах для разбора контекста присутствовали и ключ, и название пункта.
        """
        assert result_with_errors_point_json(self.name, self.key) == {
            'title': self.name,
            'key': self.key,
            'slug': None,
            'timezone': None,
            'country': None
        }

    def test_name_and_key_and_slug(self):
        """
        Во входных параметрах для разбора контекста присутствовали и ключ, и название пункта, и слаг.
        """
        assert result_with_errors_point_json(self.name, self.key, self.slug) == {
            'title': self.name,
            'key': self.key,
            'slug': self.slug,
            'timezone': None,
            'country': None
        }

    def test_name_only(self):
        """
        Во входных параметрах для разбора контекста присутствовало только название пункта.
        """
        assert result_with_errors_point_json(self.name, None) == {
            'title': self.name,
            'key': None,
            'slug': None,
            'timezone': None,
            'country': None
        }

    def test_key_only(self):
        """
        Во входных параметрах для разбора контекста присутствовал только ключ пункта.
        """
        assert result_with_errors_point_json(None, self.key) == {
            'title': None,
            'key': self.key,
            'slug': None,
            'timezone': None,
            'country': None
        }
