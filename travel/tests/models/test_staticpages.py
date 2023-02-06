import mock
from django.db.models.query import QuerySet

from common.models.staticpages import FrontendManager, StaticPage
from common.tester.testcase import TestCase


class TestFrontendManager(TestCase):
    def test_get_queryset(self):
        with mock.patch('common.models.staticpages.get_request', return_value=None, autospec=True):
            manager = FrontendManager()
            manager.model = StaticPage
            qs = manager.get_queryset()
            assert isinstance(qs, QuerySet)
