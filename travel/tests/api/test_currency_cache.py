from travel.avia.library.python.tester.factories import create_currency
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.api.models_utils import get_currency_by_id, get_currency_id_by_code


class CurrencyCache(TestCase):
    def setUp(self):
        reset_all_caches()

    def test_get_currency_by_id(self):
        c = create_currency(code='zzz', iso_code='yyy')
        c.save()

        assert get_currency_by_id(c.id).code == 'zzz'

        assert get_currency_by_id((c.id + 1) * 2) is None

    def test_get_currency_id_by_code(self):
        c = create_currency(code='xxx', iso_code='yyy')
        c.save()

        assert c.id is not None
        assert get_currency_id_by_code('xxx') == c.id

        assert get_currency_id_by_code('yyy') is None
        assert get_currency_id_by_code('zzz') is None
